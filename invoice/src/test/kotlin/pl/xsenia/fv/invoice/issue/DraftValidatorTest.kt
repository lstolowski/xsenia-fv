package pl.xsenia.fv.invoice.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

    private fun types(draft: SalesInvoiceDraft) = issues(draft).map { it.type to it.lineIndex }

    @Test
    fun `quantity and price at the FA(3) digit limits are accepted`() {
        // TIlosci: 16 integer digits; TKwotowy2: 14 integer digits. VAT 0% keeps the totals within TKwotowy.
        val atLimit = line(quantity = "1", price = "99999999999999", rate = VatRate.RATE_0)
        val quantityAtLimit = line(quantity = "9999999999999999", price = "0.5", rate = VatRate.RATE_0)

        assertEquals(emptyList<DraftIssue>(), issues(draft(lines = listOf(atLimit, quantityAtLimit))))
    }

    @Test
    fun `quantity above 16 and price above 14 integer digits are reported`() {
        val result = types(draft(lines = listOf(line(quantity = "10000000000000000", price = "1", rate = VatRate.RATE_0), line(quantity = "1", price = "100000000000000", rate = VatRate.RATE_0))))

        assertTrue(result.contains(DraftIssueType.LINE_QUANTITY_TOO_LARGE to 0))
        assertTrue(result.contains(DraftIssueType.LINE_PRICE_TOO_LARGE to 1))
    }

    @Test
    fun `a line value or a total above 16 integer digits is reported`() {
        // 10^9 × 10^8 = 10^17 — each factor is fine, the product is not.
        val result = types(draft(lines = listOf(line(quantity = "1000000000", price = "100000000", rate = VatRate.RATE_0))))

        assertTrue(result.contains(DraftIssueType.LINE_AMOUNT_TOO_LARGE to 0))
        assertTrue(result.contains(DraftIssueType.AMOUNT_TOO_LARGE to null))
    }

    @Test
    fun `lines that fit but add up above the limit exceed the total`() {
        val line = line(quantity = "1", price = "6000000000000000", rate = VatRate.RATE_0)

        val result = types(draft(lines = listOf(line, line)))

        assertFalse(result.any { it.first == DraftIssueType.LINE_AMOUNT_TOO_LARGE })
        assertTrue(result.contains(DraftIssueType.AMOUNT_TOO_LARGE to null))
    }

    @Test
    fun `VAT that pushes the gross total above the limit is reported`() {
        // Net has 16 digits, VAT 23% of it too, gross reaches 17 digits.
        val result = types(draft(lines = listOf(line(quantity = "1", price = "9000000000000000", rate = VatRate.RATE_23))))

        assertTrue(result.contains(DraftIssueType.AMOUNT_TOO_LARGE to null))
    }

    @Test
    fun `a very long sequence in the invoice number does not break validation`() {
        val longNumber = "12345678901234567890/09/2026"

        // No exception; the number is just not recognized as NN/MM/RRRR, so no month mismatch warning either.
        assertEquals(emptyList<DraftIssue>(), issues(draft(number = longNumber), existing = listOf(longNumber.replace("5", "6"))))
        assertEquals(listOf(DraftIssue(DraftIssueType.NUMBER_DUPLICATE)), issues(draft(number = longNumber), existing = listOf(longNumber)))
    }
}
