package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.currency.ExchangeRate
import java.math.BigDecimal
import java.time.LocalDate

class InvoiceFormTest {

    private val form = InvoiceForm(
        issueDate = LocalDate.parse("2026-10-14"),
        deliveryDate = LocalDate.parse("2026-10-14"),
        invoiceNumber = "01/10/2026",
        lines = listOf(LineForm("Usługa testowa", "1", "szt.", "100")),
        currency = InvoiceCurrency.EUR,
    )

    private val workingRate = SavedExchangeRate.of(
        ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3775"), "197/A/NBP/2026", LocalDate.parse("2026-10-09")),
        referenceDate = form.rateReferenceDate,
    )

    @Test
    fun `saving the working rate is not an edit of the invoice`() {
        val withRate = form.copy(exchangeRate = workingRate)

        assertTrue(form.sameInvoiceAs(withRate))
        assertTrue(withRate.sameInvoiceAs(form))
        assertEquals(workingRate.toExchangeRate(), withRate.workingRate)
    }

    @Test
    fun `any other change is an edit`() {
        assertFalse(form.sameInvoiceAs(form.copy(invoiceNumber = "02/10/2026")))
        assertFalse(form.sameInvoiceAs(form.copy(lines = listOf(LineForm("Usługa testowa", "2", "szt.", "100")))))
        assertFalse(form.sameInvoiceAs(form.copy(currency = InvoiceCurrency.USD)))
        assertFalse(form.sameInvoiceAs(form.copy(deliveryDate = LocalDate.parse("2026-10-13"))))
    }
}
