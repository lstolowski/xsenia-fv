package pl.xsenia.fv.ksef.http

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import pl.xsenia.fv.ksef.KsefApiException
import pl.xsenia.fv.ksef.KsefErrorDetail
import pl.xsenia.fv.ksef.KsefRateLimitException
import kotlin.time.Duration.Companion.seconds

/** Returns the deserialized body, or throws a [pl.xsenia.fv.ksef.KsefException] for an error response. */
internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    throwIfError()
    return body()
}

internal suspend fun HttpResponse.throwIfError() {
    if (status.isSuccess()) return
    val text = bodyAsText()

    if (status == HttpStatusCode.TooManyRequests) {
        val retryAfter = headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()?.seconds
        val details = runCatching { KsefJson.decodeFromString<TooManyRequestsResponse>(text).status.details }
            .getOrDefault(emptyList())
        throw KsefRateLimitException(retryAfter, details)
    }

    val exception = runCatching { KsefJson.decodeFromString<ExceptionResponse>(text).exception }.getOrNull()
    val errors = exception?.exceptionDetailList.orEmpty().map {
        KsefErrorDetail(it.exceptionCode, it.exceptionDescription, it.details.orEmpty())
    }
    throw KsefApiException(status.value, errors, exception?.serviceCode, rawBody = text.takeIf { errors.isEmpty() })
}

@Serializable
internal data class ExceptionResponse(val exception: ExceptionInfo? = null)

@Serializable
internal data class ExceptionInfo(
    val exceptionDetailList: List<ExceptionDetail>? = null,
    val serviceCode: String? = null,
)

@Serializable
internal data class ExceptionDetail(
    val exceptionCode: Int,
    val exceptionDescription: String? = null,
    val details: List<String>? = null,
)

@Serializable
internal data class TooManyRequestsResponse(val status: TooManyRequestsStatus)

@Serializable
internal data class TooManyRequestsStatus(val code: Int, val description: String? = null, val details: List<String> = emptyList())
