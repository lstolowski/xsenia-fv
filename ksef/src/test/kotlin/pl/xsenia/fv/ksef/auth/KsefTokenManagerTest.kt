package pl.xsenia.fv.ksef.auth

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.testing.MockKsef
import pl.xsenia.fv.ksef.testing.json
import pl.xsenia.fv.ksef.testing.rsaKeyPair
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds

class KsefTokenManagerTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = this
    }

    private val keyPair = rsaKeyPair()
    private val clock = MutableClock(Instant.parse("2026-10-11T15:06:00Z"))
    private var refreshStatus = HttpStatusCode.OK
    private var ksefTokenReads = 0

    private val ksef = MockKsef { request ->
        when (request.url.encodedPath) {
            "/v2/auth/challenge" -> json("""{"challenge":"c","timestamp":"2026-10-11T15:05:54+00:00","timestampMs":1,"clientIp":"1.2.3.4"}""")
            "/v2/auth/ksef-token" -> json("""{"referenceNumber":"AU-1","authenticationToken":{"token":"auth","validUntil":"2026-10-11T15:20:00+00:00"}}""")
            "/v2/auth/AU-1" -> json("""{"status":{"code":200,"description":"ok"}}""")
            "/v2/auth/token/redeem" -> json(
                """{"accessToken":{"token":"access-1","validUntil":"2026-10-11T15:20:00+00:00"},
                    "refreshToken":{"token":"refresh-1","validUntil":"2026-10-18T15:05:00+00:00"}}""",
            )
            "/v2/auth/token/refresh" ->
                if (refreshStatus == HttpStatusCode.OK) {
                    json("""{"accessToken":{"token":"access-refreshed","validUntil":"2026-10-11T15:40:00+00:00"}}""")
                } else {
                    json("", refreshStatus)
                }
            else -> error("Unexpected request ${request.url}")
        }
    }

    private val manager = KsefTokenManager(
        authenticator = KsefAuthenticator(
            KsefAuthApi(ksef.client),
            { KsefPublicKey(null, keyPair.public) },
            pollInterval = 1.milliseconds,
        ),
        nip = "1111111111",
        ksefToken = { ksefTokenReads++; "ksef-token" },
        clock = clock,
    )

    @Test
    fun `authenticates once and reuses a valid access token`(): Unit = runBlocking {
        assertEquals("access-1", manager.accessToken())
        clock.now = Instant.parse("2026-10-11T15:15:00Z")
        assertEquals("access-1", manager.accessToken())

        assertEquals(1, ksef.requestsTo("auth/ksef-token").size)
        assertEquals(1, ksefTokenReads)
    }

    @Test
    fun `refreshes access token shortly before it expires`(): Unit = runBlocking {
        manager.accessToken()
        clock.now = Instant.parse("2026-10-11T15:19:30Z")

        assertEquals("access-refreshed", manager.accessToken())
        assertEquals(1, ksef.requestsTo("auth/ksef-token").size)
        assertEquals(1, ksef.requestsTo("auth/token/refresh").size)
    }

    @Test
    fun `authenticates again when refresh is rejected`(): Unit = runBlocking {
        manager.accessToken()
        clock.now = Instant.parse("2026-10-11T15:25:00Z")
        refreshStatus = HttpStatusCode.Unauthorized

        assertEquals("access-1", manager.accessToken())
        assertEquals(2, ksef.requestsTo("auth/ksef-token").size)
    }

    @Test
    fun `authenticates again when refresh token expired`(): Unit = runBlocking {
        manager.accessToken()
        clock.now = Instant.parse("2026-10-19T00:00:00Z")

        manager.accessToken()

        assertEquals(0, ksef.requestsTo("auth/token/refresh").size)
        assertEquals(2, ksef.requestsTo("auth/ksef-token").size)
    }

    @Test
    fun `invalidate forces a new authentication`(): Unit = runBlocking {
        manager.accessToken()
        manager.invalidate()
        manager.accessToken()

        assertEquals(2, ksef.requestsTo("auth/ksef-token").size)
    }
}
