package pl.xsenia.fv.ksef

import kotlin.time.Duration

/** Base class of all errors reported by the KSeF client. */
sealed class KsefException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Single entry of the KSeF `exceptionDetailList`. */
data class KsefErrorDetail(val code: Int, val description: String?, val details: List<String>)

/** KSeF responded with an HTTP error (4xx/5xx other than 429). */
class KsefApiException(
    val httpStatus: Int,
    val errors: List<KsefErrorDetail>,
    val serviceCode: String?,
    rawBody: String? = null,
) : KsefException(
    buildString {
        append("KSeF API error HTTP $httpStatus")
        if (errors.isNotEmpty()) {
            append(": ")
            append(errors.joinToString("; ") { "${it.code} ${it.description.orEmpty()} ${it.details}".trim() })
        } else if (!rawBody.isNullOrBlank()) {
            append(": ")
            append(rawBody.take(300))
        }
        serviceCode?.let { append(" (serviceCode=$it)") }
    },
)

/** KSeF rejected the request because an API rate limit was exceeded (HTTP 429). */
class KsefRateLimitException(val retryAfter: Duration?, val details: List<String>) :
    KsefException("KSeF rate limit exceeded; retry after ${retryAfter?.inWholeSeconds ?: "?"} s. $details")

/** The asynchronous authentication operation finished with a non-success status. */
class KsefAuthenticationException(val statusCode: Int, val description: String, val details: List<String>) :
    KsefException("KSeF authentication failed: $statusCode $description $details")

/**
 * KSeF rejected a sent invoice (status 4xx/5xx of the invoice in the session), e.g. 450 — semantic validation
 * error, 440 — duplicate ([extensions] contain `originalKsefNumber`).
 */
class KsefInvoiceRejectedException(
    val statusCode: Int,
    val description: String,
    val details: List<String>,
    val extensions: Map<String, String?>,
) : KsefException("KSeF rejected the invoice: $statusCode $description $details")

/** An asynchronous KSeF operation did not finish within the expected time. */
class KsefTimeoutException(message: String) : KsefException(message)

/** KSeF returned data the client cannot use (e.g. no valid public key certificate). */
class KsefProtocolException(message: String, cause: Throwable? = null) : KsefException(message, cause)
