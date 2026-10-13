# ZAD-024. UI: „Kontrahenci” — lista, formularz, pobieranie; „+ Nowy kontrahent” w fakturze

- User story: US-010
- Status: Do zrobienia
- Szacunek: M

## Cel

Zarządzanie kontrahentami w aplikacji: lista w Ustawieniach, formularz z pobieraniem danych z Białej listy, szybkie dodanie kontrahenta z formularza „Nowa faktura” ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md), [ADR 0007](../adr/0007-biala-lista-mf.md)).

## Zakres

- [ ] Ustawienia → pozycja „Kontrahenci” (odnośnik z liczbą kontrahentów) → ekran listy: te same kafelki co w oknie wyboru kontrahenta na formularzu faktury (wspólny komponent), przycisk „Dodaj kontrahenta”
- [ ] Formularz kontrahenta: pierwszy element NIP z ikoną pobierania; pola: nazwa, adres linia 1, adres linia 2, kraj, e-mail; walidacja per pole (suma kontrolna NIP, duplikat NIP-u); „Zapisz”
- [ ] Pobieranie jak w ZAD-023 (nadpisanie nazwy i adresu, ostrzeżenie o statusie VAT, komunikaty); bez rachunków
- [ ] Edycja: NIP tylko do odczytu, ikona pobierania zostaje (odświeżenie nazwy i adresu z rejestru); usuwanie z potwierdzeniem i ostrzeżeniem, gdy kontrahent jest w szkicu lub fakturze niewysłanej
- [ ] Okno wyboru kontrahenta na formularzu „Nowa faktura”: „+ Nowy kontrahent” → formularz → po zapisie powrót z wybranym nowym kontrahentem (szkic zachowany)

## Definicja ukończenia

- [ ] Testy logiki formularza (walidacja, duplikat, niezmienny NIP, nadpisywanie z pobrania)
- [ ] Sprawdzone na emulatorze: dodanie kontrahentów zmyślonych ([docs/przyklady/kontrahenci.md](../przyklady/kontrahenci.md)) ręcznie; pobieranie na prawdziwym NIP-ie tylko do podglądu (kontrahenta z prawdziwymi danymi usunąć, nie wystawiać na niego faktury); „+ Nowy kontrahent” z formularza faktury
- [ ] Zaktualizowana dokumentacja / słownik
