package pl.xsenia.fv.issue

import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.issue.VatRate
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.model.InvoiceLine
import pl.xsenia.fv.invoice.model.Party
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.Countries
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.CounterpartyInput
import pl.xsenia.fv.invoice.profile.ProfileValidator
import pl.xsenia.fv.invoice.profile.TaxIdType
import pl.xsenia.fv.invoice.profile.Validation
import pl.xsenia.fv.invoice.validation.BankAccountNumberValidator
import java.math.BigDecimal
import java.time.LocalDate

/**
 * "Wystaw podobną" (US-015, ADR 0011): a new invoice form copied from a sales invoice.
 *
 * @property form the new invoice — dates and number as for any new invoice
 * @property newCounterparty the buyer of the invoice, missing among the counterparties — to be saved before the form
 */
data class SimilarInvoice(val form: InvoiceForm, val newCounterparty: Counterparty?) {

    companion object {

        /** Only an ordinary VAT invoice can be copied — the app does not issue corrections or advance invoices. */
        fun canCopy(invoice: Invoice): Boolean = invoice.kind == "VAT"

        /**
         * Copies the buyer, bank account, currency and lines of [invoice]. The issue and delivery dates and the number
         * are not copied: they are suggested exactly as for a new invoice ([InvoiceFormDefaults.withDefaults]).
         * The exchange rate is not copied either — the working rate is fetched for the new dates (ADR 0008).
         */
        fun of(invoice: Invoice, profile: CompanyProfile, today: LocalDate, existingNumbers: Collection<String>): SimilarInvoice {
            val currency = InvoiceCurrency.fromCode(invoice.currency) ?: InvoiceCurrency.DEFAULT
            val existing = findCounterparty(invoice.buyer, profile.counterparties)
            val newCounterparty = if (existing == null) counterpartyOf(invoice.buyer, profile.counterparties) else null
            val buyer = existing ?: newCounterparty
            val copied = InvoiceForm(
                counterpartyId = buyer?.id,
                bankAccountNumber = bankAccount(invoice, profile, currency),
                issueDate = today,
                deliveryDate = today,
                lines = invoice.lines.filterNot { it.isStateBefore }.map { lineOf(it, buyer) }.ifEmpty { listOf(LineForm()) },
                currency = currency,
            )
            return SimilarInvoice(InvoiceFormDefaults.withDefaults(copied, today, existingNumbers), newCounterparty)
        }

        /** The counterparty of [buyer]: by its identifier, or — without one — by country and name. */
        internal fun findCounterparty(buyer: Party, counterparties: List<Counterparty>): Counterparty? {
            val candidate = counterpartyOf(buyer, others = emptyList()) ?: return null
            return if (candidate.taxIdType == TaxIdType.NONE) {
                counterparties.firstOrNull { it.taxIdType == TaxIdType.NONE && it.countryCode == candidate.countryCode && it.name == candidate.name }
            } else {
                counterparties.firstOrNull { it.id == candidate.id }
            }
        }

        /**
         * [buyer] as a counterparty, validated like one typed in the form; null when the invoice data is not enough
         * (e.g. no address) — the user then chooses or adds the counterparty in the form.
         */
        internal fun counterpartyOf(buyer: Party, others: List<Counterparty>): Counterparty? {
            val countryCode = buyer.countryCode?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
                ?: if (buyer.nip != null) Countries.POLAND else return null
            val input = CounterpartyInput(
                nip = buyer.nip.orEmpty(),
                name = buyer.name.orEmpty(),
                addressLine1 = buyer.addressLines.firstOrNull().orEmpty(),
                addressLine2 = buyer.addressLines.drop(1).joinToString(", "),
                email = buyer.emails.firstOrNull().orEmpty(),
                countryCode = countryCode,
                // The parser gives the VAT UE number with its prefix and another identifier as "{KodKraju} {NrID}".
                taxNumber = buyer.euVatNumber ?: buyer.otherId?.let { id -> id.substringAfter(' ').takeIf { ' ' in id } ?: id }.orEmpty(),
            )
            return (ProfileValidator.counterparty(input, others) as? Validation.Valid)?.value
        }

        /** The account of the invoice when it is still among mine, otherwise the default one of the currency. */
        private fun bankAccount(invoice: Invoice, profile: CompanyProfile, currency: InvoiceCurrency): String? {
            val numbers = invoice.payment?.bankAccounts.orEmpty().map { it.number.filter(Char::isLetterOrDigit) }
            val normalized = numbers.map { BankAccountNumberValidator.normalize(it) ?: it }
            return profile.invoiceBankAccounts.firstOrNull { it.number in normalized }?.number
                ?: profile.defaultBankAccount(currency.code)?.number
        }

        private fun lineOf(line: InvoiceLine, buyer: Counterparty?) = LineForm(
            name = line.name.orEmpty(),
            quantity = line.quantity?.let(::decimalText) ?: "1",
            unit = line.unit?.takeIf { it.isNotBlank() } ?: LineUnits.DEFAULT,
            unitNetPrice = line.unitNetPrice?.let(::priceText).orEmpty(),
            vatRate = vatRate(line.vatRate, buyer),
        )

        /** `P_12` back to a rate of the form; a rate the app does not issue (e.g. `zw`) becomes the default for the buyer. */
        private fun vatRate(code: String?, buyer: Counterparty?): VatRate {
            val trimmed = code?.trim()
            return when {
                trimmed == VatRate.NOT_TAXABLE_OUTSIDE_EU_CODE || trimmed == "np" -> VatRate.NOT_TAXABLE
                else -> VatRate.entries.firstOrNull { it.code == trimmed }
                    ?: if (buyer?.isForeign == true) VatRate.NOT_TAXABLE else VatRate.DEFAULT
            }
        }

        /** As typed in the form: `1`, `2,5`. */
        private fun decimalText(value: BigDecimal): String = value.stripTrailingZeros().toPlainString().replace('.', ',')

        /** Prices with grosze: `100,00`, `12,50`, `0,1234`. */
        private fun priceText(value: BigDecimal): String {
            val stripped = value.stripTrailingZeros()
            return (if (stripped.scale() < 2) stripped.setScale(2) else stripped).toPlainString().replace('.', ',')
        }
    }
}
