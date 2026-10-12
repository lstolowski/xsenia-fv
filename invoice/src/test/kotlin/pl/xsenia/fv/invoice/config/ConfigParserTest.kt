package pl.xsenia.fv.invoice.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigParserTest {

    private val validCompany = """
        {"company": {"name": "Jan Testowy", "nip": "1996845518",
          "address": {"line1": "ul. Testowa 1", "line2": "00-001 Testowo", "country": "Polska"},
          "email": "jan.testowy@example.com", "phone": "+48 000 000 000"}}
    """.trimIndent()

    @Test
    fun `parses valid company`() {
        val result = ConfigParser.parseCompany(validCompany)

        assertEquals(emptyList<ConfigIssue>(), result.issues)
        assertEquals(
            Company("Jan Testowy", "1996845518", Address("ul. Testowa 1", "00-001 Testowo", "Polska"), "jan.testowy@example.com", "+48 000 000 000"),
            result.value,
        )
    }

    @Test
    fun `reports all company problems at once`() {
        val result = ConfigParser.parseCompany(
            """{"company": {"name": "Jan Testowy", "nip": "1234567890", "address": {"line2": "00-001 Testowo"}, "email": "brak-malpy"}}""",
        )

        assertNull(result.value)
        assertEquals(
            listOf(
                ConfigIssue(ConfigFile.COMPANY, ConfigIssueType.INVALID_NIP, entryName = "Jan Testowy", field = "nip"),
                ConfigIssue(ConfigFile.COMPANY, ConfigIssueType.MISSING_FIELD, entryName = "Jan Testowy", field = "address.line1"),
                ConfigIssue(ConfigFile.COMPANY, ConfigIssueType.INVALID_EMAIL, entryName = "Jan Testowy", field = "email"),
            ),
            result.issues,
        )
    }

    @Test
    fun `reports missing company object`() {
        assertEquals(
            ConfigIssue(ConfigFile.COMPANY, ConfigIssueType.MISSING_FIELD, field = "company"),
            ConfigParser.parseCompany("""{"firma": {}}""").issues.single(),
        )
    }

    @Test
    fun `reports missing file and invalid JSON`() {
        assertEquals(ConfigIssueType.FILE_MISSING, ConfigParser.parseCompany(null).issues.single().type)

        val invalid = ConfigParser.parseBanks("""{"banks": [ {"name": "Konto" ]}""").issues.single()
        assertEquals(ConfigIssueType.INVALID_JSON, invalid.type)
        assertEquals(ConfigFile.BANKS, invalid.file)
    }

    @Test
    fun `reports missing list field and empty list`() {
        assertEquals(
            ConfigIssue(ConfigFile.COUNTERPARTIES, ConfigIssueType.MISSING_FIELD, field = "counterparties"),
            ConfigParser.parseCounterparties("""{"kontrahenci": []}""").issues.single(),
        )
        assertEquals(
            ConfigIssue(ConfigFile.BANKS, ConfigIssueType.EMPTY_LIST, field = "banks"),
            ConfigParser.parseBanks("""{"banks": []}""").issues.single(),
        )
    }

    @Test
    fun `parses bank accounts, normalizes numbers and defaults currency to PLN`() {
        val result = ConfigParser.parseBanks(
            """{"banks": [
                 {"name": "Konto Toyota", "bank": "Toyota Bank", "account": "PL85 2160 9758 3216 8502 4600 2152", "currency": "pln"},
                 {"name": "Konto NBP", "account": "71101075753723914707418989"}
               ]}""",
        )

        assertEquals(emptyList<ConfigIssue>(), result.issues)
        assertEquals(
            listOf(
                BankAccount("Konto Toyota", "Toyota Bank", "85216097583216850246002152", "PLN"),
                BankAccount("Konto NBP", null, "71101075753723914707418989", "PLN"),
            ),
            result.value,
        )
    }

    @Test
    fun `skips invalid bank account and reports its position and name`() {
        val result = ConfigParser.parseBanks(
            """{"banks": [
                 {"name": "Konto Santander", "account": "55109004081797992973253811"},
                 {"name": "Konto z błędem", "account": "55109004081797992973253812", "currency": "EURO"}
               ]}""",
        )

        assertEquals(listOf("Konto Santander"), result.value.map { it.name })
        assertEquals(
            listOf(
                ConfigIssue(ConfigFile.BANKS, ConfigIssueType.INVALID_ACCOUNT_NUMBER, entryIndex = 1, entryName = "Konto z błędem", field = "account"),
                ConfigIssue(ConfigFile.BANKS, ConfigIssueType.INVALID_CURRENCY, entryIndex = 1, entryName = "Konto z błędem", field = "currency"),
            ),
            result.issues,
        )
    }

    @Test
    fun `accepts NIP written as JSON number but rejects other wrong types`() {
        val result = ConfigParser.parseCounterparties(
            """{"counterparties": [
                 {"name": "Testowa Sp. z o.o.", "nip": 6691034274, "address": {"line1": "ul. Testowa 6"}},
                 {"name": 123, "nip": "9945082500", "address": "ul. Testowa 7"},
                 "Lorem ipsum"
               ]}""",
        )

        assertEquals(listOf("6691034274"), result.value.map { it.nip })
        assertEquals(
            listOf(
                ConfigIssue(ConfigFile.COUNTERPARTIES, ConfigIssueType.INVALID_TYPE, entryIndex = 1, field = "name"),
                ConfigIssue(ConfigFile.COUNTERPARTIES, ConfigIssueType.INVALID_TYPE, entryIndex = 1, field = "address"),
                ConfigIssue(ConfigFile.COUNTERPARTIES, ConfigIssueType.INVALID_TYPE, entryIndex = 2),
            ),
            result.issues,
        )
    }

    @Test
    fun `skips counterparty with duplicate NIP`() {
        val result = ConfigParser.parseCounterparties(
            """{"counterparties": [
                 {"name": "Testowa A", "nip": "6691034274", "address": {"line1": "ul. Testowa 1"}},
                 {"name": "Testowa B", "nip": "669-103-42-74", "address": {"line1": "ul. Testowa 2"}}
               ]}""",
        )

        assertEquals(listOf("Testowa A"), result.value.map { it.name })
        assertEquals(
            ConfigIssue(ConfigFile.COUNTERPARTIES, ConfigIssueType.DUPLICATE_NIP, entryIndex = 1, entryName = "Testowa B", field = "nip"),
            result.issues.single(),
        )
    }

    @Test
    fun `treats blank required values as missing and enforces FA(3) length limits`() {
        val longName = "Lorem ipsum ".repeat(50)
        val result = ConfigParser.parseCounterparties(
            """{"counterparties": [{"name": "$longName", "nip": "  ", "address": {"line1": "ul. Testowa 1"}}]}""",
        )

        assertTrue(result.value.isEmpty())
        assertEquals(
            listOf(ConfigIssueType.TOO_LONG to "name", ConfigIssueType.MISSING_FIELD to "nip"),
            result.issues.map { it.type to it.field },
        )
    }
}
