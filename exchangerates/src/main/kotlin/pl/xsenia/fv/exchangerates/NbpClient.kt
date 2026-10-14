package pl.xsenia.fv.exchangerates

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
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import pl.xsenia.fv.exchangerates.ExchangeRateResult.Failure.Reason
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Client of the NBP exchange rates API, table A (average rates):
 * `GET https://api.nbp.pl/api/exchangerates/rates/a/{code}/{date}/?format=json` — the `mid` field is the rate (ADR 0008).
 * A day without a table answers `404` with a plain-text body.
 */
class NbpClient(private val http: HttpClient) : ExchangeRateSource {

    override suspend fun rateOn(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult {
        require(currency.isForeign) { "PLN has no exchange rate" }
        val (status, body) = try {
            val response = http.get("api/exchangerates/rates/a/${currency.code.lowercase()}/$date/") { parameter("format", "json") }
            response.status to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Includes timeouts (HttpRequestTimeoutException, socket timeouts) and unknown hosts.
            return ExchangeRateResult.Failure(Reason.NETWORK)
        }
        return when {
            status == HttpStatusCode.NotFound -> ExchangeRateResult.NotPublished
            !status.isSuccess() -> ExchangeRateResult.Failure(Reason.SERVER)
            else -> parse(body, currency, date)
        }
    }

    private fun parse(body: String, currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult {
        val rate = try {
            val response = json.decodeFromString(NbpRatesResponse.serializer(), body)
            val entry = response.rates.singleOrNull()
            if (response.table != TABLE || response.code != currency.code || entry == null) return ExchangeRateResult.Failure(Reason.SERVER)
            val effectiveDate = LocalDate.parse(entry.effectiveDate)
            if (effectiveDate != date) return ExchangeRateResult.Failure(Reason.SERVER)
            // Table A has 4 decimals; a different scale would be normalized (never seen in practice).
            ExchangeRate(currency, BigDecimal(entry.mid.content).setScale(ExchangeRate.SCALE, RoundingMode.HALF_UP), entry.no, effectiveDate)
        } catch (e: IllegalArgumentException) {
            // SerializationException (not JSON / other structure), NumberFormatException (mid), ExchangeRate validation.
            return ExchangeRateResult.Failure(Reason.SERVER)
        } catch (e: DateTimeParseException) {
            return ExchangeRateResult.Failure(Reason.SERVER)
        }
        return ExchangeRateResult.Found(rate)
    }

    companion object {
        const val BASE_URL = "https://api.nbp.pl/"
        private const val TABLE = "A"
        private const val TIMEOUT_MILLIS = 15_000L

        private val json = Json { ignoreUnknownKeys = true }

        fun createHttpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient =
            HttpClient(engine) {
                expectSuccess = false
                install(HttpTimeout) {
                    connectTimeoutMillis = TIMEOUT_MILLIS
                    requestTimeoutMillis = TIMEOUT_MILLIS
                }
                defaultRequest { url(BASE_URL) }
            }
    }
}

@Serializable
private data class NbpRatesResponse(val table: String, val code: String, val rates: List<NbpRate>)

/** [mid] kept as the JSON literal so the rate is read as an exact decimal. */
@Serializable
private data class NbpRate(val no: String, val effectiveDate: String, val mid: JsonPrimitive)
