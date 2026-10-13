package pl.xsenia.fv.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LegacyDataMigrationTest {

    @TempDir
    lateinit var filesDir: File

    private val nip = "1996845518"
    private val profileDir by lazy { ProfileManager.profileDirectory(filesDir, nip) }

    private fun legacyFile(path: String, content: String = "{}") =
        File(filesDir, path).apply { parentFile?.mkdirs(); writeText(content) }

    @Test
    fun `profile directory is profiles slash NIP`() {
        assertEquals(File(filesDir, "profiles/1996845518"), profileDir)
        assertThrows<IllegalArgumentException> { ProfileManager.profileDirectory(filesDir, "../evil") }
    }

    @Test
    fun `moves draft, unsent invoices, UPO and mirror into the profile`() {
        legacyFile("issue/draft.json", "draft")
        legacyFile("unsent/0f6c.json", "unsent")
        legacyFile("upo/TEST-KSEF-NUMBER.xml", "upo")
        legacyFile("invoices/sales/index.json", "index")
        legacyFile("datastore/settings.preferences_pb", "settings")

        val moved = LegacyDataMigration.run(filesDir, profileDir)

        assertEquals(listOf("issue", "unsent", "upo", "invoices"), moved)
        assertEquals("draft", File(profileDir, "issue/draft.json").readText())
        assertEquals("unsent", File(profileDir, "unsent/0f6c.json").readText())
        assertEquals("upo", File(profileDir, "upo/TEST-KSEF-NUMBER.xml").readText())
        assertEquals("index", File(profileDir, "invoices/sales/index.json").readText())
        assertFalse(File(filesDir, "issue").exists())
        // Other app files stay where they are.
        assertTrue(File(filesDir, "datastore/settings.preferences_pb").exists())
    }

    @Test
    fun `never overwrites profile data and does nothing on a fresh install`() {
        File(profileDir, "issue").mkdirs()
        File(profileDir, "issue/draft.json").writeText("profile draft")
        legacyFile("issue/draft.json", "old draft")

        assertEquals(emptyList<String>(), LegacyDataMigration.run(filesDir, profileDir))
        assertEquals("profile draft", File(profileDir, "issue/draft.json").readText())

        val fresh = File(filesDir, "fresh").apply { mkdirs() }
        assertEquals(emptyList<String>(), LegacyDataMigration.run(fresh, ProfileManager.profileDirectory(fresh, nip)))
        assertFalse(File(fresh, "profiles").exists())
    }
}
