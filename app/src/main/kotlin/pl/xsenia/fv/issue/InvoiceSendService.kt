package pl.xsenia.fv.issue

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.ksef.KsefAccess
import pl.xsenia.fv.ksef.KsefFailure
import pl.xsenia.fv.ksef.KsefInvoiceRejectedException
import pl.xsenia.fv.ksef.sending.SentInvoice
import pl.xsenia.fv.ksef.toKsefFailure
import pl.xsenia.fv.sync.InvoiceSyncService
import java.time.Clock

sealed interface SendState {
    data object Idle : SendState

    /** [submitted] — KSeF accepted the request and is processing the invoice. */
    data class Sending(val invoiceNumber: String, val submitted: Boolean) : SendState

    data class Accepted(val invoiceNumber: String, val ksefNumber: String, val upoSaved: Boolean) : SendState

    data class Rejected(
        val invoiceNumber: String,
        val statusCode: Int,
        val description: String,
        val details: List<String>,
        val originalKsefNumber: String?,
    ) : SendState

    /**
     * Communication problem. With [pendingInKsef] the invoice already reached KSeF —
     * retrying only checks its status, it never sends it again.
     */
    data class Failed(val invoiceNumber: String, val failure: KsefFailure, val pendingInKsef: Boolean) : SendState
}

/** Sends new sales invoices to KSeF in the application scope (US-006); one at a time. */
class InvoiceSendService(
    private val scope: CoroutineScope,
    private val ksef: KsefAccess,
    private val storage: IssueStorage,
    private val syncService: InvoiceSyncService,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val _state = MutableStateFlow<SendState>(SendState.Idle)
    val state: StateFlow<SendState> = _state.asStateFlow()

    private val busy get() = _state.value is SendState.Sending

    fun send(draft: SalesInvoiceDraft) {
        if (busy) return
        if (storage.loadPending() != null) return resumeIfPending()
        _state.value = SendState.Sending(draft.invoiceNumber, submitted = false)
        scope.launch {
            run(draft.invoiceNumber, pendingInKsef = { storage.loadPending() != null }) {
                val xml = Fa3XmlWriter.write(draft, clock.instant())
                ksef.invoiceSender(draft.seller.nip).send(xml) { submitted ->
                    storage.savePending(PendingSend(submitted, draft.invoiceNumber, draft.seller.nip))
                    _state.value = SendState.Sending(draft.invoiceNumber, submitted = true)
                }
            }
        }
    }

    /** Finishes an invoice sent before the app was closed (or after a communication error). */
    fun resumeIfPending() {
        if (busy) return
        val pending = storage.loadPending() ?: return
        _state.value = SendState.Sending(pending.invoiceNumber, submitted = true)
        scope.launch {
            run(pending.invoiceNumber, pendingInKsef = { true }) {
                ksef.invoiceSender(pending.nip).awaitResult(pending.submitted)
            }
        }
    }

    /** Back to idle after the user saw the result. */
    fun acknowledge() {
        if (!busy) _state.value = SendState.Idle
    }

    private suspend fun run(invoiceNumber: String, pendingInKsef: () -> Boolean, block: suspend () -> SentInvoice) {
        _state.value = try {
            val sent = withContext(Dispatchers.IO) { block() }
            val upoSaved = sent.upo?.let { withContext(Dispatchers.IO) { storage.saveUpo(sent.ksefNumber, it) }; true } ?: false
            storage.clearPending()
            storage.clearDraft()
            syncService.syncNow()
            SendState.Accepted(invoiceNumber, sent.ksefNumber, upoSaved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: KsefInvoiceRejectedException) {
            storage.clearPending()
            SendState.Rejected(invoiceNumber, e.statusCode, e.description, e.details, e.extensions["originalKsefNumber"])
        } catch (e: Exception) {
            SendState.Failed(invoiceNumber, e.toKsefFailure() ?: KsefFailure.Ksef(e.message), pendingInKsef())
        }
    }
}
