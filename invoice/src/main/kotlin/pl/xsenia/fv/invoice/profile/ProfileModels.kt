package pl.xsenia.fv.invoice.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Field names in the profile files are those of the former company.json / banks.json / counterparties.json (ADR 0006).

/** Address as used in FA(3) (`AdresL1`, `AdresL2`); the country code on invoices is always `PL`. */
@Serializable
data class Address(val line1: String, val line2: String? = null, val country: String? = null)

/** My company — the seller (Podmiot1); [nip] is the NIP of the KSeF token (ADR 0006). */
@Serializable
data class Company(
    val name: String,
    val nip: String,
    val address: Address,
    val email: String? = null,
    val phone: String? = null,
)

/** Bank account of my company; [number] is the 26-digit NRB. */
@Serializable
data class BankAccount(
    val name: String,
    val bank: String? = null,
    @SerialName("account") val number: String,
    val currency: String = CompanyProfile.INVOICE_CURRENCY,
)

/** Buyer (Podmiot2); identified by [nip], which does not change once the counterparty is saved. */
@Serializable
data class Counterparty(
    val name: String,
    val nip: String,
    val address: Address,
    val email: String? = null,
)

/** What is still missing before a sales invoice can be issued. */
enum class ProfileGap { COMPANY, BANK_ACCOUNT, COUNTERPARTY }

/** All data of one of my companies, kept in `profiles/{nip}/` (ADR 0006). */
data class CompanyProfile(
    val nip: String,
    val company: Company? = null,
    val bankAccounts: List<BankAccount> = emptyList(),
    val counterparties: List<Counterparty> = emptyList(),
) {

    /** Accounts offered on a new invoice — invoices are issued in PLN only; the first one is the default. */
    val invoiceBankAccounts: List<BankAccount>
        get() = bankAccounts.filter { it.currency == INVOICE_CURRENCY }

    val gaps: List<ProfileGap>
        get() = listOfNotNull(
            ProfileGap.COMPANY.takeIf { company == null },
            ProfileGap.BANK_ACCOUNT.takeIf { invoiceBankAccounts.isEmpty() },
            ProfileGap.COUNTERPARTY.takeIf { counterparties.isEmpty() },
        )

    val canIssueInvoices: Boolean
        get() = gaps.isEmpty()

    companion object {
        const val INVOICE_CURRENCY = "PLN"
    }
}
