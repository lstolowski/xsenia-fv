package pl.xsenia.fv.invoices

import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.MirrorEntry
import java.math.BigDecimal
import java.time.YearMonth

/** One row of an invoice list; the counterparty is the buyer on sales and the seller on purchase invoices. */
data class InvoiceRow(
    val ksefNumber: String,
    val invoiceNumber: String,
    val deliveryDate: java.time.LocalDate,
    val issueDate: java.time.LocalDate,
    val counterpartyName: String?,
    val counterpartyNip: String?,
    val netAmount: BigDecimal,
    val grossAmount: BigDecimal,
    val currency: String,
    /** KSeF invoice type: `Vat`, `Kor`, `Zal`, … */
    val invoiceType: String,
)

/** Monthly totals in one currency. */
data class CurrencyTotals(val currency: String, val count: Int, val net: BigDecimal, val vat: BigDecimal, val gross: BigDecimal)

/** Filtering and totals of the invoice lists (US-003, US-004). */
object InvoiceListLogic {

    private val NIP_SEPARATORS = Regex("[\\s-]")

    fun toRow(category: InvoiceCategory, entry: MirrorEntry): InvoiceRow {
        val metadata = entry.metadata
        val (name, nip) = when (category) {
            InvoiceCategory.SALES -> metadata.buyer.name to metadata.buyer.identifier.value
            InvoiceCategory.PURCHASE -> metadata.seller.name to metadata.seller.nip
        }
        return InvoiceRow(
            ksefNumber = metadata.ksefNumber,
            invoiceNumber = metadata.invoiceNumber,
            deliveryDate = entry.deliveryDate,
            issueDate = metadata.issueDate,
            counterpartyName = name,
            counterpartyNip = nip,
            netAmount = metadata.netAmount,
            grossAmount = metadata.grossAmount,
            currency = metadata.currency,
            invoiceType = metadata.invoiceType,
        )
    }

    /** Invoices delivered in [month] matching [query], newest first. */
    fun filter(rows: List<InvoiceRow>, month: YearMonth, query: String): List<InvoiceRow> =
        rows.filter { YearMonth.from(it.deliveryDate) == month && matches(it, query) }
            .sortedWith(compareByDescending<InvoiceRow> { it.deliveryDate }.thenByDescending { it.issueDate }.thenByDescending { it.invoiceNumber })

    /**
     * Case-insensitive match of a name fragment or NIP; spaces and dashes in the query are ignored for the NIP
     * (`669-103-42-74` finds `6691034274`).
     */
    fun matches(row: InvoiceRow, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        val nipQuery = trimmed.replace(NIP_SEPARATORS, "")
        return row.counterpartyName?.contains(trimmed, ignoreCase = true) == true ||
            (nipQuery.isNotEmpty() && row.counterpartyNip?.contains(nipQuery, ignoreCase = true) == true)
    }

    /** Totals per currency (PLN first); VAT = gross − net in the invoice currency. */
    fun totals(rows: List<InvoiceRow>): List<CurrencyTotals> =
        rows.groupBy { it.currency }
            .map { (currency, invoices) ->
                val net = invoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.netAmount }
                val gross = invoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.grossAmount }
                CurrencyTotals(currency, invoices.size, net, gross - net, gross)
            }
            .sortedWith(compareBy<CurrencyTotals> { it.currency != "PLN" }.thenBy { it.currency })

    /**
     * Months offered in the month picker, newest first: every month from [firstMonth] to [currentMonth]
     * plus any other month with invoices, with the number of invoices delivered in it.
     */
    fun months(rows: List<InvoiceRow>, firstMonth: YearMonth, currentMonth: YearMonth): List<Pair<YearMonth, Int>> {
        val counts = rows.groupingBy { YearMonth.from(it.deliveryDate) }.eachCount()
        val range = generateSequence(firstMonth) { it.plusMonths(1) }.takeWhile { !it.isAfter(currentMonth) }
        return (range + counts.keys).distinct().sortedDescending().map { it to (counts[it] ?: 0) }.toList()
    }
}
