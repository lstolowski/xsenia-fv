package pl.xsenia.fv.storage

import android.content.Context
import java.io.File

/** Local copies of KSeF data (invoice mirror, ADR 0003). Everything here can be re-downloaded from KSeF. */
class LocalCache(context: Context) {

    val invoicesDir: File = File(context.filesDir, "invoices")
    private val cacheDir: File = context.cacheDir

    fun clear() {
        invoicesDir.deleteRecursively()
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
