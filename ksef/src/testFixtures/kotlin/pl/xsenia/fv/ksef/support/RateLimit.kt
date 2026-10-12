package pl.xsenia.fv.ksef.support

import org.opentest4j.TestAbortedException
import pl.xsenia.fv.ksef.KsefRateLimitException

/**
 * Runs an integration test body; when KSeF TEST answers HTTP 429 the test is reported as skipped (not failed).
 * The metadata limit (20/h per context and IP) is shared with the emulator and other test runs.
 */
inline fun <T> skipOnRateLimit(block: () -> T): T =
    try {
        block()
    } catch (e: KsefRateLimitException) {
        throw TestAbortedException("KSeF TEST rate limit reached, retry after ${e.retryAfter?.inWholeSeconds ?: "?"} s: ${e.details}")
    }
