package pl.xsenia.fv.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.config.ConfigState
import pl.xsenia.fv.ksef.ConnectionCheckResult

data class ConnectionUiState(val checking: Boolean = false, val result: ConnectionCheckResult? = null)

/** State and actions shared by the setup, home and settings screens. */
class MainViewModel(private val container: AppContainer) : ViewModel() {

    val environment = container.environment

    /** null until loaded from DataStore. */
    val tokenSet: StateFlow<Boolean?> = container.tokenStore.isSet.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tokenNip: StateFlow<String?> = container.tokenStore.tokenNip.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val configState: StateFlow<ConfigState> = container.configRepository.state

    private val _connection = MutableStateFlow(ConnectionUiState())
    val connection: StateFlow<ConnectionUiState> = _connection.asStateFlow()

    fun reloadConfig() {
        viewModelScope.launch { container.configRepository.reload() }
    }

    fun selectFolder(treeUri: Uri) {
        viewModelScope.launch { container.configRepository.selectFolder(treeUri) }
    }

    /** Saves the token and immediately verifies it in KSeF. */
    fun saveToken(ksefToken: String) {
        viewModelScope.launch {
            container.tokenStore.save(ksefToken)
            runConnectionCheck()
        }
    }

    fun checkConnection() {
        viewModelScope.launch { runConnectionCheck() }
    }

    val sync = container.syncService.state

    fun syncIfStale() = container.syncService.syncIfStale()

    fun syncNow() = container.syncService.syncNow()

    fun clearCache() {
        container.localCache.clear()
        container.syncService.onMirrorCleared()
    }

    private suspend fun runConnectionCheck() {
        if (_connection.value.checking) return
        _connection.value = ConnectionUiState(checking = true)
        val nip = (configState.value as? ConfigState.Loaded)?.config?.company?.nip
        val result = container.connectionChecker.check(nip, container.tokenStore.read())
        _connection.value = ConnectionUiState(checking = false, result = result)
    }
}
