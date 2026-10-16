package pl.xsenia.fv.invoice.validation

/** Polish tax identification number (NIP): 10 digits, the last one being a weighted mod 11 checksum. */
object NipValidator {

    private val WEIGHTS = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
    private val SEPARATORS = Regex("[\\s-]")

    /** Returns the NIP as 10 digits, accepting separators and the `PL` prefix, or null when invalid. */
    fun normalize(raw: String): String? {
        val candidate = raw.replace(SEPARATORS, "").removePrefix("PL").removePrefix("pl")
        return candidate.takeIf(::isValid)
    }

    fun isValid(nip: String): Boolean {
        if (nip.length != 10 || !nip.isAsciiDigits()) return false
        val checksum = WEIGHTS.indices.sumOf { WEIGHTS[it] * nip[it].digitToInt() } % 11
        return checksum != 10 && checksum == nip[9].digitToInt()
    }
}
