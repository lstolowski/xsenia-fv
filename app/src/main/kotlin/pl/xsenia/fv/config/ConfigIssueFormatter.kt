package pl.xsenia.fv.config

import android.content.res.Resources
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.config.ConfigIssue
import pl.xsenia.fv.invoice.config.ConfigIssueType

/** Turns a [ConfigIssue] into a Polish message, e.g. „counterparties.json, pozycja 2 („Testowa B”): NIP powtarza się”. */
class ConfigIssueFormatter(private val resources: Resources) {

    fun format(issue: ConfigIssue): String {
        val location = buildString {
            append(issue.file.fileName)
            issue.entryIndex?.let { append(", ").append(resources.getString(R.string.config_issue_entry, it + 1)) }
            issue.entryName?.let { append(" (").append(resources.getString(R.string.config_issue_entry_name, it)).append(")") }
        }
        return "$location: ${message(issue)}"
    }

    private fun message(issue: ConfigIssue): String {
        val field = issue.field.orEmpty()
        return when (issue.type) {
            ConfigIssueType.FILE_MISSING -> resources.getString(R.string.config_issue_file_missing)
            ConfigIssueType.INVALID_JSON -> resources.getString(R.string.config_issue_invalid_json, issue.detail.orEmpty())
            ConfigIssueType.MISSING_FIELD -> resources.getString(R.string.config_issue_missing_field, field)
            ConfigIssueType.INVALID_TYPE ->
                if (issue.field == null) {
                    resources.getString(R.string.config_issue_entry_not_object)
                } else {
                    resources.getString(R.string.config_issue_invalid_type, field)
                }
            ConfigIssueType.INVALID_NIP -> resources.getString(R.string.config_issue_invalid_nip)
            ConfigIssueType.INVALID_ACCOUNT_NUMBER -> resources.getString(R.string.config_issue_invalid_account)
            ConfigIssueType.INVALID_CURRENCY -> resources.getString(R.string.config_issue_invalid_currency)
            ConfigIssueType.INVALID_EMAIL -> resources.getString(R.string.config_issue_invalid_email)
            ConfigIssueType.TOO_LONG -> resources.getString(R.string.config_issue_too_long, field, issue.detail.orEmpty())
            ConfigIssueType.EMPTY_LIST -> resources.getString(R.string.config_issue_empty_list)
            ConfigIssueType.DUPLICATE_NIP -> resources.getString(R.string.config_issue_duplicate_nip)
        }
    }
}
