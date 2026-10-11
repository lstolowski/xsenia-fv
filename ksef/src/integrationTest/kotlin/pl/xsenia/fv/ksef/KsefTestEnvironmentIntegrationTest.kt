package pl.xsenia.fv.ksef

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.support.KsefTestConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Smoke test of the integration test setup: secrets are present and KSeF TEST is reachable. */
class KsefTestEnvironmentIntegrationTest {

    private val config = KsefTestConfig.loadOrSkip()

    @Test
    fun `KSeF token belongs to the configured NIP`() {
        assertTrue(config.ksefToken.contains("|nip-${config.nip}|"), "token does not match ksef.nip")
    }

    @Test
    fun `KSeF TEST issues an authentication challenge`() {
        val request = HttpRequest.newBuilder(URI.create("${config.environment.apiBaseUrl}/auth/challenge"))
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode(), response.body())
        assertTrue(response.body().contains("\"challenge\""), response.body())
    }
}
