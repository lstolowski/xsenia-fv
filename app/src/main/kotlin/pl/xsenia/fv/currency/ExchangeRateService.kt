package pl.xsenia.fv.currency

import android.content.res.Resources
import pl.xsenia.fv.R
import pl.xsenia.fv.exchangerates.ExchangeRateResult
import pl.xsenia.fv.exchangerates.ExchangeRateSource
import pl.xsenia.fv.exchangerates.lastRateBefore
import pl.xsenia.fv.invoice.currency.ExchangeRateDateRule
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.time.LocalDate

/**
 * The single access point to NBP exchange rates in the app (ADR 0008) — the invoice form, preview and sending use it.
 * [source] is the NBP client behind the cache of past days.
 */
class ExchangeRateService(private val source: ExchangeRateSource) {

    /** Rate published on [date], e.g. EUR of 2026-10-12. */
    suspend fun rateOn(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult = source.rateOn(currency, date)

    /** Rate from the last working day before [date]. */
    suspend fun lastRateBefore(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult = source.lastRateBefore(currency, date)

    /**
     * Rate of an invoice in a foreign [currency] ([ExchangeRateDateRule]).
     *
     * @param issueDay the issue date from the form (working rate) or the day of sending to KSeF (final rate)
     */
    suspend fun rateFor(currency: InvoiceCurrency, deliveryDate: LocalDate, issueDay: LocalDate): ExchangeRateResult =
        lastRateBefore(currency, ExchangeRateDateRule.referenceDate(deliveryDate, issueDay))
}

/** Polish message for a result without a rate; null for [ExchangeRateResult.Found]. */
fun ExchangeRateResult.problemMessage(resources: Resources): String? = when (this) {
    is ExchangeRateResult.Found -> null
    ExchangeRateResult.NotPublished -> resources.getString(R.string.exchange_rate_not_published)
    is ExchangeRateResult.Failure -> when (reason) {
        ExchangeRateResult.Failure.Reason.NETWORK -> resources.getString(R.string.exchange_rate_network)
        ExchangeRateResult.Failure.Reason.SERVER -> resources.getString(R.string.exchange_rate_server)
    }
}
