package pl.xsenia.fv.exchangerates

import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.math.BigDecimal
import java.time.LocalDate

/** Answers from [answers] (missing day — not published); [asked] records every call. */
class FakeExchangeRateSource(private val answers: Map<LocalDate, ExchangeRateResult> = emptyMap()) : ExchangeRateSource {
    val asked = mutableListOf<LocalDate>()

    override suspend fun rateOn(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult {
        asked += date
        return answers[date] ?: ExchangeRateResult.NotPublished
    }

    companion object {
        fun found(date: String, rate: String = "4.3775", table: String = "197/A/NBP/2026", currency: InvoiceCurrency = InvoiceCurrency.EUR) =
            LocalDate.parse(date) to ExchangeRateResult.Found(ExchangeRate(currency, BigDecimal(rate), table, LocalDate.parse(date)))
    }
}
