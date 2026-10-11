package pl.xsenia.fv.ksef.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.xsenia.fv.ksef.testing.resourceText
import pl.xsenia.fv.ksef.testing.rsaKeyPair
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class KsefCryptoTest {

    private val keyPair = rsaKeyPair()

    private fun decrypt(data: ByteArray, mgf1: MGF1ParameterSpec): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private, OAEPParameterSpec("SHA-256", "MGF1", mgf1, PSource.PSpecified.DEFAULT))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    @Test
    fun `encrypts KSeF token with timestamp using RSA-OAEP SHA-256 and MGF1 SHA-256`() {
        val encrypted = KsefCrypto.encryptKsefToken("ref|nip-1111111111|secret", 1791731154717, keyPair.public)

        assertEquals("ref|nip-1111111111|secret|1791731154717", decrypt(encrypted, MGF1ParameterSpec.SHA256))
    }

    @Test
    fun `ciphertext is not compatible with the default MGF1 SHA-1 padding`() {
        val encrypted = KsefCrypto.encryptKsefToken("token", 1, keyPair.public)

        assertThrows<Exception> { decrypt(encrypted, MGF1ParameterSpec.SHA1) }
    }

    @Test
    fun `reads RSA public key from KSeF certificate`() {
        val certificate = Regex("\"certificate\": \"([^\"]+)\"").find(resourceText("public-key-certificates-test.json"))!!.groupValues[1]

        val key = KsefCrypto.publicKeyFromCertificate(certificate)

        assertTrue(key is RSAPublicKey)
        assertEquals(2048, (key as RSAPublicKey).modulus.bitLength())
    }
}
