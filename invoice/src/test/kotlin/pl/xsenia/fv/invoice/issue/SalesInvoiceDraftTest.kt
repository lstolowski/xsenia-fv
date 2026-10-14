package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.issue.TestDrafts.draft

class SalesInvoiceDraftTest {

    private val accountWithSwift = TestDrafts.account.copy(swift = "TESTPLPW")

    @Test
    fun `missing BIC (SWIFT) matters only for a foreign buyer`() {
        assertTrue(draft(buyer = TestDrafts.germanBuyer).swiftMissingForForeignBuyer)
        assertTrue(draft(buyer = TestDrafts.americanBuyer).swiftMissingForForeignBuyer)
        assertFalse(draft(buyer = TestDrafts.germanBuyer, account = accountWithSwift).swiftMissingForForeignBuyer)
        assertFalse(draft(buyer = TestDrafts.buyer).swiftMissingForForeignBuyer)
    }
}
