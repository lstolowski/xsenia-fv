package pl.xsenia.fv.invoice.config

/**
 * Snapshot of the three configuration files, loaded at every app start (ADR 0004).
 *
 * Invoices can be browsed regardless of configuration problems; issuing a new invoice requires
 * a valid `company.json` and `banks.json` and at least one counterparty and one PLN account.
 */
class InvoicingConfig(
    val company: Company?,
    val bankAccounts: List<BankAccount>,
    val counterparties: List<Counterparty>,
    val issues: List<ConfigIssue>,
) {

    /** Accounts offered on a new invoice — invoices are issued in PLN only. */
    val invoiceBankAccounts: List<BankAccount>
        get() = bankAccounts.filter { it.currency == INVOICE_CURRENCY }

    val canIssueInvoices: Boolean
        get() = company != null &&
            invoiceBankAccounts.isNotEmpty() &&
            counterparties.isNotEmpty() &&
            issues.none { it.file == ConfigFile.COMPANY || it.file == ConfigFile.BANKS }

    companion object {
        const val INVOICE_CURRENCY = "PLN"

        /** Each argument is the file content, or null when the file does not exist. */
        fun parse(companyJson: String?, banksJson: String?, counterpartiesJson: String?): InvoicingConfig {
            val company = ConfigParser.parseCompany(companyJson)
            val banks = ConfigParser.parseBanks(banksJson)
            val counterparties = ConfigParser.parseCounterparties(counterpartiesJson)
            return InvoicingConfig(
                company = company.value,
                bankAccounts = banks.value,
                counterparties = counterparties.value,
                issues = company.issues + banks.issues + counterparties.issues,
            )
        }
    }
}
