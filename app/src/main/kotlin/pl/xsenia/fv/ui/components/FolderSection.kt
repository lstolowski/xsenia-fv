package pl.xsenia.fv.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.config.ConfigIssueFormatter
import pl.xsenia.fv.config.ConfigState

/** Folder status, summary of loaded data and all configuration issues. */
@Composable
fun FolderSection(title: String, state: ConfigState, onSelectFolder: (android.net.Uri) -> Unit, onReload: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onSelectFolder)
    }
    SectionCard(title) {
        when (state) {
            ConfigState.Loading -> Text(stringResource(R.string.loading))
            ConfigState.FolderNotSelected -> Text(stringResource(R.string.folder_not_selected))
            ConfigState.FolderInaccessible -> Text(stringResource(R.string.folder_inaccessible), color = MaterialTheme.colorScheme.error)
            is ConfigState.Loaded -> LoadedConfig(state)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state is ConfigState.Loaded) {
                SecondaryButton(onClick = { picker.launch(null) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.folder_change)) }
                SecondaryButton(onClick = onReload, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.folder_reload)) }
            } else {
                PrimaryButton(onClick = { picker.launch(null) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.folder_select)) }
            }
        }
    }
}

@Composable
private fun LoadedConfig(state: ConfigState.Loaded) {
    val config = state.config
    val resources = LocalResources.current
    val formatter = remember(resources) { ConfigIssueFormatter(resources) }

    Text(stringResource(R.string.folder_selected, state.folderName))
    val company = config.company
    Text(
        if (company != null) stringResource(R.string.config_company, company.name, company.nip) else stringResource(R.string.config_company_missing),
    )
    Text(stringResource(R.string.config_bank_accounts, config.invoiceBankAccounts.size))
    Text(stringResource(R.string.config_counterparties, config.counterparties.size))
    if (config.canIssueInvoices) {
        Text(stringResource(R.string.config_can_issue), color = MaterialTheme.colorScheme.primary)
    } else {
        Text(stringResource(R.string.config_cannot_issue), color = MaterialTheme.colorScheme.error)
    }
    if (config.issues.isNotEmpty()) {
        Text(stringResource(R.string.config_issues_title, config.issues.size), style = MaterialTheme.typography.titleSmall)
        config.issues.forEach { issue ->
            Text("• " + formatter.format(issue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
