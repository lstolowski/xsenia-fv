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
| [ZAD-015](ZAD-015-budowa-apk-prod.md) | Budowa i podpisanie APK produkcyjnego | wszystkie | Do zrobienia |
| [ZAD-016](ZAD-016-faktury-niewyslane-model.md) | Faktury niewysłane: model, magazyn, numeracja | US-007 | Do zrobienia |
| [ZAD-017](ZAD-017-faktury-niewyslane-ui.md) | Faktury niewysłane: UI listy, edycji, zapisu i usuwania | US-007 | Do zrobienia |

Kolejność realizacji (F1): 001 → 002 → 003 → 004 → 005 → 006 → 007 → 008 → 009 → 010 → 011 → 012 → 013; 014 równolegle z 009; 016 → 017 (US-007, przed 015); 015 na końcu.
