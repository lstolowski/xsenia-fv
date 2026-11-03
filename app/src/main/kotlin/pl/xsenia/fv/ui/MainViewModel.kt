package pl.xsenia.fv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.ksef.ConnectionCheckResult
import pl.xsenia.fv.profile.ProfileState
import pl.xsenia.fv.security.TokenUnreadableException
import pl.xsenia.fv.security.VerifiedTokenSave

/** [tokenNotSaved] — the result is the check of a new token that was rejected, so the previous configuration stays. */
data class ConnectionUiState(val checking: Boolean = false, val result: ConnectionCheckResult? = null, val tokenNotSaved: Boolean = false)

/** State and actions shared by the setup, home and settings screens. */
class MainViewModel(private val container: AppContainer) : ViewModel() {

    val environment = container.environment

    /** null until loaded from DataStore. */
    val tokenSet: StateFlow<Boolean?> = container.tokenStore.isSet.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** NIP of the KSeF token = NIP of my company (ADR 0006). */
    val tokenNip: StateFlow<String?> = container.tokenStore.tokenNip.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile: StateFlow<ProfileState> = container.profiles.state

    /** The monthly "no sales invoice" reminder switch (US-017). */
    val reminderEnabled: StateFlow<Boolean> = container.appSettings.reminderEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { container.appSettings.setReminderEnabled(enabled) }
    }

    private val _connection = MutableStateFlow(ConnectionUiState())
    val connection: StateFlow<ConnectionUiState> = _connection.asStateFlow()

    private val verifiedTokenSave = VerifiedTokenSave(
        check = container.connectionChecker::check,
        store = container.tokenStore::save,
        afterSave = { nip -> container.ksefAccess.tokenManager(nip).invalidate() },
    )

    /** Verifies the token (it must contain the company NIP) in KSeF and saves it only when KSeF accepts it. */
    fun saveToken(ksefToken: String) {
        viewModelScope.launch {
            if (_connection.value.checking) return@launch
            _connection.value = ConnectionUiState(checking = true)
            val result = verifiedTokenSave.save(ksefToken)
            _connection.value = ConnectionUiState(result = result, tokenNotSaved = result != ConnectionCheckResult.Success)
        }
    }

    fun checkConnection() {
        viewModelScope.launch { runConnectionCheck() }
    }

    /** Removes the downloaded invoices of the active company and temporary files. */
    fun clearCache() {
        viewModelScope.launch {
            container.profiles.active?.clearMirror()
            withContext(Dispatchers.IO) { container.localCache.clear() }
        }
    }

    private suspend fun runConnectionCheck() {
        if (_connection.value.checking) return
        _connection.value = ConnectionUiState(checking = true)
        val result = try {
            container.connectionChecker.check(container.tokenStore.tokenNip(), container.tokenStore.read())
        } catch (e: TokenUnreadableException) {
            ConnectionCheckResult.TokenUnreadable
        }
        _connection.value = ConnectionUiState(checking = false, result = result)
    }
}
