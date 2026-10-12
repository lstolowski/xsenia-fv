package pl.xsenia.fv.ksef.invoices

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.xsenia.fv.ksef.http.BigDecimalSerializer
import pl.xsenia.fv.ksef.http.InstantSerializer
import pl.xsenia.fv.ksef.http.LocalDateSerializer
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Role of my company on the invoice. */
enum class InvoiceSubjectType(val apiValue: String) {
    /** Podmiot1 — I am the seller: sales invoices. */
    SELLER("Subject1"),

    /** Podmiot2 — I am the buyer: purchase invoices. */
    BUYER("Subject2"),
}

enum class InvoiceDateType(val apiValue: String) {
    /** Invoice issue date (`P_1`). */
    ISSUE("Issue"),

    /** Date KSeF accepted the invoice for processing. */
    INVOICING("Invoicing"),

    /** Date the invoice was permanently stored in KSeF — recommended for incremental synchronization. */
    PERMANENT_STORAGE("PermanentStorage"),
}

/**
 * Invoice metadata as returned by `POST /invoices/query/metadata` and in `_metadata.json` of export packages.
 * Serializable, so it can be kept in the local invoice mirror (ADR 0003).
 */
@Serializable
data class InvoiceMetadata(
    val ksefNumber: String,
    val invoiceNumber: String,
    @Serializable(with = LocalDateSerializer::class) val issueDate: LocalDate,
    @Serializable(with = InstantSerializer::class) val invoicingDate: Instant,
    @Serializable(with = InstantSerializer::class) val acquisitionDate: Instant,
    @Serializable(with = InstantSerializer::class) val permanentStorageDate: Instant,
    val seller: Seller,
    val buyer: Buyer,
    @Serializable(with = BigDecimalSerializer::class) val netAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val grossAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val vatAmount: BigDecimal,
    val currency: String,
    /** `Online` or `Offline`. */
    val invoicingMode: String,
    /** KSeF invoice kind: `Vat`, `Kor`, `Zal`, `Roz`, `Upr`, `KorZal`, `KorRoz`, … (kept as text to tolerate new values). */
    val invoiceType: String,
    val formCode: FormCode,
    val isSelfInvoicing: Boolean,
    val hasAttachment: Boolean,
    /** SHA-256 of the invoice XML, Base64. */
    val invoiceHash: String,
    val hashOfCorrectedInvoice: String? = null,
) {
    @Serializable
    data class Seller(val nip: String, val name: String? = null)

    @Serializable
    data class Buyer(val identifier: BuyerIdentifier, val name: String? = null)

    /** [type]: `Nip`, `VatUe`, `Other` or `None`. */
    @Serializable
    data class BuyerIdentifier(val type: String, val value: String? = null)

    @Serializable
    data class FormCode(val systemCode: String, val schemaVersion: String, val value: String)
}

/**
 * Result of a metadata query. [permanentStorageHwmDate] is set only for [InvoiceDateType.PERMANENT_STORAGE]:
 * all invoices stored up to it are already returned, later ones may still appear.
 */
class InvoiceMetadataResult(val invoices: List<InvoiceMetadata>, val permanentStorageHwmDate: Instant?)

/** Invoice XML downloaded from KSeF together with its SHA-256 hash (Base64). */
class InvoiceXml(val ksefNumber: String, val content: ByteArray, val sha256Base64: String)

/** Contents of an export package. */
class InvoiceExport(
    val invoices: List<InvoiceMetadata>,
    /** Invoice XML files by KSeF number. */
    val xmlByKsefNumber: Map<String, ByteArray>,
    /** True when KSeF cut the package (10 000 invoices or 1 GB) — continue from [lastPermanentStorageDate]. */
    val isTruncated: Boolean,
    val lastPermanentStorageDate: Instant?,
    val permanentStorageHwmDate: Instant?,
)

// ---- API DTOs ----

@Serializable
internal data class InvoiceQueryFiltersDto(
    val subjectType: String,
    val dateRange: DateRangeDto,
)

@Serializable
internal data class DateRangeDto(
    val dateType: String,
    @Serializable(with = InstantSerializer::class) val from: Instant,
    @Serializable(with = InstantSerializer::class) val to: Instant? = null,
    val restrictToPermanentStorageHwmDate: Boolean? = null,
)

@Serializable
internal data class QueryMetadataResponse(
    val hasMore: Boolean,
    val isTruncated: Boolean,
    @Serializable(with = InstantSerializer::class) val permanentStorageHwmDate: Instant? = null,
    val invoices: List<InvoiceMetadata>,
)

@Serializable
internal data class EncryptionInfoDto(
    val encryptedSymmetricKey: String,
    val initializationVector: String,
    val publicKeyId: String? = null,
)

@Serializable
internal data class ExportRequestDto(
    val encryption: EncryptionInfoDto,
    val filters: InvoiceQueryFiltersDto,
    val compressionType: String = "Zip",
)

@Serializable
internal data class ExportResponseDto(val referenceNumber: String)

@Serializable
internal data class ExportStatusDto(val code: Int, val description: String, val details: List<String>? = null)

@Serializable
internal data class ExportStatusResponseDto(
    val status: ExportStatusDto,
    @SerialName("package") val invoicePackage: InvoicePackageDto? = null,
)

@Serializable
internal data class InvoicePackageDto(
    val invoiceCount: Long,
    val parts: List<InvoicePackagePartDto>,
    val isTruncated: Boolean,
    @Serializable(with = InstantSerializer::class) val lastPermanentStorageDate: Instant? = null,
    @Serializable(with = InstantSerializer::class) val permanentStorageHwmDate: Instant? = null,
    val compressionType: String = "Zip",
)

@Serializable
internal data class InvoicePackagePartDto(
    val ordinalNumber: Int,
    val method: String,
    val url: String,
    val partHash: String,
    val encryptedPartHash: String,
)

@Serializable
internal data class PackageMetadataDto(val invoices: List<InvoiceMetadata>)
