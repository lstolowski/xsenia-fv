package pl.xsenia.fv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.ui.components.BankAccountText
import pl.xsenia.fv.ui.components.SectionCard
import pl.xsenia.fv.ui.components.SecondaryButton
import pl.xsenia.fv.ui.components.Tile
import pl.xsenia.fv.ui.format.counterpartyCount

/** Settings → „Moja firma”: the company of the active profile and its bank accounts; „Edytuj” opens the form. */
@Composable
fun CompanySection(profile: CompanyProfile, onEdit: () -> Unit) {
    SectionCard(stringResource(R.string.company_section_title)) {
        val company = profile.company
        if (company == null) {
            Text(stringResource(R.string.list_nip, profile.nip))
            Text(stringResource(R.string.company_empty), color = MaterialTheme.colorScheme.error)
        } else {
            Text(company.name, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.list_nip, company.nip))
            Column {
                Text(company.address.line1)
                company.address.line2?.let { Text(it) }
                company.address.country?.let { Text(it) }
            }
            company.email?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            company.phone?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        Text(stringResource(R.string.bank_accounts_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        if (profile.bankAccounts.isEmpty()) {
            Text(stringResource(R.string.bank_accounts_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            profile.bankAccounts.forEach { account ->
                Tile { Column(Modifier.padding(12.dp)) { BankAccountText(account) } }
            }
        }
        SecondaryButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text(stringResource(R.string.company_edit)) }
    }
}

/** Settings → „Kontrahenci”: only a link to the counterparty list (ZAD-024). */
@Composable
fun CounterpartiesLink(count: Int, onClick: () -> Unit) {
    Tile(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.counterparties_title), style = MaterialTheme.typography.titleMedium)
                Text(counterpartyCount(count), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
