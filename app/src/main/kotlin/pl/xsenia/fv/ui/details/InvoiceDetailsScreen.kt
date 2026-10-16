package pl.xsenia.fv.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.issue.SimilarInvoice
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.components.ShareInvoiceButton
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.components.SectionCard
import pl.xsenia.fv.ui.format.Formats

/** "Wystaw podobną" (US-015): [onIssueSimilar] — for sales invoices; [similarBlocked] — a new invoice is being sent meanwhile. */
data class IssueSimilarAction(val onIssueSimilar: () -> Unit, val similarBlocked: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/** [canShare] — a sales invoice: its PDF can be shared (US-013); [issueSimilar] — null for purchase invoices. */
fun InvoiceDetailsScreen(state: InvoiceDetailsState, canShare: Boolean, issueSimilar: IssueSimilarAction?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    val invoice = (state as? InvoiceDetailsState.Loaded)?.invoice
                    if (canShare && invoice != null) ShareInvoiceButton(invoice, state.entry.metadata.ksefNumber)
                },
            )
        },
    ) { padding ->
        when (state) {
            InvoiceDetailsState.Loading -> LoadingScreen()
            InvoiceDetailsState.NotFound -> Text(stringResource(R.string.details_not_found), Modifier.padding(padding).padding(16.dp))
            is InvoiceDetailsState.Loaded -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val invoice = state.invoice
                if (invoice != null) {
                    InvoiceDetailsContent(invoice, state.entry.metadata.ksefNumber)
                    if (issueSimilar != null && SimilarInvoice.canCopy(invoice)) IssueSimilarButton(issueSimilar)
                } else {
                    MetadataFallback(state)
                }
            }
        }
    }
}

@Composable
private fun IssueSimilarButton(action: IssueSimilarAction) {
    PrimaryButton(onClick = action.onIssueSimilar, enabled = !action.similarBlocked, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.details_issue_similar))
    }
    if (action.similarBlocked) {
        Text(stringResource(R.string.details_issue_similar_sending), style = MaterialTheme.typography.bodySmall)
    }
}

/** Shown when the XML cannot be parsed: data from the KSeF metadata only. */
@Composable
private fun MetadataFallback(state: InvoiceDetailsState.Loaded) {
    val metadata = state.entry.metadata
    Text(stringResource(R.string.details_parse_error, state.parseError.orEmpty()), color = MaterialTheme.colorScheme.error)
    SectionCard(metadata.invoiceNumber) {
        Text(stringResource(R.string.details_ksef_number, metadata.ksefNumber))
        Text(stringResource(R.string.details_issue_date, Formats.date(metadata.issueDate)))
        Text(stringResource(R.string.details_seller) + ": " + listOfNotNull(metadata.seller.name, metadata.seller.nip).joinToString(", "))
        Text(stringResource(R.string.details_buyer) + ": " + listOfNotNull(metadata.buyer.name, metadata.buyer.identifier.value).joinToString(", "))
        Text(
            stringResource(
                R.string.details_metadata_summary,
                Formats.money(metadata.netAmount, metadata.currency),
                Formats.money(metadata.vatAmount, "PLN"),
                Formats.money(metadata.grossAmount, metadata.currency),
            ),
        )
    }
}
