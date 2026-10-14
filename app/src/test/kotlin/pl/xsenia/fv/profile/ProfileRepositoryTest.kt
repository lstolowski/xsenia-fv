package pl.xsenia.fv.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.CompanyProfileStore
import pl.xsenia.fv.invoice.profile.Counterparty
import java.io.File

// Fictitious data only (docs/przyklady/).
class ProfileRepositoryTest {

    @TempDir
    lateinit var directory: File

    private val nip = "1996845518"

    private fun repository() = ProfileRepository(CompanyProfileStore(directory, nip), CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `changes are saved and published`() = runBlocking {
        val repository = repository()
        val company = Company("Jan Testowy Usługi Testowe", nip, Address("ul. Testowa 1", "00-001 Testowo", "Polska"))
        val account = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")
        val counterparty = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6"))

        repository.saveCompany(company)
        repository.saveBankAccount(account)
        repository.saveCounterparty(counterparty)

        val profile = repository.profile.filterNotNull().first()
        assertEquals(company, profile.company)
        assertEquals(listOf(account), profile.bankAccounts)
        assertEquals(listOf(counterparty), profile.counterparties)
        assertEquals(true, profile.canIssueInvoices)

        // Another repository (e.g. after an app restart) reads the same data.
        assertEquals(profile, repository().profile.filterNotNull().first())

        repository.deleteCounterparty(counterparty.id)
        repository.deleteBankAccount(account.number)
        assertEquals(emptyList<Counterparty>(), repository.profile.value?.counterparties)
        assertEquals(emptyList<BankAccount>(), repository.profile.value?.bankAccounts)
    }
}
