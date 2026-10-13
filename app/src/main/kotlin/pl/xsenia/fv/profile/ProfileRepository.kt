package pl.xsenia.fv.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.CompanyProfileStore
import pl.xsenia.fv.invoice.profile.Counterparty

/**
 * Observable company, bank accounts and counterparties of one profile (ADR 0006). Every change is written to disk
 * first; data must be validated with `ProfileValidator` before it is saved.
 */
class ProfileRepository(private val store: CompanyProfileStore, scope: CoroutineScope) {

    private val _profile = MutableStateFlow<CompanyProfile?>(null)

    /** null until loaded from disk. */
    val profile: StateFlow<CompanyProfile?> = _profile.asStateFlow()

    // Disk and state change together: otherwise the initial load could overwrite a change saved meanwhile.
    private val lock = Any()

    init {
        scope.launch(Dispatchers.IO) { synchronized(lock) { if (_profile.value == null) _profile.value = store.load() } }
    }

    /** Call from a background thread. */
    fun saveCompany(company: Company) = change { store.saveCompany(company) }

    /** Call from a background thread. [replacing] — number of the edited account, null for a new one. */
    fun saveBankAccount(account: BankAccount, replacing: String? = null) = change { store.saveBankAccount(account, replacing) }

    /** Call from a background thread. */
    fun deleteBankAccount(number: String) = change { store.deleteBankAccount(number) }

    /** Call from a background thread. */
    fun saveCounterparty(counterparty: Counterparty) = change { store.saveCounterparty(counterparty) }

    /** Call from a background thread. */
    fun deleteCounterparty(nip: String) = change { store.deleteCounterparty(nip) }

    private fun change(write: () -> Unit) = synchronized(lock) {
        write()
        _profile.value = store.load()
    }
}
