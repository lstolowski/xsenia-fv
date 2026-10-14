package pl.xsenia.fv.exchangerates

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

/**
 * Remembers answers of [delegate] for days before [today] in [file] — a published table never changes and a past day
 * without a table stays without one. Today's day is always asked again (the table appears around noon); failures are
 * never remembered. A missing or corrupted file only means an empty cache.
 */
class CachedExchangeRateSource(
    private val delegate: ExchangeRateSource,
    private val file: File,
    private val today: () -> LocalDate,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExchangeRateSource {

    private val mutex = Mutex()
    private var days: MutableMap<String, CachedDay>? = null

    override suspend fun rateOn(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult {
        require(currency.isForeign) { "PLN has no exchange rate" }
        val key = "${currency.code}/$date"
        mutex.withLock { loaded()[key] }?.toResultOrNull(currency, date)?.let { return it }

        val result = delegate.rateOn(currency, date)
        val cached = when (result) {
            is ExchangeRateResult.Found -> CachedDay(result.rate.tableNumber, result.rate.rate.toPlainString())
            ExchangeRateResult.NotPublished -> CachedDay()
            is ExchangeRateResult.Failure -> null
        }
        if (cached != null && date < today()) {
            mutex.withLock {
                loaded()[key] = cached
                save()
            }
        }
        return result
    }

    private suspend fun loaded(): MutableMap<String, CachedDay> =
        days ?: withContext(ioDispatcher) {
            runCatching { json.decodeFromString(CacheFile.serializer(), file.readText(Charsets.UTF_8)).days.toMutableMap() }
                .getOrElse { mutableMapOf() }
        }.also { days = it }

    private suspend fun save() = withContext(ioDispatcher) {
        val content = json.encodeToString(CacheFile.serializer(), CacheFile(requireNotNull(days).toSortedMap()))
        // The cache is optional: a failed write is retried with the next remembered day.
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(content, Charsets.UTF_8)
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** File content: `{"days": {"EUR/2026-10-09": {"table": "197/A/NBP/2026", "rate": "4.3775"}, "EUR/2026-10-11": {}}}`. */
@Serializable
private data class CacheFile(val days: Map<String, CachedDay> = emptyMap())

/** A day with a table ([table] and [rate]) or without one (both null). */
@Serializable
private data class CachedDay(val table: String? = null, val rate: String? = null) {

    /** Null for an entry that cannot be read (edited or corrupted file) — asked again. */
    fun toResultOrNull(currency: InvoiceCurrency, date: LocalDate): ExchangeRateResult? =
        if (table == null && rate == null) {
            ExchangeRateResult.NotPublished
        } else {
            runCatching { ExchangeRateResult.Found(ExchangeRate(currency, BigDecimal(requireNotNull(rate)), requireNotNull(table), date)) }.getOrNull()
        }
}
