package pl.xsenia.fv.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import pl.xsenia.fv.R

/** Voluntary support page of the author, opened in the default browser (US-016). */
const val SUPPORT_URL = "https://suppi.pl/wsparcie/lsdevelopment"

/**
 * "Postaw kawę" dialog (US-016): from Settings and once after the second issued invoice.
 * [onDismiss] is called also after the support page was opened.
 */
@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val noBrowser = stringResource(R.string.support_no_browser)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.support_title), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(painterResource(R.drawable.coffee_cup), contentDescription = null, modifier = Modifier.size(120.dp))
                Text(stringResource(R.string.support_message))
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, SUPPORT_URL.toUri()))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, noBrowser, Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.support_donate)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.support_not_now)) }
        },
    )
}

/** Wide button of the "Aplikacja" section in Settings that opens [SupportDialog]. */
@Composable
fun SupportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(onClick = onClick, modifier = modifier.fillMaxWidth()) { Text(stringResource(R.string.support_button)) }
}
