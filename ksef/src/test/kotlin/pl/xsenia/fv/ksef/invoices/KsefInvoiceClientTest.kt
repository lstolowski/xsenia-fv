package pl.xsenia.fv.ksef.invoices

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import pl.xsenia.fv.ksef.crypto.SymmetricKey
import pl.xsenia.fv.ksef.testing.MockKsef
import pl.xsenia.fv.ksef.testing.authResponse
import pl.xsenia.fv.ksef.testing.json
import pl.xsenia.fv.ksef.testing.metadataJson
import pl.xsenia.fv.ksef.testing.publicKeysOf
import pl.xsenia.fv.ksef.testing.rsaKeyPair
import pl.xsenia.fv.ksef.testing.tokenManager
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.security.spec.MGF1ParameterSpec
import java.time.Instant
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.time.Duration.Companion.milliseconds

// runBlocking: MockEngine answers on its own dispatcher (see KsefAuthenticatorTest).
class KsefInvoiceClientTest {

    private val keyPair = rsaKeyPair()

    private fun client(ksef: MockKsef) = KsefInvoiceClient(
        KsefInvoicesApi(ksef.client),
        tokenManager(ksef, keyPair),
        publicKeysOf(keyPair),
        exportPollInterval = 1.milliseconds,
    )

    private fun HttpRequestData.bodyJson() =
        Json.parseToJsonElement(String((body as OutgoingContent.ByteArrayContent).bytes(), Charsets.UTF_8)).jsonObject

    private fun page(hasMore: Boolean, isTruncated: Boolean = false, vararg invoices: String) =
        """{"hasMore":$hasMore,"isTruncated":$isTruncated,"invoices":[${invoices.joinToString(",")}]}"""

    @Test
    fun `follows pagination and reads amounts as exact decimals`(): Unit = runBlocking {
        val ksef = MockKsef { request ->
            authResponse(request) ?: when (request.url.parameters["pageOffset"]) {
                "0" -> json(page(hasMore = true, invoices = arrayOf(metadataJson("A"))))
                else -> json(page(hasMore = false, invoices = arrayOf(metadataJson("B", vatAmount = "0.1", netAmount = "0.2", grossAmount = "0.3"))))
            }
        }

        val invoices = client(ksef).queryMetadata(
            InvoiceSubjectType.SELLER,
            InvoiceDateType.ISSUE,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-11-01T00:00:00Z"),
        )

        assertEquals(listOf("A", "B"), invoices.map { it.ksefNumber })
        assertEquals(BigDecimal("53.59"), invoices[0].vatAmount)
        assertEquals(BigDecimal("0.3"), invoices[1].grossAmount)

        val requests = ksef.requestsTo("invoices/query/metadata")
        assertEquals(listOf("0", "1"), requests.map { it.url.parameters["pageOffset"] })
        assertEquals("Asc", requests[0].url.parameters["sortOrder"])
        assertEquals("250", requests[0].url.parameters["pageSize"])
        val filters = requests[0].bodyJson()
        assertEquals("Subject1", filters["subjectType"]!!.jsonPrimitive.content)
        assertEquals("Issue", filters["dateRange"]!!.jsonObject["dateType"]!!.jsonPrimitive.content)
        assertEquals("Bearer access-1", requests[0].headers["Authorization"])
    }

    @Test
    fun `splits long ranges into windows of at most 90 days and removes duplicates`(): Unit = runBlocking {
        val ksef = MockKsef { request ->
            authResponse(request) ?: json(page(hasMore = false, invoices = arrayOf(metadataJson("SAME"))))
        }

        val invoices = client(ksef).queryMetadata(
            InvoiceSubjectType.BUYER,
            InvoiceDateType.PERMANENT_STORAGE,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-10-18T00:00:00Z"),
        )

        assertEquals(1, invoices.size)
        val ranges = ksef.requestsTo("invoices/query/metadata").map {
            val range = it.bodyJson()["dateRange"]!!.jsonObject
            range["from"]!!.jsonPrimitive.content to range["to"]!!.jsonPrimitive.content
        }
        assertEquals(
            listOf(
                "2026-04-01T00:00:00Z" to "2026-06-30T00:00:00Z",
                "2026-06-30T00:00:00Z" to "2026-09-28T00:00:00Z",
                "2026-09-28T00:00:00Z" to "2026-10-18T00:00:00Z",
            ),
            ranges,
        )
        assertEquals("Subject2", ksef.requestsTo("invoices/query/metadata")[0].bodyJson()["subjectType"]!!.jsonPrimitive.content)
    }

    @Test
    fun `continues from the last record when the result set is truncated`(): Unit = runBlocking {
        var call = 0
        val ksef = MockKsef { request ->
            authResponse(request) ?: when (call++) {
                0 -> json(page(hasMore = true, isTruncated = true, metadataJson("A"), metadataJson("B", permanentStorageDate = "2026-10-05T10:00:00+00:00")))
                else -> json(page(hasMore = false, invoices = arrayOf(metadataJson("B"), metadataJson("C"))))
            }
        }

        val invoices = client(ksef).queryMetadata(
            InvoiceSubjectType.SELLER,
            InvoiceDateType.PERMANENT_STORAGE,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-11T00:00:00Z"),
        )

        assertEquals(listOf("A", "B", "C"), invoices.map { it.ksefNumber })
        val second = ksef.requestsTo("invoices/query/metadata")[1]
        assertEquals("0", second.url.parameters["pageOffset"])
        assertEquals("2026-10-05T10:00:00Z", second.bodyJson()["dateRange"]!!.jsonObject["from"]!!.jsonPrimitive.content)
    }

    @Test
    fun `re-authenticates once after HTTP 401`(): Unit = runBlocking {
        var unauthorizedSent = false
        val ksef = MockKsef { request ->
            authResponse(request) ?: if (!unauthorizedSent) {
                unauthorizedSent = true
                json("", HttpStatusCode.Unauthorized)
            } else {
                json(page(hasMore = false))
            }
        }

        client(ksef).queryMetadata(
            InvoiceSubjectType.SELLER,
            InvoiceDateType.ISSUE,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-02T00:00:00Z"),
        )

        assertEquals(2, ksef.requestsTo("auth/ksef-token").size)
        assertEquals(2, ksef.requestsTo("invoices/query/metadata").size)
    }

    @Test
    fun `downloads invoice XML and verifies its hash`(): Unit = runBlocking {
        val xml = "<Faktura><P_2>01/10/2026</P_2></Faktura>".toByteArray()
        var hash = KsefCrypto.sha256Base64(xml)
        val ksef = MockKsef { request ->
            authResponse(request) ?: respond(xml, HttpStatusCode.OK, headersOf("x-ms-meta-hash", hash))
        }
        val client = client(ksef)

        val invoice = client.downloadInvoiceXml("9945082500-20261011-0000000000AA-00")

        assertArrayEquals(xml, invoice.content)
        assertEquals("/v2/invoices/ksef/9945082500-20261011-0000000000AA-00", ksef.requests.last().url.encodedPath)

        hash = "d3Jvbmc="
        assertThrows<KsefProtocolException> { client.downloadInvoiceXml("9945082500-20261011-0000000000AA-00") }
    }

    @Test
    fun `exports, downloads, decrypts and unpacks a package`(): Unit = runBlocking {
        val xmlA = "<Faktura>A</Faktura>".toByteArray()
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use {
                it.putNextEntry(ZipEntry("KSEF-A.xml")); it.write(xmlA); it.closeEntry()
                it.putNextEntry(ZipEntry("_metadata.json")); it.write("""{"invoices":[${metadataJson("KSEF-A")}]}""".toByteArray()); it.closeEntry()
            }
        }.toByteArray()
        // Two parts, each encrypted separately with the same key and IV.
        val chunks = listOf(zip.copyOfRange(0, zip.size / 2), zip.copyOfRange(zip.size / 2, zip.size))
        var encryptedChunks: List<ByteArray> = emptyList()
        var statusCalls = 0

        val ksef = MockKsef { request ->
            authResponse(request) ?: when (request.url.encodedPath) {
                "/v2/invoices/exports" -> {
                    val encryption = request.bodyJson()["encryption"]!!.jsonObject
                    val key = SymmetricKey(
                        decryptRsa(Base64.getDecoder().decode(encryption["encryptedSymmetricKey"]!!.jsonPrimitive.content)),
                        Base64.getDecoder().decode(encryption["initializationVector"]!!.jsonPrimitive.content),
                    )
                    encryptedChunks = chunks.map { KsefCrypto.encryptAes256Cbc(it, key) }
                    json("""{"referenceNumber":"EX-1"}""", HttpStatusCode.Created)
                }
                "/v2/invoices/exports/EX-1" ->
                    if (statusCalls++ == 0) {
                        json("""{"status":{"code":100,"description":"w toku"}}""")
                    } else {
                        val parts = encryptedChunks.mapIndexed { i, encrypted ->
                            """{"ordinalNumber":${i + 1},"partName":"p$i","method":"GET","url":"https://storage.example.com/part$i",
                               "partSize":${chunks[i].size},"partHash":"${KsefCrypto.sha256Base64(chunks[i])}",
                               "encryptedPartSize":${encrypted.size},"encryptedPartHash":"${KsefCrypto.sha256Base64(encrypted)}",
                               "expirationDate":"2099-01-01T00:00:00+00:00"}"""
                        }
                        json(
                            """{"status":{"code":200,"description":"ok"},"package":{"invoiceCount":1,"size":${zip.size},
                               "parts":[${parts.reversed().joinToString(",")}],"isTruncated":false,
                               "lastPermanentStorageDate":"2026-10-11T17:57:30+00:00","compressionType":"Zip"}}""",
                        )
                    }
                "/part0" -> respond(encryptedChunks[0])
                "/part1" -> respond(encryptedChunks[1])
                else -> error("Unexpected request ${request.url}")
            }
        }

        val export = client(ksef).exportInvoices(
            InvoiceSubjectType.SELLER,
            InvoiceDateType.PERMANENT_STORAGE,
            Instant.parse("2026-10-01T00:00:00Z"),
            restrictToPermanentStorageHwmDate = true,
        )

        assertEquals(listOf("KSEF-A"), export.invoices.map { it.ksefNumber })
        assertArrayEquals(xmlA, export.xmlByKsefNumber["KSEF-A"])
        assertFalse(export.isTruncated)
        assertEquals(Instant.parse("2026-10-11T17:57:30Z"), export.lastPermanentStorageDate)

        val exportRequest = ksef.requestsTo("invoices/exports").single()
        assertEquals("key-1", exportRequest.bodyJson()["encryption"]!!.jsonObject["publicKeyId"]!!.jsonPrimitive.content)
        assertTrue(exportRequest.bodyJson()["filters"]!!.jsonObject["dateRange"]!!.jsonObject["restrictToPermanentStorageHwmDate"]!!.jsonPrimitive.content.toBoolean())
        // Pre-signed part URLs must not receive the access token.
        assertTrue(ksef.requests.filter { it.url.host == "storage.example.com" }.none { it.headers["Authorization"] != null })
    }

    @Test
    fun `reports failed export`(): Unit = runBlocking {
        val ksef = MockKsef { request ->
            authResponse(request) ?: when (request.url.encodedPath) {
                "/v2/invoices/exports" -> json("""{"referenceNumber":"EX-2"}""", HttpStatusCode.Created)
                else -> json("""{"status":{"code":415,"description":"Błąd odszyfrowania dostarczonego klucza"}}""")
            }
        }

        val e = assertThrows<KsefProtocolException> {
            client(ksef).exportInvoices(InvoiceSubjectType.SELLER, InvoiceDateType.PERMANENT_STORAGE, Instant.parse("2026-10-01T00:00:00Z"))
        }
        assertTrue(e.message!!.contains("415"))
    }

    @Test
    fun `metadata survives JSON round trip for the local mirror`() {
        val json = Json { ignoreUnknownKeys = true }
        val metadata = json.decodeFromString<InvoiceMetadata>(metadataJson("A", vatAmount = "53.59"))

        val encoded = json.encodeToString(InvoiceMetadata.serializer(), metadata)

        assertTrue(encoded.contains("\"vatAmount\":53.59"), encoded)
        assertEquals(metadata, json.decodeFromString<InvoiceMetadata>(encoded))
    }

    private fun decryptRsa(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyPair.private,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
        )
        return cipher.doFinal(data)
    }
}
