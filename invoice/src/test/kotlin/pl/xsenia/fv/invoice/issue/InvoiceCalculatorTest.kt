package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.issue.TestDrafts.line
import java.math.BigDecimal
import java.time.LocalDate

class InvoiceCalculatorTest {

    @Test
    fun `single line at 23 percent`() {
        assertEquals(InvoiceTotals(BigDecimal("233.00"), BigDecimal("53.59"), BigDecimal("286.59")), InvoiceCalculator.totals(listOf(line())))
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

        assertEquals(InvoiceTotals(BigDecimal("0.30"), BigDecimal("0.07"), BigDecimal("0.37")), totals)
        // Each line shows 0.12 gross; their sum (0.36) differs from the total by one grosz — accepted (decision 2026-10-12).
        assertEquals(BigDecimal("0.36"), lines.map { InvoiceCalculator.line(it).gross }.reduce(BigDecimal::add))
    }

    @Test
    fun `payment is due 21 days after issue`() {
        assertEquals(LocalDate.parse("2026-10-22"), InvoiceCalculator.paymentDueDate(LocalDate.parse("2026-10-01")))
    }
}
