package pl.xsenia.fv.whitelist

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalDate

/**
 * Client of the MF White List API (wykaz podatników VAT, ADR 0007):
 * `GET https://wl-api.mf.gov.pl/api/search/nip/{nip}?date={yyyy-MM-dd}`.
 *
 * Public, no authentication, about 100 queries a day — call only on the user's request. Response bodies contain data
 * of real companies and are never logged.
 */
class WhiteListClient(private val httpClient: HttpClient) {

    /**
     * Looks up the taxpayer with [nip] (10 digits, no separators) as of [date] (not in the future).
     * Never throws for HTTP or network problems — they are reported as [WhiteListResult.Failed].
     */
    suspend fun findByNip(nip: String, date: LocalDate): WhiteListResult {
        require(nip.length == 10 && nip.all { it in '0'..'9' }) { "NIP must be 10 digits" }
        val response = try {
            httpClient.get("api/search/nip/$nip") { parameter("date", date.toString()) }
        } catch (e: IOException) {
            // Includes connect and request timeouts.
            return WhiteListResult.Failed(WhiteListFailure.Network(e.message))
        }
        val status = response.status
        val text = try {
            response.bodyAsText()
        } catch (e: IOException) {
            return WhiteListResult.Failed(WhiteListFailure.Network(e.message))
        }

        if (status == HttpStatusCode.TooManyRequests) return WhiteListResult.Failed(WhiteListFailure.RateLimited)
        if (!status.isSuccess()) {
            val error = decodeOrNull<ErrorResponse>(text)
            val failure = error?.code?.let { WhiteListFailure.Api(status.value, it, error.message) }
                ?: WhiteListFailure.Server(status.value)
            return WhiteListResult.Failed(failure)
        }

        val result = decodeOrNull<EntityResponse>(text)?.result
            ?: return WhiteListResult.Failed(WhiteListFailure.Server(status.value))
        return result.subject?.let { WhiteListResult.Found(it.toSubject(nip)) } ?: WhiteListResult.NotFound
    }

    private inline fun <reified T> decodeOrNull(text: String): T? =
        try {
            json.decodeFromString<T>(text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    companion object {
        const val BASE_URL = "https://wl-api.mf.gov.pl/"

        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun create(engine: HttpClientEngine = OkHttp.create()): WhiteListClient =
            WhiteListClient(
                HttpClient(engine) {
                    // Error statuses are mapped to WhiteListFailure above.
                    expectSuccess = false
                    install(HttpTimeout) {
                        connectTimeoutMillis = 15_000
                        requestTimeoutMillis = 15_000
                    }
                    defaultRequest { url(BASE_URL) }
                },
            )
    }
}
