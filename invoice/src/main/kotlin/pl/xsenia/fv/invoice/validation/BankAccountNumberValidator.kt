package pl.xsenia.fv.invoice.validation

/** Polish bank account number (NRB): 26 digits validated as an IBAN with the `PL` country code (mod 97). */
object BankAccountNumberValidator {

    private val SEPARATORS = Regex("[\\s-]")

    // "PL" converted to digits for the IBAN check (P = 25, L = 21).
    private const val COUNTRY_DIGITS = "2521"

    /** Returns the number as 26 digits, accepting spaces and the `PL` prefix, or null when invalid. */
    fun normalize(raw: String): String? {
        val candidate = raw.replace(SEPARATORS, "").uppercase().removePrefix("PL")
        return candidate.takeIf(::isValid)
    }

    fun isValid(number: String): Boolean {
        if (number.length != 26 || !number.all(Char::isDigit)) return false
        val rearranged = number.substring(2) + COUNTRY_DIGITS + number.substring(0, 2)
        return rearranged.fold(0) { remainder, digit -> (remainder * 10 + digit.digitToInt()) % 97 } == 1
    }
}
