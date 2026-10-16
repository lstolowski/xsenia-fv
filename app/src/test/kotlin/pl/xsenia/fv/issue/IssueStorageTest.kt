package pl.xsenia.fv.issue

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.LocalDate

class IssueStorageTest {

    @TempDir
    lateinit var dir: File

    private val form = InvoiceForm(issueDate = LocalDate.parse("2026-10-01"), deliveryDate = LocalDate.parse("2026-09-30"), invoiceNumber = "04/09/2026")

    @Test
    fun `clearing a draft and a pending send that do not exist is fine`() {
        val storage = IssueStorage(dir)

        storage.clearDraft()
        storage.clearPending()
    }

    @Test
    fun `clearing removes the draft`() {
        val storage = IssueStorage(dir)
        storage.saveDraft(form)

        storage.clearDraft()

        assertNull(storage.loadDraft())
        assertFalse(File(dir, "issue/draft.json").exists())
    }

    @Test
    fun `a draft that cannot be deleted is reported instead of coming back silently`() {
        val storage = IssueStorage(dir)
        // A non-empty directory under the draft name cannot be deleted: File.delete() returns false.
        File(dir, "issue/draft.json/stuck").apply { parentFile.mkdirs(); writeText("x") }

        assertThrows<IOException> { storage.clearDraft() }
    }

    @Test
    fun `a pending send that cannot be deleted is reported`() {
        val storage = IssueStorage(dir)
        File(dir, "issue/pending-send.json/stuck").apply { parentFile.mkdirs(); writeText("x") }

        assertThrows<IOException> { storage.clearPending() }
    }
}
