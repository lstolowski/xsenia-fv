package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.currency.ExchangeRate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class LineAmounts(
    val net: BigDecimal,
    /** Informative only — totals are calculated from the sum of net values per rate (see [InvoiceTotals]). */
    val gross: BigDecimal,
)

/**
 * Net and VAT of all lines with the same rate — reported in `P_13_x` / `P_14_x`.
 *
 * @property vatPln VAT converted to PLN (`P_14_xW`) — only with an exchange rate and for rates with a VAT amount
 */
data class RateTotals(val rate: VatRate, val net: BigDecimal, val vat: BigDecimal, val vatPln: BigDecimal? = null)

/**
 * @property byRate one entry per rate used, in [VatRate] order
 * @property vatPln total VAT in PLN of a foreign currency invoice (sum of [RateTotals.vatPln]); null without an exchange rate
 */
data class InvoiceTotals(
    val net: BigDecimal,
    val vat: BigDecimal,
    val gross: BigDecimal,
    val byRate: List<RateTotals>,
    val vatPln: BigDecimal? = null,
)

/**
 * Amounts of a sales invoice with the VAT rate chosen per line (ADR 0005).
 *
 * VAT is calculated once per rate from the sum of net values of that rate ("od sumy"), as reported in `P_13_x`/`P_14_x`.
 * Line gross values are informative; with several lines their sum may differ from the total by a grosz.
 * Amounts are in the invoice currency; on a foreign currency invoice VAT of each rate is also converted to PLN:
 * VAT × exchange rate, rounded to grosze (ADR 0008, decision Q6).
 */
object InvoiceCalculator {

    const val PAYMENT_DAYS = 21L

    private val HUNDRED = BigDecimal(100)
    private val ZERO = BigDecimal.ZERO.setScale(2)

    fun lineNet(line: DraftLine): BigDecimal = money(line.quantity * line.unitNetPrice)

    fun line(line: DraftLine): LineAmounts {
        val net = lineNet(line)
        return LineAmounts(net, net + vatOf(net, line.vatRate))
    }

    /** @param exchangeRate NBP rate of a foreign currency invoice; null for PLN */
    fun totals(lines: List<DraftLine>, exchangeRate: ExchangeRate? = null): InvoiceTotals {
        val byRate = VatRate.entries.mapNotNull { rate ->
            val rateLines = lines.filter { it.vatRate == rate }
            if (rateLines.isEmpty()) return@mapNotNull null
            val net = rateLines.fold(ZERO) { sum, line -> sum + lineNet(line) }
            val vat = vatOf(net, rate)
            RateTotals(rate, net, vat, vatPln = exchangeRate?.takeIf { rate.hasVatAmount }?.let { money(vat * it.rate) })
        }
        val net = byRate.fold(ZERO) { sum, group -> sum + group.net }
        val vat = byRate.fold(ZERO) { sum, group -> sum + group.vat }
        val vatPln = exchangeRate?.let { byRate.fold(ZERO) { sum, group -> sum + (group.vatPln ?: ZERO) } }
        return InvoiceTotals(net, vat, net + vat, byRate, vatPln)
    }

    fun totals(draft: SalesInvoiceDraft): InvoiceTotals = totals(draft.lines, draft.effectiveExchangeRate)

    /** Payment term: always 21 days from the issue date (decision R5). */
    fun paymentDueDate(issueDate: LocalDate): LocalDate = issueDate.plusDays(PAYMENT_DAYS)

    private fun vatOf(net: BigDecimal, rate: VatRate): BigDecimal = money(net * rate.percent / HUNDRED)

    private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}
