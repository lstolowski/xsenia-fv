package pl.xsenia.fv.invoice.currency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeRateTest {

    private val day = LocalDate.parse("2026-10-09")

    @Test
    fun `accepts a table A rate with four decimals`() {
        val rate = ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3775"), "197/A/NBP/2026", day)
        assertEquals("4.3775", rate.rate.toPlainString())
    }

    @Test
    fun `rejects PLN, other scales and non-positive rates`() {
        assertThrows<IllegalArgumentException> { ExchangeRate(InvoiceCurrency.PLN, BigDecimal("1.0000"), "197/A/NBP/2026", day) }
        assertThrows<IllegalArgumentException> { ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.377500"), "197/A/NBP/2026", day) }
        assertThrows<IllegalArgumentException> { ExchangeRate(InvoiceCurrency.EUR, BigDecimal("0.0000"), "197/A/NBP/2026", day) }
        assertThrows<IllegalArgumentException> { ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3775"), " ", day) }
    }

    @Test
    fun `currency codes`() {
        assertEquals(InvoiceCurrency.EUR, InvoiceCurrency.fromCode(" eur "))
        assertNull(InvoiceCurrency.fromCode("CHF"))
        assertEquals(listOf("PLN", "EUR", "USD", "GBP"), InvoiceCurrency.entries.map { it.code })
        assertEquals(InvoiceCurrency.PLN, InvoiceCurrency.DEFAULT)
    }

    // Examples from docs/analiza/04-faktury-w-walucie-obcej.md (section 3).
    @ParameterizedTest(name = "delivery {0}, issued {1} -> {2}")
    @CsvSource(
        "2026-09-30, 2026-10-13, 2026-09-30", // A: issued after the delivery
        "2026-10-12, 2026-10-13, 2026-10-12", // A
        "2026-10-13, 2026-10-13, 2026-10-13", // A: issued on the delivery day
        "2026-10-20, 2026-10-13, 2026-10-13", // B: issued before the delivery
    )
    fun `reference date is the earlier of delivery date and issue day`(delivery: String, issued: String, expected: String) {
        assertEquals(LocalDate.parse(expected), ExchangeRateDateRule.referenceDate(LocalDate.parse(delivery), LocalDate.parse(issued)))
    }
}
