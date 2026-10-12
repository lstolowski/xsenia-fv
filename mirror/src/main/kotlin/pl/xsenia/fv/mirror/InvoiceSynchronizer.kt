package pl.xsenia.fv.mirror

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.xsenia.fv.invoice.xml.InvoiceParseException
import pl.xsenia.fv.invoice.xml.InvoiceXmlParser
import pl.xsenia.fv.ksef.KsefProtocolException
import pl.xsenia.fv.ksef.invoices.InvoiceMetadata
import java.time.Clock
import java.time.Duration
import java.time.Instant

enum class SyncPhase {
    /** First synchronization: downloading export packages since the initial date. */
    INITIAL_EXPORT,

    /** Checking KSeF for invoices stored since the last synchronization. */
    CHECKING,

    /** Downloading XML of new invoices. */
    DOWNLOADING,
}

data class SyncProgress(val category: InvoiceCategory, val phase: SyncPhase, val done: Int = 0, val total: Int = 0)

/** Number of invoices added to the mirror per category. */
data class SyncResult(val added: Map<InvoiceCategory, Int>)

/**
 * Keeps the local mirror in sync with KSeF (ADR 0003), using the `PermanentStorage` date and the
 * KSeF high water mark (HWM):
 *
 * 1. first run — export packages from [initialDate] in windows of at most 90 days, up to the HWM;
 * 2. every run — query invoices stored since the checkpoint up to now (also the not yet closed range after
 *    the HWM, for the newest invoices), download XML of unknown ones, move the checkpoint to the new HWM.
 *
 * Progress is saved after every export window and every downloaded invoice, so an interrupted
 * synchronization (rate limit, app closed) continues where it stopped.
 */
class InvoiceSynchronizer(
    private val source: InvoiceSource,
    private val mirror: InvoiceMirror,
    private val initialDate: Instant,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val mutex = Mutex()

    suspend fun sync(onProgress: (SyncProgress) -> Unit = {}): SyncResult = mutex.withLock {
        SyncResult(InvoiceCategory.entries.associateWith { syncCategory(it, onProgress) })
    }

    private suspend fun syncCategory(category: InvoiceCategory, onProgress: (SyncProgress) -> Unit): Int {
        val now = clock.instant()
        var added = 0
        if (state(category).checkpoint == null) {
            onProgress(SyncProgress(category, SyncPhase.INITIAL_EXPORT))
            added += initialExport(category, now)
        }

        onProgress(SyncProgress(category, SyncPhase.CHECKING))
        val checkpoint = requireNotNull(state(category).checkpoint)
        var newCheckpoint = checkpoint
        if (checkpoint.isBefore(now)) {
            val result = source.queryStoredSince(category.subjectType, checkpoint, now)
            val known = mirror.ksefNumbers(category)
            val missing = result.invoices.filter { it.ksefNumber !in known }
            missing.forEachIndexed { index, metadata ->
                onProgress(SyncProgress(category, SyncPhase.DOWNLOADING, index, missing.size))
                mirror.add(category, listOf(entryFor(metadata, source.downloadXml(metadata.ksefNumber))))
                added++
            }
            // Only the closed range (up to HWM) is final; the rest is checked again next time.
            result.permanentStorageHwmDate
                ?.takeIf { it.isAfter(checkpoint) && !it.isAfter(now) }
                ?.let { newCheckpoint = it }
        }
        saveState(category, CategorySyncState(checkpoint = newCheckpoint, lastSyncAt = now))
        return added
    }

    private suspend fun initialExport(category: InvoiceCategory, now: Instant): Int {
        var added = 0
        var from = initialDate
        while (true) {
            val windowEnd = from.plus(EXPORT_WINDOW)
            // Past windows have an explicit end; the last one runs up to the HWM (no end date).
            val closedWindow = windowEnd.isBefore(now.minus(RECENT_MARGIN))
            val export = source.exportStored(category.subjectType, from, windowEnd.takeIf { closedWindow })

            val invoices = export.invoices.map { metadata ->
                val xml = export.xmlByKsefNumber[metadata.ksefNumber] ?: source.downloadXml(metadata.ksefNumber)
                entryFor(metadata, xml)
            }
            mirror.add(category, invoices)
            added += invoices.size

            from = when {
                export.isTruncated -> export.lastPermanentStorageDate
                    ?: throw KsefProtocolException("Truncated export without lastPermanentStorageDate")
                closedWindow -> windowEnd
                else -> export.permanentStorageHwmDate ?: from
            }
            saveState(category, state(category).copy(checkpoint = from))
            if (!export.isTruncated && !closedWindow) return added
        }
    }

    private fun entryFor(metadata: InvoiceMetadata, xml: ByteArray): Pair<MirrorEntry, ByteArray> {
        val entry = try {
            MirrorEntry(metadata, InvoiceXmlParser.parse(xml).deliveryDate)
        } catch (e: InvoiceParseException) {
            MirrorEntry(metadata, metadata.issueDate, parseError = e.message)
        }
        return entry to xml
    }

    private fun state(category: InvoiceCategory) = mirror.syncState()[category]

    private fun saveState(category: InvoiceCategory, state: CategorySyncState) =
        mirror.saveSyncState(mirror.syncState().with(category, state))

    private companion object {
        // KSeF accepts at most 100 days per export.
        val EXPORT_WINDOW: Duration = Duration.ofDays(90)

        // Windows ending this close to now are left open, so the HWM decides where they end.
        val RECENT_MARGIN: Duration = Duration.ofHours(1)
    }
}
