package pl.xsenia.fv.exchangerates

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.xsenia.fv.exchangerates.ExchangeRateResult.Failure.Reason
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

// runBlocking: MockEngine answers on its own dispatcher.
class NbpClientTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        NbpClient(NbpClient.createHttpClient(MockEngine { request -> requests += request; handler(request) }))

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8"))

    private fun MockRequestHandleScope.notFound() =
        respond(resource("nbp/not-found.txt"), HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8"))

    private fun found(currency: InvoiceCurrency, rate: String, table: String, date: String) =
        ExchangeRateResult.Found(ExchangeRate(currency, BigDecimal(rate), table, LocalDate.parse(date)))

    @Test
    fun `reads the mid rate of the day from table A`() = runBlocking {
        val result = client { json(resource("nbp/eur-2026-10-12.json")) }.rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-12"))

        assertEquals(found(InvoiceCurrency.EUR, "4.3889", "198/A/NBP/2026", "2026-10-12"), result)
        val url = requests.single().url
        assertEquals("api.nbp.pl", url.host)
        assertEquals("/api/exchangerates/rates/a/eur/2026-10-12/", url.encodedPath)
        assertEquals("json", url.parameters["format"])
    }

    @ParameterizedTest
    @CsvSource("USD, usd, 3.9022", "GBP, gbp, 5.1655")
    fun `reads USD and GBP`(currency: InvoiceCurrency, code: String, rate: String) = runBlocking {
        val result = client { json(resource("nbp/$code-2026-10-09.json")) }.rateOn(currency, LocalDate.parse("2026-10-09"))

        assertEquals(found(currency, rate, "197/A/NBP/2026", "2026-10-09"), result)
        assertEquals("/api/exchangerates/rates/a/$code/2026-10-09/", requests.single().url.encodedPath)
    }

    @Test
    fun `a day without a table is not published`() = runBlocking {
        assertEquals(ExchangeRateResult.NotPublished, client { notFound() }.rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-11")))
    }

    @Test
    fun `rate always has four decimals`() = runBlocking {
        val body = """{"table":"A","currency":"euro","code":"EUR","rates":[{"no":"197/A/NBP/2026","effectiveDate":"2026-10-09","mid":4.38}]}"""
        val result = client { json(body) }.rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-09"))

        assertEquals("4.3800", (result as ExchangeRateResult.Found).rate.rate.toPlainString())
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "not JSON | <html>error</html>",
            "other currency | {\"table\":\"A\",\"code\":\"USD\",\"rates\":[{\"no\":\"197/A/NBP/2026\",\"effectiveDate\":\"2026-10-09\",\"mid\":3.9022}]}",
            "other table | {\"table\":\"B\",\"code\":\"EUR\",\"rates\":[{\"no\":\"197/A/NBP/2026\",\"effectiveDate\":\"2026-10-09\",\"mid\":4.3775}]}",
            "other day | {\"table\":\"A\",\"code\":\"EUR\",\"rates\":[{\"no\":\"196/A/NBP/2026\",\"effectiveDate\":\"2026-10-08\",\"mid\":4.3789}]}",
            "no rates | {\"table\":\"A\",\"code\":\"EUR\",\"rates\":[]}",
            "mid not a number | {\"table\":\"A\",\"code\":\"EUR\",\"rates\":[{\"no\":\"197/A/NBP/2026\",\"effectiveDate\":\"2026-10-09\",\"mid\":null}]}",
            "mid not positive | {\"table\":\"A\",\"code\":\"EUR\",\"rates\":[{\"no\":\"197/A/NBP/2026\",\"effectiveDate\":\"2026-10-09\",\"mid\":0}]}",
            "bad date | {\"table\":\"A\",\"code\":\"EUR\",\"rates\":[{\"no\":\"197/A/NBP/2026\",\"effectiveDate\":\"09.10.2026\",\"mid\":4.3775}]}",
        ],
    )
    fun `unexpected response is a server failure`(@Suppress("UNUSED_PARAMETER") case: String, body: String) = runBlocking {
        val result = client { json(body) }.rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-09"))

        assertEquals(ExchangeRateResult.Failure(Reason.SERVER), result)
    }

    @ParameterizedTest
    @CsvSource("400", "500", "503")
    fun `error status is a server failure`(status: Int) = runBlocking {
        val result = client { respond("400 BadRequest - Błędny zakres dat", HttpStatusCode.fromValue(status)) }
            .rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-09"))

        assertEquals(ExchangeRateResult.Failure(Reason.SERVER), result)
    }

    @Test
    fun `no connection is a network failure`() = runBlocking {
        val result = client { throw IOException("Unable to resolve host") }.rateOn(InvoiceCurrency.EUR, LocalDate.parse("2026-10-09"))

        assertEquals(ExchangeRateResult.Failure(Reason.NETWORK), result)
    }

    @Test
    fun `PLN is never asked for`() {
        assertThrows<IllegalArgumentException> { runBlocking { client { json("{}") }.rateOn(InvoiceCurrency.PLN, LocalDate.parse("2026-10-09")) } }
        assertEquals(emptyList<HttpRequestData>(), requests)
    }
}

fun resource(name: String): String =
    requireNotNull(NbpClientTest::class.java.classLoader.getResource(name)) { "Missing test resource $name" }.readText()
