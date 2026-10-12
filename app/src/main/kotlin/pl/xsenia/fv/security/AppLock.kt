package pl.xsenia.fv.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-level lock (US-001): locked on cold start and after the app spent at least [relockAfterMillis] in background.
 * Observe the process lifecycle (ProcessLifecycleOwner) to feed background/foreground events.
 */
class AppLock(
    private val elapsedRealtime: () -> Long,
    private val relockAfterMillis: Long = 60_000,
) : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundSince: Long? = null

    fun unlock() {
        _locked.value = false
    }

    fun onBackground() {
        backgroundSince = elapsedRealtime()
    }

    fun onForeground() {
        val since = backgroundSince ?: return
        backgroundSince = null
        if (elapsedRealtime() - since >= relockAfterMillis) _locked.value = true
    }

    override fun onStop(owner: LifecycleOwner) = onBackground()

    override fun onStart(owner: LifecycleOwner) = onForeground()
}
