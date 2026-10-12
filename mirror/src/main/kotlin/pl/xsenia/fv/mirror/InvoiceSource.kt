package pl.xsenia.fv.mirror

import pl.xsenia.fv.ksef.invoices.InvoiceDateType
import pl.xsenia.fv.ksef.invoices.InvoiceExport
import pl.xsenia.fv.ksef.invoices.InvoiceMetadataResult
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType
import pl.xsenia.fv.ksef.invoices.KsefInvoiceClient
import java.time.Instant

/** KSeF operations used by the synchronizer (an interface, so the algorithm can be tested without KSeF). */
interface InvoiceSource {
    suspend fun queryStoredSince(subjectType: InvoiceSubjectType, from: Instant, to: Instant): InvoiceMetadataResult

    suspend fun downloadXml(ksefNumber: String): ByteArray

    suspend fun exportStored(subjectType: InvoiceSubjectType, from: Instant, to: Instant?): InvoiceExport
}

/** [InvoiceSource] backed by the real KSeF API; always uses the `PermanentStorage` date (required for increments). */
class KsefInvoiceSource(private val client: KsefInvoiceClient) : InvoiceSource {

    override suspend fun queryStoredSince(subjectType: InvoiceSubjectType, from: Instant, to: Instant) =
        client.queryMetadata(subjectType, InvoiceDateType.PERMANENT_STORAGE, from, to)

    override suspend fun downloadXml(ksefNumber: String): ByteArray = client.downloadInvoiceXml(ksefNumber).content

    override suspend fun exportStored(subjectType: InvoiceSubjectType, from: Instant, to: Instant?) =
        client.exportInvoices(subjectType, InvoiceDateType.PERMANENT_STORAGE, from, to, restrictToPermanentStorageHwmDate = true)
}
