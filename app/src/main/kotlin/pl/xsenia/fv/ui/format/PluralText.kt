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
