package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.profile.Address
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Writes a sales invoice as FA(3) XML (`schemat_FA(3)_v1-0E.xsd`).
 *
 * Fixed content (decisions C1–C14, R4–R10): VAT invoice, Polish buyer with NIP, bank transfer due in 21 days,
 * no split payment, all annotations "no" except reverse charge for "np" lines. VAT rate per line (ADR 0005).
 * A foreign currency invoice (ADR 0008) adds VAT in PLN (`P_14_xW`), the NBP rate in every line (`KursWaluty`)
 * and the remark "Kurs waluty" with the NBP table; a PLN invoice has none of them.
 * The XML is built as text (not via javax.xml.transform) so the bytes — and thus the hash sent to KSeF —
 * are identical on the JVM and on Android.
 */
object Fa3XmlWriter {

    const val NAMESPACE = "http://crd.gov.pl/wzor/2025/06/25/13775/"
    const val SYSTEM_CODE = "FA (3)"
    const val SCHEMA_VERSION = "1-0E"
    const val FORM_CODE = "FA"
    private const val SYSTEM_INFO = "xsenia-fv"
    private const val COUNTRY_CODE = "PL"
    private const val PAYMENT_TRANSFER = "6"

    // Remark on invoices with "np" lines (ADR 0005).
    const val REMARKS_KEY = "Uwagi"
    const val REVERSE_CHARGE_REMARK = "Reverse charge"

    // Remark on foreign currency invoices (ADR 0008, decision Q5).
    const val EXCHANGE_RATE_KEY = "Kurs waluty"

    // FA(3) annotation values: 1 — yes, 2 — no (for the "N" elements: 1 — the case does not apply).
    private const val YES = "1"
    private const val NO = "2"
    private const val NOT_APPLICABLE = "1"

    fun write(draft: SalesInvoiceDraft, createdAt: Instant): ByteArray {
        val exchangeRate = draft.effectiveExchangeRate
        require(!draft.currency.isForeign || exchangeRate != null) { "A ${draft.currency} invoice needs an exchange rate of that currency" }
        val totals = InvoiceCalculator.totals(draft)
        val xml = XmlBuilder()
        xml.element("Faktura", "xmlns" to NAMESPACE) {
            element("Naglowek") {
                element("KodFormularza", "kodSystemowy" to SYSTEM_CODE, "wersjaSchemy" to SCHEMA_VERSION) { text(FORM_CODE) }
                value("WariantFormularza", "3")
                value("DataWytworzeniaFa", createdAt.truncatedTo(ChronoUnit.SECONDS).toString())
                value("SystemInfo", SYSTEM_INFO)
            }
            element("Podmiot1") {
                element("DaneIdentyfikacyjne") {
                    value("NIP", draft.seller.nip)
                    value("Nazwa", draft.seller.name)
                }
                address(draft.seller.address)
                if (draft.seller.email != null || draft.seller.phone != null) {
                    element("DaneKontaktowe") {
                        draft.seller.email?.let { value("Email", it) }
                        draft.seller.phone?.let { value("Telefon", it) }
                    }
                }
            }
            element("Podmiot2") {
                element("DaneIdentyfikacyjne") {
                    value("NIP", draft.buyer.nip)
                    value("Nazwa", draft.buyer.name)
                }
                address(draft.buyer.address)
                draft.buyer.email?.let { email -> element("DaneKontaktowe") { value("Email", email) } }
                // Not a local government unit, not a VAT group member.
                value("JST", NO)
                value("GV", NO)
            }
            element("Fa") {
                value("KodWaluty", draft.currency.code)
                value("P_1", draft.issueDate.toString())
                value("P_2", draft.invoiceNumber)
                // P_6 only when the delivery date differs from the issue date.
                if (draft.deliveryDate != draft.issueDate) value("P_6", draft.deliveryDate.toString())
                totals.byRate.forEach { rateSummary(it) }
                value("P_15", amount(totals.gross))
                val reverseCharge = draft.lines.any { it.vatRate == VatRate.NOT_TAXABLE }
                element("Adnotacje") {
                    value("P_16", NO)
                    value("P_17", NO)
                    value("P_18", if (reverseCharge) YES else NO)
                    value("P_18A", NO)
                    element("Zwolnienie") { value("P_19N", NOT_APPLICABLE) }
                    element("NoweSrodkiTransportu") { value("P_22N", NOT_APPLICABLE) }
                    value("P_23", NO)
                    element("PMarzy") { value("P_PMarzyN", NOT_APPLICABLE) }
                }
                value("RodzajFaktury", "VAT")
                if (reverseCharge) remark(REMARKS_KEY, REVERSE_CHARGE_REMARK)
                exchangeRate?.let { remark(EXCHANGE_RATE_KEY, exchangeRateRemark(it)) }
                draft.lines.forEachIndexed { index, line ->
                    element("FaWiersz") {
                        value("NrWierszaFa", (index + 1).toString())
                        value("P_7", line.name)
                        value("P_8A", line.unit)
                        value("P_8B", decimal(line.quantity))
                        value("P_9A", decimal(line.unitNetPrice))
                        value("P_11", amount(InvoiceCalculator.lineNet(line)))
                        value("P_12", line.vatRate.code)
                        exchangeRate?.let { value("KursWaluty", it.rate.toPlainString()) }
                    }
                }
                element("Platnosc") {
                    element("TerminPlatnosci") { value("Termin", InvoiceCalculator.paymentDueDate(draft.issueDate).toString()) }
                    value("FormaPlatnosci", PAYMENT_TRANSFER)
                    element("RachunekBankowy") {
                        value("NrRB", draft.bankAccount.number)
                        draft.bankAccount.bank?.let { value("NazwaBanku", it) }
                    }
                }
            }
        }
        return xml.toBytes()
    }

    /** Summary fields of one rate; [VatRate] order matches the FA(3) element order. */
    private fun XmlBuilder.rateSummary(totals: RateTotals) {
        when (totals.rate) {
            VatRate.RATE_23 -> netAndVat("P_13_1", "P_14_1", totals)
            VatRate.RATE_8 -> netAndVat("P_13_2", "P_14_2", totals)
            VatRate.RATE_5 -> netAndVat("P_13_3", "P_14_3", totals)
            VatRate.RATE_0 -> value("P_13_6_1", amount(totals.net))
            VatRate.NOT_TAXABLE -> value("P_13_9", amount(totals.net))
        }
    }

    private fun XmlBuilder.netAndVat(netField: String, vatField: String, totals: RateTotals) {
        value(netField, amount(totals.net))
        value(vatField, amount(totals.vat))
        totals.vatPln?.let { value(vatField + "W", amount(it)) }
    }

    private fun XmlBuilder.remark(key: String, text: String) = element("DodatkowyOpis") {
        value("Klucz", key)
        value("Wartosc", text)
    }

    /** E.g. "1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09". */
    fun exchangeRateRemark(rate: ExchangeRate): String =
        "1 ${rate.currency.code} = ${rate.rate.toPlainString().replace('.', ',')} PLN, tabela NBP ${rate.tableNumber} z ${rate.effectiveDate}"

    private fun XmlBuilder.address(address: Address) = element("Adres") {
        value("KodKraju", COUNTRY_CODE)
        value("AdresL1", address.line1)
        address.line2?.let { value("AdresL2", it) }
    }

    /** Money: exactly two decimals (`TKwotowy`). */
    private fun amount(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).toPlainString()

    /** Quantity / unit price without trailing zeros (`TIlosci`, `TKwotowy2`). */
    private fun decimal(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
}

/** Minimal XML writer producing UTF-8 with escaped text; values are trimmed and whitespace collapsed (xsd:token). */
private class XmlBuilder {
    private val out = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    private var depth = 0

    fun element(name: String, vararg attributes: Pair<String, String>, content: XmlBuilder.() -> Unit) {
        indent()
        out.append('<').append(name)
        attributes.forEach { (key, value) -> out.append(' ').append(key).append("=\"").append(escape(value)).append('"') }
        out.append('>')
        val start = out.length
        depth++
        content()
        depth--
        if (out.length > start && out[out.length - 1] == '\n') indent()
        out.append("</").append(name).append(">\n")
    }

    fun value(name: String, value: String) {
        indent()
        out.append('<').append(name).append('>').append(escape(normalize(value))).append("</").append(name).append(">\n")
    }

    fun text(value: String) {
        out.append(escape(normalize(value)))
    }

    fun toBytes(): ByteArray = out.toString().toByteArray(Charsets.UTF_8)

    private fun indent() {
        if (out.isNotEmpty() && out[out.length - 1] != '\n') out.append('\n')
        repeat(depth) { out.append("  ") }
    }

    private fun normalize(value: String) = value.trim().replace(WHITESPACE, " ")

    private fun escape(value: String) = buildString {
        value.forEach {
            when (it) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(it)
            }
        }
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
