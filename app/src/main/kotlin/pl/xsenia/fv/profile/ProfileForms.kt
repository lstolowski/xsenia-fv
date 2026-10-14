package pl.xsenia.fv.profile

import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.BankAccountInput
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.CompanyInput
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.Countries
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.CounterpartyInput
import pl.xsenia.fv.issue.InvoiceForm
import pl.xsenia.fv.issue.UnsentInvoice
import pl.xsenia.fv.lookup.LookedUpCompany

/** Default country of my company (the address on invoices is always in Poland). */
const val DEFAULT_COUNTRY = "Polska"

/** Fields of the „Moja firma” form exactly as typed. */
data class CompanyFormFields(
    val name: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val country: String = DEFAULT_COUNTRY,
    val email: String = "",
    val phone: String = "",
) {
    fun toInput() = CompanyInput(name, addressLine1, addressLine2, country, email, phone)

    /**
     * Fills the form with White List data (ADR 0007): name and address are overwritten (registry spelling),
     * e-mail and phone are kept. Without an address in the registry the typed one stays.
     */
    fun withLookup(company: LookedUpCompany): CompanyFormFields {
        val line1 = company.addressLine1 ?: return copy(name = company.name)
        return copy(name = company.name, addressLine1 = line1, addressLine2 = company.addressLine2.orEmpty(), country = company.country)
    }

    companion object {
        fun of(company: Company?): CompanyFormFields = company?.let {
            CompanyFormFields(
                name = it.name,
                addressLine1 = it.address.line1,
                addressLine2 = it.address.line2.orEmpty(),
                country = it.address.country.orEmpty(),
                email = it.email.orEmpty(),
                phone = it.phone.orEmpty(),
            )
        } ?: CompanyFormFields()
    }
}

/**
 * Fields of the counterparty form exactly as typed (ADR 0009): [nip] for Poland, [taxNumber] for another country;
 * the country and the identifier are read-only once the counterparty is saved.
 */
data class CounterpartyFormFields(
    val nip: String = "",
    val name: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val email: String = "",
    val countryCode: String = Countries.POLAND,
    val taxNumber: String = "",
    val defaultCurrency: InvoiceCurrency? = null,
) {
    fun toInput() = CounterpartyInput(nip, name, addressLine1, addressLine2, email, countryCode, taxNumber, defaultCurrency)

    /** Like [CompanyFormFields.withLookup]: name and address from the White List, the NIP normalized, e-mail kept. */
    fun withLookup(company: LookedUpCompany): CounterpartyFormFields {
        val line1 = company.addressLine1 ?: return copy(nip = company.nip, name = company.name)
        return copy(nip = company.nip, name = company.name, addressLine1 = line1, addressLine2 = company.addressLine2.orEmpty())
    }

    companion object {
        fun of(counterparty: Counterparty?): CounterpartyFormFields = counterparty?.let {
            CounterpartyFormFields(
                nip = it.nip.orEmpty(),
                name = it.name,
                addressLine1 = it.address.line1,
                addressLine2 = it.address.line2.orEmpty(),
                email = it.email.orEmpty(),
                countryCode = it.countryCode,
                taxNumber = it.taxNumber.orEmpty(),
                defaultCurrency = it.defaultCurrency,
            )
        } ?: CounterpartyFormFields()
    }
}

/** Fields of the bank account form exactly as typed. */
data class BankAccountFormFields(
    val name: String = "",
    val bank: String = "",
    val number: String = "",
    val currency: String = CompanyProfile.DEFAULT_CURRENCY,
) {
    fun toInput() = BankAccountInput(name, number, bank, currency)

    companion object {
        fun of(account: BankAccount?): BankAccountFormFields = account?.let {
            BankAccountFormFields(it.name, it.bank.orEmpty(), it.number, it.currency)
        } ?: BankAccountFormFields()
    }
}

/** Currencies offered for a bank account — the invoice currencies (ADR 0008). */
object BankAccountCurrencies {
    val ALL = InvoiceCurrency.entries.map { it.code }

    /** An account saved earlier with another currency keeps it as an extra option until the user changes it. */
    fun options(current: String): List<String> = if (current in ALL) ALL else ALL + current
}

/** Account of my company found in the White List; [saved] when the profile already has it. */
data class RegistryAccount(val number: String, val saved: Boolean)

/** Accounts from the White List offered for my company (ADR 0007, Q3). */
object RegistryAccounts {

    fun of(numbers: List<String>, saved: List<BankAccount>): List<RegistryAccount> =
        numbers.distinct().map { number -> RegistryAccount(number, saved.any { it.number == number }) }

    /** New accounts for the [selected] numbers not saved yet: PLN, no bank, name to be completed by the user. */
    fun toBankAccounts(selected: Collection<String>, saved: List<BankAccount>): List<BankAccount> =
        selected.distinct()
            .filter { number -> saved.none { it.number == number } }
            .map { number -> BankAccount(name = defaultName(number), number = number) }

    /** „Rachunek …2152” — the last four digits, until the user names the account. */
    fun defaultName(number: String) = "Rachunek …${number.takeLast(4)}"
}

/** Whether profile data is used by the new-invoice draft or by unsent invoices — warned about before deleting. */
object ProfileUsage {

    fun bankAccountUsed(number: String, draft: InvoiceForm?, unsent: List<UnsentInvoice>): Boolean =
        forms(draft, unsent).any { it.bankAccountNumber == number }

    fun counterpartyUsed(id: String, draft: InvoiceForm?, unsent: List<UnsentInvoice>): Boolean =
        forms(draft, unsent).any { it.counterpartyId == id }

    private fun forms(draft: InvoiceForm?, unsent: List<UnsentInvoice>) = listOfNotNull(draft) + unsent.map { it.form }
}
