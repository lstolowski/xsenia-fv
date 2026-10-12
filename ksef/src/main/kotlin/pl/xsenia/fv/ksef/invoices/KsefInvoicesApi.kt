package pl.xsenia.fv.ksef.invoices

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import pl.xsenia.fv.ksef.http.bodyOrThrow
import pl.xsenia.fv.ksef.http.throwIfError

/** Thin wrapper over the KSeF invoice download endpoints. */
internal class KsefInvoicesApi(private val client: HttpClient) {

    suspend fun queryMetadata(
        accessToken: String,
        filters: InvoiceQueryFiltersDto,
        pageOffset: Int,
        pageSize: Int,
    ): QueryMetadataResponse =
        client.post("invoices/query/metadata") {
            bearerAuth(accessToken)
            parameter("pageOffset", pageOffset)
            parameter("pageSize", pageSize)
            parameter("sortOrder", "Asc")
            contentType(ContentType.Application.Json)
            setBody(filters)
        }.bodyOrThrow()

    suspend fun invoiceXml(accessToken: String, ksefNumber: String): HttpResponse =
        client.get("invoices/ksef/$ksefNumber") { bearerAuth(accessToken) }.also { it.throwIfError() }

    suspend fun startExport(accessToken: String, request: ExportRequestDto): ExportResponseDto =
        client.post("invoices/exports") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrThrow()

    suspend fun exportStatus(accessToken: String, referenceNumber: String): ExportStatusResponseDto =
        client.get("invoices/exports/$referenceNumber") { bearerAuth(accessToken) }.bodyOrThrow()

    /** Package parts are served from pre-signed URLs: no access token, not subject to API limits. */
    suspend fun downloadPart(part: InvoicePackagePartDto): ByteArray =
        client.request(part.url) { method = HttpMethod.parse(part.method.uppercase()) }
            .also { it.throwIfError() }
            .readRawBytes()
}
