package pl.xsenia.fv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.ksef.ConnectionCheckResult
import pl.xsenia.fv.profile.ProfileState

data class ConnectionUiState(val checking: Boolean = false, val result: ConnectionCheckResult? = null)

/** State and actions shared by the setup, home and settings screens. */
class MainViewModel(private val container: AppContainer) : ViewModel() {

    val environment = container.environment

    /** null until loaded from DataStore. */
    val tokenSet: StateFlow<Boolean?> = container.tokenStore.isSet.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** NIP of the KSeF token = NIP of my company (ADR 0006). */
    val tokenNip: StateFlow<String?> = container.tokenStore.tokenNip.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile: StateFlow<ProfileState> = container.profiles.state

    private val _connection = MutableStateFlow(ConnectionUiState())
    val connection: StateFlow<ConnectionUiState> = _connection.asStateFlow()

    /** Saves the token (it must contain the company NIP) and immediately verifies it in KSeF. */
    fun saveToken(ksefToken: String) {
        viewModelScope.launch {
            container.tokenStore.save(ksefToken)
            runConnectionCheck()
        }
    }

    fun checkConnection() {
        viewModelScope.launch { runConnectionCheck() }
    }

    /** Removes the downloaded invoices of the active company and temporary files. */
    fun clearCache() {
        container.profiles.active?.clearMirror()
        container.localCache.clear()
    }

    private suspend fun runConnectionCheck() {
        if (_connection.value.checking) return
        _connection.value = ConnectionUiState(checking = true)
        val result = container.connectionChecker.check(container.tokenStore.tokenNip(), container.tokenStore.read())
        _connection.value = ConnectionUiState(checking = false, result = result)
    }
}
