package pl.xsenia.fv.issue

import pl.xsenia.fv.invoice.issue.VatRate
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
        val form = InvoiceFormDefaults.newForm(profile = null, today = LocalDate.parse("2026-10-05"), existingNumbers = existing)

        assertEquals(LocalDate.parse("2026-10-05"), form.issueDate)
        assertEquals(LocalDate.parse("2026-09-30"), form.deliveryDate)
        assertEquals("02/09/2026", form.invoiceNumber)

        // From day 6 the delivery date and the number are of the current month.
        val laterForm = InvoiceFormDefaults.newForm(profile = null, today = LocalDate.parse("2026-10-06"), existingNumbers = existing)
        assertEquals(LocalDate.parse("2026-10-06"), laterForm.deliveryDate)
        assertEquals("01/10/2026", laterForm.invoiceNumber)
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
    fun `default dates and suggested number replace those of any form`() {
        val form = InvoiceForm(
            issueDate = LocalDate.parse("2026-08-01"),
            deliveryDate = LocalDate.parse("2026-07-31"),
            invoiceNumber = "FV-TEST-7",
            numberEdited = true,
            datesEdited = true,
            lines = listOf(LineForm("Usługa testowa", "1", "szt.", "100")),
        )

        val defaults = InvoiceFormDefaults.withDefaults(form, LocalDate.parse("2026-10-05"), existing)

        assertEquals(form.copy(issueDate = LocalDate.parse("2026-10-05"), deliveryDate = LocalDate.parse("2026-09-30"), invoiceNumber = "02/09/2026", numberEdited = false, datesEdited = false), defaults)
    }

    @Test
    fun `changing the delivery date moves an unedited number to its month`() {
        val form = InvoiceFormDefaults.newForm(null, LocalDate.parse("2026-10-05"), existing)

        val changed = InvoiceFormDefaults.changeDeliveryDate(form, LocalDate.parse("2026-10-12"), existing)
        assertEquals("01/10/2026", changed.invoiceNumber)
        assertEquals(true, changed.datesEdited)

        val typed = form.copy(invoiceNumber = "FV-TEST-7", numberEdited = true)
        assertEquals("FV-TEST-7", InvoiceFormDefaults.changeDeliveryDate(typed, LocalDate.parse("2026-10-12"), existing).invoiceNumber)
        // "Użyj sugerowanego" brings the suggestion back.
        val suggested = InvoiceFormDefaults.withSuggestedNumber(typed, existing)
        assertEquals("02/09/2026", suggested.invoiceNumber)
        assertEquals(false, suggested.numberEdited)
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

    // Fictitious data only (docs/przyklady/).
    private val pln = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")
    private val eur = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR")
    private val polish = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6"))
    private val german = Counterparty(
        "Lorem Testowy GmbH", address = Address("Teststraße 1"), countryCode = "DE", taxNumber = "999999990", defaultCurrency = InvoiceCurrency.EUR,
    )
    private val american = Counterparty("Lorem Test Inc.", address = Address("1 Test Street"), countryCode = "US")
    private val profile = CompanyProfile(nip = "1996845518", bankAccounts = listOf(pln, eur), counterparties = listOf(polish, german, american))

    @Test
    fun `foreign buyer sets its currency and np in lines not filled in yet`() {
        val form = InvoiceFormDefaults.newForm(profile, LocalDate.parse("2026-10-12"), existing)
            .copy(lines = listOf(LineForm("Usługa testowa", "1", "godz.", "100"), LineForm()))

        val chosen = InvoiceFormDefaults.selectCounterparty(form, german, profile)

        assertEquals(german.id, chosen.counterpartyId)
        assertEquals(InvoiceCurrency.EUR, chosen.currency)
        assertEquals(eur.number, chosen.bankAccountNumber)
        assertEquals(listOf(VatRate.RATE_23, VatRate.NOT_TAXABLE), chosen.lines.map { it.vatRate })
    }

    @Test
    fun `buyer without a default currency keeps the currency`() {
        val form = InvoiceFormDefaults.newForm(profile, LocalDate.parse("2026-10-12"), existing).copy(currency = InvoiceCurrency.USD)

        val chosen = InvoiceFormDefaults.selectCounterparty(form, american, profile)

        assertEquals(InvoiceCurrency.USD, chosen.currency)
        assertEquals(listOf(VatRate.NOT_TAXABLE), chosen.lines.map { it.vatRate })
        // A Polish buyer changes neither the currency nor the rates.
        val back = InvoiceFormDefaults.selectCounterparty(chosen, polish, profile)
        assertEquals(InvoiceCurrency.USD, back.currency)
        assertEquals(listOf(VatRate.NOT_TAXABLE), back.lines.map { it.vatRate })
        assertEquals(polish.id, back.counterpartyId)
    }

    @Test
    fun `new lines get np for a foreign buyer`() {
        val form = InvoiceFormDefaults.newForm(profile, LocalDate.parse("2026-10-12"), existing)
            .copy(lines = listOf(LineForm("Usługa testowa", unit = "dni")))

        assertEquals(LineForm(unit = "dni", vatRate = VatRate.NOT_TAXABLE), InvoiceFormDefaults.newLine(form.copy(counterpartyId = german.id), profile))
        assertEquals(LineForm(unit = "dni"), InvoiceFormDefaults.newLine(form.copy(counterpartyId = polish.id), profile))
        assertEquals(LineForm(unit = "dni"), InvoiceFormDefaults.newLine(form.copy(counterpartyId = null), profile))
    }

    @Test
    fun `the only counterparty, when foreign, gets its defaults on a new form`() {
        val onlyGerman = profile.copy(counterparties = listOf(german))

        val form = InvoiceFormDefaults.newForm(onlyGerman, LocalDate.parse("2026-10-12"), existing)

        assertEquals(german.id, form.counterpartyId)
        assertEquals(InvoiceCurrency.EUR, form.currency)
        assertEquals(eur.number, form.bankAccountNumber)
        assertEquals(listOf(VatRate.NOT_TAXABLE), form.lines.map { it.vatRate })
    }
}
