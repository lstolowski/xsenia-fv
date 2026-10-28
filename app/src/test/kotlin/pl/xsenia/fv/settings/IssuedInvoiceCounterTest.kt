package pl.xsenia.fv.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IssuedInvoiceCounterTest {

    @TempDir
    lateinit var dir: File

    private val file get() = File(dir, "cache/issued-invoices.txt")

    @Test
    fun `counts issued invoices across restarts`() {
        val counter = IssuedInvoiceCounter(file)
        assertNull(counter.count.value)
        assertEquals(1, counter.increment())
        assertEquals(2, counter.increment())
        assertEquals(2, counter.count.value)
        assertEquals(3, IssuedInvoiceCounter(file).increment())
    }

    @Test
    fun `starts again from 1 when the file is lost or unreadable`() {
        val counter = IssuedInvoiceCounter(file)
        repeat(3) { counter.increment() }
        file.delete()
        assertEquals(1, counter.increment())
        file.writeText("garbage")
        assertEquals(1, counter.increment())
    }
}
