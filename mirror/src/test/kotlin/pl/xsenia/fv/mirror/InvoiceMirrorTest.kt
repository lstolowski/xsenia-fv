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
}
