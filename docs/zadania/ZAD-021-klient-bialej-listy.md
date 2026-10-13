# ZAD-021. Klient Białej listy MF: pobieranie danych firmy po NIP-ie

- User story: US-010
- Status: Zrobione (2026-10-13)
- Szacunek: M

## Cel

Czysty Kotlin: jeden klient API Białej listy MF zwracający dane firmy po NIP-ie, gotowe do wypełnienia formularza ([ADR 0007](../adr/0007-biala-lista-mf.md)), oraz serwis aplikacji używany w kilku miejscach.

## Zakres

- [x] Nowy moduł JVM `whitelist` (Ktor client OkHttp, kotlinx.serialization; testy z `MockEngine`; zestaw `integrationTest` poza `build`)
- [x] DTO odpowiedzi `result.subject` (tylko używane pola, nieznane ignorowane) i błędu `{code, message}` — `WhiteListDtos.kt`
- [x] `WhiteListClient.findByNip(nip, date)` → `WhiteListResult`: `Found(WhiteListSubject)`, `NotFound`, `Failed(WhiteListFailure)` — `Network`, `RateLimited` (`429`), `Api(httpStatus, code, message)` (`4xx` z kodem `WL-…`), `Server(httpStatus)` (`5xx` / odpowiedź nie-JSON / nieoczekiwany JSON); limit czasu 15 s; wyjątki sieciowe nie wychodzą poza klienta; treść odpowiedzi nie jest logowana; NIP inny niż 10 cyfr → `IllegalArgumentException` (bez zapytania)
- [x] Mapowanie: `vatStatus` (napis z rejestru) + `isActiveVatPayer` („Czynny”), adres `workingAddress ?: residenceAddress` → `WhiteListAddress.parse` (podział po ostatnim przecinku), `accountNumbers`, `regon`, `krs`; nazwa bez zmiany pisowni
- [x] Fixture'y JSON (`whitelist/src/test/resources/whitelist/`) z zestawów w [docs/przyklady/biala-lista.md](../przyklady/biala-lista.md) — pełny format odpowiedzi MF, dane zmyślone
- [x] `CompanyLookupService` w aplikacji (`pl.xsenia.fv.lookup`, w `AppContainer.companyLookup`): normalizacja i walidacja NIP-u (`NipValidator`, akceptuje spacje, myślniki, `PL`) przed zapytaniem, data „dziś” w strefie Europe/Warsaw, wynik `LookedUpCompany` (nazwa, adres 1/2, kraj „Polska”, status VAT, poprawne i niepowtarzające się rachunki); `CompanyLookupMessages` — polskie komunikaty z `strings.xml`, w tym „Firma nie jest czynnym podatnikiem”
- [x] Test integracyjny `WhiteListIntegrationTest`: testowy NIP → `NotFound` na prawdziwym API

## Definicja ukończenia

- [x] Testy: `WhiteListClientTest` (14 — wszystkie zestawy z `biala-lista.md`: JDG, spółka, oba adresy, zwolniony, adres bez przecinka, nie znaleziono, `WL-113`, `WL-103`, `429`, `5xx`, nieoczekiwany JSON, brak sieci, niepoprawny NIP), `WhiteListAddressTest` (4), `CompanyLookupServiceTest` (5); integracyjny (1) zielony na `wl-api.mf.gov.pl`; `./gradlew build` (z lintem i R8) zielony
- [x] Brak prawdziwych NIP-ów i danych z rejestru w repozytorium
- [x] Zaktualizowana dokumentacja / słownik (ADR 0002 — lista modułów, AGENTS.md — komendy testów)

## Uwagi

- Format sprawdzony 2026-10-13 na prawdziwych odpowiedziach (JDG, spółka, urząd) — dane nie trafiły do repozytorium. Spółki i urzędy mają adres w `workingAddress`, osoby fizyczne w `residenceAddress`; „ul.” nie występuje; duże firmy mają dziesiątki–setki rachunków (`hasVirtualAccounts`) — okno wyboru rachunków w ZAD-023 musi być przewijane.
- Kod błędu przy przekroczeniu limitu nie jest udokumentowany — przyjęto `429`; inny kod `WL-…` pokaże się jako „Biała lista odrzuciła zapytanie: … (kod)”.
- Weryfikacja na emulatorze — razem z UI w ZAD-023 (serwis nie ma jeszcze miejsca w interfejsie). Działanie po R8 (kotlinx.serialization w module `whitelist`) do sprawdzenia przy `releaseApkTest` w ZAD-023.
