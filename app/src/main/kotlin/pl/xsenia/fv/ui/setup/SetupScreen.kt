package pl.xsenia.fv.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.R
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.MainViewModel
import pl.xsenia.fv.ui.components.TokenSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: MainViewModel, canContinue: Boolean, onContinue: () -> Unit) {
    val tokenSet by viewModel.tokenSet.collectAsStateWithLifecycle()
    val tokenNip by viewModel.tokenNip.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.setup_title)) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.setup_intro))
            TokenSection(
                title = stringResource(R.string.setup_step_token),
                environment = viewModel.environment,
                tokenSet = tokenSet == true,
                tokenNip = tokenNip,
                connection = connection,
                onSaveToken = viewModel::saveToken,
                onCheckConnection = viewModel::checkConnection,
            )
            PrimaryButton(onClick = onContinue, enabled = canContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_continue))
            }
        }
    }
}
