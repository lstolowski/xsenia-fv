package pl.xsenia.fv.invoice.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.util.UUID

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
    val currency: String = CompanyProfile.DEFAULT_CURRENCY,
)

/**
 * Buyer (Podmiot2), Polish or foreign (ADR 0009). The country and the tax identifier do not change once the
 * counterparty is saved; the app refers to it by [id].
 *
 * Counterparties saved before US-012 have only [nip]: they load as Polish ones with [id] = NIP, so drafts and unsent
 * invoices that point to them by NIP keep working.
 *
 * @property nip NIP of a Polish counterparty ([countryCode] `PL`); null for a foreign one
 * @property countryCode ISO country code, as `KodKraju` in FA(3)
 * @property taxNumber VAT UE number without the country prefix (EU), or the tax identifier of another country (optional)
 * @property defaultCurrency currency set on a new invoice when this counterparty is chosen
 */
@Serializable
data class Counterparty(
    val name: String,
    val nip: String? = null,
    val address: Address,
    val email: String? = null,
    val countryCode: String = Countries.POLAND,
    val taxNumber: String? = null,
    val defaultCurrency: InvoiceCurrency? = null,
    val id: String = idFor(countryCode, nip, taxNumber),
) {
    val taxIdType: TaxIdType
        get() = when {
            nip != null -> TaxIdType.NIP
            Countries.isOtherEu(countryCode) -> TaxIdType.VAT_UE
            taxNumber != null -> TaxIdType.OTHER
            else -> TaxIdType.NONE
        }

    val isForeign: Boolean get() = countryCode != Countries.POLAND

    /** The identifier as printed: `6691034274`, `DE123456789`, `EL123456789`, the foreign ID, or null. */
    val taxIdText: String?
        get() = when (taxIdType) {
            TaxIdType.NIP -> nip
            TaxIdType.VAT_UE -> taxNumber?.let { Countries.vatPrefix(countryCode) + it }
            TaxIdType.OTHER -> taxNumber
            TaxIdType.NONE -> null
        }

    companion object {
        /** NIP for a Polish counterparty, `{country}-{number}` for a foreign one; a random one without a number. */
        fun idFor(countryCode: String, nip: String?, taxNumber: String?): String =
            nip ?: taxNumber?.let { "$countryCode-$it" } ?: "$countryCode-${UUID.randomUUID()}"
    }
}

/** How the buyer is identified in FA(3) `Podmiot2/DaneIdentyfikacyjne` (ADR 0009). */
enum class TaxIdType {
    /** `NIP` — Polish buyer. */
    NIP,

    /** `KodUE` + `NrVatUE` — buyer from another EU country. */
    VAT_UE,

    /** `KodKraju` + `NrID` — buyer from outside the EU. */
    OTHER,

    /** `BrakID` — buyer from outside the EU without an identifier. */
    NONE,
}

/** What is still missing before a sales invoice can be issued. */
enum class ProfileGap { COMPANY, BANK_ACCOUNT, COUNTERPARTY }

/** All data of one of my companies, kept in `profiles/{nip}/` (ADR 0006). */
data class CompanyProfile(
    val nip: String,
    val company: Company? = null,
    val bankAccounts: List<BankAccount> = emptyList(),
    val counterparties: List<Counterparty> = emptyList(),
) {

    /**
     * Accounts offered on a new invoice: all of them. An account in another currency than the invoice only gives
     * a warning (ADR 0008, decision Q2).
     */
    val invoiceBankAccounts: List<BankAccount>
        get() = bankAccounts

    /** Default account of an invoice in [currency]: the first one in that currency, otherwise the first one. */
    fun defaultBankAccount(currency: String): BankAccount? =
        bankAccounts.firstOrNull { it.currency == currency } ?: bankAccounts.firstOrNull()

    val gaps: List<ProfileGap>
        get() = listOfNotNull(
            ProfileGap.COMPANY.takeIf { company == null },
            ProfileGap.BANK_ACCOUNT.takeIf { invoiceBankAccounts.isEmpty() },
            ProfileGap.COUNTERPARTY.takeIf { counterparties.isEmpty() },
        )

    val canIssueInvoices: Boolean
        get() = gaps.isEmpty()

    companion object {
        /** Currency of a new bank account. */
        const val DEFAULT_CURRENCY = "PLN"
    }
}
