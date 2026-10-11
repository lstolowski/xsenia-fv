package pl.xsenia.fv.ksef.auth

import kotlinx.serialization.Serializable
import pl.xsenia.fv.ksef.http.InstantSerializer
import java.time.Instant

/** JWT issued by KSeF (access, refresh or authentication operation token). */
class AuthToken(val value: String, val validUntil: Instant) {

    fun isValidAt(instant: Instant): Boolean = instant.isBefore(validUntil)

    // Tokens are secrets — never print them.
    override fun toString(): String = "AuthToken(validUntil=$validUntil)"
}

/** Token pair obtained after a successful authentication. */
class KsefAuthTokens(val accessToken: AuthToken, val refreshToken: AuthToken) {
    override fun toString(): String = "KsefAuthTokens(accessToken=$accessToken, refreshToken=$refreshToken)"
}

// ---- API DTOs (KSeF API 2.0, schemas from open-api.json) ----

@Serializable
internal data class AuthChallengeResponse(
    val challenge: String,
    @Serializable(with = InstantSerializer::class) val timestamp: Instant,
    val timestampMs: Long,
)

@Serializable
internal data class ContextIdentifier(val type: String, val value: String)

@Serializable
internal data class KsefTokenAuthRequest(
    val challenge: String,
    val contextIdentifier: ContextIdentifier,
    val encryptedToken: String,
    val publicKeyId: String? = null,
)

@Serializable
internal data class TokenInfo(
    val token: String,
    @Serializable(with = InstantSerializer::class) val validUntil: Instant,
) {
    fun toAuthToken() = AuthToken(token, validUntil)
}

@Serializable
internal data class AuthInitResponse(val referenceNumber: String, val authenticationToken: TokenInfo)

@Serializable
internal data class StatusInfo(val code: Int, val description: String, val details: List<String>? = null)

@Serializable
internal data class AuthStatusResponse(val status: StatusInfo)

@Serializable
internal data class AuthTokensResponse(val accessToken: TokenInfo, val refreshToken: TokenInfo)

@Serializable
internal data class TokenRefreshResponse(val accessToken: TokenInfo)

@Serializable
internal data class PublicKeyCertificateDto(
    val certificate: String,
    val publicKeyId: String? = null,
    @Serializable(with = InstantSerializer::class) val validFrom: Instant,
    @Serializable(with = InstantSerializer::class) val validTo: Instant,
    val usage: List<String>,
)
