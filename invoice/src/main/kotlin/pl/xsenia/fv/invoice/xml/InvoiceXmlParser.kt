package pl.xsenia.fv.invoice.xml

import org.w3c.dom.Element
import org.xml.sax.SAXException
import pl.xsenia.fv.invoice.model.Annotations
import pl.xsenia.fv.invoice.model.BankAccountInfo
import pl.xsenia.fv.invoice.model.CorrectedInvoice
import pl.xsenia.fv.invoice.model.Correction
import pl.xsenia.fv.invoice.model.DatePeriod
import pl.xsenia.fv.invoice.model.DueDate
import pl.xsenia.fv.invoice.model.Invoice
import pl.xsenia.fv.invoice.model.InvoiceLine
import pl.xsenia.fv.invoice.model.Party
import pl.xsenia.fv.invoice.model.Payment
import pl.xsenia.fv.invoice.model.VatCategory
import pl.xsenia.fv.invoice.model.VatSummaryEntry
import java.io.ByteArrayInputStream
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class InvoiceParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Parses FA(2) and FA(3) invoices. Elements are matched by local name, so both schema namespaces work;
 * unknown elements are ignored. Uses DOM (javax.xml), available on the JVM and on Android.
 */
object InvoiceXmlParser {

    // Net / VAT / VAT-in-PLN element names per summary category (FA(2) and FA(3) use the same names).
    private val VAT_FIELDS = listOf(
        Triple(VatCategory.STANDARD, "P_13_1", "P_14_1"),
        Triple(VatCategory.REDUCED_8, "P_13_2", "P_14_2"),
        Triple(VatCategory.REDUCED_5, "P_13_3", "P_14_3"),
        Triple(VatCategory.TAXI_FLAT_RATE, "P_13_4", "P_14_4"),
        Triple(VatCategory.OSS, "P_13_5", "P_14_5"),
        Triple(VatCategory.ZERO_DOMESTIC, "P_13_6_1", null),
        Triple(VatCategory.ZERO_INTRA_EU_SUPPLY, "P_13_6_2", null),
        Triple(VatCategory.ZERO_EXPORT, "P_13_6_3", null),
        Triple(VatCategory.EXEMPT, "P_13_7", null),
        Triple(VatCategory.NOT_TAXABLE_ABROAD, "P_13_8", null),
        Triple(VatCategory.NOT_TAXABLE_ART_100, "P_13_9", null),
        Triple(VatCategory.REVERSE_CHARGE, "P_13_10", null),
        Triple(VatCategory.MARGIN, "P_13_11", null),
    )

    fun parse(xml: ByteArray): Invoice {
        val root = try {
            newDocumentBuilderFactory().newDocumentBuilder().parse(ByteArrayInputStream(xml)).also {
                // Fallback XXE guard for parsers that ignore the disallow-doctype feature (Android).
                if (it.doctype != null) throw InvoiceParseException("DOCTYPE is not allowed in invoice XML")
            }.documentElement
        } catch (e: SAXException) {
            throw InvoiceParseException("Invalid invoice XML", e)
        } catch (e: IOException) {
            throw InvoiceParseException("Cannot read invoice XML", e)
        }
        if (root.localName != "Faktura") throw InvoiceParseException("Root element is ${root.localName}, expected Faktura")

        return try {
            parseInvoice(root)
        } catch (e: DateTimeParseException) {
            throw InvoiceParseException("Invalid date in invoice XML", e)
        } catch (e: NumberFormatException) {
            throw InvoiceParseException("Invalid number in invoice XML", e)
        }
    }

    private fun parseInvoice(root: Element): Invoice {
        val fa = root.required("Fa")
        return Invoice(
            schema = root.child("Naglowek")?.child("KodFormularza")?.getAttribute("kodSystemowy")?.takeIf { it.isNotBlank() } ?: "FA",
            kind = fa.requiredText("RodzajFaktury"),
            invoiceNumber = fa.requiredText("P_2"),
            issueDate = LocalDate.parse(fa.requiredText("P_1")),
            issuePlace = fa.text("P_1M"),
            saleDate = fa.date("P_6"),
            period = fa.child("OkresFa")?.let { DatePeriod(LocalDate.parse(it.requiredText("P_6_Od")), LocalDate.parse(it.requiredText("P_6_Do"))) },
            currency = fa.requiredText("KodWaluty"),
            seller = parseParty(root.required("Podmiot1")),
            buyer = parseParty(root.required("Podmiot2")),
            otherParties = root.children("Podmiot3").map { parseParty(it).copy(role = it.text("Rola") ?: it.text("RolaInna")) },
            lines = fa.children("FaWiersz").map(::parseLine),
            vatSummary = VAT_FIELDS.mapNotNull { (category, netField, vatField) ->
                fa.decimal(netField)?.let { net ->
                    VatSummaryEntry(category, net, vatField?.let { fa.decimal(it) }, vatField?.let { fa.decimal(it + "W") })
                }
            },
            grossTotal = fa.decimal("P_15") ?: throw InvoiceParseException("Missing element P_15"),
            annotations = parseAnnotations(fa.child("Adnotacje")),
            correction = parseCorrection(fa),
            payment = fa.child("Platnosc")?.let(::parsePayment),
            additionalInfo = fa.children("DodatkowyOpis").mapNotNull { info ->
                val key = info.text("Klucz") ?: return@mapNotNull null
                key to info.text("Wartosc").orEmpty()
            },
            footer = root.child("Stopka")?.descendantTexts().orEmpty(),
        )
    }

    private fun parseParty(element: Element): Party {
        val id = element.child("DaneIdentyfikacyjne")
        val address = element.child("Adres")
        val contacts = element.children("DaneKontaktowe")
        return Party(
            name = id?.text("Nazwa"),
            nip = id?.text("NIP"),
            euVatNumber = id?.text("NrVatUE")?.let { (id.text("KodUE").orEmpty()) + it },
            otherId = id?.text("NrID")?.let { listOfNotNull(id.text("KodKraju"), it).joinToString(" ") },
            addressLines = listOfNotNull(address?.text("AdresL1"), address?.text("AdresL2"))
                .flatMap { it.lines() }
                .map(String::trim)
                .filter(String::isNotEmpty),
            countryCode = address?.text("KodKraju"),
            emails = contacts.mapNotNull { it.text("Email") },
            phones = contacts.mapNotNull { it.text("Telefon") },
        )
    }

    private fun parseLine(line: Element) = InvoiceLine(
        number = line.text("NrWierszaFa")?.toIntOrNull(),
        name = line.text("P_7"),
        unit = line.text("P_8A"),
        quantity = line.decimal("P_8B"),
        unitNetPrice = line.decimal("P_9A"),
        unitGrossPrice = line.decimal("P_9B"),
        discount = line.decimal("P_10"),
        netValue = line.decimal("P_11"),
        grossValue = line.decimal("P_11A"),
        vatRate = line.text("P_12"),
        deliveryDate = line.date("P_6A"),
        isStateBefore = line.text("StanPrzed") == "1",
    )

    private fun parseAnnotations(element: Element?) = Annotations(
        cashMethod = element?.text("P_16") == "1",
        selfInvoicing = element?.text("P_17") == "1",
        reverseCharge = element?.text("P_18") == "1",
        splitPayment = element?.text("P_18A") == "1",
        exemptionBasis = element?.child("Zwolnienie")?.let { it.text("P_19A") ?: it.text("P_19B") ?: it.text("P_19C") },
        marginScheme = element?.child("PMarzy")?.text("P_PMarzy") == "1",
    )

    private fun parseCorrection(fa: Element): Correction? {
        val corrected = fa.children("DaneFaKorygowanej")
        if (corrected.isEmpty() && fa.child("PrzyczynaKorekty") == null) return null
        return Correction(
            reason = fa.text("PrzyczynaKorekty"),
            type = fa.text("TypKorekty"),
            correctedInvoices = corrected.map {
                CorrectedInvoice(it.text("NrFaKorygowanej"), it.date("DataWystFaKorygowanej"), it.text("NrKSeFFaKorygowanej"))
            },
        )
    }

    private fun parsePayment(element: Element) = Payment(
        paid = element.text("Zaplacono") == "1",
        paymentDate = element.date("DataZaplaty"),
        dueDates = element.children("TerminPlatnosci").map { term ->
            DueDate(term.date("Termin"), term.child("TerminOpis")?.descendantTexts()?.joinToString(" "))
        },
        methodCode = element.text("FormaPlatnosci")?.toIntOrNull(),
        otherMethod = element.text("OpisPlatnosci"),
        bankAccounts = element.children("RachunekBankowy").mapNotNull { account ->
            val number = account.text("NrRB") ?: return@mapNotNull null
            BankAccountInfo(number, account.text("NazwaBanku"), account.text("SWIFT"), account.text("OpisRachunku"))
        },
    )

    private fun newDocumentBuilderFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        setFeatureIfSupported("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeatureIfSupported(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    }

    private fun DocumentBuilderFactory.setFeatureIfSupported(feature: String, value: Boolean) {
        try {
            setFeature(feature, value)
        } catch (_: ParserConfigurationException) {
            // Not supported by this parser implementation (e.g. on Android) — the DOCTYPE check in parse() covers it.
        }
    }

    // ---- DOM helpers (match elements by local name, ignoring namespaces) ----

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>()

    private fun Element.child(name: String): Element? = childElements().firstOrNull { it.localName == name }

    private fun Element.children(name: String): List<Element> = childElements().filter { it.localName == name }

    private fun Element.required(name: String): Element = child(name) ?: throw InvoiceParseException("Missing element $name")

    private fun Element.text(name: String): String? = child(name)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

    private fun Element.requiredText(name: String): String = text(name) ?: throw InvoiceParseException("Missing element $name")

    private fun Element.decimal(name: String): BigDecimal? = text(name)?.let(::BigDecimal)

    private fun Element.date(name: String): LocalDate? = text(name)?.let(LocalDate::parse)

    /** Texts of all leaf elements, in document order. */
    private fun Element.descendantTexts(): List<String> {
        val elements = childElements()
        if (elements.isEmpty()) return listOfNotNull(textContent?.trim()?.takeIf { it.isNotEmpty() })
        return elements.flatMap { it.descendantTexts() }
    }
}
