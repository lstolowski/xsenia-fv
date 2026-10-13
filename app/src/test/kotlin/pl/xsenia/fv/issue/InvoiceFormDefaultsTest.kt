package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InvoiceFormDefaultsTest {

    private val existing = listOf("01/09/2026")

    @Test
    fun `new form uses today and the suggested number`() {
        val form = InvoiceFormDefaults.newForm(profile = null, today = LocalDate.parse("2026-10-12"), existingNumbers = existing)

        assertEquals(LocalDate.parse("2026-10-12"), form.issueDate)
        assertEquals(LocalDate.parse("2026-09-30"), form.deliveryDate)
        assertEquals("02/09/2026", form.invoiceNumber)
    }

    @Test
    fun `untouched draft opened later gets today's dates and a fresh number`() {
        val draft = InvoiceFormDefaults.newForm(null, LocalDate.parse("2026-10-12"), emptyList())
            .copy(lines = listOf(LineForm("Usługa testowa", "1", "szt.", "100")))

        val refreshed = InvoiceFormDefaults.refresh(draft, LocalDate.parse("2026-10-20"), existing)

        assertEquals(LocalDate.parse("2026-10-20"), refreshed.issueDate)
        assertEquals(LocalDate.parse("2026-10-20"), refreshed.deliveryDate)
        assertEquals("01/10/2026", refreshed.invoiceNumber)
        assertEquals(draft.lines, refreshed.lines)
    }

    @Test
    fun `dates and number chosen by the user are kept`() {
        val draft = InvoiceForm(
            issueDate = LocalDate.parse("2026-10-01"),
            deliveryDate = LocalDate.parse("2026-09-15"),
            invoiceNumber = "FV-TEST-7",
            numberEdited = true,
            datesEdited = true,
        )

        assertEquals(draft, InvoiceFormDefaults.refresh(draft, LocalDate.parse("2026-10-20"), existing))
    }

    @Test
    fun `unedited number follows kept dates`() {
        val draft = InvoiceForm(issueDate = LocalDate.parse("2026-10-01"), deliveryDate = LocalDate.parse("2026-09-30"), invoiceNumber = "01/09/2026", datesEdited = true)

        assertEquals("02/09/2026", InvoiceFormDefaults.refresh(draft, LocalDate.parse("2026-10-20"), existing).invoiceNumber)
    }
}
