package pl.xsenia.fv.ksef

/**
 * Public KSeF API 2.0 environments.
 *
 * @property apiBaseUrl base URL of the REST API (all endpoints are relative to it)
 * @property qrBaseUrl base URL used to build invoice verification links (QR code "KOD I")
 */
enum class KsefEnvironment(val apiBaseUrl: String, val qrBaseUrl: String) {
    TEST("https://api-test.ksef.mf.gov.pl/v2", "https://qr-test.ksef.mf.gov.pl"),
    DEMO("https://api-demo.ksef.mf.gov.pl/v2", "https://qr-demo.ksef.mf.gov.pl"),
    PROD("https://api.ksef.mf.gov.pl/v2", "https://qr.ksef.mf.gov.pl"),
}
