package pl.xsenia.fv.invoice.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class InvoicingConfigTest {

    private val examplesDir: Path = Path.of(requireNotNull(System.getProperty("examplesDir")) { "examplesDir not set by Gradle" })

    private fun example(name: String) = Files.readString(examplesDir.resolve(name))

    private fun parseExamples(
        company: String? = example("company.json"),
        banks: String? = example("banks.json"),
        counterparties: String? = example("counterparties.json"),
    ) = InvoicingConfig.parse(company, banks, counterparties)

    @Test
    fun `example files in docs are valid`() {
        val config = parseExamples()

        assertEquals(emptyList<ConfigIssue>(), config.issues)
        assertEquals("1996845518", config.company?.nip)
        assertEquals(2, config.invoiceBankAccounts.size)
        assertEquals(listOf("6691034274", "9945082500"), config.counterparties.map { it.nip })
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
