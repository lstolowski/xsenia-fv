package pl.xsenia.fv.ui.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.invoices.CurrencyTotals
import pl.xsenia.fv.invoices.InvoiceRow
import pl.xsenia.fv.invoices.UnsentSummary
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.sync.SyncUiState
import pl.xsenia.fv.ui.components.ScreenHeader
import pl.xsenia.fv.ui.components.SyncStatus
import pl.xsenia.fv.ui.components.Tile
import pl.xsenia.fv.ui.format.Formats
import pl.xsenia.fv.ui.format.invoiceCount
import pl.xsenia.fv.ui.theme.CardShape
import pl.xsenia.fv.ui.theme.WarningAmber
import pl.xsenia.fv.ui.theme.WarningText
import java.math.BigDecimal
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    category: InvoiceCategory,
    state: InvoiceListUiState,
    sync: SyncUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenInvoice: (InvoiceRow) -> Unit,
    onNewInvoice: (() -> Unit)?,
) {
    var monthPickerOpen by remember { mutableStateOf(false) }
    Scaffold(
        floatingActionButton = {
            onNewInvoice?.let {
                ExtendedFloatingActionButton(
                    onClick = it,
                    shape = CardShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_invoice)) },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(isRefreshing = sync.running, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header, month and search scroll away with the list, leaving room for the invoice tiles.
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") {
                    ListHeader(category, state.totals, sync.running, onRefresh)
                }
                item(key = "sync") { SyncStatus(sync) }
                item(key = "month") { MonthSelector(state.month, onPreviousMonth, onNextMonth, onOpenPicker = { monthPickerOpen = true }) }
                item(key = "search") { SearchField(state.query, onQueryChange) }
                state.unsentSummary?.let { item(key = "unsent") { UnsentSummaryRow(it) } }
                if (state.invoices.isEmpty() && !state.loading) {
                    item(key = "empty") { EmptyState(state, neverSynced = sync.lastSyncAt == null) }
                } else {
                    items(state.invoices, key = { it.key }) { invoice ->
                        InvoiceTile(invoice, onClick = { onOpenInvoice(invoice) })
                    }
                }
            }
        }
    }
    if (monthPickerOpen) {
        MonthPickerDialog(
            months = state.months,
            selected = state.month,
            onSelect = {
                monthPickerOpen = false
                onSelectMonth(it)
            },
            onDismiss = { monthPickerOpen = false },
        )
    }
}

/** Title with the gross total of the month in the accent color, like the template's wallet amount. */
@Composable
private fun ListHeader(category: InvoiceCategory, totals: List<CurrencyTotals>, syncing: Boolean, onRefresh: () -> Unit) {
    val main = totals.firstOrNull()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
        ScreenHeader(
            title = stringResource(if (category == InvoiceCategory.SALES) R.string.sales_title else R.string.purchase_title),
            amount = Formats.money(main?.gross ?: BigDecimal.ZERO, main?.currency ?: "PLN"),
            caption = main?.let {
                stringResource(R.string.header_totals, invoiceCount(it.count), Formats.money(it.net, it.currency), Formats.money(it.vat, it.currency))
            } ?: stringResource(R.string.header_totals_empty),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh, enabled = !syncing) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync_now))
        }
    }
    // Other currencies (rare): one line each.
    totals.drop(1).forEach { total ->
        Text(
            stringResource(
                R.string.totals_line,
                Formats.money(total.net, total.currency),
                Formats.money(total.vat, total.currency),
                Formats.money(total.gross, total.currency),
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, onOpenPicker: () -> Unit) {
    Tile {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.month_previous))
            }
            TextButton(onClick = onOpenPicker, modifier = Modifier.weight(1f)) {
                Text(Formats.month(month), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.month_next))
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_label)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Each invoice in its own tile (US-008); unsent ones with an amber border and a warning triangle (US-007). */
@Composable
private fun InvoiceTile(invoice: InvoiceRow, onClick: () -> Unit) {
    val typeLabel = when {
        invoice.rejected -> stringResource(R.string.list_unsent_rejected)
        invoice.isUnsent -> stringResource(R.string.list_unsent)
        else -> invoiceTypeLabel(invoice.invoiceType)
    }
    Tile(onClick = onClick, borderColor = if (invoice.isUnsent) WarningAmber else null) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (invoice.isUnsent) {
                        Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.list_unsent), tint = WarningAmber, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        invoice.counterpartyName ?: stringResource(R.string.list_no_name),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(stringResource(R.string.list_number, invoice.invoiceNumber), style = MaterialTheme.typography.bodyMedium)
                invoice.counterpartyNip?.let {
                    Text(stringResource(R.string.list_nip, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    listOfNotNull(stringResource(R.string.list_delivery, Formats.date(invoice.deliveryDate)), typeLabel).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        invoice.isUnsent -> WarningText
                        typeLabel != null -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Formats.money(invoice.grossAmount, invoice.currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (invoice.isUnsent) WarningText else MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.list_net, Formats.money(invoice.netAmount, invoice.currency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun invoiceTypeLabel(type: String): String? = when (type) {
    "Vat" -> null
    "Kor" -> stringResource(R.string.invoice_type_kor)
    "Zal" -> stringResource(R.string.invoice_type_zal)
    "Roz" -> stringResource(R.string.invoice_type_roz)
    "Upr" -> stringResource(R.string.invoice_type_upr)
    "KorZal" -> stringResource(R.string.invoice_type_kor_zal)
    "KorRoz" -> stringResource(R.string.invoice_type_kor_roz)
    else -> type
}

@Composable
private fun UnsentSummaryRow(unsent: UnsentSummary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.list_unsent_summary, invoiceCount(unsent.count), Formats.money(unsent.gross, "PLN")),
            style = MaterialTheme.typography.labelMedium,
            color = WarningText,
        )
    }
}

@Composable
private fun EmptyState(state: InvoiceListUiState, neverSynced: Boolean) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            when {
                state.mirrorEmpty && neverSynced -> stringResource(R.string.list_empty_never_synced)
                state.query.isNotBlank() -> stringResource(R.string.list_empty_query, state.query.trim())
                else -> stringResource(R.string.list_empty_month)
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthPickerDialog(
    months: List<Pair<YearMonth, Int>>,
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.month_picker_title)) },
        text = {
            LazyColumn {
                items(months, key = { it.first.toString() }) { (month, count) ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(month) },
                        headlineContent = {
                            Text(Formats.month(month), fontWeight = if (month == selected) FontWeight.Bold else FontWeight.Normal)
                        },
                        trailingContent = { Text(invoiceCount(count)) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
