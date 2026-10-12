package pl.xsenia.fv.invoice.issue

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class LineAmounts(
    val net: BigDecimal,
    /** Informative only — totals are calculated from the sum of net values per rate (see [InvoiceTotals]). */
    val gross: BigDecimal,
)

/** Net and VAT of all lines with the same rate — reported in `P_13_x` / `P_14_x`. */
data class RateTotals(val rate: VatRate, val net: BigDecimal, val vat: BigDecimal)

/** @property byRate one entry per rate used, in [VatRate] order. */
data class InvoiceTotals(val net: BigDecimal, val vat: BigDecimal, val gross: BigDecimal, val byRate: List<RateTotals>)

/**
 * Amounts of a sales invoice with the VAT rate chosen per line (ADR 0005).
 *
 * VAT is calculated once per rate from the sum of net values of that rate ("od sumy"), as reported in `P_13_x`/`P_14_x`.
 * Line gross values are informative; with several lines their sum may differ from the total by a grosz.
 */
object InvoiceCalculator {

    const val CURRENCY = "PLN"
    const val PAYMENT_DAYS = 21L

    private val HUNDRED = BigDecimal(100)
    private val ZERO = BigDecimal.ZERO.setScale(2)

    fun lineNet(line: DraftLine): BigDecimal = money(line.quantity * line.unitNetPrice)

    fun line(line: DraftLine): LineAmounts {
        val net = lineNet(line)
        return LineAmounts(net, net + vatOf(net, line.vatRate))
    }

    fun totals(lines: List<DraftLine>): InvoiceTotals {
        val byRate = VatRate.entries.mapNotNull { rate ->
            val rateLines = lines.filter { it.vatRate == rate }
            if (rateLines.isEmpty()) return@mapNotNull null
            val net = rateLines.fold(ZERO) { sum, line -> sum + lineNet(line) }
            RateTotals(rate, net, vatOf(net, rate))
        }
        val net = byRate.fold(ZERO) { sum, group -> sum + group.net }
        val vat = byRate.fold(ZERO) { sum, group -> sum + group.vat }
        return InvoiceTotals(net, vat, net + vat, byRate)
    }

    /** Payment term: always 21 days from the issue date (decision R5). */
    fun paymentDueDate(issueDate: LocalDate): LocalDate = issueDate.plusDays(PAYMENT_DAYS)

    private fun vatOf(net: BigDecimal, rate: VatRate): BigDecimal = money(net * rate.percent / HUNDRED)

    private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}
