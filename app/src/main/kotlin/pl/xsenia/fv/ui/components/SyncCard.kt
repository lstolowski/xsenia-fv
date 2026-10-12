package pl.xsenia.fv.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pl.xsenia.fv.R
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.SyncPhase
import pl.xsenia.fv.mirror.SyncProgress
import pl.xsenia.fv.sync.SyncError
import pl.xsenia.fv.sync.SyncUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@Composable
fun SyncCard(state: SyncUiState, onSyncNow: () -> Unit) {
    SectionCard(stringResource(R.string.sync_title)) {
        val lastSync = state.lastSyncAt
        Text(if (lastSync == null) stringResource(R.string.sync_never) else stringResource(R.string.sync_last, lastSync.format()))
        Text(
            stringResource(
                R.string.sync_counts,
                state.invoiceCounts[InvoiceCategory.SALES] ?: 0,
                state.invoiceCounts[InvoiceCategory.PURCHASE] ?: 0,
            ),
        )
        if (state.running) {
            Text(progressText(state.progress))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSyncNow, enabled = !state.running) { Text(stringResource(R.string.sync_now)) }
    }
}

@Composable
private fun progressText(progress: SyncProgress?): String {
    if (progress == null) return stringResource(R.string.sync_running)
    val sales = progress.category == InvoiceCategory.SALES
    return when (progress.phase) {
        SyncPhase.INITIAL_EXPORT -> stringResource(if (sales) R.string.sync_phase_initial_sales else R.string.sync_phase_initial_purchase)
        SyncPhase.CHECKING -> stringResource(if (sales) R.string.sync_phase_checking_sales else R.string.sync_phase_checking_purchase)
        SyncPhase.DOWNLOADING -> stringResource(R.string.sync_phase_downloading, progress.done + 1, progress.total)
    }
}

@Composable
private fun errorText(error: SyncError): String = when (error) {
    SyncError.MissingToken -> stringResource(R.string.sync_error_missing_token)
    SyncError.MissingCompany -> stringResource(R.string.sync_error_missing_company)
    is SyncError.RateLimited -> stringResource(R.string.sync_error_rate_limited, error.retryAfterSeconds?.toString() ?: "?")
    is SyncError.AuthenticationFailed -> stringResource(R.string.sync_error_auth, error.statusCode, error.description)
    is SyncError.Network -> stringResource(R.string.sync_error_network, error.message.orEmpty())
    is SyncError.Ksef -> stringResource(R.string.sync_error_ksef, error.message.orEmpty())
}

private fun Instant.format(): String = DATE_TIME.format(atZone(ZoneId.systemDefault()))
