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
import pl.xsenia.fv.profile.BankAccountFormFields
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.profile.ProfileUsage

data class BankAccountFormUiState(
    val fields: BankAccountFormFields,
    /** Shown after the first „Zapisz”. */
    val issues: List<FieldIssue>,
    val editing: Boolean,
    /** The account is chosen in the draft or in an unsent invoice — warned about before deleting or renumbering. */
    val usedByInvoices: Boolean,
    val dirty: Boolean,
)

/** New or edited bank account of my company (ZAD-023); [originalNumber] null for a new one. */
class BankAccountFormViewModel(private val session: ProfileSession, private val originalNumber: String?) : ViewModel() {

    private val fields = MutableStateFlow<BankAccountFormFields?>(null)
    private val original = MutableStateFlow<BankAccountFormFields?>(null)
    private val showErrors = MutableStateFlow(false)
    private val usedByInvoices = MutableStateFlow(false)

    /** Becomes true once the account was saved or deleted. */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    val state: StateFlow<BankAccountFormUiState?> = combine(fields, original, showErrors, usedByInvoices, session.profile.profile) {
            fields, original, showErrors, used, profile ->
        if (fields == null || profile == null) return@combine null
        BankAccountFormUiState(
            fields = fields,
            issues = if (showErrors) issuesOf(fields, others(profile.bankAccounts)) else emptyList(),
            editing = originalNumber != null,
            usedByInvoices = used,
            dirty = fields != original,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val accounts = session.profile.profile.filterNotNull().first().bankAccounts
            BankAccountFormFields.of(accounts.firstOrNull { it.number == originalNumber }).let {
                original.value = it
                fields.value = it
            }
            if (originalNumber != null) {
                usedByInvoices.value = withContext(Dispatchers.IO) {
                    ProfileUsage.bankAccountUsed(originalNumber, session.issueStorage.loadDraft(), session.unsentInvoices.invoices.value)
                }
            }
        }
    }

    fun update(change: (BankAccountFormFields) -> BankAccountFormFields) {
        fields.value = fields.value?.let(change)
    }

    fun save() {
        showErrors.value = true
        val current = fields.value ?: return
        val accounts = session.profile.profile.value?.bankAccounts.orEmpty()
        val account = (ProfileValidator.bankAccount(current.toInput(), others(accounts)) as? Validation.Valid)?.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.profile.saveBankAccount(account, replacing = originalNumber) }
            _closed.value = true
        }
    }

    fun delete() {
        val number = originalNumber ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.profile.deleteBankAccount(number) }
            _closed.value = true
        }
    }

    private fun others(accounts: List<BankAccount>) = accounts.filter { it.number != originalNumber }

    private fun issuesOf(fields: BankAccountFormFields, others: List<BankAccount>): List<FieldIssue> =
        (ProfileValidator.bankAccount(fields.toInput(), others) as? Validation.Invalid)?.issues.orEmpty()
}
