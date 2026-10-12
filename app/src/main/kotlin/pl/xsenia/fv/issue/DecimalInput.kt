package pl.xsenia.fv.issue

import java.math.BigDecimal

/** Parses numbers typed by the user: `1 234,50`, `1234.5`, `2,5` (spaces and non-breaking spaces ignored). */
object DecimalInput {

    private val SPACES = Regex("[\\s  ]")
    private val NUMBER = Regex("^-?\\d+([.,]\\d+)?$")

    fun parse(text: String): BigDecimal? {
        val compact = text.replace(SPACES, "")
        if (!NUMBER.matches(compact)) return null
        return BigDecimal(compact.replace(',', '.'))
    }
}
