package pl.xsenia.fv.ksef

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KsefEnvironmentTest {

    @Test
    fun `only PROD points to the production API host`() {
        val productionHosts = KsefEnvironment.entries.filter { it.apiBaseUrl.startsWith("https://api.ksef.mf.gov.pl") }

        assertEquals(listOf(KsefEnvironment.PROD), productionHosts)
    }

    @Test
    fun `all API URLs use HTTPS and API version 2`() {
        KsefEnvironment.entries.forEach {
            assertTrue(it.apiBaseUrl.startsWith("https://") && it.apiBaseUrl.endsWith("/v2"), it.name)
        }
    }
}
