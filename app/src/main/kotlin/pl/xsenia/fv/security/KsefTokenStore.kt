package pl.xsenia.fv.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Keeps the KSeF token encrypted with a Keystore key; only the context NIP is stored in plain text. */
class KsefTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) {

    val isSet: Flow<Boolean> = dataStore.data.map { it[ENCRYPTED_TOKEN] != null }

    /** NIP embedded in the token (`…|nip-XXXXXXXXXX|…`), if any — not secret, shown in settings. */
    val tokenNip: Flow<String?> = dataStore.data.map { it[TOKEN_NIP] }

    suspend fun save(ksefToken: String) {
        val token = ksefToken.trim()
        require(token.isNotEmpty()) { "KSeF token must not be blank" }
        val encrypted = Base64.encodeToString(cipher.encrypt(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        dataStore.edit {
            it[ENCRYPTED_TOKEN] = encrypted
            val nip = nipOf(token)
            if (nip != null) it[TOKEN_NIP] = nip else it.remove(TOKEN_NIP)
        }
    }

    suspend fun read(): String? {
        val encrypted = dataStore.data.first()[ENCRYPTED_TOKEN] ?: return null
        return String(cipher.decrypt(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
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
