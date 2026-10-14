package pl.xsenia.fv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.profile.FieldIssue
import pl.xsenia.fv.invoice.profile.FieldIssueType
import pl.xsenia.fv.invoice.profile.ProfileField
import pl.xsenia.fv.lookup.CompanyLookupMessages
import pl.xsenia.fv.lookup.CompanyLookupResult
import pl.xsenia.fv.ui.theme.WarningAmber
import pl.xsenia.fv.ui.theme.WarningText

/**
 * Single-line text field of the company / bank account / counterparty forms with the message of its [issues]
 * (or [supportingText] without one); [prefix] — e.g. the country prefix of a VAT UE number.
 */
@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    field: ProfileField,
    issues: List<FieldIssue>,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    readOnly: Boolean = false,
    prefix: String? = null,
    supportingText: String? = null,
) {
    val issue = issues.firstOrNull { it.field == field }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text(label) },
        prefix = prefix?.let { { Text(it) } },
        singleLine = true,
        isError = issue != null,
        supportingText = (issue?.let { fieldIssueText(it) } ?: supportingText)?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * NIP field with the White List download icon (ADR 0007). [onValueChange] null makes the NIP read-only
 * (my company — NIP of the KSeF token; an existing counterparty). Below: the result of the last lookup.
 */
@Composable
fun NipField(
    value: String,
    onValueChange: ((String) -> Unit)?,
    lookupRunning: Boolean,
    lookupResult: CompanyLookupResult?,
    onLookup: () -> Unit,
    issues: List<FieldIssue>,
    supportingText: String? = null,
) {
    val issue = issues.firstOrNull { it.field == ProfileField.NIP }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange ?: {},
            readOnly = onValueChange == null,
            label = { Text(stringResource(R.string.field_nip)) },
            singleLine = true,
            isError = issue != null,
            supportingText = (issue?.let { fieldIssueText(it) } ?: supportingText)?.let { text -> { Text(text) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                if (lookupRunning) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onLookup) {
                        Icon(
                            painterResource(R.drawable.ic_download),
                            contentDescription = stringResource(R.string.lookup_action),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        LookupResultText(lookupResult)
    }
}

/** Outcome of a White List lookup: what was filled in, the VAT status warning, or why nothing was filled. */
@Composable
private fun LookupResultText(result: CompanyLookupResult?) {
    result ?: return
    val resources = LocalResources.current
    val messages = remember(resources) { CompanyLookupMessages(resources) }
    messages.problem(result)?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        return
    }
    Text(stringResource(R.string.lookup_filled), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    messages.vatWarning(result)?.let { warning ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
            Text(warning, color = WarningText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Polish message of a form field problem. */
@Composable
fun fieldIssueText(issue: FieldIssue): String = when (issue.type) {
    FieldIssueType.REQUIRED -> stringResource(R.string.field_issue_required)
    FieldIssueType.TOO_LONG -> stringResource(R.string.field_issue_too_long, issue.maxLength ?: 0)
    FieldIssueType.INVALID_NIP -> stringResource(R.string.field_issue_invalid_nip)
    FieldIssueType.INVALID_VAT_UE -> stringResource(R.string.field_issue_invalid_vat_ue)
    FieldIssueType.INVALID_COUNTRY -> stringResource(R.string.field_issue_invalid_country)
    FieldIssueType.INVALID_ACCOUNT_NUMBER -> stringResource(R.string.field_issue_invalid_account)
    FieldIssueType.INVALID_EMAIL -> stringResource(R.string.field_issue_invalid_email)
    FieldIssueType.INVALID_CURRENCY -> stringResource(R.string.field_issue_invalid_currency)
    FieldIssueType.DUPLICATE ->
        stringResource(
            when (issue.field) {
                ProfileField.NIP -> R.string.field_issue_duplicate_nip
                ProfileField.TAX_NUMBER -> R.string.field_issue_duplicate_tax_number
                else -> R.string.field_issue_duplicate_account
            },
        )
}
