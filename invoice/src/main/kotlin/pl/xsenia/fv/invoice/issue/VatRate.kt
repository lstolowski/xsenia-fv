package pl.xsenia.fv.invoice.issue

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
     * "np" in the form: not taxable in Poland, services under art. 100 ust. 1 pkt 4 (`np II`) — reverse charge,
     * the buyer settles the VAT. The invoice gets the `P_18` annotation and the remark "Reverse charge".
     */
    NOT_TAXABLE("np II", BigDecimal.ZERO),
    ;

    /** The rate has a VAT amount field (`P_14_x`) and, on a foreign currency invoice, its PLN value (`P_14_xW`). */
    val hasVatAmount: Boolean get() = percent.signum() > 0

    companion object {
        val DEFAULT = RATE_23
    }
}
