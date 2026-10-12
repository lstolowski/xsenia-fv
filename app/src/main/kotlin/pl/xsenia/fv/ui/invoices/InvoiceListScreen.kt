package pl.xsenia.fv.ui.invoices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.invoices.CurrencyTotals
import pl.xsenia.fv.invoices.InvoiceRow
import pl.xsenia.fv.invoices.UnsentSummary
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.sync.SyncUiState
import pl.xsenia.fv.ui.components.SyncStatus
import pl.xsenia.fv.ui.format.Formats
import pl.xsenia.fv.ui.format.invoiceCount
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (category == InvoiceCategory.SALES) R.string.sales_title else R.string.purchase_title)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !sync.running) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync_now))
                    }
                },
            )
        },
        bottomBar = { if (state.totals.isNotEmpty() || state.unsentSummary != null) TotalsBar(state.totals, state.unsentSummary) },
        floatingActionButton = {
            onNewInvoice?.let {
                ExtendedFloatingActionButton(
                    onClick = it,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_invoice)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SyncStatus(sync)
            MonthSelector(state.month, onPreviousMonth, onNextMonth, onOpenPicker = { monthPickerOpen = true })
            SearchField(state.query, onQueryChange)
            PullToRefreshBox(isRefreshing = sync.running, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                if (state.invoices.isEmpty() && !state.loading) {
                    EmptyState(state, neverSynced = sync.lastSyncAt == null)
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.invoices, key = { it.key }) { invoice ->
                            InvoiceListItem(invoice, onClick = { onOpenInvoice(invoice) })
                            HorizontalDivider()
                        }
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

@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, onOpenPicker: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.month_previous))
        }
        TextButton(onClick = onOpenPicker, modifier = Modifier.weight(1f)) {
            Text(Formats.month(month), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.month_next))
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.search_label)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun InvoiceListItem(invoice: InvoiceRow, onClick: () -> Unit) {
    val typeLabel = when {
        invoice.rejected -> stringResource(R.string.list_unsent_rejected)
        invoice.isUnsent -> stringResource(R.string.list_unsent)
        else -> invoiceTypeLabel(invoice.invoiceType)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = if (invoice.isUnsent) {
            { Icon(Icons.Filled.Warning, contentDescription = stringResource(R.string.list_unsent), tint = WARNING_AMBER) }
        } else {
            null
        },
        overlineContent = {
            Text(
                listOfNotNull(stringResource(R.string.list_delivery, Formats.date(invoice.deliveryDate)), typeLabel).joinToString(" · "),
                color = when {
                    invoice.isUnsent -> WARNING_TEXT
                    typeLabel != null -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        headlineContent = { Text(invoice.counterpartyName ?: stringResource(R.string.list_no_name), maxLines = 2) },
        supportingContent = {
            Text(
                listOfNotNull(
                    invoice.counterpartyNip?.let { stringResource(R.string.list_nip, it) },
                    stringResource(R.string.list_number, invoice.invoiceNumber),
                ).joinToString(" · "),
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(Formats.money(invoice.grossAmount, invoice.currency), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.list_net, Formats.money(invoice.netAmount, invoice.currency)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
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

// Warning colors of unsent invoices (yellow triangle with an exclamation mark), the same in both themes.
private val WARNING_AMBER = Color(0xFFF9A825)
private val WARNING_TEXT = Color(0xFFB26A00)

@Composable
private fun TotalsBar(totals: List<CurrencyTotals>, unsent: UnsentSummary?) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            unsent?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = WARNING_AMBER, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(R.string.list_unsent_summary, invoiceCount(it.count), Formats.money(it.gross, "PLN")),
                        style = MaterialTheme.typography.labelMedium,
                        color = WARNING_TEXT,
                    )
                }
            }
            totals.forEach { total ->
                Text(stringResource(R.string.totals_count, invoiceCount(total.count)), style = MaterialTheme.typography.labelMedium)
                Text(
                    stringResource(
                        R.string.totals_line,
                        Formats.money(total.net, total.currency),
                        Formats.money(total.vat, total.currency),
                        Formats.money(total.gross, total.currency),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(state: InvoiceListUiState, neverSynced: Boolean) {
    // Scrollable, so pull-to-refresh also works on an empty list.
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            when {
                state.mirrorEmpty && neverSynced -> stringResource(R.string.list_empty_never_synced)
                state.query.isNotBlank() -> stringResource(R.string.list_empty_query, state.query.trim())
                else -> stringResource(R.string.list_empty_month)
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
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
