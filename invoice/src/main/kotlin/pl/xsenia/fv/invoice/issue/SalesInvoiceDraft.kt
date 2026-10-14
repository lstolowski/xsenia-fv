package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import java.math.BigDecimal
import java.time.LocalDate

/**
 * New sales invoice entered by the user (US-006): bank transfer due in 21 days; VAT rate per line (ADR 0005).
 *
 * Amounts are in [currency]; a foreign currency needs the NBP [exchangeRate] of the same currency (ADR 0008).
 */
data class SalesInvoiceDraft(
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val deliveryDate: LocalDate,
    val seller: Company,
    val buyer: Counterparty,
    val bankAccount: BankAccount,
    val lines: List<DraftLine>,
    val currency: InvoiceCurrency = InvoiceCurrency.DEFAULT,
    val exchangeRate: ExchangeRate? = null,
) {
    /** The rate used on the invoice: only for a foreign currency and only when it is of that currency. */
    val effectiveExchangeRate: ExchangeRate?
        get() = exchangeRate?.takeIf { currency.isForeign && it.currency == currency }

    /** A foreign buyer may not be able to pay without the BIC (SWIFT) of the bank — warned about in the preview (US-014). */
    val swiftMissingForForeignBuyer: Boolean
        get() = buyer.isForeign && bankAccount.swift == null
}

/** Invoice line as entered: the user types the unit net price (decision C5). */
data class DraftLine(
    val name: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitNetPrice: BigDecimal,
    val vatRate: VatRate = VatRate.DEFAULT,
)
