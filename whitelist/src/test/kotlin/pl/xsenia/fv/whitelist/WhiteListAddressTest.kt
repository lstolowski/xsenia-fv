package pl.xsenia.fv.whitelist

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WhiteListAddressTest {

    @Test
    fun `splits at the last comma`() {
        assertEquals(WhiteListAddress("TESTOWA 1", "00-001 TESTOWO"), WhiteListAddress.parse("TESTOWA 1, 00-001 TESTOWO"))
        assertEquals(
            WhiteListAddress("OSIEDLE TESTOWE, BLOK 3/4", "00-005 TESTOWO"),
            WhiteListAddress.parse("OSIEDLE TESTOWE, BLOK 3/4, 00-005 TESTOWO"),
        )
    }

    @Test
    fun `without a comma everything is the first line`() {
        assertEquals(WhiteListAddress("TESTOWO 15", null), WhiteListAddress.parse("  TESTOWO 15 "))
    }

    @Test
    fun `empty parts are dropped`() {
        assertEquals(WhiteListAddress("TESTOWA 1", null), WhiteListAddress.parse("TESTOWA 1, "))
        assertEquals(WhiteListAddress("00-001 TESTOWO", null), WhiteListAddress.parse(", 00-001 TESTOWO"))
    }

    @Test
    fun `blank address is null`() {
        assertNull(WhiteListAddress.parse(null))
        assertNull(WhiteListAddress.parse("   "))
    }
}
