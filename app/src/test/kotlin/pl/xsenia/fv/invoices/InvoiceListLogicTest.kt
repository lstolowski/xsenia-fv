package pl.xsenia.fv.invoices

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.invoices.InvoiceMetadata
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.MirrorEntry
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class InvoiceListLogicTest {

    private fun row(
        number: String,
        delivery: String,
        name: String = "Testowa Sp. z o.o.",
        nip: String = "6691034274",
        net: String = "100.00",
        gross: String = "123.00",
        currency: String = "PLN",
        issue: String = delivery,
    ) = InvoiceRow(
        ksefNumber = "K-$number",
        invoiceNumber = number,
        deliveryDate = LocalDate.parse(delivery),
        issueDate = LocalDate.parse(issue),
        counterpartyName = name,
        counterpartyNip = nip,
        netAmount = BigDecimal(net),
        grossAmount = BigDecimal(gross),
        currency = currency,
        invoiceType = "Vat",
    )

    @Test
    fun `filters by delivery month, not issue month, newest first`() {
        val rows = listOf(
            row("01/09/2026", delivery = "2026-09-30", issue = "2026-10-01"),
            row("02/09/2026", delivery = "2026-09-15"),
            row("01/10/2026", delivery = "2026-10-05"),
        )

        val september = InvoiceListLogic.filter(rows, YearMonth.of(2026, 9), "")

        assertEquals(listOf("01/09/2026", "02/09/2026"), september.map { it.invoiceNumber })
    }

    @Test
    fun `searches name fragment case-insensitively and NIP ignoring separators`() {
        val testowa = row("A", "2026-09-01", name = "Testowa Sp. z o.o.", nip = "6691034274")
        val lorem = row("B", "2026-09-02", name = "Lorem Ipsum S.A.", nip = "9945082500")

        assertTrue(InvoiceListLogic.matches(testowa, "testowa"))
        assertTrue(InvoiceListLogic.matches(lorem, "IPSUM"))
        assertTrue(InvoiceListLogic.matches(testowa, "669-103"))
        assertTrue(InvoiceListLogic.matches(testowa, " 669 103 42 74 "))
        assertFalse(InvoiceListLogic.matches(lorem, "669"))
        assertTrue(InvoiceListLogic.matches(lorem, "  "))
        assertEquals(listOf("B"), InvoiceListLogic.filter(listOf(testowa, lorem), YearMonth.of(2026, 9), "9945").map { it.invoiceNumber })
    }

    @Test
    fun `totals are computed per currency with PLN first`() {
        val totals = InvoiceListLogic.totals(
            listOf(
                row("C", "2026-09-03", net = "10.00", gross = "12.30", currency = "EUR"),
                row("A", "2026-09-01", net = "100.00", gross = "123.00"),
                row("B", "2026-09-02", net = "200.10", gross = "246.12"),
            ),
        )

        assertEquals(
            listOf(
                CurrencyTotals("PLN", 2, BigDecimal("300.10"), BigDecimal("69.02"), BigDecimal("369.12")),
                CurrencyTotals("EUR", 1, BigDecimal("10.00"), BigDecimal("2.30"), BigDecimal("12.30")),
            ),
            totals,
        )
    }

    @Test
    fun `months cover the range up to now plus months with invoices, newest first`() {
        val rows = listOf(row("A", "2026-03-31"), row("B", "2026-09-30"), row("C", "2026-09-01"))

        val months = InvoiceListLogic.months(rows, YearMonth.of(2026, 4), YearMonth.of(2026, 10))

        assertEquals(YearMonth.of(2026, 10) to 0, months.first())
        assertEquals(YearMonth.of(2026, 9) to 2, months[1])
        assertEquals(YearMonth.of(2026, 3) to 1, months.last())
        assertEquals(8, months.size)
    }

    @Test
    fun `counterparty is the buyer on sales and the seller on purchase invoices`() {
        val metadata = InvoiceMetadata(
            ksefNumber = "K", invoiceNumber = "01/09/2026", issueDate = LocalDate.parse("2026-10-01"),
            invoicingDate = Instant.EPOCH, acquisitionDate = Instant.EPOCH, permanentStorageDate = Instant.EPOCH,
            seller = InvoiceMetadata.Seller("9945082500", "Jan Testowy"),
            buyer = InvoiceMetadata.Buyer(InvoiceMetadata.BuyerIdentifier("Nip", "6691034274"), "Testowa Sp. z o.o."),
            netAmount = BigDecimal("233"), grossAmount = BigDecimal("286.59"), vatAmount = BigDecimal("53.59"),
            currency = "PLN", invoicingMode = "Online", invoiceType = "Vat",
            formCode = InvoiceMetadata.FormCode("FA (3)", "1-0E", "FA"), isSelfInvoicing = false, hasAttachment = false, invoiceHash = "x",
        )
        val entry = MirrorEntry(metadata, LocalDate.parse("2026-09-30"))

        assertEquals("6691034274", InvoiceListLogic.toRow(InvoiceCategory.SALES, entry).counterpartyNip)
        assertEquals("Jan Testowy", InvoiceListLogic.toRow(InvoiceCategory.PURCHASE, entry).counterpartyName)
        assertEquals(LocalDate.parse("2026-09-30"), InvoiceListLogic.toRow(InvoiceCategory.SALES, entry).deliveryDate)
    }
}
