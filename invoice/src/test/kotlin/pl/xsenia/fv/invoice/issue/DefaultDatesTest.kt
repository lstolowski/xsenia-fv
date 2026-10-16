package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class DefaultDatesTest {

    @ParameterizedTest
    @CsvSource(
        "2026-10-01, 2026-09-30",
        "2026-10-05, 2026-09-30",
        "2026-10-06, 2026-10-06",
        "2026-10-15, 2026-10-15",
        "2026-10-31, 2026-10-31",
        "2027-01-05, 2026-12-31",
        "2027-01-06, 2027-01-06",
        "2028-03-05, 2028-02-29",
    )
    fun `delivery date defaults to last month end until the 5th`(issue: String, expected: String) {
        assertEquals(LocalDate.parse(expected), DefaultDates.deliveryDate(LocalDate.parse(issue)))
    }
}
