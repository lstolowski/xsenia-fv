# 0007. Pobieranie danych firmy po NIP-ie z Białej listy MF

- Status: Zaakceptowany
- Data: 2026-10-13

## Kontekst

- Formularze „Moja firma” i „Kontrahent” ([ADR 0006](0006-dane-firmy-w-aplikacji.md)) mają się wypełniać danymi pobranymi po NIP-ie. Mechanizm ma być jednym serwisem używanym w kilku miejscach (Moja firma, kontrahent, pierwsze uruchomienie).
- Źródło wskazane przez użytkownika: API wykazu podatników VAT („Biała lista”) Ministerstwa Finansów:
  `GET https://wl-api.mf.gov.pl/api/search/nip/{nip}?date={yyyy-MM-dd}` — NIP 10 cyfr bez separatorów, data nie może być z przyszłości.
- API jest publiczne, bez uwierzytelnienia. Limit (KAS, strona „Jak korzystać z wykazu”): **100 zapytań dziennie** (do 30 podmiotów w jednym zapytaniu); po przekroczeniu dostęp może być zablokowany do północy.
- Sprawdzone 2026-10-13 na prawdziwych odpowiedziach:
  - `result.subject`: `name`, `nip`, `statusVat` („Czynny”, „Zwolniony”, …), `regon`, `krs`, `residenceAddress`, `workingAddress`, `accountNumbers[]` (NRB, 26 cyfr), daty rejestracji/wykreślenia i inne,
  - nazwa i adres **wielkimi literami**, adres w jednym napisie „ULICA NR, KOD MIEJSCOWOŚĆ”, bez „ul.”; u spółek i urzędów w `workingAddress`,
  - duże firmy mają dziesiątki–setki rachunków (`hasVirtualAccounts: true` — rachunki wirtualne),
  - dla osoby fizycznej (JDG) `name` to imię i nazwisko, nie nazwa firmy; adres w `residenceAddress`, `workingAddress` bywa `null`,
  - nieznany NIP → `200` z `subject: null`; błędy → `400` z `{"code": "WL-1xx", "message": "…"}` (np. `WL-113` — zła długość NIP, `WL-103` — data z przyszłości).
- API nie zna testowych NIP-ów, a środowisko testowe `wl-test.mf.gov.pl` nie zwraca danych (sprawdzone 2026-10-13) — logiki nie da się testować na danych zmyślonych w prawdziwym API.
- To nie jest KSeF: zakaz łączenia z KSeF DEMO/PROD w developmencie nie dotyczy Białej listy (tylko odczyt publicznych danych).

## Rozważane opcje

1. **API Białej listy MF** — bez klucza, REST/JSON, status VAT i rachunki; adres w jednym napisie, wielkie litery.
2. API BIR GUS (REGON) — adres w osobnych polach, ale wymaga klucza, SOAP, brak statusu VAT i rachunków.
3. Plik płaski Białej listy — do masowej weryfikacji, nie do wyszukiwania danych firmy.

## Decyzja

**Opcja 1.**

### Klient i serwis

- Nowy moduł JVM `whitelist` (pakiet `pl.xsenia.fv.whitelist`): klient Ktor + kotlinx.serialization (jak moduł `ksef`), adres `https://wl-api.mf.gov.pl` w obu wersjach aplikacji (`ksefTest` i `ksefProd`).
- `WhiteListClient.findByNip(nip, date): WhiteListResult`:
  - `Found(WhiteListSubject)` — `name`, `nip`, `vatStatus`, `address` (`line1`, `line2`), `accountNumbers`, `regon`, `krs`,
  - `NotFound` — `subject: null`,
  - `Failure` — `Network` (brak sieci, przekroczenie czasu 15 s), `RateLimit` (`429`), `Server` (`5xx`, odpowiedź nie-JSON), `Api(code, message)` (`400` z kodem `WL-…`).
- Serwis w aplikacji (`CompanyLookupService`): normalizuje i waliduje NIP (suma kontrolna — niepoprawny NIP nie jest wysyłany), używa daty „dziś” w strefie Europe/Warsaw, mapuje wynik na stan formularza. Używany w formularzach „Moja firma”, „Kontrahent” i przy pierwszym uruchomieniu.

### Mapowanie na formularz

- **Nadpisywane** są nazwa i adres (decyzja Q2: 1a); e-mail, telefon i pozostałe pola bez zmian. NIP nie jest zmieniany.
- Pisownia **jak w rejestrze** (wielkie litery, bez zamiany na małe — Q2: 2a); poprawki ręcznie.
- Adres: `workingAddress`, a gdy brak — `residenceAddress`; podział po **ostatnim** przecinku: `line1` = część przed, `line2` = część po; bez przecinka — całość w `line1`, `line2` puste. `country` = „Polska”.
- Status VAT inny niż „Czynny” → ikona ostrzeżenia i komunikat **„Firma nie jest czynnym podatnikiem”** (Q2: 4). Status nie jest zapisywany — widoczny w formularzu po pobraniu.
- `subject: null` → „Nie znaleziono firmy w Białej liście”, pola bez zmian.
- Rachunki (tylko „Moja firma”, Q3: A): po pobraniu przewijane okno z rachunkami z rejestru do zaznaczenia (domyślnie nic nie jest zaznaczone) (rachunki już zapisane — oznaczone i pominięte); zaznaczone są dodawane z nazwą „Rachunek …{ostatnie 4 cyfry}”, pustym bankiem i walutą PLN — do uzupełnienia ręcznie.

### Ruch i prywatność

- Zapytanie tylko na żądanie (przycisk pobierania obok NIP-u) i raz automatycznie na kroku „Moja firma” pierwszego uruchomienia — bez cyklicznego sprawdzania; przy limicie 100/dzień nie potrzeba pamięci podręcznej.
- Wysyłany jest tylko NIP. Treść odpowiedzi (dane realnych firm) nie jest logowana.

### Testy

- Testy jednostkowe na `MockEngine` z **zmyślonymi** odpowiedziami odwzorowującymi prawdziwy format ([docs/przyklady/biala-lista.md](../przyklady/biala-lista.md)): JDG, spółka, oba adresy, podatnik zwolniony, adres bez przecinka, nie znaleziono, błędy `WL-113`/`WL-103`, `429`, `5xx`, brak sieci.
- Prawdziwe NIP-y wolno sprawdzać tylko w trakcie sesji (podejrzenie formatu); **żaden prawdziwy NIP ani dane z rejestru nie trafiają do repozytorium**.

## Konsekwencje

- Wpisanie kontrahenta lub firmy sprowadza się do NIP-u i jednego dotknięcia; rachunki mojej firmy bez przepisywania 26 cyfr.
- Nazwy i adresy z rejestru są wielkimi literami — użytkownik może je poprawić; u JDG nazwę firmy trzeba dopisać ręcznie.
- Zależność od dostępności API MF tylko przy dodawaniu/edycji danych — wystawianie faktur jej nie wymaga.
- Zadanie: [ZAD-021](../zadania/ZAD-021-klient-bialej-listy.md). Historyjka: [US-010](../user-stories/US-010-dane-firmy-w-aplikacji.md).
