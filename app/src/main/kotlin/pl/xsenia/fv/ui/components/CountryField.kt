package pl.xsenia.fv.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.profile.Countries

/**
 * Country of a counterparty (ADR 0009): the Polish name; a tap opens the list of all countries with a search field,
 * Poland first. [onSelect] null makes it read-only (a saved counterparty).
 */
@Composable
fun CountryField(countryCode: String, onSelect: ((String) -> Unit)?, supportingText: String? = null) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = Countries.name(countryCode),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(stringResource(R.string.field_country)) },
        trailingIcon = if (onSelect != null) {
            { Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.country_choose)) }
        } else {
            null
        },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth().let { if (onSelect != null) it.clickable { open = true } else it },
    )
    if (open && onSelect != null) {
        var query by remember { mutableStateOf("") }
        val countries = remember(query) { CountryList.matching(query) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.country_choose)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.country_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(countries, key = { it }) { code ->
                            Text(
                                Countries.name(code),
                                fontWeight = if (code == countryCode) FontWeight.SemiBold else null,
                                color = if (code == countryCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    open = false
                                    onSelect(code)
                                }.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Countries offered in [CountryField]: Poland first, then all by Polish name; search by name or code. */
object CountryList {

    fun matching(query: String): List<String> {
        val all = listOf(Countries.POLAND) + Countries.ALL.filter { it != Countries.POLAND }
        val text = query.trim()
        if (text.isEmpty()) return all
        return all.filter { Countries.name(it).contains(text, ignoreCase = true) || it.equals(text, ignoreCase = true) }
    }
}
