package pl.xsenia.fv.invoice.issue

import java.math.BigDecimal
import java.time.LocalDate

enum class DraftIssueType {
    NUMBER_MISSING,
    NUMBER_TOO_LONG,
    NUMBER_DUPLICATE,

    /** Warning only: `NN/MM/RRRR` month differs from the delivery month. */
    NUMBER_MONTH_MISMATCH,
    ISSUE_DATE_IN_FUTURE,
    NO_LINES,
    LINE_NAME_MISSING,
    LINE_NAME_TOO_LONG,
    LINE_UNIT_MISSING,
    LINE_UNIT_TOO_LONG,
    LINE_QUANTITY_NOT_POSITIVE,
    LINE_QUANTITY_TOO_PRECISE,
    LINE_PRICE_NEGATIVE,
    LINE_PRICE_TOO_PRECISE,

    /** More integer digits than FA(3) allows: `TIlosci` — 16, `TKwotowy2` — 14, `TKwotowy` — 16. */
    LINE_QUANTITY_TOO_LARGE,
    LINE_PRICE_TOO_LARGE,

    /** The net value of a line (`P_11`) does not fit `TKwotowy`. */
    LINE_AMOUNT_TOO_LARGE,

    /** A total of the invoice (`P_13_x`, `P_14_x`, `P_14_xW`, `P_15`) does not fit `TKwotowy`. */
    AMOUNT_TOO_LARGE,
    TOTAL_NOT_POSITIVE,

    /** Foreign currency invoice without the NBP rate of that currency (ADR 0008). */
    EXCHANGE_RATE_MISSING,

    /** Warning only: the bank account is in another currency than the invoice (decision Q2). */
    BANK_ACCOUNT_CURRENCY_MISMATCH,
}

/** @property lineIndex 0-based line, for line issues. @property blocking false for warnings. */
data class DraftIssue(val type: DraftIssueType, val lineIndex: Int? = null, val blocking: Boolean = true)

/** Checks a draft before preview and sending; limits follow the FA(3) schema. */
object DraftValidator {

    // FA(3): P_2 and P_8A — TZnakowy (256), P_7 — TZnakowy512, P_8B — 6 decimals, P_9A — 8 decimals.
    private const val MAX_NUMBER = 256
    private const val MAX_NAME = 512
    private const val MAX_UNIT = 256
    private const val QUANTITY_SCALE = 6
    private const val PRICE_SCALE = 8

    // Integer digits (the schema patterns): TIlosci [1-9]\d{0,15}, TKwotowy2 [1-9]\d{0,13}, TKwotowy [1-9]\d{0,15}.
    private const val MAX_QUANTITY_DIGITS = 16
    private const val MAX_PRICE_DIGITS = 14
    private const val MAX_AMOUNT_DIGITS = 16

    fun validate(draft: SalesInvoiceDraft, existingNumbers: Collection<String>, today: LocalDate): List<DraftIssue> = buildList {
        val number = draft.invoiceNumber.trim()
        when {
            number.isEmpty() -> add(DraftIssue(DraftIssueType.NUMBER_MISSING))
            number.length > MAX_NUMBER -> add(DraftIssue(DraftIssueType.NUMBER_TOO_LONG))
            InvoiceNumbering.isDuplicate(number, existingNumbers) -> add(DraftIssue(DraftIssueType.NUMBER_DUPLICATE))
        }
        if (number.isNotEmpty() && InvoiceNumbering.monthMismatch(number, draft.deliveryDate)) {
            add(DraftIssue(DraftIssueType.NUMBER_MONTH_MISMATCH, blocking = false))
        }
        if (draft.issueDate.isAfter(today)) add(DraftIssue(DraftIssueType.ISSUE_DATE_IN_FUTURE))
        if (draft.lines.isEmpty()) add(DraftIssue(DraftIssueType.NO_LINES))

        draft.lines.forEachIndexed { index, line ->
            fun issue(type: DraftIssueType) = add(DraftIssue(type, index))
            when {
                line.name.isBlank() -> issue(DraftIssueType.LINE_NAME_MISSING)
                line.name.trim().length > MAX_NAME -> issue(DraftIssueType.LINE_NAME_TOO_LONG)
            }
            when {
                line.unit.isBlank() -> issue(DraftIssueType.LINE_UNIT_MISSING)
                line.unit.trim().length > MAX_UNIT -> issue(DraftIssueType.LINE_UNIT_TOO_LONG)
            }
            if (line.quantity.signum() <= 0) issue(DraftIssueType.LINE_QUANTITY_NOT_POSITIVE)
            if (line.quantity.stripTrailingZeros().scale() > QUANTITY_SCALE) issue(DraftIssueType.LINE_QUANTITY_TOO_PRECISE)
            if (line.unitNetPrice.signum() < 0) issue(DraftIssueType.LINE_PRICE_NEGATIVE)
            if (line.unitNetPrice.stripTrailingZeros().scale() > PRICE_SCALE) issue(DraftIssueType.LINE_PRICE_TOO_PRECISE)
            if (integerDigits(line.quantity) > MAX_QUANTITY_DIGITS) issue(DraftIssueType.LINE_QUANTITY_TOO_LARGE)
            if (integerDigits(line.unitNetPrice) > MAX_PRICE_DIGITS) issue(DraftIssueType.LINE_PRICE_TOO_LARGE)
            if (integerDigits(InvoiceCalculator.lineNet(line)) > MAX_AMOUNT_DIGITS) issue(DraftIssueType.LINE_AMOUNT_TOO_LARGE)
        }
        if (draft.lines.isNotEmpty()) {
            val totals = InvoiceCalculator.totals(draft)
            if (totals.gross <= BigDecimal.ZERO) add(DraftIssue(DraftIssueType.TOTAL_NOT_POSITIVE))
            if (amounts(totals).any { integerDigits(it) > MAX_AMOUNT_DIGITS }) add(DraftIssue(DraftIssueType.AMOUNT_TOO_LARGE))
        }
        if (draft.currency.isForeign && draft.effectiveExchangeRate == null) add(DraftIssue(DraftIssueType.EXCHANGE_RATE_MISSING))
        if (draft.bankAccount.currency != draft.currency.code) {
            add(DraftIssue(DraftIssueType.BANK_ACCOUNT_CURRENCY_MISMATCH, blocking = false))
        }
    }

    /** Every amount of the invoice that is written as `TKwotowy` (net, VAT, gross, VAT in PLN — in total and per rate). */
    private fun amounts(totals: InvoiceTotals): List<BigDecimal> =
        listOfNotNull(totals.net, totals.vat, totals.gross, totals.vatPln) +
            totals.byRate.flatMap { listOfNotNull(it.net, it.vat, it.vatPln) }

    private fun integerDigits(value: BigDecimal): Int = value.abs().toBigInteger().toString().length
}
