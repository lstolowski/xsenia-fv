package pl.xsenia.fv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.xml.InvoiceParseException
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.MirrorEntry
import pl.xsenia.fv.profile.ProfileSession

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

    init {
        viewModelScope.launch { _state.value = withContext(Dispatchers.IO) { load() } }
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
