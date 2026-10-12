package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.config.Address
import pl.xsenia.fv.invoice.config.BankAccount
import pl.xsenia.fv.invoice.config.Company
import pl.xsenia.fv.invoice.config.Counterparty
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

    fun line(name: String = "Usługa testowa", quantity: String = "1", unit: String = "szt.", price: String = "233") =
        DraftLine(name, BigDecimal(quantity), unit, BigDecimal(price))

    fun draft(
        number: String = "01/09/2026",
        issueDate: String = "2026-10-01",
        deliveryDate: String = "2026-09-30",
        lines: List<DraftLine> = listOf(line()),
        buyer: Counterparty = this.buyer,
        seller: Company = this.seller,
    ) = SalesInvoiceDraft(number, LocalDate.parse(issueDate), LocalDate.parse(deliveryDate), seller, buyer, account, lines)
}
