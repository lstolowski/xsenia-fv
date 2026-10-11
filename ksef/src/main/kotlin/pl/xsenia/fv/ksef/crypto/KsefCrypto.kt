package pl.xsenia.fv.ksef.crypto

import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

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

    /** Reads the public key from a Base64-encoded DER X.509 certificate, as returned by KSeF. */
    fun publicKeyFromCertificate(certificateBase64: String): PublicKey {
        val der = Base64.getDecoder().decode(certificateBase64)
        return CertificateFactory.getInstance("X.509").generateCertificate(der.inputStream()).publicKey
    }
}
