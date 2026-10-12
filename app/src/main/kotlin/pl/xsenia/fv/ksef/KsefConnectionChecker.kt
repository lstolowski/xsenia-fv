package pl.xsenia.fv.ksef

import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import java.io.IOException

sealed interface ConnectionCheckResult {
    data object Success : ConnectionCheckResult
    data object MissingToken : ConnectionCheckResult
    data object MissingCompany : ConnectionCheckResult
    data class AuthenticationFailed(val statusCode: Int, val description: String) : ConnectionCheckResult
    data class RateLimited(val retryAfterSeconds: Long?) : ConnectionCheckResult
    data class NetworkError(val message: String?) : ConnectionCheckResult
    data class ApiError(val message: String?) : ConnectionCheckResult
}

/** Verifies that the stored KSeF token authenticates in the context of the company NIP. */
class KsefConnectionChecker(private val authenticator: KsefAuthenticator) {

    suspend fun check(nip: String?, ksefToken: String?): ConnectionCheckResult {
        if (ksefToken == null) return ConnectionCheckResult.MissingToken
        if (nip == null) return ConnectionCheckResult.MissingCompany
        return try {
            authenticator.authenticate(nip, ksefToken)
            ConnectionCheckResult.Success
        } catch (e: KsefAuthenticationException) {
            ConnectionCheckResult.AuthenticationFailed(e.statusCode, e.description)
        } catch (e: KsefRateLimitException) {
            ConnectionCheckResult.RateLimited(e.retryAfter?.inWholeSeconds)
        } catch (e: KsefException) {
            ConnectionCheckResult.ApiError(e.message)
        } catch (e: IOException) {
            ConnectionCheckResult.NetworkError(e.message)
        }
    }
}
