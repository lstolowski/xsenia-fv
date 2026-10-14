package pl.xsenia.fv.ksef

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.exchangerates.ExchangeRateResult
import pl.xsenia.fv.exchangerates.NbpClient
import pl.xsenia.fv.exchangerates.lastRateBefore
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.ExchangeRateDateRule
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.InvoiceCalculator
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.invoice.issue.VatRate
import pl.xsenia.fv.invoice.model.Invoice
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
 * Each test sends ONE invoice with fictitious data to KSeF TEST (allowed by the user, see AGENTS.md).
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

    private val today = LocalDate.now(ZoneId.of("Europe/Warsaw"))
    private val seller = Company("Jan Testowy Usługi Testowe", config.nip, Address("ul. Testowa 1", "00-001 Testowo", null), null, null)
    private val buyer = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo", null), null)
    private val euroAccount = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR")

    @Test
    fun `sends a generated FA(3) invoice and receives KSeF number and UPO`(): Unit = runBlocking {
        val number = invoiceNumber("IT/")
        val invoice = sendAndDownload(
            SalesInvoiceDraft(
                invoiceNumber = number,
                issueDate = today,
                deliveryDate = today,
                seller = seller,
                buyer = buyer,
                bankAccount = BankAccount("Konto testowe", "Toyota Bank", "85216097583216850246002152", "PLN"),
                // One line per supported rate, including "np" (np II, reverse charge with the remark) — ADR 0005.
                lines = VatRate.entries.map { rate ->
                    DraftLine("Usługa testowa (test integracyjny, ${rate.code})", BigDecimal.ONE, "godz.", BigDecimal("1.00"), rate)
                },
            ),
        )

        assertEquals(number, invoice.invoiceNumber)
        assertEquals("PLN", invoice.currency)
        assertEquals(VatRate.entries.map { it.code }, invoice.lines.map { it.vatRate })
        assertTrue(invoice.annotations.reverseCharge)
        assertEquals(listOf("Uwagi" to "Reverse charge"), invoice.additionalInfo)
    }

    /** EUR invoice with the real NBP rate chosen by the VAT act rule for sending today (ADR 0008). */
    @Test
    fun `sends an EUR invoice with the NBP rate and VAT in PLN`(): Unit = runBlocking {
        val rate = nbpRate(InvoiceCurrency.EUR)
        val number = invoiceNumber("IT/EUR/")
        val draft = SalesInvoiceDraft(
            invoiceNumber = number,
            issueDate = today,
            deliveryDate = today,
            seller = seller,
            buyer = buyer,
            bankAccount = euroAccount,
            lines = listOf(
                DraftLine("Usługa testowa (test integracyjny, EUR 23%)", BigDecimal(10), "godz.", BigDecimal("100.00"), VatRate.RATE_23),
                DraftLine("Usługa testowa (test integracyjny, EUR np)", BigDecimal.ONE, "usł.", BigDecimal("500.00"), VatRate.NOT_TAXABLE),
            ),
            currency = InvoiceCurrency.EUR,
            exchangeRate = rate,
        )

        val invoice = sendAndDownload(draft)

        assertEquals(number, invoice.invoiceNumber)
        assertEquals("EUR", invoice.currency)
        assertEquals(List(2) { rate.rate }, invoice.lines.map { it.exchangeRate })
        assertEquals(InvoiceCalculator.totals(draft).byRate.first().vatPln, invoice.vatSummary.first().vatInPln)
        assertEquals(BigDecimal("1730.00"), invoice.grossTotal)
        assertEquals(
            listOf("Uwagi" to "Reverse charge", Fa3XmlWriter.EXCHANGE_RATE_KEY to Fa3XmlWriter.exchangeRateRemark(rate)),
            invoice.additionalInfo,
        )
    }

    /** Buyer from another EU country: VAT UE number, its country, `np II` (ADR 0009). Fictitious buyer. */
    @Test
    fun `sends an invoice to a buyer from another EU country`(): Unit = runBlocking {
        val german = Counterparty(
            "Lorem Testowy GmbH", address = Address("Teststraße 1", "00000 Testdorf", null), countryCode = "DE", taxNumber = "999999990",
        )
        val invoice = sendAndDownload(foreignDraft(invoiceNumber("IT/UE/"), german, InvoiceCurrency.EUR, euroAccount))

        assertEquals("DE999999990", invoice.buyer.euVatNumber)
        assertEquals("DE", invoice.buyer.countryCode)
        assertEquals(listOf("np II"), invoice.lines.map { it.vatRate })
        assertTrue(invoice.annotations.reverseCharge)
    }

    /** Buyer from outside the EU: its tax identifier, its country, `np I` (ADR 0009). Fictitious buyer. */
    @Test
    fun `sends an invoice to a buyer from outside the EU`(): Unit = runBlocking {
        val american = Counterparty(
            "Lorem Test Inc.", address = Address("1 Test Street", "Testville, TS 00000", null), countryCode = "US", taxNumber = "00-0000000",
        )
        val dollarAccount = BankAccount("Konto testowe USD", "Santander Bank Polska", "14109004081809679290810033", "USD")
        val invoice = sendAndDownload(foreignDraft(invoiceNumber("IT/US/"), american, InvoiceCurrency.USD, dollarAccount))

        assertEquals("US 00-0000000", invoice.buyer.otherId)
        assertEquals("US", invoice.buyer.countryCode)
        assertEquals(listOf("np I"), invoice.lines.map { it.vatRate })
        assertTrue(invoice.annotations.reverseCharge)
        assertEquals(listOf("Uwagi" to "Reverse charge"), invoice.additionalInfo.take(1))
    }

    private suspend fun foreignDraft(number: String, buyer: Counterparty, currency: InvoiceCurrency, account: BankAccount) = SalesInvoiceDraft(
        invoiceNumber = number,
        issueDate = today,
        deliveryDate = today,
        seller = seller,
        buyer = buyer,
        bankAccount = account,
        lines = listOf(DraftLine("Usługa testowa (test integracyjny, nabywca ${buyer.countryCode})", BigDecimal.ONE, "usł.", BigDecimal("500.00"), VatRate.NOT_TAXABLE)),
        currency = currency,
        exchangeRate = nbpRate(currency),
    )

    /** The real NBP rate chosen by the VAT act rule for sending today (ADR 0008). */
    private suspend fun nbpRate(currency: InvoiceCurrency): ExchangeRate {
        val nbp = NbpClient.createHttpClient()
        val result = try {
            NbpClient(nbp).lastRateBefore(currency, ExchangeRateDateRule.referenceDate(deliveryDate = today, issueDay = today))
        } finally {
            nbp.close()
        }
        return (result as? ExchangeRateResult.Found)?.rate ?: error("No NBP rate: $result")
    }

    private fun invoiceNumber(prefix: String) =
        prefix + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now())

    /** Sends the invoice, checks the KSeF number and UPO, and returns the invoice as stored in KSeF. */
    private suspend fun sendAndDownload(draft: SalesInvoiceDraft): Invoice {
        val xml = Fa3XmlWriter.write(draft, createdAt = Instant.now())

        val sent = KsefInvoiceSender(httpClient, tokens, publicKeys).send(xml)

        assertTrue(sent.ksefNumber.startsWith(config.nip + "-"), "KSeF number starts with the seller NIP")
        val upo = requireNotNull(sent.upo) { "UPO should be available" }.toString(Charsets.UTF_8)
        assertTrue(upo.contains(sent.ksefNumber), "UPO confirms the KSeF number")

        // The invoice stored in KSeF is exactly the XML we generated.
        val downloaded = KsefInvoiceClient(httpClient, tokens, publicKeys).downloadInvoiceXml(sent.ksefNumber)
        assertEquals(KsefCrypto.sha256Base64(xml), downloaded.sha256Base64)
        return InvoiceXmlParser.parse(downloaded.content)
    }
}
