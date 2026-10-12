package pl.xsenia.fv.ksef

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.config.Address
import pl.xsenia.fv.invoice.config.BankAccount
import pl.xsenia.fv.invoice.config.Company
import pl.xsenia.fv.invoice.config.Counterparty
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.ksef.invoices.KsefInvoiceClient
import pl.xsenia.fv.ksef.sending.KsefInvoiceSender
import pl.xsenia.fv.ksef.support.KsefTestConfig
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Sends ONE invoice with fictitious data to KSeF TEST (allowed by the user, see AGENTS.md).
 * The number `IT/...` does not follow the NN/MM/RRRR scheme, so it never affects number suggestions.
 */
class KsefInvoiceSendIntegrationTest {

    private val config = KsefTestConfig.loadOrSkip().also {
        // Every run creates an invoice that cannot be deleted — run only on request (-PsendInvoices).
        assumeTrue(System.getProperty("ksef.sendInvoices") == "true", "invoice sending disabled; run with -PsendInvoices")
    }
    private val httpClient = KsefHttpClientFactory.create(config.environment)
    private val publicKeys = KsefPublicKeyProvider.fromApi(httpClient)
    private val tokens = KsefTokenManager(KsefAuthenticator(httpClient, publicKeys), config.nip, ksefToken = { config.ksefToken })

    @AfterEach
    fun closeClient() = httpClient.close()

    @Test
    fun `sends a generated FA(3) invoice and receives KSeF number and UPO`(): Unit = runBlocking {
        val now = Instant.now()
        val today = LocalDate.now(ZoneId.of("Europe/Warsaw"))
        val number = "IT/" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(now)
        val xml = Fa3XmlWriter.write(
            SalesInvoiceDraft(
                invoiceNumber = number,
                issueDate = today,
                deliveryDate = today,
                seller = Company("Jan Testowy Usługi Testowe", config.nip, Address("ul. Testowa 1", "00-001 Testowo", null), null, null),
                buyer = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo", null), null),
                bankAccount = BankAccount("Konto testowe", "Toyota Bank", "85216097583216850246002152", "PLN"),
                lines = listOf(DraftLine("Usługa testowa (test integracyjny)", BigDecimal.ONE, "szt.", BigDecimal("1.00"))),
            ),
            createdAt = now,
        )

        val sent = KsefInvoiceSender(httpClient, tokens, publicKeys).send(xml)

        assertTrue(sent.ksefNumber.startsWith(config.nip + "-"), "KSeF number starts with the seller NIP")
        val upo = requireNotNull(sent.upo) { "UPO should be available" }.toString(Charsets.UTF_8)
        assertTrue(upo.contains(sent.ksefNumber), "UPO confirms the KSeF number")

        // The invoice stored in KSeF is exactly the XML we generated.
        val downloaded = KsefInvoiceClient(httpClient, tokens, publicKeys).downloadInvoiceXml(sent.ksefNumber)
        assertEquals(KsefCrypto.sha256Base64(xml), downloaded.sha256Base64)
        assertEquals(number, InvoiceXmlParser.parse(downloaded.content).invoiceNumber)
    }
}
