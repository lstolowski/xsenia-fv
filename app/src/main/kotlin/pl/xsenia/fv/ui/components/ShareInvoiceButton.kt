package pl.xsenia.fv.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.pdf.InvoicePdfExporter

/**
 * Share icon (US-013): creates the PDF of [invoice] and opens the system share sheet.
 * [ksefNumber] null — the invoice is not in KSeF yet (preview).
 */
@Composable
fun ShareInvoiceButton(invoice: Invoice, ksefNumber: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember(context) { InvoicePdfExporter(context.applicationContext) }
    val title = stringResource(R.string.share_invoice)
    val failed = stringResource(R.string.share_invoice_failed)
    var running by remember { mutableStateOf(false) }
    IconButton(
        enabled = !running,
        onClick = {
            running = true
            scope.launch {
                try {
                    val file = exporter.export(invoice, ksefNumber)
                    context.startActivity(exporter.shareIntent(file, title))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                } finally {
                    running = false
                }
            }
        },
    ) {
        if (running) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.Share, contentDescription = title)
        }
    }
}
