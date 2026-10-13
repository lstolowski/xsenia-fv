# ZAD-022. Profil firmy: model, magazyn per NIP, migracja, usunięcie plików JSON

- User story: US-010
- Status: Zrobione (2026-10-13)
- Szacunek: L

## Cel

Dane mojej firmy, rachunki i kontrahenci zapisywane w aplikacji, w katalogu profilu `profiles/{nip}/` (NIP z tokena KSeF), zamiast plików JSON we wspólnym folderze ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)). Po tym zadaniu reszta aplikacji (synchronizacja, formularz faktury) korzysta z profilu.

## Zakres

- [x] Moduł `invoice`, pakiet `profile`: modele (`Company`, `BankAccount`, `Counterparty`, `Address` — przeniesione z `config`, `@Serializable` z dotychczasowymi nazwami pól), `CompanyProfileStore` (`company.json`, `bank-accounts.json`, `counterparties.json` z polem `version`; zapis atomowy; uszkodzony plik odkładany jako `*.damaged-…` zamiast nadpisania); operacje: zapis firmy (tylko NIP profilu), dodaj/edytuj (z zachowaniem pozycji)/usuń rachunek, dodaj/edytuj/usuń kontrahenta (po NIP-ie)
- [x] `ProfileValidator`: dotychczasowe reguły (`NipValidator`, `BankAccountNumberValidator`, limity FA(3), e-mail, pola wymagane, kod waluty) + unikalny numer rachunku i NIP kontrahenta; wynik `Validation.Valid` / `Invalid(List<FieldIssue>)` — wszystkie błędy naraz, per pole (`ProfileField`, `FieldIssueType`)
- [x] `CompanyProfile` zamiast `InvoicingConfig`: firma, rachunki, kontrahenci, `invoiceBankAccounts` (PLN), `gaps` (`ProfileGap`: firma / rachunek PLN / kontrahent), `canIssueInvoices`
- [x] Aplikacja: `ProfileManager` (aktywny profil = NIP z tokena, `ProfileState`: `Loading` / `NoProfile` / `Active`) tworzy `ProfileSession` — `ProfileRepository` (`StateFlow` danych), `IssueStorage`, faktury niewysłane, lustro, `InvoiceSyncService` (NIP sesji zamiast `company.json`), `InvoiceSendService`. Ekran główny i jego ViewModele dostają sesję; `key(nip)` — inny NIP to nowe ekrany
- [x] Token bez NIP-u odrzucany: w oknie wpisywania („Token nie zawiera NIP-u firmy…”, przycisk „Zapisz” nieaktywny) i w `KsefTokenStore.save`; `ConnectionCheckResult.MissingNip`; `KsefFailure.MissingCompany` usunięte
- [x] „Wyczyść cache” czyści lustro aktywnego profilu i pliki tymczasowe
- [x] Migracja (`LegacyDataMigration`) przy pierwszym utworzeniu sesji: `issue/`, `unsent/`, `upo/`, `invoices/` → `profiles/{nip}/` (tylko gdy w profilu ich nie ma); zwolnienie trwałego uprawnienia do folderu i usunięcie ustawienia `config_folder_uri` (`AppSettings.forgetLegacyConfigFolder`)
- [x] Usunięte: `ConfigRepository`, `FolderSection`, `ConfigParser`, `ConfigIssue`, `JsonObjectReader`, `InvoicingConfig`, `ConfigIssueFormatter`, wybór folderu w konfiguracji i Ustawieniach, znaczek błędów na zakładce Ustawienia, ostrzeżenie o niezgodności NIP-u tokena i firmy, teksty o plikach JSON, zależność `documentfile`, pliki `invoice/src/test/resources/config/*.json` i ich testy
- [x] Komunikat „Wystawianie faktur jest niedostępne” bez odwołań do plików (szczegółowe braki i „Uzupełnij” — ZAD-025)

## Definicja ukończenia

- [x] Testy: `ProfileValidatorTest` (8), `CompanyProfileStoreTest` (7), `CompanyProfileTest` (2), `LegacyDataMigrationTest` (3), `ProfileRepositoryTest` (1); dotychczasowe testy przepięte na `CompanyProfile`; `./gradlew build` (lint, R8) zielony
- [x] Sprawdzone na emulatorze z KSeF TEST (2026-10-13): wersja sprzed zmian (debug) ze szkicem, fakturą niewysłaną, 7 UPO i lustrem (11 sprzedażowych, 5 zakupowych) zaktualizowana do nowej — wszystko przeniesione do `profiles/{nip}/`, w katalogu głównym nic nie zostało; ustawienie folderu usunięte. Dane firmy, 2 rachunki i 2 kontrahenci (zmyśleni, `docs/przyklady/`) wgrane do profilu przez `run-as` (UI w ZAD-023/024). Synchronizacja z KSeF TEST, listy (faktura niewysłana z nazwą kontrahenta z profilu), formularz ze szkicem i kafelkami kontrahenta/rachunku, sugestia numeru z pominięciem numeru faktury niewysłanej, podgląd ze sprzedawcą z profilu (NIP z tokena), „Sprawdź połączenie”, Ustawienia bez folderu — działają. Bez wysyłki faktury
- [x] Zaktualizowana dokumentacja / słownik (ADR 0006 — moduły, AGENTS.md — dane testowe na emulatorze, `docs/srodowisko/instalacja-apk-telefon.md`, `docs/przyklady/README.md`)

## Uwagi

- Do czasu ZAD-023 na czystej instalacji nie da się wpisać danych firmy w aplikacji (wystawianie niedostępne); w wersji debug można je wgrać przez `run-as` (AGENTS.md).
- Sesja profilu żyje do końca procesu; po zmianie tokena na inny NIP stara sesja zostaje w pamięci (nic nie jest usuwane z dysku).
