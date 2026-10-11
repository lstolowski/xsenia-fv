package pl.xsenia.fv.ksef.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pl.xsenia.fv.ksef.http.bodyOrThrow

/** Thin wrapper over the KSeF authentication endpoints. */
internal class KsefAuthApi(private val client: HttpClient) {

    suspend fun challenge(): AuthChallengeResponse =
        client.post("auth/challenge").bodyOrThrow()

    suspend fun publicKeyCertificates(): List<PublicKeyCertificateDto> =
        client.get("security/public-key-certificates").bodyOrThrow()

    suspend fun authenticateWithKsefToken(request: KsefTokenAuthRequest): AuthInitResponse =
        client.post("auth/ksef-token") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrThrow()

    suspend fun authStatus(referenceNumber: String, authenticationToken: String): AuthStatusResponse =
        client.get("auth/$referenceNumber") { bearerAuth(authenticationToken) }.bodyOrThrow()

    suspend fun redeemTokens(authenticationToken: String): AuthTokensResponse =
        client.post("auth/token/redeem") { bearerAuth(authenticationToken) }.bodyOrThrow()

    suspend fun refreshAccessToken(refreshToken: String): TokenRefreshResponse =
        client.post("auth/token/refresh") { bearerAuth(refreshToken) }.bodyOrThrow()
}
