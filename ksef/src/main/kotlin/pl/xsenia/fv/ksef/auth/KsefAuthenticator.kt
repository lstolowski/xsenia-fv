package pl.xsenia.fv.ksef.auth

import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import pl.xsenia.fv.ksef.KsefAuthenticationException
import pl.xsenia.fv.ksef.KsefTimeoutException
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Authenticates in KSeF with a KSeF token (`POST /auth/ksef-token`) and refreshes access tokens.
 *
 * Flow: challenge → encrypt `token|timestampMs` → submit → poll status until done → redeem token pair.
 */
class KsefAuthenticator internal constructor(
    private val api: KsefAuthApi,
    private val publicKeys: KsefPublicKeyProvider,
    private val pollInterval: Duration = 1.seconds,
    private val timeout: Duration = 2.minutes,
) {

    constructor(client: HttpClient, publicKeys: KsefPublicKeyProvider = KsefPublicKeyProvider.fromApi(client)) :
        this(KsefAuthApi(client), publicKeys)

    suspend fun authenticate(nip: String, ksefToken: String): KsefAuthTokens {
        require(NIP_PATTERN.matches(nip)) { "NIP must have 10 digits" }

        val challenge = api.challenge()
        val publicKey = publicKeys.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)
        val encryptedToken = KsefCrypto.encryptKsefToken(ksefToken, challenge.timestampMs, publicKey.key)

        val operation = api.authenticateWithKsefToken(
            KsefTokenAuthRequest(
                challenge = challenge.challenge,
                contextIdentifier = ContextIdentifier(type = "Nip", value = nip),
                encryptedToken = Base64.getEncoder().encodeToString(encryptedToken),
                publicKeyId = publicKey.id,
            ),
        )
        val authenticationToken = operation.authenticationToken.token

        awaitSuccess(operation.referenceNumber, authenticationToken)

        val tokens = api.redeemTokens(authenticationToken)
        return KsefAuthTokens(tokens.accessToken.toAuthToken(), tokens.refreshToken.toAuthToken())
    }

    /** Obtains a new access token using a refresh token (valid up to 7 days). */
    suspend fun refresh(refreshToken: AuthToken): AuthToken =
        api.refreshAccessToken(refreshToken.value).accessToken.toAuthToken()

    private suspend fun awaitSuccess(referenceNumber: String, authenticationToken: String) {
        val finished = withTimeoutOrNull(timeout) {
            while (true) {
                val status = api.authStatus(referenceNumber, authenticationToken).status
                when (status.code) {
                    STATUS_SUCCESS -> break
                    STATUS_IN_PROGRESS -> delay(pollInterval)
                    else -> throw KsefAuthenticationException(status.code, status.description, status.details.orEmpty())
                }
            }
            true
        }
        if (finished == null) {
            throw KsefTimeoutException("KSeF authentication $referenceNumber did not finish within $timeout")
        }
    }

    private companion object {
        const val STATUS_IN_PROGRESS = 100
        const val STATUS_SUCCESS = 200
        val NIP_PATTERN = Regex("\\d{10}")
    }
}
