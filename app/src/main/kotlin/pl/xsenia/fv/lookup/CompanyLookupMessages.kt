package pl.xsenia.fv.lookup

import android.content.res.Resources
import pl.xsenia.fv.R
import pl.xsenia.fv.whitelist.WhiteListFailure

/** Polish texts for White List lookup results shown next to the NIP field. */
class CompanyLookupMessages(private val resources: Resources) {

    /** Message for a lookup that filled nothing; null when the company was found. */
    fun problem(result: CompanyLookupResult): String? = when (result) {
        is CompanyLookupResult.Found -> null
        CompanyLookupResult.InvalidNip -> resources.getString(R.string.lookup_invalid_nip)
        CompanyLookupResult.NotFound -> resources.getString(R.string.lookup_not_found)
        is CompanyLookupResult.Failed -> failure(result.failure)
    }

    /** Warning for a found company that is not an active VAT payer; null otherwise. */
    fun vatWarning(result: CompanyLookupResult): String? =
        (result as? CompanyLookupResult.Found)?.company
            ?.takeUnless { it.isActiveVatPayer }
            ?.let { resources.getString(R.string.lookup_inactive_vat_payer) }

    private fun failure(failure: WhiteListFailure): String = when (failure) {
        is WhiteListFailure.Network -> resources.getString(R.string.lookup_network)
        WhiteListFailure.RateLimited -> resources.getString(R.string.lookup_rate_limited)
        is WhiteListFailure.Server -> resources.getString(R.string.lookup_server, failure.httpStatus)
        is WhiteListFailure.Api -> resources.getString(R.string.lookup_api_error, failure.message ?: failure.code, failure.code)
    }
}
