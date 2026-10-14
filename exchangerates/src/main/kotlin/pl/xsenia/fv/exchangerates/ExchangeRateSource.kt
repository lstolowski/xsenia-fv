package pl.xsenia.fv.exchangerates

import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.time.LocalDate

sealed interface ExchangeRateResult {
    data class Found(val rate: ExchangeRate) : ExchangeRateResult

    /** No NBP table for the day (weekend, holiday, today before publication around noon). */
    data object NotPublished : ExchangeRateResult

    data class Failure(val reason: Reason) : ExchangeRateResult {
        enum class Reason {
            /** No connection or timeout. */
            NETWORK,

            /** Error status or an unexpected response. */
            SERVER,
        }
    }
}

/** NBP table A average rates of foreign currencies (ADR 0008). */
interface ExchangeRateSource {

    /** Rate published on [date]; [currency] must be foreign. */
    suspend fun rateOn(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult
}

/**
 * Rate from the last working day before [date] (VAT act art. 31a): [ExchangeRateSource.rateOn] for `date − 1`, `date − 2`, …
 * Days without a table are skipped; the first failure is returned as is. Gaps between tables are at most a few days
 * (holidays), so after [maxDaysBack] days without a table the result is [ExchangeRateResult.NotPublished].
 */
suspend fun ExchangeRateSource.lastRateBefore(currency: InvoiceCurrency, date: LocalDate, maxDaysBack: Int = MAX_DAYS_BACK): ExchangeRateResult {
    for (daysBack in 1..maxDaysBack) {
        val result = rateOn(currency, date.minusDays(daysBack.toLong()))
        if (result != ExchangeRateResult.NotPublished) return result
    }
    return ExchangeRateResult.NotPublished
}

const val MAX_DAYS_BACK = 10
