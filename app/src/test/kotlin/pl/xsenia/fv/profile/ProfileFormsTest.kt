package pl.xsenia.fv.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.issue.InvoiceForm
import pl.xsenia.fv.issue.UnsentInvoice
import pl.xsenia.fv.lookup.LookedUpCompany
import java.time.Instant
import java.time.LocalDate

// Fictitious data only (docs/przyklady/).
class ProfileFormsTest {

    private val typed = CompanyFormFields(
        name = "Jan Testowy Usługi Testowe",
        addressLine1 = "ul. Testowa 1",
        addressLine2 = "00-001 Testowo",
        country = "Polska",
        email = "jan.testowy@example.com",
        phone = "+48 000 000 000",
    )

    private fun lookedUp(line1: String?, line2: String?) =
        LookedUpCompany("1996845518", "JAN TESTOWY", line1, line2, isActiveVatPayer = true, accountNumbers = emptyList())

    @Test
    fun `lookup overwrites name and address and keeps e-mail and phone`() {
        val fields = typed.withLookup(lookedUp("TESTOWA 1", "00-001 TESTOWO"))

        assertEquals(
            typed.copy(name = "JAN TESTOWY", addressLine1 = "TESTOWA 1", addressLine2 = "00-001 TESTOWO", country = "Polska"),
            fields,
        )
    }

    @Test
    fun `lookup without a second address line clears it and without an address keeps the typed one`() {
        assertEquals("", typed.withLookup(lookedUp("TESTOWO 15", null)).addressLine2)
        assertEquals(typed.copy(name = "JAN TESTOWY"), typed.withLookup(lookedUp(null, null)))
    }

    @Test
    fun `form fields of a saved company`() {
        val company = Company("Jan Testowy Usługi Testowe", "1996845518", Address("ul. Testowa 1", null, null), null, "+48 000 000 000")

        assertEquals(
            CompanyFormFields("Jan Testowy Usługi Testowe", "ul. Testowa 1", "", "", "", "+48 000 000 000"),
            CompanyFormFields.of(company),
        )
        assertEquals("Polska", CompanyFormFields.of(null).country)
    }

    @Test
    fun `registry accounts already saved are marked and not added again`() {
        val saved = listOf(BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152"))
        val registry = listOf("85216097583216850246002152", "55109004081797992973253811", "55109004081797992973253811")

        assertEquals(
            listOf(RegistryAccount("85216097583216850246002152", saved = true), RegistryAccount("55109004081797992973253811", saved = false)),
            RegistryAccounts.of(registry, saved),
        )
        assertEquals(
            listOf(BankAccount(name = "Rachunek …3811", number = "55109004081797992973253811", currency = "PLN")),
            RegistryAccounts.toBankAccounts(setOf("85216097583216850246002152", "55109004081797992973253811"), saved),
        )
    }

    @Test
    fun `usage in the draft and unsent invoices`() {
        val draft = InvoiceForm(counterpartyId = "6691034274", bankAccountNumber = "85216097583216850246002152", issueDate = LocalDate.parse("2026-10-13"), deliveryDate = LocalDate.parse("2026-09-30"))
        val unsent = UnsentInvoice.create(draft.copy(counterpartyId = "9945082500", bankAccountNumber = "55109004081797992973253811"), Instant.parse("2026-10-13T12:00:00Z"))

        assertTrue(ProfileUsage.bankAccountUsed("85216097583216850246002152", draft, emptyList()))
        assertTrue(ProfileUsage.bankAccountUsed("55109004081797992973253811", null, listOf(unsent)))
        assertFalse(ProfileUsage.bankAccountUsed("22161029092633573189646018", draft, listOf(unsent)))
        assertTrue(ProfileUsage.counterpartyUsed("9945082500", draft, listOf(unsent)))
        assertFalse(ProfileUsage.counterpartyUsed("9579102904", draft, listOf(unsent)))
    }

    @Test
    fun `counterparty lookup normalizes the NIP and overwrites name and address`() {
        val typed = CounterpartyFormFields(nip = "669-103-42-74", email = "faktury@example.com")
        val company = LookedUpCompany("6691034274", "TESTOWA SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ", "TESTOWA 6", "00-002 TESTOWO", true, emptyList())

        assertEquals(
            CounterpartyFormFields(
                nip = "6691034274",
                name = "TESTOWA SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ",
                addressLine1 = "TESTOWA 6",
                addressLine2 = "00-002 TESTOWO",
                email = "faktury@example.com",
            ),
            typed.withLookup(company),
        )
    }

    @Test
    fun `form fields of a saved counterparty`() {
        val counterparty = Counterparty("Lorem Ipsum Testowy S.A.", "9945082500", Address("ul. Dolor Sit Amet 12/3", "00-003 Testowo"))

        assertEquals(
            CounterpartyFormFields("9945082500", "Lorem Ipsum Testowy S.A.", "ul. Dolor Sit Amet 12/3", "00-003 Testowo", ""),
            CounterpartyFormFields.of(counterparty),
        )
        assertEquals(CounterpartyFormFields(countryCode = "PL"), CounterpartyFormFields.of(null))
    }

    @Test
    fun `currency options keep an earlier currency of the account`() {
        assertEquals(listOf("PLN", "EUR", "USD", "GBP"), BankAccountCurrencies.options("PLN"))
        assertEquals(listOf("PLN", "EUR", "USD", "GBP", "CHF"), BankAccountCurrencies.options("CHF"))
        assertEquals("PLN", BankAccountFormFields.of(null).currency)
    }
}
