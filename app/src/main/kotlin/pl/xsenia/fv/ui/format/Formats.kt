package pl.xsenia.fv.ui.format

import pl.xsenia.fv.invoice.validation.isAsciiDigits
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

    /** `1 234,56` — two decimals, Polish grouping. */
    fun amount(amount: BigDecimal): String = NumberFormat.getNumberInstance(POLISH).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)

    /** Unit price: at least two decimals, up to eight (`P_9A`), e.g. `100,00`, `0,12345`. */
    fun unitPrice(price: BigDecimal): String = NumberFormat.getNumberInstance(POLISH).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 8
    }.format(price)

    /** `1 234,56 zł` for PLN, `1 234,56 EUR` for other currencies. */
    fun money(amount: BigDecimal, currency: String): String {
        val number = NumberFormat.getNumberInstance(POLISH).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)
        return "$number ${if (currency == "PLN") "zł" else currency}"
    }

    /** NBP rate with its four decimals and a Polish decimal comma: `4,3775`. */
    fun exchangeRate(rate: BigDecimal): String = rate.toPlainString().replace('.', ',')

    /** Quantity without trailing zeros, Polish decimal comma: `2,5`, `1`. */
    fun quantity(value: BigDecimal): String =
        NumberFormat.getNumberInstance(POLISH).apply { maximumFractionDigits = 6 }.format(value.stripTrailingZeros())

    /** 26-digit NRB grouped as on Polish invoices: `85 2160 9758 3216 8502 4600 2152`; other numbers unchanged. */
    fun bankAccount(number: String): String {
        if (number.length != 26 || !number.isAsciiDigits()) return number
        return number.take(2) + " " + number.drop(2).chunked(4).joinToString(" ")
    }

    fun date(date: LocalDate): String = DATE.format(date)

    fun dateTime(instant: Instant): String = DATE_TIME.format(instant.atZone(ZoneId.systemDefault()))

    /** `Wrzesień 2026`. */
    fun month(month: YearMonth): String = MONTH.format(month).replaceFirstChar { it.titlecase(POLISH) }
}
