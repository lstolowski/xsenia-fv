package pl.xsenia.fv.issue

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
import pl.xsenia.fv.ksef.sending.SubmittedInvoice
import pl.xsenia.fv.ksef.toKsefFailure
import java.time.Clock

sealed interface SendState {
    data object Idle : SendState

    /** [submitted] — KSeF accepted the request and is processing the invoice. */
    data class Sending(val invoiceNumber: String, val submitted: Boolean, val unsentId: String?) : SendState

    data class Accepted(val invoiceNumber: String, val ksefNumber: String, val upoSaved: Boolean) : SendState

    /** The invoice was kept as unsent invoice [unsentId] together with the [rejection] reason (Q4). */
    data class Rejected(val invoiceNumber: String, val rejection: Rejection, val unsentId: String) : SendState

    /**
     * Communication problem. With [pendingInKsef] the invoice already reached KSeF — retrying only checks its status,
     * it never sends it again; otherwise the invoice was kept as unsent invoice [unsentId] (Q4).
     */
    data class Failed(val invoiceNumber: String, val failure: KsefFailure, val pendingInKsef: Boolean, val unsentId: String?) : SendState
}

/** KSeF operations used by [InvoiceSendService] — an interface so the state transitions can be tested offline. */
interface InvoiceSendBackend {
    suspend fun send(nip: String, invoiceXml: ByteArray, onSubmitted: suspend (SubmittedInvoice) -> Unit): SentInvoice

    suspend fun awaitResult(nip: String, submitted: SubmittedInvoice): SentInvoice
}

class KsefSendBackend(private val ksef: KsefAccess) : InvoiceSendBackend {
    override suspend fun send(nip: String, invoiceXml: ByteArray, onSubmitted: suspend (SubmittedInvoice) -> Unit) =
        ksef.invoiceSender(nip).send(invoiceXml, onSubmitted = onSubmitted)

    override suspend fun awaitResult(nip: String, submitted: SubmittedInvoice) = ksef.invoiceSender(nip).awaitResult(submitted)
}

/**
 * Sends sales invoices to KSeF in the application scope (US-006, US-007); one at a time.
 *
 * Outcome handling:
 * - accepted — UPO saved; the source (new-invoice draft or unsent invoice) removed; [onAccepted] (sync) called,
 * - rejected by KSeF, or failed before reaching KSeF — kept as an unsent invoice (with the rejection reason),
 * - failed after reaching KSeF — kept as pending; the status is checked again on retry / next app start.
 */
class InvoiceSendService(
    private val scope: CoroutineScope,
    private val backend: InvoiceSendBackend,
    private val storage: IssueStorage,
    private val unsent: UnsentInvoiceRepository,
    private val onAccepted: () -> Unit,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val _state = MutableStateFlow<SendState>(SendState.Idle)
    val state: StateFlow<SendState> = _state.asStateFlow()

    private val busy get() = _state.value is SendState.Sending

    /**
     * @param form the form the draft was built from — saved as an unsent invoice when sending does not succeed
     * @param unsentId the unsent invoice being sent, or null for a new invoice (from the draft)
     */
    fun send(draft: SalesInvoiceDraft, form: InvoiceForm, unsentId: String?) {
        if (busy) return
        if (storage.loadPending() != null) return resumeIfPending()
        _state.value = SendState.Sending(draft.invoiceNumber, submitted = false, unsentId = unsentId)
        scope.launch {
            run(draft.invoiceNumber, form, unsentId) {
                val xml = Fa3XmlWriter.write(draft, clock.instant())
                backend.send(draft.seller.nip, xml) { submitted ->
                    storage.savePending(PendingSend(submitted, draft.invoiceNumber, draft.seller.nip, form, unsentId))
                    _state.value = SendState.Sending(draft.invoiceNumber, submitted = true, unsentId = unsentId)
                }
            }
        }
    }

    /** Finishes an invoice sent before the app was closed (or after a communication error). */
    fun resumeIfPending() {
        if (busy) return
        val pending = storage.loadPending() ?: return
        _state.value = SendState.Sending(pending.invoiceNumber, submitted = true, unsentId = pending.unsentId)
        scope.launch {
            run(pending.invoiceNumber, pending.form, pending.unsentId) { backend.awaitResult(pending.nip, pending.submitted) }
        }
    }

    /** Back to idle after the user saw the result. */
    fun acknowledge() {
        if (!busy) _state.value = SendState.Idle
    }

    private suspend fun run(invoiceNumber: String, form: InvoiceForm?, unsentId: String?, block: suspend () -> SentInvoice) {
        _state.value = try {
            val sent = withContext(ioDispatcher) { block() }
            withContext(ioDispatcher) {
                sent.upo?.let { storage.saveUpo(sent.ksefNumber, it) }
                storage.clearPending()
                if (unsentId != null) unsent.delete(unsentId) else storage.clearDraft()
            }
            onAccepted()
            SendState.Accepted(invoiceNumber, sent.ksefNumber, upoSaved = sent.upo != null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: KsefInvoiceRejectedException) {
            val rejection = Rejection(e.statusCode, e.description, e.details, e.extensions["originalKsefNumber"])
            withContext(ioDispatcher) { storage.clearPending() }
            SendState.Rejected(invoiceNumber, rejection, keepAsUnsent(form, unsentId, rejection))
        } catch (e: Exception) {
            val failure = e.toKsefFailure() ?: KsefFailure.Ksef(e.message)
            val pendingInKsef = withContext(ioDispatcher) { storage.loadPending() != null }
            SendState.Failed(
                invoiceNumber,
                failure,
                pendingInKsef,
                unsentId = if (pendingInKsef) unsentId else keepAsUnsent(form, unsentId, rejection = null),
            )
        }
    }

    /** Saves the invoice as unsent (new one, or update of [unsentId]) and returns its id. */
    private suspend fun keepAsUnsent(form: InvoiceForm?, unsentId: String?, rejection: Rejection?): String = withContext(ioDispatcher) {
        val now = clock.instant()
        val existing = unsentId?.let { unsent.get(it) }
        val invoice = when {
            existing != null -> existing.copy(form = form ?: existing.form, updatedAt = now, rejection = rejection)
            else -> UnsentInvoice.create(requireNotNull(form) { "form needed to keep the invoice as unsent" }, now, rejection)
        }
        unsent.save(invoice)
        if (unsentId == null) storage.clearDraft()
        invoice.id
    }
}
