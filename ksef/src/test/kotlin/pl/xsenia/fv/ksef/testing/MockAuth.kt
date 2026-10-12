package pl.xsenia.fv.ksef.testing

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKey
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import java.security.KeyPair

/** Successful KSeF authentication responses; returns null for non-auth requests. */
fun MockRequestHandleScope.authResponse(request: HttpRequestData): HttpResponseData? = when (request.url.encodedPath) {
    "/v2/auth/challenge" -> json("""{"challenge":"c","timestamp":"2026-10-11T15:05:54+00:00","timestampMs":1,"clientIp":"1.2.3.4"}""")
    "/v2/auth/ksef-token" -> json("""{"referenceNumber":"AU-1","authenticationToken":{"token":"auth","validUntil":"2099-01-01T00:00:00+00:00"}}""")
    "/v2/auth/AU-1" -> json("""{"status":{"code":200,"description":"ok"}}""")
    "/v2/auth/token/redeem" -> json(
        """{"accessToken":{"token":"access-1","validUntil":"2099-01-01T00:00:00+00:00"},
            "refreshToken":{"token":"refresh-1","validUntil":"2099-01-02T00:00:00+00:00"}}""",
    )
    else -> null
}

fun publicKeysOf(keyPair: KeyPair) = KsefPublicKeyProvider { KsefPublicKey("key-1", keyPair.public) }

fun tokenManager(ksef: MockKsef, keyPair: KeyPair): KsefTokenManager = KsefTokenManager(
    authenticator = KsefAuthenticator(ksef.client, publicKeysOf(keyPair)),
    nip = "9945082500",
    ksefToken = { "ksef-token" },
)

/** Invoice metadata JSON with fictitious test data. */
fun metadataJson(
    ksefNumber: String,
    invoiceNumber: String = "01/10/2026",
    permanentStorageDate: String = "2026-10-11T17:57:30+00:00",
    netAmount: String = "233",
    vatAmount: String = "53.59",
    grossAmount: String = "286.59",
) = """{"ksefNumber":"$ksefNumber","invoiceNumber":"$invoiceNumber","issueDate":"2026-10-11",
    "invoicingDate":"2026-10-11T17:57:25+00:00","acquisitionDate":"2026-10-11T17:57:26+00:00",
    "permanentStorageDate":"$permanentStorageDate",
    "seller":{"nip":"9945082500","name":"Jan Testowy"},
    "buyer":{"identifier":{"type":"Nip","value":"6691034274"},"name":"Testowa Sp. z o.o."},
    "netAmount":$netAmount,"grossAmount":$grossAmount,"vatAmount":$vatAmount,"currency":"PLN",
    "invoicingMode":"Online","invoiceType":"Vat","formCode":{"systemCode":"FA (3)","schemaVersion":"1-0E","value":"FA"},
    "isSelfInvoicing":false,"hasAttachment":false,"invoiceHash":"aGFzaA=="}"""
