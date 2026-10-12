package pl.xsenia.fv.ksef.sending

import kotlinx.serialization.Serializable
import pl.xsenia.fv.ksef.http.InstantSerializer
import pl.xsenia.fv.ksef.invoices.EncryptionInfoDto
import java.time.Instant

/** Schema of the invoices sent in a session, e.g. `FA (3)` / `1-0E` / `FA`. */
data class InvoiceFormCode(val systemCode: String, val schemaVersion: String, val value: String) {
    companion object {
        val FA3 = InvoiceFormCode("FA (3)", "1-0E", "FA")
    }
}

/** Identifies an invoice sent to KSeF and still being processed — persist it to resume after an app restart. */
@Serializable
data class SubmittedInvoice(val sessionReferenceNumber: String, val invoiceReferenceNumber: String)

/** Invoice accepted by KSeF. */
class SentInvoice(
    val submitted: SubmittedInvoice,
    val ksefNumber: String,
    val acquisitionDate: Instant?,
    /** Official receipt (UPO) XML, or null when KSeF did not provide it yet. */
    val upo: ByteArray?,
)

// ---- API DTOs ----

@Serializable
internal data class FormCodeDto(val systemCode: String, val schemaVersion: String, val value: String)

@Serializable
internal data class OpenOnlineSessionRequestDto(val formCode: FormCodeDto, val encryption: EncryptionInfoDto)

@Serializable
internal data class OpenOnlineSessionResponseDto(
    val referenceNumber: String,
    @Serializable(with = InstantSerializer::class) val validUntil: Instant,
)

@Serializable
internal data class SendInvoiceRequestDto(
    val invoiceHash: String,
    val invoiceSize: Long,
    val encryptedInvoiceHash: String,
    val encryptedInvoiceSize: Long,
    val encryptedInvoiceContent: String,
)

@Serializable
internal data class SendInvoiceResponseDto(val referenceNumber: String)

@Serializable
internal data class InvoiceStatusInfoDto(
    val code: Int,
    val description: String,
    val details: List<String>? = null,
    val extensions: Map<String, String?>? = null,
)

@Serializable
internal data class SessionInvoiceStatusDto(
    val referenceNumber: String,
    val ksefNumber: String? = null,
    @Serializable(with = InstantSerializer::class) val acquisitionDate: Instant? = null,
    val status: InvoiceStatusInfoDto,
)
