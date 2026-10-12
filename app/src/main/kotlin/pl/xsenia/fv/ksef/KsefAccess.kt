package pl.xsenia.fv.ksef

import io.ktor.client.HttpClient
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.invoices.KsefInvoiceClient
import pl.xsenia.fv.ksef.sending.KsefInvoiceSender
import pl.xsenia.fv.security.KsefTokenStore
import java.io.IOException

/**
 * Shared access to KSeF: one HTTP client, MF public keys and one token manager per company NIP,
 * so synchronization and sending reuse the same KSeF session tokens.
 */
class KsefAccess(
    private val httpClient: HttpClient,
    private val publicKeys: KsefPublicKeyProvider,
    private val authenticator: KsefAuthenticator,
    private val tokenStore: KsefTokenStore,
) {

    private val tokenManagers = mutableMapOf<String, KsefTokenManager>()

    @Synchronized
    fun tokenManager(nip: String): KsefTokenManager = tokenManagers.getOrPut(nip) {
        KsefTokenManager(authenticator, nip, ksefToken = { tokenStore.read() ?: throw KsefFailureException(KsefFailure.MissingToken) })
    }

    fun invoiceClient(nip: String) = KsefInvoiceClient(httpClient, tokenManager(nip), publicKeys)

    fun invoiceSender(nip: String) = KsefInvoiceSender(httpClient, tokenManager(nip), publicKeys)
}

/** Problems talking to KSeF, shown to the user in Polish. */
sealed interface KsefFailure {
    data object MissingToken : KsefFailure
    data object MissingCompany : KsefFailure
    data class RateLimited(val retryAfterSeconds: Long?) : KsefFailure
    data class AuthenticationFailed(val statusCode: Int, val description: String) : KsefFailure
    data class Network(val message: String?) : KsefFailure
    data class Ksef(val message: String?) : KsefFailure
}

/** Carries a [KsefFailure] that is known before calling KSeF (missing token / company). */
class KsefFailureException(val failure: KsefFailure) : Exception()

/** Maps errors of KSeF calls to [KsefFailure]; returns null for unrelated exceptions. */
fun Throwable.toKsefFailure(): KsefFailure? = when (this) {
    is KsefFailureException -> failure
    is KsefRateLimitException -> KsefFailure.RateLimited(retryAfter?.inWholeSeconds)
    is KsefAuthenticationException -> KsefFailure.AuthenticationFailed(statusCode, description)
    is KsefException -> KsefFailure.Ksef(message)
    is IOException -> KsefFailure.Network(message)
    else -> null
}
