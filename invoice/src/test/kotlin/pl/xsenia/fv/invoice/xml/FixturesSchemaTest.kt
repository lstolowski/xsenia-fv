package pl.xsenia.fv.invoice.xml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.xml.sax.SAXException
import org.junit.jupiter.params.provider.CsvSource

/** Test invoices must be valid against the official schemas, so the parser is tested on realistic input. */
class FixturesSchemaTest {

    @ParameterizedTest
    @CsvSource("fa3-vat.xml, FA3", "fa3-multi-rate.xml, FA3", "fa3-correction.xml, FA3", "fa2-vat.xml, FA2")
    fun `fixture is valid against XSD`(file: String, schema: String) {
        val xml = requireNotNull(javaClass.classLoader.getResource("invoices/$file")).readBytes()
        FaSchemaValidator.validate(if (schema == "FA3") FaSchemaValidator.fa3 else FaSchemaValidator.fa2, xml)
    }

    @Test
    fun `validator rejects an invoice without number`() {
        val xml = requireNotNull(javaClass.classLoader.getResource("invoices/fa3-vat.xml")).readText()
            .replace("<P_2>01/09/2026</P_2>", "")

        assertThrows<SAXException> { FaSchemaValidator.validate(FaSchemaValidator.fa3, xml.toByteArray()) }
    }
}
