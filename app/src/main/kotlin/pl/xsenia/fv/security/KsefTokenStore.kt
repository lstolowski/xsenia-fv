package pl.xsenia.fv.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64

/** Keeps the KSeF token encrypted with a Keystore key; only the context NIP is stored in plain text. */
class KsefTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {

    val isSet: Flow<Boolean> = dataStore.data.map { it[ENCRYPTED_TOKEN] != null }

    /**
     * NIP embedded in the token (`…|nip-XXXXXXXXXX|…`) — the KSeF context and the active company profile (ADR 0006).
     * Not secret, shown in settings. null without a token (or for a token saved by an older version without a NIP).
     */
    val tokenNip: Flow<String?> = dataStore.data.map { it[TOKEN_NIP] }

    suspend fun tokenNip(): String? = tokenNip.first()

    /** The token must contain the company NIP ([nipOf]) — tokens of other contexts are not supported. */
    suspend fun save(ksefToken: String) {
        val token = ksefToken.trim()
        val nip = requireNotNull(nipOf(token)) { "KSeF token has no company NIP" }
        val encrypted = Base64.getEncoder().encodeToString(cipher.encrypt(token.toByteArray(Charsets.UTF_8)))
        dataStore.edit {
            it[ENCRYPTED_TOKEN] = encrypted
            it[TOKEN_NIP] = nip
        }
    }

    /**
     * The decrypted token, or null when none is stored.
     *
     * @throws TokenUnreadableException when the stored token cannot be decrypted (corrupted data, or the Keystore key
     *   is gone) — the user has to enter the token again; the stored data is left as it is
     */
    suspend fun read(): String? {
        val encrypted = dataStore.data.first()[ENCRYPTED_TOKEN] ?: return null
        return try {
            String(cipher.decrypt(Base64.getDecoder().decode(encrypted)), Charsets.UTF_8)
        } catch (e: Exception) {
            throw TokenUnreadableException(e)
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(ENCRYPTED_TOKEN)
            it.remove(TOKEN_NIP)
        }
    }

    companion object {
        private val ENCRYPTED_TOKEN = stringPreferencesKey("ksef_token_encrypted")
        private val TOKEN_NIP = stringPreferencesKey("ksef_token_nip")
        private val NIP_IN_TOKEN = Regex("\\|nip-(\\d{10})\\|")

        fun nipOf(ksefToken: String): String? = NIP_IN_TOKEN.find(ksefToken)?.groupValues?.get(1)
    }
}

/** The stored KSeF token cannot be decrypted (corrupted data or lost Keystore key). */
class TokenUnreadableException(cause: Throwable) : Exception("Stored KSeF token cannot be read", cause)
