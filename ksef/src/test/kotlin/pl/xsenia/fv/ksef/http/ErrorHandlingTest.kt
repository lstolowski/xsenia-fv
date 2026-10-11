package pl.xsenia.fv.ksef.http

import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.ksef.KsefApiException
import pl.xsenia.fv.ksef.KsefRateLimitException
import pl.xsenia.fv.ksef.testing.MockKsef
import pl.xsenia.fv.ksef.testing.json
import kotlin.time.Duration.Companion.seconds

class ErrorHandlingTest {

    @Test
    fun `maps KSeF exception response to KsefApiException`() = runTest {
        val ksef = MockKsef {
            json(
                """{"exception":{"exceptionDetailList":[{"exceptionCode":21405,"exceptionDescription":"Błąd walidacji danych wejściowych.",
                   "details":["'dateRange' must not exceed 100 days."]}],"serviceCode":"00-abc","timestamp":"2026-10-11T17:53:25.8357141Z"}}""",
                HttpStatusCode.BadRequest,
            )
        }

        val e = assertThrows<KsefApiException> { ksef.client.post("invoices/query/metadata").throwIfError() }

        assertEquals(400, e.httpStatus)
        assertEquals(21405, e.errors.single().code)
        assertEquals(listOf("'dateRange' must not exceed 100 days."), e.errors.single().details)
        assertEquals("00-abc", e.serviceCode)
    }

    @Test
    fun `maps HTTP 429 to KsefRateLimitException with Retry-After`() = runTest {
        val ksef = MockKsef {
            json(
                """{"status":{"code":429,"description":"Too Many Requests","details":["Przekroczono limit 20 żądań na minutę."]}}""",
                HttpStatusCode.TooManyRequests,
                mapOf("Retry-After" to "30"),
            )
        }

        val e = assertThrows<KsefRateLimitException> { ksef.client.post("auth/challenge").throwIfError() }

        assertEquals(30.seconds, e.retryAfter)
        assertEquals(listOf("Przekroczono limit 20 żądań na minutę."), e.details)
    }

    @Test
    fun `handles error without body`() = runTest {
        val ksef = MockKsef { json("", HttpStatusCode.Unauthorized) }

        val e = assertThrows<KsefApiException> { ksef.client.post("auth/token/refresh").throwIfError() }

        assertEquals(401, e.httpStatus)
        assertEquals(emptyList<Any>(), e.errors)
        assertNull(e.serviceCode)
    }
}
