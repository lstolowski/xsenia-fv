package pl.xsenia.fv.exchangerates

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.math.BigDecimal
import java.time.LocalDate

/** The real NBP API with past tables (they never change) — checks that the address and response format still match. */
class NbpClientIntegrationTest {

    private val client = NbpClient(NbpClient.createHttpClient())

    @Test
    fun `USD rate of 2016-04-04`() = runBlocking {
        val expected = ExchangeRate(InvoiceCurrency.USD, BigDecimal("3.7254"), "064/A/NBP/2016", LocalDate.parse("2016-04-04"))

        assertEquals(ExchangeRateResult.Found(expected), client.rateOn(InvoiceCurrency.USD, LocalDate.parse("2016-04-04")))
    }

    @Test
    fun `Sunday has no table`() = runBlocking {
        assertEquals(ExchangeRateResult.NotPublished, client.rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-11")))
    }

    @Test
    fun `EUR rate before Monday 2026-10-12 is Friday's`() = runBlocking {
        val expected = ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3775"), "197/A/NBP/2026", LocalDate.parse("2026-10-09"))

        assertEquals(ExchangeRateResult.Found(expected), client.lastRateBefore(InvoiceCurrency.EUR, LocalDate.parse("2026-10-12")))
    }
}
