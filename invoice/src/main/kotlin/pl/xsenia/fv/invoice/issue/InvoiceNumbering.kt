package pl.xsenia.fv.invoice.issue

import java.time.LocalDate
import java.time.YearMonth

/**
 * Invoice numbers `NN/MM/RRRR` (decision C6): NN — next number within the month, MM/RRRR — month of the
 * delivery date (D2), zero-padded (R9), e.g. `01/09/2026` for a delivery on 2026-09-30 invoiced on 2026-10-01.
 */
object InvoiceNumbering {

    private val NUMBER = Regex("^(\\d+)/(\\d{2})/(\\d{4})$")

    /**
     * Next number for the month of [deliveryDate]: highest NN among [existingNumbers] ending with `/MM/RRRR`, plus one.
     * Computed from the numbers themselves (not delivery dates), so a wrongly numbered invoice neither shifts
     * the sequence of another month nor causes a duplicate.
     */
    fun suggest(deliveryDate: LocalDate, existingNumbers: Collection<String>): String {
        val month = YearMonth.from(deliveryDate)
        val highest = existingNumbers.mapNotNull { parse(it) }
            .filter { (_, numberMonth) -> numberMonth == month }
            .maxOfOrNull { (sequence, _) -> sequence } ?: 0
        return format(highest + 1, month)
    }

    fun format(sequence: Int, month: YearMonth): String =
        "%02d/%02d/%04d".format(sequence, month.monthValue, month.year)

    fun isDuplicate(number: String, existingNumbers: Collection<String>): Boolean =
        existingNumbers.any { it.trim() == number.trim() }

    /** True when the number has the `NN/MM/RRRR` format but its month differs from the delivery month. */
    fun monthMismatch(number: String, deliveryDate: LocalDate): Boolean {
        val (_, month) = parse(number) ?: return false
        return month != YearMonth.from(deliveryDate)
    }

    private fun parse(number: String): Pair<Int, YearMonth>? {
        val match = NUMBER.matchEntire(number.trim()) ?: return null
        val (sequence, month, year) = match.destructured
        val monthValue = month.toInt()
        if (monthValue !in 1..12) return null
        return sequence.toInt() to YearMonth.of(year.toInt(), monthValue)
    }
}
