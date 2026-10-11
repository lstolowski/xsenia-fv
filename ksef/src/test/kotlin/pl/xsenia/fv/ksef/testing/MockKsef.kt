package pl.xsenia.fv.ksef.testing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import java.security.KeyPair
import java.security.KeyPairGenerator

/** KSeF HTTP client backed by a [MockEngine]; [requests] records every call. */
class MockKsef(private val handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
    val requests = mutableListOf<HttpRequestData>()

    val client: HttpClient = KsefHttpClientFactory.create(
        KsefEnvironment.TEST,
        MockEngine { request ->
            requests += request
            handler(request)
        },
    )

    fun requestsTo(path: String) = requests.filter { it.url.encodedPath == "/v2/$path" }
}

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK, extraHeaders: Map<String, String> = emptyMap()) =
    respond(
        body,
        status,
        headersOf(*(mapOf(HttpHeaders.ContentType to "application/json") + extraHeaders).map { it.key to listOf(it.value) }.toTypedArray()),
    )

fun rsaKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

fun resourceText(name: String): String =
    requireNotNull(MockKsef::class.java.classLoader.getResource(name)) { "Missing test resource $name" }.readText()
