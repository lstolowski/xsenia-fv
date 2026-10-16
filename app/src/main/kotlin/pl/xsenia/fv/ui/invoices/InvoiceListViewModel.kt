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
import pl.xsenia.fv.invoices.UnsentSummary
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.mirror.InvoiceCategory
import java.time.Clock
import java.time.YearMonth

/** The current month in the time zone of the invoices (Europe/Warsaw), not of the device. */
private fun currentMonth(): YearMonth = InvoiceListLogic.currentMonth(Clock.systemUTC(), AppContainer.POLAND)

data class InvoiceListUiState(
    val loading: Boolean = true,
    val month: YearMonth = currentMonth(),
    val query: String = "",
    val invoices: List<InvoiceRow> = emptyList(),
    val totals: List<CurrencyTotals> = emptyList(),
    val unsentSummary: UnsentSummary? = null,
    /** Months for the picker with invoice counts, newest first. */
    val months: List<Pair<YearMonth, Int>> = emptyList(),
    /** True when the mirror holds no invoices of this category at all. */
    val mirrorEmpty: Boolean = true,
)

/** Sales or purchase invoice list: month by delivery date, search, totals (US-003, US-004). */
class InvoiceListViewModel(private val category: InvoiceCategory, private val session: ProfileSession) : ViewModel() {

    private val rows = MutableStateFlow<List<InvoiceRow>?>(null)
    private val month = MutableStateFlow(currentMonth())
    private val query = MutableStateFlow("")

    /** Unsent invoices (US-007) — only on the sales list. */
    private val unsentRows = combine(session.unsentInvoices.invoices, session.profile.profile) { unsent, profile ->
        if (category != InvoiceCategory.SALES) return@combine emptyList()
        unsent.map { InvoiceListLogic.toRow(it, profile) }
    }

    val state: StateFlow<InvoiceListUiState> = combine(rows, unsentRows, month, query) { ksefRows, unsentRows, month, query ->
        if (ksefRows == null) return@combine InvoiceListUiState(month = month, query = query)
        val rows = ksefRows + unsentRows
        // TODO CODE REVIEWED: Filtering, sorting, totals and month counts all scan the mirror on the view-model (normally Main) dispatcher after each query/month change; a large mirror can cause visible jank. Move derived work off Main or cache by inputs.
        val visible = InvoiceListLogic.filter(rows, month, query)
        InvoiceListUiState(
            loading = false,
            month = month,
            query = query,
            invoices = visible,
            totals = InvoiceListLogic.totals(visible),
            unsentSummary = InvoiceListLogic.unsentSummary(visible),
            months = InvoiceListLogic.months(rows, YearMonth.from(AppContainer.MIRROR_START_DATE), currentMonth()),
            mirrorEmpty = ksefRows.isEmpty() && unsentRows.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceListUiState())

    init {
        // Reload whenever a synchronization changed the mirror.
        viewModelScope.launch {
            session.syncService.state.map { it.mirrorVersion }.distinctUntilChanged().collect {
                rows.value = withContext(Dispatchers.IO) {
                    session.syncService.mirror.entries(category).map { InvoiceListLogic.toRow(category, it) }
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
