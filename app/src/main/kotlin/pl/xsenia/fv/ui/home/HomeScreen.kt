package pl.xsenia.fv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.BuildConfig
import pl.xsenia.fv.R
import pl.xsenia.fv.config.ConfigState
import pl.xsenia.fv.ui.MainViewModel
import pl.xsenia.fv.ui.components.SyncCard

/** Temporary home screen — invoice lists arrive in ZAD-009. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val configState by viewModel.configState.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    // Runs after every unlock (the home screen is recreated); the service skips it if synced < 15 min ago.
    LaunchedEffect(Unit) { viewModel.syncIfStale() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_settings)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.environment_label, viewModel.environment.name), style = MaterialTheme.typography.titleMedium)
            (configState as? ConfigState.Loaded)?.config?.let { config ->
                config.company?.let { Text(stringResource(R.string.config_company, it.name, it.nip)) }
                if (config.issues.isNotEmpty()) {
                    Text(stringResource(R.string.home_config_issues, config.issues.size), color = MaterialTheme.colorScheme.error)
                }
            }
            if (configState == ConfigState.FolderInaccessible) {
                Text(stringResource(R.string.folder_inaccessible), color = MaterialTheme.colorScheme.error)
            }
            SyncCard(state = sync, onSyncNow = viewModel::syncNow)
            Text(stringResource(R.string.home_lists_coming), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
        }
    }
}
