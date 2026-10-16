package pl.xsenia.fv.ksef.sending

import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import pl.xsenia.fv.ksef.KsefApiException
import pl.xsenia.fv.ksef.KsefException
import pl.xsenia.fv.ksef.KsefInvoiceRejectedException
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.KsefTimeoutException
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.auth.PublicKeyUsage
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import pl.xsenia.fv.ksef.invoices.EncryptionInfoDto
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Sends a single invoice in an interactive (online) session:
 * open session (AES key wrapped with the MF key) → send encrypted invoice → wait for the KSeF number →
 * download UPO → close session. The session is closed also when the invoice is rejected.
 */
class KsefInvoiceSender internal constructor(
    private val api: KsefSessionApi,
    private val tokens: KsefTokenManager,
    private val publicKeys: KsefPublicKeyProvider,
    private val pollInterval: Duration = 2.seconds,
    private val timeout: Duration = 3.minutes,
    private val upoAttempts: Int = 3,
    private val upoRetryDelay: Duration = 2.seconds,
) {

    constructor(client: HttpClient, tokens: KsefTokenManager, publicKeys: KsefPublicKeyProvider) :
        this(KsefSessionApi(client), tokens, publicKeys)

    /**
     * @param onSubmitted called as soon as KSeF accepted the request for processing — persist [SubmittedInvoice]
     *   so the result can be fetched with [awaitResult] if the app is closed before processing ends.
     * @throws KsefInvoiceRejectedException when KSeF rejects the invoice
     */
    suspend fun send(
        invoiceXml: ByteArray,
        formCode: InvoiceFormCode = InvoiceFormCode.FA3,
        onSubmitted: suspend (SubmittedInvoice) -> Unit = {},
    ): SentInvoice {
        val key = KsefCrypto.generateSymmetricKey()
        val publicKey = publicKeys.publicKey(PublicKeyUsage.SYMMETRIC_KEY_ENCRYPTION)
        val encoder = Base64.getEncoder()
        val session = tokens.withAccessToken {
            api.open(
                it,
                OpenOnlineSessionRequestDto(
                    formCode = FormCodeDto(formCode.systemCode, formCode.schemaVersion, formCode.value),
                    encryption = EncryptionInfoDto(
                        encryptedSymmetricKey = encoder.encodeToString(KsefCrypto.encryptRsaOaepSha256(key.key, publicKey.key)),
                        initializationVector = encoder.encodeToString(key.iv),
                        publicKeyId = publicKey.id,
                    ),
                ),
            )
        }

        try {
            val encrypted = KsefCrypto.encryptAes256Cbc(invoiceXml, key)
            val request = SendInvoiceRequestDto(
                invoiceHash = KsefCrypto.sha256Base64(invoiceXml),
                invoiceSize = invoiceXml.size.toLong(),
                encryptedInvoiceHash = KsefCrypto.sha256Base64(encrypted),
                encryptedInvoiceSize = encrypted.size.toLong(),
                encryptedInvoiceContent = encoder.encodeToString(encrypted),
            )
            // TODO CODE REVIEWED: If KSeF accepts this POST but the response is lost, no SubmittedInvoice is persisted and a retry can submit the same invoice again. Reconcile uncertain submissions before treating this as a safe resend.
            val invoiceReference = tokens.withAccessToken { api.sendInvoice(it, session.referenceNumber, request) }.referenceNumber
            val submitted = SubmittedInvoice(session.referenceNumber, invoiceReference)
            onSubmitted(submitted)
            return awaitResult(submitted)
        } finally {
            closeQuietly(session.referenceNumber)
        }
    }

    /** Waits until KSeF finishes processing a submitted invoice and downloads its UPO. */
    suspend fun awaitResult(submitted: SubmittedInvoice): SentInvoice {
        var status: SessionInvoiceStatusDto? = null
        withTimeoutOrNull(timeout) {
            while (status == null) {
                val current = tokens.withAccessToken {
                    api.invoiceStatus(it, submitted.sessionReferenceNumber, submitted.invoiceReferenceNumber)
                }
                when (current.status.code) {
                    ACCEPTED, PROCESSING -> delay(pollInterval)
                    else -> status = current
                }
            }
        }
        val final = status ?: throw KsefTimeoutException("KSeF did not process invoice ${submitted.invoiceReferenceNumber} within $timeout")
        if (final.status.code != SUCCESS) {
            throw KsefInvoiceRejectedException(
                final.status.code,
                final.status.description,
                final.status.details.orEmpty(),
                final.status.extensions.orEmpty(),
            )
        }
        // Not a rejection: KSeF says "accepted", so the invoice may well be in KSeF — it must not be offered for resending.
        val ksefNumber = final.ksefNumber
            ?: throw KsefProtocolException("KSeF accepted invoice ${submitted.invoiceReferenceNumber} but returned no KSeF number")
        return SentInvoice(submitted, ksefNumber, final.acquisitionDate, downloadUpo(submitted))
    }

    /**
     * UPO of an accepted invoice; null when KSeF has not produced it (yet) — it can be fetched later.
     * A missing UPO (404) and temporary server errors (5xx) are retried [upoAttempts] times; other errors mean "no UPO".
     */
    suspend fun downloadUpo(submitted: SubmittedInvoice): ByteArray? {
        repeat(upoAttempts) { attempt ->
            try {
                return tokens.withAccessToken { api.invoiceUpo(it, submitted.sessionReferenceNumber, submitted.invoiceReferenceNumber) }
            } catch (e: KsefApiException) {
                val retryable = e.httpStatus == HTTP_NOT_FOUND || e.httpStatus >= HTTP_SERVER_ERROR
                if (!retryable) return null
                if (attempt < upoAttempts - 1) delay(upoRetryDelay)
            }
        }
        return null
    }

    private suspend fun closeQuietly(sessionReference: String) {
        try {
            tokens.withAccessToken { api.close(it, sessionReference) }
        } catch (e: KsefException) {
            // Not critical: an unclosed session expires on its own (12 h); the invoice result is already known.
        }
    }


    private companion object {
        const val ACCEPTED = 100
        const val PROCESSING = 150
        const val SUCCESS = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_SERVER_ERROR = 500
    }
}
