package pl.xsenia.fv.ksef.sending

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pl.xsenia.fv.ksef.http.bodyOrThrow
import pl.xsenia.fv.ksef.http.throwIfError

/** Thin wrapper over the KSeF interactive (online) session endpoints. */
internal class KsefSessionApi(private val client: HttpClient) {

    suspend fun open(accessToken: String, request: OpenOnlineSessionRequestDto): OpenOnlineSessionResponseDto =
        client.post("sessions/online") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrThrow()

    suspend fun sendInvoice(accessToken: String, sessionReference: String, request: SendInvoiceRequestDto): SendInvoiceResponseDto =
        client.post("sessions/online/$sessionReference/invoices") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOrThrow()

    suspend fun invoiceStatus(accessToken: String, sessionReference: String, invoiceReference: String): SessionInvoiceStatusDto =
        client.get("sessions/$sessionReference/invoices/$invoiceReference") { bearerAuth(accessToken) }.bodyOrThrow()

    suspend fun invoiceUpo(accessToken: String, sessionReference: String, invoiceReference: String): ByteArray =
        client.get("sessions/$sessionReference/invoices/$invoiceReference/upo") { bearerAuth(accessToken) }
            .also { it.throwIfError() }
            .readRawBytes()

    suspend fun close(accessToken: String, sessionReference: String) {
        client.post("sessions/online/$sessionReference/close") { bearerAuth(accessToken) }.throwIfError()
    }
}
