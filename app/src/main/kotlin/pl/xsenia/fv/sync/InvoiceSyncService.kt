package pl.xsenia.fv.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.config.ConfigRepository
import pl.xsenia.fv.config.ConfigState
import pl.xsenia.fv.ksef.KsefAuthenticationException
import pl.xsenia.fv.ksef.KsefException
import pl.xsenia.fv.ksef.KsefRateLimitException
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.invoices.KsefInvoiceClient
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.InvoiceMirror
import pl.xsenia.fv.mirror.InvoiceSynchronizer
import pl.xsenia.fv.mirror.KsefInvoiceSource
import pl.xsenia.fv.mirror.SyncProgress
import pl.xsenia.fv.security.KsefTokenStore
import io.ktor.client.HttpClient
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant

sealed interface SyncError {
    data object MissingToken : SyncError
    data object MissingCompany : SyncError
    data class RateLimited(val retryAfterSeconds: Long?) : SyncError
    data class AuthenticationFailed(val statusCode: Int, val description: String) : SyncError
    data class Network(val message: String?) : SyncError
    data class Ksef(val message: String?) : SyncError
}

data class SyncUiState(
    val running: Boolean = false,
    val progress: SyncProgress? = null,
    val lastSyncAt: Instant? = null,
    val error: SyncError? = null,
    val invoiceCounts: Map<InvoiceCategory, Int> = emptyMap(),
    /** Incremented after every change of the mirror, so lists know when to reload. */
    val mirrorVersion: Int = 0,
)

/**
 * Runs invoice synchronization in the application scope (not tied to a screen), at most one at a time.
 * Automatic sync on app start happens at most every [autoSyncInterval] (MF recommends ≥ 15 min).
 */
class InvoiceSyncService(
    private val scope: CoroutineScope,
    private val httpClient: HttpClient,
    private val publicKeys: KsefPublicKeyProvider,
    private val authenticator: KsefAuthenticator,
    private val tokenStore: KsefTokenStore,
    private val configRepository: ConfigRepository,
    val mirror: InvoiceMirror,
    private val initialDate: Instant,
    private val clock: Clock = Clock.systemUTC(),
    private val autoSyncInterval: Duration = Duration.ofMinutes(15),
) {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    // One synchronizer (and token manager) per company NIP; normally there is just one.
    private var synchronizerNip: String? = null
    private var synchronizer: InvoiceSynchronizer? = null

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
        scope.launch {
            val error = try {
                synchronize()
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: KsefRateLimitException) {
                SyncError.RateLimited(e.retryAfter?.inWholeSeconds)
            } catch (e: KsefAuthenticationException) {
                SyncError.AuthenticationFailed(e.statusCode, e.description)
            } catch (e: KsefException) {
                SyncError.Ksef(e.message)
            } catch (e: IOException) {
                SyncError.Network(e.message)
            } catch (e: MissingSetupException) {
                e.error
            }
            refreshFromMirror()
            _state.update { it.copy(running = false, progress = null, error = error) }
        }
    }

    /** Call after the local cache (mirror) was deleted. */
    fun onMirrorCleared() {
        scope.launch { refreshFromMirror() }
    }

    private suspend fun synchronize() {
        val config = configRepository.state.first { it != ConfigState.Loading }
        val nip = (config as? ConfigState.Loaded)?.config?.company?.nip ?: throw MissingSetupException(SyncError.MissingCompany)
        if (tokenStore.read() == null) throw MissingSetupException(SyncError.MissingToken)

        withContext(Dispatchers.IO) {
            synchronizerFor(nip).sync { progress -> _state.update { it.copy(progress = progress) } }
        }
    }

    private fun synchronizerFor(nip: String): InvoiceSynchronizer {
        synchronizer?.takeIf { synchronizerNip == nip }?.let { return it }
        val tokens = KsefTokenManager(authenticator, nip, ksefToken = {
            tokenStore.read() ?: throw MissingSetupException(SyncError.MissingToken)
        })
        val source = KsefInvoiceSource(KsefInvoiceClient(httpClient, tokens, publicKeys))
        return InvoiceSynchronizer(source, mirror, initialDate, clock).also {
            synchronizer = it
            synchronizerNip = nip
        }
    }

    private suspend fun refreshFromMirror() {
        val (counts, lastSync) = withContext(Dispatchers.IO) {
            InvoiceCategory.entries.associateWith { mirror.entries(it).size } to mirror.syncState().lastSyncAt
        }
        _state.update { it.copy(invoiceCounts = counts, lastSyncAt = lastSync, mirrorVersion = it.mirrorVersion + 1) }
    }

    private class MissingSetupException(val error: SyncError) : Exception()
}
