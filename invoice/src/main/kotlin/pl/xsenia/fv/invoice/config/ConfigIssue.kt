package pl.xsenia.fv.invoice.config

/** Configuration files kept by the user in a shared folder (ADR 0004). */
enum class ConfigFile(val fileName: String) {
    COMPANY("company.json"),
    BANKS("banks.json"),
    COUNTERPARTIES("counterparties.json"),
}

enum class ConfigIssueType {
    FILE_MISSING,
    INVALID_JSON,
    MISSING_FIELD,
    INVALID_TYPE,
    INVALID_NIP,
    INVALID_ACCOUNT_NUMBER,
    INVALID_CURRENCY,
    INVALID_EMAIL,
    TOO_LONG,
    EMPTY_LIST,
    DUPLICATE_NIP,
}

/**
 * Problem found in a configuration file. The UI turns it into a Polish message.
 *
 * @property entryIndex 0-based position in the list (`banks`, `counterparties`); null for `company.json` or file-level issues
 * @property entryName `name` of the entry, when available, to help the user find it
 * @property field JSON path inside the entry, e.g. `address.line1`; null for file-level issues
 * @property detail technical detail (parser message, length limit)
 */
data class ConfigIssue(
    val file: ConfigFile,
    val type: ConfigIssueType,
    val entryIndex: Int? = null,
    val entryName: String? = null,
    val field: String? = null,
    val detail: String? = null,
)
