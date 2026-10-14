package pl.xsenia.fv.currency

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.xsenia.fv.exchangerates.ExchangeRateResult
import pl.xsenia.fv.exchangerates.ExchangeRateSource
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeRateServiceTest {

    // NBP tables around 2026-10-13 (Tuesday): every working day, none on 10–11 October (weekend).
    private val source = object : ExchangeRateSource {
        override suspend fun rateOn(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult =
            if (date.dayOfWeek.value >= 6) {
                ExchangeRateResult.NotPublished
            } else {
                ExchangeRateResult.Found(ExchangeRate(currency, BigDecimal("4.3775"), "TEST/A/NBP/2026", date))
            }
    }

    private val service = ExchangeRateService(source)

    // Examples from docs/analiza/04-faktury-w-walucie-obcej.md (section 3).
    @ParameterizedTest(name = "delivery {0}, issued {1} -> table of {2}")
    @CsvSource(
        "2026-09-30, 2026-10-13, 2026-09-29", // A
        "2026-10-12, 2026-10-13, 2026-10-09", // A, weekend skipped
        "2026-10-13, 2026-10-13, 2026-10-12", // A
        "2026-10-20, 2026-10-13, 2026-10-12", // B: issued before the delivery
    )
    fun `invoice rate follows the VAT act rule`(delivery: String, issued: String, table: String) = runBlocking {
        val result = service.rateFor(InvoiceCurrency.EUR, LocalDate.parse(delivery), LocalDate.parse(issued))

        assertEquals(LocalDate.parse(table), (result as ExchangeRateResult.Found).rate.effectiveDate)
    }
}
