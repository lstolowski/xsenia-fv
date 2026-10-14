package pl.xsenia.fv.ui.counterparties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.R
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.ui.components.CounterpartyText
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.components.Tile

/** Settings → „Kontrahenci” (ZAD-024): the same tiles as when choosing the buyer of a new invoice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterpartyListScreen(session: ProfileSession, onBack: () -> Unit, onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val profile by session.profile.profile.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.counterparties_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val counterparties = profile?.counterparties
        if (counterparties == null) {
            LoadingScreen()
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.counterparty_add))
            }
            if (counterparties.isEmpty()) {
                Text(stringResource(R.string.counterparties_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(counterparties, key = { it.id }) { counterparty ->
                    Tile(onClick = { onEdit(counterparty.id) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) { CounterpartyText(counterparty) }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
