package pl.xsenia.fv.invoice.issue

import pl.xsenia.fv.invoice.profile.Counterparty
import java.math.BigDecimal

/**
 * VAT rate of an issued invoice line (ADR 0005); [code] is the FA(3) `P_12` value.
 * The order of entries follows the order of the summary fields (`P_13_x`) in FA(3).
 */
enum class VatRate(val code: String, val percent: BigDecimal) {
    RATE_23("23", BigDecimal(23)),
    RATE_8("8", BigDecimal(8)),
    RATE_5("5", BigDecimal(5)),

    /** 0% for domestic sales (`0 KR`) — buyers are always Polish (decision C1). */
    RATE_0("0 KR", BigDecimal.ZERO),

    /**
     * "np" in the form: not taxable in Poland — reverse charge, the buyer settles the VAT. The invoice gets the `P_18`
     * annotation and the remark "Reverse charge". [code] `np II` (services under art. 100 ust. 1 pkt 4, `P_13_9`) is for
     * Polish and EU buyers; a buyer from outside the EU gets `np I` (`P_13_8`) — see [codeFor] (ADR 0009).
     */
    NOT_TAXABLE("np II", BigDecimal.ZERO),
    ;

    /** `P_12` code on an invoice for [buyer]. */
    fun codeFor(buyer: Counterparty): String = if (this == NOT_TAXABLE && buyer.isOutsideEu) NOT_TAXABLE_OUTSIDE_EU_CODE else code

    /** The rate has a VAT amount field (`P_14_x`) and, on a foreign currency invoice, its PLN value (`P_14_xW`). */
    val hasVatAmount: Boolean get() = percent.signum() > 0

    companion object {
        val DEFAULT = RATE_23

        /** "np" for a buyer from outside the EU: services outside Poland other than art. 100 ust. 1 pkt 4. */
        const val NOT_TAXABLE_OUTSIDE_EU_CODE = "np I"
    }
}
