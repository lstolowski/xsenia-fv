package pl.xsenia.fv.invoice.profile

import pl.xsenia.fv.invoice.validation.BankAccountNumberValidator
import pl.xsenia.fv.invoice.validation.NipValidator

/** Form field of the company, bank account and counterparty forms. */
enum class ProfileField { NAME, NIP, ADDRESS_LINE1, ADDRESS_LINE2, COUNTRY, EMAIL, PHONE, BANK, ACCOUNT_NUMBER, CURRENCY }

enum class FieldIssueType { REQUIRED, TOO_LONG, INVALID_NIP, INVALID_ACCOUNT_NUMBER, INVALID_EMAIL, INVALID_CURRENCY, DUPLICATE }

/** Problem with one form field; the UI turns it into a Polish message. [maxLength] is set for [FieldIssueType.TOO_LONG]. */
data class FieldIssue(val field: ProfileField, val type: FieldIssueType, val maxLength: Int? = null)

sealed interface Validation<out T> {
    data class Valid<T>(val value: T) : Validation<T>

    data class Invalid(val issues: List<FieldIssue>) : Validation<Nothing>
}

/** Form values exactly as typed. */
data class CompanyInput(
    val name: String,
    val addressLine1: String,
    val addressLine2: String = "",
    val country: String = "",
    val email: String = "",
    val phone: String = "",
)

data class BankAccountInput(
    val name: String,
    val number: String,
    val bank: String = "",
    val currency: String = CompanyProfile.INVOICE_CURRENCY,
)

data class CounterpartyInput(
    val nip: String,
    val name: String,
    val addressLine1: String,
    val addressLine2: String = "",
    val country: String = "",
    val email: String = "",
)

/**
 * Validates form input before it is saved (ADR 0006): required fields, NIP and NRB checksums, FA(3) length limits,
 * e-mail format and uniqueness. Values are trimmed; blank optional fields become null; NIP and account numbers are
 * normalized (spaces, dashes and the `PL` prefix are accepted). All issues are reported at once.
 */
object ProfileValidator {

    // FA(3) limits: TZnakowy512 (names, address lines), TZnakowy (bank name), TAdresEmail, TNumerTelefonu.
    const val MAX_NAME = 512
    const val MAX_ADDRESS_LINE = 512
    const val MAX_BANK_NAME = 256
    const val MAX_EMAIL = 255
    const val MAX_PHONE = 16
    const val MAX_COUNTRY = 64

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val CURRENCY = Regex("^[A-Z]{3}$")

    /** [nip] is the NIP of the KSeF token — not typed by the user. */
    fun company(nip: String, input: CompanyInput): Validation<Company> = collect {
        val name = required(ProfileField.NAME, input.name, MAX_NAME)
        val address = address(input.addressLine1, input.addressLine2, input.country)
        val email = email(input.email)
        val phone = optional(ProfileField.PHONE, input.phone, MAX_PHONE)
        if (name == null || address == null) null else Company(name, nip, address, email, phone)
    }

    /** [others] — the accounts already saved, without the one being edited (its number may stay the same). */
    fun bankAccount(input: BankAccountInput, others: List<BankAccount>): Validation<BankAccount> = collect {
        val name = required(ProfileField.NAME, input.name, MAX_NAME)
        val bank = optional(ProfileField.BANK, input.bank, MAX_BANK_NAME)
        val number = when {
            input.number.isBlank() -> null.also { report(ProfileField.ACCOUNT_NUMBER, FieldIssueType.REQUIRED) }
            else -> BankAccountNumberValidator.normalize(input.number.trim())
                ?.also { number -> if (others.any { it.number == number }) report(ProfileField.ACCOUNT_NUMBER, FieldIssueType.DUPLICATE) }
                ?: null.also { report(ProfileField.ACCOUNT_NUMBER, FieldIssueType.INVALID_ACCOUNT_NUMBER) }
        }
        val currency = input.currency.trim().uppercase().ifEmpty { CompanyProfile.INVOICE_CURRENCY }
        if (!CURRENCY.matches(currency)) report(ProfileField.CURRENCY, FieldIssueType.INVALID_CURRENCY)
        if (name == null || number == null) null else BankAccount(name, bank, number, currency)
    }

    /**
     * [others] — the counterparties already saved, without the one being edited. The NIP of an existing counterparty
     * cannot change (ADR 0006) — the form keeps it read-only.
     */
    fun counterparty(input: CounterpartyInput, others: List<Counterparty>): Validation<Counterparty> = collect {
        val nip = nip(input.nip)?.also { nip -> if (others.any { it.nip == nip }) report(ProfileField.NIP, FieldIssueType.DUPLICATE) }
        val name = required(ProfileField.NAME, input.name, MAX_NAME)
        val address = address(input.addressLine1, input.addressLine2, input.country)
        val email = email(input.email)
        if (nip == null || name == null || address == null) null else Counterparty(name, nip, address, email)
    }

    private fun <T> collect(build: Collector.() -> T?): Validation<T> {
        val collector = Collector()
        val value = collector.build()
        return if (collector.issues.isEmpty() && value != null) Validation.Valid(value) else Validation.Invalid(collector.issues)
    }

    private class Collector {
        val issues = mutableListOf<FieldIssue>()

        fun report(field: ProfileField, type: FieldIssueType, maxLength: Int? = null) {
            issues += FieldIssue(field, type, maxLength)
        }

        fun required(field: ProfileField, raw: String, maxLength: Int): String? {
            val value = raw.trim()
            if (value.isEmpty()) return null.also { report(field, FieldIssueType.REQUIRED) }
            return limited(field, value, maxLength)
        }

        fun optional(field: ProfileField, raw: String, maxLength: Int): String? =
            raw.trim().takeIf { it.isNotEmpty() }?.let { limited(field, it, maxLength) }

        fun nip(raw: String): String? {
            if (raw.isBlank()) return null.also { report(ProfileField.NIP, FieldIssueType.REQUIRED) }
            return NipValidator.normalize(raw.trim()) ?: null.also { report(ProfileField.NIP, FieldIssueType.INVALID_NIP) }
        }

        fun email(raw: String): String? =
            optional(ProfileField.EMAIL, raw, MAX_EMAIL)?.let { email ->
                email.takeIf(EMAIL::matches) ?: null.also { report(ProfileField.EMAIL, FieldIssueType.INVALID_EMAIL) }
            }

        fun address(line1: String, line2: String, country: String): Address? {
            val first = required(ProfileField.ADDRESS_LINE1, line1, MAX_ADDRESS_LINE)
            val second = optional(ProfileField.ADDRESS_LINE2, line2, MAX_ADDRESS_LINE)
            val countryName = optional(ProfileField.COUNTRY, country, MAX_COUNTRY)
            return first?.let { Address(it, second, countryName) }
        }

        private fun limited(field: ProfileField, value: String, maxLength: Int): String? =
            if (value.length > maxLength) null.also { report(field, FieldIssueType.TOO_LONG, maxLength) } else value
    }
}
