package pl.xsenia.fv.ui.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PolishPluralTest {

    @ParameterizedTest
    @CsvSource(
        "0, MANY", "1, ONE", "2, FEW", "4, FEW", "5, MANY", "11, MANY", "12, MANY", "14, MANY",
        "21, MANY", "22, FEW", "24, FEW", "25, MANY", "101, MANY", "102, FEW", "112, MANY",
    )
    fun `chooses Polish plural form`(count: Long, expected: PolishPlural) {
        assertEquals(expected, PolishPlural.of(count))
    }
}
