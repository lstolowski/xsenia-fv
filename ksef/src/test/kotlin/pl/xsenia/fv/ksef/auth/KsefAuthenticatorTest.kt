package pl.xsenia.fv.ksef.auth

import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.ksef.KsefAuthenticationException
import pl.xsenia.fv.ksef.KsefTimeoutException
import pl.xsenia.fv.ksef.testing.MockKsef
import pl.xsenia.fv.ksef.testing.json
import pl.xsenia.fv.ksef.testing.rsaKeyPair
import java.security.spec.MGF1ParameterSpec
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.time.Duration.Companion.milliseconds

// Uses runBlocking (real time): MockEngine responds on its own dispatcher, so runTest's virtual time
// would fire the polling timeout before the first response arrives.
class KsefAuthenticatorTest {

    private val keyPair = rsaKeyPair()
    private val publicKeys = KsefPublicKeyProvider { KsefPublicKey("key-1", keyPair.public) }

    private fun authenticator(ksef: MockKsef) = KsefAuthenticator(KsefAuthApi(ksef.client), publicKeys, pollInterval = 1.milliseconds)

    private fun ksefWithStatuses(vararg statusCodes: Int): MockKsef {
        val statuses = ArrayDeque(statusCodes.toList())
        return MockKsef { request ->
            when (request.url.encodedPath) {
                "/v2/auth/challenge" -> json(
                    """{"challenge":"20261011-CR-1","timestamp":"2026-10-11T15:05:54.7178209+00:00","timestampMs":1791731154717,"clientIp":"1.2.3.4"}""",
                )
                "/v2/auth/ksef-token" -> json(
                    """{"referenceNumber":"20261011-AU-1","authenticationToken":{"token":"auth-jwt","validUntil":"2026-10-11T15:20:00+00:00"}}""",
                    io.ktor.http.HttpStatusCode.Accepted,
                )
                "/v2/auth/20261011-AU-1" -> {
                    val code = statuses.removeFirstOrNull() ?: 100
                    json("""{"startDate":"2026-10-11T15:05:55+00:00","authenticationMethod":"Token","status":{"code":$code,"description":"status $code","details":["detail $code"]}}""")
                }
                "/v2/auth/token/redeem" -> json(
                    """{"accessToken":{"token":"access-jwt","validUntil":"2026-10-11T15:20:00+00:00"},
                        "refreshToken":{"token":"refresh-jwt","validUntil":"2026-10-18T15:05:00+00:00"}}""",
                )
                "/v2/auth/token/refresh" -> json("""{"accessToken":{"token":"access-jwt-2","validUntil":"2026-10-11T15:40:00+00:00"}}""")
                else -> error("Unexpected request ${request.url}")
            }
        }
    }

    private fun HttpRequestData.bodyJson() = Json.parseToJsonElement(
        String((body as OutgoingContent.ByteArrayContent).bytes(), Charsets.UTF_8),
    ).jsonObject

    private fun decrypt(base64: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyPair.private,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
        )
        return String(cipher.doFinal(Base64.getDecoder().decode(base64)), Charsets.UTF_8)
    }

    @Test
    fun `authenticates with KSeF token and redeems token pair`(): Unit = runBlocking {
        val ksef = ksefWithStatuses(100, 100, 200)

        val tokens = authenticator(ksef).authenticate("1111111111", "ksef-token")

        assertEquals("access-jwt", tokens.accessToken.value)
        assertEquals(Instant.parse("2026-10-18T15:05:00Z"), tokens.refreshToken.validUntil)

        val authRequest = ksef.requestsTo("auth/ksef-token").single().bodyJson()
        assertEquals("20261011-CR-1", authRequest["challenge"]!!.jsonPrimitive.content)
        assertEquals("1111111111", authRequest["contextIdentifier"]!!.jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("Nip", authRequest["contextIdentifier"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("key-1", authRequest["publicKeyId"]!!.jsonPrimitive.content)
        assertEquals("ksef-token|1791731154717", decrypt(authRequest["encryptedToken"]!!.jsonPrimitive.content))

        assertEquals(3, ksef.requestsTo("auth/20261011-AU-1").size)
        assertEquals("Bearer auth-jwt", ksef.requestsTo("auth/token/redeem").single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `reports failed authentication status`(): Unit = runBlocking {
        val ksef = ksefWithStatuses(450)

        val e = assertThrows<KsefAuthenticationException> {
            authenticator(ksef).authenticate("1111111111", "bad-token")
        }

        assertEquals(450, e.statusCode)
        assertEquals(listOf("detail 450"), e.details)
        assertEquals(0, ksef.requestsTo("auth/token/redeem").size)
    }

    @Test
    fun `times out when authentication stays in progress`(): Unit = runBlocking {
        val ksef = ksefWithStatuses()

        assertThrows<KsefTimeoutException> {
            KsefAuthenticator(KsefAuthApi(ksef.client), publicKeys, pollInterval = 10.milliseconds, timeout = 200.milliseconds)
                .authenticate("1111111111", "ksef-token")
        }
    }

    @Test
    fun `rejects invalid NIP before calling KSeF`(): Unit = runBlocking {
        val ksef = ksefWithStatuses(200)

        assertThrows<IllegalArgumentException> {
            authenticator(ksef).authenticate("123", "ksef-token")
        }
        assertEquals(0, ksef.requests.size)
    }

    @Test
    fun `refreshes access token with refresh token as bearer`(): Unit = runBlocking {
        val ksef = ksefWithStatuses()

        val accessToken = authenticator(ksef)
            .refresh(AuthToken("refresh-jwt", Instant.parse("2026-10-18T15:05:00Z")))

        assertEquals("access-jwt-2", accessToken.value)
        assertEquals("Bearer refresh-jwt", ksef.requestsTo("auth/token/refresh").single().headers[HttpHeaders.Authorization])
    }
}
