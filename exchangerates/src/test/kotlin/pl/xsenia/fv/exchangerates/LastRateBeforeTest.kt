package pl.xsenia.fv.exchangerates

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.exchangerates.ExchangeRateResult.Failure.Reason
import pl.xsenia.fv.exchangerates.FakeExchangeRateSource.Companion.found
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.time.LocalDate

class LastRateBeforeTest {

    private fun dates(vararg values: String) = values.map(LocalDate::parse)

    @Test
    fun `skips the weekend`() = runTest {
        val friday = found("2026-10-09")
        val source = FakeExchangeRateSource(mapOf(friday))

        assertEquals(friday.second, source.lastRateBefore(InvoiceCurrency.EUR, LocalDate.parse("2026-10-12")))
        assertEquals(dates("2026-10-11", "2026-10-10", "2026-10-09"), source.asked)
    }

    @Test
    fun `takes the previous working day, never the day itself`() = runTest {
        val monday = found("2026-10-12", "4.3889", "198/A/NBP/2026")
        val friday = found("2026-10-09")
        val source = FakeExchangeRateSource(mapOf(monday, friday))

        assertEquals(monday.second, source.lastRateBefore(InvoiceCurrency.EUR, LocalDate.parse("2026-10-13")))
        assertEquals(dates("2026-10-12"), source.asked)
    }

    @Test
    fun `skips Christmas holidays`() = runTest {
        // 2026: 24–26 December are holidays (Thu–Sat), 27 December is a Sunday.
        val beforeChristmas = found("2026-12-23", "4.2100", "248/A/NBP/2026")
        val source = FakeExchangeRateSource(mapOf(beforeChristmas))

        assertEquals(beforeChristmas.second, source.lastRateBefore(InvoiceCurrency.EUR, LocalDate.parse("2026-12-28")))
        assertEquals(dates("2026-12-27", "2026-12-26", "2026-12-25", "2026-12-24", "2026-12-23"), source.asked)
    }

    @Test
    fun `gives up after ten days without a table`() = runTest {
        val source = FakeExchangeRateSource()

        assertEquals(ExchangeRateResult.NotPublished, source.lastRateBefore(InvoiceCurrency.EUR, LocalDate.parse("2026-10-12")))
        assertEquals(MAX_DAYS_BACK, source.asked.size)
        assertEquals(LocalDate.parse("2026-10-02"), source.asked.last())
    }

    @Test
    fun `stops at the first failure`() = runTest {
        val failure = ExchangeRateResult.Failure(Reason.NETWORK)
        val source = FakeExchangeRateSource(mapOf(LocalDate.parse("2026-10-11") to failure, found("2026-10-09")))

        assertEquals(failure, source.lastRateBefore(InvoiceCurrency.EUR, LocalDate.parse("2026-10-12")))
        assertEquals(dates("2026-10-11"), source.asked)
    }
}
