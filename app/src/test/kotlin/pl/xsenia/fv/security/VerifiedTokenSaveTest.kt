package pl.xsenia.fv.security

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.ksef.ConnectionCheckResult

class VerifiedTokenSaveTest {

    // Fictitious token in the KSeF format: reference|nip-NIP|key.
    private val token = "20261016-EU-0000000000-AAAAAAAAAA-00|nip-6691034274|0000000000000000000000000000000000000000000000000000000000000000"

    private class Recorder(var result: ConnectionCheckResult, var failure: Exception? = null) {
        val checked = mutableListOf<Pair<String?, String>>()
        val stored = mutableListOf<String>()
        val after = mutableListOf<String>()

        val save = VerifiedTokenSave(
            check = { nip, token ->
                checked += nip to token
                failure?.let { throw it }
                result
            },
            store = { stored += it },
            afterSave = { after += it },
        )
    }

    @Test
    fun `accepted token is stored after the check and the old session is dropped`() = runBlocking {
        val recorder = Recorder(ConnectionCheckResult.Success)

        val result = recorder.save.save("  $token\n")

        assertEquals(ConnectionCheckResult.Success, result)
        assertEquals(listOf("6691034274" to token), recorder.checked)
        assertEquals(listOf(token), recorder.stored)
        assertEquals(listOf("6691034274"), recorder.after)
    }

    @Test
    fun `rejected token is not stored, so the working one stays`() = runBlocking {
        val rejections = listOf(
            ConnectionCheckResult.AuthenticationFailed(450, "invalid token"),
            ConnectionCheckResult.RateLimited(30),
            ConnectionCheckResult.NetworkError("no network"),
            ConnectionCheckResult.ApiError("HTTP 503"),
        )
        rejections.forEach { rejection ->
            val recorder = Recorder(rejection)

            assertEquals(rejection, recorder.save.save(token))
            assertTrue(recorder.stored.isEmpty(), "stored after $rejection")
            assertTrue(recorder.after.isEmpty())
        }
    }

    @Test
    fun `an unexpected exception of the check is reported and nothing is stored`() = runBlocking {
        val recorder = Recorder(ConnectionCheckResult.Success, failure = IllegalStateException("boom"))

        assertEquals(ConnectionCheckResult.ApiError("boom"), recorder.save.save(token))
        assertTrue(recorder.stored.isEmpty())
    }

    @Test
    fun `token without NIP is not stored even if a check would pass`() = runBlocking {
        val recorder = Recorder(ConnectionCheckResult.Success)

        recorder.save.save("no-nip-here")

        assertTrue(recorder.stored.isEmpty())
    }
}
