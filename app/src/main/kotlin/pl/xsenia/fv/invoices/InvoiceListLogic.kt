package pl.xsenia.fv.invoices

import pl.xsenia.fv.invoice.validation.isAsciiDigits
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.TaxIdType
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.InvoiceCalculator
import pl.xsenia.fv.issue.DecimalInput
import pl.xsenia.fv.issue.UnsentInvoice
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.MirrorEntry
import java.math.BigDecimal
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

/** One row of an invoice list; the counterparty is the buyer on sales and the seller on purchase invoices. */
data class InvoiceRow(
    val ksefNumber: String,
    val invoiceNumber: String,
    val deliveryDate: java.time.LocalDate,
    val issueDate: java.time.LocalDate,
    val counterpartyName: String?,
    val counterpartyTaxId: CounterpartyTaxId?,
    val netAmount: BigDecimal,
    val grossAmount: BigDecimal,
    val currency: String,
    /** KSeF invoice type: `Vat`, `Kor`, `Zal`, … */
    val invoiceType: String,
    /** Set for a sales invoice saved without sending to KSeF (US-007); [ksefNumber] is then empty. */
    val unsentId: String? = null,
    /** Unsent invoice that KSeF rejected the last time it was sent. */
    val rejected: Boolean = false,
    /** Unsent invoice with a line that has no readable quantity or price: the amounts do not include it (REV-29). */
    val amountsIncomplete: Boolean = false,
) {
    val isUnsent: Boolean get() = unsentId != null

    /** Stable list key: KSeF number, or the local id of an unsent invoice. */
    val key: String get() = unsentId?.let { "unsent:$it" } ?: ksefNumber
}

/**
 * Identifier of the counterparty on a row (ADR 0009): NIP, VAT UE number with its prefix (`DE999999990`) or another
 * identifier — as KSeF lists it, with the country code in front (`US00-0000000`).
 */
data class CounterpartyTaxId(val type: TaxIdType, val value: String)

/** Unsent invoices of the month — shown below the totals, not included in them (decision Q2). */
/** @property incomplete some of the unsent invoices have lines without a readable quantity or price, so [gross] is a lower bound */
data class UnsentSummary(val count: Int, val gross: BigDecimal, val incomplete: Boolean = false)

/** Monthly totals in one currency. */
data class CurrencyTotals(val currency: String, val count: Int, val net: BigDecimal, val vat: BigDecimal, val gross: BigDecimal)

/** Filtering and totals of the invoice lists (US-003, US-004). */
object InvoiceListLogic {

    private val ID_SEPARATORS = Regex("[\\s.\\-]")

    // KSeF metadata `buyer.identifier.type`; `None` — no identifier.
    private val KSEF_ID_TYPES = mapOf("Nip" to TaxIdType.NIP, "VatUe" to TaxIdType.VAT_UE, "Other" to TaxIdType.OTHER)

    fun toRow(category: InvoiceCategory, entry: MirrorEntry): InvoiceRow {
        val metadata = entry.metadata
        val (name, taxId) = when (category) {
            InvoiceCategory.SALES -> {
                val identifier = metadata.buyer.identifier
                val type = KSEF_ID_TYPES[identifier.type]
                metadata.buyer.name to identifier.value?.let { value -> type?.let { CounterpartyTaxId(it, value) } }
            }
            InvoiceCategory.PURCHASE -> metadata.seller.name to CounterpartyTaxId(TaxIdType.NIP, metadata.seller.nip)
        }
        return InvoiceRow(
            ksefNumber = metadata.ksefNumber,
            invoiceNumber = metadata.invoiceNumber,
            deliveryDate = entry.deliveryDate,
            issueDate = metadata.issueDate,
            counterpartyName = name,
            counterpartyTaxId = taxId,
            netAmount = metadata.netAmount,
            grossAmount = metadata.grossAmount,
            currency = metadata.currency,
            invoiceType = metadata.invoiceType,
        )
    }

    /** Row of an unsent invoice: buyer from the company profile, amounts calculated from the form lines. */
    fun toRow(unsent: UnsentInvoice, profile: CompanyProfile?): InvoiceRow {
        val form = unsent.form
        // An unsent invoice may be incomplete (it is not validated when saved): a line without a readable quantity or
        // price is left out of the amounts, and the row says so. A line with nothing typed yet is just an empty line.
        var incomplete = false
        val lines = form.lines.mapNotNull { line ->
            val quantity = DecimalInput.parse(line.quantity)
            val price = DecimalInput.parse(line.unitNetPrice)
            if (quantity == null || price == null) {
                if (!line.isBlank) incomplete = true
                return@mapNotNull null
            }
            DraftLine(line.name, quantity, line.unit, price, line.vatRate)
        }
        val totals = InvoiceCalculator.totals(lines)
        val buyer = profile?.counterparties?.firstOrNull { it.id == form.counterpartyId }
        return InvoiceRow(
            ksefNumber = "",
            invoiceNumber = form.invoiceNumber,
            deliveryDate = form.deliveryDate,
            issueDate = form.issueDate,
            counterpartyName = buyer?.name,
            // Without the counterparty in the profile only a NIP-like id can still be shown.
            counterpartyTaxId = buyer?.let(::taxIdOf)
                ?: form.counterpartyId?.takeIf { id -> id.length == NIP_LENGTH && id.isAsciiDigits() }?.let { CounterpartyTaxId(TaxIdType.NIP, it) },
            netAmount = totals.net,
            grossAmount = totals.gross,
            currency = form.currency.code,
            invoiceType = "Vat",
            unsentId = unsent.id,
            rejected = unsent.rejection != null,
            amountsIncomplete = incomplete,
        )
    }

    fun unsentSummary(rows: List<InvoiceRow>): UnsentSummary? {
        val unsent = rows.filter { it.isUnsent }
        if (unsent.isEmpty()) return null
        return UnsentSummary(unsent.size, unsent.fold(BigDecimal.ZERO) { sum, it -> sum + it.grossAmount }, unsent.any { it.amountsIncomplete })
    }

    /** Invoices delivered in [month] matching [query], newest first. */
    fun filter(rows: List<InvoiceRow>, month: YearMonth, query: String): List<InvoiceRow> =
        rows.filter { YearMonth.from(it.deliveryDate) == month && matches(it, query) }
            .sortedWith(compareByDescending<InvoiceRow> { it.deliveryDate }.thenByDescending { it.issueDate }.thenByDescending { it.invoiceNumber })

    /**
     * Case-insensitive match of a name fragment or of the identifier (NIP, VAT UE number, another ID); spaces, dots
     * and dashes are ignored there (`669-103-42-74` finds `6691034274`, `999 999 990` finds `DE999999990`).
     */
    fun matches(row: InvoiceRow, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        val idQuery = trimmed.replace(ID_SEPARATORS, "")
        return row.counterpartyName?.contains(trimmed, ignoreCase = true) == true ||
            (idQuery.isNotEmpty() && row.counterpartyTaxId?.value?.replace(ID_SEPARATORS, "")?.contains(idQuery, ignoreCase = true) == true)
    }

    /** Identifier of a counterparty from the profile, in the form KSeF lists it. */
    fun taxIdOf(counterparty: Counterparty): CounterpartyTaxId? = when (counterparty.taxIdType) {
        TaxIdType.NIP -> counterparty.nip?.let { CounterpartyTaxId(TaxIdType.NIP, it) }
        TaxIdType.VAT_UE -> counterparty.taxIdText?.let { CounterpartyTaxId(TaxIdType.VAT_UE, it) }
        TaxIdType.OTHER -> counterparty.taxNumber?.let { CounterpartyTaxId(TaxIdType.OTHER, counterparty.countryCode + it) }
        TaxIdType.NONE -> null
    }

    /** Totals per currency (PLN first) of invoices in KSeF — unsent invoices excluded; VAT = gross − net. */
    fun totals(rows: List<InvoiceRow>): List<CurrencyTotals> =
        rows.filterNot { it.isUnsent }.groupBy { it.currency }
            .map { (currency, invoices) ->
                val net = invoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.netAmount }
                val gross = invoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.grossAmount }
                CurrencyTotals(currency, invoices.size, net, gross - net, gross)
            }
            .sortedWith(compareBy<CurrencyTotals> { it.currency != "PLN" }.thenBy { it.currency })

    /**
     * The current month in [zone] (the app uses Europe/Warsaw, like invoice dates), not in the time zone of the device:
     * on the last day of a month abroad the device month could differ from the month of the invoices.
     */
    fun currentMonth(clock: Clock, zone: ZoneId): YearMonth = YearMonth.now(clock.withZone(zone))

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

private const val NIP_LENGTH = 10
