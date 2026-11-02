package pl.xsenia.fv.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.invoices.InvoiceMetadata
import pl.xsenia.fv.mirror.MirrorEntry
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleTest {

    private val zone = ZoneId.of("Europe/Warsaw")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    @Test
    fun `triggers at 19 on the last day of the month`() {
        assertEquals(at(2026, 10, 31, 19), ReminderSchedule.triggerOf(YearMonth.of(2026, 10), at(2026, 10, 5)))
        assertEquals(at(2028, 2, 29, 19), ReminderSchedule.triggerOf(YearMonth.of(2028, 2), at(2028, 2, 1)))
    }

    @Test
    fun `next reminder is this month until 19 on its last day`() {
        assertEquals(YearMonth.of(2026, 10), ReminderSchedule.nextMonth(at(2026, 10, 31, 18, 59)))
        assertEquals(YearMonth.of(2026, 11), ReminderSchedule.nextMonth(at(2026, 10, 31, 19, 0)))
        assertEquals(YearMonth.of(2026, 11), ReminderSchedule.nextMonth(at(2026, 11, 1)))
    }

    @Test
    fun `next reminder rolls over the year`() {
        assertEquals(YearMonth.of(2027, 1), ReminderSchedule.nextMonth(at(2026, 12, 31, 20)))
    }

    @Test
    fun `delayed reminder is dropped after midnight`() {
        val month = YearMonth.of(2026, 10)
        assertTrue(ReminderSchedule.isStillCurrent(month, at(2026, 10, 31, 23, 30)))
        assertFalse(ReminderSchedule.isStillCurrent(month, at(2026, 11, 1, 0, 5)))
    }

    private fun entry(deliveryDate: String) = MirrorEntry(
        InvoiceMetadata(
            ksefNumber = "K", invoiceNumber = "01/10/2026", issueDate = LocalDate.parse(deliveryDate),
            invoicingDate = Instant.EPOCH, acquisitionDate = Instant.EPOCH, permanentStorageDate = Instant.EPOCH,
            seller = InvoiceMetadata.Seller("9945082500", "Jan Testowy"),
            buyer = InvoiceMetadata.Buyer(InvoiceMetadata.BuyerIdentifier("Nip", "5260250274"), "Lorem Testowy"),
            netAmount = BigDecimal("500"), grossAmount = BigDecimal("500"), vatAmount = BigDecimal.ZERO,
            currency = "PLN", invoicingMode = "Online", invoiceType = "Vat",
            formCode = InvoiceMetadata.FormCode("FA (3)", "1-0E", "FA"), isSelfInvoicing = false, hasAttachment = false, invoiceHash = "x",
        ),
        LocalDate.parse(deliveryDate),
    )

    @Test
    fun `reminder is needed only when no invoice was delivered in the month`() {
        val october = YearMonth.of(2026, 10)
        assertTrue(ReminderSchedule.noSalesInvoice(emptyList(), october))
        assertTrue(ReminderSchedule.noSalesInvoice(listOf(entry("2026-09-30"), entry("2026-11-01")), october))
        assertFalse(ReminderSchedule.noSalesInvoice(listOf(entry("2026-09-30"), entry("2026-10-31")), october))
    }
}
