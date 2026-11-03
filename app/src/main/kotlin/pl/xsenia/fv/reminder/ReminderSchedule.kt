package pl.xsenia.fv.reminder

import pl.xsenia.fv.mirror.MirrorEntry
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime

/** When the monthly "no sales invoice" reminder fires and whether it is needed (US-017). */
object ReminderSchedule {

    val REMINDER_TIME: LocalTime = LocalTime.of(19, 0)

    /** 19:00 on the last calendar day of [month] in the zone of [now]. */
    fun triggerOf(month: YearMonth, now: ZonedDateTime): ZonedDateTime =
        month.atEndOfMonth().atTime(REMINDER_TIME).atZone(now.zone)

    /** The month whose reminder comes next: the current one until its trigger passes, then the following one. */
    fun nextMonth(now: ZonedDateTime): YearMonth {
        val current = YearMonth.from(now)
        return if (now.isBefore(triggerOf(current, now))) current else current.plusMonths(1)
    }

    /**
     * A reminder delayed by the system is still valid on the day it was meant for; past midnight it would talk about
     * "this month" in the wrong month, so it is dropped.
     */
    fun isStillCurrent(month: YearMonth, now: ZonedDateTime): Boolean = now.toLocalDate() == month.atEndOfMonth()

    /** True when no sales invoice of KSeF was delivered in [month] (the same month criterion as the invoice list). */
    fun noSalesInvoice(salesEntries: List<MirrorEntry>, month: YearMonth): Boolean =
        salesEntries.none { YearMonth.from(it.deliveryDate) == month }
}
