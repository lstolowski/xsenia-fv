package pl.xsenia.fv.invoice.profile

import org.junit.jupiter.api.Assertions.assertTrue
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// Fictitious data only (docs/przyklady/).
class ProfileValidatorTest {

    private val companyNip = "1996845518"

    private fun issues(validation: Validation<*>) = (validation as Validation.Invalid).issues

    @Test
    fun `valid company is trimmed and blank optional fields become null`() {
        val result = ProfileValidator.company(
            companyNip,
            CompanyInput(name = " Jan Testowy Usługi Testowe ", addressLine1 = "ul. Testowa 1", addressLine2 = " ", country = "Polska", email = "", phone = ""),
        )

        assertEquals(
            Validation.Valid(Company("Jan Testowy Usługi Testowe", companyNip, Address("ul. Testowa 1", null, "Polska"), null, null)),
            result,
        )
    }

    @Test
    fun `company reports every problem at once`() {
        val result = ProfileValidator.company(
            companyNip,
            CompanyInput(name = "", addressLine1 = "  ", email = "jan.testowy", phone = "+48 000 000 000 000"),
        )

        assertEquals(
            listOf(
                FieldIssue(ProfileField.NAME, FieldIssueType.REQUIRED),
                FieldIssue(ProfileField.ADDRESS_LINE1, FieldIssueType.REQUIRED),
                FieldIssue(ProfileField.EMAIL, FieldIssueType.INVALID_EMAIL),
                FieldIssue(ProfileField.PHONE, FieldIssueType.TOO_LONG, ProfileValidator.MAX_PHONE),
            ),
            issues(result),
        )
    }

    @Test
    fun `name longer than the FA(3) limit`() {
        val result = ProfileValidator.company(companyNip, CompanyInput(name = "T".repeat(513), addressLine1 = "ul. Testowa 1"))

        assertEquals(listOf(FieldIssue(ProfileField.NAME, FieldIssueType.TOO_LONG, 512)), issues(result))
    }

    @Test
    fun `bank account number is normalized and currency defaults to PLN`() {
        val result = ProfileValidator.bankAccount(
            BankAccountInput(name = "Konto testowe Toyota", number = "PL 85 2160 9758 3216 8502 4600 2152", bank = "Toyota Bank", currency = ""),
            others = emptyList(),
        )

        assertEquals(Validation.Valid(BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152", "PLN")), result)
    }

    @Test
    fun `bank account problems`() {
        assertEquals(
            listOf(FieldIssue(ProfileField.NAME, FieldIssueType.REQUIRED), FieldIssue(ProfileField.ACCOUNT_NUMBER, FieldIssueType.REQUIRED)),
            issues(ProfileValidator.bankAccount(BankAccountInput(name = "", number = ""), emptyList())),
        )
        assertEquals(
            listOf(FieldIssue(ProfileField.ACCOUNT_NUMBER, FieldIssueType.INVALID_ACCOUNT_NUMBER), FieldIssue(ProfileField.CURRENCY, FieldIssueType.INVALID_CURRENCY)),
            issues(ProfileValidator.bankAccount(BankAccountInput(name = "Konto", number = "85216097583216850246002153", currency = "złoty"), emptyList())),
        )
    }

    @Test
    fun `bank account number must be unique`() {
        val existing = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")

        val result = ProfileValidator.bankAccount(BankAccountInput(name = "Drugie", number = "85216097583216850246002152"), listOf(existing))

        assertEquals(listOf(FieldIssue(ProfileField.ACCOUNT_NUMBER, FieldIssueType.DUPLICATE)), issues(result))
    }

    @Test
    fun `valid counterparty with normalized NIP`() {
        val result = ProfileValidator.counterparty(
            CounterpartyInput(nip = "PL 669-103-42-74", name = "Testowa Sp. z o.o.", addressLine1 = "ul. Testowa 6", addressLine2 = "00-002 Testowo", email = "faktury@example.com"),
            others = emptyList(),
        )

        assertEquals(
            Validation.Valid(Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo", "Polska"), "faktury@example.com")),
            result,
        )
    }

    @Test
    fun `counterparty NIP problems`() {
        val existing = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6"))

        assertEquals(
            listOf(FieldIssue(ProfileField.NIP, FieldIssueType.REQUIRED)),
            issues(ProfileValidator.counterparty(CounterpartyInput(nip = " ", name = "Test", addressLine1 = "ul. Testowa 1"), emptyList())),
        )
        assertEquals(
            listOf(FieldIssue(ProfileField.NIP, FieldIssueType.INVALID_NIP)),
            issues(ProfileValidator.counterparty(CounterpartyInput(nip = "1234567890", name = "Test", addressLine1 = "ul. Testowa 1"), emptyList())),
        )
        assertEquals(
            listOf(FieldIssue(ProfileField.NIP, FieldIssueType.DUPLICATE)),
            issues(ProfileValidator.counterparty(CounterpartyInput(nip = "6691034274", name = "Test", addressLine1 = "ul. Testowa 1"), listOf(existing))),
        )
    }

    // Fictitious numbers (docs/przyklady/kontrahenci.md) — invalid in VIES.
    private fun foreign(countryCode: String, taxNumber: String = "", currency: InvoiceCurrency? = null) = CounterpartyInput(
        nip = "", name = "Lorem Testowy", addressLine1 = "Test Street 1", countryCode = countryCode, taxNumber = taxNumber, defaultCurrency = currency,
    )

    @Test
    fun `EU counterparty has a VAT UE number without the prefix`() {
        val result = ProfileValidator.counterparty(foreign("de", " de 999.999-990 ", InvoiceCurrency.EUR), emptyList())

        val counterparty = (result as Validation.Valid).value
        assertEquals("DE", counterparty.countryCode)
        assertEquals("999999990", counterparty.taxNumber)
        assertEquals(null, counterparty.nip)
        assertEquals(TaxIdType.VAT_UE, counterparty.taxIdType)
        assertEquals("DE999999990", counterparty.taxIdText)
        assertEquals("DE-999999990", counterparty.id)
        assertEquals(InvoiceCurrency.EUR, counterparty.defaultCurrency)
        assertEquals("Niemcy", counterparty.address.country)
    }

    @Test
    fun `Greek VAT UE numbers use the EL prefix`() {
        val withEl = (ProfileValidator.counterparty(foreign("GR", "EL000000000"), emptyList()) as Validation.Valid).value
        val withGr = (ProfileValidator.counterparty(foreign("GR", "GR000000000"), emptyList()) as Validation.Valid).value

        assertEquals("000000000", withEl.taxNumber)
        assertEquals("000000000", withGr.taxNumber)
        assertEquals("EL000000000", withEl.taxIdText)
    }

    @Test
    fun `VAT UE number problems`() {
        assertEquals(listOf(FieldIssue(ProfileField.TAX_NUMBER, FieldIssueType.REQUIRED)), issues(ProfileValidator.counterparty(foreign("DE"), emptyList())))
        assertEquals(
            listOf(FieldIssue(ProfileField.TAX_NUMBER, FieldIssueType.INVALID_VAT_UE)),
            issues(ProfileValidator.counterparty(foreign("DE", "9999999900000"), emptyList())),
        )
        assertEquals(
            listOf(FieldIssue(ProfileField.TAX_NUMBER, FieldIssueType.INVALID_VAT_UE)),
            issues(ProfileValidator.counterparty(foreign("FR", "ab/12"), emptyList())),
        )
        val saved = (ProfileValidator.counterparty(foreign("DE", "999999990"), emptyList()) as Validation.Valid).value
        assertEquals(
            listOf(FieldIssue(ProfileField.TAX_NUMBER, FieldIssueType.DUPLICATE)),
            issues(ProfileValidator.counterparty(foreign("DE", "DE999999990"), listOf(saved))),
        )
    }

    @Test
    fun `counterparty from outside the EU has an optional identifier`() {
        val withId = (ProfileValidator.counterparty(foreign("US", " 00-0000000 "), emptyList()) as Validation.Valid).value
        val withoutId = (ProfileValidator.counterparty(foreign("GB"), emptyList()) as Validation.Valid).value

        assertEquals(TaxIdType.OTHER, withId.taxIdType)
        assertEquals("00-0000000", withId.taxIdText)
        assertEquals(TaxIdType.NONE, withoutId.taxIdType)
        assertEquals(null, withoutId.taxIdText)
        // Without a number the id is random, so two such counterparties never clash.
        val another = (ProfileValidator.counterparty(foreign("GB"), listOf(withoutId)) as Validation.Valid).value
        assertTrue(another.id != withoutId.id)
        assertEquals(
            listOf(FieldIssue(ProfileField.TAX_NUMBER, FieldIssueType.TOO_LONG, ProfileValidator.MAX_TAX_ID)),
            issues(ProfileValidator.counterparty(foreign("US", "1".repeat(51)), emptyList())),
        )
    }

    @Test
    fun `unknown country`() {
        assertEquals(listOf(FieldIssue(ProfileField.COUNTRY, FieldIssueType.INVALID_COUNTRY)), issues(ProfileValidator.counterparty(foreign("ZZ"), emptyList())))
    }

    @Test
    fun `edited counterparty keeps its id, country and identifier`() {
        val saved = (ProfileValidator.counterparty(foreign("DE", "999999990"), emptyList()) as Validation.Valid).value

        val edited = ProfileValidator.counterparty(foreign("FR", "123", InvoiceCurrency.GBP).copy(name = "Lorem Testowy AG"), emptyList(), existing = saved)

        assertEquals(saved.copy(name = "Lorem Testowy AG", defaultCurrency = InvoiceCurrency.GBP), (edited as Validation.Valid).value)
    }
}
