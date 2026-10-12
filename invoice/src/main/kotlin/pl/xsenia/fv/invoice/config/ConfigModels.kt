package pl.xsenia.fv.invoice.config

/** Address as used in FA(3) (`AdresL1`, `AdresL2`); the country code on invoices is always `PL`. */
data class Address(val line1: String, val line2: String?, val country: String?)

/** My company — the seller (Podmiot1), from `company.json`. */
data class Company(
    val name: String,
    val nip: String,
    val address: Address,
    val email: String?,
    val phone: String?,
)

/** Bank account from `banks.json`; [number] is the 26-digit NRB. */
data class BankAccount(
    val name: String,
    val bank: String?,
    val number: String,
    val currency: String,
)

/** Buyer (Podmiot2) from `counterparties.json`. */
data class Counterparty(
    val name: String,
    val nip: String,
    val address: Address,
    val email: String?,
)
