package pl.xsenia.fv.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.BuildConfig
import pl.xsenia.fv.R
import pl.xsenia.fv.ui.components.SecondaryButton
import pl.xsenia.fv.ui.MainViewModel
import pl.xsenia.fv.ui.components.SectionCard
import pl.xsenia.fv.ui.components.TokenSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: (() -> Unit)?) {
    val tokenSet by viewModel.tokenSet.collectAsStateWithLifecycle()
    val tokenNip by viewModel.tokenNip.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    var confirmClearCache by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cacheClearedMessage = stringResource(R.string.settings_cache_cleared)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { onBack?.let { TextButton(onClick = it) { Text(stringResource(R.string.action_back)) } } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(stringResource(R.string.settings_app_section)) {
                Text(stringResource(R.string.environment_label, viewModel.environment.name))
                Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME))
            }
            TokenSection(
                title = stringResource(R.string.token_section_title),
                environment = viewModel.environment,
                tokenSet = tokenSet == true,
                tokenNip = tokenNip,
                connection = connection,
                onSaveToken = viewModel::saveToken,
                onCheckConnection = viewModel::checkConnection,
            )
            SectionCard(stringResource(R.string.settings_cache_section)) {
                Text(stringResource(R.string.settings_cache_description))
                SecondaryButton(onClick = { confirmClearCache = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_cache_clear)) }
            }
        }
    }

    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text(stringResource(R.string.settings_cache_confirm_title)) },
            text = { Text(stringResource(R.string.settings_cache_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    viewModel.clearCache()
                    Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.settings_cache_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
