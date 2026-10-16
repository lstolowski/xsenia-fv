package pl.xsenia.fv.ksef.auth

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.testing.MockKsef
import pl.xsenia.fv.ksef.testing.json
import pl.xsenia.fv.ksef.testing.resourceText
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ApiPublicKeyProviderTest {

    private val ksef = MockKsef { json(resourceText("public-key-certificates-test.json")) }

    private fun provider(now: String) = ApiPublicKeyProvider(KsefAuthApi(ksef.client), Clock.fixed(Instant.parse(now), ZoneOffset.UTC))

    @Test
    fun `picks certificate by usage and fetches certificates only once`() = runTest {
        val provider = provider("2026-10-11T12:00:00Z")

        val tokenKey = provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)
        val symmetricKey = provider.publicKey(PublicKeyUsage.SYMMETRIC_KEY_ENCRYPTION)

        assertEquals("IPbPM4CB49vtoR/x/3fEI+Y+Q6lK/bVVehQ7/NlPJoo=", tokenKey.id)
        assertEquals("tmtCidSRzR4fvNpLU5hMOM6FzamxJf0BBR8IkXIAwsY=", symmetricKey.id)
        assertEquals(1, ksef.requestsTo("security/public-key-certificates").size)
    }

    @Test
    fun `fails when no certificate is currently valid`() = runTest {
        assertThrows<KsefProtocolException> { provider("2028-01-01T00:00:00Z").publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION) }
    }

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): java.time.ZoneId = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
    }

    @Test
    fun `fetches the certificates again after the time to live`() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-11T12:00:00Z"))
        val provider = ApiPublicKeyProvider(KsefAuthApi(ksef.client), clock, ttl = Duration.ofHours(24))

        provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)
        clock.now = Instant.parse("2026-10-12T11:00:00Z")
        provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)
        assertEquals(1, ksef.requestsTo("security/public-key-certificates").size)

        clock.now = Instant.parse("2026-10-12T12:00:00Z")
        provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)
        assertEquals(2, ksef.requestsTo("security/public-key-certificates").size)
    }

    @Test
    fun `fetches the certificates again when the cached ones are no longer valid`() = runTest {
        // The certificates are valid until 2027-09-29; the cached list has no valid one at the later time.
        val clock = MutableClock(Instant.parse("2027-09-29T00:00:00Z"))
        val provider = ApiPublicKeyProvider(KsefAuthApi(ksef.client), clock, ttl = Duration.ofDays(365))
        provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)

        clock.now = Instant.parse("2027-09-30T00:00:00Z")

        // Expired for good: asked again (the new list has none either) before giving up.
        assertThrows<KsefProtocolException> { provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION) }
        assertEquals(2, ksef.requestsTo("security/public-key-certificates").size)
    }

    @Test
    fun `keeps using the cached certificates when the refresh fails`() = runTest {
        var fail = false
        val flaky = MockKsef { if (fail) json("", io.ktor.http.HttpStatusCode.ServiceUnavailable) else json(resourceText("public-key-certificates-test.json")) }
        val clock = MutableClock(Instant.parse("2026-10-11T12:00:00Z"))
        val provider = ApiPublicKeyProvider(KsefAuthApi(flaky.client), clock, ttl = Duration.ofHours(1))
        val first = provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION)

        clock.now = Instant.parse("2026-10-11T14:00:00Z")
        fail = true

        assertEquals(first.id, provider.publicKey(PublicKeyUsage.KSEF_TOKEN_ENCRYPTION).id)
    }
}
