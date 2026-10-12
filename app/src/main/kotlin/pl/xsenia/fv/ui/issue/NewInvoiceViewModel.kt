package pl.xsenia.fv.ui.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.config.ConfigState
import pl.xsenia.fv.invoice.config.InvoicingConfig
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.InvoiceNumbering
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import pl.xsenia.fv.issue.FormCheck
import pl.xsenia.fv.issue.InvoiceForm
import pl.xsenia.fv.issue.InvoiceFormCheck
import pl.xsenia.fv.issue.InvoiceFormDefaults
import pl.xsenia.fv.issue.LineForm
import pl.xsenia.fv.issue.SendState
import pl.xsenia.fv.mirror.InvoiceCategory
import java.time.Instant
import java.time.LocalDate

enum class IssueStep { FORM, PREVIEW, STATUS }

data class NewInvoiceUiState(
    val form: InvoiceForm,
    val config: InvoicingConfig?,
    val check: FormCheck?,
    val suggestedNumber: String,
    /** Set after "Podgląd" was tapped, so field errors are shown only then. */
    val showErrors: Boolean,
)

/** New sales invoice: form → preview → sending (US-006). */
@OptIn(FlowPreview::class)
class NewInvoiceViewModel(private val container: AppContainer) : ViewModel() {

    private val form = MutableStateFlow<InvoiceForm?>(null)
    private val existingNumbers = MutableStateFlow<List<String>>(emptyList())
    private val showErrors = MutableStateFlow(false)

    private val _step = MutableStateFlow(IssueStep.FORM)
    val step: StateFlow<IssueStep> = _step.asStateFlow()

    private val _preview = MutableStateFlow<Invoice?>(null)
    val preview: StateFlow<Invoice?> = _preview.asStateFlow()

    val sendState: StateFlow<SendState> = container.sendService.state
    val environment = container.environment

    val state: StateFlow<NewInvoiceUiState?> = combine(
        form,
        container.configRepository.state,
        existingNumbers,
        showErrors,
    ) { form, configState, numbers, showErrors ->
        if (form == null) return@combine null
        val config = (configState as? ConfigState.Loaded)?.config
        NewInvoiceUiState(
            form = form,
            config = config,
            check = config?.let { InvoiceFormCheck.check(form, it, numbers, today()) },
            suggestedNumber = InvoiceNumbering.suggest(form.deliveryDate, numbers),
            showErrors = showErrors,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            existingNumbers.value = withContext(Dispatchers.IO) {
                container.syncService.mirror.entries(InvoiceCategory.SALES).map { it.metadata.invoiceNumber }
            }
            val config = (container.configRepository.state.first { it != ConfigState.Loading } as? ConfigState.Loaded)?.config
            val draft = withContext(Dispatchers.IO) { container.issueStorage.loadDraft() }
            form.value = draft?.let { InvoiceFormDefaults.refresh(it, today(), existingNumbers.value) }
                ?: InvoiceFormDefaults.newForm(config, today(), existingNumbers.value)
            if (sendState.value !is SendState.Idle) _step.value = IssueStep.STATUS
        }
        // Save the draft after every change (debounced).
        viewModelScope.launch {
            form.filterNotNull().debounce(400).collect { withContext(Dispatchers.IO) { container.issueStorage.saveDraft(it) } }
        }
    }

    fun selectCounterparty(nip: String) = update { it.copy(counterpartyNip = nip) }

    fun selectBankAccount(number: String) = update { it.copy(bankAccountNumber = number) }

    fun setIssueDate(date: LocalDate) = update { it.copy(issueDate = date, datesEdited = true) }

    /** Changing the delivery date updates the suggested number unless the user typed their own. */
    fun setDeliveryDate(date: LocalDate) = update {
        val number = if (it.numberEdited) it.invoiceNumber else InvoiceNumbering.suggest(date, existingNumbers.value)
        it.copy(deliveryDate = date, invoiceNumber = number, datesEdited = true)
    }

    fun setInvoiceNumber(number: String) = update { it.copy(invoiceNumber = number, numberEdited = true) }

    fun useSuggestedNumber() = update {
        it.copy(invoiceNumber = InvoiceNumbering.suggest(it.deliveryDate, existingNumbers.value), numberEdited = false)
    }

    fun updateLine(index: Int, line: LineForm) = update { form ->
        form.copy(lines = form.lines.mapIndexed { i, existing -> if (i == index) line else existing })
    }

    fun addLine() = update { it.copy(lines = it.lines + LineForm(unit = it.lines.lastOrNull()?.unit ?: "szt.")) }

    fun removeLine(index: Int) = update { form -> form.copy(lines = form.lines.filterIndexed { i, _ -> i != index }) }

    /** Shows the invoice exactly as it will be sent: generated FA(3) XML read back by the parser. */
    fun openPreview() {
        showErrors.value = true
        val check = state.value?.check ?: return
        val draft = check.draft
        if (!check.canPreview || draft == null) return
        _preview.value = InvoiceXmlParser.parse(Fa3XmlWriter.write(draft, Instant.now()))
        _step.value = IssueStep.PREVIEW
    }

    fun backToForm() {
        _step.value = IssueStep.FORM
    }

    /** Called after the biometric confirmation. */
    fun send() {
        val draft = state.value?.check?.takeIf { it.canPreview }?.draft ?: return
        _step.value = IssueStep.STATUS
        container.sendService.send(draft)
    }

    fun retry() {
        val current = sendState.value
        if (current is SendState.Failed && current.pendingInKsef) container.sendService.resumeIfPending() else send()
    }

    /** After a rejection: back to the form with the same data. */
    fun fixInvoice() {
        container.sendService.acknowledge()
        _step.value = IssueStep.FORM
    }

    /** After success: start a fresh form (the draft was removed by the send service). */
    fun finish() {
        container.sendService.acknowledge()
        val config = state.value?.config
        viewModelScope.launch {
            existingNumbers.value = withContext(Dispatchers.IO) {
                container.syncService.mirror.entries(InvoiceCategory.SALES).map { it.metadata.invoiceNumber }
            }
            form.value = InvoiceFormDefaults.newForm(config, today(), existingNumbers.value)
            showErrors.value = false
            _step.value = IssueStep.FORM
        }
    }


    private fun update(change: (InvoiceForm) -> InvoiceForm) {
        form.value = form.value?.let(change)
    }

    private fun today(): LocalDate = LocalDate.now(AppContainer.POLAND)
}
