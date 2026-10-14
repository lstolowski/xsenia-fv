package pl.xsenia.fv.invoice.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CountriesTest {

    private fun enumeration(resource: String, type: String): Set<String> {
        val xsd = requireNotNull(javaClass.classLoader.getResource(resource)).readText()
        val start = xsd.indexOf("name=\"$type\"")
        val end = xsd.indexOf("</xsd:simpleType>", start)
        return Regex("<xsd:enumeration value=\"([A-Z]+)\"").findAll(xsd.substring(start, end)).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `every country offered is a FA(3) country code`() {
        val countryCodes = enumeration("xsd/bazowe/KodyKrajow_v10-0E.xsd", "TKodKraju")

        assertEquals(emptyList<String>(), Countries.ALL.filterNot { it in countryCodes })
    }

    @Test
    fun `every EU country has a FA(3) VAT UE prefix`() {
        val prefixes = enumeration("xsd/FA3.xsd", "TKodyKrajowUE")

        assertEquals(emptyList<String>(), Countries.EU.map(Countries::vatPrefix).filterNot { it in prefixes })
        assertEquals("EL", Countries.vatPrefix("GR"))
        assertEquals("DE", Countries.vatPrefix("DE"))
    }

    @Test
    fun `EU membership and Polish names`() {
        assertTrue(Countries.isOtherEu("DE"))
        assertFalse(Countries.isOtherEu("PL"))
        assertFalse(Countries.isOtherEu("GB"))
        assertEquals("Niemcy", Countries.name("DE"))
        assertEquals("Polska", Countries.name("PL"))
        assertEquals("Polska", Countries.name(Countries.ALL.first { it == "PL" }))
    }
}
