package pl.xsenia.fv.ksef.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import java.security.PublicKey
import java.time.Clock

/** Purpose of a Ministry of Finance public key, as reported by `GET /security/public-key-certificates`. */
enum class PublicKeyUsage(val apiValue: String) {
    KSEF_TOKEN_ENCRYPTION("KsefTokenEncryption"),
    SYMMETRIC_KEY_ENCRYPTION("SymmetricKeyEncryption"),
}

class KsefPublicKey(val id: String?, val key: PublicKey)

fun interface KsefPublicKeyProvider {
    suspend fun publicKey(usage: PublicKeyUsage): KsefPublicKey
}

/** Fetches the KSeF public key certificates once and picks the newest one currently valid for a given usage. */
internal class ApiPublicKeyProvider(
    private val api: KsefAuthApi,
    private val clock: Clock = Clock.systemUTC(),
) : KsefPublicKeyProvider {

    private val mutex = Mutex()
    private var certificates: List<PublicKeyCertificateDto>? = null

    override suspend fun publicKey(usage: PublicKeyUsage): KsefPublicKey {
        val all = mutex.withLock { certificates ?: api.publicKeyCertificates().also { certificates = it } }
        val now = clock.instant()
        val certificate = all
            .filter { usage.apiValue in it.usage && !now.isBefore(it.validFrom) && now.isBefore(it.validTo) }
            .maxByOrNull { it.validFrom }
            ?: throw KsefProtocolException("No valid KSeF public key certificate for usage ${usage.apiValue}")
        return try {
            KsefPublicKey(certificate.publicKeyId, KsefCrypto.publicKeyFromCertificate(certificate.certificate))
        } catch (e: Exception) {
            throw KsefProtocolException("Invalid KSeF public key certificate for usage ${usage.apiValue}", e)
        }
    }
}
