package pl.xsenia.fv.invoice.validation

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class BankAccountNumberValidatorTest {

    @ParameterizedTest
    @CsvSource(
        "PL71101075753723914707418989, 71101075753723914707418989",
        "PL85216097583216850246002152, 85216097583216850246002152",
        "22161029092633573189646018, 22161029092633573189646018",
        "'PL55 1090 0408 1797 9929 7325 3811', 55109004081797992973253811",
        "'pl55-1090-0408-1797-9929-7325-3811', 55109004081797992973253811",
    )
    fun `accepts and normalizes test account numbers`(raw: String, expected: String) {
        assertEquals(expected, BankAccountNumberValidator.normalize(raw))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "85216097583216850246002153", // changed last digit
            "58216097583216850246002152", // swapped check digits
            "8521609758321685024600215", // 25 digits
            "852160975832168502460021520", // 27 digits
            "8521609758321685024600215X",
            "DE85216097583216850246002152",
        ],
    )
    fun `rejects invalid account numbers`(raw: String) {
        assertNull(BankAccountNumberValidator.normalize(raw))
        assertFalse(BankAccountNumberValidator.isValid(raw))
    }

    @Test
    fun `rejects digits outside ASCII even when the checksum is right`() {
        val valid = "85216097583216850246002152"
        val arabicIndic = valid.map { '\u0660' + (it - '0') }.joinToString("")
        val fullWidth = valid.map { '\uFF10' + (it - '0') }.joinToString("")

        assertTrue(BankAccountNumberValidator.isValid(valid))
        assertFalse(BankAccountNumberValidator.isValid(arabicIndic))
        assertFalse(BankAccountNumberValidator.isValid(fullWidth))
        assertNull(BankAccountNumberValidator.normalize(fullWidth))
    }
}
