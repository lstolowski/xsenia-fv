package pl.xsenia.fv

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.xsenia.fv.reminder.MonthlyReminder
import pl.xsenia.fv.security.BiometricGate
import pl.xsenia.fv.ui.XseniaApp
import pl.xsenia.fv.ui.theme.XseniaTheme
import java.time.YearMonth

/** FragmentActivity is required by BiometricPrompt. */
class MainActivity : FragmentActivity() {

    private val container get() = (application as XseniaApplication).container

    private var promptVisible = false

    // Prompt automatically once per lock; after a cancel the user taps "Odblokuj" (avoids a prompt loop).
    private var autoPromptPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Invoice data must not be visible in the recent apps overview.
        setRecentsScreenshotEnabled(false)

        readReminderMonth(intent)

        setContent {
            XseniaTheme {
                XseniaApp(
                    container = container,
                    biometricAvailable = BiometricGate.isAvailable(this),
                    onUnlockRequest = ::requestUnlock,
                )
            }
        }

        lifecycleScope.launch {
            container.appLock.locked.collect { locked ->
                autoPromptPending = locked
                maybeAutoPrompt()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readReminderMonth(intent)
    }

    /** A tapped reminder notification asks for the sales list of its month. */
    private fun readReminderMonth(intent: Intent) {
        intent.getStringExtra(MonthlyReminder.EXTRA_MONTH)?.let { month ->
            runCatching { YearMonth.parse(month) }.getOrNull()?.let { container.requestedSalesMonth.value = it }
            intent.removeExtra(MonthlyReminder.EXTRA_MONTH)
        }
    }

    // Lifecycle state is not yet RESUMED inside onResume(), so track it explicitly; fragments
    // (BiometricPrompt) are ready only after onPostResume().
    private var readyForPrompt = false

    override fun onPostResume() {
        super.onPostResume()
        readyForPrompt = true
        maybeAutoPrompt()
    }

    override fun onPause() {
        readyForPrompt = false
        super.onPause()
    }

    private fun maybeAutoPrompt() {
        if (autoPromptPending && readyForPrompt) {
            autoPromptPending = false
            requestUnlock()
        }
    }

    private fun requestUnlock() {
        if (promptVisible || !BiometricGate.isAvailable(this)) return
        promptVisible = true
        BiometricGate.authenticate(
            activity = this,
            title = getString(R.string.unlock_title),
            subtitle = getString(R.string.unlock_subtitle),
            onSuccess = {
                promptVisible = false
                container.appLock.unlock()
            },
            onFailure = { promptVisible = false },
        )
    }
}
