package pl.xsenia.fv.config

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pl.xsenia.fv.invoice.config.ConfigFile
import pl.xsenia.fv.invoice.config.InvoicingConfig
import pl.xsenia.fv.settings.AppSettings

sealed interface ConfigState {
    data object Loading : ConfigState
    data object FolderNotSelected : ConfigState

    /** Folder was selected but is no longer accessible (moved, deleted or permission revoked). */
    data object FolderInaccessible : ConfigState

    data class Loaded(val folderName: String, val config: InvoicingConfig) : ConfigState
}

/** Reads the user-edited JSON files from the folder chosen with the Storage Access Framework (ADR 0004). */
class ConfigRepository(
    private val context: Context,
    private val settings: AppSettings,
) {

    private val _state = MutableStateFlow<ConfigState>(ConfigState.Loading)
    val state: StateFlow<ConfigState> = _state.asStateFlow()

    /** Remembers the folder picked with `ACTION_OPEN_DOCUMENT_TREE` and loads its files. */
    suspend fun selectFolder(treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        settings.setConfigFolderUri(treeUri.toString())
        reload()
    }

    suspend fun reload() {
        _state.value = withContext(Dispatchers.IO) { load() }
    }

    private suspend fun load(): ConfigState {
        val uri = settings.configFolderUri()?.let(Uri::parse) ?: return ConfigState.FolderNotSelected
        val hasPermission = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        val folder = DocumentFile.fromTreeUri(context, uri)
        if (!hasPermission || folder == null || !folder.isDirectory) return ConfigState.FolderInaccessible

        val config = InvoicingConfig.parse(
            companyJson = readFile(folder, ConfigFile.COMPANY),
            banksJson = readFile(folder, ConfigFile.BANKS),
            counterpartiesJson = readFile(folder, ConfigFile.COUNTERPARTIES),
        )
        return ConfigState.Loaded(folder.name ?: uri.lastPathSegment.orEmpty(), config)
    }

    private fun readFile(folder: DocumentFile, file: ConfigFile): String? {
        val document = folder.findFile(file.fileName)?.takeIf { it.isFile } ?: return null
        return context.contentResolver.openInputStream(document.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
