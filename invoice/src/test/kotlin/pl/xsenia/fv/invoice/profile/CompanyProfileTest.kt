package pl.xsenia.fv.invoice.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Fictitious data only (docs/przyklady/).
class CompanyProfileTest {

    private val company = Company("Jan Testowy Usługi Testowe", "1996845518", Address("ul. Testowa 1", "00-001 Testowo"))
    private val pln = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")
    private val eur = BankAccount("Konto testowe NBP", "NBP", "71101075753723914707418989", currency = "EUR")
    private val counterparty = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6"))

    @Test
    fun `complete profile can issue invoices`() {
        val profile = CompanyProfile("1996845518", company, listOf(eur, pln), listOf(counterparty))

        assertTrue(profile.canIssueInvoices)
        assertEquals(emptyList<ProfileGap>(), profile.gaps)
        assertEquals(listOf(pln), profile.invoiceBankAccounts)
    }

    @Test
    fun `missing data is listed`() {
        assertEquals(
            listOf(ProfileGap.COMPANY, ProfileGap.BANK_ACCOUNT, ProfileGap.COUNTERPARTY),
            CompanyProfile("1996845518").gaps,
        )
        // Only PLN accounts can be put on an invoice.
        val onlyEur = CompanyProfile("1996845518", company, listOf(eur), listOf(counterparty))
        assertEquals(listOf(ProfileGap.BANK_ACCOUNT), onlyEur.gaps)
        assertFalse(onlyEur.canIssueInvoices)
    }
}
