package pl.xsenia.fv.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.ksef.KsefFailure
import pl.xsenia.fv.ksef.invoices.InvoiceExport
import pl.xsenia.fv.ksef.invoices.InvoiceMetadataResult
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType
import pl.xsenia.fv.mirror.InvoiceMirror
import pl.xsenia.fv.mirror.InvoiceSource
import pl.xsenia.fv.mirror.InvoiceSynchronizer
import java.io.File
import java.time.Instant

class InvoiceSyncServiceTest {

    @TempDir
    lateinit var dir: File

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** KSeF that never answers the first request: the synchronization stays running until it is cancelled. */
    private class HangingSource : InvoiceSource {
        val started = CompletableDeferred<Unit>()
        private val never = CompletableDeferred<Nothing>()

        override suspend fun queryStoredSince(subjectType: InvoiceSubjectType, from: Instant, to: Instant): InvoiceMetadataResult {
            started.complete(Unit)
            never.await()
        }

        override suspend fun downloadXml(ksefNumber: String): ByteArray = never.await()

        override suspend fun exportStored(subjectType: InvoiceSubjectType, from: Instant, to: Instant?): InvoiceExport {
            started.complete(Unit)
            never.await()
        }
    }

    /** KSeF source that fails with [error] (not a KSeF error: e.g. damaged local data). */
    private class BrokenSource(var error: Exception? = IllegalStateException("boom")) : InvoiceSource {
        private val hanging = HangingSource()

        override suspend fun queryStoredSince(subjectType: InvoiceSubjectType, from: Instant, to: Instant): InvoiceMetadataResult {
            error?.let { throw it }
            return hanging.queryStoredSince(subjectType, from, to)
        }

        override suspend fun downloadXml(ksefNumber: String): ByteArray = hanging.downloadXml(ksefNumber)

        override suspend fun exportStored(subjectType: InvoiceSubjectType, from: Instant, to: Instant?): InvoiceExport {
            error?.let { throw it }
            return hanging.exportStored(subjectType, from, to)
        }
    }

    private fun service(source: InvoiceSource, mirror: InvoiceMirror) = InvoiceSyncService(
        scope,
        mirror,
        readToken = { "token" },
        synchronizer = lazy { InvoiceSynchronizer(source, mirror, Instant.parse("2026-03-31T22:00:00Z")) },
    )

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `clearing the mirror cancels a running synchronization and leaves no data`() = runBlocking {
        val mirrorDir = File(dir, "invoices").apply { mkdirs() }
        val marker = File(mirrorDir, "sales/xml/old.xml").apply { parentFile.mkdirs(); writeText("<Faktura/>") }
        val source = HangingSource()
        val service = service(source, InvoiceMirror(mirrorDir))

        service.syncNow()
        withTimeout(10_000) { source.started.await() }
        assertTrue(service.state.value.running)

        withTimeout(10_000) { service.clearMirror() }

        assertFalse(service.state.value.running, "cancelled synchronization must not stay marked as running")
        assertFalse(marker.exists())
        assertFalse(mirrorDir.exists())
        assertNull(service.state.value.lastSyncAt)
        assertNull(service.state.value.error)
    }

    @Test
    fun `a synchronization can be started again after clearing`() = runBlocking {
        val mirror = InvoiceMirror(File(dir, "invoices"))
        val source = HangingSource()
        val service = service(source, mirror)
        service.syncNow()
        withTimeout(10_000) { source.started.await() }
        service.clearMirror()

        service.syncNow()

        assertTrue(service.state.value.running)
        withTimeout(10_000) { service.clearMirror() }
        assertEquals(false, service.state.first().running)
    }

    @Test
    fun `clearing without a running synchronization just removes the data`() = runBlocking {
        val mirrorDir = File(dir, "invoices").apply { mkdirs() }
        File(mirrorDir, "sync-state.json").writeText("{}")
        val service = service(HangingSource(), InvoiceMirror(mirrorDir))

        service.clearMirror()

        assertFalse(mirrorDir.exists())
        assertFalse(service.state.value.running)
    }

    @Test
    fun `an unexpected error ends the synchronization with a generic failure and the next one can start`() = runBlocking {
        val source = BrokenSource()
        val service = service(source, InvoiceMirror(File(dir, "invoices")))

        service.syncNow()
        val failed = withTimeout(10_000) { service.state.first { !it.running && it.error != null } }

        assertEquals(KsefFailure.Unexpected, failed.error)

        // Not stuck: another attempt starts (and now runs into the hanging source).
        source.error = null
        service.syncNow()
        assertTrue(service.state.value.running)
        withTimeout(10_000) { service.clearMirror() }
        assertFalse(service.state.value.running)
    }

    @Test
    fun `a corrupted mirror index does not crash the service and the mirror is downloaded again`() = runBlocking {
        val mirrorDir = File(dir, "invoices")
        File(mirrorDir, "sales").mkdirs()
        File(mirrorDir, "sales/index.json").writeText("[{ broken")
        val source = HangingSource()
        val service = service(source, InvoiceMirror(mirrorDir))

        // The initial refresh read the damaged index: counts are zero, no exception.
        withTimeout(10_000) { service.state.first { it.mirrorVersion > 0 } }
        assertEquals(0, service.state.value.invoiceCounts[pl.xsenia.fv.mirror.InvoiceCategory.SALES])

        service.syncNow()
        withTimeout(10_000) { source.started.await() }
        assertTrue(service.state.value.running)
        withTimeout(10_000) { service.clearMirror() }
    }
}
