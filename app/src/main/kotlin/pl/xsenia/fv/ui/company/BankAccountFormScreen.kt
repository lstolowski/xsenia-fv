package pl.xsenia.fv.ui.company

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.profile.ProfileField
import pl.xsenia.fv.profile.BankAccountCurrencies
import pl.xsenia.fv.ui.components.DropdownField
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.components.ProfileTextField
import pl.xsenia.fv.ui.components.fieldIssueText
import pl.xsenia.fv.ui.theme.WarningText

/** New or edited bank account of my company (ZAD-023). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountFormScreen(viewModel: BankAccountFormViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    LaunchedEffect(closed) { if (closed) onClose() }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val dirty = state?.dirty == true
    BackHandler(enabled = dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state?.editing == true) R.string.bank_account_form_edit else R.string.bank_account_form_new)) },
                navigationIcon = {
                    IconButton(onClick = { if (dirty) confirmDiscard = true else onClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state?.editing == true) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.bank_account_delete))
                        }
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
        val fields = current.fields
        val issues = current.issues
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileTextField(stringResource(R.string.field_account_name), fields.name, { value -> viewModel.update { it.copy(name = value) } }, ProfileField.NAME, issues)
            ProfileTextField(
                stringResource(R.string.field_account_number), fields.number, { value -> viewModel.update { it.copy(number = value) } },
                ProfileField.ACCOUNT_NUMBER, issues, keyboardType = KeyboardType.Number,
            )
            if (current.usedByInvoices) {
                Text(stringResource(R.string.bank_account_used), color = WarningText, style = MaterialTheme.typography.bodySmall)
            }
            ProfileTextField(stringResource(R.string.field_bank_name), fields.bank, { value -> viewModel.update { it.copy(bank = value) } }, ProfileField.BANK, issues)
            ProfileTextField(
                stringResource(R.string.field_swift), fields.swift, { value -> viewModel.update { it.copy(swift = value) } },
                ProfileField.SWIFT, issues, capitalization = KeyboardCapitalization.Characters,
                supportingText = stringResource(R.string.field_swift_hint),
            )
            DropdownField(
                label = stringResource(R.string.field_currency),
                options = BankAccountCurrencies.options(fields.currency),
                selected = fields.currency,
                optionText = { it },
                error = issues.firstOrNull { it.field == ProfileField.CURRENCY }?.let { fieldIssueText(it) },
                onSelect = { value -> viewModel.update { it.copy(currency = value) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.bank_account_currency_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_save)) }
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
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.bank_account_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.bank_account_delete_text, state?.fields?.name.orEmpty()))
                    if (state?.usedByInvoices == true) Text(stringResource(R.string.bank_account_used), color = WarningText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.bank_account_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
