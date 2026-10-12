package pl.xsenia.fv.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Polish formatting of amounts and dates shown in the UI. */
object Formats {

    private val POLISH: Locale = Locale.forLanguageTag("pl-PL")
    private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val MONTH = DateTimeFormatter.ofPattern("LLLL yyyy", POLISH)

    /** `1 234,56 zł` for PLN, `1 234,56 EUR` for other currencies. */
    fun money(amount: BigDecimal, currency: String): String {
        val number = NumberFormat.getNumberInstance(POLISH).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)
        return "$number ${if (currency == "PLN") "zł" else currency}"
    }

    fun date(date: LocalDate): String = DATE.format(date)

    fun dateTime(instant: Instant): String = DATE_TIME.format(instant.atZone(ZoneId.systemDefault()))

    /** `Wrzesień 2026`. */
    fun month(month: YearMonth): String = MONTH.format(month).replaceFirstChar { it.titlecase(POLISH) }
}
