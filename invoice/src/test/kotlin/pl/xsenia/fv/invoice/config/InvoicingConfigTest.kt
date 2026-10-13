package pl.xsenia.fv.invoice.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InvoicingConfigTest {

    private fun example(name: String) =
        requireNotNull(javaClass.getResource("/config/$name")) { "missing test resource $name" }.readText()

    private fun parseExamples(
        company: String? = example("company.json"),
        banks: String? = example("banks.json"),
        counterparties: String? = example("counterparties.json"),
    ) = InvoicingConfig.parse(company, banks, counterparties)

    @Test
    fun `example files are valid`() {
        val config = parseExamples()

        // The examples may be edited freely — they only have to stay valid.
        assertEquals(emptyList<ConfigIssue>(), config.issues)
        assertTrue(config.company != null)
        assertTrue(config.invoiceBankAccounts.isNotEmpty())
        assertTrue(config.counterparties.isNotEmpty())
        assertTrue(config.canIssueInvoices)
    }

    @Test
    fun `cannot issue invoices without a valid company`() {
        val config = parseExamples(company = null)

        assertFalse(config.canIssueInvoices)
        assertEquals(ConfigIssueType.FILE_MISSING, config.issues.single().type)
    }

    @Test
    fun `cannot issue invoices when banks file has an invalid entry`() {
        val banks = """{"banks": [
            {"name": "Konto Toyota", "account": "85216097583216850246002152"},
            {"name": "Konto błędne", "account": "1"}
        ]}"""

        val config = parseExamples(banks = banks)

        assertEquals(1, config.invoiceBankAccounts.size)
        assertFalse(config.canIssueInvoices)
    }

    @Test
    fun `offers only PLN accounts on invoices`() {
        val banks = """{"banks": [{"name": "Konto EUR", "account": "85216097583216850246002152", "currency": "EUR"}]}"""

        val config = parseExamples(banks = banks)

        assertTrue(config.issues.isEmpty())
        assertTrue(config.invoiceBankAccounts.isEmpty())
        assertFalse(config.canIssueInvoices)
    }

    @Test
    fun `invalid counterparty does not block invoicing to the remaining ones`() {
        val counterparties = """{"counterparties": [
            {"name": "Testowa Sp. z o.o.", "nip": "6691034274", "address": {"line1": "ul. Testowa 6"}},
            {"name": "Błędny NIP", "nip": "6691034275", "address": {"line1": "ul. Testowa 7"}}
        ]}"""

        val config = parseExamples(counterparties = counterparties)

        assertEquals(1, config.counterparties.size)
        assertEquals(1, config.issues.size)
        assertTrue(config.canIssueInvoices)
    }
}
