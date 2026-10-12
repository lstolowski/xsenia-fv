package pl.xsenia.fv.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.invoices.CurrencyTotals
import pl.xsenia.fv.invoices.InvoiceListLogic
import pl.xsenia.fv.invoices.InvoiceRow
import pl.xsenia.fv.mirror.InvoiceCategory
import java.time.YearMonth

data class InvoiceListUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val query: String = "",
    val invoices: List<InvoiceRow> = emptyList(),
    val totals: List<CurrencyTotals> = emptyList(),
    /** Months for the picker with invoice counts, newest first. */
    val months: List<Pair<YearMonth, Int>> = emptyList(),
    /** True when the mirror holds no invoices of this category at all. */
    val mirrorEmpty: Boolean = true,
)

/** Sales or purchase invoice list: month by delivery date, search, totals (US-003, US-004). */
class InvoiceListViewModel(private val category: InvoiceCategory, private val container: AppContainer) : ViewModel() {

    private val rows = MutableStateFlow<List<InvoiceRow>?>(null)
    private val month = MutableStateFlow(YearMonth.now())
    private val query = MutableStateFlow("")

    val state: StateFlow<InvoiceListUiState> = combine(rows, month, query) { rows, month, query ->
        if (rows == null) return@combine InvoiceListUiState(month = month, query = query)
        val visible = InvoiceListLogic.filter(rows, month, query)
        InvoiceListUiState(
            loading = false,
            month = month,
            query = query,
            invoices = visible,
            totals = InvoiceListLogic.totals(visible),
            months = InvoiceListLogic.months(rows, YearMonth.from(AppContainer.MIRROR_START_DATE), YearMonth.now()),
            mirrorEmpty = rows.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceListUiState())

    init {
        // Reload whenever a synchronization changed the mirror.
        viewModelScope.launch {
            container.syncService.state.map { it.mirrorVersion }.distinctUntilChanged().collect {
                rows.value = withContext(Dispatchers.IO) {
                    container.syncService.mirror.entries(category).map { InvoiceListLogic.toRow(category, it) }
                }
            }
        }
    }

    fun selectMonth(value: YearMonth) {
        month.value = value
    }

    fun previousMonth() = selectMonth(month.value.minusMonths(1))

    fun nextMonth() = selectMonth(month.value.plusMonths(1))

    fun setQuery(value: String) {
        query.value = value
    }
}
