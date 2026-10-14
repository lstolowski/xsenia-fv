package pl.xsenia.fv.invoice.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Structured invoice (FA(2)/FA(3)) as needed by the invoice details view.
 * Field comments name the FA element the value comes from.
 */
data class Invoice(
    /** System code of the schema, e.g. `FA (3)` (`KodFormularza@kodSystemowy`). */
    val schema: String,
    /** `RodzajFaktury`: VAT, KOR, ZAL, ROZ, UPR, KOR_ZAL, KOR_ROZ. */
    val kind: String,
    /** `P_2`. */
    val invoiceNumber: String,
    /** `P_1`. */
    val issueDate: LocalDate,
    /** `P_1M`. */
    val issuePlace: String?,
    /** `P_6` — common delivery date, present only when different from the issue date. */
    val saleDate: LocalDate?,
    /** `OkresFa` (`P_6_Od`, `P_6_Do`). */
    val period: DatePeriod?,
    /** `KodWaluty`. */
    val currency: String,
    /** `Podmiot1`. */
    val seller: Party,
    /** `Podmiot2`. */
    val buyer: Party,
    /** `Podmiot3` — other parties (e.g. recipient, payer) with their role code. */
    val otherParties: List<Party>,
    /** `FaWiersz`. */
    val lines: List<InvoiceLine>,
    /** `P_13_x` / `P_14_x` — net and VAT per rate. */
    val vatSummary: List<VatSummaryEntry>,
    /** `P_15` — total amount due. */
    val grossTotal: BigDecimal,
    /** `Adnotacje`. */
    val annotations: Annotations,
    /** `PrzyczynaKorekty`, `TypKorekty`, `DaneFaKorygowanej` — for corrections only. */
    val correction: Correction?,
    /** `Platnosc`. */
    val payment: Payment?,
    /** `DodatkowyOpis` (`Klucz`, `Wartosc`). */
    val additionalInfo: List<Pair<String, String>>,
    /** `Stopka` texts. */
    val footer: List<String>,
) {
    val netTotal: BigDecimal get() = vatSummary.fold(BigDecimal.ZERO) { sum, it -> sum + it.net }

    val vatTotal: BigDecimal get() = vatSummary.fold(BigDecimal.ZERO) { sum, it -> sum + (it.vat ?: BigDecimal.ZERO) }

    val isCorrection: Boolean get() = kind.startsWith("KOR")

    /** VAT converted to PLN (sum of `P_14_xW`) on a foreign currency invoice; null when the invoice has none. */
    val vatInPlnTotal: BigDecimal?
        get() = vatSummary.mapNotNull { it.vatInPln }.takeIf { it.isNotEmpty() }?.fold(BigDecimal.ZERO, BigDecimal::add)

    /** `KursWaluty` when all lines that have it use the same rate (our invoices: always); null otherwise. */
    val exchangeRate: BigDecimal?
        get() = lines.mapNotNull { it.exchangeRate }.distinct().singleOrNull()

    /**
     * Delivery date used to assign the invoice to a month (decision D2):
     * `P_6` → `OkresFa/P_6_Do` → latest `P_6A` of the lines → issue date `P_1`.
     */
    val deliveryDate: LocalDate
        get() = saleDate ?: period?.to ?: lines.mapNotNull { it.deliveryDate }.maxOrNull() ?: issueDate
}

data class DatePeriod(val from: LocalDate, val to: LocalDate)

data class Party(
    val name: String?,
    val nip: String?,
    /** EU VAT number with country prefix, e.g. `DE123456789` (`KodUE` + `NrVatUE`). */
    val euVatNumber: String?,
    /** Other identifier with country code (`KodKraju` + `NrID`). */
    val otherId: String?,
    /** `AdresL1` (may contain line breaks) and `AdresL2`, split into lines. */
    val addressLines: List<String>,
    val countryCode: String?,
    val emails: List<String>,
    val phones: List<String>,
    /** `Rola` code of a `Podmiot3`; null for the seller and buyer. */
    val role: String? = null,
)

data class InvoiceLine(
    /** `NrWierszaFa`. */
    val number: Int?,
    /** `P_7`. */
    val name: String?,
    /** `P_8A`. */
    val unit: String?,
    /** `P_8B`. */
    val quantity: BigDecimal?,
    /** `P_9A`. */
    val unitNetPrice: BigDecimal?,
    /** `P_9B`. */
    val unitGrossPrice: BigDecimal?,
    /** `P_10`. */
    val discount: BigDecimal?,
    /** `P_11`. */
    val netValue: BigDecimal?,
    /** `P_11A` — only when the invoice is calculated from gross prices. */
    val grossValue: BigDecimal?,
    /** `P_12`: `23`, `8`, `5`, `0 KR`, `zw`, `np I`, … */
    val vatRate: String?,
    /** `P_6A`. */
    val deliveryDate: LocalDate?,
    /** `StanPrzed` — line describing the state before a correction. */
    val isStateBefore: Boolean,
    /** `KursWaluty` — exchange rate used to convert VAT to PLN on a foreign currency invoice. */
    val exchangeRate: BigDecimal? = null,
) {
    /** Gross value: `P_11A` when present, otherwise net value × (1 + rate), rounded to grosze. */
    val displayGrossValue: BigDecimal?
        get() = grossValue ?: netValue?.let { net ->
            val rate = vatRatePercent
            when {
                rate != null -> net + (net * rate).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                // Exempt, not taxable, reverse charge: the seller charges no VAT.
                vatRate in NO_VAT_RATES -> net
                else -> null
            }
        }

    /** Numeric VAT rate (`23`, `0` for `0 KR`), or null for `zw`, `np`, `oo`. */
    val vatRatePercent: BigDecimal?
        get() = vatRate?.substringBefore(' ')?.toBigDecimalOrNull()

    private companion object {
        val HUNDRED = BigDecimal(100)
        val NO_VAT_RATES = setOf("zw", "oo", "np", "np I", "np II")
    }
}

/** VAT categories of the invoice summary (`P_13_x`/`P_14_x`). */
enum class VatCategory {
    /** 23% (22%). */
    STANDARD,

    /** 8% (7%). */
    REDUCED_8,

    /** 5%. */
    REDUCED_5,

    /** 4% (3%) — flat-rate taxi. */
    TAXI_FLAT_RATE,

    /** OSS / IOSS procedure. */
    OSS,
    ZERO_DOMESTIC,
    ZERO_INTRA_EU_SUPPLY,
    ZERO_EXPORT,
    EXEMPT,
    NOT_TAXABLE_ABROAD,
    NOT_TAXABLE_ART_100,
    REVERSE_CHARGE,
    MARGIN,
}

data class VatSummaryEntry(
    val category: VatCategory,
    val net: BigDecimal,
    /** VAT in the invoice currency; null for categories without VAT. */
    val vat: BigDecimal?,
    /** VAT converted to PLN (`P_14_xW`) for foreign currency invoices. */
    val vatInPln: BigDecimal?,
)

data class Annotations(
    /** `P_16` — metoda kasowa. */
    val cashMethod: Boolean,
    /** `P_17` — samofakturowanie. */
    val selfInvoicing: Boolean,
    /** `P_18` — odwrotne obciążenie. */
    val reverseCharge: Boolean,
    /** `P_18A` — mechanizm podzielonej płatności. */
    val splitPayment: Boolean,
    /** `Zwolnienie/P_19A|B|C` — legal basis of the VAT exemption. */
    val exemptionBasis: String?,
    /** `PMarzy` — margin scheme. */
    val marginScheme: Boolean,
)

data class Correction(
    val reason: String?,
    /** `TypKorekty`: 1 — as of the corrected invoice date, 2 — as of the correction date, 3 — other. */
    val type: String?,
    val correctedInvoices: List<CorrectedInvoice>,
)

data class CorrectedInvoice(val invoiceNumber: String?, val issueDate: LocalDate?, val ksefNumber: String?)

data class Payment(
    /** `Zaplacono`. */
    val paid: Boolean,
    /** `DataZaplaty`. */
    val paymentDate: LocalDate?,
    /** `TerminPlatnosci`. */
    val dueDates: List<DueDate>,
    /** `FormaPlatnosci`: 1 gotówka, 2 karta, 3 bon, 4 czek, 5 kredyt, 6 przelew, 7 mobilna. */
    val methodCode: Int?,
    /** `OpisPlatnosci` when `PlatnoscInna` is set. */
    val otherMethod: String?,
    /** `RachunekBankowy`. */
    val bankAccounts: List<BankAccountInfo>,
)

data class DueDate(val date: LocalDate?, val description: String?)

data class BankAccountInfo(val number: String, val bankName: String?, val swift: String?, val description: String?)
