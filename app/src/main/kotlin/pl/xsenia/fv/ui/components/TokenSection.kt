package pl.xsenia.fv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.ksef.ConnectionCheckResult
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.security.KsefTokenStore
import pl.xsenia.fv.ui.ConnectionUiState

/** Token status (never the token itself), token entry and KSeF connection check. */
@Composable
fun TokenSection(
    title: String,
    environment: KsefEnvironment,
    tokenSet: Boolean,
    tokenNip: String?,
    connection: ConnectionUiState,
    onSaveToken: (String) -> Unit,
    onCheckConnection: () -> Unit,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    SectionCard(title) {
        Text(
            when {
                !tokenSet -> stringResource(R.string.token_not_set)
                tokenNip != null -> stringResource(R.string.token_set_for_nip, tokenNip)
                else -> stringResource(R.string.token_set)
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tokenSet) {
                SecondaryButton(onClick = { dialogOpen = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.token_change)) }
                SecondaryButton(onClick = onCheckConnection, enabled = !connection.checking, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.connection_check))
                }
            } else {
                PrimaryButton(onClick = { dialogOpen = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.token_enter)) }
            }
        }
        ConnectionStatus(connection)
    }
    if (dialogOpen) {
        TokenInputDialog(
            environment = environment,
            onDismiss = { dialogOpen = false },
            onSave = {
                dialogOpen = false
                onSaveToken(it)
            },
        )
    }
}

@Composable
private fun ConnectionStatus(connection: ConnectionUiState) {
    if (connection.checking) {
        Text(stringResource(R.string.connection_checking))
        return
    }
    val result = connection.result ?: return
    val text = when (result) {
        ConnectionCheckResult.Success -> stringResource(R.string.connection_success)
        ConnectionCheckResult.MissingToken -> stringResource(R.string.connection_missing_token)
        ConnectionCheckResult.TokenUnreadable -> stringResource(R.string.token_unreadable)
        ConnectionCheckResult.MissingNip -> stringResource(R.string.connection_missing_nip)
        is ConnectionCheckResult.AuthenticationFailed ->
            stringResource(R.string.connection_auth_failed, result.statusCode, result.description)
        is ConnectionCheckResult.RateLimited ->
            stringResource(R.string.connection_rate_limited, result.retryAfterSeconds?.toString() ?: "?")
        is ConnectionCheckResult.NetworkError -> stringResource(R.string.connection_network_error, result.message.orEmpty())
        is ConnectionCheckResult.ApiError -> stringResource(R.string.connection_api_error, result.message.orEmpty())
    }
    val color = if (result == ConnectionCheckResult.Success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Text(text, color = color)
    if (connection.tokenNotSaved) Text(stringResource(R.string.connection_token_not_saved), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun TokenInputDialog(environment: KsefEnvironment, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val missingNip = token.isNotBlank() && KsefTokenStore.nipOf(token.trim()) == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.token_dialog_title)) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.token_dialog_hint, environment.name))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.token_field_label)) },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { visible = !visible }) {
                    Text(stringResource(if (visible) R.string.token_hide else R.string.token_show))
                }
                // The NIP in the token selects the company profile (ADR 0006).
                if (missingNip) Text(stringResource(R.string.token_missing_nip), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(token) }, enabled = token.isNotBlank() && !missingNip) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
