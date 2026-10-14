package pl.xsenia.fv.invoice.profile

import java.util.Locale

/** Countries of counterparties (ADR 0009): ISO 3166 codes as `KodKraju` in FA(3), Polish names. */
object Countries {

    const val POLAND = "PL"

    /** EU member states by ISO code (Greece is `GR` here, `EL` only as the VAT prefix). */
    val EU: Set<String> = setOf(
        "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU",
        "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK",
    )

    private val POLISH: Locale = Locale.forLanguageTag("pl-PL")

    /** All ISO country codes, sorted by Polish name. */
    val ALL: List<String> by lazy { Locale.getISOCountries().sortedBy { name(it).lowercase(POLISH) } }

    fun isKnown(code: String): Boolean = code in Locale.getISOCountries()

    /** Another EU country than Poland — the buyer is identified by a VAT UE number. */
    fun isOtherEu(code: String): Boolean = code != POLAND && code in EU

    /** Prefix of a VAT UE number (`KodUE`): the country code, except Greece (`EL`). */
    fun vatPrefix(code: String): String = if (code == "GR") "EL" else code

    /** Polish name, e.g. „Niemcy”; the code itself for an unknown one. */
    fun name(code: String): String = Locale.Builder().setRegion(code).build().getDisplayCountry(POLISH).ifEmpty { code }
}
