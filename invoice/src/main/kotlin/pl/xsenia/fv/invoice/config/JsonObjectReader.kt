package pl.xsenia.fv.invoice.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Issue context shared by all readers of one entry (one company / bank account / counterparty). */
internal class EntryContext(
    val file: ConfigFile,
    val entryIndex: Int?,
    private val issues: MutableList<ConfigIssue>,
) {
    var entryName: String? = null
    var hasIssues = false
        private set

    fun report(type: ConfigIssueType, field: String?, detail: String? = null) {
        hasIssues = true
        issues += ConfigIssue(file, type, entryIndex, entryName, field, detail)
    }
}

/** Reads fields of a JSON object, reporting every problem instead of failing on the first one. */
internal class JsonObjectReader(
    private val json: JsonObject,
    private val context: EntryContext,
    private val pathPrefix: String = "",
) {

    /**
     * Returns a trimmed, non-blank string, or null (reporting an issue if [required]).
     * With [allowNumber], a JSON number is accepted too (e.g. a NIP written without quotes).
     */
    fun string(field: String, required: Boolean, maxLength: Int, allowNumber: Boolean = false): String? {
        val element = json[field]
        if (element == null || element is JsonNull) return missing(field, required)
        if (element !is JsonPrimitive || !(element.isString || allowNumber && element.content.all(Char::isDigit))) {
            context.report(ConfigIssueType.INVALID_TYPE, path(field))
            return null
        }
        val value = element.content.trim()
        if (value.isEmpty()) return missing(field, required)
        if (value.length > maxLength) {
            context.report(ConfigIssueType.TOO_LONG, path(field), "max $maxLength")
            return null
        }
        return value
    }

    fun obj(field: String, required: Boolean): JsonObjectReader? {
        val element: JsonElement? = json[field]
        if (element == null || element is JsonNull) return missing(field, required)
        if (element !is JsonObject) {
            context.report(ConfigIssueType.INVALID_TYPE, path(field))
            return null
        }
        return JsonObjectReader(element, context, path(field) + ".")
    }

    fun report(type: ConfigIssueType, field: String, detail: String? = null) = context.report(type, path(field), detail)

    private fun <T> missing(field: String, required: Boolean): T? {
        if (required) context.report(ConfigIssueType.MISSING_FIELD, path(field))
        return null
    }

    private fun path(field: String) = pathPrefix + field
}
