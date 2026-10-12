package pl.xsenia.fv.ui.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.xsenia.fv.R
import pl.xsenia.fv.ui.components.PrimaryButton

@Composable
fun LockScreen(biometricAvailable: Boolean, onUnlock: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.lock_portrait), contentDescription = null, modifier = Modifier.size(192.dp))
            Text(stringResource(R.string.app_name), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (biometricAvailable) {
                Text(stringResource(R.string.lock_message), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                PrimaryButton(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.unlock_button)) }
            } else {
                Text(
                    stringResource(R.string.lock_biometric_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
