package pl.xsenia.fv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.R
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.profile.ProfileState
import pl.xsenia.fv.ui.company.CompanyFormScreen
import pl.xsenia.fv.ui.company.CompanyFormViewModel
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.lock.LockScreen
import pl.xsenia.fv.ui.setup.SetupScreen

/** Root of the UI: lock screen, first-run setup (token → „Moja firma”, ZAD-025), then the main navigation. */
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
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    if (tokenSet == null || profileState == ProfileState.Loading) {
        LoadingScreen()
        return
    }

    // The company profile comes from the NIP of the KSeF token (ADR 0006).
    val session = (profileState as? ProfileState.Active)?.session
    // Step 1, the token: decided once, so it stays visible until the user taps "Dalej".
    var showTokenStep by rememberSaveable { mutableStateOf(session == null) }
    if (showTokenStep || session == null) {
        SetupScreen(viewModel = viewModel, canContinue = session != null, onContinue = { showTokenStep = false })
        return
    }

    // Another company (token of another NIP) gets fresh screens and view models.
    key(session.nip) {
        CompanyOrMain(container, viewModel, session, onBackToToken = { showTokenStep = true })
    }
}

/** Step 2, „Moja firma”, until the company is saved — also after an update from the version with JSON files. */
@Composable
private fun CompanyOrMain(container: AppContainer, viewModel: MainViewModel, session: ProfileSession, onBackToToken: () -> Unit) {
    val profile by session.profile.profile.collectAsStateWithLifecycle()
    val current = profile
    when {
        current == null -> LoadingScreen()
        current.company == null -> {
            val companyViewModel: CompanyFormViewModel = viewModel(
                key = "setup-company-${session.nip}",
                // The NIP is known from the token, so the White List data is fetched right away.
                factory = viewModelFactory { initializer { CompanyFormViewModel(session, container.companyLookup, autoLookup = true) } },
            )
            CompanyFormScreen(
                viewModel = companyViewModel,
                onClose = onBackToToken,
                onAddAccount = null,
                onEditAccount = null,
                title = stringResource(R.string.setup_company_title),
                intro = stringResource(R.string.setup_company_intro),
                // Saving the company shows the main screen by itself (company != null).
                onSaved = {},
            )
        }
        else -> MainScaffold(container = container, viewModel = viewModel, session = session)
    }
}
