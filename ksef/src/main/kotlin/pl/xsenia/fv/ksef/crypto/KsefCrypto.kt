package pl.xsenia.fv.ksef.crypto

import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/** AES-256 key and 128-bit IV used to encrypt invoices and export packages (AES-256-CBC, PKCS#7). */
class SymmetricKey(val key: ByteArray, val iv: ByteArray) {
    init {
        require(key.size == 32) { "AES-256 key must have 32 bytes" }
        require(iv.size == 16) { "IV must have 16 bytes" }
    }
}

/** Cryptographic operations required by the KSeF API. */
object KsefCrypto {

    /**
     * RSA-OAEP with SHA-256 for both the digest and MGF1, as required by KSeF.
     *
     * The parameters must be explicit: the transformation `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`
     * uses SHA-1 for MGF1 by default (on the JVM and on Android), which KSeF rejects.
     */
    private val OAEP_SHA256 = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)

    fun encryptRsaOaepSha256(data: ByteArray, publicKey: PublicKey): ByteArray {
        require(publicKey is RSAPublicKey) { "Unsupported KSeF public key algorithm: ${publicKey.algorithm}" }
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256)
        return cipher.doFinal(data)
    }

    /** Encrypts `{ksefToken}|{timestampMs}` for `POST /auth/ksef-token`. */
    fun encryptKsefToken(ksefToken: String, challengeTimestampMs: Long, publicKey: PublicKey): ByteArray =
        encryptRsaOaepSha256("$ksefToken|$challengeTimestampMs".toByteArray(Charsets.UTF_8), publicKey)

    private val random = SecureRandom()

    fun generateSymmetricKey(): SymmetricKey =
        SymmetricKey(ByteArray(32).also(random::nextBytes), ByteArray(16).also(random::nextBytes))

    // PKCS5Padding in JCA is PKCS#7 for 16-byte blocks.
    fun encryptAes256Cbc(data: ByteArray, key: SymmetricKey): ByteArray = aes(Cipher.ENCRYPT_MODE, data, key)

    fun decryptAes256Cbc(data: ByteArray, key: SymmetricKey): ByteArray = aes(Cipher.DECRYPT_MODE, data, key)

    fun sha256Base64(data: ByteArray): String =
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data))

    private fun aes(mode: Int, data: ByteArray, key: SymmetricKey): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, SecretKeySpec(key.key, "AES"), IvParameterSpec(key.iv))
        return cipher.doFinal(data)
    }

    /** Reads the public key from a Base64-encoded DER X.509 certificate, as returned by KSeF. */
    fun publicKeyFromCertificate(certificateBase64: String): PublicKey {
        val der = Base64.getDecoder().decode(certificateBase64)
        return CertificateFactory.getInstance("X.509").generateCertificate(der.inputStream()).publicKey
    }
}
