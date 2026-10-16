package pl.xsenia.fv.ksef.invoices

import io.ktor.client.HttpClient
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.KsefTimeoutException
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.auth.PublicKeyUsage
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import pl.xsenia.fv.ksef.http.KsefJson
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.zip.ZipInputStream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Downloads invoices from KSeF: metadata lists, single invoice XML and encrypted export packages. */
class KsefInvoiceClient internal constructor(
    private val api: KsefInvoicesApi,
    private val tokens: KsefTokenManager,
    private val publicKeys: KsefPublicKeyProvider,
    private val exportPollInterval: kotlin.time.Duration = 2.seconds,
    private val exportTimeout: kotlin.time.Duration = 5.minutes,
    private val limits: ExportLimits = ExportLimits(),
) {

    constructor(client: HttpClient, tokens: KsefTokenManager, publicKeys: KsefPublicKeyProvider) :
        this(KsefInvoicesApi(client), tokens, publicKeys)

    /**
     * Returns metadata of all invoices in `[from, to)` for the given role, oldest first, without duplicates,
     * and — for [InvoiceDateType.PERMANENT_STORAGE] — the KSeF high water mark (data up to it is complete).
     *
     * Splits the range into windows accepted by KSeF (max 100 days), follows pagination and
     * continues from the last record when KSeF truncates a result set (10 000 records).
     */
    suspend fun queryMetadata(
        subjectType: InvoiceSubjectType,
        dateType: InvoiceDateType,
        from: Instant,
        to: Instant,
    ): InvoiceMetadataResult {
        require(from.isBefore(to)) { "from must be before to" }
        val result = LinkedHashMap<String, InvoiceMetadata>()
        var hwm: Instant? = null
        var windowStart = from
        while (windowStart.isBefore(to)) {
            val windowEnd = minOf(windowStart.plus(MAX_WINDOW), to)
            val window = queryWindow(subjectType, dateType, windowStart, windowEnd)
            window.invoices.forEach { result.putIfAbsent(it.ksefNumber, it) }
            hwm = window.permanentStorageHwmDate ?: hwm
            windowStart = windowEnd
        }
        return InvoiceMetadataResult(result.values.toList(), hwm)
    }

    /** Downloads the invoice XML and verifies its SHA-256 against the `x-ms-meta-hash` header, if present. */
    suspend fun downloadInvoiceXml(ksefNumber: String): InvoiceXml = tokens.withAccessToken { accessToken ->
        val response = api.invoiceXml(accessToken, ksefNumber)
        val content = response.readRawBytes()
        val hash = KsefCrypto.sha256Base64(content)
        response.headers[HASH_HEADER]?.let { expected ->
            if (expected != hash) throw KsefProtocolException("Invoice $ksefNumber hash mismatch")
        }
        InvoiceXml(ksefNumber, content, hash)
    }

    /**
     * Exports invoices (XML + metadata) with dates in `[from, to)` as an encrypted package, waits for it,
     * then downloads, decrypts and unpacks it. Ranges longer than 100 days must be split by the caller.
     */
    suspend fun exportInvoices(
        subjectType: InvoiceSubjectType,
        dateType: InvoiceDateType,
        from: Instant,
        to: Instant? = null,
        restrictToPermanentStorageHwmDate: Boolean? = null,
    ): InvoiceExport {
        val symmetricKey = KsefCrypto.generateSymmetricKey()
        val publicKey = publicKeys.publicKey(PublicKeyUsage.SYMMETRIC_KEY_ENCRYPTION)
        val encoder = Base64.getEncoder()
        val request = ExportRequestDto(
            encryption = EncryptionInfoDto(
                encryptedSymmetricKey = encoder.encodeToString(KsefCrypto.encryptRsaOaepSha256(symmetricKey.key, publicKey.key)),
                initializationVector = encoder.encodeToString(symmetricKey.iv),
                publicKeyId = publicKey.id,
            ),
            filters = InvoiceQueryFiltersDto(
                subjectType = subjectType.apiValue,
                dateRange = DateRangeDto(dateType.apiValue, from, to, restrictToPermanentStorageHwmDate),
            ),
        )

        val referenceNumber = tokens.withAccessToken { api.startExport(it, request) }.referenceNumber
        val invoicePackage = awaitExport(referenceNumber)

        // The decrypted package goes to a temporary file (the cache directory on Android), not to memory.
        val archive = File.createTempFile("ksef-export-", ".zip")
        try {
            archive.outputStream().buffered().use { out ->
                invoicePackage.parts.sortedBy { it.ordinalNumber }.forEach { part ->
                    val encrypted = api.downloadPart(part)
                    if (KsefCrypto.sha256Base64(encrypted) != part.encryptedPartHash) {
                        throw KsefProtocolException("Export $referenceNumber part ${part.ordinalNumber}: encrypted hash mismatch")
                    }
                    val decrypted = KsefCrypto.decryptAes256Cbc(encrypted, symmetricKey)
                    if (KsefCrypto.sha256Base64(decrypted) != part.partHash) {
                        throw KsefProtocolException("Export $referenceNumber part ${part.ordinalNumber}: hash mismatch")
                    }
                    out.write(decrypted)
                }
            }
            return unpack(archive, invoicePackage)
        } finally {
            archive.delete()
        }
    }

    private suspend fun queryWindow(
        subjectType: InvoiceSubjectType,
        dateType: InvoiceDateType,
        from: Instant,
        to: Instant,
    ): InvoiceMetadataResult {
        val invoices = mutableListOf<InvoiceMetadata>()
        var rangeFrom = from
        var pageOffset = 0
        while (true) {
            val filters = InvoiceQueryFiltersDto(subjectType.apiValue, DateRangeDto(dateType.apiValue, rangeFrom, to))
            val page = tokens.withAccessToken { api.queryMetadata(it, filters, pageOffset, PAGE_SIZE) }
            invoices += page.invoices
            when {
                !page.hasMore -> return InvoiceMetadataResult(invoices, page.permanentStorageHwmDate)
                page.isTruncated -> {
                    // Result set limit reached: restart from the date of the last record (duplicates removed by the caller).
                    rangeFrom = page.invoices.last().dateOf(dateType)
                    pageOffset = 0
                }
                else -> pageOffset++
            }
        }
    }

    private suspend fun awaitExport(referenceNumber: String): InvoicePackageDto {
        var invoicePackage: InvoicePackageDto? = null
        withTimeoutOrNull(exportTimeout) {
            while (invoicePackage == null) {
                val response = tokens.withAccessToken { api.exportStatus(it, referenceNumber) }
                when (response.status.code) {
                    EXPORT_IN_PROGRESS -> delay(exportPollInterval)
                    EXPORT_DONE -> invoicePackage = response.invoicePackage
                        ?: throw KsefProtocolException("Export $referenceNumber finished without a package")
                    else -> throw KsefProtocolException(
                        "Export $referenceNumber failed: ${response.status.code} ${response.status.description} " +
                            response.status.details.orEmpty(),
                    )
                }
            }
        }
        return invoicePackage ?: throw KsefTimeoutException("KSeF export $referenceNumber did not finish within $exportTimeout")
    }

    private fun unpack(archive: File, invoicePackage: InvoicePackageDto): InvoiceExport {
        if (!invoicePackage.compressionType.equals("Zip", ignoreCase = true)) {
            throw KsefProtocolException("Unsupported export compression: ${invoicePackage.compressionType}")
        }
        var metadata: List<InvoiceMetadata> = emptyList()
        val xmlFiles = mutableMapOf<String, ByteArray>()
        var entries = 0
        var totalBytes = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                if (++entries > limits.maxEntries) throw KsefProtocolException("Export package has more than ${limits.maxEntries} entries")
                val name = entry.name.substringAfterLast('/')
                val bytes = zip.readLimited(minOf(limits.maxEntryBytes, limits.maxTotalBytes - totalBytes), name)
                totalBytes += bytes.size
                when {
                    name == METADATA_FILE -> metadata = KsefJson.decodeFromString<PackageMetadataDto>(bytes.toString(Charsets.UTF_8)).invoices
                    name.endsWith(XML_SUFFIX, ignoreCase = true) -> xmlFiles[name.dropLast(XML_SUFFIX.length)] = bytes
                }
            }
        }
        return InvoiceExport(
            invoices = metadata,
            xmlByKsefNumber = xmlFiles,
            isTruncated = invoicePackage.isTruncated,
            lastPermanentStorageDate = invoicePackage.lastPermanentStorageDate,
            permanentStorageHwmDate = invoicePackage.permanentStorageHwmDate,
        )
    }

    /** Reads the current entry, failing as soon as it exceeds [limit] bytes (a ZIP can be tiny and expand enormously). */
    private fun ZipInputStream.readLimited(limit: Long, name: String): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) return out.toByteArray()
            if (out.size() + read > limit) throw KsefProtocolException("Export package entry $name exceeds the size limits")
            out.write(buffer, 0, read)
        }
    }

    private fun InvoiceMetadata.dateOf(dateType: InvoiceDateType): Instant = when (dateType) {
        InvoiceDateType.ISSUE -> issueDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        InvoiceDateType.INVOICING -> invoicingDate
        InvoiceDateType.PERMANENT_STORAGE -> permanentStorageDate
    }

    /** Bounds for what an export package may unpack to; KSeF invoices are far smaller. */
    class ExportLimits(
        val maxEntries: Int = 50_000,
        val maxEntryBytes: Long = 10L * 1024 * 1024,
        val maxTotalBytes: Long = 100L * 1024 * 1024,
    )

    private companion object {
        const val XML_SUFFIX = ".xml"
        const val BUFFER_SIZE = 8192
        // KSeF accepts at most 100 days per query; keep a margin for time zone handling.
        val MAX_WINDOW: Duration = Duration.ofDays(90)
        const val PAGE_SIZE = 250
        const val HASH_HEADER = "x-ms-meta-hash"
        const val METADATA_FILE = "_metadata.json"
        const val EXPORT_IN_PROGRESS = 100
        const val EXPORT_DONE = 200
    }
}
