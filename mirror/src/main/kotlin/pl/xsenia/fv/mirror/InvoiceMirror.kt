package pl.xsenia.fv.mirror

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Local copy of KSeF invoices in plain files (ADR 0003) — no database:
 *
 * ```
 * <root>/sync-state.json
 * <root>/{sales|purchase}/index.json
 * <root>/{sales|purchase}/xml/{ksefNumber}.xml
 * ```
 *
 * Every file is written atomically (temporary file + rename). XML files are written before the index,
 * so a crash can leave only an unreferenced XML, never an index entry without its XML.
 * Everything can be deleted and re-downloaded from KSeF.
 */
class InvoiceMirror(private val root: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val entriesSerializer = ListSerializer(MirrorEntry.serializer())

    fun entries(category: InvoiceCategory): List<MirrorEntry> {
        val file = indexFile(category)
        if (!file.isFile) return emptyList()
        return json.decodeFromString(entriesSerializer, file.readText(Charsets.UTF_8))
    }

    fun ksefNumbers(category: InvoiceCategory): Set<String> = entries(category).mapTo(mutableSetOf()) { it.metadata.ksefNumber }

    fun readXml(category: InvoiceCategory, ksefNumber: String): ByteArray? =
        xmlFile(category, ksefNumber).takeIf { it.isFile }?.readBytes()

    /** Adds invoices (with their XML); entries with an already known KSeF number are replaced. */
    fun add(category: InvoiceCategory, invoices: List<Pair<MirrorEntry, ByteArray>>) {
        if (invoices.isEmpty()) return
        invoices.forEach { (entry, xml) -> writeAtomically(xmlFile(category, entry.metadata.ksefNumber), xml) }
        val byNumber = LinkedHashMap<String, MirrorEntry>()
        entries(category).forEach { byNumber[it.metadata.ksefNumber] = it }
        invoices.forEach { (entry, _) -> byNumber[entry.metadata.ksefNumber] = entry }
        val sorted = byNumber.values.sortedBy { it.metadata.permanentStorageDate }
        writeAtomically(indexFile(category), json.encodeToString(entriesSerializer, sorted).toByteArray(Charsets.UTF_8))
    }

    fun syncState(): SyncState {
        val file = stateFile()
        if (!file.isFile) return SyncState()
        return json.decodeFromString(SyncState.serializer(), file.readText(Charsets.UTF_8))
    }

    fun saveSyncState(state: SyncState) =
        writeAtomically(stateFile(), json.encodeToString(SyncState.serializer(), state).toByteArray(Charsets.UTF_8))

    fun clear() {
        root.deleteRecursively()
    }

    private fun categoryDir(category: InvoiceCategory) = File(root, category.directoryName)

    private fun indexFile(category: InvoiceCategory) = File(categoryDir(category), "index.json")

    // KSeF numbers contain only digits, letters and dashes — safe as file names.
    private fun xmlFile(category: InvoiceCategory, ksefNumber: String): File {
        require(KSEF_NUMBER.matches(ksefNumber)) { "Unexpected KSeF number format: $ksefNumber" }
        return File(File(categoryDir(category), "xml"), "$ksefNumber.xml")
    }

    private fun stateFile() = File(root, "sync-state.json")

    private fun writeAtomically(target: File, content: ByteArray) {
        target.parentFile.mkdirs()
        val temporary = File(target.parentFile, target.name + ".tmp")
        temporary.writeBytes(content)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private companion object {
        val KSEF_NUMBER = Regex("[0-9A-Za-z-]{1,64}")
    }
}
