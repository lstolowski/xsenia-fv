package pl.xsenia.fv.ksef.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.TestAbortedException
import pl.xsenia.fv.ksef.KsefEnvironment
import java.nio.file.Files
import java.nio.file.Path

class KsefTestConfigTest {

    @TempDir
    lateinit var dir: Path

    private fun secretsFile(content: String): Path = dir.resolve("ksef-test.properties").also { Files.writeString(it, content) }

    @Test
    fun `loads TEST configuration`() {
        val config = KsefTestConfig.load(
            secretsFile(
                """
                ksef.environment=TEST
                ksef.apiBaseUrl=https://api-test.ksef.mf.gov.pl/v2
                ksef.nip=1111111111
                ksef.token=ref|nip-1111111111|secret
                """.trimIndent(),
            ),
        )

        assertEquals(KsefEnvironment.TEST, config.environment)
        assertEquals("1111111111", config.nip)
        assertEquals("ref|nip-1111111111|secret", config.ksefToken)
    }

    @Test
    fun `rejects non-TEST environment`() {
        val file = secretsFile("ksef.environment=PROD\nksef.nip=1111111111\nksef.token=x")

        assertThrows<IllegalStateException> { KsefTestConfig.load(file) }
    }

    @Test
    fun `rejects production API URL even when environment says TEST`() {
        val file = secretsFile(
            "ksef.environment=TEST\nksef.apiBaseUrl=https://api.ksef.mf.gov.pl/v2\nksef.nip=1111111111\nksef.token=x",
        )

        assertThrows<IllegalStateException> { KsefTestConfig.load(file) }
    }

    @Test
    fun `rejects missing token`() {
        val file = secretsFile("ksef.environment=TEST\nksef.nip=1111111111\nksef.token=")

        assertThrows<IllegalStateException> { KsefTestConfig.load(file) }
    }

    @Test
    fun `skips test when secrets file is missing`() {
        assertThrows<TestAbortedException> { KsefTestConfig.loadOrSkip(dir.resolve("missing.properties")) }
        assertThrows<TestAbortedException> { KsefTestConfig.loadOrSkip(null) }
    }

    @Test
    fun `does not expose token in toString`() {
        val config = KsefTestConfig.load(secretsFile("ksef.environment=TEST\nksef.nip=1111111111\nksef.token=top-secret"))

        assertFalse(config.toString().contains("top-secret"))
    }
}
