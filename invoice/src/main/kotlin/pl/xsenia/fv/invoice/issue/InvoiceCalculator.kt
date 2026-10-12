package pl.xsenia.fv.invoice.issue

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class LineAmounts(
    val net: BigDecimal,
    /** Informative only — totals are calculated from the sum of net values (see [InvoiceTotals]). */
    val gross: BigDecimal,
)

data class InvoiceTotals(val net: BigDecimal, val vat: BigDecimal, val gross: BigDecimal)

/**
 * Amounts of a sales invoice with the fixed VAT rate of 23% (decision C2).
 *
 * VAT is calculated once from the sum of net values ("od sumy"), as reported in `P_13_1`/`P_14_1`.
 * Line gross values are informative; with several lines their sum may differ from the total by a grosz.
 */
object InvoiceCalculator {

    val VAT_RATE = BigDecimal(23)
    const val VAT_RATE_CODE = "23"
    const val CURRENCY = "PLN"
    const val PAYMENT_DAYS = 21L

    private val HUNDRED = BigDecimal(100)

    fun lineNet(line: DraftLine): BigDecimal = money(line.quantity * line.unitNetPrice)

    fun line(line: DraftLine): LineAmounts {
        val net = lineNet(line)
        return LineAmounts(net, net + vatOf(net))
    }

    fun totals(lines: List<DraftLine>): InvoiceTotals {
        val net = lines.fold(BigDecimal.ZERO.setScale(2)) { sum, line -> sum + lineNet(line) }
        val vat = vatOf(net)
        return InvoiceTotals(net, vat, net + vat)
    }

    /** Payment term: always 21 days from the issue date (decision R5). */
    fun paymentDueDate(issueDate: LocalDate): LocalDate = issueDate.plusDays(PAYMENT_DAYS)

    private fun vatOf(net: BigDecimal): BigDecimal = money(net * VAT_RATE / HUNDRED)

    private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}
