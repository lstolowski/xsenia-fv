package pl.xsenia.fv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.profile.ProfileState
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.lock.LockScreen
import pl.xsenia.fv.ui.setup.SetupScreen

/** Root of the UI: lock screen, first-run setup, then the main navigation. */
@Composable
fun XseniaApp(
    container: AppContainer,
    biometricAvailable: Boolean,
    onUnlockRequest: () -> Unit,
) {
    val locked by container.appLock.locked.collectAsStateWithLifecycle()
    if (locked) {
        LockScreen(biometricAvailable = biometricAvailable, onUnlock = onUnlockRequest)
        return
    }

    val viewModel: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(container) } })

    val tokenSet by viewModel.tokenSet.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    if (tokenSet == null || profile == ProfileState.Loading) {
        LoadingScreen()
        return
    }

    // The company profile comes from the NIP of the KSeF token (ADR 0006).
    val session = (profile as? ProfileState.Active)?.session
    // Decided once, so the setup screen stays visible until the user taps "Dalej".
    var showSetup by rememberSaveable { mutableStateOf(session == null) }
    if (showSetup || session == null) {
        SetupScreen(viewModel = viewModel, canContinue = session != null, onContinue = { showSetup = false })
        return
    }

    // Another company (token of another NIP) gets fresh screens and view models.
    key(session.nip) { MainScaffold(container = container, viewModel = viewModel, session = session) }
}
