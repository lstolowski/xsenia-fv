package pl.xsenia.fv.issue

import kotlinx.serialization.Serializable
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.ExchangeRateDateRule
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.issue.DefaultDates
import pl.xsenia.fv.invoice.issue.DraftIssue
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.DraftValidator
import pl.xsenia.fv.invoice.issue.InvoiceNumbering
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.invoice.issue.VatRate
import pl.xsenia.fv.ksef.http.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate

/** Form of a new sales invoice exactly as typed — saved as a draft after every change (US-006). */
@Serializable
data class InvoiceForm(
    val counterpartyNip: String? = null,
    val bankAccountNumber: String? = null,
    @Serializable(with = LocalDateSerializer::class) val issueDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val deliveryDate: LocalDate,
    val invoiceNumber: String = "",
    /** Once the user edits the number, it is no longer replaced by suggestions. */
    val numberEdited: Boolean = false,
    /** Once the user picks a date, it is kept; otherwise dates are recomputed for today when the draft is opened. */
    val datesEdited: Boolean = false,
    val lines: List<LineForm> = listOf(LineForm()),
    /** Drafts and unsent invoices saved before US-011 have no currency and load in PLN. */
    val currency: InvoiceCurrency = InvoiceCurrency.DEFAULT,
    /** Working NBP rate of a foreign currency (ADR 0008); see [workingRate]. */
    val exchangeRate: SavedExchangeRate? = null,
) {
    /** Day the working rate is chosen for: the issue date from the form stands for the day of issue (ADR 0008). */
    val rateReferenceDate: LocalDate
        get() = ExchangeRateDateRule.referenceDate(deliveryDate, issueDate)

    /** The saved working rate while it still fits the currency and dates; null for PLN or when it has to be fetched again. */
    val workingRate: ExchangeRate?
        get() = exchangeRate
            ?.takeIf { currency.isForeign && it.currency == currency && it.referenceDate == rateReferenceDate }
            ?.toExchangeRate()
}

/** [ExchangeRate] as saved with the form, with the [referenceDate] it was chosen for. */
@Serializable
data class SavedExchangeRate(
    val currency: InvoiceCurrency,
    val rate: String,
    val tableNumber: String,
    @Serializable(with = LocalDateSerializer::class) val effectiveDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val referenceDate: LocalDate,
) {
    /** Null when the saved values are not a valid rate (edited or corrupted file) — the rate is fetched again. */
    fun toExchangeRate(): ExchangeRate? = runCatching { ExchangeRate(currency, BigDecimal(rate), tableNumber, effectiveDate) }.getOrNull()

    companion object {
        fun of(rate: ExchangeRate, referenceDate: LocalDate) =
            SavedExchangeRate(rate.currency, rate.rate.toPlainString(), rate.tableNumber, rate.effectiveDate, referenceDate)
    }
}

/** Drafts saved before ADR 0005 have no [vatRate] and load with the default 23%. */
@Serializable
data class LineForm(
    val name: String = "",
    val quantity: String = "1",
    val unit: String = LineUnits.DEFAULT,
    val unitNetPrice: String = "",
    val vatRate: VatRate = VatRate.DEFAULT,
)

/** Units offered in the form (`P_8A`, ADR 0005). */
object LineUnits {
    val ALL = listOf("szt.", "dni", "godz.", "usł.")
    const val DEFAULT = "godz."
}

/** Default values of a new form and refreshing of a saved draft. */
object InvoiceFormDefaults {

    fun newForm(profile: CompanyProfile?, today: LocalDate, existingNumbers: Collection<String>): InvoiceForm {
        val deliveryDate = DefaultDates.deliveryDate(today)
        return InvoiceForm(
            counterpartyNip = profile?.counterparties?.singleOrNull()?.nip,
            bankAccountNumber = profile?.defaultBankAccount(InvoiceCurrency.DEFAULT.code)?.number,
            issueDate = today,
            deliveryDate = deliveryDate,
            invoiceNumber = InvoiceNumbering.suggest(deliveryDate, existingNumbers),
        )
    }

    /**
     * Defaults that were not available when the form was opened (no account or counterparty yet): the default account
     * of the invoice currency and the only counterparty. Choices already made are kept.
     */
    fun fillMissing(form: InvoiceForm, profile: CompanyProfile): InvoiceForm = form.copy(
        counterpartyNip = form.counterpartyNip ?: profile.counterparties.singleOrNull()?.nip,
        bankAccountNumber = form.bankAccountNumber ?: profile.defaultBankAccount(form.currency.code)?.number,
    )

    /**
     * New invoice currency: when the chosen account is in another currency and there is an account in the new one,
     * the first such account is chosen; otherwise the account stays (only a warning — ADR 0008, Q2).
     */
    fun changeCurrency(form: InvoiceForm, currency: InvoiceCurrency, profile: CompanyProfile?): InvoiceForm {
        val accounts = profile?.invoiceBankAccounts.orEmpty()
        val current = accounts.firstOrNull { it.number == form.bankAccountNumber }
        val account = current?.takeIf { it.currency == currency.code } ?: accounts.firstOrNull { it.currency == currency.code } ?: current
        return form.copy(currency = currency, bankAccountNumber = account?.number ?: form.bankAccountNumber)
    }

    /**
     * A draft opened on a later day: dates not chosen by the user are moved to today's defaults and an
     * unedited number is suggested again (other invoices may have been issued meanwhile).
     */
    fun refresh(draft: InvoiceForm, today: LocalDate, existingNumbers: Collection<String>): InvoiceForm {
        val dated = if (draft.datesEdited) draft else draft.copy(issueDate = today, deliveryDate = DefaultDates.deliveryDate(today))
        return if (dated.numberEdited) dated else dated.copy(invoiceNumber = InvoiceNumbering.suggest(dated.deliveryDate, existingNumbers))
    }
}

enum class FormIssueType {
    COUNTERPARTY_MISSING,
    BANK_ACCOUNT_MISSING,
    LINE_QUANTITY_INVALID,
    LINE_PRICE_INVALID,
}

data class FormIssue(val type: FormIssueType, val lineIndex: Int? = null)

/** Result of turning the form into a [SalesInvoiceDraft]: the draft, or the reasons it cannot be built yet. */
data class FormCheck(
    val draft: SalesInvoiceDraft?,
    val formIssues: List<FormIssue>,
    val draftIssues: List<DraftIssue>,
) {
    val canPreview: Boolean get() = draft != null && formIssues.isEmpty() && draftIssues.none { it.blocking }
}

object InvoiceFormCheck {

    /** @param exchangeRate rate of a foreign currency invoice: the working one in the form, the final one for the preview */
    fun check(
        form: InvoiceForm,
        profile: CompanyProfile,
        existingNumbers: Collection<String>,
        today: LocalDate,
        exchangeRate: ExchangeRate? = form.workingRate,
    ): FormCheck {
        val formIssues = mutableListOf<FormIssue>()
        val buyer = profile.counterparties.firstOrNull { it.nip == form.counterpartyNip }
        if (buyer == null) formIssues += FormIssue(FormIssueType.COUNTERPARTY_MISSING)
        val account = profile.invoiceBankAccounts.firstOrNull { it.number == form.bankAccountNumber }
        if (account == null) formIssues += FormIssue(FormIssueType.BANK_ACCOUNT_MISSING)

        val lines = form.lines.mapIndexedNotNull { index, line ->
            val quantity = DecimalInput.parse(line.quantity)
            val price = DecimalInput.parse(line.unitNetPrice)
            if (quantity == null) formIssues += FormIssue(FormIssueType.LINE_QUANTITY_INVALID, index)
            if (price == null) formIssues += FormIssue(FormIssueType.LINE_PRICE_INVALID, index)
            if (quantity != null && price != null) DraftLine(line.name.trim(), quantity, line.unit.trim(), price, line.vatRate) else null
        }

        val seller = profile.company
        if (buyer == null || account == null || seller == null || lines.size != form.lines.size) {
            return FormCheck(null, formIssues, emptyList())
        }
        val draft = SalesInvoiceDraft(
            form.invoiceNumber.trim(), form.issueDate, form.deliveryDate, seller, buyer, account, lines, form.currency, exchangeRate,
        )
        return FormCheck(draft, formIssues, DraftValidator.validate(draft, existingNumbers, today))
    }
}
