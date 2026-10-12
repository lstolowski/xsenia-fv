package pl.xsenia.fv.ksef.support

import org.junit.jupiter.api.Assumptions.assumeTrue
import pl.xsenia.fv.ksef.KsefEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Credentials for integration tests, read from `secrets/ksef-test.properties` (ignored by git).
 *
 * Integration tests may only ever talk to [KsefEnvironment.TEST]; any other configuration is rejected.
 */
class KsefTestConfig private constructor(
    val environment: KsefEnvironment,
    val nip: String,
    val ksefToken: String,
    /** Optional second TEST context — a fictitious supplier issuing purchase invoices to [nip]. */
    val supplier: Context?,
) {
    class Context(val nip: String, val ksefToken: String) {
        override fun toString(): String = "Context(nip=$nip, ksefToken=***)"
    }

    // Never expose the tokens in test reports or logs.
    override fun toString(): String = "KsefTestConfig(environment=$environment, nip=$nip, ksefToken=***, supplier=$supplier)"

    companion object {
        const val SECRETS_FILE_PROPERTY = "ksef.secretsFile"

        private val NIP_PATTERN = Regex("\\d{10}")

        /**
         * Loads the configuration from the file passed by Gradle in [SECRETS_FILE_PROPERTY],
         * or aborts (skips) the calling test when the file is not available.
         */
        fun loadOrSkip(path: Path? = System.getProperty(SECRETS_FILE_PROPERTY)?.let(Path::of)): KsefTestConfig {
            assumeTrue(
                path != null && Files.isRegularFile(path),
                "KSeF secrets file not found ($path) — skipping integration test",
            )
            return load(path!!)
        }

        fun load(path: Path): KsefTestConfig {
            val properties = Properties().apply { Files.newBufferedReader(path).use(::load) }

            val environmentName = properties.required("ksef.environment")
            check(environmentName == KsefEnvironment.TEST.name) {
                "Integration tests may only run against KSeF TEST, but ksef.environment=$environmentName"
            }
            properties.getProperty("ksef.apiBaseUrl")?.let { url ->
                check(url == KsefEnvironment.TEST.apiBaseUrl) {
                    "Integration tests may only run against ${KsefEnvironment.TEST.apiBaseUrl}, but ksef.apiBaseUrl=$url"
                }
            }

            val nip = properties.required("ksef.nip")
            check(NIP_PATTERN.matches(nip)) { "ksef.nip must have 10 digits" }

            val supplier = properties.getProperty("ksef.supplier.nip")?.trim()?.takeIf { it.isNotEmpty() }?.let { supplierNip ->
                check(NIP_PATTERN.matches(supplierNip)) { "ksef.supplier.nip must have 10 digits" }
                Context(supplierNip, properties.required("ksef.supplier.token"))
            }
            return KsefTestConfig(KsefEnvironment.TEST, nip, properties.required("ksef.token"), supplier)
        }

        private fun Properties.required(key: String): String =
            getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Missing required property $key in KSeF secrets file")
    }
}
