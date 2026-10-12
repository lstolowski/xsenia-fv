package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.config.Address
import pl.xsenia.fv.invoice.config.BankAccount
import pl.xsenia.fv.invoice.config.Company
import pl.xsenia.fv.invoice.config.Counterparty
import pl.xsenia.fv.invoice.config.InvoicingConfig
import pl.xsenia.fv.invoice.issue.DraftIssue
import pl.xsenia.fv.invoice.issue.DraftIssueType
import java.math.BigDecimal
import java.time.LocalDate

class InvoiceFormCheckTest {

    private val config = InvoicingConfig(
        company = Company("Jan Testowy", "9945082500", Address("ul. Testowa 1", null, null), null, null),
        bankAccounts = listOf(BankAccount("Konto testowe", "Toyota Bank", "85216097583216850246002152", "PLN")),
        counterparties = listOf(Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", null, null), null)),
        issues = emptyList(),
    )
    private val today = LocalDate.parse("2026-10-01")

    private fun form(vararg lines: LineForm, nip: String? = "6691034274", number: String = "01/09/2026") = InvoiceForm(
        counterpartyNip = nip,
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
}
