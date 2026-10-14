package pl.xsenia.fv.invoice.currency

import java.math.BigDecimal
import java.time.LocalDate

/**
 * NBP table A average rate (`mid`) of a foreign [currency] in PLN (ADR 0008) — written to `KursWaluty`.
 *
 * @property rate PLN for one unit of [currency], with exactly [SCALE] decimals as published by NBP, e.g. `4.3775`
 * @property tableNumber NBP table number, e.g. `197/A/NBP/2026`
 * @property effectiveDate publication date of the table
 */
data class ExchangeRate(
    val currency: InvoiceCurrency,
    val rate: BigDecimal,
    val tableNumber: String,
    val effectiveDate: LocalDate,
) {
    init {
        require(currency.isForeign) { "PLN has no exchange rate" }
        require(rate.signum() > 0 && rate.scale() == SCALE) { "Exchange rate must be positive with $SCALE decimals: $rate" }
        require(tableNumber.isNotBlank()) { "Missing NBP table number" }
    }

    companion object {
        /** Decimals of NBP table A rates. */
        const val SCALE = 4
    }
}

/**
 * Which day's rate an invoice uses (VAT act art. 31a, ADR 0008): the last NBP table published before the reference day.
 *
 * - Scenario A — issued on or after the tax point (the delivery date): rate from before the delivery date.
 * - Scenario B — issued before the tax point: rate from before the issue day; in KSeF that is the day of sending.
 */
object ExchangeRateDateRule {

    /**
     * @param issueDay the issue date from the form for a working rate (draft, unsent invoice),
     *   or the day of sending for the final rate written to KSeF
     */
    fun referenceDate(deliveryDate: LocalDate, issueDay: LocalDate): LocalDate = minOf(deliveryDate, issueDay)
}
