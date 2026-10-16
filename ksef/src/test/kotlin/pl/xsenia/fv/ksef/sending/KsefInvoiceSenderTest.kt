package pl.xsenia.fv.ksef.sending

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.ksef.KsefInvoiceRejectedException
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.KsefTimeoutException
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import pl.xsenia.fv.ksef.crypto.SymmetricKey
import pl.xsenia.fv.ksef.testing.MockKsef
import pl.xsenia.fv.ksef.testing.authResponse
import pl.xsenia.fv.ksef.testing.json
import pl.xsenia.fv.ksef.testing.publicKeysOf
import pl.xsenia.fv.ksef.testing.rsaKeyPair
import pl.xsenia.fv.ksef.testing.tokenManager
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.time.Duration.Companion.milliseconds

// runBlocking: MockEngine answers on its own dispatcher (see KsefAuthenticatorTest).
class KsefInvoiceSenderTest {

    private val keyPair = rsaKeyPair()
    private val invoiceXml = "<Faktura>Lorem ipsum</Faktura>".toByteArray()

    private fun HttpRequestData.bodyJson() =
        Json.parseToJsonElement(String((body as OutgoingContent.ByteArrayContent).bytes(), Charsets.UTF_8)).jsonObject

    /** [upoStatuses] — answers of the UPO endpoint in order (the last one repeats); [upoStatus] — one answer. */
    private fun ksef(
        statuses: List<String>,
        upoStatus: HttpStatusCode = HttpStatusCode.OK,
        upoStatuses: List<HttpStatusCode> = listOf(upoStatus),
    ): MockKsef {
        val queue = ArrayDeque(statuses)
        val upoQueue = ArrayDeque(upoStatuses)
        return MockKsef { request ->
            authResponse(request) ?: when (request.url.encodedPath) {
                "/v2/sessions/online" -> json("""{"referenceNumber":"SO-1","validUntil":"2099-01-01T00:00:00+00:00"}""", HttpStatusCode.Created)
                "/v2/sessions/online/SO-1/invoices" -> json("""{"referenceNumber":"INV-1"}""", HttpStatusCode.Accepted)
                "/v2/sessions/SO-1/invoices/INV-1" -> json(queue.removeFirstOrNull() ?: status(150))
                "/v2/sessions/SO-1/invoices/INV-1/upo" ->
                    (if (upoQueue.size > 1) upoQueue.removeFirst() else upoQueue.first()).let {
                        if (it == HttpStatusCode.OK) respond("<UPO>test</UPO>") else json("", it)
                    }
                "/v2/sessions/online/SO-1/close" -> respond("", HttpStatusCode.NoContent)
                else -> error("Unexpected request ${request.url}")
            }
        }
    }

    private fun status(code: Int, ksefNumber: String? = null, extensions: String = "null") =
        """{"ordinalNumber":1,"referenceNumber":"INV-1","invoiceHash":"x","invoicingDate":"2026-10-12T10:00:00+00:00",
           ${ksefNumber?.let { "\"ksefNumber\":\"$it\",\"acquisitionDate\":\"2026-10-12T10:00:05+00:00\"," }.orEmpty()}
           "status":{"code":$code,"description":"status $code","details":["detail $code"],"extensions":$extensions}}"""

    private fun sender(ksef: MockKsef) = KsefInvoiceSender(
        KsefSessionApi(ksef.client),
        tokenManager(ksef, keyPair),
        publicKeysOf(keyPair),
        pollInterval = 1.milliseconds,
        timeout = 500.milliseconds,
        upoAttempts = 3,
        upoRetryDelay = 1.milliseconds,
    )

    @Test
    fun `sends encrypted invoice, waits for KSeF number, downloads UPO and closes session`(): Unit = runBlocking {
        val ksef = ksef(listOf(status(100), status(150), status(200, ksefNumber = "9945082500-20261012-0000000000AA-00")))
        val submitted = mutableListOf<SubmittedInvoice>()

        val sent = sender(ksef).send(invoiceXml) { submitted += it }

        assertEquals("9945082500-20261012-0000000000AA-00", sent.ksefNumber)
        assertArrayEquals("<UPO>test</UPO>".toByteArray(), sent.upo)
        assertEquals(listOf(SubmittedInvoice("SO-1", "INV-1")), submitted)
        assertEquals(1, ksef.requestsTo("sessions/online/SO-1/close").size)

        // Session opened for FA(3) with the AES key wrapped by the MF key.
        val open = ksef.requestsTo("sessions/online").single().bodyJson()
        assertEquals("FA (3)", open["formCode"]!!.jsonObject["systemCode"]!!.jsonPrimitive.content)
        val encryption = open["encryption"]!!.jsonObject
        val key = SymmetricKey(
            unwrap(encryption["encryptedSymmetricKey"]!!.jsonPrimitive.content),
            Base64.getDecoder().decode(encryption["initializationVector"]!!.jsonPrimitive.content),
        )

        // The invoice is encrypted with that key; hashes and sizes describe plain and encrypted content.
        val sendBody = ksef.requestsTo("sessions/online/SO-1/invoices").single().bodyJson()
        val encrypted = Base64.getDecoder().decode(sendBody["encryptedInvoiceContent"]!!.jsonPrimitive.content)
        assertArrayEquals(invoiceXml, KsefCrypto.decryptAes256Cbc(encrypted, key))
        assertEquals(KsefCrypto.sha256Base64(invoiceXml), sendBody["invoiceHash"]!!.jsonPrimitive.content)
        assertEquals(invoiceXml.size.toLong(), sendBody["invoiceSize"]!!.jsonPrimitive.long)
        assertEquals(KsefCrypto.sha256Base64(encrypted), sendBody["encryptedInvoiceHash"]!!.jsonPrimitive.content)
        assertEquals(encrypted.size.toLong(), sendBody["encryptedInvoiceSize"]!!.jsonPrimitive.long)
    }

    @Test
    fun `reports rejected invoice and still closes the session`(): Unit = runBlocking {
        val ksef = ksef(listOf(status(450)))

        val e = assertThrows<KsefInvoiceRejectedException> { sender(ksef).send(invoiceXml) }

        assertEquals(450, e.statusCode)
        assertEquals(listOf("detail 450"), e.details)
        assertEquals(1, ksef.requestsTo("sessions/online/SO-1/close").size)
    }

    @Test
    fun `reports duplicate with the original KSeF number`(): Unit = runBlocking {
        val ksef = ksef(listOf(status(440, extensions = """{"originalKsefNumber":"9945082500-20261001-0000000000BB-00"}""")))

        val e = assertThrows<KsefInvoiceRejectedException> { sender(ksef).send(invoiceXml) }

        assertEquals(440, e.statusCode)
        assertEquals("9945082500-20261001-0000000000BB-00", e.extensions["originalKsefNumber"])
    }

    @Test
    fun `times out when KSeF keeps processing, result can be awaited later`(): Unit = runBlocking {
        val ksef = ksef(emptyList())
        val sender = sender(ksef)
        var submitted: SubmittedInvoice? = null

        assertThrows<KsefTimeoutException> { sender.send(invoiceXml) { submitted = it } }
        assertEquals(1, ksef.requestsTo("sessions/online/SO-1/close").size)

        val later = ksef(listOf(status(200, ksefNumber = "9945082500-20261012-0000000000CC-00")))
        assertEquals("9945082500-20261012-0000000000CC-00", sender(later).awaitResult(requireNotNull(submitted)).ksefNumber)
    }

    @Test
    fun `success status without a KSeF number is a protocol error, not a rejection`(): Unit = runBlocking {
        val ksef = ksef(listOf(status(200)))
        var submitted: SubmittedInvoice? = null

        assertThrows<KsefProtocolException> { sender(ksef).send(invoiceXml) { submitted = it } }

        // The invoice was submitted, so the caller keeps it pending and only checks the status again.
        assertEquals(SubmittedInvoice("SO-1", "INV-1"), submitted)
    }

    @Test
    fun `missing UPO does not fail the accepted invoice`(): Unit = runBlocking {
        val ksef = ksef(listOf(status(200, ksefNumber = "9945082500-20261012-0000000000DD-00")), upoStatus = HttpStatusCode.NotFound)

        val sent = sender(ksef).send(invoiceXml)

        assertEquals("9945082500-20261012-0000000000DD-00", sent.ksefNumber)
        assertNull(sent.upo)
    }

    @Test
    fun `UPO is retried after 404 and server errors until KSeF provides it`(): Unit = runBlocking {
        val ksef = ksef(
            listOf(status(200, ksefNumber = "9945082500-20261012-0000000000EE-00")),
            upoStatuses = listOf(HttpStatusCode.NotFound, HttpStatusCode.ServiceUnavailable, HttpStatusCode.OK),
        )

        val sent = sender(ksef).send(invoiceXml)

        assertArrayEquals("<UPO>test</UPO>".toByteArray(), sent.upo)
        assertEquals(3, ksef.requestsTo("sessions/SO-1/invoices/INV-1/upo").size)
    }

    @Test
    fun `UPO is not retried after a client error and gives up after the last attempt`(): Unit = runBlocking {
        val forbidden = ksef(listOf(status(200, ksefNumber = "9945082500-20261012-0000000000FF-00")), upoStatus = HttpStatusCode.Forbidden)
        assertNull(sender(forbidden).send(invoiceXml).upo)
        assertEquals(1, forbidden.requestsTo("sessions/SO-1/invoices/INV-1/upo").size)

        val unavailable = ksef(listOf(status(200, ksefNumber = "9945082500-20261012-0000000000GG-00")), upoStatus = HttpStatusCode.ServiceUnavailable)
        assertNull(sender(unavailable).send(invoiceXml).upo)
        assertEquals(3, unavailable.requestsTo("sessions/SO-1/invoices/INV-1/upo").size)
    }

    private fun unwrap(base64: String): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private, OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT))
        return cipher.doFinal(Base64.getDecoder().decode(base64))
    }
}
