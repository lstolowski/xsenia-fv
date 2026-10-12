package pl.xsenia.fv.invoice.xml

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.invoice.model.BankAccountInfo
import pl.xsenia.fv.invoice.model.CorrectedInvoice
import pl.xsenia.fv.invoice.model.DatePeriod
import pl.xsenia.fv.invoice.model.DueDate
import pl.xsenia.fv.invoice.model.VatCategory
import java.math.BigDecimal
import java.time.LocalDate

class InvoiceXmlParserTest {

    private fun parse(file: String) =
        InvoiceXmlParser.parse(requireNotNull(javaClass.classLoader.getResource("invoices/$file")).readBytes())

    @Test
    fun `parses FA(3) VAT invoice`() {
        val invoice = parse("fa3-vat.xml")

        assertEquals("FA (3)", invoice.schema)
        assertEquals("VAT", invoice.kind)
        assertFalse(invoice.isCorrection)
        assertEquals("01/09/2026", invoice.invoiceNumber)
        assertEquals(LocalDate.parse("2026-10-01"), invoice.issueDate)
        assertEquals("Testowo", invoice.issuePlace)
        assertEquals("PLN", invoice.currency)

        assertEquals("Jan Testowy Usługi Testowe", invoice.seller.name)
        assertEquals("9945082500", invoice.seller.nip)
        // AdresL1 with a line break (as written by Aplikacja Podatnika KSeF) is split into lines.
        assertEquals(listOf("ul. Testowa 1", "00-001 Testowo"), invoice.seller.addressLines)
        assertEquals(listOf("jan.testowy@example.com"), invoice.seller.emails)
        assertEquals(listOf("+48000000000"), invoice.seller.phones)
        assertEquals("6691034274", invoice.buyer.nip)
        assertEquals(listOf("ul. Testowa 6", "00-002 Testowo"), invoice.buyer.addressLines)

        val line = invoice.lines.single()
        assertEquals("Usługa testowa", line.name)
        assertEquals("szt.", line.unit)
        assertEquals(BigDecimal("1"), line.quantity)
        assertEquals(BigDecimal("233"), line.unitNetPrice)
        assertEquals(BigDecimal("233"), line.netValue)
        assertEquals("23", line.vatRate)
        assertEquals(BigDecimal("286.59"), line.displayGrossValue)

        assertEquals(VatCategory.STANDARD, invoice.vatSummary.single().category)
        assertEquals(BigDecimal("233"), invoice.netTotal)
        assertEquals(BigDecimal("53.59"), invoice.vatTotal)
        assertEquals(BigDecimal("286.59"), invoice.grossTotal)

        val payment = requireNotNull(invoice.payment)
        assertFalse(payment.paid)
        assertEquals(6, payment.methodCode)
        assertEquals(listOf(DueDate(LocalDate.parse("2026-10-22"), null)), payment.dueDates)
        assertEquals(
            listOf(
                BankAccountInfo("85216097583216850246002152", "Toyota Bank", null, null),
                BankAccountInfo("55109004081797992973253811", "Santander Bank Polska", null, "Konto testowe"),
            ),
            payment.bankAccounts,
        )

        assertEquals(listOf("Zamówienie" to "Lorem ipsum 1"), invoice.additionalInfo)
        assertEquals(listOf("Dziękujemy — dane testowe"), invoice.footer)
        assertFalse(invoice.annotations.splitPayment)
        assertNull(invoice.annotations.exemptionBasis)
        assertNull(invoice.correction)
    }

    @Test
    fun `delivery date is P_6 when present`() {
        assertEquals(LocalDate.parse("2026-09-30"), parse("fa3-vat.xml").deliveryDate)
    }

    @Test
    fun `delivery date is the latest line P_6A when P_6 is absent`() {
        val invoice = parse("fa3-multi-rate.xml")

        assertNull(invoice.saleDate)
        assertEquals(LocalDate.parse("2026-09-28"), invoice.deliveryDate)
    }

    @Test
    fun `delivery date is the end of OkresFa for period invoices`() {
        val invoice = parse("fa3-correction.xml")

        assertEquals(DatePeriod(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31")), invoice.period)
        assertEquals(LocalDate.parse("2026-08-31"), invoice.deliveryDate)
    }

    @Test
    fun `delivery date falls back to issue date`() {
        val invoice = parse("fa2-vat.xml")

        assertEquals(invoice.issueDate, invoice.deliveryDate)
    }

    @Test
    fun `parses several VAT rates, exemption and paid invoice`() {
        val invoice = parse("fa3-multi-rate.xml")

        assertEquals(
            listOf(VatCategory.STANDARD, VatCategory.REDUCED_8, VatCategory.EXEMPT),
            invoice.vatSummary.map { it.category },
        )
        assertNull(invoice.vatSummary.last().vat)
        assertEquals(BigDecimal("160.00"), invoice.netTotal)
        assertEquals(BigDecimal("27.00"), invoice.vatTotal)
        assertEquals(BigDecimal("187.00"), invoice.grossTotal)

        assertEquals(listOf("23", "8", "zw"), invoice.lines.map { it.vatRate })
        assertEquals(BigDecimal("2.5"), invoice.lines[0].quantity)
        assertEquals(listOf(BigDecimal("123.00"), BigDecimal("54.00"), BigDecimal("10.00")), invoice.lines.map { it.displayGrossValue })
        assertEquals(LocalDate.parse("2026-09-10"), invoice.lines[0].deliveryDate)

        assertEquals("Art. 43 ust. 1 pkt 37 ustawy o VAT (test)", invoice.annotations.exemptionBasis)
        val payment = requireNotNull(invoice.payment)
        assertTrue(payment.paid)
        assertEquals(LocalDate.parse("2026-10-05"), payment.paymentDate)
        assertEquals(2, payment.methodCode)
        assertTrue(payment.bankAccounts.isEmpty())
        assertTrue(invoice.buyer.addressLines.isEmpty())
    }

    @Test
    fun `parses correction details and state-before lines`() {
        val invoice = parse("fa3-correction.xml")

        assertEquals("KOR", invoice.kind)
        assertTrue(invoice.isCorrection)
        val correction = requireNotNull(invoice.correction)
        assertEquals("Błędna cena jednostkowa (test)", correction.reason)
        assertEquals("1", correction.type)
        assertEquals(
            listOf(CorrectedInvoice("FV/TEST/77", LocalDate.parse("2026-09-02"), "4824062973-20260902-0123456789AB-01")),
            correction.correctedInvoices,
        )
        assertEquals(listOf(true, false), invoice.lines.map { it.isStateBefore })
        assertEquals(BigDecimal("-24.60"), invoice.grossTotal)
        assertEquals(BigDecimal("-4.60"), invoice.vatTotal)
    }

    @Test
    fun `parses FA(2) invoice calculated from gross prices`() {
        val invoice = parse("fa2-vat.xml")

        assertEquals("FA (2)", invoice.schema)
        assertEquals("2026/04/15/1", invoice.invoiceNumber)
        assertEquals(listOf("ul. Testowa 2, 00-005 Testowo"), invoice.seller.addressLines)
        val line = invoice.lines.single()
        assertNull(line.netValue)
        assertEquals(BigDecimal("100.00"), line.unitGrossPrice)
        assertEquals(BigDecimal("100.00"), line.displayGrossValue)
        assertNull(invoice.payment)
    }

    @Test
    fun `rejects invalid XML, other root elements and DOCTYPE`() {
        assertThrows<InvoiceParseException> { InvoiceXmlParser.parse("<Faktura><Fa>".toByteArray()) }
        assertThrows<InvoiceParseException> { InvoiceXmlParser.parse("<Lorem/>".toByteArray()) }
        assertThrows<InvoiceParseException> {
            InvoiceXmlParser.parse(
                """<?xml version="1.0"?><!DOCTYPE Faktura [<!ENTITY x SYSTEM "file:///etc/passwd">]><Faktura>&x;</Faktura>""".toByteArray(),
            )
        }
    }

    @Test
    fun `reports missing required element`() {
        val xml = requireNotNull(javaClass.classLoader.getResource("invoices/fa3-vat.xml")).readText()
            .replace("<P_2>01/09/2026</P_2>", "")

        val e = assertThrows<InvoiceParseException> { InvoiceXmlParser.parse(xml.toByteArray()) }
        assertTrue(e.message!!.contains("P_2"))
    }
}
