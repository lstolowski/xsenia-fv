package pl.xsenia.fv.ui.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.exchangerates.ExchangeRateResult
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.InvoiceNumbering
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import pl.xsenia.fv.issue.FormCheck
import pl.xsenia.fv.issue.InvoiceForm
import pl.xsenia.fv.issue.InvoiceFormCheck
import pl.xsenia.fv.issue.InvoiceFormDefaults
import pl.xsenia.fv.issue.LineForm
import pl.xsenia.fv.issue.Rejection
import pl.xsenia.fv.issue.SavedExchangeRate
import pl.xsenia.fv.issue.SendState
import pl.xsenia.fv.issue.TakenNumbers
import pl.xsenia.fv.issue.UnsentInvoice
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.profile.ProfileSession
import java.time.Instant
import java.time.LocalDate

enum class IssueStep { FORM, PREVIEW, STATUS }

/** Working NBP rate of the form (ADR 0008). */
sealed interface RateStatus {
    /** PLN invoice — no rate. */
    data object None : RateStatus

    data object Loading : RateStatus

    data class Ready(val rate: ExchangeRate) : RateStatus

    /** [problem] — not published or a failure; the rate is fetched again before the preview. */
    data class Unavailable(val problem: ExchangeRateResult) : RateStatus
}

data class NewInvoiceUiState(
    val form: InvoiceForm,
    /** Seller, bank accounts and counterparties of the active company; null while loading. */
    val profile: CompanyProfile?,
    val check: FormCheck?,
    val suggestedNumber: String,
    /** Set after "Podgląd" was tapped, so field errors are shown only then. */
    val showErrors: Boolean,
    /** Editing an unsent invoice (US-007); null for a new invoice. */
    val unsentId: String?,
    /** Why KSeF rejected this invoice the last time it was sent (Q4). */
    val rejection: Rejection?,
    val rate: RateStatus = RateStatus.None,
    /** Fetching the final rate before the preview. */
    val previewLoading: Boolean = false,
    /** Why the preview could not get the rate for the day of sending (ADR 0008, Q3). */
    val previewRateProblem: ExchangeRateResult? = null,
)

/** Rate in the preview: [final] — for the day of sending; [working] — the one shown in the form (null when there was none). */
data class PreviewRate(val final: ExchangeRate, val working: ExchangeRate?) {
    val changed: Boolean get() = working != null && working != final
}

/**
 * New sales invoice or edit of an unsent one: form → preview → send to KSeF / save without sending (US-006, US-007).
 *
 * @param initialUnsentId the unsent invoice to edit, or null for a new invoice (backed by the auto-saved draft)
 */
@OptIn(FlowPreview::class)
class NewInvoiceViewModel(container: AppContainer, private val session: ProfileSession, initialUnsentId: String?) : ViewModel() {

    private val exchangeRates = container.exchangeRates

    private val form = MutableStateFlow<InvoiceForm?>(null)
    private val ksefNumbers = MutableStateFlow<List<String>>(emptyList())
    private val showErrors = MutableStateFlow(false)
    private val unsentId = MutableStateFlow(initialUnsentId)
    private val rateStatus = MutableStateFlow<RateStatus>(RateStatus.None)
    private val previewLoading = MutableStateFlow(false)
    private val previewRateProblem = MutableStateFlow<ExchangeRateResult?>(null)

    /** The draft shown in the preview — exactly what [send] sends (with the final rate). */
    private var previewDraft: SalesInvoiceDraft? = null

    private val _step = MutableStateFlow(IssueStep.FORM)
    val step: StateFlow<IssueStep> = _step.asStateFlow()

    private val _preview = MutableStateFlow<Invoice?>(null)
    val preview: StateFlow<Invoice?> = _preview.asStateFlow()

    private val _previewRate = MutableStateFlow<PreviewRate?>(null)
    val previewRate: StateFlow<PreviewRate?> = _previewRate.asStateFlow()

    /** Emits true when the screen should close (saved without sending, deleted). */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    val sendState: StateFlow<SendState> = session.sendService.state
    val environment = container.environment

    val state: StateFlow<NewInvoiceUiState?> = combine(
        combine(form, session.profile.profile, rateStatus, ::Triple),
        combine(ksefNumbers, previewLoading, previewRateProblem, ::Triple),
        session.unsentInvoices.invoices,
        showErrors,
        unsentId,
    ) { (form, profile, rate), (ksef, loading, rateProblem), unsent, showErrors, unsentId ->
        if (form == null) return@combine null
        val taken = TakenNumbers.of(ksef, unsent, excludeUnsentId = unsentId)
        NewInvoiceUiState(
            form = form,
            profile = profile,
            check = profile?.let { InvoiceFormCheck.check(form, it, taken, today()) },
            suggestedNumber = InvoiceNumbering.suggest(form.deliveryDate, taken),
            showErrors = showErrors,
            unsentId = unsentId,
            rejection = unsent.firstOrNull { it.id == unsentId }?.rejection,
            rate = rate,
            previewLoading = loading,
            previewRateProblem = rateProblem,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            ksefNumbers.value = loadKsefNumbers()
            val profile = session.profile.profile.filterNotNull().first()
            val id = unsentId.value
            form.value = if (id != null) {
                // Unsent invoice: exactly as saved (dates included — an old issue date is handled in the preview, Q5).
                withContext(Dispatchers.IO) { session.unsentInvoices.get(id) }?.form
                    ?: InvoiceFormDefaults.newForm(profile, today(), taken())
            } else {
                withContext(Dispatchers.IO) { session.issueStorage.loadDraft() }
                    ?.let { InvoiceFormDefaults.refresh(it, today(), taken()) }
                    ?: InvoiceFormDefaults.newForm(profile, today(), taken())
            }
            val sending = sendState.value
            if (sending !is SendState.Idle && (id == null || id == sending.unsentIdOrNull())) _step.value = IssueStep.STATUS
        }
        // Data completed after the form was opened ("Uzupełnij", "+ Nowy kontrahent"): fill in what is still empty.
        viewModelScope.launch {
            session.profile.profile.filterNotNull().collect { profile -> update { InvoiceFormDefaults.fillMissing(it, profile) } }
        }
        // Working NBP rate: fetched again whenever the currency or the day it depends on changes (ADR 0008).
        viewModelScope.launch {
            form.filterNotNull().map { it.currency to it.rateReferenceDate }.distinctUntilChanged().collectLatest { refreshRate() }
        }
        // Auto-save after every change: into the unsent invoice being edited, otherwise into the new-invoice draft.
        viewModelScope.launch {
            form.filterNotNull().drop(1).debounce(400).collect { saveCurrent(it) }
        }
    }

    fun selectCounterparty(nip: String) = update { it.copy(counterpartyNip = nip) }

    fun selectBankAccount(number: String) = update { it.copy(bankAccountNumber = number) }

    fun selectCurrency(currency: InvoiceCurrency) = update { InvoiceFormDefaults.changeCurrency(it, currency, session.profile.profile.value) }

    fun retryRate() {
        viewModelScope.launch { refreshRate() }
    }

    fun setIssueDate(date: LocalDate) = update { it.copy(issueDate = date, datesEdited = true) }

    /** Changing the delivery date updates the suggested number unless the user typed their own. */
    fun setDeliveryDate(date: LocalDate) = update {
        val number = if (it.numberEdited) it.invoiceNumber else InvoiceNumbering.suggest(date, taken())
        it.copy(deliveryDate = date, invoiceNumber = number, datesEdited = true)
    }

    fun setInvoiceNumber(number: String) = update { it.copy(invoiceNumber = number, numberEdited = true) }

    fun useSuggestedNumber() = update { it.copy(invoiceNumber = InvoiceNumbering.suggest(it.deliveryDate, taken()), numberEdited = false) }

    fun updateLine(index: Int, line: LineForm) = update { form ->
        form.copy(lines = form.lines.mapIndexed { i, existing -> if (i == index) line else existing })
    }

    fun addLine() = update { it.copy(lines = it.lines + LineForm(unit = it.lines.lastOrNull()?.unit ?: "szt.")) }

    fun removeLine(index: Int) = update { form -> form.copy(lines = form.lines.filterIndexed { i, _ -> i != index }) }

    /**
     * Shows the invoice exactly as it will be sent: generated FA(3) XML read back by the parser.
     * A foreign currency invoice gets the final NBP rate — for sending today (ADR 0008, Q4); without it there is no preview.
     */
    fun openPreview() {
        showErrors.value = true
        previewRateProblem.value = null
        val current = form.value ?: return
        if (!current.currency.isForeign) return showPreview(current, finalRate = null)
        if (previewLoading.value) return
        viewModelScope.launch {
            previewLoading.value = true
            val result = exchangeRates.rateFor(current.currency, current.deliveryDate, today())
            previewLoading.value = false
            // The form may have changed while the rate was fetched.
            if (form.value != current) return@launch
            when (result) {
                is ExchangeRateResult.Found -> showPreview(current, result.rate)
                else -> previewRateProblem.value = result
            }
        }
    }

    private fun showPreview(current: InvoiceForm, finalRate: ExchangeRate?) {
        val profile = session.profile.profile.value ?: return
        val check = InvoiceFormCheck.check(current, profile, taken(), today(), exchangeRate = finalRate)
        val draft = check.draft
        if (!check.canPreview || draft == null) return
        previewDraft = draft
        _previewRate.value = finalRate?.let { PreviewRate(it, current.workingRate) }
        _preview.value = InvoiceXmlParser.parse(Fa3XmlWriter.write(draft, Instant.now()))
        _step.value = IssueStep.PREVIEW
    }

    /** Keeps the saved working rate while it fits; otherwise fetches it (rule: ADR 0008) and saves it with the form. */
    private suspend fun refreshRate() {
        val current = form.value ?: return
        val currency = current.currency
        if (!currency.isForeign) {
            rateStatus.value = RateStatus.None
            return
        }
        current.workingRate?.let {
            rateStatus.value = RateStatus.Ready(it)
            return
        }
        val referenceDate = current.rateReferenceDate
        rateStatus.value = RateStatus.Loading
        val result = exchangeRates.rateFor(currency, current.deliveryDate, current.issueDate)
        // The form may have changed meanwhile: the result is used only for the same currency and day.
        val latest = form.value ?: return
        if (latest.currency != currency || latest.rateReferenceDate != referenceDate) return
        rateStatus.value = when (result) {
            is ExchangeRateResult.Found -> {
                update { it.copy(exchangeRate = SavedExchangeRate.of(result.rate, referenceDate)) }
                RateStatus.Ready(result.rate)
            }
            else -> RateStatus.Unavailable(result)
        }
    }

    /** Q5: move an old issue date to today (the delivery date stays) and refresh the preview. */
    fun useTodayAsIssueDate() {
        setIssueDate(today())
        openPreview()
    }

    fun backToForm() {
        _step.value = IssueStep.FORM
    }

    /** Called after the biometric confirmation; the service checks the rate again for the day of sending. */
    fun send() {
        val current = form.value ?: return
        val draft = previewDraft ?: state.value?.check?.takeIf { it.canPreview }?.draft ?: return
        _step.value = IssueStep.STATUS
        session.sendService.send(draft, current, unsentId.value)
    }

    /** Saves the invoice on the phone without sending it to KSeF (US-007); no biometrics (P6). */
    fun saveWithoutSending() {
        val current = form.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val id = unsentId.value
                val existing = id?.let { session.unsentInvoices.get(it) }
                session.unsentInvoices.save(
                    existing?.copy(form = current, updatedAt = Instant.now()) ?: UnsentInvoice.create(current, Instant.now()),
                )
                if (id == null) session.issueStorage.clearDraft()
            }
            _closed.value = true
        }
    }

    fun deleteUnsent() {
        val id = unsentId.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.unsentInvoices.delete(id) }
            _closed.value = true
        }
    }

    fun retry() {
        val current = sendState.value
        if (current is SendState.Failed && current.pendingInKsef) session.sendService.resumeIfPending() else send()
    }

    /** After a rejection or failure the invoice is an unsent invoice — continue editing it. */
    fun fixInvoice() {
        when (val current = sendState.value) {
            is SendState.Rejected -> unsentId.value = current.unsentId
            is SendState.Failed -> current.unsentId?.let { unsentId.value = it }
            else -> Unit
        }
        session.sendService.acknowledge()
        _step.value = IssueStep.FORM
    }

    /** Leaves the result screen. */
    fun finish() {
        session.sendService.acknowledge()
    }

    private suspend fun saveCurrent(current: InvoiceForm) = withContext(Dispatchers.IO) {
        val id = unsentId.value
        if (id == null) {
            session.issueStorage.saveDraft(current)
        } else {
            session.unsentInvoices.get(id)?.let { session.unsentInvoices.save(it.copy(form = current, updatedAt = Instant.now())) }
        }
    }

    private suspend fun loadKsefNumbers(): List<String> = withContext(Dispatchers.IO) {
        session.syncService.mirror.entries(InvoiceCategory.SALES).map { it.metadata.invoiceNumber }
    }

    private fun taken(): List<String> = TakenNumbers.of(ksefNumbers.value, session.unsentInvoices.invoices.value, unsentId.value)

    private fun update(change: (InvoiceForm) -> InvoiceForm) {
        form.value = form.value?.let(change)
    }

    private fun today(): LocalDate = LocalDate.now(AppContainer.POLAND)

    private fun SendState.unsentIdOrNull(): String? = when (this) {
        is SendState.Sending -> unsentId
        is SendState.Rejected -> unsentId
        is SendState.Failed -> unsentId
        is SendState.RateUnavailable -> unsentId
        else -> null
    }
}
