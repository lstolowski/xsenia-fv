package pl.xsenia.fv.invoice.validation

/**
 * True when every character is an ASCII digit `0`..`9`. Unlike `Char.isDigit`, which accepts any Unicode decimal digit
 * (Arabic-Indic, full-width, …): such digits can pass a checksum but are not valid in NIP / NRB of FA(3) and KSeF.
 */
fun CharSequence.isAsciiDigits(): Boolean = all { it in '0'..'9' }
