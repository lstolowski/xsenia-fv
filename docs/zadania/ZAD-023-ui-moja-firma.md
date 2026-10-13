# ZAD-023. UI: sekcja „Moja firma” — podgląd, edycja, pobieranie, rachunki

- User story: US-010
- Status: Do zrobienia
- Szacunek: M

## Cel

W Ustawieniach sekcja „Moja firma”: podgląd danych i rachunków, formularz edycji z pobieraniem danych z Białej listy i zarządzanie rachunkami ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md), [ADR 0007](../adr/0007-biala-lista-mf.md)).

## Zakres

- [ ] Ustawienia → karta „Moja firma”: nazwa, NIP, adres, e-mail, telefon oraz lista rachunków (kafelki jak w oknie wyboru rachunku); na dole przycisk „Edytuj”. Brak danych — zachęta do uzupełnienia
- [ ] Formularz „Moja firma” (osobny ekran): NIP z tokena tylko do odczytu, obok ikona pobierania; pola: nazwa, adres linia 1, adres linia 2, kraj (domyślnie „Polska”), e-mail, telefon; walidacja per pole; „Zapisz”; pytanie o porzucenie niezapisanych zmian
- [ ] Pobieranie (`CompanyLookupService`): wskaźnik postępu na ikonie, nadpisanie nazwy i adresu, ostrzeżenie ⚠ „Firma nie jest czynnym podatnikiem”, komunikaty „Nie znaleziono…” i błędów
- [ ] Po pobraniu: okno z rachunkami z rejestru do zaznaczenia (już zapisane — oznaczone, nie do zaznaczenia); zaznaczone dodawane jako „Rachunek …{4 ostatnie cyfry}”, PLN
- [ ] Rachunki: dodaj / edytuj / usuń (formularz: nazwa, bank, numer, waluta); usuwanie z potwierdzeniem i ostrzeżeniem, gdy rachunek jest w szkicu lub fakturze niewysłanej
- [ ] Wygląd zgodny z US-008 (kafelki, `SectionCard`, `PrimaryButton`)

## Definicja ukończenia

- [ ] Testy logiki formularza (stan, nadpisywanie pól z wyniku pobrania, dodawanie rachunków z rejestru bez duplikatów)
- [ ] Sprawdzone na emulatorze: pobranie na prawdziwym NIP-ie podanym przez użytkownika w sesji (bez zapisywania go w repozytorium), następnie **nadpisanie danych zmyślonymi** ([docs/przyklady/firma.md](../przyklady/firma.md), [rachunki.md](../przyklady/rachunki.md)) przed jakąkolwiek wysyłką do KSeF TEST; ostrzeżenie o statusie VAT na mocku / zmyślonej odpowiedzi
- [ ] Zaktualizowana dokumentacja / słownik
