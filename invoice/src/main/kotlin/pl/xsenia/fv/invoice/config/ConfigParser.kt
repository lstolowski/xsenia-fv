package pl.xsenia.fv.invoice.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import pl.xsenia.fv.invoice.validation.BankAccountNumberValidator
import pl.xsenia.fv.invoice.validation.NipValidator

/** Parsing result: valid data (if any) and all issues found. */
class ParsedConfig<T>(val value: T, val issues: List<ConfigIssue>)

/**
 * Parses the user-edited configuration files (ADR 0004). Every problem is reported; parsing never throws.
 *
 * A `null` file content means the file does not exist.
 */
object ConfigParser {

    // FA(3) limits: TZnakowy512 (names, address lines), TZnakowy (bank name), TAdresEmail, TNumerTelefonu.
    private const val MAX_NAME = 512
    private const val MAX_ADDRESS_LINE = 512
    private const val MAX_BANK_NAME = 256
    private const val MAX_EMAIL = 255
    private const val MAX_PHONE = 16
    private const val MAX_SHORT = 64

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val CURRENCY = Regex("^[A-Z]{3}$")

    const val DEFAULT_CURRENCY = "PLN"

    /** Returns the company, or null when the file is missing or has any issue. */
    fun parseCompany(content: String?): ParsedConfig<Company?> {
        val issues = mutableListOf<ConfigIssue>()
        val root = parseRoot(content, ConfigFile.COMPANY, issues) ?: return ParsedConfig(null, issues)
        val companyObject = root["company"]
        if (companyObject !is JsonObject) {
            val type = if (companyObject == null) ConfigIssueType.MISSING_FIELD else ConfigIssueType.INVALID_TYPE
            issues += ConfigIssue(ConfigFile.COMPANY, type, field = "company")
            return ParsedConfig(null, issues)
        }
        // Field paths are relative to the company object, like those of list entries.
        val context = EntryContext(ConfigFile.COMPANY, entryIndex = null, issues)
        val company = readCompany(JsonObjectReader(companyObject, context), context)
        return ParsedConfig(company.takeUnless { context.hasIssues }, issues)
    }

    /** Returns valid bank accounts; invalid entries are skipped and reported. */
    fun parseBanks(content: String?): ParsedConfig<List<BankAccount>> =
        parseList(content, ConfigFile.BANKS, "banks") { reader, context -> readBankAccount(reader, context) }

    /** Returns valid counterparties; invalid entries and duplicate NIPs are skipped and reported. */
    fun parseCounterparties(content: String?): ParsedConfig<List<Counterparty>> {
        val seenNips = mutableSetOf<String>()
        return parseList(content, ConfigFile.COUNTERPARTIES, "counterparties") { reader, context ->
            readCounterparty(reader, context)?.let { counterparty ->
                if (seenNips.add(counterparty.nip)) {
                    counterparty
                } else {
                    reader.report(ConfigIssueType.DUPLICATE_NIP, "nip")
                    null
                }
            }
        }
    }

    private fun parseRoot(content: String?, file: ConfigFile, issues: MutableList<ConfigIssue>): JsonObject? {
        if (content == null) {
            issues += ConfigIssue(file, ConfigIssueType.FILE_MISSING)
            return null
        }
        val element = try {
            Json.parseToJsonElement(content)
        } catch (e: SerializationException) {
            issues += ConfigIssue(file, ConfigIssueType.INVALID_JSON, detail = e.message)
            return null
        }
        if (element !is JsonObject) {
            issues += ConfigIssue(file, ConfigIssueType.INVALID_JSON, detail = "root must be a JSON object")
            return null
        }
        return element
    }

    private fun <T : Any> parseList(
        content: String?,
        file: ConfigFile,
        listField: String,
        readEntry: (JsonObjectReader, EntryContext) -> T?,
    ): ParsedConfig<List<T>> {
        val issues = mutableListOf<ConfigIssue>()
        val root = parseRoot(content, file, issues) ?: return ParsedConfig(emptyList(), issues)

        val array = root[listField]
        if (array !is JsonArray) {
            val type = if (array == null) ConfigIssueType.MISSING_FIELD else ConfigIssueType.INVALID_TYPE
            issues += ConfigIssue(file, type, field = listField)
            return ParsedConfig(emptyList(), issues)
        }
        if (array.isEmpty()) issues += ConfigIssue(file, ConfigIssueType.EMPTY_LIST, field = listField)

        val values = array.mapIndexedNotNull { index, element ->
            val context = EntryContext(file, index, issues)
            if (element !is JsonObject) {
                context.report(ConfigIssueType.INVALID_TYPE, field = null)
                return@mapIndexedNotNull null
            }
            readEntry(JsonObjectReader(element, context), context).takeUnless { context.hasIssues }
        }
        return ParsedConfig(values, issues)
    }

    private fun readCompany(reader: JsonObjectReader, context: EntryContext): Company? {
        val name = reader.string("name", required = true, MAX_NAME).also { context.entryName = it }
        val nip = reader.nip()
        val address = reader.address()
        val email = reader.email()
        val phone = reader.string("phone", required = false, MAX_PHONE)
        if (name == null || nip == null || address == null) return null
        return Company(name, nip, address, email, phone)
    }

    private fun readBankAccount(reader: JsonObjectReader, context: EntryContext): BankAccount? {
        val name = reader.string("name", required = true, MAX_NAME).also { context.entryName = it }
        val bank = reader.string("bank", required = false, MAX_BANK_NAME)
        val number = reader.string("account", required = true, MAX_SHORT, allowNumber = true)?.let { raw ->
            BankAccountNumberValidator.normalize(raw)
                ?: run { reader.report(ConfigIssueType.INVALID_ACCOUNT_NUMBER, "account"); null }
        }
        val currency = reader.string("currency", required = false, MAX_SHORT)?.let { raw ->
            raw.uppercase().takeIf(CURRENCY::matches)
                ?: run { reader.report(ConfigIssueType.INVALID_CURRENCY, "currency"); null }
        } ?: DEFAULT_CURRENCY
        if (name == null || number == null) return null
        return BankAccount(name, bank, number, currency)
    }

    private fun readCounterparty(reader: JsonObjectReader, context: EntryContext): Counterparty? {
        val name = reader.string("name", required = true, MAX_NAME).also { context.entryName = it }
        val nip = reader.nip()
        val address = reader.address()
        val email = reader.email()
        if (name == null || nip == null || address == null) return null
        return Counterparty(name, nip, address, email)
    }

    private fun JsonObjectReader.nip(): String? =
        string("nip", required = true, MAX_SHORT, allowNumber = true)?.let { raw ->
            NipValidator.normalize(raw) ?: run { report(ConfigIssueType.INVALID_NIP, "nip"); null }
        }

    private fun JsonObjectReader.email(): String? =
        string("email", required = false, MAX_EMAIL)?.let { raw ->
            raw.takeIf(EMAIL::matches) ?: run { report(ConfigIssueType.INVALID_EMAIL, "email"); null }
        }

    private fun JsonObjectReader.address(): Address? {
        val address = obj("address", required = true) ?: return null
        val line1 = address.string("line1", required = true, MAX_ADDRESS_LINE)
        val line2 = address.string("line2", required = false, MAX_ADDRESS_LINE)
        val country = address.string("country", required = false, MAX_SHORT)
        return line1?.let { Address(it, line2, country) }
    }
}
