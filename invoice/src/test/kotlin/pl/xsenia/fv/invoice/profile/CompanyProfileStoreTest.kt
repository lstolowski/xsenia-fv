package pl.xsenia.fv.invoice.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

// Fictitious data only (docs/przyklady/).
class CompanyProfileStoreTest {

    @TempDir
    lateinit var directory: File

    private val nip = "1996845518"
    private val store by lazy { CompanyProfileStore(directory, nip) }

    private val company = Company("Jan Testowy Usługi Testowe", nip, Address("ul. Testowa 1", "00-001 Testowo", "Polska"), "jan.testowy@example.com", "+48 000 000 000")
    private val toyota = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")
    private val santander = BankAccount("Konto testowe Santander", "Santander Bank Polska", "55109004081797992973253811")
    private val testowa = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo", "Polska"), "faktury@example.com")
    private val lorem = Counterparty("Lorem Ipsum Testowy S.A.", "9945082500", Address("ul. Dolor Sit Amet 12/3", "00-003 Testowo"))

    @Test
    fun `empty profile`() {
        assertEquals(CompanyProfile(nip), store.load())
    }

    @Test
    fun `company is saved and read back`() {
        store.saveCompany(company)

        assertEquals(company, CompanyProfileStore(directory, nip).load().company)
        assertFalse(File(directory, "company.json.tmp").exists())
    }

    @Test
    fun `company of another NIP is refused`() {
        assertThrows<IllegalArgumentException> { store.saveCompany(company.copy(nip = "6691034274")) }
    }

    @Test
    fun `bank accounts keep their order when edited and can be deleted`() {
        store.saveBankAccount(toyota)
        store.saveBankAccount(santander)
        val renamed = toyota.copy(name = "Konto główne")
        store.saveBankAccount(renamed, replacing = toyota.number)

        assertEquals(listOf(renamed, santander), store.load().bankAccounts)

        // A changed number replaces the old entry in place.
        val renumbered = santander.copy(number = "22161029092633573189646018")
        store.saveBankAccount(renumbered, replacing = santander.number)
        assertEquals(listOf(renamed, renumbered), store.load().bankAccounts)

        store.deleteBankAccount(renamed.number)
        assertEquals(listOf(renumbered), store.load().bankAccounts)
    }

    @Test
    fun `counterparties are replaced by NIP and deleted`() {
        store.saveCounterparty(testowa)
        store.saveCounterparty(lorem)
        val edited = testowa.copy(name = "Testowa Spółka", email = null)
        store.saveCounterparty(edited)

        assertEquals(listOf(edited, lorem), store.load().counterparties)

        store.deleteCounterparty(testowa.nip)
        assertEquals(listOf(lorem), store.load().counterparties)
    }

    @Test
    fun `files use the former JSON field names`() {
        store.saveBankAccount(toyota)

        val text = File(directory, CompanyProfileStore.BANK_ACCOUNTS_FILE).readText()
        assertTrue("\"account\": \"85216097583216850246002152\"" in text, text)
        assertTrue("\"currency\": \"PLN\"" in text, text)
        assertTrue("\"version\": 1" in text, text)
    }

    @Test
    fun `damaged file is set aside and not overwritten`() {
        File(directory, CompanyProfileStore.COUNTERPARTIES_FILE).writeText("{not json")

        assertEquals(emptyList<Counterparty>(), store.load().counterparties)
        assertTrue(directory.listFiles().orEmpty().any { it.name.startsWith("counterparties.json.damaged-") })
        assertNull(store.load().company)
    }
}
