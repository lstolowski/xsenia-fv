package pl.xsenia.fv.ui.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class FormatsTest {

    // Polish grouping uses a non-breaking space.
    private fun String.normalizeSpaces() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun `formats money in Polish`() {
        assertEquals("1 234,56 zł", Formats.money(BigDecimal("1234.56"), "PLN").normalizeSpaces())
        assertEquals("233,00 zł", Formats.money(BigDecimal("233"), "PLN"))
        assertEquals("-24,60 zł", Formats.money(BigDecimal("-24.60"), "PLN").replace('−', '-'))
        assertEquals("10,00 EUR", Formats.money(BigDecimal("10"), "EUR"))
    }

    @Test
    fun `formats quantity without trailing zeros`() {
        assertEquals("2,5", Formats.quantity(BigDecimal("2.500")))
        assertEquals("1", Formats.quantity(BigDecimal("1.00")))
        assertEquals("1 000", Formats.quantity(BigDecimal("1000")).normalizeSpaces())
    }

    @Test
    fun `groups bank account numbers`() {
        assertEquals("85 2160 9758 3216 8502 4600 2152", Formats.bankAccount("85216097583216850246002152"))
        assertEquals("DE89370400440532013000", Formats.bankAccount("DE89370400440532013000"))
    }

    @Test
    fun `formats dates and months`() {
        assertEquals("30.09.2026", Formats.date(LocalDate.parse("2026-09-30")))
        assertEquals("Wrzesień 2026", Formats.month(YearMonth.of(2026, 9)))
    }
}
