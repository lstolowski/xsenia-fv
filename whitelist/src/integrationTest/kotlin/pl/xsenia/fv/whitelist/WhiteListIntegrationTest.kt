package pl.xsenia.fv.whitelist

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Checks that the real White List API still answers in the expected format. Uses a fictitious NIP only
 * (docs/przyklady/nip.md) — the registry does not know it, so no real company data is fetched.
 */
class WhiteListIntegrationTest {

    @Test
    fun `fictitious NIP is not found`() {
        val today = LocalDate.now(ZoneId.of("Europe/Warsaw"))

        val result = runBlocking { WhiteListClient.create().findByNip("4824062973", today) }

        assertEquals(WhiteListResult.NotFound, result)
    }
}
