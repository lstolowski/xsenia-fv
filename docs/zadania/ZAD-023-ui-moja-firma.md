# ZAD-023. UI: sekcja „Moja firma” — podgląd, edycja, pobieranie, rachunki

- User story: US-010
- Status: Zrobione (2026-10-13)
- Szacunek: M

## Cel

W Ustawieniach sekcja „Moja firma”: podgląd danych i rachunków, formularz edycji z pobieraniem danych z Białej listy i zarządzanie rachunkami ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md), [ADR 0007](../adr/0007-biala-lista-mf.md)).

## Zakres

- [x] Ustawienia → karta „Moja firma” (na górze): nazwa, NIP, adres, kraj, e-mail, telefon oraz rachunki (kafelki jak w oknie wyboru rachunku); na dole „Edytuj”. Brak danych — NIP z tokena i „Nie uzupełniono danych firmy — bez nich nie wystawisz faktury.” (`CompanySection`)
- [x] Formularz „Moja firma” (`CompanyFormScreen`, `CompanyFormViewModel`): NIP z tokena tylko do odczytu („NIP z tokena KSeF”) z ikoną pobierania (`ic_download`, w trakcie — wskaźnik postępu); pola: nazwa, adres linia 1 i 2, kraj (domyślnie „Polska”), e-mail, telefon; błędy per pole po pierwszym „Zapisz” (`ProfileValidator`); pytanie „Porzucić zmiany?” przy wyjściu z niezapisanymi zmianami
- [x] Pobieranie (`CompanyLookupService`): nadpisanie nazwy i adresu (e-mail, telefon bez zmian; bez adresu w rejestrze — adres wpisany zostaje), „Pobrano nazwę i adres z Białej listy — sprawdź i zapisz.”, ⚠ „Firma nie jest czynnym podatnikiem”, komunikaty „Nie znaleziono…” i błędów — wspólny komponent `NipField` (do ZAD-024/025)
- [x] Po pobraniu: przewijane okno „Rachunki w Białej liście” z polami wyboru (domyślnie nic; zapisane — „już zapisany”, nieaktywne); „Dodaj zaznaczone” dodaje je jako „Rachunek …{4 ostatnie cyfry}”, PLN; „Pomiń” zamyka okno (`RegistryAccounts`)
- [x] Rachunki pod formularzem: kafelki (dotknięcie → edycja), „Dodaj rachunek”; ekran rachunku (`BankAccountFormScreen`): nazwa, numer (26 cyfr), bank, waluta (informacja: na fakturze tylko PLN, pierwszy rachunek domyślny), zapis z walidacją (suma kontrolna, duplikat), usuwanie z potwierdzeniem; ostrzeżenie, gdy rachunek jest w szkicu lub fakturze niewysłanej (`ProfileUsage`)
- [x] Wygląd zgodny z US-008; kafelki rachunku i kontrahenta przeniesione do wspólnych komponentów (`ProfileTiles.kt`)

## Definicja ukończenia

- [x] Testy logiki formularza: `ProfileFormsTest` (5 — nadpisywanie pól wynikiem pobrania, pola zapisanej firmy, rachunki z rejestru bez duplikatów, użycie w szkicu i fakturach niewysłanych); `./gradlew build` zielony
- [x] Sprawdzone na emulatorze (2026-10-13, KSeF TEST, profil ze zmyślonymi danymi): podgląd w Ustawieniach; pobranie z Białej listy na NIP-ie z tokena — nazwa i adres nadpisane, e-mail i telefon bez zmian, okno rachunków z rejestru (nic niezaznaczone, „Dodaj zaznaczone” nieaktywne) → „Pomiń”, wyjście → „Porzucić zmiany?” → „Porzuć” (pobrane dane **nie zostały zapisane**, w profilu zostały dane zmyślone); dodanie rachunku z błędnym numerem (komunikat) i poprawnym (zapis w `bank-accounts.json`), usunięcie z potwierdzeniem, ostrzeżenie przy rachunku ze szkicu; zmiana nazwy firmy i zapis → podgląd zaktualizowany. Ostrzeżenie o statusie VAT — w testach na zmyślonych odpowiedziach (prawdziwy NIP jest czynnym podatnikiem)
- [x] Wersja release (R8): `releaseApkTest` podpisany kluczem debug, zainstalowany jako aktualizacja — pobieranie z Białej listy i okno rachunków działają, zmiany porzucone, bez błędów w logach
- [x] Zaktualizowana dokumentacja / słownik

## Uwagi

- Dane firmy zapisuje „Zapisz”; rachunki zapisują się od razu (każdy własnym „Zapisz” lub „Dodaj zaznaczone”).
- Na emulatorze pływający pasek klawiatury Gboard zasłania lewą część pól — przy `adb input tap` celować w prawą część pola; „Zaznacz wszystko” (Ctrl+A) nie działa w polach Compose — czyścić `KEYCODE_DEL`.
