package pl.xsenia.fv.issue

import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.issue.DraftIssue
import pl.xsenia.fv.invoice.issue.DraftIssueType
import pl.xsenia.fv.invoice.issue.VatRate
import java.math.BigDecimal
import java.time.LocalDate

class InvoiceFormCheckTest {

    private val config = CompanyProfile(
        nip = "9945082500",
        company = Company("Jan Testowy", "9945082500", Address("ul. Testowa 1", null, null), null, null),
        bankAccounts = listOf(BankAccount("Konto testowe", "Toyota Bank", "85216097583216850246002152", "PLN")),
        counterparties = listOf(Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", null, null), null)),
    )
    private val today = LocalDate.parse("2026-10-01")

    private fun form(vararg lines: LineForm, nip: String? = "6691034274", number: String = "01/09/2026") = InvoiceForm(
        counterpartyId = nip,
        bankAccountNumber = "85216097583216850246002152",
        issueDate = today,
        deliveryDate = LocalDate.parse("2026-09-30"),
        invoiceNumber = number,
        lines = lines.toList(),
    )

    @Test
    fun `builds a draft from a complete form`() {
        val check = InvoiceFormCheck.check(form(LineForm("Usługa testowa", "1", "szt.", "1 234,50")), config, emptyList(), today)

        assertTrue(check.canPreview)
        val draft = requireNotNull(check.draft)
        assertEquals(BigDecimal("1234.50"), draft.lines.single().unitNetPrice)
        assertEquals("Testowa Sp. z o.o.", draft.buyer.name)
    }

    @Test
    fun `line rate and unit are passed to the draft, new lines default to 23 percent and hours`() {
        val check = InvoiceFormCheck.check(
            form(LineForm("Usługa testowa", "2", "dni", "100", VatRate.RATE_8), LineForm("Usługa testowa", unitNetPrice = "10")),
            config,
            emptyList(),
            today,
        )

        val lines = requireNotNull(check.draft).lines
        assertEquals(listOf(VatRate.RATE_8, VatRate.RATE_23), lines.map { it.vatRate })
        assertEquals(listOf("dni", "godz."), lines.map { it.unit })
    }

    @Test
    fun `not taxable line can be previewed`() {
        val check = InvoiceFormCheck.check(form(LineForm("Usługa testowa", "1", "usł.", "100", VatRate.NOT_TAXABLE)), config, emptyList(), today)

        assertTrue(check.canPreview)
    }

    @Test
    fun `draft saved before VAT rates were added loads with 23 percent and keeps its unit`() {
        val line = Json.decodeFromString(LineForm.serializer(), """{"name": "Usługa testowa", "quantity": "1", "unit": "szt.", "unitNetPrice": "10"}""")

        assertEquals(LineForm("Usługa testowa", "1", "szt.", "10", VatRate.RATE_23), line)
    }

    @Test
    fun `reports missing counterparty and unparsable numbers`() {
        val check = InvoiceFormCheck.check(form(LineForm("A", "x", "szt.", ""), nip = null), config, emptyList(), today)

        assertNull(check.draft)
        assertFalse(check.canPreview)
        assertEquals(
            listOf(
                FormIssue(FormIssueType.COUNTERPARTY_MISSING),
                FormIssue(FormIssueType.LINE_QUANTITY_INVALID, 0),
                FormIssue(FormIssueType.LINE_PRICE_INVALID, 0),
            ),
            check.formIssues,
        )
    }

    @Test
    fun `month mismatch is only a warning`() {
        val check = InvoiceFormCheck.check(form(LineForm("A", "1", "szt.", "10"), number = "01/10/2026"), config, emptyList(), today)

        assertEquals(listOf(DraftIssue(DraftIssueType.NUMBER_MONTH_MISMATCH, blocking = false)), check.draftIssues)
        assertTrue(check.canPreview)
    }

    @Test
    fun `duplicate number blocks the preview`() {
        val check = InvoiceFormCheck.check(form(LineForm("A", "1", "szt.", "10")), config, listOf("01/09/2026"), today)

        assertFalse(check.canPreview)
    }

    private val euroAccount = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR")
    private val euroRate = SavedExchangeRate(InvoiceCurrency.EUR, "4.3775", "197/A/NBP/2026", LocalDate.parse("2026-10-09"), LocalDate.parse("2026-09-30"))

    @Test
    fun `currency and working rate are passed to the draft`() {
        val form = form(LineForm("Usługa testowa", "1", "szt.", "100"))
            .copy(currency = InvoiceCurrency.EUR, exchangeRate = euroRate, bankAccountNumber = euroAccount.number)
        val check = InvoiceFormCheck.check(form, config.copy(bankAccounts = listOf(euroAccount)), emptyList(), today)

        assertTrue(check.canPreview)
        val draft = requireNotNull(check.draft)
        assertEquals(InvoiceCurrency.EUR, draft.currency)
        assertEquals(BigDecimal("4.3775"), draft.exchangeRate?.rate)
    }

    @Test
    fun `working rate of other dates or another currency is not used`() {
        val base = form(LineForm("Usługa testowa", "1", "szt.", "100")).copy(currency = InvoiceCurrency.EUR, exchangeRate = euroRate)
        val profile = config.copy(bankAccounts = config.bankAccounts + euroAccount)

        // Delivery date moved: the rate was chosen for 2026-09-30, now the reference day is 2026-10-01.
        val moved = base.copy(deliveryDate = LocalDate.parse("2026-10-01"), bankAccountNumber = euroAccount.number)
        assertNull(moved.workingRate)
        assertFalse(InvoiceFormCheck.check(moved, profile, emptyList(), today).canPreview)
        assertEquals(
            listOf(DraftIssue(DraftIssueType.EXCHANGE_RATE_MISSING)),
            InvoiceFormCheck.check(moved, profile, emptyList(), today).draftIssues.filter { it.blocking },
        )

        assertNull(base.copy(currency = InvoiceCurrency.USD).workingRate)
        assertNull(base.copy(currency = InvoiceCurrency.PLN).workingRate)
        assertNull(base.copy(exchangeRate = euroRate.copy(rate = "abc")).workingRate)
        assertEquals(BigDecimal("4.3775"), base.workingRate?.rate)
    }

    @Test
    fun `account in another currency is only a warning`() {
        val form = form(LineForm("Usługa testowa", "1", "szt.", "100")).copy(currency = InvoiceCurrency.EUR, exchangeRate = euroRate)
        val check = InvoiceFormCheck.check(form, config, emptyList(), today)

        assertTrue(check.canPreview)
        assertEquals(listOf(DraftIssue(DraftIssueType.BANK_ACCOUNT_CURRENCY_MISMATCH, blocking = false)), check.draftIssues)
    }

    @Test
    fun `draft saved before currencies were added loads in PLN without a rate`() {
        val form = Json.decodeFromString(InvoiceForm.serializer(), """{"issueDate": "2026-10-01", "deliveryDate": "2026-09-30"}""")

        assertEquals(InvoiceCurrency.PLN, form.currency)
        assertNull(form.exchangeRate)
    }

    @Test
    fun `currency and working rate survive saving`() {
        val form = form(LineForm("Usługa testowa", "1", "szt.", "100")).copy(currency = InvoiceCurrency.EUR, exchangeRate = euroRate)

        assertEquals(form, Json.decodeFromString(InvoiceForm.serializer(), Json.encodeToString(InvoiceForm.serializer(), form)))
    }

    @Test
    fun `draft saved before US-012 points to its counterparty by NIP, which is the counterparty id`() {
        val form = Json.decodeFromString(InvoiceForm.serializer(), """{"counterpartyNip": "6691034274", "issueDate": "2026-10-01", "deliveryDate": "2026-09-30"}""")

        assertEquals("6691034274", form.counterpartyId)
        assertEquals(config.counterparties.single().id, form.counterpartyId)
        assertTrue(Json.encodeToString(InvoiceForm.serializer(), form).contains("\"counterpartyNip\":\"6691034274\""))
    }

    @Test
    fun `foreign counterparty is found by its id`() {
        val german = Counterparty("Lorem Testowy GmbH", address = Address("Teststraße 1", null, null), countryCode = "DE", taxNumber = "999999990")
        val form = form(LineForm("Usługa testowa", "1", "szt.", "100"), nip = german.id)

        val draft = requireNotNull(InvoiceFormCheck.check(form, config.copy(counterparties = config.counterparties + german), emptyList(), today).draft)

        assertEquals(german, draft.buyer)
    }
}
