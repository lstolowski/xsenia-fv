package pl.xsenia.fv.issue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.xsenia.fv.ksef.http.InstantSerializer
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID

/** Why KSeF rejected the invoice (US-007, Q4) — shown when the user edits it. */
@Serializable
data class Rejection(
    val statusCode: Int,
    val description: String,
    val details: List<String> = emptyList(),
    val originalKsefNumber: String? = null,
)

/** Sales invoice saved on the phone without sending it to KSeF; editable until it is sent (US-007). */
@Serializable
data class UnsentInvoice(
    val id: String,
    val form: InvoiceForm,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
    val rejection: Rejection? = null,
) {
    companion object {
        fun create(form: InvoiceForm, now: Instant, rejection: Rejection? = null) =
            UnsentInvoice(UUID.randomUUID().toString(), form, now, now, rejection)
    }
}

/**
 * Unsent invoices, one file per invoice in `filesDir/unsent/{id}.json` (atomic writes). Kept outside the invoice
 * cache, so "Wyczyść cache" never removes them — they cannot be restored from KSeF (decision P7).
 */
class UnsentInvoiceStore(filesDir: File) {

    private val directory = File(filesDir, "unsent")
    private val json = Json { ignoreUnknownKeys = true }

    fun list(): List<UnsentInvoice> =
        directory.listFiles { file -> file.name.endsWith(".json") }.orEmpty()
            // A corrupted file must not hide the other invoices.
            .mapNotNull { runCatching { json.decodeFromString(UnsentInvoice.serializer(), it.readText(Charsets.UTF_8)) }.getOrNull() }
            .sortedByDescending { it.updatedAt }

    fun get(id: String): UnsentInvoice? = list().firstOrNull { it.id == id }

    fun save(invoice: UnsentInvoice) {
        val target = file(invoice.id)
        directory.mkdirs()
        val temporary = File(directory, target.name + ".tmp")
        temporary.writeText(json.encodeToString(UnsentInvoice.serializer(), invoice), Charsets.UTF_8)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun delete(id: String) {
        file(id).delete()
    }

    private fun file(id: String): File {
        require(ID.matches(id)) { "Unexpected unsent invoice id" }
        return File(directory, "$id.json")
    }

    private companion object {
        val ID = Regex("[0-9a-fA-F-]{1,64}")
    }
}

/** Observable access to unsent invoices; every change is written to disk first. */
class UnsentInvoiceRepository(private val store: UnsentInvoiceStore, scope: CoroutineScope) {

    private val _invoices = MutableStateFlow<List<UnsentInvoice>>(emptyList())
    val invoices: StateFlow<List<UnsentInvoice>> = _invoices.asStateFlow()

    // Disk and state change together: otherwise the initial load could overwrite an invoice saved meanwhile.
    private val lock = Any()

    init {
        scope.launch(Dispatchers.IO) { synchronized(lock) { _invoices.value = store.list() } }
    }

    fun get(id: String): UnsentInvoice? = _invoices.value.firstOrNull { it.id == id } ?: store.get(id)

    /** Call from a background thread. */
    fun save(invoice: UnsentInvoice) = synchronized(lock) {
        store.save(invoice)
        _invoices.update { current -> (listOf(invoice) + current.filter { it.id != invoice.id }).sortedByDescending { it.updatedAt } }
    }

    /** Call from a background thread. */
    fun delete(id: String) = synchronized(lock) {
        store.delete(id)
        _invoices.update { current -> current.filter { it.id != id } }
    }
}

/** Invoice numbers already taken: KSeF invoices and unsent invoices (which reserve their number — decision Q1). */
object TakenNumbers {

    fun of(ksefNumbers: Collection<String>, unsent: Collection<UnsentInvoice>, excludeUnsentId: String?): List<String> =
        ksefNumbers + unsent.filter { it.id != excludeUnsentId }.map { it.form.invoiceNumber.trim() }.filter { it.isNotEmpty() }
}
