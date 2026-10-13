package pl.xsenia.fv.storage

import android.content.Context

/** Temporary files in the app cache directory (e.g. downloaded invoice packages) — safe to delete at any time. */
class LocalCache(context: Context) {

    private val cacheDir = context.cacheDir

    fun clear() {
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
