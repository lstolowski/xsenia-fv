package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.issue.DraftIssueType
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.invoice.issue.VatRate
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Fictitious data only (docs/przyklady/). The copied invoice is read back from FA(3) XML — as one from the KSeF mirror. */
class SimilarInvoiceTest {

    private val seller = Company("Jan Testowy Usługi Testowe", "9945082500", Address("ul. Testowa 1", "00-001 Testowo", "Polska"))
    private val buyer = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo", "Polska"), "faktury@example.com")
    private val germanBuyer = Counterparty(
        "Lorem Testowy GmbH", address = Address("Teststraße 1", "00000 Testdorf", "Niemcy"), countryCode = "DE", taxNumber = "999999990",
        defaultCurrency = InvoiceCurrency.EUR,
    )
    private val greekBuyer = Counterparty("Dolor Testowy A.E.", address = Address("Odos Test 1", "00000 Testopoli", "Grecja"), countryCode = "GR", taxNumber = "000000000")
    private val americanBuyer = Counterparty("Lorem Test Inc.", address = Address("1 Test Street", "Testville, TS 00000", "Stany Zjednoczone"), countryCode = "US", taxNumber = "00-0000000")
    private val britishBuyer = Counterparty("Ipsum Test Ltd", address = Address("1 Test Lane", "Testbury TE1 1ST", "Wielka Brytania"), countryCode = "GB")

    private val toyota = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152")
    private val santander = BankAccount("Konto testowe Santander", "Santander Bank Polska", "55109004081797992973253811")
    private val euroAccount = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR", "TESTPLPW")

    private val profile = CompanyProfile(
        nip = seller.nip,
        company = seller,
        bankAccounts = listOf(toyota, santander, euroAccount),
        counterparties = listOf(buyer, germanBuyer, britishBuyer),
    )

    private val today = LocalDate.parse("2026-10-16")
    private val taken = listOf("01/09/2026", "02/09/2026", "01/10/2026")

    @Test
    fun `buyer, bank account, currency and lines are copied`() {
        val source = invoice(
            account = santander,
            lines = listOf(
                DraftLine("Usługa testowa", BigDecimal("2.5"), "godz.", BigDecimal("120"), VatRate.RATE_23),
                DraftLine("Licencja testowa", BigDecimal("1"), "szt.", BigDecimal("12.5"), VatRate.RATE_8),
            ),
        )

        val similar = SimilarInvoice.of(source, profile, today, taken)

        assertNull(similar.newCounterparty)
        assertEquals("6691034274", similar.form.counterpartyId)
        assertEquals(santander.number, similar.form.bankAccountNumber)
        assertEquals(InvoiceCurrency.PLN, similar.form.currency)
        assertEquals(
            listOf(LineForm("Usługa testowa", "2,5", "godz.", "120,00", VatRate.RATE_23), LineForm("Licencja testowa", "1", "szt.", "12,50", VatRate.RATE_8)),
            similar.form.lines,
        )
    }

    @Test
    fun `dates and number are suggested as for a new invoice, not copied`() {
        val source = invoice(number = "01/09/2026", issueDate = "2026-10-01", deliveryDate = "2026-09-30")

        val form = SimilarInvoice.of(source, profile, today, taken).form

        // Day 16: delivery today, the next number of October.
        assertEquals(today, form.issueDate)
        assertEquals(today, form.deliveryDate)
        assertEquals("02/10/2026", form.invoiceNumber)
        assertFalse(form.datesEdited)
        assertFalse(form.numberEdited)
    }

    @Test
    fun `on day 1 to 5 the delivery date is the last day of the previous month`() {
        val form = SimilarInvoice.of(invoice(), profile, LocalDate.parse("2026-10-05"), taken).form

        assertEquals(LocalDate.parse("2026-10-05"), form.issueDate)
        assertEquals(LocalDate.parse("2026-09-30"), form.deliveryDate)
        assertEquals("03/09/2026", form.invoiceNumber)

        // Day 6: already the current month.
        val laterForm = SimilarInvoice.of(invoice(), profile, LocalDate.parse("2026-10-06"), taken).form
        assertEquals(LocalDate.parse("2026-10-06"), laterForm.deliveryDate)
        assertEquals("02/10/2026", laterForm.invoiceNumber)
    }

    @Test
    fun `dates and number come from the same logic as a new invoice`() {
        listOf("2026-10-01", "2026-10-05", "2026-10-06", "2026-10-16", "2026-12-31", "2027-01-05", "2027-01-06").map(LocalDate::parse).forEach { day ->
            val similar = SimilarInvoice.of(invoice(), profile, day, taken).form
            val new = InvoiceFormDefaults.newForm(profile, day, taken)

            assertEquals(new.issueDate, similar.issueDate, "issue date on $day")
            assertEquals(new.deliveryDate, similar.deliveryDate, "delivery date on $day")
            assertEquals(new.invoiceNumber, similar.invoiceNumber, "number on $day")
        }
    }

    @Test
    fun `the copy opened as a draft on the same day stays as it is`() {
        val form = SimilarInvoice.of(invoice(), profile, today, taken).form

        assertEquals(form, InvoiceFormDefaults.refresh(form, today, taken))
    }

    @Test
    fun `the exchange rate is not copied - it is fetched for the new dates`() {
        // Source issued 12.10 with the rate of 09.10 (docs/przyklady/kursy-nbp.md).
        val rate = ExchangeRate(InvoiceCurrency.EUR, BigDecimal("4.3775"), "197/A/NBP/2026", LocalDate.parse("2026-10-09"))
        val source = invoice(
            number = "01/10/2026", issueDate = "2026-10-12", deliveryDate = "2026-10-12",
            buyer = germanBuyer, account = euroAccount, currency = InvoiceCurrency.EUR, rate = rate,
            lines = listOf(DraftLine("Usługa testowa", BigDecimal.ONE, "dni", BigDecimal("500"), VatRate.NOT_TAXABLE)),
        )
        assertEquals(BigDecimal("4.3775"), source.exchangeRate)

        val form = SimilarInvoice.of(source, profile, today, taken).form

        assertEquals(InvoiceCurrency.EUR, form.currency)
        // No rate in the form: the form fetches the working rate exactly as for a new invoice (ADR 0008).
        assertNull(form.exchangeRate)
        assertNull(form.workingRate)
        // ... for the new dates: the earlier of delivery and issue date — today (16.10), not the source's 12.10.
        assertEquals(today, form.rateReferenceDate)
        assertEquals(InvoiceFormDefaults.newForm(profile, today, taken).rateReferenceDate, form.rateReferenceDate)
        // Until the rate is fetched the invoice is blocked: nothing of the source's rate can reach the XML.
        val check = InvoiceFormCheck.check(form, profile, taken, today)
        assertNull(check.draft?.effectiveExchangeRate)
        assertTrue(check.draftIssues.any { it.type == DraftIssueType.EXCHANGE_RATE_MISSING && it.blocking })
        assertEquals(germanBuyer.id, form.counterpartyId)
        assertEquals(euroAccount.number, form.bankAccountNumber)
        assertEquals(VatRate.NOT_TAXABLE, form.lines.single().vatRate)
    }

    @Test
    fun `a Polish buyer missing among the counterparties is added from the invoice`() {
        val other = Counterparty("Testowy Dolor Sp. j.", "9579102904", Address("ul. Testowa 8", "00-004 Testowo", "Polska"), "biuro@example.com")

        val similar = SimilarInvoice.of(invoice(buyer = other), profile, today, taken)

        assertEquals(other.copy(address = other.address.copy(country = "Polska")), similar.newCounterparty)
        assertEquals("9579102904", similar.form.counterpartyId)
    }

    @Test
    fun `a foreign buyer missing among the counterparties is added - VAT UE with the EL prefix`() {
        val similar = SimilarInvoice.of(invoice(buyer = greekBuyer), profile, today, taken)

        val added = requireNotNull(similar.newCounterparty)
        assertEquals("GR", added.countryCode)
        assertEquals("000000000", added.taxNumber)
        assertEquals("Dolor Testowy A.E.", added.name)
        assertEquals(Address("Odos Test 1", "00000 Testopoli", "Grecja"), added.address)
        assertEquals(added.id, similar.form.counterpartyId)
    }

    @Test
    fun `a buyer from outside the EU keeps its identifier and gets np`() {
        val source = invoice(buyer = americanBuyer, lines = listOf(DraftLine("Usługa testowa", BigDecimal.ONE, "usł.", BigDecimal("100"), VatRate.NOT_TAXABLE)))

        val similar = SimilarInvoice.of(source, profile, today, taken)

        assertEquals("00-0000000", similar.newCounterparty?.taxNumber)
        assertEquals("US", similar.newCounterparty?.countryCode)
        assertEquals(VatRate.NOT_TAXABLE, similar.form.lines.single().vatRate)
    }

    @Test
    fun `a buyer without an identifier is found by country and name`() {
        val similar = SimilarInvoice.of(invoice(buyer = britishBuyer), profile, today, taken)

        assertNull(similar.newCounterparty)
        assertEquals(britishBuyer.id, similar.form.counterpartyId)
    }

    @Test
    fun `an account no longer among mine is replaced by the default one of the currency`() {
        val closed = BankAccount("Konto zamknięte", "NBP", "71101075753723914707418989")

        val form = SimilarInvoice.of(invoice(account = closed), profile, today, taken).form

        assertEquals(toyota.number, form.bankAccountNumber)
    }

    @Test
    fun `only ordinary VAT invoices can be copied`() {
        val source = invoice()

        assertTrue(SimilarInvoice.canCopy(source))
        assertFalse(SimilarInvoice.canCopy(source.copy(kind = "KOR")))
        assertFalse(SimilarInvoice.canCopy(source.copy(kind = "ZAL")))
    }

    @Test
    fun `a buyer without an address is left to choose in the form`() {
        val source = invoice().let { it.copy(buyer = it.buyer.copy(addressLines = emptyList())) }

        val similar = SimilarInvoice.of(source, profile.copy(counterparties = emptyList()), today, taken)

        assertNull(similar.newCounterparty)
        assertNull(similar.form.counterpartyId)
    }

    private fun invoice(
        number: String = "01/09/2026",
        issueDate: String = "2026-10-01",
        deliveryDate: String = "2026-09-30",
        buyer: Counterparty = this.buyer,
        account: BankAccount = toyota,
        currency: InvoiceCurrency = InvoiceCurrency.PLN,
        rate: ExchangeRate? = null,
        lines: List<DraftLine> = listOf(DraftLine("Usługa testowa", BigDecimal.ONE, "szt.", BigDecimal("233"), VatRate.RATE_23)),
    ): Invoice {
        val draft = SalesInvoiceDraft(number, LocalDate.parse(issueDate), LocalDate.parse(deliveryDate), seller, buyer, account, lines, currency, rate)
        return InvoiceXmlParser.parse(Fa3XmlWriter.write(draft, Instant.parse("2026-10-01T10:00:00Z")))
    }
}
