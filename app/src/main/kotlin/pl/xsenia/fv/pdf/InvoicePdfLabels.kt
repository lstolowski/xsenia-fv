package pl.xsenia.fv.pdf

import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.model.VatCategory
import pl.xsenia.fv.invoice.profile.Countries
import java.util.Locale

/**
 * Texts of the invoice PDF (ADR 0010): Polish, or "Polish / English" for a foreign buyer. They are part of the
 * document, not of the app UI, so they live here and not in `strings.xml`.
 */
class InvoicePdfLabels private constructor(val bilingual: Boolean) {

    /** "pl" or "pl / en" (once when both are the same, e.g. "Bank"). */
    private fun t(pl: String, en: String) = if (bilingual && pl != en) "$pl / $en" else pl

    /** Table header cell: Polish over English. */
    private fun cell(pl: String, en: String) = if (bilingual && pl != en) listOf(pl, en) else listOf(pl)

    fun title(kind: String): String = when (kind.uppercase()) {
        "VAT" -> t("Faktura VAT", "VAT invoice")
        "KOR" -> t("Faktura korygująca", "Corrective invoice")
        "ZAL" -> t("Faktura zaliczkowa", "Advance invoice")
        "ROZ" -> t("Faktura rozliczeniowa", "Settlement invoice")
        else -> t("Faktura", "Invoice")
    }

    fun number(number: String) = "${t("nr", "No.")} $number"
    fun ksefNumber(number: String) = "${t("Numer KSeF", "KSeF number")}: $number"
    val notInKsef = t("Faktura nie wysłana do KSeF", "Not yet submitted to KSeF")
    fun issueDate(date: String) = "${t("Data wystawienia", "Issue date")}: $date"
    fun deliveryDate(date: String) = "${t("Data dostawy / wykonania usługi", "Date of supply")}: $date"
    fun period(from: String, to: String) = "${t("Okres", "Period")}: $from – $to"
    fun currency(code: String) = "${t("Waluta", "Currency")}: $code"

    val seller = t("Sprzedawca", "Seller")
    val buyer = t("Nabywca", "Buyer")
    fun nip(value: String) = "NIP: $value"
    fun euVat(value: String) = "${t("Numer VAT UE", "EU VAT number")}: $value"
    fun otherId(value: String) = "${t("Identyfikator podatkowy", "Tax ID")}: $value"

    /** Polish name of the country; with the English one for a foreign buyer, e.g. „Niemcy / Germany”. */
    fun country(code: String): String {
        val polish = Countries.name(code)
        val english = Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH).ifEmpty { code }
        return if (bilingual && english != polish) "$polish / $english" else polish
    }

    val linesHeading = t("Pozycje", "Items")
    val columnNumber = cell("Lp.", "No.")
    val columnName = cell("Nazwa towaru lub usługi", "Description")
    val columnQuantity = cell("Ilość", "Qty")
    val columnUnit = cell("J.m.", "Unit")
    val columnPrice = cell("Cena netto", "Net price")
    val columnRate = cell("VAT", "VAT")
    val columnNet = cell("Wartość netto", "Net amount")
    val columnGross = cell("Wartość brutto", "Gross amount")

    val summaryHeading = t("Podsumowanie", "Summary")
    fun summaryRate(rate: String, net: String, vat: String?) =
        if (vat == null) "$rate: ${t("netto", "net")} $net" else "$rate: ${t("netto", "net")} $net · VAT $vat"
    val totalNet = t("Razem netto", "Total net")
    val totalVat = t("Razem VAT", "Total VAT")
    val totalGross = t("Do zapłaty (brutto)", "Amount due (gross)")
    fun exchangeRate(rate: String) = "${t("Kurs waluty", "Exchange rate")}: $rate"

    fun vatCategory(category: VatCategory): String = when (category) {
        VatCategory.STANDARD -> t("Stawka 23%", "Rate 23%")
        VatCategory.REDUCED_8 -> t("Stawka 8%", "Rate 8%")
        VatCategory.REDUCED_5 -> t("Stawka 5%", "Rate 5%")
        VatCategory.TAXI_FLAT_RATE -> t("Taxi (ryczałt)", "Taxi flat rate")
        VatCategory.OSS -> t("Procedura OSS", "OSS procedure")
        VatCategory.ZERO_DOMESTIC -> t("0% (kraj)", "0% domestic")
        VatCategory.ZERO_INTRA_EU_SUPPLY -> t("0% (WDT)", "0% intra-EU supply")
        VatCategory.ZERO_EXPORT -> t("0% (eksport)", "0% export")
        VatCategory.EXEMPT -> t("Zwolnione", "Exempt")
        VatCategory.NOT_TAXABLE_ABROAD -> t("Niepodlegające (poza krajem)", "Not taxable in Poland")
        VatCategory.NOT_TAXABLE_ART_100 -> t("Niepodlegające (art. 100)", "Not taxable in Poland")
        VatCategory.REVERSE_CHARGE -> t("Odwrotne obciążenie", "Reverse charge")
        VatCategory.MARGIN -> t("Procedura marży", "Margin scheme")
    }

    /** `P_12` of a line as printed: `23%`, `0%`, `np`, `zw`. */
    fun lineRate(code: String?): String = when {
        code == null -> ""
        code.startsWith("np") -> "np"
        code.substringBefore(' ').toBigDecimalOrNull() != null -> code.substringBefore(' ') + "%"
        else -> code
    }

    val paymentHeading = t("Płatność", "Payment")
    fun paymentMethod(code: Int?, other: String?): String? {
        val name = when (code) {
            1 -> t("gotówka", "cash")
            2 -> t("karta", "card")
            3 -> t("bon", "voucher")
            4 -> t("czek", "cheque")
            5 -> t("kredyt", "credit")
            6 -> t("przelew", "bank transfer")
            7 -> t("płatność mobilna", "mobile payment")
            else -> other
        } ?: return null
        return "${t("Forma płatności", "Payment method")}: $name"
    }
    fun dueDate(date: String) = "${t("Termin płatności", "Due date")}: $date"
    fun bankAccount(number: String) = "${t("Rachunek", "Bank account")}: $number"
    fun bank(name: String) = "${t("Bank", "Bank")}: $name"
    val paid = t("Zapłacono", "Paid")

    val remarksHeading = t("Uwagi", "Remarks")
    fun page(page: Int, of: Int) = t("Strona $page z $of", "Page $page of $of")

    /** A remark as printed under the remarks heading: without the key "Uwagi" ([Fa3XmlWriter.REMARKS_KEY]) repeated. */
    fun remark(key: String, value: String) = if (key == Fa3XmlWriter.REMARKS_KEY) value else "$key: $value"

    companion object {
        val POLISH = InvoicePdfLabels(bilingual = false)
        val POLISH_ENGLISH = InvoicePdfLabels(bilingual = true)

        /** Polish for a Polish buyer (or without a country), "Polish / English" for a foreign one (US-013, Q2). */
        fun forBuyerCountry(countryCode: String?): InvoicePdfLabels =
            if (countryCode == null || countryCode == Countries.POLAND) POLISH else POLISH_ENGLISH
    }
}
