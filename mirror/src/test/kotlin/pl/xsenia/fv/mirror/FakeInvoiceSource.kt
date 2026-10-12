package pl.xsenia.fv.mirror

import pl.xsenia.fv.ksef.invoices.InvoiceExport
import pl.xsenia.fv.ksef.invoices.InvoiceMetadata
import pl.xsenia.fv.ksef.invoices.InvoiceMetadataResult
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** In-memory KSeF: invoices with a permanent storage date and a high water mark. Fictitious data only. */
class FakeInvoiceSource(var hwm: Instant) : InvoiceSource {

    class StoredInvoice(val subjectType: InvoiceSubjectType, val metadata: InvoiceMetadata, val xml: ByteArray)

    val invoices = mutableListOf<StoredInvoice>()
    val exports = mutableListOf<Triple<InvoiceSubjectType, Instant, Instant?>>()
    val queries = mutableListOf<Triple<InvoiceSubjectType, Instant, Instant>>()
    val downloads = mutableListOf<String>()

    /** Max invoices per export package (simulates KSeF truncation). */
    var exportLimit = Int.MAX_VALUE
    var failDownloadsAfter = Int.MAX_VALUE

    fun store(subjectType: InvoiceSubjectType, number: String, stored: String, saleDate: String? = null) {
        // KSeF number format: NIP-YYYYMMDD-12 hex chars-checksum.
        val ksefNumber = "9945082500-20260101-%012X-00".format(invoices.size + 1)
        val metadata = metadata(ksefNumber, number, Instant.parse(stored))
        invoices += StoredInvoice(subjectType, metadata, invoiceXml(number, metadata.issueDate, saleDate?.let(LocalDate::parse)))
    }

    override suspend fun queryStoredSince(subjectType: InvoiceSubjectType, from: Instant, to: Instant): InvoiceMetadataResult {
        queries += Triple(subjectType, from, to)
        val found = invoices.filter { it.subjectType == subjectType && it.storedIn(from, to) }.map { it.metadata }
        return InvoiceMetadataResult(found, hwm)
    }

    override suspend fun downloadXml(ksefNumber: String): ByteArray {
        if (downloads.size >= failDownloadsAfter) throw IllegalStateException("simulated rate limit")
        downloads += ksefNumber
        return invoices.first { it.metadata.ksefNumber == ksefNumber }.xml
    }

    override suspend fun exportStored(subjectType: InvoiceSubjectType, from: Instant, to: Instant?): InvoiceExport {
        exports += Triple(subjectType, from, to)
        val end = to ?: hwm
        val matching = invoices.filter { it.subjectType == subjectType && it.storedIn(from, end) }
            .sortedBy { it.metadata.permanentStorageDate }
        val packaged = matching.take(exportLimit)
        val truncated = packaged.size < matching.size
        return InvoiceExport(
            invoices = packaged.map { it.metadata },
            xmlByKsefNumber = packaged.associate { it.metadata.ksefNumber to it.xml },
            isTruncated = truncated,
            lastPermanentStorageDate = packaged.lastOrNull()?.metadata?.permanentStorageDate,
            permanentStorageHwmDate = hwm,
        )
    }

    private fun StoredInvoice.storedIn(from: Instant, to: Instant) =
        !metadata.permanentStorageDate.isBefore(from) && metadata.permanentStorageDate.isBefore(to)

    companion object {
        fun metadata(ksefNumber: String, number: String, stored: Instant) = InvoiceMetadata(
            ksefNumber = ksefNumber,
            invoiceNumber = number,
            issueDate = LocalDate.ofInstant(stored, java.time.ZoneOffset.UTC),
            invoicingDate = stored,
            acquisitionDate = stored,
            permanentStorageDate = stored,
            seller = InvoiceMetadata.Seller("9945082500", "Jan Testowy"),
            buyer = InvoiceMetadata.Buyer(InvoiceMetadata.BuyerIdentifier("Nip", "6691034274"), "Testowa Sp. z o.o."),
            netAmount = BigDecimal("100"),
            grossAmount = BigDecimal("123"),
            vatAmount = BigDecimal("23"),
            currency = "PLN",
            invoicingMode = "Online",
            invoiceType = "Vat",
            formCode = InvoiceMetadata.FormCode("FA (3)", "1-0E", "FA"),
            isSelfInvoicing = false,
            hasAttachment = false,
            invoiceHash = "aGFzaA==",
        )

        fun invoiceXml(number: String, issueDate: LocalDate, saleDate: LocalDate?) = """
            <Faktura xmlns="http://crd.gov.pl/wzor/2025/06/25/13775/">
              <Podmiot1><DaneIdentyfikacyjne><NIP>9945082500</NIP><Nazwa>Jan Testowy</Nazwa></DaneIdentyfikacyjne></Podmiot1>
              <Podmiot2><DaneIdentyfikacyjne><NIP>6691034274</NIP><Nazwa>Testowa Sp. z o.o.</Nazwa></DaneIdentyfikacyjne></Podmiot2>
              <Fa><KodWaluty>PLN</KodWaluty><P_1>$issueDate</P_1><P_2>$number</P_2>${saleDate?.let { "<P_6>$it</P_6>" }.orEmpty()}
                <P_13_1>100</P_13_1><P_14_1>23</P_14_1><P_15>123</P_15><RodzajFaktury>VAT</RodzajFaktury></Fa>
            </Faktura>
        """.trimIndent().toByteArray()
    }
}
