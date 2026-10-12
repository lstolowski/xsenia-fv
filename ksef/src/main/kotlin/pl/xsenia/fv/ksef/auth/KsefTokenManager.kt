package pl.xsenia.fv.ksef.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.xsenia.fv.ksef.KsefApiException
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Provides a valid KSeF access token, authenticating or refreshing only when needed.
 *
 * The KSeF token is requested from [ksefToken] only when a full authentication is required,
 * so the caller can keep it encrypted (e.g. in Android Keystore) the rest of the time.
 */
class KsefTokenManager(
    private val authenticator: KsefAuthenticator,
    private val nip: String,
    private val ksefToken: suspend () -> String,
    private val clock: Clock = Clock.systemUTC(),
    private val expiryMargin: Duration = 60.seconds,
) {

    private val mutex = Mutex()
    private var tokens: KsefAuthTokens? = null

    /** Returns an access token valid for at least [expiryMargin]. */
    suspend fun accessToken(): String = mutex.withLock {
        val threshold = clock.instant().plus(expiryMargin.toJavaDuration())
        val current = tokens

        val valid = when {
            current != null && current.accessToken.isValidAt(threshold) -> current
            current != null && current.refreshToken.isValidAt(threshold) -> refreshOrAuthenticate(current)
            else -> authenticate()
        }
        tokens = valid
        valid.accessToken.value
    }

    /** Forgets the current tokens, e.g. after KSeF rejected the access token with HTTP 401. */
    suspend fun invalidate() = mutex.withLock { tokens = null }

    /** Runs [block] with a valid access token; after HTTP 401 re-authenticates once and retries. */
    suspend fun <T> withAccessToken(block: suspend (String) -> T): T =
        try {
            block(accessToken())
        } catch (e: KsefApiException) {
            if (e.httpStatus != HTTP_UNAUTHORIZED) throw e
            invalidate()
            block(accessToken())
        }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }

    private suspend fun refreshOrAuthenticate(current: KsefAuthTokens): KsefAuthTokens =
        try {
            KsefAuthTokens(authenticator.refresh(current.refreshToken), current.refreshToken)
        } catch (e: KsefApiException) {
            // Refresh token revoked or rejected — start a new authentication.
            authenticate()
        }

    private suspend fun authenticate(): KsefAuthTokens = authenticator.authenticate(nip, ksefToken())
}
