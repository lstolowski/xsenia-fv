package pl.xsenia.fv.ui.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.FieldIssue
import pl.xsenia.fv.invoice.profile.ProfileValidator
import pl.xsenia.fv.invoice.profile.Validation
import pl.xsenia.fv.lookup.CompanyLookupResult
import pl.xsenia.fv.lookup.CompanyLookupService
import pl.xsenia.fv.profile.CompanyFormFields
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.profile.RegistryAccount
import pl.xsenia.fv.profile.RegistryAccounts

data class LookupUiState(val running: Boolean = false, val result: CompanyLookupResult? = null)

data class CompanyFormUiState(
    /** NIP of the KSeF token — read-only (ADR 0006). */
    val nip: String,
    val fields: CompanyFormFields,
    /** Shown after the first „Zapisz”. */
    val issues: List<FieldIssue>,
    val lookup: LookupUiState,
    val bankAccounts: List<BankAccount>,
    /** White List accounts offered after a lookup (Q3); null when the dialog is closed. */
    val registryAccounts: List<RegistryAccount>?,
    val dirty: Boolean,
)

/**
 * „Moja firma” form (ZAD-023): company data with the White List lookup, and the bank accounts of the company.
 * Company fields are saved with „Zapisz”; bank accounts are saved one by one as they are added or edited.
 *
 * @param autoLookup fetch the data as soon as the form opens (first-run setup, ZAD-025)
 */
class CompanyFormViewModel(
    private val session: ProfileSession,
    private val lookupService: CompanyLookupService,
    autoLookup: Boolean = false,
) : ViewModel() {

    private val fields = MutableStateFlow<CompanyFormFields?>(null)
    private val original = MutableStateFlow<CompanyFormFields?>(null)
    private val showErrors = MutableStateFlow(false)
    private val lookup = MutableStateFlow(LookupUiState())
    private val registryAccounts = MutableStateFlow<List<RegistryAccount>?>(null)

    /** Becomes true once the company was saved. */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    val state: StateFlow<CompanyFormUiState?> = combine(
        combine(fields, original, showErrors, ::Triple),
        lookup,
        session.profile.profile,
        registryAccounts,
    ) { (fields, original, showErrors), lookup, profile, registryAccounts ->
        if (fields == null || profile == null) return@combine null
        CompanyFormUiState(
            nip = session.nip,
            fields = fields,
            issues = if (showErrors) issuesOf(fields) else emptyList(),
            lookup = lookup,
            bankAccounts = profile.bankAccounts,
            registryAccounts = registryAccounts,
            dirty = fields != original,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val company = session.profile.profile.filterNotNull().first().company
            CompanyFormFields.of(company).let {
                original.value = it
                fields.value = it
            }
            if (autoLookup) lookup()
        }
    }

    fun update(change: (CompanyFormFields) -> CompanyFormFields) {
        fields.value = fields.value?.let(change)
    }

    /** Fetches the company by the token NIP; name and address are overwritten, registry accounts offered. */
    fun lookup() {
        if (lookup.value.running) return
        lookup.value = LookupUiState(running = true)
        viewModelScope.launch {
            val result = lookupService.lookup(session.nip)
            lookup.value = LookupUiState(result = result)
            if (result is CompanyLookupResult.Found) {
                update { it.withLookup(result.company) }
                val saved = session.profile.profile.value?.bankAccounts.orEmpty()
                RegistryAccounts.of(result.company.accountNumbers, saved).takeIf { accounts -> accounts.any { !it.saved } }
                    ?.let { registryAccounts.value = it }
            }
        }
    }

    fun dismissRegistryAccounts() {
        registryAccounts.value = null
    }

    /** Adds the chosen White List accounts to the company (saved at once, like any added account). */
    fun addRegistryAccounts(selected: Set<String>) {
        registryAccounts.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val saved = session.profile.profile.value?.bankAccounts.orEmpty()
            RegistryAccounts.toBankAccounts(selected, saved).forEach { session.profile.saveBankAccount(it) }
        }
    }

    fun save() {
        showErrors.value = true
        val current = fields.value ?: return
        val company = (ProfileValidator.company(session.nip, current.toInput()) as? Validation.Valid)?.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.profile.saveCompany(company) }
            original.value = current
            _saved.value = true
        }
    }

    private fun issuesOf(fields: CompanyFormFields): List<FieldIssue> =
        (ProfileValidator.company(session.nip, fields.toInput()) as? Validation.Invalid)?.issues.orEmpty()
}
