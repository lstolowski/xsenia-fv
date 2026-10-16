package pl.xsenia.fv.mirror

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import java.time.LocalDate

class InvoiceMirrorTest {

    @TempDir
    lateinit var dir: File

    private fun entry(ksefNumber: String, stored: String) =
        MirrorEntry(FakeInvoiceSource.metadata(ksefNumber, "FV/$ksefNumber", Instant.parse(stored)), LocalDate.parse("2026-09-30"))

    @Test
    fun `empty mirror`() {
        val mirror = InvoiceMirror(File(dir, "invoices"))

        assertTrue(mirror.entries(InvoiceCategory.SALES).isEmpty())
        assertEquals(SyncState(), mirror.syncState())
    }

    @Test
    fun `stores entries and XML per category, sorted by storage date, replacing duplicates`() {
        val mirror = InvoiceMirror(dir)
        mirror.add(InvoiceCategory.SALES, listOf(entry("B", "2026-10-02T00:00:00Z") to "<b/>".toByteArray()))
        mirror.add(
            InvoiceCategory.SALES,
            listOf(entry("A", "2026-10-01T00:00:00Z") to "<a/>".toByteArray(), entry("B", "2026-10-02T00:00:00Z") to "<b2/>".toByteArray()),
        )

        assertEquals(listOf("A", "B"), mirror.entries(InvoiceCategory.SALES).map { it.metadata.ksefNumber })
        assertArrayEquals("<b2/>".toByteArray(), mirror.readXml(InvoiceCategory.SALES, "B"))
        assertTrue(mirror.entries(InvoiceCategory.PURCHASE).isEmpty())
        assertTrue(File(dir, "sales/xml/A.xml").isFile)
        assertTrue(dir.walk().none { it.name.endsWith(".tmp") }, "no temporary files left")
    }

    @Test
    fun `persists sync state and survives a new instance`() {
        val state = SyncState(sales = CategorySyncState(Instant.parse("2026-10-12T11:50:00Z"), Instant.parse("2026-10-12T12:00:00Z")))
        InvoiceMirror(dir).saveSyncState(state)

        assertEquals(state, InvoiceMirror(dir).syncState())
        assertEquals(null, state.lastSyncAt, "whole mirror is synced only when both categories are")
    }

    @Test
    fun `clear removes everything`() {
        val mirror = InvoiceMirror(dir)
        mirror.add(InvoiceCategory.SALES, listOf(entry("A", "2026-10-01T00:00:00Z") to "<a/>".toByteArray()))

        mirror.clear()

        assertTrue(mirror.entries(InvoiceCategory.SALES).isEmpty())
    }

    @Test
    fun `rejects KSeF numbers that are not safe file names`() {
        assertThrows<IllegalArgumentException> {
            InvoiceMirror(dir).add(InvoiceCategory.SALES, listOf(entry("../evil", "2026-10-01T00:00:00Z") to ByteArray(0)))
        }
    }

    @Test
    fun `a corrupted index is dropped with the checkpoint of its category, so that category is downloaded again`() {
        val mirror = InvoiceMirror(dir)
        mirror.add(InvoiceCategory.SALES, listOf(entry("A", "2026-10-01T00:00:00Z") to "<a/>".toByteArray()))
        mirror.add(InvoiceCategory.PURCHASE, listOf(entry("P", "2026-10-01T00:00:00Z") to "<p/>".toByteArray()))
        val synced = CategorySyncState(checkpoint = Instant.parse("2026-10-12T10:00:00Z"), lastSyncAt = Instant.parse("2026-10-12T10:05:00Z"))
        mirror.saveSyncState(SyncState(sales = synced, purchase = synced))
        File(dir, "sales/index.json").writeText("{ this is not json")

        assertTrue(mirror.entries(InvoiceCategory.SALES).isEmpty())

        // Sales start from scratch, purchase invoices are untouched.
        assertEquals(CategorySyncState(), mirror.syncState().sales)
        assertEquals(synced, mirror.syncState().purchase)
        assertEquals(listOf("P"), mirror.entries(InvoiceCategory.PURCHASE).map { it.metadata.ksefNumber })
        // The mirror works again: new invoices can be added.
        mirror.add(InvoiceCategory.SALES, listOf(entry("B", "2026-10-02T00:00:00Z") to "<b/>".toByteArray()))
        assertEquals(listOf("B"), mirror.entries(InvoiceCategory.SALES).map { it.metadata.ksefNumber })
    }

    @Test
    fun `a corrupted sync state is dropped, so everything is downloaded again`() {
        val mirror = InvoiceMirror(dir)
        mirror.saveSyncState(SyncState(sales = CategorySyncState(checkpoint = Instant.parse("2026-10-12T10:00:00Z"))))
        File(dir, "sync-state.json").writeText("\u0000\u0000 broken")

        assertEquals(SyncState(), mirror.syncState())
        assertEquals(false, File(dir, "sync-state.json").exists())
    }
}
