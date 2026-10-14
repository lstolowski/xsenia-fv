package pl.xsenia.fv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.Countries
import pl.xsenia.fv.ui.format.Formats
import androidx.compose.ui.unit.dp

/** Content of a bank account tile: name, bank, grouped number (and the currency when it is not PLN). */
@Composable
fun BankAccountText(account: BankAccount) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(account.name, style = MaterialTheme.typography.titleSmall)
        account.bank?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val number = Formats.bankAccount(account.number)
        Text(
            if (account.currency == "PLN") number else "$number · ${account.currency}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Content of a counterparty tile: name, NIP (or VAT UE number / foreign identifier with the country), address. */
@Composable
fun CounterpartyText(counterparty: Counterparty) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(counterparty.name, style = MaterialTheme.typography.titleSmall)
        val taxId = counterparty.taxIdText
        val identifier = when {
            !counterparty.isForeign -> stringResource(R.string.list_nip, taxId.orEmpty())
            taxId != null -> "${Countries.name(counterparty.countryCode)} · $taxId"
            else -> Countries.name(counterparty.countryCode)
        }
        Text(identifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            listOfNotNull(counterparty.address.line1, counterparty.address.line2).joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
