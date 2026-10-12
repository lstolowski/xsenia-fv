package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import java.time.LocalDate

class UnsentInvoicesTest {

    @TempDir
    lateinit var dir: File

    private fun form(number: String) =
        InvoiceForm(issueDate = LocalDate.parse("2026-10-01"), deliveryDate = LocalDate.parse("2026-09-30"), invoiceNumber = number)

    @Test
    fun `saves, updates, lists newest first and deletes unsent invoices`() {
        val store = UnsentInvoiceStore(dir)
        val first = UnsentInvoice.create(form("04/09/2026"), Instant.parse("2026-10-01T10:00:00Z"))
        val second = UnsentInvoice.create(form("05/09/2026"), Instant.parse("2026-10-02T10:00:00Z"))
        store.save(first)
        store.save(second)
        store.save(first.copy(form = form("06/09/2026"), updatedAt = Instant.parse("2026-10-03T10:00:00Z")))

        assertEquals(listOf("06/09/2026", "05/09/2026"), store.list().map { it.form.invoiceNumber })
        assertEquals("06/09/2026", store.get(first.id)?.form?.invoiceNumber)

        store.delete(first.id)

        assertEquals(listOf(second.id), store.list().map { it.id })
        assertNull(store.get(first.id))
        assertTrue(File(dir, "unsent").listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun `keeps rejection reason`() {
        val store = UnsentInvoiceStore(dir)
        val rejected = UnsentInvoice.create(form("04/09/2026"), Instant.EPOCH, Rejection(450, "Błąd semantyki", listOf("detail"), null))

        store.save(rejected)

        assertEquals(Rejection(450, "Błąd semantyki", listOf("detail"), null), store.get(rejected.id)?.rejection)
    }

    @Test
    fun `corrupted file does not hide other invoices`() {
        val store = UnsentInvoiceStore(dir)
        store.save(UnsentInvoice.create(form("04/09/2026"), Instant.EPOCH))
        File(dir, "unsent/broken.json").writeText("{ lorem")

        assertEquals(listOf("04/09/2026"), store.list().map { it.form.invoiceNumber })
    }

    @Test
    fun `unsent invoices reserve their numbers, except the one being edited`() {
        val edited = UnsentInvoice.create(form("04/09/2026"), Instant.EPOCH)
        val other = UnsentInvoice.create(form("05/09/2026"), Instant.EPOCH)
        val blank = UnsentInvoice.create(form(" "), Instant.EPOCH)

        assertEquals(
            listOf("01/09/2026", "05/09/2026"),
            TakenNumbers.of(listOf("01/09/2026"), listOf(edited, other, blank), excludeUnsentId = edited.id),
        )
        assertEquals(listOf("01/09/2026", "04/09/2026", "05/09/2026"), TakenNumbers.of(listOf("01/09/2026"), listOf(edited, other), null))
    }
}
