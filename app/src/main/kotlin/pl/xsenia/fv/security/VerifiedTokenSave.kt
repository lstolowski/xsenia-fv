package pl.xsenia.fv.security

import kotlinx.coroutines.CancellationException
import pl.xsenia.fv.ksef.ConnectionCheckResult

/**
 * Replaces the KSeF token only after KSeF accepted the new one: a typo or a revoked token must not destroy a working
 * configuration (the token is shown only once in KSeF, so the old one usually cannot be restored). Another token NIP
 * also switches the active company profile, so nothing is stored before the check succeeds.
 */
class VerifiedTokenSave(
    private val check: suspend (nip: String?, token: String) -> ConnectionCheckResult,
    private val store: suspend (token: String) -> Unit,
    /** Called after the token was stored, e.g. to forget the KSeF session of the previous token. */
    private val afterSave: suspend (nip: String) -> Unit,
) {

    /** Stores [ksefToken] when KSeF authenticates with it; returns the check result ([ConnectionCheckResult.Success] — stored). */
    suspend fun save(ksefToken: String): ConnectionCheckResult {
        val token = ksefToken.trim()
        val nip = KsefTokenStore.nipOf(token)
        val result = try {
            check(nip, token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ConnectionCheckResult.ApiError(e.message)
        }
        if (result == ConnectionCheckResult.Success && nip != null) {
            store(token)
            afterSave(nip)
        }
        return result
    }
}
