package pl.xsenia.fv.ksef.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pl.xsenia.fv.ksef.KsefEnvironment

internal val KsefJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Creates HTTP clients preconfigured for a KSeF environment. Request paths are relative, e.g. `auth/challenge`. */
object KsefHttpClientFactory {

    fun create(environment: KsefEnvironment, engine: HttpClientEngine = OkHttp.create()): HttpClient =
        HttpClient(engine) {
            // Errors are mapped to KsefException by the API classes.
            expectSuccess = false
            install(ContentNegotiation) { json(KsefJson) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
            }
            defaultRequest { url(environment.apiBaseUrl.trimEnd('/') + "/") }
        }
}
