package pl.xsenia.fv.settings

import android.content.ContentResolver
import android.content.Intent
import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    /** The monthly "no sales invoice" reminder (US-017); on by default. */
    val reminderEnabled: Flow<Boolean> = dataStore.data.map { it[REMINDER_ENABLED] ?: true }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    /** Month (`yyyy-MM`) the reminder was last shown for — it is shown at most once per month. */
    suspend fun lastReminderMonth(): String? = dataStore.data.first()[LAST_REMINDER_MONTH]

    suspend fun saveLastReminderMonth(month: String) {
        dataStore.edit { it[LAST_REMINDER_MONTH] = month }
    }

    /** Whether the system notification permission has already been requested once (the first start asks only once). */
    suspend fun notificationPermissionAsked(): Boolean = dataStore.data.first()[NOTIFICATION_PERMISSION_ASKED] ?: false

    suspend fun markNotificationPermissionAsked() {
        dataStore.edit { it[NOTIFICATION_PERMISSION_ASKED] = true }
    }

    private companion object {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val LAST_REMINDER_MONTH = stringPreferencesKey("reminder_last_month")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val LEGACY_CONFIG_FOLDER_URI = stringPreferencesKey("config_folder_uri")
    }
}
