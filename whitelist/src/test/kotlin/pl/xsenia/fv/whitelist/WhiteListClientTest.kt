package pl.xsenia.fv.whitelist

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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.time.LocalDate

// Responses are fictitious, modelled on real ones (docs/przyklady/biala-lista.md).
class WhiteListClientTest {

    private val date = LocalDate.of(2026, 10, 13)
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        WhiteListClient.create(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        )

    private fun clientReturning(resource: String, status: HttpStatusCode = HttpStatusCode.OK) =
        client { respondJson(fixture(resource), status) }

    private fun lookup(client: WhiteListClient, nip: String) = runBlocking { client.findByNip(nip, date) }

    private fun found(result: WhiteListResult): WhiteListSubject {
        assertTrue(result is WhiteListResult.Found, "expected Found, got $result")
        return (result as WhiteListResult.Found).subject
    }

    @Test
    fun `calls search by NIP with the date`() {
        lookup(clientReturning("not-found.json"), "6691034274")

        val url = requests.single().url
        assertEquals("wl-api.mf.gov.pl", url.host)
        assertEquals("/api/search/nip/6691034274", url.encodedPath)
        assertEquals("2026-10-13", url.parameters["date"])
    }

    @Test
    fun `sole proprietor with residence address and two accounts`() {
        val subject = found(lookup(clientReturning("sole-proprietor-active.json"), "1996845518"))

        assertEquals("JAN TESTOWY", subject.name)
        assertEquals("1996845518", subject.nip)
        assertTrue(subject.isActiveVatPayer)
        assertEquals(WhiteListAddress("TESTOWA 1", "00-001 TESTOWO"), subject.address)
        assertEquals(listOf("85216097583216850246002152", "55109004081797992973253811"), subject.accountNumbers)
    }

    @Test
    fun `company with place of business keeps registry spelling`() {
        val subject = found(lookup(clientReturning("company-active.json"), "6691034274"))

        assertEquals("TESTOWA SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ", subject.name)
        assertEquals(WhiteListAddress("TESTOWA 6", "00-002 TESTOWO"), subject.address)
        assertEquals("0000000000", subject.krs)
        assertEquals(listOf("22161029092633573189646018"), subject.accountNumbers)
    }

    @Test
    fun `place of business wins over residence address`() {
        val subject = found(lookup(clientReturning("both-addresses.json"), "9579102904"))

        assertEquals(WhiteListAddress("TESTOWA 8", "00-004 TESTOWO"), subject.address)
        assertEquals(emptyList<String>(), subject.accountNumbers)
    }

    @Test
    fun `exempt taxpayer is not an active VAT payer`() {
        val subject = found(lookup(clientReturning("exempt.json"), "9945082500"))

        assertEquals("Zwolniony", subject.vatStatus)
        assertFalse(subject.isActiveVatPayer)
    }

    @Test
    fun `address without a comma goes to the first line`() {
        val subject = found(lookup(clientReturning("address-without-comma.json"), "6973432790"))

        assertEquals(WhiteListAddress("TESTOWO 15", null), subject.address)
    }

    @Test
    fun `unknown NIP is not found`() {
        assertEquals(WhiteListResult.NotFound, lookup(clientReturning("not-found.json"), "4824062973"))
    }

    @Test
    fun `API error code is reported`() {
        val result = lookup(clientReturning("error-wl103.json", HttpStatusCode.BadRequest), "6691034274")

        val failure = (result as WhiteListResult.Failed).failure as WhiteListFailure.Api
        assertEquals(400, failure.httpStatus)
        assertEquals("WL-103", failure.code)
        assertEquals("Data nie może być datą przyszłą.", failure.message)
    }

    @Test
    fun `WL-113 error is reported`() {
        val result = lookup(clientReturning("error-wl113.json", HttpStatusCode.BadRequest), "6691034274")

        assertEquals("WL-113", ((result as WhiteListResult.Failed).failure as WhiteListFailure.Api).code)
    }

    @Test
    fun `too many requests is a rate limit`() {
        val result = lookup(client { respondJson("", HttpStatusCode.TooManyRequests) }, "6691034274")

        assertEquals(WhiteListResult.Failed(WhiteListFailure.RateLimited), result)
    }

    @Test
    fun `server error without JSON`() {
        val result = lookup(client { respond("<html>Service Unavailable</html>", HttpStatusCode.ServiceUnavailable) }, "6691034274")

        assertEquals(WhiteListResult.Failed(WhiteListFailure.Server(503)), result)
    }

    @Test
    fun `unexpected success body is a server failure`() {
        val result = lookup(client { respondJson("""{"unexpected": true}""") }, "6691034274")

        assertEquals(WhiteListResult.Failed(WhiteListFailure.Server(200)), result)
    }

    @Test
    fun `network problem is reported`() {
        val result = lookup(client { throw IOException("no route to host") }, "6691034274")

        assertEquals(WhiteListResult.Failed(WhiteListFailure.Network("no route to host")), result)
    }

    @Test
    fun `malformed NIP is rejected before calling the API`() {
        val client = clientReturning("not-found.json")

        assertThrows<IllegalArgumentException> { lookup(client, "669-103-42-74") }
        assertThrows<IllegalArgumentException> { lookup(client, "12345") }
        assertTrue(requests.isEmpty())
    }

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json;charset=UTF-8"))

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/whitelist/$name")) { "Missing test resource $name" }.readText()
}
