package pl.xsenia.fv

import android.content.Context
import android.os.SystemClock
import pl.xsenia.fv.config.ConfigRepository
import pl.xsenia.fv.ksef.KsefConnectionChecker
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.security.AppLock
import pl.xsenia.fv.security.KeystoreCipher
import pl.xsenia.fv.security.KsefTokenStore
import pl.xsenia.fv.settings.AppSettings
import pl.xsenia.fv.settings.settingsDataStore
import pl.xsenia.fv.storage.LocalCache

/** Manual dependency container, created once per process by [XseniaApplication]. */
class AppContainer(context: Context) {

    val environment: KsefEnvironment = KsefEnvironment.valueOf(BuildConfig.KSEF_ENVIRONMENT)

    val appLock = AppLock(elapsedRealtime = SystemClock::elapsedRealtime)

    val settings = AppSettings(context.settingsDataStore)

    val tokenStore = KsefTokenStore(context.settingsDataStore, KeystoreCipher(keyAlias = "ksef_token_key"))

    val configRepository = ConfigRepository(context, settings)

    val localCache = LocalCache(context)

    private val httpClient by lazy { KsefHttpClientFactory.create(environment) }

    val authenticator by lazy { KsefAuthenticator(httpClient) }

    val connectionChecker by lazy { KsefConnectionChecker(authenticator) }
}
