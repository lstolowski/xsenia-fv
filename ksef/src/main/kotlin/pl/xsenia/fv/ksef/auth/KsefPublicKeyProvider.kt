package pl.xsenia.fv.ksef.auth

import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.crypto.KsefCrypto
import java.security.PublicKey
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Purpose of a Ministry of Finance public key, as reported by `GET /security/public-key-certificates`. */
enum class PublicKeyUsage(val apiValue: String) {
    KSEF_TOKEN_ENCRYPTION("KsefTokenEncryption"),
    SYMMETRIC_KEY_ENCRYPTION("SymmetricKeyEncryption"),
}

class KsefPublicKey(val id: String?, val key: PublicKey)

fun interface KsefPublicKeyProvider {
    suspend fun publicKey(usage: PublicKeyUsage): KsefPublicKey

    companion object {
        /** Provider reading Ministry of Finance certificates from KSeF (fetched once, then cached). */
        fun fromApi(client: HttpClient): KsefPublicKeyProvider = ApiPublicKeyProvider(KsefAuthApi(client))
    }
}

/**
 * Fetches the KSeF public key certificates and picks the newest one currently valid for a given usage.
 *
 * The list is cached, but not for ever: it is fetched again after [ttl] and whenever it has no currently valid
 * certificate for the usage (MF may publish a new certificate before the old one expires). If a refresh fails while
 * the cached list still has a valid certificate, the cached one is used.
 */
internal class ApiPublicKeyProvider(
    private val api: KsefAuthApi,
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofHours(24),
) : KsefPublicKeyProvider {

    private val mutex = Mutex()
    private var certificates: List<PublicKeyCertificateDto>? = null
    private var fetchedAt: Instant = Instant.MIN

    override suspend fun publicKey(usage: PublicKeyUsage): KsefPublicKey {
        val certificate = mutex.withLock { currentCertificate(usage) }
            ?: throw KsefProtocolException("No valid KSeF public key certificate for usage ${usage.apiValue}")
        return try {
            KsefPublicKey(certificate.publicKeyId, KsefCrypto.publicKeyFromCertificate(certificate.certificate))
        } catch (e: Exception) {
            throw KsefProtocolException("Invalid KSeF public key certificate for usage ${usage.apiValue}", e)
        }
    }

    private suspend fun currentCertificate(usage: PublicKeyUsage): PublicKeyCertificateDto? {
        val cached = certificates
        val expired = cached == null || Duration.between(fetchedAt, clock.instant()) >= ttl
        if (!expired) pick(cached.orEmpty(), usage)?.let { return it }

        // Expired list or no valid certificate in it: ask KSeF; keep using the old list when KSeF cannot be reached.
        val fresh = try {
            api.publicKeyCertificates().also {
                certificates = it
                fetchedAt = clock.instant()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached == null) throw e
            cached
        }
        return pick(fresh, usage)
    }

    private fun pick(all: List<PublicKeyCertificateDto>, usage: PublicKeyUsage): PublicKeyCertificateDto? {
        val now = clock.instant()
        return all
            .filter { usage.apiValue in it.usage && !now.isBefore(it.validFrom) && now.isBefore(it.validTo) }
            .maxByOrNull { it.validFrom }
    }
}
