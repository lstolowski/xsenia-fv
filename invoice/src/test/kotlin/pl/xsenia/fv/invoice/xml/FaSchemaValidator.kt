package pl.xsenia.fv.invoice.xml

import org.w3c.dom.ls.LSInput
import org.w3c.dom.ls.LSResourceResolver
import java.io.InputStream
import java.io.Reader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

/** Validates invoice XML against the official FA(2)/FA(3) XSD (test resources, offline). */
object FaSchemaValidator {

    val fa3: Schema by lazy { load("xsd/FA3.xsd") }
    val fa2: Schema by lazy { load("xsd/FA2.xsd") }

    fun validate(schema: Schema, xml: ByteArray) {
        schema.newValidator().validate(StreamSource(xml.inputStream()))
    }

    private fun load(path: String): Schema {
        val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        // Imports point to http://crd.gov.pl/...; resolve them from local copies by file name.
        factory.resourceResolver = LSResourceResolver { _, namespace, publicId, systemId, baseUri ->
            val fileName = systemId.substringAfterLast('/')
            LocalInput(publicId, systemId, baseUri, schemaSource("xsd/bazowe/$fileName"))
        }
        return factory.newSchema(StreamSource(schemaSource(path), path))
    }

    /**
     * Reads a schema, replacing large `maxOccurs` limits (e.g. 10000 invoice lines) with `unbounded`:
     * the JDK validator expands them into content models that exceed its limits / heap.
     * Structure, order and types are still fully validated; only these upper bounds are relaxed.
     */
    private fun schemaSource(path: String): InputStream {
        val text = requireNotNull(FaSchemaValidator::class.java.classLoader.getResource(path)) { "Missing test resource $path" }
            .readText(Charsets.UTF_8)
        return LARGE_MAX_OCCURS.replace(text, "maxOccurs=\"unbounded\"").byteInputStream(Charsets.UTF_8)
    }

    private val LARGE_MAX_OCCURS = Regex("maxOccurs=\"\\d{3,}\"")

    private class LocalInput(
        private var publicId: String?,
        private var systemId: String?,
        private var baseUri: String?,
        private var stream: InputStream?,
    ) : LSInput {
        override fun getCharacterStream(): Reader? = null
        override fun setCharacterStream(characterStream: Reader?) = Unit
        override fun getByteStream(): InputStream? = stream
        override fun setByteStream(byteStream: InputStream?) { stream = byteStream }
        override fun getStringData(): String? = null
        override fun setStringData(stringData: String?) = Unit
        override fun getSystemId(): String? = systemId
        override fun setSystemId(systemId: String?) { this.systemId = systemId }
        override fun getPublicId(): String? = publicId
        override fun setPublicId(publicId: String?) { this.publicId = publicId }
        override fun getBaseURI(): String? = baseUri
        override fun setBaseURI(baseURI: String?) { baseUri = baseURI }
        override fun getEncoding(): String? = null
        override fun setEncoding(encoding: String?) = Unit
        override fun getCertifiedText(): Boolean = false
        override fun setCertifiedText(certifiedText: Boolean) = Unit
    }
}
