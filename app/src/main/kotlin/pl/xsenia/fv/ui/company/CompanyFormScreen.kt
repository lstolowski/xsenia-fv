package pl.xsenia.fv.ui.company

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.ProfileField
import pl.xsenia.fv.profile.RegistryAccount
import pl.xsenia.fv.ui.components.BankAccountText
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.components.NipField
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.components.ProfileTextField
import pl.xsenia.fv.ui.components.SecondaryButton
import pl.xsenia.fv.ui.components.Tile
import pl.xsenia.fv.ui.format.Formats

/** „Moja firma” form (ZAD-023): NIP from the token with the White List lookup, company fields, bank accounts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyFormScreen(
    viewModel: CompanyFormViewModel,
    onClose: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    title: String = stringResource(R.string.company_form_title),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { if (saved) onClose() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val dirty = state?.dirty == true
    BackHandler(enabled = dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { if (dirty) confirmDiscard = true else onClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val current = state
        if (current == null) {
            LoadingScreen()
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompanyFields(current, viewModel)
            PrimaryButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_save)) }

            Text(stringResource(R.string.bank_accounts_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            if (current.bankAccounts.isEmpty()) {
                Text(stringResource(R.string.bank_accounts_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            current.bankAccounts.forEach { account -> BankAccountTile(account, onClick = { onEditAccount(account.number) }) }
            SecondaryButton(onClick = onAddAccount, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.bank_account_add))
            }
        }
        current.registryAccounts?.let { accounts ->
            RegistryAccountsDialog(accounts, onAdd = viewModel::addRegistryAccounts, onDismiss = viewModel::dismissRegistryAccounts)
        }
    }

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                onClose()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun CompanyFields(state: CompanyFormUiState, viewModel: CompanyFormViewModel) {
    val fields = state.fields
    val issues = state.issues
    NipField(
        value = state.nip,
        onValueChange = null,
        lookupRunning = state.lookup.running,
        lookupResult = state.lookup.result,
        onLookup = viewModel::lookup,
        issues = issues,
        supportingText = stringResource(R.string.company_nip_from_token),
    )
    ProfileTextField(stringResource(R.string.field_name), fields.name, { value -> viewModel.update { it.copy(name = value) } }, ProfileField.NAME, issues)
    ProfileTextField(
        stringResource(R.string.field_address_line1), fields.addressLine1, { value -> viewModel.update { it.copy(addressLine1 = value) } },
        ProfileField.ADDRESS_LINE1, issues,
    )
    ProfileTextField(
        stringResource(R.string.field_address_line2), fields.addressLine2, { value -> viewModel.update { it.copy(addressLine2 = value) } },
        ProfileField.ADDRESS_LINE2, issues,
    )
    ProfileTextField(stringResource(R.string.field_country), fields.country, { value -> viewModel.update { it.copy(country = value) } }, ProfileField.COUNTRY, issues)
    ProfileTextField(
        stringResource(R.string.field_email), fields.email, { value -> viewModel.update { it.copy(email = value) } }, ProfileField.EMAIL, issues,
        keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None,
    )
    ProfileTextField(
        stringResource(R.string.field_phone), fields.phone, { value -> viewModel.update { it.copy(phone = value) } }, ProfileField.PHONE, issues,
        keyboardType = KeyboardType.Phone,
    )
}

@Composable
private fun BankAccountTile(account: BankAccount, onClick: () -> Unit) {
    Tile(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { BankAccountText(account) }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Accounts of my company found in the White List (ADR 0007, Q3); nothing is selected by default. */
@Composable
private fun RegistryAccountsDialog(accounts: List<RegistryAccount>, onAdd: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.registry_accounts_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.registry_accounts_hint), style = MaterialTheme.typography.bodySmall)
                // Large companies have dozens of accounts — the list scrolls.
                LazyColumn {
                    items(accounts, key = { it.number }) { account ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = account.saved || account.number in selected,
                                enabled = !account.saved,
                                onCheckedChange = { checked -> selected = if (checked) selected + account.number else selected - account.number },
                            )
                            Column {
                                Text(Formats.bankAccount(account.number), style = MaterialTheme.typography.bodyMedium)
                                if (account.saved) {
                                    Text(stringResource(R.string.registry_account_saved), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(selected) }, enabled = selected.isNotEmpty()) { Text(stringResource(R.string.registry_accounts_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.registry_accounts_skip)) } },
    )
}

@Composable
fun DiscardChangesDialog(onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_text)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
