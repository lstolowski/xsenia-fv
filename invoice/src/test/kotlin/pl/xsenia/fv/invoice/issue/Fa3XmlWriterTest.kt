package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.issue.TestDrafts.draft
import pl.xsenia.fv.invoice.issue.TestDrafts.line
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
}
