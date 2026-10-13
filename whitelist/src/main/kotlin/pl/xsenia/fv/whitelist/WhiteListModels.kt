package pl.xsenia.fv.whitelist

/** Result of a White List lookup by NIP (ADR 0007). */
sealed interface WhiteListResult {
    data class Found(val subject: WhiteListSubject) : WhiteListResult

    /** The registry has no taxpayer with this NIP (`subject: null`). */
    data object NotFound : WhiteListResult

    data class Failed(val failure: WhiteListFailure) : WhiteListResult
}

/** Why a lookup could not be completed. */
sealed interface WhiteListFailure {
    /** No connection or timeout. */
    data class Network(val message: String?) : WhiteListFailure

    /** The daily query limit is used up (HTTP 429). */
    data object RateLimited : WhiteListFailure

    /** The API rejected the request with an error code, e.g. `WL-113` (wrong NIP length), `WL-103` (future date). */
    data class Api(val httpStatus: Int, val code: String, val message: String?) : WhiteListFailure

    /** Server error or a response that is not the expected JSON. */
    data class Server(val httpStatus: Int) : WhiteListFailure
}

/**
 * Taxpayer from the registry. [name] and [address] keep the registry spelling (upper case);
 * for a sole proprietor [name] is the owner's first and last name, not the trade name.
 */
data class WhiteListSubject(
    val name: String,
    val nip: String,
    /** `statusVat` as returned, e.g. „Czynny”, „Zwolniony”. */
    val vatStatus: String?,
    /** Place of business (`workingAddress`), or the residence address when there is none. */
    val address: WhiteListAddress?,
    /** 26-digit NRB numbers. */
    val accountNumbers: List<String>,
    val regon: String?,
    val krs: String?,
) {
    val isActiveVatPayer: Boolean
        get() = vatStatus.equals(ACTIVE_VAT_STATUS, ignoreCase = true)

    companion object {
        const val ACTIVE_VAT_STATUS = "Czynny"
    }
}

/** Registry address split for FA(3) `AdresL1` / `AdresL2`. */
data class WhiteListAddress(val line1: String, val line2: String?) {

    companion object {
        /**
         * Splits „STREET 1, 00-001 CITY” at the last comma: street part in [line1], postal code and city in [line2].
         * Without a comma the whole text goes to [line1]. Returns null for a blank address.
         */
        fun parse(raw: String?): WhiteListAddress? {
            val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val comma = text.lastIndexOf(',')
            if (comma < 0) return WhiteListAddress(text, null)
            val line1 = text.substring(0, comma).trim()
            val line2 = text.substring(comma + 1).trim().takeIf { it.isNotEmpty() }
            return if (line1.isEmpty()) WhiteListAddress(line2.orEmpty(), null) else WhiteListAddress(line1, line2)
        }
    }
}
