package pl.xsenia.fv.issue

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.xsenia.fv.ksef.sending.SubmittedInvoice
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Invoice sent to KSeF whose result is not known yet — survives an app restart. */
@Serializable
data class PendingSend(val submitted: SubmittedInvoice, val invoiceNumber: String, val nip: String)

/**
 * Files of the invoice issuing flow in the app's private storage (not part of the invoice cache):
 * `issue/draft.json`, `issue/pending-send.json` and `upo/{ksefNumber}.xml`.
 */
class IssueStorage(filesDir: File) {

    private val issueDir = File(filesDir, "issue")
    private val upoDir = File(filesDir, "upo")
    private val json = Json { ignoreUnknownKeys = true }

    fun loadDraft(): InvoiceForm? = read("draft.json", InvoiceForm.serializer())

    fun saveDraft(form: InvoiceForm) = write("draft.json", InvoiceForm.serializer(), form)

    fun clearDraft() {
        File(issueDir, "draft.json").delete()
    }

    fun loadPending(): PendingSend? = read("pending-send.json", PendingSend.serializer())

    fun savePending(pending: PendingSend) = write("pending-send.json", PendingSend.serializer(), pending)

    fun clearPending() {
        File(issueDir, "pending-send.json").delete()
    }

    fun saveUpo(ksefNumber: String, upo: ByteArray) {
        require(KSEF_NUMBER.matches(ksefNumber)) { "Unexpected KSeF number format" }
        writeAtomically(File(upoDir, "$ksefNumber.xml"), upo)
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
    }
}
