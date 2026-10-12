package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InvoiceNumberingTest {

    private fun date(value: String) = LocalDate.parse(value)

    @Test
    fun `first invoice of a month`() {
        assertEquals("01/09/2026", InvoiceNumbering.suggest(date("2026-09-30"), emptyList()))
    }

    @Test
    fun `next number within the delivery month, zero padded`() {
        val existing = listOf("01/09/2026", "02/09/2026", "07/08/2026", "09/09/2025")

        assertEquals("03/09/2026", InvoiceNumbering.suggest(date("2026-09-15"), existing))
        assertEquals("08/08/2026", InvoiceNumbering.suggest(date("2026-08-31"), existing))
        assertEquals("100/09/2026", InvoiceNumbering.suggest(date("2026-09-15"), listOf("99/09/2026")))
    }

    @Test
    fun `wrongly numbered invoice from KSeF TEST does not shift the sequence`() {
        // Invoice 01/10/2026 was issued with delivery date 2026-09-30 (should have been 01/09/2026).
        val existing = listOf("01/10/2026")

        assertEquals("01/09/2026", InvoiceNumbering.suggest(date("2026-09-30"), existing))
        assertEquals("02/10/2026", InvoiceNumbering.suggest(date("2026-10-20"), existing))
        assertTrue(InvoiceNumbering.monthMismatch("01/10/2026", date("2026-09-30")))
    }

    @Test
    fun `ignores numbers in other formats`() {
        assertEquals("01/09/2026", InvoiceNumbering.suggest(date("2026-09-30"), listOf("FV/09/2026/1", "1/9/2026", "05/13/2026")))
    }

    @Test
    fun `detects duplicates and month mismatch`() {
        assertTrue(InvoiceNumbering.isDuplicate(" 01/09/2026 ", listOf("01/09/2026")))
        assertFalse(InvoiceNumbering.isDuplicate("02/09/2026", listOf("01/09/2026")))
        assertFalse(InvoiceNumbering.monthMismatch("01/09/2026", date("2026-09-30")))
        assertFalse(InvoiceNumbering.monthMismatch("FV-TEST", date("2026-09-30")))
    }
}
