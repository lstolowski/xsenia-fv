package pl.xsenia.fv.ksef

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.ksef.support.KsefTestConfig
import java.time.Duration
import java.time.Instant

class KsefAuthenticationIntegrationTest {

    private val config = KsefTestConfig.loadOrSkip()
    private val client = KsefHttpClientFactory.create(config.environment)
    private val authenticator = KsefAuthenticator(client)

    @AfterEach
    fun closeClient() = client.close()

    @Test
    fun `authenticates with KSeF token and refreshes access token`(): Unit = runBlocking {
        val tokens = authenticator.authenticate(config.nip, config.ksefToken)

        val now = Instant.now()
        assertTrue(tokens.accessToken.isValidAt(now.plusSeconds(60)), "access token should be valid for a while")
        assertTrue(tokens.refreshToken.isValidAt(now.plus(Duration.ofDays(1))), "refresh token should be valid for days")

        val refreshed = authenticator.refresh(tokens.refreshToken)

        assertNotEquals(tokens.accessToken.value, refreshed.value)
        assertTrue(refreshed.isValidAt(now.plusSeconds(60)))
    }

    @Test
    fun `token manager authenticates once and reuses the access token`(): Unit = runBlocking {
        val manager = KsefTokenManager(authenticator, config.nip, ksefToken = { config.ksefToken })

        val first = manager.accessToken()
        val second = manager.accessToken()

        assertEquals(first, second)
    }
}
