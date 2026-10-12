package pl.xsenia.fv.ui.format

/**
 * Polish plural forms (CLDR rules for integers): 1 → one, 2–4 / 22–24 / … (except 12–14) → few, otherwise → many.
 * Android picks plural forms by the resource locale, which would be English for the default `values/` folder,
 * so the choice is made here.
 */
enum class PolishPlural {
    ONE,
    FEW,
    MANY,
    ;

    companion object {
        fun of(count: Long): PolishPlural {
            val lastDigit = count % 10
            val lastTwoDigits = count % 100
            return when {
                count == 1L -> ONE
                lastDigit in 2..4 && lastTwoDigits !in 12..14 -> FEW
                else -> MANY
            }
        }
    }
}
