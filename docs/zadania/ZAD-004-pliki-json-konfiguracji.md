# ZAD-004. Pliki JSON: firma, rachunki, kontrahenci — model i walidacja

- User story: US-001, US-006
- Status: Zrobione (2026-10-12)
- Szacunek: S

## Cel

Czysty Kotlin: wczytanie i walidacja `company.json`, `banks.json`, `counterparties.json` wg ADR 0004.

## Zakres

- [x] Modele (`Company`, `BankAccount`, `Counterparty`, `Address`) + `ConfigParser` (kotlinx.serialization `JsonElement`), wymagane/opcjonalne pola, limity długości z FA(3); `InvoicingConfig.parse(...)` łączy trzy pliki i wylicza `canIssueInvoices`, `invoiceBankAccounts` (tylko PLN)
- [x] Walidacja NIP (suma kontrolna) i NRB (26 cyfr, mod 97 z prefiksem PL) — `NipValidator`, `BankAccountNumberValidator`; normalizacja: spacje, myślniki, prefiks `PL`; NIP/konto zapisane jako liczba JSON też akceptowane
- [x] Raport błędów `ConfigIssue`: plik, typ (`ConfigIssueType`), indeks pozycji, nazwa wpisu, pole. **Polskie komunikaty powstaną w aplikacji (ZAD-005) z `strings.xml`** — moduł `invoice` nie zawiera tekstów UI. Parser zbiera wszystkie problemy naraz i nigdy nie rzuca wyjątku
- [x] Przykładowe pliki w [`docs/przyklady/`](../przyklady/README.md) (dane zmyślone, testowe NIP-y i rachunki) — sprawdzane testem `example files in docs are valid`

## Definicja ukończenia

- [x] Testy (47: walidatory na testowych NIP-ach i rachunkach, parser, `InvoicingConfig`)
- [x] Nie dotyczy (czysty Kotlin)
- [x] Zaktualizowana dokumentacja / słownik

## Reguły wynikowe

- Wpis listy (`banks`, `counterparties`) z jakimkolwiek błędem jest pomijany; pozostałe są dostępne.
- Duplikat NIP kontrahenta — drugi wpis pomijany (`DUPLICATE_NIP`).
- `company.json` z błędem → brak firmy (`company = null`).
- Wystawienie faktury możliwe tylko, gdy: firma poprawna, `company.json` i `banks.json` bez błędów, co najmniej jedno konto PLN i jeden kontrahent. Błędy w `counterparties.json` nie blokują wystawiania na rzecz poprawnych kontrahentów.
