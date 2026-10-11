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
}
