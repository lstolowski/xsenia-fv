package pl.xsenia.fv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.xml.InvoiceParseException
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import pl.xsenia.fv.issue.SendState
import pl.xsenia.fv.issue.SimilarInvoice
import pl.xsenia.fv.issue.TakenNumbers
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.MirrorEntry
import pl.xsenia.fv.profile.ProfileSession
import java.io.IOException
import java.time.LocalDate

sealed interface InvoiceDetailsState {
    data object Loading : InvoiceDetailsState
    data object NotFound : InvoiceDetailsState

    /** [invoice] is null when the XML could not be parsed ([parseError]); [entry] has the KSeF metadata. */
    data class Loaded(val entry: MirrorEntry, val invoice: Invoice?, val parseError: String?) : InvoiceDetailsState
}

/** Invoice details read from the local mirror — works offline (US-005). */
class InvoiceDetailsViewModel(
    private val category: InvoiceCategory,
    private val ksefNumber: String,
    private val session: ProfileSession,
) : ViewModel() {

    private val _state = MutableStateFlow<InvoiceDetailsState>(InvoiceDetailsState.Loading)
    val state: StateFlow<InvoiceDetailsState> = _state.asStateFlow()

    /** "Wystaw podobną" replaces the new-invoice draft, which a new invoice being sent still uses (ADR 0011). */
    val sendingNewInvoice: StateFlow<Boolean> = session.sendService.state
        .map { it is SendState.Sending && it.unsentId == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** True once the copy is saved as the new-invoice draft — the form should be opened; [similarOpened] resets it. */
    private val _similarReady = MutableStateFlow(false)
    val similarReady: StateFlow<Boolean> = _similarReady.asStateFlow()

    /** True when the copy could not be saved; [similarFailureShown] resets it. */
    private val _similarFailed = MutableStateFlow(false)
    val similarFailed: StateFlow<Boolean> = _similarFailed.asStateFlow()

    private var preparingSimilar = false

    init {
        viewModelScope.launch { _state.value = withContext(Dispatchers.IO) { load() } }
    }

    /**
     * "Wystaw podobną" (US-015): the invoice copied into the new-invoice draft (replacing it, ADR 0011), with a buyer
     * missing among the counterparties added to them first. The form then opens the draft as usual.
     */
    fun issueSimilar() {
        val invoice = (_state.value as? InvoiceDetailsState.Loaded)?.invoice ?: return
        if (preparingSimilar || sendingNewInvoice.value) return
        preparingSimilar = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val profile = session.profile.profile.filterNotNull().first()
                    val ksefNumbers = session.syncService.mirror.entries(InvoiceCategory.SALES).map { it.metadata.invoiceNumber }
                    val taken = TakenNumbers.of(ksefNumbers, session.unsentInvoices.invoices.value, excludeUnsentId = null)
                    val similar = SimilarInvoice.of(invoice, profile, LocalDate.now(AppContainer.POLAND), taken)
                    // The counterparty first: the draft must not point to a buyer that is not saved.
                    similar.newCounterparty?.let { session.profile.saveCounterparty(it) }
                    session.issueStorage.saveDraft(similar.form)
                }
                _similarReady.value = true
            } catch (e: IOException) {
                _similarFailed.value = true
            } finally {
                preparingSimilar = false
            }
        }
    }

    fun similarOpened() {
        _similarReady.value = false
    }

    fun similarFailureShown() {
        _similarFailed.value = false
    }

    private fun load(): InvoiceDetailsState {
        val mirror = session.syncService.mirror
        val entry = mirror.entries(category).firstOrNull { it.metadata.ksefNumber == ksefNumber } ?: return InvoiceDetailsState.NotFound
        val xml = mirror.readXml(category, ksefNumber) ?: return InvoiceDetailsState.NotFound
        return try {
            InvoiceDetailsState.Loaded(entry, InvoiceXmlParser.parse(xml), parseError = null)
        } catch (e: InvoiceParseException) {
            InvoiceDetailsState.Loaded(entry, invoice = null, parseError = e.message)
        }
    }
}
