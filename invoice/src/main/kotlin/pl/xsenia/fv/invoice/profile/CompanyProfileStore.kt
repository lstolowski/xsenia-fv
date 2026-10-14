package pl.xsenia.fv.invoice.profile

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Company, bank accounts and counterparties of one profile, in [directory] (`profiles/{nip}/`, ADR 0006):
 * `company.json`, `bank-accounts.json`, `counterparties.json`. Writes are atomic. Data is validated by
 * [ProfileValidator] before it is saved; the store does not validate.
 *
 * Not thread-safe — callers serialize access (one writer per profile).
 */
class CompanyProfileStore(private val directory: File, private val nip: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
        // Writes the format version and the default currency.
        encodeDefaults = true
    }

    fun load(): CompanyProfile = CompanyProfile(
        nip = nip,
        company = read(COMPANY_FILE, CompanyFile.serializer())?.company,
        bankAccounts = read(BANK_ACCOUNTS_FILE, BankAccountsFile.serializer())?.bankAccounts.orEmpty(),
        counterparties = read(COUNTERPARTIES_FILE, CounterpartiesFile.serializer())?.counterparties.orEmpty(),
    )

    fun saveCompany(company: Company) {
        require(company.nip == nip) { "Company NIP must be the profile NIP" }
        write(COMPANY_FILE, CompanyFile.serializer(), CompanyFile(company = company))
    }

    /** Replaces the account numbered [replacing] (keeping its position), or appends [account]. */
    fun saveBankAccount(account: BankAccount, replacing: String? = null) =
        writeBankAccounts(load().bankAccounts.upsert(account) { it.number == (replacing ?: account.number) })

    fun deleteBankAccount(number: String) = writeBankAccounts(load().bankAccounts.filter { it.number != number })

    /**
     * Replaces the counterparty with the same id (keeping its position), or appends a new one. Counterparties saved
     * before US-012 are written in the new format (country, id) with the next change (ADR 0009).
     */
    fun saveCounterparty(counterparty: Counterparty) =
        writeCounterparties(load().counterparties.upsert(counterparty) { it.id == counterparty.id })

    fun deleteCounterparty(id: String) = writeCounterparties(load().counterparties.filter { it.id != id })

    private fun writeBankAccounts(accounts: List<BankAccount>) =
        write(BANK_ACCOUNTS_FILE, BankAccountsFile.serializer(), BankAccountsFile(bankAccounts = accounts))

    private fun writeCounterparties(counterparties: List<Counterparty>) =
        write(COUNTERPARTIES_FILE, CounterpartiesFile.serializer(), CounterpartiesFile(counterparties = counterparties))

    private fun <T> List<T>.upsert(item: T, matches: (T) -> Boolean): List<T> =
        if (any(matches)) map { if (matches(it)) item else it } else this + item

    private fun <T> read(name: String, serializer: KSerializer<T>): T? {
        val file = File(directory, name)
        if (!file.isFile) return null
        return try {
            json.decodeFromString(serializer, file.readText(Charsets.UTF_8))
        } catch (_: IllegalArgumentException) {
            // SerializationException included. Should not happen (atomic writes); keep the damaged file aside
            // instead of overwriting it on the next save.
            file.renameTo(File(directory, "$name.damaged-${System.currentTimeMillis()}"))
            null
        }
    }

    private fun <T> write(name: String, serializer: KSerializer<T>, value: T) {
        directory.mkdirs()
        val target = File(directory, name)
        val temporary = File(directory, "$name.tmp")
        temporary.writeText(json.encodeToString(serializer, value), Charsets.UTF_8)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    @Serializable
    private data class CompanyFile(val version: Int = FORMAT_VERSION, val company: Company? = null)

    @Serializable
    private data class BankAccountsFile(val version: Int = FORMAT_VERSION, val bankAccounts: List<BankAccount> = emptyList())

    @Serializable
    private data class CounterpartiesFile(val version: Int = FORMAT_VERSION, val counterparties: List<Counterparty> = emptyList())

    companion object {
        const val COMPANY_FILE = "company.json"
        const val BANK_ACCOUNTS_FILE = "bank-accounts.json"
        const val COUNTERPARTIES_FILE = "counterparties.json"
        private const val FORMAT_VERSION = 1
    }
}
