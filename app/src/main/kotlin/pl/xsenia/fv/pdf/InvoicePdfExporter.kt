package pl.xsenia.fv.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.xsenia.fv.invoice.model.Invoice
import java.io.File

/**
 * Draws the invoice PDF ([InvoicePdfLayout]) with `PdfDocument` into `cacheDir/pdf/` and builds the share intent
 * (ADR 0010). The previous PDF is removed — only one file waits to be shared; "Wyczyść cache" removes it too.
 */
class InvoicePdfExporter(private val context: Context) {

    private val paints = PdfTextStyle.entries.associateWith { style ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.size
            typeface = if (style.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            color = TEXT_COLOR
        }
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE_COLOR }
    private val measurer = TextMeasurer { text, style -> paints.getValue(style).measureText(text) }

    /** @param ksefNumber null for an invoice not sent to KSeF yet */
    suspend fun export(invoice: Invoice, ksefNumber: String?): File = withContext(Dispatchers.IO) {
        val pages = InvoicePdfLayout(measurer).layout(invoice, ksefNumber)
        val directory = File(context.cacheDir, DIRECTORY).apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(directory, InvoicePdfLayout.fileName(invoice.invoiceNumber))
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val pageInfo = PdfDocument.PageInfo.Builder(PdfPage.WIDTH.toInt(), PdfPage.HEIGHT.toInt(), index + 1).create()
                val pdfPage = document.startPage(pageInfo)
                draw(page, pdfPage.canvas)
                document.finishPage(pdfPage)
            }
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        file
    }

    /** System share sheet with the PDF ("Udostępnij fakturę"). */
    fun shareIntent(file: File, title: String): Intent {
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title)
    }

    private fun draw(page: PdfPage, canvas: android.graphics.Canvas) {
        page.elements.forEach { element ->
            when (element) {
                is PdfElement.Text -> {
                    val paint = paints.getValue(element.style)
                    paint.textAlign = if (element.align == PdfAlign.RIGHT) Paint.Align.RIGHT else Paint.Align.LEFT
                    canvas.drawText(element.text, element.x, element.baseline, paint)
                }
                is PdfElement.Line -> {
                    linePaint.strokeWidth = element.width
                    canvas.drawLine(element.x1, element.y1, element.x2, element.y2, linePaint)
                }
            }
        }
    }

    private companion object {
        const val DIRECTORY = "pdf"
        const val MIME_TYPE = "application/pdf"

        /** Must match the provider in AndroidManifest.xml. */
        const val AUTHORITY_SUFFIX = ".files"
        const val TEXT_COLOR = 0xFF1A1A1A.toInt()
        const val LINE_COLOR = 0xFF9E9E9E.toInt()
    }
}
