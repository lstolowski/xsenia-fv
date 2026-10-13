package pl.xsenia.fv.settings

import android.content.ContentResolver
import android.content.Intent
import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Non-secret app settings. */
class AppSettings(private val dataStore: DataStore<Preferences>) {

    /**
     * The shared folder with company.json, banks.json and counterparties.json is no longer used (ADR 0006):
     * releases its persisted read permission and forgets it. The files in the folder are left untouched.
     */
    suspend fun forgetLegacyConfigFolder(contentResolver: ContentResolver) {
        val uri = dataStore.data.first()[LEGACY_CONFIG_FOLDER_URI] ?: return
        runCatching { contentResolver.releasePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        dataStore.edit { it.remove(LEGACY_CONFIG_FOLDER_URI) }
    }

    private companion object {
        val LEGACY_CONFIG_FOLDER_URI = stringPreferencesKey("config_folder_uri")
    }
}
