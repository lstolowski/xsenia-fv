# Zadania

Zadania implementacyjne (**jak**), wynikające z user stories. Plik: `ZAD-NNN-krotki-tytul.md` wg [szablonu](_szablon.md).

| Nr | Tytuł | User story | Status |
|---|---|---|---|
| [ZAD-001](ZAD-001-szkielet-projektu.md) | Szkielet projektu Android (Gradle, moduły, wersje test/prod) | wszystkie | Zrobione |
| [ZAD-002](ZAD-002-testy-integracyjne-ksef-test.md) | Infrastruktura testów integracyjnych z KSeF TEST | US-002, US-006 | Zrobione |
| [ZAD-003](ZAD-003-ksef-uwierzytelnianie.md) | Klient KSeF: uwierzytelnianie tokenem i odświeżanie | US-002 | Zrobione |
| [ZAD-004](ZAD-004-pliki-json-konfiguracji.md) | Pliki JSON: firma, rachunki, kontrahenci — model i walidacja | US-001, US-006 | Zrobione |
| [ZAD-005](ZAD-005-bezpieczenstwo-i-ustawienia.md) | Aplikacja: token w Keystore, biometria, wybór folderu, ustawienia | US-001 | Zrobione |
| [ZAD-006](ZAD-006-ksef-pobieranie-faktur.md) | Klient KSeF: metadane, pobieranie XML, eksport paczki | US-002 | Zrobione |
| [ZAD-007](ZAD-007-parser-xml-faktury.md) | Parser XML FA(2)/FA(3) → model faktury, data dostawy | US-002, US-005 | Zrobione |
| [ZAD-008](ZAD-008-lustro-i-synchronizacja.md) | Lokalne lustro faktur i synchronizacja | US-002 | Zrobione |
| [ZAD-009](ZAD-009-ui-listy-faktur.md) | UI: listy sprzedażowa i zakupowa z filtrem i podsumowaniem | US-003, US-004 | Zrobione |
| [ZAD-010](ZAD-010-ui-szczegoly-faktury.md) | UI: szczegóły faktury (bez udostępniania) | US-005 | Zrobione |
| [ZAD-011](ZAD-011-generator-xml-fa3.md) | Generator XML FA(3), wyliczenia, sugestia numeru i daty dostawy | US-006 | Zrobione |
| [ZAD-012](ZAD-012-ksef-sesja-interaktywna.md) | Klient KSeF: sesja interaktywna, status, UPO | US-006 | Zrobione |
| [ZAD-013](ZAD-013-ui-nowa-faktura.md) | UI: formularz nowej faktury, podgląd, biometria, śledzenie wysyłki | US-006 | Zrobione |
| [ZAD-014](ZAD-014-dane-testowe-zakupowe.md) | Dane testowe: faktury zakupowe na KSeF TEST | US-004 | Zrobione |
| [ZAD-015](ZAD-015-budowa-apk-prod.md) | Budowa i podpisanie APK produkcyjnego | wszystkie | Zrobione |
| [ZAD-016](ZAD-016-faktury-niewyslane-model.md) | Faktury niewysłane: model, magazyn, numeracja | US-007 | Zrobione |
| [ZAD-017](ZAD-017-faktury-niewyslane-ui.md) | Faktury niewysłane: UI listy, edycji, zapisu i usuwania | US-007 | Zrobione |
| [ZAD-018](ZAD-018-wyglad-payment-ui.md) | Wygląd w stylu „Payment UI” | US-008 | Zrobione |
| [ZAD-019](ZAD-019-stawki-vat-model.md) | Stawki VAT w modelu faktury: kalkulator, XML FA(3), reverse charge | US-009 | Zrobione |
| [ZAD-020](ZAD-020-formularz-stawki-jednostki-uklad.md) | Formularz: listy stawki VAT i jednostki, nowa kolejność sekcji | US-009 | Zrobione |
| [ZAD-021](ZAD-021-klient-bialej-listy.md) | Klient Białej listy MF: pobieranie danych firmy po NIP-ie | US-010 | Zrobione |
| [ZAD-022](ZAD-022-profil-firmy-magazyn.md) | Profil firmy: model, magazyn per NIP, migracja, usunięcie plików JSON | US-010 | Zrobione |
| [ZAD-023](ZAD-023-ui-moja-firma.md) | UI: sekcja „Moja firma” — podgląd, edycja, pobieranie, rachunki | US-010 | Zrobione |
| [ZAD-024](ZAD-024-ui-kontrahenci.md) | UI: „Kontrahenci” — lista, formularz, pobieranie; „+ Nowy kontrahent” w fakturze | US-010 | Zrobione |
| [ZAD-025](ZAD-025-pierwsze-uruchomienie-braki.md) | Pierwsze uruchomienie (token → Moja firma) i braki w formularzu faktury | US-010 | Zrobione |
| [ZAD-026](ZAD-026-serwis-kursow-walut.md) | Serwis kursów walut: klient NBP, pamięć podręczna, reguła daty kursu | US-011 | Zrobione |
| [ZAD-027](ZAD-027-faktura-w-walucie-model-xml.md) | Faktura w walucie obcej: model, VAT w PLN, XML FA(3) | US-011 | Zrobione |
| [ZAD-028](ZAD-028-ui-waluta-formularz.md) | UI: lista „Waluta”, rachunek z ostrzeżeniem, kurs roboczy | US-011 | Zrobione |
| [ZAD-029](ZAD-029-wysylka-kurs-ostateczny.md) | Podgląd i wysyłka faktury w walucie: kurs ostateczny w locie, szczegóły | US-011 | Zrobione |
| [ZAD-030](ZAD-030-kontrahent-zagraniczny-model.md) | Kontrahent zagraniczny: model, walidacja, migracja | US-012 | Zrobione |
| [ZAD-031](ZAD-031-fa3-nabywca-zagraniczny.md) | FA(3): nabywca z numerem VAT UE lub innym identyfikatorem, „np I” / „np II” | US-012 | Zrobione |
| [ZAD-032](ZAD-032-ui-kontrahent-zagraniczny.md) | UI: formularz kontrahenta z krajem, domyślna waluta, wartości domyślne faktury | US-012 | Zrobione |
| [ZAD-033](ZAD-033-listy-identyfikator-nabywcy.md) | Listy, szczegóły i wyszukiwanie: numer VAT UE / identyfikator nabywcy | US-012 | Zrobione |
| [ZAD-034](ZAD-034-pdf-uklad.md) | PDF faktury: układ, strony, etykiety PL / PL+EN | US-013 | Zrobione |
| [ZAD-035](ZAD-035-pdf-rysowanie-udostepnianie.md) | PDF faktury: rysowanie, udostępnianie, ikony w aplikacji | US-013 | Do zrobienia |

Weryfikacja na urządzeniu fizycznym (2026-10-13): użytkownik sprawdził zadania 001–020 (poza 002 — infrastruktura testów integracyjnych) na swoim telefonie z KSeF TEST — aplikacja działa i wygląda poprawnie.

Kolejność realizacji (F1): 001 → 002 → 003 → 004 → 005 → 006 → 007 → 008 → 009 → 010 → 011 → 012 → 013; 014 równolegle z 009; 016 → 017 (US-007, przed 015); 019 → 020 (US-009, przed 015); 015 na końcu.

US-010: 021 → 022 → 023 → 024 → 025 (021 i 022 mogą iść równolegle).

US-011 (po US-010): 026 → 027 → 028 → 029 (026 może iść równolegle z US-010; 027–029 wymagają ZAD-022, ZAD-023 i ZAD-025).

US-012: 030 → 031 → 032 → 033.

US-013: 034 → 035.
