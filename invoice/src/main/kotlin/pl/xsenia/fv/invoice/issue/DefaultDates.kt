package pl.xsenia.fv.invoice.issue

import java.time.LocalDate
import java.time.YearMonth

/** Default dates of a new invoice (decision R3). */
object DefaultDates {

    private const val LAST_DAY_FOR_PREVIOUS_MONTH = 5

    /**
     * Last day of the previous month when issuing on day 1–5 (invoicing last month's work),
     * otherwise the issue date.
     */
    fun deliveryDate(issueDate: LocalDate): LocalDate =
        if (issueDate.dayOfMonth <= LAST_DAY_FOR_PREVIOUS_MONTH) {
            YearMonth.from(issueDate).minusMonths(1).atEndOfMonth()
        } else {
            issueDate
        }
}
