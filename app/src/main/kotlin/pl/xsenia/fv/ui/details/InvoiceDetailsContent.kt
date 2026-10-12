package pl.xsenia.fv.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.model.InvoiceLine
import pl.xsenia.fv.invoice.model.Party
import pl.xsenia.fv.invoice.model.VatCategory
import pl.xsenia.fv.ui.components.InfoRow
import pl.xsenia.fv.ui.components.SectionCard
import pl.xsenia.fv.ui.components.Tile
import pl.xsenia.fv.ui.format.Formats
import java.math.BigDecimal

/**
 * Human-readable invoice (US-005). Shared by the details screen and the preview before sending (ZAD-013).
 * Sections without data are skipped.
 */
@Composable
fun InvoiceDetailsContent(invoice: Invoice, ksefNumber: String?) {
    val money = { amount: BigDecimal -> Formats.money(amount, invoice.currency) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header(invoice, ksefNumber)
        PartyCard(stringResource(R.string.details_seller), invoice.seller)
        PartyCard(stringResource(R.string.details_buyer), invoice.buyer)
        invoice.otherParties.forEach { PartyCard(stringResource(R.string.details_other_party, it.role.orEmpty()), it) }
        invoice.correction?.let { correction ->
            SectionCard(stringResource(R.string.details_correction)) {
                correction.reason?.let { Text(stringResource(R.string.details_correction_reason, it)) }
                correctionType(correction.type)?.let { Text(stringResource(R.string.details_correction_type, it)) }
                correction.correctedInvoices.forEach {
                    Text(stringResource(R.string.details_corrected_invoice, it.invoiceNumber.orEmpty(), it.issueDate?.let(Formats::date).orEmpty()))
                    it.ksefNumber?.let { number -> Text(stringResource(R.string.details_ksef_number, number), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (invoice.lines.isNotEmpty()) {
            SectionCard(stringResource(R.string.details_lines)) {
                invoice.lines.forEachIndexed { index, line ->
                    if (index > 0) HorizontalDivider()
                    LineItem(line, money)
                }
            }
        }
        SectionCard(stringResource(R.string.details_summary)) {
            invoice.vatSummary.forEach { entry ->
                val label = vatCategoryLabel(entry.category)
                Text(
                    if (entry.vat != null) {
                        stringResource(R.string.details_summary_rate, label, money(entry.net), money(entry.vat!!))
                    } else {
                        stringResource(R.string.details_summary_rate_no_vat, label, money(entry.net))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider()
            InfoRow(stringResource(R.string.details_total_net), money(invoice.netTotal))
            InfoRow(stringResource(R.string.details_total_vat), money(invoice.vatTotal))
            InfoRow(stringResource(R.string.details_total_gross), money(invoice.grossTotal), emphasized = true)
        }
        invoice.payment?.let { payment ->
            SectionCard(stringResource(R.string.details_payment)) {
                (payment.methodCode?.let { paymentMethod(it) } ?: payment.otherMethod)?.let {
                    Text(stringResource(R.string.details_payment_method, it))
                }
                payment.dueDates.forEach { due ->
                    val text = listOfNotNull(due.date?.let(Formats::date), due.description).joinToString(" ")
                    if (text.isNotEmpty()) Text(stringResource(R.string.details_payment_due, text), fontWeight = FontWeight.SemiBold)
                }
                if (payment.paid) {
                    Text(
                        payment.paymentDate?.let { stringResource(R.string.details_payment_paid, Formats.date(it)) }
                            ?: stringResource(R.string.details_payment_paid_no_date),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                payment.bankAccounts.forEach { account ->
                    SelectionContainer {
                        Text(stringResource(R.string.details_bank_account, Formats.bankAccount(account.number)))
                    }
                    listOfNotNull(account.bankName, account.swift, account.description).takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Annotations(invoice)
        if (invoice.additionalInfo.isNotEmpty() || invoice.footer.isNotEmpty()) {
            SectionCard(stringResource(R.string.details_additional)) {
                invoice.additionalInfo.forEach { (key, value) -> Text("$key: $value") }
                invoice.footer.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun Header(invoice: Invoice, ksefNumber: String?) {
    // Template's wallet header: kind, large number and the gross amount in the accent color.
    Tile {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(invoiceKind(invoice.kind), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(invoice.invoiceNumber, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
            Text(
                Formats.money(invoice.grossTotal, invoice.currency),
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val secondary = MaterialTheme.colorScheme.onSurfaceVariant
            Text(stringResource(R.string.details_issue_date, Formats.date(invoice.issueDate)), color = secondary)
            invoice.issuePlace?.let { Text(stringResource(R.string.details_issue_place, it), color = secondary) }
            val period = invoice.period
            if (period != null) {
                Text(stringResource(R.string.details_period, Formats.date(period.from), Formats.date(period.to)), color = secondary)
            } else {
                Text(stringResource(R.string.details_delivery_date, Formats.date(invoice.deliveryDate)), color = secondary)
            }
            if (invoice.currency != "PLN") Text(stringResource(R.string.details_currency, invoice.currency), color = secondary)
            ksefNumber?.let {
                SelectionContainer { Text(stringResource(R.string.details_ksef_number, it), style = MaterialTheme.typography.bodySmall, color = secondary) }
            }
        }
    }
}

@Composable
private fun PartyCard(title: String, party: Party) {
    SectionCard(title) {
        party.name?.let { Text(it, fontWeight = FontWeight.SemiBold) }
        party.nip?.let { Text(stringResource(R.string.list_nip, it)) }
        party.euVatNumber?.let { Text(stringResource(R.string.details_eu_vat, it)) }
        party.otherId?.let { Text(stringResource(R.string.details_other_id, it)) }
        party.addressLines.forEach { Text(it) }
        (party.emails + party.phones).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun LineItem(line: InvoiceLine, money: (BigDecimal) -> String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (line.isStateBefore) {
            Text(stringResource(R.string.details_line_state_before), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
        }
        Text(listOfNotNull(line.number?.let { "$it." }, line.name).joinToString(" "), fontWeight = FontWeight.SemiBold)
        val quantity = line.quantity?.let(Formats::quantity)
        when {
            quantity != null && line.unitNetPrice != null ->
                Text(stringResource(R.string.details_line_quantity, quantity, line.unit.orEmpty(), money(line.unitNetPrice!!)))
            quantity != null && line.unitGrossPrice != null ->
                Text(stringResource(R.string.details_line_quantity_gross, quantity, line.unit.orEmpty(), money(line.unitGrossPrice!!)))
        }
        val net = line.netValue
        val gross = line.displayGrossValue
        if (net != null || gross != null) {
            Text(
                stringResource(R.string.details_line_values, net?.let(money) ?: "—", vatRateLabel(line.vatRate), gross?.let(money) ?: "—"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        line.deliveryDate?.let { Text(stringResource(R.string.details_line_delivery, Formats.date(it)), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Annotations(invoice: Invoice) {
    val annotations = invoice.annotations
    val items = buildList {
        if (annotations.splitPayment) add(stringResource(R.string.details_split_payment))
        if (annotations.reverseCharge) add(stringResource(R.string.details_reverse_charge))
        if (annotations.cashMethod) add(stringResource(R.string.details_cash_method))
        if (annotations.selfInvoicing) add(stringResource(R.string.details_self_invoicing))
        if (annotations.marginScheme) add(stringResource(R.string.details_margin))
        annotations.exemptionBasis?.let { add(stringResource(R.string.details_exemption, it)) }
    }
    if (items.isEmpty()) return
    SectionCard(stringResource(R.string.details_annotations)) { items.forEach { Text("• $it") } }
}

@Composable
private fun invoiceKind(kind: String): String = when (kind.uppercase()) {
    "VAT" -> stringResource(R.string.invoice_kind_vat)
    "KOR" -> stringResource(R.string.invoice_kind_kor)
    "ZAL" -> stringResource(R.string.invoice_kind_zal)
    "ROZ" -> stringResource(R.string.invoice_kind_roz)
    "UPR" -> stringResource(R.string.invoice_kind_upr)
    "KOR_ZAL" -> stringResource(R.string.invoice_kind_kor_zal)
    "KOR_ROZ" -> stringResource(R.string.invoice_kind_kor_roz)
    else -> kind
}

@Composable
private fun correctionType(type: String?): String? = when (type) {
    "1" -> stringResource(R.string.correction_type_1)
    "2" -> stringResource(R.string.correction_type_2)
    "3" -> stringResource(R.string.correction_type_3)
    else -> null
}

@Composable
private fun paymentMethod(code: Int): String? = when (code) {
    1 -> stringResource(R.string.payment_method_1)
    2 -> stringResource(R.string.payment_method_2)
    3 -> stringResource(R.string.payment_method_3)
    4 -> stringResource(R.string.payment_method_4)
    5 -> stringResource(R.string.payment_method_5)
    6 -> stringResource(R.string.payment_method_6)
    7 -> stringResource(R.string.payment_method_7)
    else -> null
}

@Composable
private fun vatCategoryLabel(category: VatCategory): String = stringResource(
    when (category) {
        VatCategory.STANDARD -> R.string.vat_category_standard
        VatCategory.REDUCED_8 -> R.string.vat_category_reduced_8
        VatCategory.REDUCED_5 -> R.string.vat_category_reduced_5
        VatCategory.TAXI_FLAT_RATE -> R.string.vat_category_taxi
        VatCategory.OSS -> R.string.vat_category_oss
        VatCategory.ZERO_DOMESTIC -> R.string.vat_category_zero_domestic
        VatCategory.ZERO_INTRA_EU_SUPPLY -> R.string.vat_category_zero_wdt
        VatCategory.ZERO_EXPORT -> R.string.vat_category_zero_export
        VatCategory.EXEMPT -> R.string.vat_category_exempt
        VatCategory.NOT_TAXABLE_ABROAD -> R.string.vat_category_np_abroad
        VatCategory.NOT_TAXABLE_ART_100 -> R.string.vat_category_np_art100
        VatCategory.REVERSE_CHARGE -> R.string.vat_category_reverse_charge
        VatCategory.MARGIN -> R.string.vat_category_margin
    },
)

/** `23%`, `0% (KR)`, `zw.`, `np.`, `o.o.` */
private fun vatRateLabel(rate: String?): String = when {
    rate == null -> "—"
    rate == "zw" -> "zw."
    rate == "oo" -> "o.o."
    rate.startsWith("np") -> "np."
    rate.contains(' ') -> "${rate.substringBefore(' ')}% (${rate.substringAfter(' ')})"
    else -> "$rate%"
}
