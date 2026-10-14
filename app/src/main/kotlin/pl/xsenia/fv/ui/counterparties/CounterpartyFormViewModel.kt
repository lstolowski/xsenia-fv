package pl.xsenia.fv.ui.counterparties

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
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.FieldIssue
import pl.xsenia.fv.invoice.profile.ProfileValidator
import pl.xsenia.fv.invoice.profile.Validation
import pl.xsenia.fv.lookup.CompanyLookupResult
import pl.xsenia.fv.lookup.CompanyLookupService
import pl.xsenia.fv.profile.CounterpartyFormFields
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.profile.ProfileUsage
import pl.xsenia.fv.ui.company.LookupUiState

data class CounterpartyFormUiState(
    val fields: CounterpartyFormFields,
    /** Shown after the first „Zapisz”. */
    val issues: List<FieldIssue>,
    val lookup: LookupUiState,
    /** Editing a saved counterparty — its country and identifier are read-only (ADR 0009). */
    val editing: Boolean,
    /** Chosen in the draft or in an unsent invoice — warned about before deleting. */
    val usedByInvoices: Boolean,
    val dirty: Boolean,
)

/** New or edited counterparty (ZAD-024); [originalId] — its [Counterparty.id], null for a new one. */
class CounterpartyFormViewModel(
    private val session: ProfileSession,
    private val lookupService: CompanyLookupService,
    private val originalId: String?,
) : ViewModel() {

    private val fields = MutableStateFlow<CounterpartyFormFields?>(null)
    private val original = MutableStateFlow<CounterpartyFormFields?>(null)
    private val showErrors = MutableStateFlow(false)
    private val lookup = MutableStateFlow(LookupUiState())
    private val usedByInvoices = MutableStateFlow(false)

    /** Id of the saved counterparty (to select it on the invoice form), or "" after deleting; null while open. */
    private val _closedWith = MutableStateFlow<String?>(null)
    val closedWith: StateFlow<String?> = _closedWith.asStateFlow()

    val state: StateFlow<CounterpartyFormUiState?> = combine(
        combine(fields, original, showErrors, ::Triple),
        lookup,
        usedByInvoices,
        session.profile.profile,
    ) { (fields, original, showErrors), lookup, used, profile ->
        if (fields == null || profile == null) return@combine null
        CounterpartyFormUiState(
            fields = fields,
            issues = if (showErrors) issuesOf(fields, profile.counterparties) else emptyList(),
            lookup = lookup,
            editing = originalId != null,
            usedByInvoices = used,
            dirty = fields != original,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val counterparties = session.profile.profile.filterNotNull().first().counterparties
            CounterpartyFormFields.of(counterparties.firstOrNull { it.id == originalId }).let {
                original.value = it
                fields.value = it
            }
            if (originalId != null) {
                usedByInvoices.value = withContext(Dispatchers.IO) {
                    ProfileUsage.counterpartyUsed(originalId, session.issueStorage.loadDraft(), session.unsentInvoices.invoices.value)
                }
            }
        }
    }

    fun update(change: (CounterpartyFormFields) -> CounterpartyFormFields) {
        // The country and identifier of a saved counterparty never change.
        val saved = original.value
        fields.value = fields.value?.let(change)?.let {
            if (originalId != null && saved != null) it.copy(nip = saved.nip, countryCode = saved.countryCode, taxNumber = saved.taxNumber) else it
        }
    }

    /** Fetches the company by the typed NIP from the White List; name and address are overwritten. */
    fun lookup() {
        val nip = fields.value?.nip ?: return
        if (lookup.value.running) return
        lookup.value = LookupUiState(running = true)
        viewModelScope.launch {
            val result = lookupService.lookup(nip)
            lookup.value = LookupUiState(result = result)
            if (result is CompanyLookupResult.Found) update { it.withLookup(result.company) }
        }
    }

    fun save() {
        showErrors.value = true
        val current = fields.value ?: return
        val counterparties = session.profile.profile.value?.counterparties.orEmpty()
        val existing = counterparties.firstOrNull { it.id == originalId }
        val counterparty = (ProfileValidator.counterparty(current.toInput(), others(counterparties), existing) as? Validation.Valid)?.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.profile.saveCounterparty(counterparty) }
            _closedWith.value = counterparty.id
        }
    }

    fun delete() {
        val id = originalId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.profile.deleteCounterparty(id) }
            _closedWith.value = ""
        }
    }

    private fun others(counterparties: List<Counterparty>) = counterparties.filter { it.id != originalId }

    private fun issuesOf(fields: CounterpartyFormFields, counterparties: List<Counterparty>): List<FieldIssue> {
        val existing = counterparties.firstOrNull { it.id == originalId }
        return (ProfileValidator.counterparty(fields.toInput(), others(counterparties), existing) as? Validation.Invalid)?.issues.orEmpty()
    }
}
