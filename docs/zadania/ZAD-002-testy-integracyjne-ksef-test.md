# ZAD-002. Infrastruktura testów integracyjnych z KSeF TEST

- User story: US-002, US-006
- Status: Zrobione (2026-10-11)
- Szacunek: S

## Cel

Testy w modułach JVM mogą łączyć się z KSeF TEST danymi z `secrets/ksef-test.properties`.

## Zakres

- [x] Odczyt `secrets/ksef-test.properties` w testach (`KsefTestConfig.loadOrSkip()` — pominięcie testów, gdy pliku brak); token maskowany w `toString()`
- [x] Osobny zestaw testów Gradle (JVM Test Suite) `integrationTest` w module `ksef` (`src/integrationTest/kotlin`), nieuruchamiany przez `test`/`check`/`build`, bez cache wyników
- [x] Ochrona przed przypadkowym użyciem PROD: konfiguracja odrzuca `ksef.environment` ≠ TEST i `ksef.apiBaseUrl` ≠ adres TEST (pokryte testami)
- [x] Test dymny: token pasuje do NIP, KSeF TEST wydaje `auth/challenge`

## Uwagi

- Testy integracyjne uruchamiać poza oknem prac serwisowych TEST (16:00–18:00)

## Definicja ukończenia

- [x] Testy (`KsefTestConfigTest` — 6, `KsefTestEnvironmentIntegrationTest` — 2)
- [x] Sprawdzone na środowisku KSeF TEST
- [x] Zaktualizowana dokumentacja (AGENTS.md)

## Jak dodać test integracyjny

```kotlin
class SomethingIntegrationTest {
    private val config = KsefTestConfig.loadOrSkip() // skips when secrets are missing
    // use config.environment.apiBaseUrl, config.nip, config.ksefToken
}
```

Pliki w `ksef/src/integrationTest/kotlin`, nazwa klasy z sufiksem `IntegrationTest`.
