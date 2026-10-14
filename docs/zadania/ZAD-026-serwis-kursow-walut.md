# ZAD-026. Serwis kursów walut: klient NBP, pamięć podręczna, reguła daty kursu

- User story: US-011
- Status: Zrobione (2026-10-13)
- Szacunek: M

## Cel

Czysty Kotlin: jeden klient API NBP (tabela A) zwracający kurs średni EUR, USD lub GBP w PLN z danego dnia lub z ostatniego dnia roboczego przed nim, oraz serwis aplikacji używany w kilku miejscach ([ADR 0008](../adr/0008-kursy-walut-nbp.md)).

## Zakres

- [x] Nowy moduł JVM `exchangerates` (`settings.gradle.kts`, zależności jak `whitelist`: Ktor client, kotlinx.serialization; testy z `MockEngine`)
- [x] `InvoiceCurrency` (`PLN`, `EUR`, `USD`, `GBP`; `DEFAULT = PLN`) w module `invoice` — używany przez model faktury i klienta
- [x] DTO odpowiedzi `GET /api/exchangerates/rates/a/{code}/{date}/?format=json` (`table`, `code`, `rates[]`: `no`, `effectiveDate`, `mid`; ignorowanie nieznanych pól)
- [x] `NbpClient.rateOn(currency, date)` → `ExchangeRateResult`: `Found(ExchangeRate)`, `NotPublished` (`404`), `Failure(NETWORK | SERVER)`; `ExchangeRate(currency, rate` — `mid` z 4 miejscami`, tableNumber, effectiveDate)` w module `invoice` (pakiet `currency`); limit czasu 15 s; PLN — błąd programisty (nie pytamy o kurs PLN)
- [x] `lastRateBefore(currency, date)` (rozszerzenie `ExchangeRateSource`) — `rateOn` dla kolejnych dni wstecz od `date − 1`, maks. 10 dni; po wyczerpaniu — `NotPublished`, pierwszy błąd przerywa
- [x] `ExchangeRateDateRule.referenceDate(deliveryDate, issueDay) = min(...)` w module `invoice` (czysta logika, przykłady z analizy 04)
- [x] `CachedExchangeRateSource` (moduł `exchangerates`): pamięć podręczna w pliku — kursy i dni bez tabeli, tylko dla dni sprzed dziś (Europe/Warsaw); błędy nie są zapamiętywane
- [x] `ExchangeRateService` w aplikacji (`AppContainer.exchangeRates`): `rateOn`, `lastRateBefore`, `rateFor(currency, deliveryDate, issueDay)`; plik `cacheDir/exchange-rates.json` (kursy nie zależą od firmy, więc nie w profilu; „Wyczyść cache” je usuwa); polskie komunikaty błędów (`problemMessage`) w `strings.xml`
- [x] Fixture'y JSON w zasobach testowych wg [docs/przyklady/kursy-nbp.md](../przyklady/kursy-nbp.md)
- [x] Test integracyjny (`./gradlew :exchangerates:integrationTest`, bez sekretów, poza `build`): kurs USD z 2016-04-04 = `3.7254`, niedziela bez tabeli, kurs EUR przed 2026-10-12 = piątkowy `4.3775`

## Definicja ukończenia

- [x] Testy: kurs z dnia, dzień bez tabeli (`404`), „przed dniem” przez weekend i kilkudniowe święta, limit 10 dni, skala 4 (`4.3775`, także `mid` z mniejszą liczbą cyfr, np. `4.38` → `4.3800`), brak sieci, `5xx`, odpowiedź nie-JSON, pamięć podręczna (drugie zapytanie bez sieci, dziś nie jest zapamiętywane)
- [x] Zaktualizowana dokumentacja / słownik
