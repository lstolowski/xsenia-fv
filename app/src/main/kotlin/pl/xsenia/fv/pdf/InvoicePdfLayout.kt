package pl.xsenia.fv.pdf

import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.model.Party
import pl.xsenia.fv.ui.format.Formats
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Lays out a sales invoice on A4 pages (US-013, ADR 0010): header with the KSeF number (or "not submitted"),
 * seller and buyer side by side, the table of lines (its header repeated on every page), summary per rate with
 * VAT in PLN and the exchange rate, payment, remarks and page numbers. No QR code (decision 2026-10-14).
 *
 * Pure Kotlin — the drawing ([PdfPage] elements on a `Canvas`) is done elsewhere; [measurer] measures text.
 */
class InvoicePdfLayout(private val measurer: TextMeasurer) {

    fun layout(invoice: Invoice, ksefNumber: String?): List<PdfPage> =
        Builder(invoice, InvoicePdfLabels.forBuyerCountry(invoice.buyer.countryCode)).apply {
            header(ksefNumber)
            parties()
            lines()
            summary()
            payment()
            remarks()
        }.finish()

    private inner class Builder(private val invoice: Invoice, private val labels: InvoicePdfLabels) {
        private val pages = mutableListOf<MutableList<PdfElement>>()
        private var elements = mutableListOf<PdfElement>()
        private var y = TOP

        /** Set while the table of lines is being laid out — a new page repeats its header. */
        private var inTable = false

        init {
            pages += elements
        }

        private fun money(amount: BigDecimal, currency: String = invoice.currency): String =
            // The PDF of a foreign buyer is read in English too: "PLN" instead of "zł".
            if (labels.bilingual) "${Formats.amount(amount)} $currency" else Formats.money(amount, currency)

        fun header(ksefNumber: String?) {
            text(labels.title(invoice.kind), PdfTextStyle.TITLE)
            text(labels.number(invoice.invoiceNumber), PdfTextStyle.HEADING)
            if (ksefNumber != null) text(labels.ksefNumber(ksefNumber), PdfTextStyle.STRONG) else text(labels.notInKsef, PdfTextStyle.STRONG)
            text(labels.issueDate(Formats.date(invoice.issueDate)), PdfTextStyle.NORMAL)
            val period = invoice.period
            if (period != null) {
                text(labels.period(Formats.date(period.from), Formats.date(period.to)), PdfTextStyle.NORMAL)
            } else {
                text(labels.deliveryDate(Formats.date(invoice.deliveryDate)), PdfTextStyle.NORMAL)
            }
            if (invoice.currency != PLN) text(labels.currency(invoice.currency), PdfTextStyle.NORMAL)
            gap(SECTION_GAP)
        }

        fun parties() {
            val columnWidth = (CONTENT_WIDTH - COLUMN_GAP) / 2
            val seller = partyLines(labels.seller, invoice.seller, columnWidth)
            val buyer = partyLines(labels.buyer, invoice.buyer, columnWidth)
            val height = maxOf(seller.height(), buyer.height())
            ensure(height)
            column(seller, LEFT)
            column(buyer, LEFT + columnWidth + COLUMN_GAP)
            y += height
            gap(SECTION_GAP)
        }

        private fun partyLines(title: String, party: Party, width: Float): List<Pair<String, PdfTextStyle>> = buildList {
            add(title to PdfTextStyle.HEADING)
            fun add(text: String?, style: PdfTextStyle = PdfTextStyle.NORMAL) {
                text ?: return
                measurer.wrap(text, style, width).forEach { add(it to style) }
            }
            add(party.name, PdfTextStyle.STRONG)
            add(party.nip?.let(labels::nip))
            add(party.euVatNumber?.let(labels::euVat))
            add(party.otherId?.let(labels::otherId))
            party.addressLines.forEach { add(it) }
            add(party.countryCode?.takeIf { it != PL_COUNTRY }?.let(labels::country))
            (party.emails + party.phones).forEach { add(it, PdfTextStyle.SMALL) }
        }

        private fun List<Pair<String, PdfTextStyle>>.height() = sumOf { it.second.lineHeight.toDouble() }.toFloat()

        private fun column(lines: List<Pair<String, PdfTextStyle>>, x: Float) {
            var lineY = y
            lines.forEach { (text, style) ->
                lineY += style.lineHeight
                elements += PdfElement.Text(text, x, lineY - DESCENT, style)
            }
        }

        fun lines() {
            if (invoice.lines.isEmpty()) return
            // The heading, the table header and the first row stay together.
            ensure(TABLE_START_HEIGHT)
            text(labels.linesHeading, PdfTextStyle.HEADING)
            inTable = true
            tableHeader()
            invoice.lines.forEachIndexed { index, line ->
                val cells = listOf(
                    (line.number ?: (index + 1)).toString(),
                    line.name.orEmpty(),
                    line.quantity?.let(Formats::quantity).orEmpty(),
                    line.unit.orEmpty(),
                    line.unitNetPrice?.let(Formats::unitPrice).orEmpty(),
                    labels.lineRate(line.vatRate),
                    line.netValue?.let(Formats::amount).orEmpty(),
                    line.displayGrossValue?.let(Formats::amount).orEmpty(),
                )
                row(cells.mapIndexed { column, text -> measurer.wrap(text, PdfTextStyle.TABLE, COLUMNS[column].width - CELL_PADDING * 2) }, PdfTextStyle.TABLE)
            }
            inTable = false
            gap(SECTION_GAP)
        }

        private fun tableHeader() {
            val header = listOf(
                labels.columnNumber, labels.columnName, labels.columnQuantity, labels.columnUnit,
                labels.columnPrice, labels.columnRate, labels.columnNet, labels.columnGross,
            ).mapIndexed { column, lines -> lines.flatMap { measurer.wrap(it, PdfTextStyle.TABLE_HEADER, COLUMNS[column].width - CELL_PADDING * 2) } }
            row(header, PdfTextStyle.TABLE_HEADER, isHeader = true)
        }

        /** One table row; cells already wrapped. Breaks the page before the row when it does not fit. */
        private fun row(cells: List<List<String>>, style: PdfTextStyle, isHeader: Boolean = false) {
            val height = cells.maxOf { it.size } * style.lineHeight + CELL_PADDING * 2
            if (!isHeader) ensure(height)
            if (isHeader) elements += PdfElement.Line(LEFT, y, RIGHT, y, LINE_STRONG)
            var x = LEFT
            cells.forEachIndexed { index, lines ->
                val column = COLUMNS[index]
                lines.forEachIndexed { line, text ->
                    val baseline = y + CELL_PADDING + (line + 1) * style.lineHeight - DESCENT
                    elements += if (column.numeric) {
                        PdfElement.Text(text, x + column.width - CELL_PADDING, baseline, style, PdfAlign.RIGHT)
                    } else {
                        PdfElement.Text(text, x + CELL_PADDING, baseline, style)
                    }
                }
                x += column.width
            }
            y += height
            elements += PdfElement.Line(LEFT, y, RIGHT, y, if (isHeader) LINE_STRONG else LINE_THIN)
        }

        /**
         * Totals; the breakdown per rate only with several rates — with one rate it repeats the totals
         * (decision 2026-10-14). Per-rate sums are a required part of an invoice (VAT act art. 106e ust. 1 pkt 13–14).
         */
        fun summary() {
            if (invoice.vatSummary.size > 1) {
                text(labels.summaryHeading, PdfTextStyle.HEADING)
                invoice.vatSummary.forEach { entry ->
                    text(labels.summaryRate(labels.vatCategory(entry.category), money(entry.net), entry.vat?.let { money(it) }), PdfTextStyle.NORMAL)
                }
                gap(4f)
            }
            val pln = plnTotals()
            amountRow(labels.totalNet, money(invoice.netTotal), PdfTextStyle.NORMAL, pln?.net)
            amountRow(labels.totalVat, money(invoice.vatTotal), PdfTextStyle.NORMAL, pln?.vat)
            invoice.exchangeRate?.let { text(labels.exchangeRate(Formats.exchangeRate(it)), PdfTextStyle.NORMAL) }
            amountRow(labels.totalGross, money(invoice.grossTotal), PdfTextStyle.HEADING, pln?.gross)
            gap(SECTION_GAP)
        }

        /**
         * Totals of a foreign currency invoice converted to PLN, informative (decision 2026-10-14): net × rate, VAT in PLN
         * from the invoice (`P_14_xW`, or VAT × rate) — only when VAT > 0, gross = net + VAT in PLN. Null for PLN.
         */
        private fun plnTotals(): PlnTotals? {
            val rate = invoice.exchangeRate ?: return null
            if (invoice.currency == PLN) return null
            val net = (invoice.netTotal * rate).setScale(2, RoundingMode.HALF_UP)
            val vat = invoice.vatInPlnTotal ?: (invoice.vatTotal * rate).setScale(2, RoundingMode.HALF_UP)
            return PlnTotals(
                net = plnText(net),
                vat = vat.takeIf { invoice.vatTotal.signum() > 0 }?.let(::plnText),
                gross = plnText(net + vat),
            )
        }

        private fun plnText(amount: BigDecimal) = "(${Formats.amount(amount)} $PLN)"

        /** Label left, amount right; [pln] — the PLN equivalent in a smaller font, right of the amount. */
        private fun amountRow(label: String, amount: String, style: PdfTextStyle, pln: String? = null) {
            ensure(style.lineHeight)
            y += style.lineHeight
            elements += PdfElement.Text(label, LEFT, y - DESCENT, style)
            if (pln == null) {
                elements += PdfElement.Text(amount, RIGHT, y - DESCENT, style, PdfAlign.RIGHT)
            } else {
                elements += PdfElement.Text(pln, RIGHT, y - DESCENT, PdfTextStyle.SMALL, PdfAlign.RIGHT)
                val amountRight = RIGHT - measurer.width(pln, PdfTextStyle.SMALL) - PLN_GAP
                elements += PdfElement.Text(amount, amountRight, y - DESCENT, style, PdfAlign.RIGHT)
            }
        }

        fun payment() {
            val payment = invoice.payment ?: return
            text(labels.paymentHeading, PdfTextStyle.HEADING)
            labels.paymentMethod(payment.methodCode, payment.otherMethod)?.let { text(it, PdfTextStyle.NORMAL) }
            payment.dueDates.forEach { due ->
                val date = listOfNotNull(due.date?.let(Formats::date), due.description).joinToString(" ")
                if (date.isNotEmpty()) text(labels.dueDate(date), PdfTextStyle.STRONG)
            }
            if (payment.paid) text(labels.paid + (payment.paymentDate?.let { " " + Formats.date(it) }.orEmpty()), PdfTextStyle.NORMAL)
            payment.bankAccounts.forEach { account ->
                text(labels.bankAccount(Formats.bankAccount(account.number)), PdfTextStyle.NORMAL)
                account.swift?.let { text(labels.swift(it), PdfTextStyle.NORMAL) }
                account.bankName?.let { text(labels.bank(it), PdfTextStyle.NORMAL) }
            }
            gap(SECTION_GAP)
        }

        fun remarks() {
            if (invoice.additionalInfo.isEmpty() && invoice.footer.isEmpty()) return
            text(labels.remarksHeading, PdfTextStyle.HEADING)
            invoice.additionalInfo.forEach { (key, value) -> text(labels.remark(key, value), PdfTextStyle.NORMAL) }
            invoice.footer.forEach { text(it, PdfTextStyle.SMALL) }
        }

        /** A paragraph across the content width, wrapped. */
        private fun text(value: String, style: PdfTextStyle) {
            measurer.wrap(value, style, CONTENT_WIDTH).forEach { line ->
                ensure(style.lineHeight)
                y += style.lineHeight
                elements += PdfElement.Text(line, LEFT, y - DESCENT, style)
            }
        }

        private fun gap(height: Float) {
            y += height
        }

        /** Starts a new page when [height] does not fit below the cursor (and repeats the table header in a table). */
        private fun ensure(height: Float) {
            if (y + height <= BOTTOM || y == TOP) return
            elements = mutableListOf()
            pages += elements
            y = TOP
            if (inTable) tableHeader()
        }

        fun finish(): List<PdfPage> = pages.mapIndexed { index, page ->
            val footer = PdfElement.Text(labels.page(index + 1, pages.size), RIGHT, PdfPage.HEIGHT - MARGIN / 2, PdfTextStyle.SMALL, PdfAlign.RIGHT)
            PdfPage(page + footer)
        }
    }

    private data class Column(val width: Float, val numeric: Boolean)

    private data class PlnTotals(val net: String, val vat: String?, val gross: String)

    companion object {
        const val MARGIN = 40f
        const val LEFT = MARGIN
        const val RIGHT = PdfPage.WIDTH - MARGIN
        const val CONTENT_WIDTH = RIGHT - LEFT
        const val TOP = MARGIN

        /** Lowest y of the content — below it only the page number. */
        const val BOTTOM = PdfPage.HEIGHT - MARGIN - 12f

        private const val PLN = "PLN"
        private const val PL_COUNTRY = "PL"
        private const val SECTION_GAP = 14f
        private const val PLN_GAP = 4f
        private const val TABLE_START_HEIGHT = 70f
        private const val COLUMN_GAP = 15f
        private const val CELL_PADDING = 3f
        private const val DESCENT = 2f
        private const val LINE_THIN = 0.3f
        private const val LINE_STRONG = 0.8f

        // No. | name | quantity | unit | net price | VAT | net amount | gross amount — 515 pt in total.
        private val COLUMNS = listOf(
            Column(22f, numeric = true),
            Column(168f, numeric = false),
            Column(40f, numeric = true),
            Column(35f, numeric = false),
            Column(62f, numeric = true),
            Column(30f, numeric = false),
            Column(78f, numeric = true),
            Column(80f, numeric = true),
        )

        /** `Faktura_10-09-2026.pdf` — characters not safe in file names replaced by `-`. */
        fun fileName(invoiceNumber: String): String =
            "Faktura_" + invoiceNumber.trim().replace(Regex("[^A-Za-z0-9._-]"), "-").ifEmpty { "bez-numeru" } + ".pdf"
    }
}
