package pl.xsenia.fv.invoice.currency

/**
 * Currency of an issued invoice (`KodWaluty`, ADR 0008). Foreign currencies need an NBP table A exchange rate;
 * adding a currency means adding an entry here (it must be published in table A).
 */
enum class InvoiceCurrency {
    PLN,
    EUR,
    USD,
    GBP,
    ;

    /** ISO 4217 code, as in `KodWaluty` and the NBP API. */
    val code: String get() = name

    val isForeign: Boolean get() = this != PLN

    companion object {
        val DEFAULT = PLN

        fun fromCode(code: String): InvoiceCurrency? = entries.firstOrNull { it.code == code.trim().uppercase() }
    }
}
