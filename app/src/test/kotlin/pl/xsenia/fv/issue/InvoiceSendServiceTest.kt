package pl.xsenia.fv.issue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.xsenia.fv.exchangerates.ExchangeRateResult
import pl.xsenia.fv.invoice.currency.ExchangeRate
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.Address
import pl.xsenia.fv.invoice.profile.BankAccount
import pl.xsenia.fv.invoice.profile.Company
import pl.xsenia.fv.invoice.profile.Counterparty
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.ksef.KsefFailure
import pl.xsenia.fv.ksef.KsefInvoiceRejectedException
import pl.xsenia.fv.ksef.sending.SentInvoice
import pl.xsenia.fv.ksef.sending.SubmittedInvoice
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class InvoiceSendServiceTest {

    @TempDir
    lateinit var dir: File

    private val submitted = SubmittedInvoice("SO-1", "INV-1")
    private val form = InvoiceForm(
        counterpartyId = "6691034274",
        issueDate = LocalDate.parse("2026-10-01"),
        deliveryDate = LocalDate.parse("2026-09-30"),
        invoiceNumber = "04/09/2026",
        lines = listOf(LineForm("Usługa testowa", "1", "szt.", "100")),
    )
    private val draft = SalesInvoiceDraft(
        "04/09/2026", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-09-30"),
        Company("Jan Testowy", "9945082500", Address("ul. Testowa 1", null, null), null, null),
        Counterparty("Testowa Sp. z o.o.", "6691034274", Address("ul. Testowa 6", null, null), null),
        BankAccount("Konto testowe", null, "85216097583216850246002152", "PLN"),
        listOf(DraftLine("Usługa testowa", BigDecimal.ONE, "szt.", BigDecimal(100))),
    )

    /** Fake KSeF: [outcome] runs after the invoice was submitted (unless [failBeforeSubmit]); [sentXml] — what was sent. */
    private class FakeBackend(var failBeforeSubmit: Exception? = null, var outcome: () -> SentInvoice) : InvoiceSendBackend {
        val sentXml = mutableListOf<String>()

        override suspend fun send(nip: String, invoiceXml: ByteArray, onSubmitted: suspend (SubmittedInvoice) -> Unit): SentInvoice {
            sentXml += invoiceXml.toString(Charsets.UTF_8)
            failBeforeSubmit?.let { throw it }
            onSubmitted(SubmittedInvoice("SO-1", "INV-1"))
            return outcome()
        }

        override suspend fun awaitResult(nip: String, submitted: SubmittedInvoice) = outcome()
    }

    private val accepted = { SentInvoice(submitted, "9945082500-20261001-0000000000AA-00", null, "<UPO/>".toByteArray()) }

    /** NBP rates (docs/przyklady/kursy-nbp.md): working 09.10.2026, final for sending 12.10.2026. */
    private fun rate(rate: String, table: String, date: String) = ExchangeRate(InvoiceCurrency.EUR, BigDecimal(rate), table, LocalDate.parse(date))
    private val workingRate = rate("4.3775", "197/A/NBP/2026", "2026-10-09")
    private val finalRate = rate("4.3889", "198/A/NBP/2026", "2026-10-12")
    private val euroDraft = draft.copy(
        bankAccount = BankAccount("Konto testowe EUR", null, "83216097587877893287921742", "EUR"),
        currency = InvoiceCurrency.EUR,
        exchangeRate = workingRate,
    )

    /** Fake final rates: [result] for every invoice; [asked] — the days of sending asked for. */
    private class FakeRates(var result: ExchangeRateResult) : FinalRateSource {
        val asked = mutableListOf<LocalDate>()

        override suspend fun rateFor(draft: SalesInvoiceDraft, sendDay: LocalDate): ExchangeRateResult {
            asked += sendDay
            return result
        }
    }

    // 2026-10-13 22:30 UTC is already 14 October in Poland.
    private val clock = Clock.fixed(Instant.parse("2026-10-13T22:30:00Z"), ZoneOffset.UTC)

    private inner class Fixture(backend: InvoiceSendBackend, scope: CoroutineScope, val rates: FakeRates) {
        val storage = IssueStorage(dir)
        val unsent = UnsentInvoiceRepository(UnsentInvoiceStore(dir), scope)
        var synced = 0
        val service = InvoiceSendService(
            scope, backend, storage, unsent, onAccepted = { synced++ }, finalRates = rates, clock = clock, ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun run(
        backend: InvoiceSendBackend,
        rates: FakeRates = FakeRates(ExchangeRateResult.Found(finalRate)),
        block: Fixture.() -> Unit,
    ): Fixture = runBlocking {
        lateinit var fixture: Fixture
        coroutineScope {
            fixture = Fixture(backend, this, rates)
            fixture.block()
        }
        fixture
    }

    @Test
    fun `accepted new invoice clears the draft, saves UPO and syncs`() {
        val fixture = run(FakeBackend(outcome = accepted)) {
            storage.saveDraft(form)
            service.send(draft, form, unsentId = null)
        }

        assertTrue(fixture.service.state.value is SendState.Accepted)
        assertNull(fixture.storage.loadDraft())
        assertNull(fixture.storage.loadPending())
        assertTrue(File(dir, "upo/9945082500-20261001-0000000000AA-00.xml").isFile)
        assertEquals(1, fixture.synced)
        assertTrue(fixture.unsent.invoices.value.isEmpty())
    }

    @Test
    fun `accepted unsent invoice is removed from unsent invoices, the draft is kept`() {
        val existing = UnsentInvoice.create(form, Instant.EPOCH)
        val fixture = run(FakeBackend(outcome = accepted)) {
            storage.saveDraft(form.copy(invoiceNumber = "draft"))
            unsent.save(existing)
            service.send(draft, form, unsentId = existing.id)
        }

        assertTrue(fixture.unsent.invoices.value.isEmpty())
        assertEquals("draft", fixture.storage.loadDraft()?.invoiceNumber)
    }

    @Test
    fun `rejected invoice is kept as unsent with the reason`() {
        val rejected = KsefInvoiceRejectedException(450, "Błąd weryfikacji semantyki", listOf("P_2"), mapOf("originalKsefNumber" to null))
        val fixture = run(FakeBackend(outcome = { throw rejected })) {
            storage.saveDraft(form)
            service.send(draft, form, unsentId = null)
        }

        val state = fixture.service.state.value as SendState.Rejected
        val saved = fixture.unsent.invoices.value.single()
        assertEquals(saved.id, state.unsentId)
        assertEquals(Rejection(450, "Błąd weryfikacji semantyki", listOf("P_2"), null), saved.rejection)
        assertEquals(form, saved.form)
        assertNull(fixture.storage.loadDraft())
        assertNull(fixture.storage.loadPending())
    }

    @Test
    fun `failure before reaching KSeF keeps the invoice as unsent`() {
        val fixture = run(FakeBackend(failBeforeSubmit = IOException("no network"), outcome = accepted)) {
            service.send(draft, form, unsentId = null)
        }

        val state = fixture.service.state.value as SendState.Failed
        assertEquals(false, state.pendingInKsef)
        assertTrue(state.failure is KsefFailure.Network)
        assertEquals(fixture.unsent.invoices.value.single().id, state.unsentId)
    }

    @Test
    fun `failure after reaching KSeF keeps it pending, not unsent, and retry only checks the status`() {
        val backend = FakeBackend(outcome = { throw IOException("connection lost") })
        val fixture = run(backend) { service.send(draft, form, unsentId = null) }

        val state = fixture.service.state.value as SendState.Failed
        assertTrue(state.pendingInKsef)
        assertTrue(fixture.unsent.invoices.value.isEmpty())
        assertNotNull(fixture.storage.loadPending())

        backend.outcome = accepted
        runBlocking {
            coroutineScope {
                val resumed = InvoiceSendService(this, backend, fixture.storage, fixture.unsent, {}, fixture.rates, ioDispatcher = Dispatchers.Unconfined)
                resumed.resumeIfPending()
            }
        }
        assertNull(fixture.storage.loadPending())
    }

    @Test
    fun `PLN invoice is sent without asking for a rate`() {
        val backend = FakeBackend(outcome = accepted)
        val fixture = run(backend) { service.send(draft, form, unsentId = null) }

        assertTrue(fixture.service.state.value is SendState.Accepted)
        assertTrue(fixture.rates.asked.isEmpty())
        assertTrue(backend.sentXml.single().contains("<KodWaluty>PLN</KodWaluty>"))
    }

    @Test
    fun `foreign currency invoice gets the rate for the day of sending in Poland`() {
        val backend = FakeBackend(outcome = accepted)
        val fixture = run(backend) { service.send(euroDraft, form, unsentId = null) }

        assertTrue(fixture.service.state.value is SendState.Accepted)
        assertEquals(listOf(LocalDate.parse("2026-10-14")), fixture.rates.asked)
        val xml = backend.sentXml.single()
        assertTrue(xml.contains("<KursWaluty>4.3889</KursWaluty>"))
        assertTrue(xml.contains("tabela NBP 198/A/NBP/2026 z 2026-10-12"))
        assertFalse(xml.contains("4.3775"))
    }

    @Test
    fun `without the rate nothing is sent and the invoice stays a draft`() {
        val backend = FakeBackend(outcome = accepted)
        val problem = ExchangeRateResult.Failure(ExchangeRateResult.Failure.Reason.NETWORK)
        val fixture = run(backend, FakeRates(problem)) {
            storage.saveDraft(form)
            service.send(euroDraft, form, unsentId = null)
        }

        assertEquals(SendState.RateUnavailable("04/09/2026", problem, unsentId = null), fixture.service.state.value)
        assertTrue(backend.sentXml.isEmpty())
        assertEquals(form, fixture.storage.loadDraft())
        assertTrue(fixture.unsent.invoices.value.isEmpty())
        assertNull(fixture.storage.loadPending())

        fixture.service.acknowledge()
        assertEquals(SendState.Idle, fixture.service.state.value)
    }
}
