package pl.xsenia.fv.issue

import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.Counterparty
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

    @Test
    fun `defaults added later fill only empty choices`() {
        // Fictitious data only (docs/przyklady/).
        val profile = CompanyProfile(
            nip = "1996845518",
            bankAccounts = listOf(
                BankAccount("Konto testowe NBP", "NBP", "71101075753723914707418989", "EUR"),
                BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152"),
            ),
            counterparties = listOf(Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6"))),
        )
        val empty = InvoiceFormDefaults.newForm(profile = null, today = LocalDate.parse("2026-10-12"), existingNumbers = existing)

        val filled = InvoiceFormDefaults.fillMissing(empty, profile)
        assertEquals("6691034274", filled.counterpartyId)
        assertEquals("85216097583216850246002152", filled.bankAccountNumber)

        val chosen = empty.copy(counterpartyId = "9945082500", bankAccountNumber = "55109004081797992973253811")
        assertEquals(chosen, InvoiceFormDefaults.fillMissing(chosen, profile))
    }

    @Test
    fun `changing the currency picks an account in that currency when the chosen one does not fit`() {
        val pln = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")
        val eur = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR")
        val profile = CompanyProfile(nip = "1996845518", bankAccounts = listOf(pln, eur))
        val form = InvoiceFormDefaults.newForm(profile, LocalDate.parse("2026-10-12"), existing)
        assertEquals(pln.number, form.bankAccountNumber)

        val euro = InvoiceFormDefaults.changeCurrency(form, InvoiceCurrency.EUR, profile)
        assertEquals(InvoiceCurrency.EUR, euro.currency)
        assertEquals(eur.number, euro.bankAccountNumber)

        // No GBP account: the chosen one stays (the form shows a warning).
        assertEquals(eur.number, InvoiceFormDefaults.changeCurrency(euro, InvoiceCurrency.GBP, profile).bankAccountNumber)
        assertEquals(pln.number, InvoiceFormDefaults.changeCurrency(euro, InvoiceCurrency.PLN, profile).bankAccountNumber)

        // An account added later is filled in for the invoice currency.
        val empty = InvoiceFormDefaults.newForm(null, LocalDate.parse("2026-10-12"), existing).copy(currency = InvoiceCurrency.EUR)
        assertEquals(eur.number, InvoiceFormDefaults.fillMissing(empty, profile).bankAccountNumber)
    }
}
