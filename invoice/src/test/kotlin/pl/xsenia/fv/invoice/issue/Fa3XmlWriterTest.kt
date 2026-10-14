package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.issue.TestDrafts.draft
import pl.xsenia.fv.invoice.issue.TestDrafts.line
import pl.xsenia.fv.invoice.issue.TestDrafts.rate
import pl.xsenia.fv.invoice.model.BankAccountInfo
import pl.xsenia.fv.invoice.model.DueDate
import pl.xsenia.fv.invoice.model.VatCategory
import pl.xsenia.fv.invoice.xml.FaSchemaValidator
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class Fa3XmlWriterTest {

    private val createdAt = Instant.parse("2026-10-01T08:15:30.123Z")

    private fun writeValid(draft: SalesInvoiceDraft): ByteArray =
        Fa3XmlWriter.write(draft, createdAt).also { FaSchemaValidator.validate(FaSchemaValidator.fa3, it) }

    @Test
    fun `generated invoice is valid FA(3) and reads back the same`() {
        val invoice = InvoiceXmlParser.parse(writeValid(draft()))

        assertEquals("FA (3)", invoice.schema)
        assertEquals("VAT", invoice.kind)
        assertEquals("01/09/2026", invoice.invoiceNumber)
        assertEquals(LocalDate.parse("2026-10-01"), invoice.issueDate)
        assertEquals(LocalDate.parse("2026-09-30"), invoice.saleDate)
        assertEquals(LocalDate.parse("2026-09-30"), invoice.deliveryDate)
        assertEquals("PLN", invoice.currency)

        assertEquals("9945082500", invoice.seller.nip)
        assertEquals(listOf("ul. Testowa 1", "00-001 Testowo"), invoice.seller.addressLines)
        assertEquals(listOf("jan.testowy@example.com"), invoice.seller.emails)
        assertEquals("6691034274", invoice.buyer.nip)
        assertEquals("Testowa Sp. z o.o.", invoice.buyer.name)

        val line = invoice.lines.single()
        assertEquals("Usługa testowa", line.name)
        assertEquals(BigDecimal("1"), line.quantity)
        assertEquals(BigDecimal("233"), line.unitNetPrice)
        assertEquals(BigDecimal("233.00"), line.netValue)
        assertEquals("23", line.vatRate)

        assertEquals(VatCategory.STANDARD, invoice.vatSummary.single().category)
        assertEquals(BigDecimal("233.00"), invoice.netTotal)
        assertEquals(BigDecimal("53.59"), invoice.vatTotal)
        assertEquals(BigDecimal("286.59"), invoice.grossTotal)

        val payment = requireNotNull(invoice.payment)
        assertEquals(6, payment.methodCode)
        assertEquals(listOf(DueDate(LocalDate.parse("2026-10-22"), null)), payment.dueDates)
        assertEquals(listOf(BankAccountInfo("85216097583216850246002152", "Toyota Bank", null, null)), payment.bankAccounts)
        assertFalse(invoice.annotations.splitPayment)
        assertFalse(invoice.annotations.reverseCharge)
        assertNull(invoice.annotations.exemptionBasis)
    }

    @Test
    fun `lines with different rates are summed per rate`() {
        val draft = draft(
            lines = listOf(
                line(name = "Usługa testowa A", price = "100", rate = VatRate.RATE_23),
                line(name = "Usługa testowa B", price = "100", rate = VatRate.RATE_8),
                line(name = "Usługa testowa C", price = "100", rate = VatRate.RATE_5),
                line(name = "Usługa testowa D", price = "100", rate = VatRate.RATE_0),
                line(name = "Usługa testowa E", price = "100", rate = VatRate.NOT_TAXABLE),
            ),
        )

        val invoice = InvoiceXmlParser.parse(writeValid(draft))

        assertEquals(listOf("23", "8", "5", "0 KR", "np II"), invoice.lines.map { it.vatRate })
        assertEquals(
            listOf(VatCategory.STANDARD, VatCategory.REDUCED_8, VatCategory.REDUCED_5, VatCategory.ZERO_DOMESTIC, VatCategory.NOT_TAXABLE_ART_100),
            invoice.vatSummary.map { it.category },
        )
        assertEquals(List(5) { BigDecimal("100.00") }, invoice.vatSummary.map { it.net })
        assertEquals(listOf(BigDecimal("23.00"), BigDecimal("8.00"), BigDecimal("5.00"), null, null), invoice.vatSummary.map { it.vat })
        assertEquals(BigDecimal("500.00"), invoice.netTotal)
        assertEquals(BigDecimal("36.00"), invoice.vatTotal)
        assertEquals(BigDecimal("536.00"), invoice.grossTotal)
        assertNull(invoice.annotations.exemptionBasis)
    }

    @Test
    fun `not taxable line marks the invoice as reverse charge with a remark`() {
        val xml = writeValid(draft(lines = listOf(line(rate = VatRate.RATE_23), line(price = "150", rate = VatRate.NOT_TAXABLE))))
        val invoice = InvoiceXmlParser.parse(xml)

        assertTrue(invoice.annotations.reverseCharge)
        assertEquals(listOf("Uwagi" to "Reverse charge"), invoice.additionalInfo)
        assertEquals(VatCategory.NOT_TAXABLE_ART_100, invoice.vatSummary.last().category)
        assertTrue(xml.toString(Charsets.UTF_8).contains("<P_13_9>150.00</P_13_9>"))
    }

    @Test
    fun `invoice without not taxable lines has no reverse charge and no remarks`() {
        val invoice = InvoiceXmlParser.parse(writeValid(draft()))

        assertFalse(invoice.annotations.reverseCharge)
        assertTrue(invoice.additionalInfo.isEmpty())
    }

    @Test
    fun `PLN invoice has no exchange rate, VAT in PLN or rate remark`() {
        val xml = writeValid(draft(exchangeRate = rate())).toString(Charsets.UTF_8)

        assertTrue(xml.contains("<KodWaluty>PLN</KodWaluty>"))
        assertFalse(xml.contains("KursWaluty"))
        assertNull(InvoiceXmlParser.parse(xml.toByteArray()).exchangeRate)
        assertNull(InvoiceXmlParser.parse(xml.toByteArray()).vatInPlnTotal)
        assertFalse(xml.contains("P_14_1W"))
        assertFalse(xml.contains("Kurs waluty"))
    }

    // Example from docs/przyklady/kursy-nbp.md (section 6).
    @Test
    fun `EUR invoice has VAT in PLN, the rate in every line and the rate remark`() {
        val draft = draft(
            lines = listOf(line(quantity = "10", unit = "godz.", price = "100.00"), line(price = "500.00", rate = VatRate.NOT_TAXABLE)),
            account = TestDrafts.euroAccount,
            currency = InvoiceCurrency.EUR,
            exchangeRate = rate(),
        )
        val xml = writeValid(draft)
        val text = xml.toString(Charsets.UTF_8)
        val invoice = InvoiceXmlParser.parse(xml)

        assertEquals("EUR", invoice.currency)
        assertTrue(text.contains("<P_13_1>1000.00</P_13_1>\n    <P_14_1>230.00</P_14_1>\n    <P_14_1W>1006.83</P_14_1W>\n    <P_13_9>500.00</P_13_9>"))
        assertEquals(BigDecimal("1006.83"), invoice.vatSummary.first().vatInPln)
        assertEquals(BigDecimal("1730.00"), invoice.grossTotal)
        assertTrue(text.contains("<P_12>23</P_12>\n      <KursWaluty>4.3775</KursWaluty>\n    </FaWiersz>"))
        assertEquals(listOf(BigDecimal("4.3775"), BigDecimal("4.3775")), invoice.lines.map { it.exchangeRate })
        assertEquals(BigDecimal("4.3775"), invoice.exchangeRate)
        assertEquals(BigDecimal("1006.83"), invoice.vatInPlnTotal)
        assertEquals(
            listOf("Uwagi" to "Reverse charge", "Kurs waluty" to "1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09"),
            invoice.additionalInfo,
        )
        assertEquals(listOf(BankAccountInfo("83216097587877893287921742", "Toyota Bank", null, null)), invoice.payment?.bankAccounts)
    }

    @Test
    fun `VAT in PLN for every rate with a VAT amount`() {
        val draft = draft(
            lines = listOf(
                line(price = "100", rate = VatRate.RATE_23),
                line(price = "100", rate = VatRate.RATE_8),
                line(price = "100", rate = VatRate.RATE_5),
                line(price = "100", rate = VatRate.RATE_0),
                line(price = "100", rate = VatRate.NOT_TAXABLE),
            ),
            currency = InvoiceCurrency.USD,
            exchangeRate = rate(InvoiceCurrency.USD, "3.9022"),
        )

        val invoice = InvoiceXmlParser.parse(writeValid(draft))

        assertEquals("USD", invoice.currency)
        // 23.00 × 3.9022 = 89.7506, 8.00 × 3.9022 = 31.2176, 5.00 × 3.9022 = 19.511
        assertEquals(listOf(BigDecimal("89.75"), BigDecimal("31.22"), BigDecimal("19.51"), null, null), invoice.vatSummary.map { it.vatInPln })
        assertEquals(List(5) { BigDecimal("3.9022") }, invoice.lines.map { it.exchangeRate })
        assertEquals(BigDecimal("140.48"), invoice.vatInPlnTotal)
    }

    @Test
    fun `GBP invoice with only not taxable lines has the rate but no VAT in PLN`() {
        val draft = draft(lines = listOf(line(rate = VatRate.NOT_TAXABLE)), currency = InvoiceCurrency.GBP, exchangeRate = rate(InvoiceCurrency.GBP, "5.1655"))
        val text = writeValid(draft).toString(Charsets.UTF_8)

        assertTrue(text.contains("<KodWaluty>GBP</KodWaluty>"))
        assertFalse(text.contains("P_14_"))
        assertTrue(text.contains("<KursWaluty>5.1655</KursWaluty>"))
        assertTrue(text.contains("1 GBP = 5,1655 PLN"))
    }

    @Test
    fun `rate keeps four decimals`() {
        val text = writeValid(draft(currency = InvoiceCurrency.EUR, exchangeRate = rate(rate = "4.3800"))).toString(Charsets.UTF_8)

        assertTrue(text.contains("<KursWaluty>4.3800</KursWaluty>"))
        assertTrue(text.contains("1 EUR = 4,3800 PLN"))
    }

    @Test
    fun `foreign currency invoice without a rate of that currency is not written`() {
        assertThrows<IllegalArgumentException> { Fa3XmlWriter.write(draft(currency = InvoiceCurrency.EUR), createdAt) }
        assertThrows<IllegalArgumentException> {
            Fa3XmlWriter.write(draft(currency = InvoiceCurrency.EUR, exchangeRate = rate(InvoiceCurrency.USD)), createdAt)
        }
    }

    @Test
    fun `omits P_6 when delivery date equals issue date`() {
        val xml = writeValid(draft(issueDate = "2026-10-20", deliveryDate = "2026-10-20", number = "01/10/2026"))

        assertFalse(xml.toString(Charsets.UTF_8).contains("<P_6>"))
        val invoice = InvoiceXmlParser.parse(xml)
        assertNull(invoice.saleDate)
        assertEquals(LocalDate.parse("2026-10-20"), invoice.deliveryDate)
    }

    @Test
    fun `several lines with fractional quantities`() {
        val invoice = InvoiceXmlParser.parse(
            writeValid(
                draft(
                    lines = listOf(
                        line(name = "Lorem ipsum", quantity = "2.5", unit = "godz.", price = "40"),
                        line(name = "Dolor sit", quantity = "3", unit = "szt.", price = "0.10"),
                    ),
                ),
            ),
        )

        assertEquals(listOf(BigDecimal("100.00"), BigDecimal("0.30")), invoice.lines.map { it.netValue })
        assertEquals(BigDecimal("2.5"), invoice.lines[0].quantity)
        assertEquals(BigDecimal("100.30"), invoice.netTotal)
        assertEquals(BigDecimal("23.07"), invoice.vatTotal)
        assertEquals(BigDecimal("123.37"), invoice.grossTotal)
    }

    @Test
    fun `escapes special characters and keeps Polish letters`() {
        val buyer = TestDrafts.buyer.copy(name = "Test & Syn <Spółka> \"Ż\"", address = Address("ul. Łąkowa 1", null, null), email = null)
        val seller = TestDrafts.seller.copy(email = null, phone = null)

        val invoice = InvoiceXmlParser.parse(writeValid(draft(buyer = buyer, seller = seller, lines = listOf(line(name = "Usługa 'A' & B")))))

        assertEquals("Test & Syn <Spółka> \"Ż\"", invoice.buyer.name)
        assertEquals(listOf("ul. Łąkowa 1"), invoice.buyer.addressLines)
        assertEquals("Usługa 'A' & B", invoice.lines.single().name)
        assertTrue(invoice.seller.emails.isEmpty())
    }

    @Test
    fun `output is deterministic`() {
        assertEquals(
            Fa3XmlWriter.write(draft(), createdAt).toString(Charsets.UTF_8),
            Fa3XmlWriter.write(draft(), createdAt).toString(Charsets.UTF_8),
        )
        assertTrue(Fa3XmlWriter.write(draft(), createdAt).toString(Charsets.UTF_8).contains("<DataWytworzeniaFa>2026-10-01T08:15:30Z</DataWytworzeniaFa>"))
    }

    @Test
    fun `buyer from another EU country has the VAT UE number, its country and np II`() {
        val xml = writeValid(draft(buyer = TestDrafts.germanBuyer, lines = listOf(line(price = "500", rate = VatRate.NOT_TAXABLE))))
        val text = xml.toString(Charsets.UTF_8)
        val invoice = InvoiceXmlParser.parse(xml)

        assertTrue(text.contains("<KodUE>DE</KodUE>\n      <NrVatUE>999999990</NrVatUE>\n      <Nazwa>Lorem Testowy GmbH</Nazwa>"))
        assertFalse(text.contains("<Podmiot2>\n    <DaneIdentyfikacyjne>\n      <NIP>"))
        assertEquals("DE999999990", invoice.buyer.euVatNumber)
        assertEquals("DE", invoice.buyer.countryCode)
        assertEquals("PL", invoice.seller.countryCode)
        assertEquals(listOf("np II"), invoice.lines.map { it.vatRate })
        assertEquals(VatCategory.NOT_TAXABLE_ART_100, invoice.vatSummary.single().category)
        assertTrue(invoice.annotations.reverseCharge)
    }

    @Test
    fun `Greek buyer has the EL prefix and the GR country`() {
        val text = writeValid(draft(buyer = TestDrafts.greekBuyer, lines = listOf(line(rate = VatRate.NOT_TAXABLE)))).toString(Charsets.UTF_8)

        assertTrue(text.contains("<KodUE>EL</KodUE>"))
        assertTrue(text.contains("<KodKraju>GR</KodKraju>"))
    }

    @Test
    fun `buyer from outside the EU has its identifier and np I with reverse charge`() {
        val xml = writeValid(
            draft(buyer = TestDrafts.americanBuyer, lines = listOf(line(price = "500", rate = VatRate.NOT_TAXABLE), line(price = "100", rate = VatRate.RATE_23))),
        )
        val text = xml.toString(Charsets.UTF_8)
        val invoice = InvoiceXmlParser.parse(xml)

        assertTrue(text.contains("<KodKraju>US</KodKraju>\n      <NrID>00-0000000</NrID>"))
        assertEquals("US 00-0000000", invoice.buyer.otherId)
        assertEquals("US", invoice.buyer.countryCode)
        assertEquals(listOf("np I", "23"), invoice.lines.map { it.vatRate })
        assertTrue(text.contains("<P_13_8>500.00</P_13_8>"))
        assertFalse(text.contains("P_13_9"))
        assertEquals(listOf(VatCategory.STANDARD, VatCategory.NOT_TAXABLE_ABROAD), invoice.vatSummary.map { it.category })
        assertTrue(invoice.annotations.reverseCharge)
        assertEquals(listOf("Uwagi" to "Reverse charge"), invoice.additionalInfo)
    }

    @Test
    fun `buyer from outside the EU without an identifier gets BrakID`() {
        val text = writeValid(draft(buyer = TestDrafts.britishBuyer, lines = listOf(line(rate = VatRate.NOT_TAXABLE)))).toString(Charsets.UTF_8)

        assertTrue(text.contains("<BrakID>1</BrakID>\n      <Nazwa>Ipsum Test Ltd</Nazwa>"))
        assertTrue(text.contains("<KodKraju>GB</KodKraju>"))
        assertTrue(text.contains("<P_12>np I</P_12>"))
    }

    @Test
    fun `Polish buyer keeps NIP, PL and np II`() {
        val text = writeValid(draft(lines = listOf(line(rate = VatRate.NOT_TAXABLE)))).toString(Charsets.UTF_8)

        assertTrue(text.contains("<NIP>6691034274</NIP>"))
        assertTrue(text.contains("<P_12>np II</P_12>"))
        assertTrue(text.contains("<P_13_9>"))
    }
}
