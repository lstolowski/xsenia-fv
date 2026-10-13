package pl.xsenia.fv.lookup

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.whitelist.WhiteListClient
import pl.xsenia.fv.whitelist.WhiteListFailure
import java.time.LocalDate

// Fictitious registry data (docs/przyklady/biala-lista.md).
class CompanyLookupServiceTest {

    private val requested = mutableListOf<Url>()

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK) = CompanyLookupService(
        WhiteListClient.create(
            MockEngine { request ->
                requested += request.url
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
        today = { LocalDate.of(2026, 10, 13) },
    )

    private fun subject(statusVat: String, accounts: String) = """
        {"result": {"subject": {
            "name": "TESTOWA SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ", "nip": "6691034274", "statusVat": "$statusVat",
            "residenceAddress": null, "workingAddress": "TESTOWA 6, 00-002 TESTOWO",
            "accountNumbers": [$accounts], "hasVirtualAccounts": false
        }, "requestId": "Test2-000aaaa", "requestDateTime": "13-10-2026 15:00:00"}}
    """.trimIndent()

    @Test
    fun `found company fills the form fields`() {
        val result = runBlocking {
            service(subject("Czynny", "\"22161029092633573189646018\"")).lookup("669-103-42-74")
        }

        assertEquals(
            CompanyLookupResult.Found(
                LookedUpCompany(
                    nip = "6691034274",
                    name = "TESTOWA SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ",
                    addressLine1 = "TESTOWA 6",
                    addressLine2 = "00-002 TESTOWO",
                    isActiveVatPayer = true,
                    accountNumbers = listOf("22161029092633573189646018"),
                ),
            ),
            result,
        )
        assertEquals("/api/search/nip/6691034274", requested.single().encodedPath)
        assertEquals("2026-10-13", requested.single().parameters["date"])
    }

    @Test
    fun `NIP with prefix and spaces is normalized`() {
        runBlocking { service(subject("Czynny", "")).lookup(" PL 669 103 42 74 ") }

        assertEquals("/api/search/nip/6691034274", requested.single().encodedPath)
    }

    @Test
    fun `invalid NIP does not call the registry`() {
        val service = service(subject("Czynny", ""))

        assertEquals(CompanyLookupResult.InvalidNip, runBlocking { service.lookup("1234567890") })
        assertEquals(CompanyLookupResult.InvalidNip, runBlocking { service.lookup("12345") })
        assertTrue(requested.isEmpty())
    }

    @Test
    fun `exempt company is flagged and invalid or repeated accounts are dropped`() {
        val accounts = "\"71101075753723914707418989\", \"71101075753723914707418989\", \"1\""
        val result = runBlocking { service(subject("Zwolniony", accounts)).lookup("6691034274") }

        val company = (result as CompanyLookupResult.Found).company
        assertEquals(false, company.isActiveVatPayer)
        assertEquals(listOf("71101075753723914707418989"), company.accountNumbers)
    }

    @Test
    fun `not found and failures are passed through`() {
        val notFound = """{"result": {"subject": null, "requestId": "Test6-000aaaa", "requestDateTime": "13-10-2026 15:00:00"}}"""

        assertEquals(CompanyLookupResult.NotFound, runBlocking { service(notFound).lookup("4824062973") })
        assertEquals(
            CompanyLookupResult.Failed(WhiteListFailure.RateLimited),
            runBlocking { service("", HttpStatusCode.TooManyRequests).lookup("6691034274") },
        )
    }
}
