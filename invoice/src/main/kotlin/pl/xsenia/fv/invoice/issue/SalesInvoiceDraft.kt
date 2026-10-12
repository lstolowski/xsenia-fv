package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.config.BankAccount
import pl.xsenia.fv.invoice.config.Company
import pl.xsenia.fv.invoice.config.Counterparty
import java.math.BigDecimal
import java.time.LocalDate

/**
 * New sales invoice entered by the user (US-006): PLN, bank transfer due in 21 days; VAT rate per line (ADR 0005).
 */
data class SalesInvoiceDraft(
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val deliveryDate: LocalDate,
    val seller: Company,
    val buyer: Counterparty,
    val bankAccount: BankAccount,
    val lines: List<DraftLine>,
)

/** Invoice line as entered: the user types the unit net price (decision C5). */
data class DraftLine(
    val name: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitNetPrice: BigDecimal,
    val vatRate: VatRate = VatRate.DEFAULT,
)
