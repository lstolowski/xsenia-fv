package pl.xsenia.fv.mirror

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.ksef.invoices.KsefInvoiceClient
import pl.xsenia.fv.ksef.support.KsefTestConfig
import java.io.File
import java.time.Instant

/** Full synchronization of the TEST context into a temporary mirror. Never prints invoice contents. */
class InvoiceSyncIntegrationTest {

    @TempDir
    lateinit var dir: File

    private val config = KsefTestConfig.loadOrSkip()
    private val httpClient = KsefHttpClientFactory.create(config.environment)
    private val publicKeys = KsefPublicKeyProvider.fromApi(httpClient)
    private val tokens = KsefTokenManager(KsefAuthenticator(httpClient, publicKeys), config.nip, ksefToken = { config.ksefToken })
    private val source = KsefInvoiceSource(KsefInvoiceClient(httpClient, tokens, publicKeys))

    @AfterEach
    fun closeClient() = httpClient.close()

    @Test
    fun `synchronizes TEST invoices and then only checks for new ones`(): Unit = runBlocking {
        val mirror = InvoiceMirror(dir)
        val synchronizer = InvoiceSynchronizer(source, mirror, initialDate = Instant.parse("2026-09-30T22:00:00Z"))
        val phases = mutableListOf<SyncPhase>()

        val first = synchronizer.sync { phases += it.phase }

        val sales = mirror.entries(InvoiceCategory.SALES)
        assertTrue(sales.isNotEmpty(), "TEST context has at least one sales invoice since 2026-10-01")
        sales.forEach {
            assertEquals(config.nip, it.metadata.seller.nip)
            assertNotNull(mirror.readXml(InvoiceCategory.SALES, it.metadata.ksefNumber))
            assertEquals(null, it.parseError, "XML from KSeF should be parsable")
        }
        assertEquals(sales.size, first.added[InvoiceCategory.SALES])
        assertTrue(SyncPhase.INITIAL_EXPORT in phases)
        assertNotNull(mirror.syncState().lastSyncAt)

        val second = synchronizer.sync()

        assertEquals(0, second.added[InvoiceCategory.SALES])
        assertEquals(sales.size, mirror.entries(InvoiceCategory.SALES).size)
    }
}
