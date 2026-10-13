# ZAD-022. Profil firmy: model, magazyn per NIP, migracja, usunięcie plików JSON

- User story: US-010
- Status: Do zrobienia
- Szacunek: L

## Cel

Dane mojej firmy, rachunki i kontrahenci zapisywane w aplikacji, w katalogu profilu `profiles/{nip}/` (NIP z tokena KSeF), zamiast plików JSON we wspólnym folderze ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)). Po tym zadaniu reszta aplikacji (synchronizacja, formularz faktury) korzysta z profilu.

## Zakres

- [ ] Moduł `invoice`, pakiet `profile`: magazyn profilu (`company.json`, `bank-accounts.json`, `counterparties.json`; kotlinx.serialization, zapis atomowy), operacje: zapis firmy, dodaj/edytuj/usuń rachunek, dodaj/edytuj/usuń kontrahenta
- [ ] Walidacja przed zapisem z dotychczasowych reguł (`NipValidator`, `BankAccountNumberValidator`, limity FA(3), e-mail, wymagane pola); wynik walidacji per pole (do pokazania w formularzu); unikalny numer rachunku i NIP kontrahenta; NIP kontrahenta niezmienny
- [ ] Odpowiednik `InvoicingConfig` dla formularza faktury: firma, rachunki PLN, kontrahenci, `canIssueInvoices` i lista braków (firma / rachunek PLN / kontrahent)
- [ ] `ProfileRepository` w aplikacji (aktywny profil = NIP z tokena, `StateFlow`) zastępuje `ConfigRepository`; token bez NIP-u odrzucany przy zapisie
- [ ] `IssueStorage`, `UnsentInvoiceStore`, UPO i `InvoiceMirror` w katalogu profilu; „Wyczyść cache” czyści lustro aktywnego profilu; synchronizacja i sprawdzanie połączenia biorą NIP z tokena
- [ ] Migracja przy pierwszym starcie po aktualizacji: `issue/`, `unsent/`, `upo/`, `invoices/` → `profiles/{nip}/`; zwolnienie trwałego uprawnienia do folderu i usunięcie ustawienia `config_folder_uri`
- [ ] Usunięcie: `ConfigRepository`, `FolderSection`, wybór folderu w konfiguracji i Ustawieniach, `ConfigParser`/`ConfigIssue`/`ConfigIssueFormatter` (o ile nieużywane), znaczek błędów konfiguracji na zakładce Ustawienia, zależność `documentfile`, zasoby testowe `invoice/src/test/resources/config/*.json` i ich test
- [ ] Ostrzeżenie o niezgodności NIP-u tokena z `company.json` — usunięte (NIP firmy = NIP tokena)

## Definicja ukończenia

- [ ] Testy: magazyn (zapis, odczyt, edycja, usuwanie, zapis atomowy), walidacja, migracja katalogów, `canIssueInvoices` i braki
- [ ] Sprawdzone na emulatorze z KSeF TEST: po aktualizacji z poprzedniej wersji szkic, faktury niewysłane i lustro zachowane; synchronizacja działa (dane firmy wpisane tymczasowo — UI w ZAD-023)
- [ ] Zaktualizowana dokumentacja / słownik (`docs/srodowisko/instalacja-apk-telefon.md` — bez folderu z plikami)
