package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class DecimalInputTest {

    @ParameterizedTest
    @CsvSource(
        "'1 234,50', 1234.50",
        "1234.5, 1234.5",
        "'2,5', 2.5",
        "233, 233",
        "' 0,10 ', 0.10",
        "'1 000', 1000",
    )
    fun `parses Polish and dot decimals`(text: String, expected: String) {
        assertEquals(BigDecimal(expected), DecimalInput.parse(text))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "abc", "1,2,3", "1.2.3", ",5", "5,", "1e3"])
    fun `rejects invalid input`(text: String) {
        assertNull(DecimalInput.parse(text))
    }
}
