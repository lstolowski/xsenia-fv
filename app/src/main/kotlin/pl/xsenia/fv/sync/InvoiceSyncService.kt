package pl.xsenia.fv.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.xsenia.fv.ksef.KsefAccess
import pl.xsenia.fv.ksef.KsefFailure
import pl.xsenia.fv.ksef.KsefFailureException
import pl.xsenia.fv.ksef.toKsefFailure
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.InvoiceMirror
import pl.xsenia.fv.mirror.InvoiceSynchronizer
import pl.xsenia.fv.mirror.KsefInvoiceSource
import pl.xsenia.fv.mirror.SyncProgress
import pl.xsenia.fv.security.KsefTokenStore
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class SyncUiState(
    val running: Boolean = false,
    val progress: SyncProgress? = null,
    val lastSyncAt: Instant? = null,
    val error: KsefFailure? = null,
    val invoiceCounts: Map<InvoiceCategory, Int> = emptyMap(),
    /** Incremented after every change of the mirror, so lists know when to reload. */
    val mirrorVersion: Int = 0,
)

/**
 * Runs invoice synchronization in the application scope (not tied to a screen), at most one at a time.
 * Automatic sync on app start happens at most every [autoSyncInterval] (MF recommends ≥ 15 min).
 * One service per company profile: [nip] is the KSeF context, [mirror] lives in the profile directory (ADR 0006).
 */
class InvoiceSyncService(
    private val scope: CoroutineScope,
    val mirror: InvoiceMirror,
    private val readToken: suspend () -> String?,
    private val synchronizer: Lazy<InvoiceSynchronizer>,
    private val clock: Clock = Clock.systemUTC(),
    private val autoSyncInterval: Duration = Duration.ofMinutes(15),
) {

    constructor(
        scope: CoroutineScope,
        ksef: KsefAccess,
        tokenStore: KsefTokenStore,
        nip: String,
        mirror: InvoiceMirror,
        initialDate: Instant,
    ) : this(
        scope,
        mirror,
        readToken = tokenStore::read,
        synchronizer = lazy { InvoiceSynchronizer(KsefInvoiceSource(ksef.invoiceClient(nip)), mirror, initialDate, Clock.systemUTC()) },
    )

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    /** The running synchronization; [clearMirror] cancels it. */
    private var syncJob: Job? = null

    /** Held while synchronizing and while the mirror is deleted, so the two never overlap. */
    private val mirrorLock = Mutex()

    init {
        scope.launch { refreshFromMirror() }
    }

    fun syncIfStale() {
        val lastSync = _state.value.lastSyncAt ?: mirror.syncState().lastSyncAt
        if (lastSync == null || Duration.between(lastSync, clock.instant()) >= autoSyncInterval) syncNow()
    }

    fun syncNow() {
        if (_state.value.running) return
        _state.update { it.copy(running = true, error = null, progress = null) }
        // ATOMIC: a job cancelled by clearMirror() before it started must still reach the `finally` that resets `running`.
        syncJob = scope.launch(start = CoroutineStart.ATOMIC) {
            var error: KsefFailure? = null
            try {
                // TODO CODE REVIEWED: Unmapped exceptions are rethrown before running is reset; e.g. a corrupted mirror index can leave this service permanently marked running and disable future sync attempts. Reset state in a finally path and surface a generic failure.
                error = try {
                    mirrorLock.withLock { synchronize() }
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.toKsefFailure() ?: throw e
                }
                refreshFromMirror()
            } finally {
                // Also after a cancellation by clearMirror().
                _state.update { it.copy(running = false, progress = null, error = error) }
            }
        }
    }

    /**
     * Deletes the local copy of the invoices. A running synchronization is cancelled first, so it cannot write to the
     * emptied mirror (it would leave the mirror incomplete but marked as synchronized); the next one downloads everything.
     */
    suspend fun clearMirror() {
        syncJob?.cancelAndJoin()
        mirrorLock.withLock {
            withContext(Dispatchers.IO) { mirror.clear() }
            refreshFromMirror()
        }
    }

    private suspend fun synchronize() {
        if (readToken() == null) throw KsefFailureException(KsefFailure.MissingToken)

        withContext(Dispatchers.IO) {
            synchronizer.value.sync { progress -> _state.update { it.copy(progress = progress) } }
        }
    }

    private suspend fun refreshFromMirror() {
        val (counts, lastSync) = withContext(Dispatchers.IO) {
            InvoiceCategory.entries.associateWith { mirror.entries(it).size } to mirror.syncState().lastSyncAt
        }
        _state.update { it.copy(invoiceCounts = counts, lastSyncAt = lastSync, mirrorVersion = it.mirrorVersion + 1) }
    }
}
