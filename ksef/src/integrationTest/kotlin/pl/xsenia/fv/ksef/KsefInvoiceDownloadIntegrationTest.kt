package pl.xsenia.fv.ksef

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.ksef.invoices.InvoiceDateType
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType
import pl.xsenia.fv.ksef.invoices.KsefInvoiceClient
import pl.xsenia.fv.ksef.support.KsefTestConfig
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Reads invoices of the TEST context. Never print invoice contents — the TEST environment is public. */
class KsefInvoiceDownloadIntegrationTest {

    private val config = KsefTestConfig.loadOrSkip()
    private val httpClient = KsefHttpClientFactory.create(config.environment)
    private val publicKeys = KsefPublicKeyProvider.fromApi(httpClient)
    private val tokens = KsefTokenManager(KsefAuthenticator(httpClient, publicKeys), config.nip, ksefToken = { config.ksefToken })
    private val invoices = KsefInvoiceClient(httpClient, tokens, publicKeys)

    private val from = Instant.parse("2026-10-01T00:00:00Z")
    private val to = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)

    @AfterEach
    fun closeClient() = httpClient.close()

    @Test
    fun `queries sales and purchase invoice metadata`(): Unit = runBlocking {
        val sales = invoices.queryMetadata(InvoiceSubjectType.SELLER, InvoiceDateType.ISSUE, from, to).invoices
        invoices.queryMetadata(InvoiceSubjectType.BUYER, InvoiceDateType.ISSUE, from, to)

        sales.forEach { assertEquals(config.nip, it.seller.nip) }
    }

    @Test
    fun `downloads invoice XML and the same invoice in an export package`(): Unit = runBlocking {
        val sales = invoices.queryMetadata(InvoiceSubjectType.SELLER, InvoiceDateType.PERMANENT_STORAGE, from, to).invoices
        assumeTrue(sales.isNotEmpty(), "no sales invoices in the TEST context since $from")
        val first = sales.first()

        val xml = invoices.downloadInvoiceXml(first.ksefNumber)
        assertEquals(first.invoiceHash, xml.sha256Base64, "XML hash should match metadata")
        assertTrue(xml.content.toString(Charsets.UTF_8).contains("<P_2>${first.invoiceNumber}</P_2>"), "XML should contain the invoice number")

        // The XML was produced by another system (e.g. Aplikacja Podatnika) — the parser must agree with KSeF metadata.
        val parsed = InvoiceXmlParser.parse(xml.content)
        assertEquals(first.invoiceNumber, parsed.invoiceNumber)
        assertEquals(first.issueDate, parsed.issueDate)
        assertEquals(first.seller.nip, parsed.seller.nip)
        assertEquals(first.buyer.identifier.value, parsed.buyer.nip)
        assertEquals(0, first.grossAmount.compareTo(parsed.grossTotal), "gross amount")
        assertEquals(0, first.netAmount.compareTo(parsed.netTotal), "net amount")
        assertEquals(0, first.vatAmount.compareTo(parsed.vatTotal), "VAT amount")

        val export = invoices.exportInvoices(InvoiceSubjectType.SELLER, InvoiceDateType.PERMANENT_STORAGE, from, to)
        assertTrue(export.invoices.any { it.ksefNumber == first.ksefNumber }, "export metadata should contain the invoice")
        val exportedXml = requireNotNull(export.xmlByKsefNumber[first.ksefNumber]) { "export should contain the invoice XML" }
        assertEquals(first.invoiceHash, KsefCrypto.sha256Base64(exportedXml))
    }
}
