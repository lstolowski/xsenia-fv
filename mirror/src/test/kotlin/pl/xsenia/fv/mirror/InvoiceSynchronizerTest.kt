package pl.xsenia.fv.mirror

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType.BUYER
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType.SELLER
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class InvoiceSynchronizerTest {

    @TempDir
    lateinit var dir: File

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = this
    }

    private val initialDate = Instant.parse("2026-03-31T22:00:00Z") // 2026-04-01 in Poland
    private val clock = MutableClock(Instant.parse("2026-10-12T12:00:00Z"))
    private val source = FakeInvoiceSource(hwm = Instant.parse("2026-10-12T11:50:00Z"))
    private val mirror by lazy { InvoiceMirror(dir) }
    private val synchronizer by lazy { InvoiceSynchronizer(source, mirror, initialDate, clock) }

    @Test
    fun `first sync exports history in 90-day windows up to the HWM for both categories`() = runTest {
        source.store(SELLER, "01/04/2026", stored = "2026-04-30T10:00:00Z")
        source.store(SELLER, "01/09/2026", stored = "2026-10-01T10:00:00Z", saleDate = "2026-09-30")
        source.store(BUYER, "FV/TEST/1", stored = "2026-08-15T10:00:00Z")

        val result = synchronizer.sync()

        assertEquals(mapOf(InvoiceCategory.SALES to 2, InvoiceCategory.PURCHASE to 1), result.added)
        assertEquals(
            listOf(
                Triple(SELLER, initialDate, Instant.parse("2026-06-29T22:00:00Z")),
                Triple(SELLER, Instant.parse("2026-06-29T22:00:00Z"), Instant.parse("2026-09-27T22:00:00Z")),
                Triple(SELLER, Instant.parse("2026-09-27T22:00:00Z"), null),
            ),
            source.exports.filter { it.first == SELLER },
        )
        // Initial export already brought the XML; no single downloads needed.
        assertTrue(source.downloads.isEmpty())

        val sales = mirror.entries(InvoiceCategory.SALES)
        assertEquals(listOf("01/04/2026", "01/09/2026"), sales.map { it.metadata.invoiceNumber })
        // Delivery date comes from the XML (P_6), not from the issue date.
        assertEquals(LocalDate.parse("2026-09-30"), sales[1].deliveryDate)
        assertNotNull(mirror.readXml(InvoiceCategory.SALES, sales[1].metadata.ksefNumber))
        assertEquals(1, mirror.entries(InvoiceCategory.PURCHASE).size)

        val state = mirror.syncState()
        assertEquals(source.hwm, state.sales.checkpoint)
        assertEquals(clock.now, state.lastSyncAt)
    }

    @Test
    fun `next sync downloads only invoices stored since the checkpoint`() = runTest {
        source.store(SELLER, "01/09/2026", stored = "2026-10-01T10:00:00Z")
        synchronizer.sync()

        source.store(SELLER, "01/10/2026", stored = "2026-10-12T12:30:00Z")
        source.store(BUYER, "FV/TEST/2", stored = "2026-10-12T12:40:00Z")
        clock.now = Instant.parse("2026-10-12T13:00:00Z")
        source.hwm = Instant.parse("2026-10-12T12:35:00Z")
        val exportsBefore = source.exports.size

        val result = synchronizer.sync()

        assertEquals(mapOf(InvoiceCategory.SALES to 1, InvoiceCategory.PURCHASE to 1), result.added)
        assertEquals(exportsBefore, source.exports.size, "no new exports after the first sync")
        assertEquals(Triple(SELLER, Instant.parse("2026-10-12T11:50:00Z"), clock.now), source.queries.last { it.first == SELLER })
        assertEquals(2, source.downloads.size)
        assertEquals(Instant.parse("2026-10-12T12:35:00Z"), mirror.syncState().sales.checkpoint)
    }

    @Test
    fun `invoices after the HWM are kept and not downloaded twice`() = runTest {
        source.store(SELLER, "01/10/2026", stored = "2026-10-12T11:55:00Z") // after HWM, already visible
        synchronizer.sync()
        assertEquals(1, mirror.entries(InvoiceCategory.SALES).size)
        val downloads = source.downloads.size

        clock.now = Instant.parse("2026-10-12T13:00:00Z")
        synchronizer.sync()

        // The range after the old HWM is queried again, but the known invoice is not downloaded again.
        assertEquals(downloads, source.downloads.size)
        assertEquals(1, mirror.entries(InvoiceCategory.SALES).size)
    }

    @Test
    fun `continues a truncated export from the last stored invoice`() = runTest {
        source.exportLimit = 2
        listOf("2026-04-10", "2026-04-11", "2026-04-12", "2026-04-13", "2026-04-14").forEachIndexed { i, day ->
            source.store(SELLER, "0${i + 1}/04/2026", stored = "${day}T10:00:00Z")
        }

        synchronizer.sync()

        assertEquals(5, mirror.entries(InvoiceCategory.SALES).size)
        val sellerExports = source.exports.filter { it.first == SELLER }
        assertEquals(Instant.parse("2026-04-11T10:00:00Z"), sellerExports[1].second)
    }

    @Test
    fun `resumes after an interrupted sync without losing progress`() = runTest {
        source.store(SELLER, "01/09/2026", stored = "2026-10-01T10:00:00Z")
        synchronizer.sync()
        source.store(SELLER, "02/10/2026", stored = "2026-10-12T12:10:00Z")
        source.store(SELLER, "03/10/2026", stored = "2026-10-12T12:20:00Z")
        clock.now = Instant.parse("2026-10-12T13:00:00Z")
        source.failDownloadsAfter = 1

        assertThrows<IllegalStateException> { synchronizer.sync() }
        assertEquals(2, mirror.entries(InvoiceCategory.SALES).size, "the first new invoice is kept")

        source.failDownloadsAfter = Int.MAX_VALUE
        synchronizer.sync()

        assertEquals(3, mirror.entries(InvoiceCategory.SALES).size)
        assertEquals(2, source.downloads.size, "the kept invoice is not downloaded again")
    }

    @Test
    fun `unparsable XML falls back to the issue date`() = runTest {
        source.store(BUYER, "Lorem", stored = "2026-05-05T10:00:00Z")
        val stored = source.invoices.single()
        source.invoices[0] = FakeInvoiceSource.StoredInvoice(BUYER, stored.metadata, "<Lorem/>".toByteArray())

        synchronizer.sync()

        val entry = mirror.entries(InvoiceCategory.PURCHASE).single()
        assertEquals(LocalDate.parse("2026-05-05"), entry.deliveryDate)
        assertNotNull(entry.parseError)
        assertNull(mirror.entries(InvoiceCategory.SALES).firstOrNull())
    }
}
