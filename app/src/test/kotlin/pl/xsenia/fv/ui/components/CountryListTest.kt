package pl.xsenia.fv.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CountryListTest {

    @Test
    fun `Poland first, then all countries`() {
        val all = CountryList.matching("")

        assertEquals("PL", all.first())
        assertEquals(1, all.count { it == "PL" })
        assertTrue(all.size > 200)
    }

    @Test
    fun `searches the Polish name or the code`() {
        assertEquals(listOf("DE"), CountryList.matching("niemc"))
        assertTrue("US" in CountryList.matching("Stany"))
        assertEquals(listOf("GB"), CountryList.matching("gb"))
        assertEquals(emptyList<String>(), CountryList.matching("Xyzzy"))
    }
}
