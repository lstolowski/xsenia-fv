# ZAD-026. Serwis kursów walut: klient NBP, pamięć podręczna, reguła daty kursu

- User story: US-011
- Status: Do zrobienia
- Szacunek: M

## Cel

Czysty Kotlin: jeden klient API NBP (tabela A) zwracający kurs średni EUR, USD lub GBP w PLN z danego dnia lub z ostatniego dnia roboczego przed nim, oraz serwis aplikacji używany w kilku miejscach ([ADR 0008](../adr/0008-kursy-walut-nbp.md)).

## Zakres

- [ ] Nowy moduł JVM `exchangerates` (`settings.gradle.kts`, zależności jak `whitelist`: Ktor client, kotlinx.serialization; testy z `MockEngine`)
- [ ] `InvoiceCurrency` (`PLN`, `EUR`, `USD`, `GBP`; `DEFAULT = PLN`) w module `invoice` — używany przez model faktury i klienta
- [ ] DTO odpowiedzi `GET /api/exchangerates/rates/a/{code}/{date}/?format=json` (`table`, `code`, `rates[]`: `no`, `effectiveDate`, `mid`; ignorowanie nieznanych pól)
- [ ] `NbpClient.rateOn(currency, date)` → `ExchangeRateResult`: `Found(ExchangeRate)`, `NotPublished` (`404`), `Failure` (`Network`, `Server`); `ExchangeRate(currency, rate` — `mid` z 4 miejscami`, tableNumber, effectiveDate)`; limit czasu 15 s; PLN — błąd programisty (nie pytamy o kurs PLN)
- [ ] `NbpClient.lastRateBefore(currency, date)` — `rateOn` dla kolejnych dni wstecz od `date − 1`, maks. 10 dni; po wyczerpaniu — `Failure`
- [ ] `ExchangeRateDateRule.referenceDate(deliveryDate, issueDay) = min(...)` w module `invoice` (czysta logika, przykłady z analizy 04)
- [ ] `ExchangeRateService` w aplikacji: `rateFor(currency, deliveryDate, issueDay)`; pamięć podręczna `profiles/{nip}/exchange-rates.json` — kursy i dni bez tabeli, tylko dla dni sprzed dziś; polskie komunikaty błędów w `strings.xml`
- [ ] Fixture'y JSON w zasobach testowych wg [docs/przyklady/kursy-nbp.md](../przyklady/kursy-nbp.md)
- [ ] Test integracyjny (`integrationTest`, poza `build`): kurs USD z 2016-04-04 = `3.7254` (format API bez zmian)

## Definicja ukończenia

- [ ] Testy: kurs z dnia, dzień bez tabeli (`404`), „przed dniem” przez weekend i kilkudniowe święta, limit 10 dni, skala 4 (`4.3775`, także `mid` z mniejszą liczbą cyfr, np. `4.38` → `4.3800`), brak sieci, `5xx`, odpowiedź nie-JSON, pamięć podręczna (drugie zapytanie bez sieci, dziś nie jest zapamiętywane)
- [ ] Zaktualizowana dokumentacja / słownik
