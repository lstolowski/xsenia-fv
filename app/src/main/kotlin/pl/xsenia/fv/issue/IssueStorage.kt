package pl.xsenia.fv.issue

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pl.xsenia.fv.ksef.sending.SubmittedInvoice
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Invoice sent to KSeF whose result is not known yet — survives an app restart. */
@Serializable
data class PendingSend(
    val submitted: SubmittedInvoice,
    val invoiceNumber: String,
    val nip: String,
    /** Form of the invoice — needed to keep it as unsent if KSeF rejects it after an app restart. */
    val form: InvoiceForm? = null,
    /** Set when an unsent invoice (US-007) is being sent; it is removed once KSeF accepts the invoice. */
    val unsentId: String? = null,
)

/** An accepted invoice whose UPO KSeF had not provided yet — fetched again later (REV-07). */
@Serializable
data class MissingUpo(
    val ksefNumber: String,
    val nip: String,
    val submitted: SubmittedInvoice,
    /** How many times KSeF answered without the UPO since; the entry is dropped after too many. */
    val attempts: Int = 0,
)

/**
 * Files of the invoice issuing flow in the app's private storage (not part of the invoice cache):
 * `issue/draft.json`, `issue/pending-send.json`, `issue/missing-upo.json` and `upo/{ksefNumber}.xml`.
 */
class IssueStorage(filesDir: File) {

    private val issueDir = File(filesDir, "issue")
    private val upoDir = File(filesDir, "upo")
    private val json = Json { ignoreUnknownKeys = true }

    fun loadDraft(): InvoiceForm? = read("draft.json", InvoiceForm.serializer())

    fun saveDraft(form: InvoiceForm) = write("draft.json", InvoiceForm.serializer(), form)

    /** @throws IOException when the draft exists and cannot be deleted — it would come back on the next launch */
    fun clearDraft() = File(issueDir, "draft.json").deleteOrThrow()

    fun loadPending(): PendingSend? = read("pending-send.json", PendingSend.serializer())

    fun savePending(pending: PendingSend) = write("pending-send.json", PendingSend.serializer(), pending)

    /** @throws IOException when the file exists and cannot be deleted */
    fun clearPending() = File(issueDir, "pending-send.json").deleteOrThrow()

    fun saveUpo(ksefNumber: String, upo: ByteArray) {
        require(KSEF_NUMBER.matches(ksefNumber)) { "Unexpected KSeF number format" }
        writeAtomically(File(upoDir, "$ksefNumber.xml"), upo)
    }

    fun loadMissingUpos(): List<MissingUpo> = read("missing-upo.json", MISSING_UPO_LIST).orEmpty()

    /** Adds [missing], or replaces the entry of the same invoice. */
    @Synchronized
    fun saveMissingUpo(missing: MissingUpo) =
        write("missing-upo.json", MISSING_UPO_LIST, loadMissingUpos().filter { it.ksefNumber != missing.ksefNumber } + missing)

    @Synchronized
    fun removeMissingUpo(ksefNumber: String) {
        val rest = loadMissingUpos().filter { it.ksefNumber != ksefNumber }
        if (rest.isEmpty()) File(issueDir, "missing-upo.json").delete() else write("missing-upo.json", MISSING_UPO_LIST, rest)
    }

    private fun <T> read(name: String, serializer: KSerializer<T>): T? {
        val file = File(issueDir, name)
        if (!file.isFile) return null
        // A corrupted file must not block issuing invoices.
        return runCatching { json.decodeFromString(serializer, file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    private fun <T> write(name: String, serializer: KSerializer<T>, value: T) =
        writeAtomically(File(issueDir, name), json.encodeToString(serializer, value).toByteArray(Charsets.UTF_8))

    private fun writeAtomically(target: File, content: ByteArray) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, target.name + ".tmp")
        temporary.writeBytes(content)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private companion object {
        val KSEF_NUMBER = Regex("[0-9A-Za-z-]{1,64}")
        val MISSING_UPO_LIST = ListSerializer(MissingUpo.serializer())
    }
}

/** Deletes the file; a missing file is fine, but a file that stays would silently come back after a restart. */
internal fun File.deleteOrThrow() {
    if (!delete() && exists()) throw IOException("Cannot delete $name")
}
