package pl.xsenia.fv.profile

import java.io.File

/**
 * One-off move of data saved before company profiles existed (ADR 0006): `issue/`, `unsent/`, `upo/` and `invoices/`
 * from the root of the app's files directory into the profile directory of the token NIP.
 *
 * A directory is moved only when the profile has none yet, so running it again never overwrites anything.
 */
object LegacyDataMigration {

    val DIRECTORIES = listOf("issue", "unsent", "upo", ProfileSession.MIRROR_DIRECTORY)

    /** Returns the names of the directories that were moved. */
    fun run(filesDir: File, profileDir: File): List<String> = DIRECTORIES.filter { name ->
        val source = File(filesDir, name)
        val target = File(profileDir, name)
        if (!source.isDirectory || target.exists()) return@filter false
        profileDir.mkdirs()
        source.renameTo(target)
    }
}
