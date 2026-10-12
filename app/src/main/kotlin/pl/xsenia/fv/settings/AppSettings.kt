package pl.xsenia.fv.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Non-secret app settings. */
class AppSettings(private val dataStore: DataStore<Preferences>) {

    /** Tree URI of the folder with company.json, banks.json and counterparties.json (ADR 0004). */
    val configFolderUri: Flow<String?> = dataStore.data.map { it[CONFIG_FOLDER_URI] }

    suspend fun configFolderUri(): String? = configFolderUri.first()

    suspend fun setConfigFolderUri(uri: String) {
        dataStore.edit { it[CONFIG_FOLDER_URI] = uri }
    }

    private companion object {
        val CONFIG_FOLDER_URI = stringPreferencesKey("config_folder_uri")
    }
}
