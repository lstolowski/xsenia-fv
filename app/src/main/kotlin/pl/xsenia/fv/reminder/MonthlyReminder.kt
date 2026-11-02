package pl.xsenia.fv.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pl.xsenia.fv.MainActivity
import pl.xsenia.fv.R
import pl.xsenia.fv.XseniaApplication
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.InvoiceMirror
import pl.xsenia.fv.profile.ProfileManager
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.settings.AppSettings
import pl.xsenia.fv.settings.settingsDataStore
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Duration
import java.time.YearMonth
import java.time.ZonedDateTime

/**
 * Monthly reminder (US-017): at about 19:00 on the last day of a month without a sales invoice a local notification
 * is shown. No push server — WorkManager runs one delayed job per month; it schedules the next one itself, and the
 * app re-schedules at every start (a job that is already queued is kept). Timing is approximate (Doze may delay it).
 */
object MonthlyReminder {

    const val EXTRA_MONTH = "reminder_month"
    const val CHANNEL_ID = "monthly_reminder"
    private const val NOTIFICATION_ID = 1
    private const val KEY_MONTH = "month"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Queues the job for the next reminder; keeps it if it is already queued. */
    fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val month = ReminderSchedule.nextMonth(now)
        val delay = Duration.between(now, ReminderSchedule.triggerOf(month, now))
        val request = OneTimeWorkRequestBuilder<Worker>()
            .setInitialDelay(delay)
            .setInputData(Data.Builder().putString(KEY_MONTH, month.toString()).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("monthly-reminder-$month", ExistingWorkPolicy.KEEP, request)
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    class Worker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            val month = inputData.getString(KEY_MONTH)?.let(YearMonth::parse) ?: return Result.failure()
            val settings = AppSettings(context.settingsDataStore)
            try {
                if (shouldNotify(month, settings)) {
                    show(context, month)
                    settings.saveLastReminderMonth(month.toString())
                }
            } finally {
                // Queue the next month's job even when this one failed or was skipped.
                schedule(context)
            }
            return Result.success()
        }

        private suspend fun shouldNotify(month: YearMonth, settings: AppSettings): Boolean {
            if (!ReminderSchedule.isStillCurrent(month, ZonedDateTime.now())) return false
            if (!settings.reminderEnabled.first() || !hasPermission(context)) return false
            if (settings.lastReminderMonth() == month.toString()) return false
            val container = (context.applicationContext as XseniaApplication).container
            val nip = container.tokenStore.tokenNip() ?: return false
            val mirror = InvoiceMirror(File(ProfileManager.profileDirectory(context.filesDir, nip), ProfileSession.MIRROR_DIRECTORY))
            return ReminderSchedule.noSalesInvoice(mirror.entries(InvoiceCategory.SALES), month)
        }
    }

    // Notification permission is checked by the caller (hasPermission).
    @android.annotation.SuppressLint("MissingPermission")
    private fun show(context: Context, month: YearMonth) {
        // Opens the sales list on the month of the reminder, also when tapped in a later month.
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_MONTH, month.toString())
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}
