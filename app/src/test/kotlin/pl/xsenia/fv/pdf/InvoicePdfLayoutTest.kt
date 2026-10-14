package pl.xsenia.fv.pdf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.invoice.issue.VatRate
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class InvoicePdfLayoutTest {

    /** Fixed-width estimate: half of the font size per character. */
    private val measurer = TextMeasurer { text, style -> text.length * style.size * 0.5f }
    private val layout = InvoicePdfLayout(measurer)

    // Fictitious data only (docs/przyklady/).
    private val seller = Company("Jan Testowy Usługi Testowe", "9945082500", Address("ul. Testowa 1", "00-001 Testowo"), "jan.testowy@example.com")
    private val polishBuyer = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo"))
    private val germanBuyer = Counterparty(
        "Lorem Testowy GmbH", address = Address("Teststraße 1", "00000 Testdorf"), countryCode = "DE", taxNumber = "999999990",
    )
    private val plnAccount = BankAccount("Konto testowe", "Toyota Bank", "85216097583216850246002152")
    private val eurAccount = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR")

    private fun line(name: String = "Usługa testowa", quantity: String = "1", price: String = "233", rate: VatRate = VatRate.RATE_23) =
        DraftLine(name, BigDecimal(quantity), "godz.", BigDecimal(price), rate)

    private fun invoice(
        buyer: Counterparty = polishBuyer,
        lines: List<DraftLine> = listOf(line()),
        currency: InvoiceCurrency = InvoiceCurrency.PLN,
        rate: ExchangeRate? = null,
        account: BankAccount = plnAccount,
    ): Invoice = InvoiceXmlParser.parse(
        Fa3XmlWriter.write(
            SalesInvoiceDraft("01/09/2026", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-09-30"), seller, buyer, account, lines, currency, rate),
            Instant.parse("2026-10-01T08:00:00Z"),
        ),
    )

    private fun List<PdfPage>.texts(): List<String> =
        flatMap { page -> page.elements.filterIsInstance<PdfElement.Text>().map { it.text.replace(' ', ' ').replace(' ', ' ') } }

    @Test
    fun `Polish invoice in KSeF`() {
        val pages = layout.layout(invoice(), ksefNumber = "9945082500-20261001-0000000000AA-00")
        val texts = pages.texts()

        assertEquals(1, pages.size)
        assertTrue("Faktura VAT" in texts)
        assertTrue("nr 01/09/2026" in texts)
        assertTrue("Numer KSeF: 9945082500-20261001-0000000000AA-00" in texts)
        assertTrue("Data wystawienia: 01.10.2026" in texts)
        assertTrue("Data dostawy / wykonania usługi: 30.09.2026" in texts)
        assertTrue("Sprzedawca" in texts && "Nabywca" in texts)
        assertTrue("NIP: 6691034274" in texts)
        assertTrue("286,59 zł" in texts)
        assertTrue("Termin płatności: 22.10.2026" in texts)
        assertTrue("Rachunek: 85 2160 9758 3216 8502 4600 2152" in texts)
        assertFalse(texts.any { "BIC" in it })
        assertTrue("Strona 1 z 1" in texts)
        assertFalse(texts.any { "Seller" in it || "Faktura nie wysłana" in it })
    }

    @Test
    fun `unsent invoice is marked instead of the KSeF number`() {
        val texts = layout.layout(invoice(), ksefNumber = null).texts()

        assertTrue("Faktura nie wysłana do KSeF" in texts)
        assertFalse(texts.any { it.startsWith("Numer KSeF") })
    }

    @Test
    fun `foreign buyer gets Polish and English labels, amounts in EUR, VAT in PLN and the rate`() {
        val rate = ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3775"), "197/A/NBP/2026", LocalDate.parse("2026-10-09"))
        val lines = listOf(line(quantity = "10", price = "100.00"), line(price = "500.00", rate = VatRate.NOT_TAXABLE))
        val texts = layout.layout(invoice(germanBuyer, lines, InvoiceCurrency.EUR, rate, eurAccount), ksefNumber = null).texts()

        assertTrue("Faktura VAT / VAT invoice" in texts)
        assertTrue("Faktura nie wysłana do KSeF / Not yet submitted to KSeF" in texts)
        assertTrue("Sprzedawca / Seller" in texts && "Nabywca / Buyer" in texts)
        assertTrue("Numer VAT UE / EU VAT number: DE999999990" in texts)
        assertTrue("Niemcy / Germany" in texts)
        assertTrue("Waluta / Currency: EUR" in texts)
        assertTrue("1 730,00 EUR" in texts)
        // PLN equivalents in a smaller font: net 1 500,00 × 4,3775, VAT in PLN from P_14_1W, gross = net + VAT.
        assertTrue("(6 566,25 PLN)" in texts)
        assertTrue("(1 006,83 PLN)" in texts)
        assertTrue("(7 573,08 PLN)" in texts)
        assertTrue("Kurs waluty / Exchange rate: 4,3775" in texts)
        assertTrue("np" in texts)
        assertTrue("Reverse charge" in texts)
        assertTrue("Kurs waluty: 1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09" in texts)
        assertTrue("Strona 1 z 1 / Page 1 of 1" in texts)
        assertFalse(texts.any { it == "Bank / Bank: Toyota Bank" })
        assertTrue("Bank: Toyota Bank" in texts)
        // Table header: Polish over English.
        assertTrue("Description" in texts && "Nazwa towaru lub usługi" in texts)
    }

    @Test
    fun `many lines continue on next pages with the table header repeated`() {
        val lines = (1..60).map { line(name = "Usługa testowa nr $it", price = "10") }
        val pages = layout.layout(invoice(lines = lines), ksefNumber = "K")

        assertTrue(pages.size >= 2, "pages: ${pages.size}")
        pages.forEachIndexed { index, page ->
            val texts = page.elements.filterIsInstance<PdfElement.Text>().map { it.text }
            assertTrue("Strona ${index + 1} z ${pages.size}" in texts)
            if (texts.any { it.startsWith("Usługa testowa nr") }) assertTrue("Nazwa towaru lub usługi" in texts, "table header on page ${index + 1}")
            page.elements.filterIsInstance<PdfElement.Text>().forEach { assertTrue(it.baseline in 0f..PdfPage.HEIGHT, it.toString()) }
        }
        val names = pages.texts().filter { it.startsWith("Usługa testowa nr") }
        assertEquals((1..60).map { "Usługa testowa nr $it" }, names)
        assertTrue(pages.dropLast(1).all { page -> page.elements.filterIsInstance<PdfElement.Text>().all { it.baseline <= InvoicePdfLayout.BOTTOM || it.text.startsWith("Strona") } })
    }

    @Test
    fun `long line name wraps inside its column`() {
        val name = (1..40).joinToString(" ") { "słowo$it" }
        val pages = layout.layout(invoice(lines = listOf(line(name = name))), ksefNumber = "K")
        val nameParts = pages.single().elements.filterIsInstance<PdfElement.Text>().filter { it.text.startsWith("słowo") }

        assertTrue(nameParts.size > 3)
        assertEquals(name, nameParts.joinToString(" ") { it.text })
        nameParts.forEach { assertTrue(measurer.width(it.text, it.style) <= 168f) }
    }

    @Test
    fun `wrapping keeps line breaks and cuts words wider than the line`() {
        assertEquals(listOf("ab cd", "ef"), measurer.wrap("ab cd ef", PdfTextStyle.NORMAL, 25f))
        assertEquals(listOf("a", "b"), measurer.wrap("a\nb", PdfTextStyle.NORMAL, 100f))
        assertEquals(listOf("abcde", "fghij"), measurer.wrap("abcdefghij", PdfTextStyle.NORMAL, 25f))
        assertEquals(listOf(""), measurer.wrap("", PdfTextStyle.NORMAL, 25f))
    }

    @Test
    fun `file name`() {
        assertEquals("Faktura_01-09-2026.pdf", InvoicePdfLayout.fileName("01/09/2026"))
        assertEquals("Faktura_FV-1-a-b.pdf", InvoicePdfLayout.fileName(" FV 1:a\\b "))
        assertEquals("Faktura_bez-numeru.pdf", InvoicePdfLayout.fileName(""))
    }

    @Test
    fun `breakdown per rate only with several rates`() {
        val single = layout.layout(invoice(), ksefNumber = "K").texts()
        assertFalse("Podsumowanie" in single)
        assertFalse(single.any { it.startsWith("Stawka 23%") })
        assertTrue("Do zapłaty (brutto)" in single)

        val mixed = layout.layout(invoice(lines = listOf(line(), line(price = "100", rate = VatRate.RATE_8))), ksefNumber = "K").texts()
        assertTrue("Podsumowanie" in mixed)
        assertTrue("Stawka 23%: netto 233,00 zł · VAT 53,59 zł" in mixed)
        assertTrue("Stawka 8%: netto 100,00 zł · VAT 8,00 zł" in mixed)
    }

    @Test
    fun `PLN equivalents in a smaller font right of the totals, not for VAT 0 and not for PLN invoices`() {
        val rate = ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3728"), "189/A/NBP/2026", LocalDate.parse("2026-09-29"))
        val page = layout.layout(
            invoice(germanBuyer, listOf(line(price = "250.00", rate = VatRate.NOT_TAXABLE)), InvoiceCurrency.EUR, rate, eurAccount), ksefNumber = null,
        ).single()
        val texts = page.elements.filterIsInstance<PdfElement.Text>()
        // The first one — net (gross is the same with VAT 0).
        fun find(text: String) = texts.first { it.text.replace('\u00A0', ' ').replace('\u202F', ' ') == text }

        val netPln = find("(1 093,20 PLN)")
        assertEquals(PdfTextStyle.SMALL, netPln.style)
        assertEquals(InvoicePdfLayout.RIGHT, netPln.x)
        val net = texts.single { it.baseline == netPln.baseline && it.text.replace('\u00A0', ' ') == "250,00 EUR" }
        assertTrue(net.x < netPln.x - measurer.width(netPln.text, PdfTextStyle.SMALL))
        assertEquals(2, texts.count { it.text.replace('\u00A0', ' ') == "(1 093,20 PLN)" }, "net and gross; no VAT line in PLN for VAT 0")

        val polish = layout.layout(invoice(), ksefNumber = "K").texts()
        assertFalse(polish.any { it.startsWith("(") && it.endsWith("PLN)") })
    }

    @Test
    fun `BIC (SWIFT) under the account number`() {
        val texts = layout.layout(invoice(germanBuyer, account = eurAccount.copy(swift = "TESTPLPW")), ksefNumber = null).texts()

        val account = texts.indexOf("Rachunek / Bank account: 83 2160 9758 7877 8932 8792 1742")
        assertTrue(account >= 0, texts.toString())
        assertEquals("BIC (SWIFT): TESTPLPW", texts[account + 1])
        assertEquals("Bank: Toyota Bank", texts[account + 2])
    }
}
