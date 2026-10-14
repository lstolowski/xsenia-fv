package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.issue.TestDrafts.draft
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.issue.TestDrafts.line
import pl.xsenia.fv.invoice.issue.TestDrafts.rate
import java.time.LocalDate

class DraftValidatorTest {

    private val today = LocalDate.parse("2026-10-01")

    private fun issues(draft: SalesInvoiceDraft, existing: List<String> = emptyList()) =
        DraftValidator.validate(draft, existing, today)

    @Test
    fun `valid draft has no issues`() {
        assertEquals(emptyList<DraftIssue>(), issues(draft()))
    }

    @Test
    fun `not taxable and zero rate lines need nothing more`() {
        assertEquals(emptyList<DraftIssue>(), issues(draft(lines = listOf(line(rate = VatRate.NOT_TAXABLE), line(rate = VatRate.RATE_0)))))
    }

    @Test
    fun `reports number problems, mismatch only as a warning`() {
        assertEquals(listOf(DraftIssue(DraftIssueType.NUMBER_MISSING)), issues(draft(number = " ")))
        assertEquals(listOf(DraftIssue(DraftIssueType.NUMBER_DUPLICATE)), issues(draft(), existing = listOf("01/09/2026")))
        assertEquals(
            listOf(DraftIssue(DraftIssueType.NUMBER_MONTH_MISMATCH, blocking = false)),
            issues(draft(number = "01/10/2026")),
        )
    }

    @Test
    fun `reports line problems with their index`() {
        val result = issues(
            draft(
                lines = listOf(
                    line(),
                    line(name = " ", unit = "", quantity = "0", price = "-1"),
                    line(quantity = "1.1234567", price = "1.123456789"),
                ),
            ),
        )

        assertEquals(
            listOf(
                DraftIssue(DraftIssueType.LINE_NAME_MISSING, 1),
                DraftIssue(DraftIssueType.LINE_UNIT_MISSING, 1),
                DraftIssue(DraftIssueType.LINE_QUANTITY_NOT_POSITIVE, 1),
                DraftIssue(DraftIssueType.LINE_PRICE_NEGATIVE, 1),
                DraftIssue(DraftIssueType.LINE_QUANTITY_TOO_PRECISE, 2),
                DraftIssue(DraftIssueType.LINE_PRICE_TOO_PRECISE, 2),
            ),
            result,
        )
    }

    @Test
    fun `rejects missing lines, zero total and future issue date`() {
        assertEquals(listOf(DraftIssue(DraftIssueType.NO_LINES)), issues(draft(lines = emptyList())))
        assertEquals(listOf(DraftIssue(DraftIssueType.TOTAL_NOT_POSITIVE)), issues(draft(lines = listOf(line(price = "0")))))
        assertEquals(listOf(DraftIssue(DraftIssueType.ISSUE_DATE_IN_FUTURE)), issues(draft(issueDate = "2026-10-02", deliveryDate = "2026-10-02", number = "01/10/2026")))
    }

    @Test
    fun `foreign currency invoice needs the rate of its currency`() {
        val missing = listOf(DraftIssue(DraftIssueType.EXCHANGE_RATE_MISSING))
        val euro = TestDrafts.euroAccount

        assertEquals(missing, issues(draft(currency = InvoiceCurrency.EUR, account = euro)))
        assertEquals(missing, issues(draft(currency = InvoiceCurrency.EUR, account = euro, exchangeRate = rate(InvoiceCurrency.USD))))
        assertEquals(emptyList<DraftIssue>(), issues(draft(currency = InvoiceCurrency.EUR, account = euro, exchangeRate = rate())))
    }

    @Test
    fun `bank account in another currency is only a warning`() {
        val warning = DraftIssue(DraftIssueType.BANK_ACCOUNT_CURRENCY_MISMATCH, blocking = false)

        assertEquals(listOf(warning), issues(draft(currency = InvoiceCurrency.EUR, exchangeRate = rate())))
        assertEquals(listOf(warning), issues(draft(account = TestDrafts.euroAccount)))
    }
}
