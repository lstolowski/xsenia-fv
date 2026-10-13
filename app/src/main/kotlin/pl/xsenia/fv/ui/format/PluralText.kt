package pl.xsenia.fv.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.xsenia.fv.R

/** „1 faktura”, „2 faktury”, „5 faktur”. */
@Composable
fun invoiceCount(count: Int): String = when (PolishPlural.of(count.toLong())) {
    PolishPlural.ONE -> stringResource(R.string.invoice_count_one, count)
    PolishPlural.FEW -> stringResource(R.string.invoice_count_few, count)
    PolishPlural.MANY -> stringResource(R.string.invoice_count_many, count)
}

/** „1 kontrahent”, „2 kontrahentów”, „5 kontrahentów” (a masculine personal noun: 2–4 take the same form as 5+). */
@Composable
fun counterpartyCount(count: Int): String = when (PolishPlural.of(count.toLong())) {
    PolishPlural.ONE -> stringResource(R.string.counterparty_count_one, count)
    PolishPlural.FEW -> stringResource(R.string.counterparty_count_few, count)
    PolishPlural.MANY -> stringResource(R.string.counterparty_count_many, count)
}
