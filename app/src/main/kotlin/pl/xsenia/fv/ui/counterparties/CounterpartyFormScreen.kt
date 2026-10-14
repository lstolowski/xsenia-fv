package pl.xsenia.fv.ui.counterparties

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
import pl.xsenia.fv.ui.company.DiscardChangesDialog
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.components.NipField
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.components.ProfileTextField
import pl.xsenia.fv.ui.theme.WarningText

/**
 * New or edited counterparty (ZAD-024): NIP first, with the White List lookup.
 * [onClose] gets the NIP of the saved counterparty, or null when nothing was saved (back, delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterpartyFormScreen(viewModel: CounterpartyFormViewModel, onClose: (savedId: String?) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val closedWith by viewModel.closedWith.collectAsStateWithLifecycle()
    LaunchedEffect(closedWith) { closedWith?.let { onClose(it.ifEmpty { null }) } }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val dirty = state?.dirty == true
    BackHandler(enabled = dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state?.editing == true) R.string.counterparty_form_edit else R.string.counterparty_form_new)) },
                navigationIcon = {
                    IconButton(onClick = { if (dirty) confirmDiscard = true else onClose(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state?.editing == true) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.counterparty_delete))
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
            NipField(
                value = fields.nip,
                onValueChange = if (current.editing) null else { value -> viewModel.update { it.copy(nip = value) } },
                lookupRunning = current.lookup.running,
                lookupResult = current.lookup.result,
                onLookup = viewModel::lookup,
                issues = issues,
                supportingText = stringResource(if (current.editing) R.string.counterparty_nip_fixed else R.string.counterparty_nip_hint),
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
            ProfileTextField(
                stringResource(R.string.field_email), fields.email, { value -> viewModel.update { it.copy(email = value) } }, ProfileField.EMAIL, issues,
                keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None,
            )
            if (current.usedByInvoices) Text(stringResource(R.string.counterparty_used), color = WarningText)
            PrimaryButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_save)) }
        }
    }

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                onClose(null)
            },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.counterparty_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.counterparty_delete_text, state?.fields?.name.orEmpty()))
                    if (state?.usedByInvoices == true) Text(stringResource(R.string.counterparty_used), color = WarningText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.counterparty_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
