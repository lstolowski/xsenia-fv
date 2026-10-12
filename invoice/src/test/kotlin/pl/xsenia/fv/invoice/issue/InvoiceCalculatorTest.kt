package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.issue.TestDrafts.line
import java.math.BigDecimal
import java.time.LocalDate

class InvoiceCalculatorTest {

    @Test
    fun `single line at 23 percent`() {
        assertEquals(
            InvoiceTotals(
                BigDecimal("233.00"),
                BigDecimal("53.59"),
                BigDecimal("286.59"),
                listOf(RateTotals(VatRate.RATE_23, BigDecimal("233.00"), BigDecimal("53.59"))),
            ),
            InvoiceCalculator.totals(listOf(line())),
        )
        assertEquals(LineAmounts(BigDecimal("233.00"), BigDecimal("286.59")), InvoiceCalculator.line(line()))
    }

    @Test
    fun `line net is quantity times unit price rounded half up`() {
        assertEquals(BigDecimal("100.00"), InvoiceCalculator.lineNet(line(quantity = "2.5", price = "40")))
        assertEquals(BigDecimal("0.13"), InvoiceCalculator.lineNet(line(quantity = "0.5", price = "0.25")))
        assertEquals(BigDecimal("33.33"), InvoiceCalculator.lineNet(line(quantity = "1", price = "33.3333")))
    }

    @Test
    fun `VAT is calculated from the sum of net values, line gross values are informative`() {
        val lines = List(3) { line(price = "0.10") }

        val totals = InvoiceCalculator.totals(lines)

        assertEquals(BigDecimal("0.30"), totals.net)
        assertEquals(BigDecimal("0.07"), totals.vat)
        assertEquals(BigDecimal("0.37"), totals.gross)
        // Each line shows 0.12 gross; their sum (0.36) differs from the total by one grosz — accepted (decision 2026-10-12).
        assertEquals(BigDecimal("0.36"), lines.map { InvoiceCalculator.line(it).gross }.reduce(BigDecimal::add))
    }

    @Test
    fun `VAT is calculated per rate from the net sum of that rate`() {
        val lines = listOf(
            line(price = "0.10", rate = VatRate.RATE_8),
            line(price = "100", rate = VatRate.RATE_23),
            line(price = "0.10", rate = VatRate.RATE_8),
            line(price = "50", rate = VatRate.NOT_TAXABLE),
            line(price = "10.05", rate = VatRate.RATE_5),
            line(price = "20", rate = VatRate.RATE_0),
        )

        val totals = InvoiceCalculator.totals(lines)

        assertEquals(
            listOf(
                RateTotals(VatRate.RATE_23, BigDecimal("100.00"), BigDecimal("23.00")),
                RateTotals(VatRate.RATE_8, BigDecimal("0.20"), BigDecimal("0.02")),
                RateTotals(VatRate.RATE_5, BigDecimal("10.05"), BigDecimal("0.50")),
                RateTotals(VatRate.RATE_0, BigDecimal("20.00"), BigDecimal("0.00")),
                RateTotals(VatRate.NOT_TAXABLE, BigDecimal("50.00"), BigDecimal("0.00")),
            ),
            totals.byRate,
        )
        assertEquals(BigDecimal("180.25"), totals.net)
        assertEquals(BigDecimal("23.52"), totals.vat)
        assertEquals(BigDecimal("203.77"), totals.gross)
    }

    @Test
    fun `line gross uses the line rate`() {
        assertEquals(LineAmounts(BigDecimal("100.00"), BigDecimal("108.00")), InvoiceCalculator.line(line(price = "100", rate = VatRate.RATE_8)))
        assertEquals(LineAmounts(BigDecimal("100.00"), BigDecimal("100.00")), InvoiceCalculator.line(line(price = "100", rate = VatRate.RATE_0)))
        assertEquals(LineAmounts(BigDecimal("100.00"), BigDecimal("100.00")), InvoiceCalculator.line(line(price = "100", rate = VatRate.NOT_TAXABLE)))
    }

    @Test
    fun `payment is due 21 days after issue`() {
        assertEquals(LocalDate.parse("2026-10-22"), InvoiceCalculator.paymentDueDate(LocalDate.parse("2026-10-01")))
    }
}
