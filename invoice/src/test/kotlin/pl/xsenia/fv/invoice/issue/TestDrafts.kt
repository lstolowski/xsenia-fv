package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import java.math.BigDecimal
import java.time.LocalDate

/** Fictitious test data only (AGENTS.md). */
object TestDrafts {
    val seller = Company(
        name = "Jan Testowy Usługi Testowe",
        nip = "9945082500",
        address = Address("ul. Testowa 1", "00-001 Testowo", "Polska"),
        email = "jan.testowy@example.com",
        phone = "+48000000000",
    )
    val buyer = Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", "00-002 Testowo", "Polska"), "faktury@example.com")
    val account = BankAccount("Konto testowe Toyota", "Toyota Bank", "85216097583216850246002152", "PLN")
    val euroAccount = BankAccount("Konto testowe EUR", "Toyota Bank", "83216097587877893287921742", "EUR")

    /** docs/przyklady/kursy-nbp.md */
    fun rate(currency: InvoiceCurrency = InvoiceCurrency.EUR, rate: String = "4.3775") =
        ExchangeRate(currency, BigDecimal(rate), "197/A/NBP/2026", LocalDate.parse("2026-10-09"))

    fun line(name: String = "Usługa testowa", quantity: String = "1", unit: String = "szt.", price: String = "233", rate: VatRate = VatRate.RATE_23) =
        DraftLine(name, BigDecimal(quantity), unit, BigDecimal(price), rate)

    fun draft(
        number: String = "01/09/2026",
        issueDate: String = "2026-10-01",
        deliveryDate: String = "2026-09-30",
        lines: List<DraftLine> = listOf(line()),
        buyer: Counterparty = this.buyer,
        seller: Company = this.seller,
        account: BankAccount = this.account,
        currency: InvoiceCurrency = InvoiceCurrency.PLN,
        exchangeRate: ExchangeRate? = null,
    ) = SalesInvoiceDraft(number, LocalDate.parse(issueDate), LocalDate.parse(deliveryDate), seller, buyer, account, lines, currency, exchangeRate)
}
