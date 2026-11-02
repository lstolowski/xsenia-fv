package pl.xsenia.fv.ui.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.LifecycleResumeEffect
import pl.xsenia.fv.reminder.MonthlyReminder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.BuildConfig
import pl.xsenia.fv.R
import pl.xsenia.fv.ui.components.SecondaryButton
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.ui.MainViewModel
import pl.xsenia.fv.ui.components.SectionCard
import pl.xsenia.fv.ui.components.SupportButton
import pl.xsenia.fv.ui.components.SupportDialog
import pl.xsenia.fv.ui.components.TokenSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    session: ProfileSession,
    onEditCompany: () -> Unit,
    onOpenCounterparties: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val tokenSet by viewModel.tokenSet.collectAsStateWithLifecycle()
    val tokenNip by viewModel.tokenNip.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val profile by session.profile.profile.collectAsStateWithLifecycle()
    var confirmClearCache by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cacheClearedMessage = stringResource(R.string.settings_cache_cleared)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { onBack?.let { TextButton(onClick = it) { Text(stringResource(R.string.action_back)) } } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            profile?.let {
                CompanySection(it, onEdit = onEditCompany)
                CounterpartiesLink(it.counterparties.size, onClick = onOpenCounterparties)
            }
            SectionCard(stringResource(R.string.settings_app_section)) {
                Text(stringResource(R.string.environment_label, viewModel.environment.name))
                Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME))
                SupportButton(onClick = { showSupport = true })
            }
            TokenSection(
                title = stringResource(R.string.token_section_title),
                environment = viewModel.environment,
                tokenSet = tokenSet == true,
                tokenNip = tokenNip,
                connection = connection,
                onSaveToken = viewModel::saveToken,
                onCheckConnection = viewModel::checkConnection,
            )
            ReminderSection(enabled = reminderEnabled, onEnabledChange = viewModel::setReminderEnabled)
            SectionCard(stringResource(R.string.settings_cache_section)) {
                Text(stringResource(R.string.settings_cache_description))
                SecondaryButton(onClick = { confirmClearCache = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_cache_clear)) }
            }
        }
    }

    if (showSupport) SupportDialog(onDismiss = { showSupport = false })

    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text(stringResource(R.string.settings_cache_confirm_title)) },
            text = { Text(stringResource(R.string.settings_cache_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    viewModel.clearCache()
                    Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.settings_cache_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Switch of the monthly reminder (US-017); asks for the system notification permission when it is missing. */
@Composable
private fun ReminderSection(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(MonthlyReminder.hasPermission(context)) }
    LifecycleResumeEffect(Unit) {
        permissionGranted = MonthlyReminder.hasPermission(context)
        onPauseOrDispose {}
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }

    SectionCard(stringResource(R.string.settings_reminder_section)) {
        Text(stringResource(R.string.settings_reminder_description))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_reminder_switch), modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = {
                onEnabledChange(it)
                if (it && !permissionGranted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        if (enabled && !permissionGranted) {
            Text(stringResource(R.string.settings_reminder_no_permission), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }) { Text(stringResource(R.string.settings_reminder_open_settings)) }
        }
    }
}
