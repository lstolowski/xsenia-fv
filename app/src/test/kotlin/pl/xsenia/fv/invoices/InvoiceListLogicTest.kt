package pl.xsenia.fv.invoices

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.profile.TaxIdType
import pl.xsenia.fv.ksef.invoices.InvoiceMetadata
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.MirrorEntry
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class InvoiceListLogicTest {

    private fun row(
        number: String,
        delivery: String,
        name: String = "Testowa Sp. z o.o.",
        nip: String = "6691034274",
        taxId: CounterpartyTaxId = CounterpartyTaxId(TaxIdType.NIP, nip),
        net: String = "100.00",
        gross: String = "123.00",
        currency: String = "PLN",
        issue: String = delivery,
    ) = InvoiceRow(
        ksefNumber = "K-$number",
        invoiceNumber = number,
        deliveryDate = LocalDate.parse(delivery),
        issueDate = LocalDate.parse(issue),
        counterpartyName = name,
        counterpartyTaxId = taxId,
        netAmount = BigDecimal(net),
        grossAmount = BigDecimal(gross),
        currency = currency,
        invoiceType = "Vat",
    )

    @Test
    fun `filters by delivery month, not issue month, newest first`() {
        val rows = listOf(
            row("01/09/2026", delivery = "2026-09-30", issue = "2026-10-01"),
            row("02/09/2026", delivery = "2026-09-15"),
            row("01/10/2026", delivery = "2026-10-05"),
        )

        val september = InvoiceListLogic.filter(rows, YearMonth.of(2026, 9), "")

        assertEquals(listOf("01/09/2026", "02/09/2026"), september.map { it.invoiceNumber })
    }

    @Test
    fun `searches name fragment case-insensitively and NIP ignoring separators`() {
        val testowa = row("A", "2026-09-01", name = "Testowa Sp. z o.o.", nip = "6691034274")
        val lorem = row("B", "2026-09-02", name = "Lorem Ipsum S.A.", nip = "9945082500")

        assertTrue(InvoiceListLogic.matches(testowa, "testowa"))
        assertTrue(InvoiceListLogic.matches(lorem, "IPSUM"))
        assertTrue(InvoiceListLogic.matches(testowa, "669-103"))
        assertTrue(InvoiceListLogic.matches(testowa, " 669 103 42 74 "))
        assertFalse(InvoiceListLogic.matches(lorem, "669"))
        assertTrue(InvoiceListLogic.matches(lorem, "  "))
        assertEquals(listOf("B"), InvoiceListLogic.filter(listOf(testowa, lorem), YearMonth.of(2026, 9), "9945").map { it.invoiceNumber })
    }

    @Test
    fun `totals are computed per currency with PLN first`() {
        val totals = InvoiceListLogic.totals(
            listOf(
                row("C", "2026-09-03", net = "10.00", gross = "12.30", currency = "EUR"),
                row("A", "2026-09-01", net = "100.00", gross = "123.00"),
                row("B", "2026-09-02", net = "200.10", gross = "246.12"),
            ),
        )

        assertEquals(
            listOf(
                CurrencyTotals("PLN", 2, BigDecimal("300.10"), BigDecimal("69.02"), BigDecimal("369.12")),
                CurrencyTotals("EUR", 1, BigDecimal("10.00"), BigDecimal("2.30"), BigDecimal("12.30")),
            ),
            totals,
        )
    }

    @Test
    fun `months cover the range up to now plus months with invoices, newest first`() {
        val rows = listOf(row("A", "2026-03-31"), row("B", "2026-09-30"), row("C", "2026-09-01"))

        val months = InvoiceListLogic.months(rows, YearMonth.of(2026, 4), YearMonth.of(2026, 10))

        assertEquals(YearMonth.of(2026, 10) to 0, months.first())
        assertEquals(YearMonth.of(2026, 9) to 2, months[1])
        assertEquals(YearMonth.of(2026, 3) to 1, months.last())
        assertEquals(8, months.size)
    }

    private fun metadata(buyer: InvoiceMetadata.BuyerIdentifier) = InvoiceMetadata(
        ksefNumber = "K", invoiceNumber = "01/09/2026", issueDate = LocalDate.parse("2026-10-01"),
        invoicingDate = Instant.EPOCH, acquisitionDate = Instant.EPOCH, permanentStorageDate = Instant.EPOCH,
        seller = InvoiceMetadata.Seller("9945082500", "Jan Testowy"),
        buyer = InvoiceMetadata.Buyer(buyer, "Lorem Testowy"),
        netAmount = BigDecimal("500"), grossAmount = BigDecimal("500"), vatAmount = BigDecimal.ZERO,
        currency = "EUR", invoicingMode = "Online", invoiceType = "Vat",
        formCode = InvoiceMetadata.FormCode("FA (3)", "1-0E", "FA"), isSelfInvoicing = false, hasAttachment = false, invoiceHash = "x",
    )

    // As KSeF TEST listed the invoices of ZAD-031 (2026-10-14), fictitious buyers.
    @Test
    fun `foreign buyer identifiers from KSeF metadata`() {
        fun taxId(type: String, value: String?) =
            InvoiceListLogic.toRow(InvoiceCategory.SALES, MirrorEntry(metadata(InvoiceMetadata.BuyerIdentifier(type, value)), LocalDate.parse("2026-10-14"))).counterpartyTaxId

        assertEquals(CounterpartyTaxId(TaxIdType.VAT_UE, "DE999999990"), taxId("VatUe", "DE999999990"))
        assertEquals(CounterpartyTaxId(TaxIdType.OTHER, "US00-0000000"), taxId("Other", "US00-0000000"))
        assertEquals(null, taxId("None", null))
        assertEquals(null, taxId("Unknown", "X"))
        val purchase = InvoiceListLogic.toRow(InvoiceCategory.PURCHASE, MirrorEntry(metadata(InvoiceMetadata.BuyerIdentifier("VatUe", "DE999999990")), LocalDate.parse("2026-10-14")))
        assertEquals(CounterpartyTaxId(TaxIdType.NIP, "9945082500"), purchase.counterpartyTaxId)
    }

    @Test
    fun `searches VAT UE numbers and foreign identifiers ignoring prefix and separators`() {
        val german = row("A", "2026-10-01", name = "Lorem Testowy GmbH", taxId = CounterpartyTaxId(TaxIdType.VAT_UE, "DE999999990"))
        val american = row("B", "2026-10-02", name = "Lorem Test Inc.", taxId = CounterpartyTaxId(TaxIdType.OTHER, "US00-0000000"))

        assertTrue(InvoiceListLogic.matches(german, "999 999 990"))
        assertTrue(InvoiceListLogic.matches(german, "de999999990"))
        assertTrue(InvoiceListLogic.matches(american, "00-0000000"))
        assertTrue(InvoiceListLogic.matches(american, "us 000"))
        assertFalse(InvoiceListLogic.matches(german, "US"))
    }

    @Test
    fun `unsent invoice shows the identifier of its foreign buyer`() {
        val german = Counterparty("Lorem Testowy GmbH", address = Address("Teststraße 1"), countryCode = "DE", taxNumber = "999999990")
        val american = Counterparty("Lorem Test Inc.", address = Address("1 Test Street"), countryCode = "US", taxNumber = "00-0000000")
        val british = Counterparty("Ipsum Test Ltd", address = Address("1 Test Lane"), countryCode = "GB")
        val profile = CompanyProfile("9945082500", counterparties = listOf(german, american, british))
        fun rowFor(buyer: Counterparty) = InvoiceListLogic.toRow(
            pl.xsenia.fv.issue.UnsentInvoice.create(
                pl.xsenia.fv.issue.InvoiceForm(counterpartyId = buyer.id, issueDate = LocalDate.parse("2026-10-14"), deliveryDate = LocalDate.parse("2026-10-14")),
                Instant.EPOCH,
            ),
            profile,
        )

        assertEquals(CounterpartyTaxId(TaxIdType.VAT_UE, "DE999999990"), rowFor(german).counterpartyTaxId)
        assertEquals(CounterpartyTaxId(TaxIdType.OTHER, "US00-0000000"), rowFor(american).counterpartyTaxId)
        assertEquals(null, rowFor(british).counterpartyTaxId)
        assertEquals("Ipsum Test Ltd", rowFor(british).counterpartyName)
    }

    @Test
    fun `counterparty is the buyer on sales and the seller on purchase invoices`() {
        val metadata = InvoiceMetadata(
            ksefNumber = "K", invoiceNumber = "01/09/2026", issueDate = LocalDate.parse("2026-10-01"),
            invoicingDate = Instant.EPOCH, acquisitionDate = Instant.EPOCH, permanentStorageDate = Instant.EPOCH,
            seller = InvoiceMetadata.Seller("9945082500", "Jan Testowy"),
            buyer = InvoiceMetadata.Buyer(InvoiceMetadata.BuyerIdentifier("Nip", "6691034274"), "Testowa Sp. z o.o."),
            netAmount = BigDecimal("233"), grossAmount = BigDecimal("286.59"), vatAmount = BigDecimal("53.59"),
            currency = "PLN", invoicingMode = "Online", invoiceType = "Vat",
            formCode = InvoiceMetadata.FormCode("FA (3)", "1-0E", "FA"), isSelfInvoicing = false, hasAttachment = false, invoiceHash = "x",
        )
        val entry = MirrorEntry(metadata, LocalDate.parse("2026-09-30"))

        assertEquals(CounterpartyTaxId(TaxIdType.NIP, "6691034274"), InvoiceListLogic.toRow(InvoiceCategory.SALES, entry).counterpartyTaxId)
        assertEquals("Jan Testowy", InvoiceListLogic.toRow(InvoiceCategory.PURCHASE, entry).counterpartyName)
        assertEquals(LocalDate.parse("2026-09-30"), InvoiceListLogic.toRow(InvoiceCategory.SALES, entry).deliveryDate)
    }

    @Test
    fun `unsent invoices are listed by delivery month but excluded from totals`() {
        val unsent = pl.xsenia.fv.issue.UnsentInvoice.create(
            pl.xsenia.fv.issue.InvoiceForm(
                counterpartyId = "6691034274",
                issueDate = LocalDate.parse("2026-10-01"),
                deliveryDate = LocalDate.parse("2026-09-30"),
                invoiceNumber = "04/09/2026",
                lines = listOf(pl.xsenia.fv.issue.LineForm("Usługa testowa", "2", "szt.", "1 000,00"), pl.xsenia.fv.issue.LineForm("Błędna", "x", "szt.", "1")),
            ),
            Instant.EPOCH,
        )
        val unsentRow = InvoiceListLogic.toRow(unsent, profile = null)
        val rows = listOf(row("01/09/2026", "2026-09-15"), unsentRow)

        val september = InvoiceListLogic.filter(rows, YearMonth.of(2026, 9), "")

        assertEquals(listOf("04/09/2026", "01/09/2026"), september.map { it.invoiceNumber })
        assertTrue(unsentRow.isUnsent)
        assertEquals(BigDecimal("2000.00"), unsentRow.netAmount)
        assertEquals(BigDecimal("2460.00"), unsentRow.grossAmount)
        assertEquals(listOf(CurrencyTotals("PLN", 1, BigDecimal("100.00"), BigDecimal("23.00"), BigDecimal("123.00"))), InvoiceListLogic.totals(september))
        assertEquals(UnsentSummary(1, BigDecimal("2460.00")), InvoiceListLogic.unsentSummary(september))
        assertEquals(null, InvoiceListLogic.unsentSummary(listOf(row("01/09/2026", "2026-09-15"))))
    }

    @Test
    fun `unsent invoice keeps its currency`() {
        val unsent = pl.xsenia.fv.issue.UnsentInvoice.create(
            pl.xsenia.fv.issue.InvoiceForm(
                issueDate = LocalDate.parse("2026-10-01"),
                deliveryDate = LocalDate.parse("2026-09-30"),
                lines = listOf(pl.xsenia.fv.issue.LineForm("Usługa testowa", "10", "godz.", "100")),
                currency = pl.xsenia.fv.invoice.currency.InvoiceCurrency.EUR,
            ),
            Instant.EPOCH,
        )

        val row = InvoiceListLogic.toRow(unsent, profile = null)

        assertEquals("EUR", row.currency)
        assertEquals(BigDecimal("1230.00"), row.grossAmount)
    }
}
