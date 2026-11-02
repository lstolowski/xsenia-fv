package pl.xsenia.fv

import android.app.Application
import android.app.LocaleManager
import android.os.LocaleList
import androidx.lifecycle.ProcessLifecycleOwner
import pl.xsenia.fv.reminder.MonthlyReminder

class XseniaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        usePolishLocale()
        container = AppContainer(this)
        MonthlyReminder.createChannel(this)
        MonthlyReminder.schedule(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.appLock)
    }

    /**
     * The UI exists only in Polish; use Polish as the per-app language so plural forms
     * ("0 faktur", "2 faktury"), month names and formats follow Polish rules on any device language.
     */
    private fun usePolishLocale() {
        val localeManager = getSystemService(LocaleManager::class.java)
        if (localeManager.applicationLocales.isEmpty) localeManager.applicationLocales = LocaleList.forLanguageTags("pl")
    }
}
