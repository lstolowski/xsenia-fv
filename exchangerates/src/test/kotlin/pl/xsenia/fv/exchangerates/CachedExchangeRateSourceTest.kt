package pl.xsenia.fv.exchangerates

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.exchangerates.ExchangeRateResult.Failure.Reason
import pl.xsenia.fv.exchangerates.FakeExchangeRateSource.Companion.found
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.io.File
import java.time.LocalDate

class CachedExchangeRateSourceTest {

    @TempDir
    lateinit var directory: File

    private val file get() = File(directory, "exchange-rates.json")
    private val today = LocalDate.parse("2026-10-13")
    private val friday = found("2026-10-09")
    private val sunday = LocalDate.parse("2026-10-11")

    private fun TestScope.cached(delegate: ExchangeRateSource) =
        CachedExchangeRateSource(delegate, file, today = { today }, ioDispatcher = StandardTestDispatcher(testScheduler))

    @Test
    fun `remembers past rates and days without a table`() = runTest {
        val delegate = FakeExchangeRateSource(mapOf(friday))
        val source = cached(delegate)

        repeat(2) {
            assertEquals(friday.second, source.rateOn(InvoiceCurrency.EUR, friday.first))
            assertEquals(ExchangeRateResult.NotPublished, source.rateOn(InvoiceCurrency.EUR, sunday))
        }
        assertEquals(listOf(friday.first, sunday), delegate.asked)
    }

    @Test
    fun `keeps the cache in the file`() = runTest {
        cached(FakeExchangeRateSource(mapOf(friday))).apply {
            rateOn(InvoiceCurrency.EUR, friday.first)
            rateOn(InvoiceCurrency.EUR, sunday)
        }
        val offline = FakeExchangeRateSource(mapOf(friday.first to ExchangeRateResult.Failure(Reason.NETWORK)))
        val reopened = cached(offline)

        assertEquals(friday.second, reopened.rateOn(InvoiceCurrency.EUR, friday.first))
        assertEquals(ExchangeRateResult.NotPublished, reopened.rateOn(InvoiceCurrency.EUR, sunday))
        assertEquals(emptyList<LocalDate>(), offline.asked)
    }

    @Test
    fun `currencies are cached separately`() = runTest {
        val delegate = FakeExchangeRateSource(mapOf(friday))
        val source = cached(delegate)

        source.rateOn(InvoiceCurrency.EUR, friday.first)
        source.rateOn(InvoiceCurrency.USD, friday.first)

        assertEquals(listOf(friday.first, friday.first), delegate.asked)
    }

    @Test
    fun `today is always asked again - the table appears around noon`() = runTest {
        val delegate = FakeExchangeRateSource()
        val source = cached(delegate)

        repeat(2) { source.rateOn(InvoiceCurrency.EUR, today) }

        assertEquals(listOf(today, today), delegate.asked)
        assertFalse(file.exists())
    }

    @Test
    fun `failures are not remembered`() = runTest {
        val failure = ExchangeRateResult.Failure(Reason.SERVER)
        val delegate = FakeExchangeRateSource(mapOf(friday.first to failure))
        val source = cached(delegate)

        repeat(2) { assertEquals(failure, source.rateOn(InvoiceCurrency.EUR, friday.first)) }

        assertEquals(2, delegate.asked.size)
    }

    @Test
    fun `corrupted file or entry means asking again`() = runTest {
        file.writeText("""{"days": {"EUR/2026-10-09": {"table": "197/A/NBP/2026", "rate": "abc"}}}""")
        val delegate = FakeExchangeRateSource(mapOf(friday))
        assertEquals(friday.second, cached(delegate).rateOn(InvoiceCurrency.EUR, friday.first))

        file.writeText("not json")
        assertEquals(friday.second, cached(delegate).rateOn(InvoiceCurrency.EUR, friday.first))

        assertEquals(2, delegate.asked.size)
    }
}
