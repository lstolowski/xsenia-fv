package pl.xsenia.fv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.mirror.SyncPhase
import pl.xsenia.fv.mirror.SyncProgress
import pl.xsenia.fv.ksef.KsefFailure
import pl.xsenia.fv.sync.SyncUiState
import pl.xsenia.fv.ui.format.Formats

/** Compact synchronization status shown above the invoice lists. */
@Composable
fun SyncStatus(state: SyncUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        when {
            state.running -> {
                Text(progressText(state.progress), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            state.lastSyncAt != null ->
                Text(stringResource(R.string.sync_last, Formats.dateTime(state.lastSyncAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Text(stringResource(R.string.sync_never), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.error?.let { Text(ksefFailureText(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (state.error is KsefFailure.Network) Text(stringResource(R.string.sync_offline_hint), style = MaterialTheme.typography.bodySmall)
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

/** Polish message for a KSeF problem (sync and sending). */
@Composable
fun ksefFailureText(error: KsefFailure): String = when (error) {
    KsefFailure.MissingToken -> stringResource(R.string.sync_error_missing_token)
    KsefFailure.TokenUnreadable -> stringResource(R.string.token_unreadable)
    is KsefFailure.RateLimited -> stringResource(R.string.sync_error_rate_limited, error.retryAfterSeconds?.toString() ?: "?")
    is KsefFailure.AuthenticationFailed -> stringResource(R.string.sync_error_auth, error.statusCode, error.description)
    is KsefFailure.Network -> stringResource(R.string.sync_error_network, error.message.orEmpty())
    is KsefFailure.Ksef -> stringResource(R.string.sync_error_ksef, error.message.orEmpty())
}
