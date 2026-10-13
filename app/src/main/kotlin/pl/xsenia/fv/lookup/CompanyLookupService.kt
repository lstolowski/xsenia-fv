package pl.xsenia.fv.lookup

import pl.xsenia.fv.invoice.validation.BankAccountNumberValidator
import pl.xsenia.fv.invoice.validation.NipValidator
import pl.xsenia.fv.whitelist.WhiteListClient
import pl.xsenia.fv.whitelist.WhiteListFailure
import pl.xsenia.fv.whitelist.WhiteListResult
import java.time.LocalDate

/** Company data found in the White List, ready to fill the „Moja firma” / „Kontrahent” forms (ADR 0007). */
data class LookedUpCompany(
    val nip: String,
    /** Registry spelling (upper case); for a sole proprietor the owner's name. */
    val name: String,
    /** Null when the registry has no address — the form keeps its address then. */
    val addressLine1: String?,
    val addressLine2: String?,
    val isActiveVatPayer: Boolean,
    /** Valid 26-digit NRB numbers registered for the company. */
    val accountNumbers: List<String>,
    /** The registry lists Polish taxpayers only. */
    val country: String = "Polska",
)

sealed interface CompanyLookupResult {
    data class Found(val company: LookedUpCompany) : CompanyLookupResult

    /** The NIP has a wrong length or checksum — the registry was not asked. */
    data object InvalidNip : CompanyLookupResult

    data object NotFound : CompanyLookupResult

    data class Failed(val failure: WhiteListFailure) : CompanyLookupResult
}

/** Fetches company data by NIP from the White List; shared by every form with a NIP field. */
class CompanyLookupService(
    private val client: WhiteListClient,
    private val today: () -> LocalDate,
) {

    /** Accepts the NIP as typed (spaces, dashes, `PL` prefix); validates it before calling the registry. */
    suspend fun lookup(rawNip: String): CompanyLookupResult {
        val nip = NipValidator.normalize(rawNip.trim()) ?: return CompanyLookupResult.InvalidNip
        return when (val result = client.findByNip(nip, today())) {
            is WhiteListResult.Found -> with(result.subject) {
                CompanyLookupResult.Found(
                    LookedUpCompany(
                        nip = nip,
                        name = name,
                        addressLine1 = address?.line1,
                        addressLine2 = address?.line2,
                        isActiveVatPayer = isActiveVatPayer,
                        accountNumbers = accountNumbers.filter(BankAccountNumberValidator::isValid).distinct(),
                    ),
                )
            }
            WhiteListResult.NotFound -> CompanyLookupResult.NotFound
            is WhiteListResult.Failed -> CompanyLookupResult.Failed(result.failure)
        }
    }
}
