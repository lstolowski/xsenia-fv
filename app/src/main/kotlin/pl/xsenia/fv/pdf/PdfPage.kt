package pl.xsenia.fv.pdf

/** Text styles of the invoice PDF; [size] in points. */
enum class PdfTextStyle(val size: Float, val bold: Boolean) {
    TITLE(16f, true),
    HEADING(10f, true),
    NORMAL(9f, false),
    STRONG(9f, true),
    TABLE(8f, false),
    TABLE_HEADER(7f, true),
    SMALL(7f, false),
    ;

    /** Distance between baselines of two lines. */
    val lineHeight: Float get() = size * 1.35f
}

enum class PdfAlign { LEFT, RIGHT }

sealed interface PdfElement {
    /** [x] — left edge, or right edge for [PdfAlign.RIGHT]; [baseline] — y of the text baseline (y grows downwards). */
    data class Text(val text: String, val x: Float, val baseline: Float, val style: PdfTextStyle, val align: PdfAlign = PdfAlign.LEFT) : PdfElement

    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val width: Float = 0.5f) : PdfElement
}

/** One A4 page in points (595 × 842), y from the top. */
data class PdfPage(val elements: List<PdfElement>) {
    companion object {
        const val WIDTH = 595f
        const val HEIGHT = 842f
    }
}

/** Width of [text] in [style] — from `Paint` on Android, a fixed-width estimate in tests. */
fun interface TextMeasurer {
    fun width(text: String, style: PdfTextStyle): Float
}

/** Splits [text] into lines not wider than [maxWidth]; words longer than a line are cut. Keeps explicit line breaks. */
fun TextMeasurer.wrap(text: String, style: PdfTextStyle, maxWidth: Float): List<String> =
    text.split('\n').flatMap { paragraph ->
        val lines = mutableListOf<String>()
        var current = ""
        paragraph.split(' ').filter { it.isNotEmpty() }.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            when {
                width(candidate, style) <= maxWidth -> current = candidate
                current.isNotEmpty() && width(word, style) <= maxWidth -> {
                    lines += current
                    current = word
                }
                else -> {
                    // A word wider than the line: cut it.
                    var rest = candidate
                    while (width(rest, style) > maxWidth) {
                        var cut = rest.length - 1
                        while (cut > 1 && width(rest.substring(0, cut), style) > maxWidth) cut--
                        lines += rest.substring(0, cut).trimEnd()
                        rest = rest.substring(cut).trimStart()
                    }
                    current = rest
                }
            }
        }
        lines + current
    }.let { lines -> if (lines.size > 1) lines.dropLastWhile { it.isEmpty() } else lines }
