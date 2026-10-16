package pl.xsenia.fv.profile

import kotlinx.coroutines.CoroutineScope
import pl.xsenia.fv.currency.ExchangeRateService
import pl.xsenia.fv.invoice.profile.CompanyProfileStore
import pl.xsenia.fv.issue.FinalRateSource
import pl.xsenia.fv.issue.InvoiceSendService
import pl.xsenia.fv.issue.IssueStorage
import pl.xsenia.fv.issue.KsefSendBackend
import pl.xsenia.fv.issue.UnsentInvoiceRepository
import pl.xsenia.fv.issue.UnsentInvoiceStore
import pl.xsenia.fv.ksef.KsefAccess
import pl.xsenia.fv.mirror.InvoiceMirror
import pl.xsenia.fv.security.KsefTokenStore
import pl.xsenia.fv.sync.InvoiceSyncService
import java.io.File
import java.time.Instant

/**
 * Everything the app does in the context of one of my companies (ADR 0006): its data, issuing and the invoice mirror,
 * all kept in [directory] (`profiles/{nip}/`). [nip] is the NIP of the KSeF token.
 */
class ProfileSession(
    val nip: String,
    val directory: File,
    scope: CoroutineScope,
    ksef: KsefAccess,
    tokenStore: KsefTokenStore,
    mirrorStartDate: Instant,
    exchangeRates: ExchangeRateService,
) {

    val profile = ProfileRepository(CompanyProfileStore(directory, nip), scope)

    /** `issue/` (draft, pending send) and `upo/`. */
    val issueStorage = IssueStorage(directory)

    /** `unsent/` — never removed by "Wyczyść cache" (they cannot be restored from KSeF). */
    val unsentInvoices = UnsentInvoiceRepository(UnsentInvoiceStore(directory), scope)

    /** `invoices/` — the local copy of KSeF invoices (ADR 0003). */
    private val mirror = InvoiceMirror(File(directory, MIRROR_DIRECTORY))

    val syncService = InvoiceSyncService(scope, ksef, tokenStore, nip, mirror, mirrorStartDate)

    val sendService = InvoiceSendService(
        scope = scope,
        backend = KsefSendBackend(ksef),
        storage = issueStorage,
        unsent = unsentInvoices,
        onAccepted = syncService::syncNow,
        finalRates = FinalRateSource { draft, sendDay -> exchangeRates.rateFor(draft.currency, draft.deliveryDate, sendDay) },
    )

    /** Removes the downloaded invoices (cancelling a running synchronization); they are downloaded again by the next one. */
    suspend fun clearMirror() = syncService.clearMirror()

    companion object {
        const val MIRROR_DIRECTORY = "invoices"
    }
}
