# ZAD-021. Klient Białej listy MF: pobieranie danych firmy po NIP-ie

- User story: US-010
- Status: Do zrobienia
- Szacunek: M

## Cel

Czysty Kotlin: jeden klient API Białej listy MF zwracający dane firmy po NIP-ie, gotowe do wypełnienia formularza ([ADR 0007](../adr/0007-biala-lista-mf.md)), oraz serwis aplikacji używany w kilku miejscach.

## Zakres

- [ ] Nowy moduł JVM `whitelist` (`settings.gradle.kts`, zależności jak `ksef`: Ktor client, kotlinx.serialization; testy z `MockEngine`)
- [ ] DTO odpowiedzi `result.subject` (ignorowanie nieznanych pól) i błędu `{code, message}`
- [ ] `WhiteListClient.findByNip(nip, date)` → `WhiteListResult`: `Found(WhiteListSubject)`, `NotFound`, `Failure` (`Network`, `RateLimit`, `Server`, `Api(code, message)`); limit czasu 15 s; bez logowania treści odpowiedzi
- [ ] Mapowanie: `vatStatus` (czynny / zwolniony / inny — z oryginalnym napisem), adres `workingAddress ?: residenceAddress` rozbity po ostatnim przecinku na `line1`/`line2`, `accountNumbers`, nazwa bez zmiany pisowni
- [ ] Fixture'y JSON w zasobach testowych z zestawów w [docs/przyklady/biala-lista.md](../przyklady/biala-lista.md) (zmyślone dane, format sprawdzony na prawdziwych odpowiedziach)
- [ ] `CompanyLookupService` w aplikacji: normalizacja i walidacja NIP-u (`NipValidator`) przed zapytaniem, data „dziś” w strefie Europe/Warsaw, wynik dla formularza; polskie komunikaty błędów w `strings.xml`
- [ ] Test integracyjny (`integrationTest`, poza `build`): testowy NIP → `NotFound` (sprawdza, że adres i format API się nie zmieniły); żadnych prawdziwych NIP-ów

## Definicja ukończenia

- [ ] Testy: wszystkie zestawy z `biala-lista.md` (JDG, spółka, oba adresy, zwolniony, adres bez przecinka, nie znaleziono, `WL-113`, `WL-103`, `429`, `5xx`, brak sieci), podział adresu, niepoprawny NIP bez zapytania
- [ ] Brak prawdziwych NIP-ów i danych z rejestru w repozytorium
- [ ] Zaktualizowana dokumentacja / słownik
