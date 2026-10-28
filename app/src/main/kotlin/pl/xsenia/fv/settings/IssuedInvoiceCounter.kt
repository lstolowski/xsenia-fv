package pl.xsenia.fv.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * Number of invoices issued in the app (accepted by KSeF, all companies together), kept in a cache [file] (US-016).
 * A lost or unreadable file is not a problem — counting simply starts again from 1.
 */
class IssuedInvoiceCounter(private val file: File) {

    private val _count = MutableStateFlow<Int?>(null)

    /** The count after the last [increment] in this process; null before any. */
    val count: StateFlow<Int?> = _count.asStateFlow()

    /** Call off the main thread. */
    fun increment(): Int {
        val next = read() + 1
        try {
            file.parentFile?.mkdirs()
            file.writeText(next.toString())
        } catch (_: IOException) {
            // Only the support dialog depends on it; the next invoice tries again.
        }
        _count.value = next
        return next
    }

    private fun read(): Int = try {
        file.readText().trim().toIntOrNull() ?: 0
    } catch (_: IOException) {
        0
    }

    companion object {
        /** The support dialog is shown after this invoice (US-016). */
        const val SUPPORT_PROMPT_AT = 2
    }
}
