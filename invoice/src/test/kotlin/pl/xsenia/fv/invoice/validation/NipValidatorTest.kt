package pl.xsenia.fv.invoice.validation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class NipValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = ["6691034274", "1996845518", "9945082500", "9579102904", "6973432790", "4824062973", "3563998415", "8771617926"])
    fun `accepts test NIPs`(nip: String) {
        assertTrue(NipValidator.isValid(nip))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "6691034275", // wrong check digit
            "1234567890", // wrong check digit
            "1000000160", // weighted sum mod 11 = 10 is never valid
            "669103427", // too short
            "66910342740", // too long
            "66910342a4", // not a digit
            "",
        ],
    )
    fun `rejects invalid NIPs`(nip: String) {
        assertFalse(NipValidator.isValid(nip))
    }

    @ParameterizedTest
    @ValueSource(strings = ["669-103-42-74", "669 103 42 74", "PL6691034274", " 6691034274 "])
    fun `normalizes separators and PL prefix`(raw: String) {
        assertEquals("6691034274", NipValidator.normalize(raw))
    }

    @ParameterizedTest
    @ValueSource(strings = ["PL6691034275", "abc"])
    fun `normalize returns null for invalid NIP`(raw: String) {
        assertNull(NipValidator.normalize(raw))
    }

    @Test
    fun `rejects digits outside ASCII even when the checksum is right`() {
        val valid = "6691034274"
        // The same digits written as Arabic-Indic (U+0660..) and as full-width (U+FF10..) characters.
        val arabicIndic = valid.map { '\u0660' + (it - '0') }.joinToString("")
        val fullWidth = valid.map { '\uFF10' + (it - '0') }.joinToString("")

        assertTrue(NipValidator.isValid(valid))
        assertFalse(NipValidator.isValid(arabicIndic))
        assertFalse(NipValidator.isValid(fullWidth))
        assertNull(NipValidator.normalize(arabicIndic))
    }
}
