package pl.xsenia.fv

import android.content.Context
import android.os.SystemClock
import pl.xsenia.fv.config.ConfigRepository
import pl.xsenia.fv.issue.InvoiceSendService
import pl.xsenia.fv.issue.IssueStorage
import pl.xsenia.fv.issue.KsefSendBackend
import pl.xsenia.fv.issue.UnsentInvoiceRepository
import pl.xsenia.fv.issue.UnsentInvoiceStore
import pl.xsenia.fv.ksef.KsefAccess
import pl.xsenia.fv.ksef.KsefConnectionChecker
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.lookup.CompanyLookupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.mirror.InvoiceMirror
import pl.xsenia.fv.sync.InvoiceSyncService
import java.time.LocalDate
import java.time.ZoneId
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.security.AppLock
import pl.xsenia.fv.security.KeystoreCipher
import pl.xsenia.fv.security.KsefTokenStore
import pl.xsenia.fv.settings.AppSettings
import pl.xsenia.fv.settings.settingsDataStore
import pl.xsenia.fv.storage.LocalCache
import pl.xsenia.fv.whitelist.WhiteListClient

/** Manual dependency container, created once per process by [XseniaApplication]. */
class AppContainer(context: Context) {

    val environment: KsefEnvironment = KsefEnvironment.valueOf(BuildConfig.KSEF_ENVIRONMENT)

    val appLock = AppLock(elapsedRealtime = SystemClock::elapsedRealtime)

    val settings = AppSettings(context.settingsDataStore)

    val tokenStore = KsefTokenStore(context.settingsDataStore, KeystoreCipher(keyAlias = "ksef_token_key"))

    val configRepository = ConfigRepository(context, settings)

    val localCache = LocalCache(context)

    /** Scope of work that must survive screen changes (e.g. synchronization). */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val httpClient by lazy { KsefHttpClientFactory.create(environment) }

    private val publicKeys by lazy { KsefPublicKeyProvider.fromApi(httpClient) }

    val authenticator by lazy { KsefAuthenticator(httpClient, publicKeys) }

    val connectionChecker by lazy { KsefConnectionChecker(authenticator) }

    val ksefAccess by lazy { KsefAccess(httpClient, publicKeys, authenticator, tokenStore) }

    val syncService by lazy {
        InvoiceSyncService(
            scope = applicationScope,
            ksef = ksefAccess,
            tokenStore = tokenStore,
            configRepository = configRepository,
            mirror = InvoiceMirror(localCache.invoicesDir),
            initialDate = MIRROR_START_DATE.atStartOfDay(POLAND).toInstant(),
        )
    }

    /** Company data by NIP from the MF White List (ADR 0007) — used by the company and counterparty forms. */
    val companyLookup by lazy { CompanyLookupService(WhiteListClient.create(), today = { LocalDate.now(POLAND) }) }

    val issueStorage = IssueStorage(context.filesDir)

    val unsentInvoices = UnsentInvoiceRepository(UnsentInvoiceStore(context.filesDir), applicationScope)

    val sendService by lazy {
        InvoiceSendService(
            scope = applicationScope,
            backend = KsefSendBackend(ksefAccess),
            storage = issueStorage,
            unsent = unsentInvoices,
            onAccepted = syncService::syncNow,
        )
    }

    companion object {
        /** Invoices are kept locally from this date (decision R2). */
        val MIRROR_START_DATE: LocalDate = LocalDate.of(2026, 4, 1)
        val POLAND: ZoneId = ZoneId.of("Europe/Warsaw")
    }
}
