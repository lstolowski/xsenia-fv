package pl.xsenia.fv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.config.ConfigState
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.home.HomeScreen
import pl.xsenia.fv.ui.lock.LockScreen
import pl.xsenia.fv.ui.settings.SettingsScreen
import pl.xsenia.fv.ui.setup.SetupScreen

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

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
    // Files may have been edited while the app was locked or in background.
    LaunchedEffect(Unit) { viewModel.reloadConfig() }

    val tokenSet by viewModel.tokenSet.collectAsStateWithLifecycle()
    val configState by viewModel.configState.collectAsStateWithLifecycle()
    if (tokenSet == null || configState == ConfigState.Loading) {
        LoadingScreen()
        return
    }

    val setupComplete = tokenSet == true && configState != ConfigState.FolderNotSelected
    // Decided once, so the setup screen stays visible until the user taps "Dalej".
    var showSetup by rememberSaveable { mutableStateOf(!setupComplete) }
    if (showSetup) {
        SetupScreen(viewModel = viewModel, canContinue = setupComplete, onContinue = { showSetup = false })
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(viewModel = viewModel, onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
