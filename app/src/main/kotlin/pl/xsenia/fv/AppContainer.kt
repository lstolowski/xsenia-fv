package pl.xsenia.fv

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pl.xsenia.fv.currency.ExchangeRateService
import pl.xsenia.fv.exchangerates.CachedExchangeRateSource
import pl.xsenia.fv.exchangerates.NbpClient
import pl.xsenia.fv.ksef.KsefAccess
import pl.xsenia.fv.ksef.KsefConnectionChecker
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.lookup.CompanyLookupService
import pl.xsenia.fv.profile.ProfileManager
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.security.AppLock
import pl.xsenia.fv.security.KeystoreCipher
import pl.xsenia.fv.security.KsefTokenStore
import pl.xsenia.fv.settings.AppSettings
import pl.xsenia.fv.settings.IssuedInvoiceCounter
import pl.xsenia.fv.settings.settingsDataStore
import pl.xsenia.fv.storage.LocalCache
import pl.xsenia.fv.whitelist.WhiteListClient
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Manual dependency container, created once per process by [XseniaApplication]. */
class AppContainer(context: Context) {

    val environment: KsefEnvironment = KsefEnvironment.valueOf(BuildConfig.KSEF_ENVIRONMENT)

    val appLock = AppLock(elapsedRealtime = SystemClock::elapsedRealtime)

    val tokenStore = KsefTokenStore(context.settingsDataStore, KeystoreCipher(keyAlias = "ksef_token_key"))

    val localCache = LocalCache(context)

    val appSettings = AppSettings(context.settingsDataStore)

    /** Kept in cacheDir: "Wyczyść cache" or the system may remove it, and counting starts again (US-016). */
    val issuedInvoices = IssuedInvoiceCounter(File(context.cacheDir, "issued-invoices.txt"))

    /** Month of a tapped reminder notification, until the sales list shows it (US-017). */
    val requestedSalesMonth = MutableStateFlow<YearMonth?>(null)

    /** Scope of work that must survive screen changes (e.g. synchronization). */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val httpClient by lazy { KsefHttpClientFactory.create(environment) }

    private val publicKeys by lazy { KsefPublicKeyProvider.fromApi(httpClient) }

    val authenticator by lazy { KsefAuthenticator(httpClient, publicKeys) }

    val connectionChecker by lazy { KsefConnectionChecker(authenticator) }

    val ksefAccess by lazy { KsefAccess(httpClient, publicKeys, authenticator, tokenStore) }

    /** Company data by NIP from the MF White List (ADR 0007) — used by the company and counterparty forms. */
    val companyLookup by lazy { CompanyLookupService(WhiteListClient.create(), today = { LocalDate.now(POLAND) }) }

    // Past NBP tables never change; kept in cacheDir, so "Wyczyść cache" clears them too (ADR 0008).
    private val exchangeRateCacheFile = File(context.cacheDir, "exchange-rates.json")

    /** NBP exchange rates (ADR 0008) — used by the invoice form, preview and sending. */
    val exchangeRates by lazy {
        ExchangeRateService(
            CachedExchangeRateSource(
                delegate = NbpClient(NbpClient.createHttpClient()),
                file = exchangeRateCacheFile,
                today = { LocalDate.now(POLAND) },
            ),
        )
    }

    /** The active company profile — the NIP of the KSeF token (ADR 0006). */
    val profiles = ProfileManager(context.filesDir, tokenStore.tokenNip, applicationScope) { nip, directory ->
        ProfileSession(
            nip = nip,
            directory = directory,
            scope = applicationScope,
            ksef = ksefAccess,
            tokenStore = tokenStore,
            mirrorStartDate = MIRROR_START_DATE.atStartOfDay(POLAND).toInstant(),
            exchangeRates = exchangeRates,
            onInvoiceIssued = { applicationScope.launch(Dispatchers.IO) { issuedInvoices.increment() } },
        )
    }

    init {
        // The shared folder with JSON files is no longer used (ADR 0006).
        applicationScope.launch { appSettings.forgetLegacyConfigFolder(context.contentResolver) }
    }

    companion object {
        /** Invoices are kept locally from this date (decision R2). */
        val MIRROR_START_DATE: LocalDate = LocalDate.of(2026, 4, 1)
        val POLAND: ZoneId = ZoneId.of("Europe/Warsaw")
    }
}
