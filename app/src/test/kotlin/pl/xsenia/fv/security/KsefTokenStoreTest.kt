package pl.xsenia.fv.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.ksef.KsefFailure
import pl.xsenia.fv.ksef.toKsefFailure
import java.io.File
import java.security.GeneralSecurityException

class KsefTokenStoreTest {

    @TempDir
    lateinit var dir: File

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Fictitious token in the KSeF format: reference|nip-NIP|key.
    private val token = "20261016-EU-0000000000-AAAAAAAAAA-00|nip-6691034274|0000000000000000000000000000000000000000000000000000000000000000"

    /** Reversible fake "encryption"; [lostKey] — like a Keystore key that disappeared. */
    private class FakeCipher(var lostKey: Boolean = false) : TokenCipher {
        override fun encrypt(plain: ByteArray) = plain.reversedArray()

        override fun decrypt(data: ByteArray): ByteArray {
            if (lostKey) throw GeneralSecurityException("Key permanently invalidated")
            return data.reversedArray()
        }
    }

    private fun dataStore() = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "settings.preferences_pb") }

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `saves the token encrypted and reads it back`() = runBlocking {
        val store = KsefTokenStore(dataStore(), FakeCipher())

        store.save("  $token ")

        assertEquals(token, store.read())
        assertEquals("6691034274", store.tokenNip())
    }

    @Test
    fun `no token is null, not an error`() = runBlocking {
        assertNull(KsefTokenStore(dataStore(), FakeCipher()).read())
    }

    @Test
    fun `a token that cannot be decrypted is reported, not thrown as a raw crypto error`() = runBlocking {
        val cipher = FakeCipher()
        val store = KsefTokenStore(dataStore(), cipher)
        store.save(token)

        cipher.lostKey = true

        val error = assertThrows<TokenUnreadableException> { store.read() }
        assertTrue(error.cause is GeneralSecurityException)
        assertEquals(KsefFailure.TokenUnreadable, error.toKsefFailure())
        // The stored data is not removed: the user decides (enters a new token).
        assertTrue(store.isSetNow())
    }

    @Test
    fun `corrupted stored data is reported as an unreadable token`() = runBlocking {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("ksef_token_encrypted")] = "!!! not base64 !!!" }

        assertThrows<TokenUnreadableException> { KsefTokenStore(dataStore, FakeCipher()).read() }
        Unit
    }

    private suspend fun KsefTokenStore.isSetNow() = isSet.let { it.first() }
}
