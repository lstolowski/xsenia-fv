# ZAD-024. UI: „Kontrahenci” — lista, formularz, pobieranie; „+ Nowy kontrahent” w fakturze

- User story: US-010
- Status: Zrobione (2026-10-13)
- Szacunek: M

## Cel

Zarządzanie kontrahentami w aplikacji: lista w Ustawieniach, formularz z pobieraniem danych z Białej listy, szybkie dodanie kontrahenta z formularza „Nowa faktura” ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md), [ADR 0007](../adr/0007-biala-lista-mf.md)).

## Zakres

- [x] Ustawienia → pozycja „Kontrahenci” (odnośnik z liczbą, np. „3 kontrahentów” — `counterpartyCount`, polska odmiana) → ekran listy (`CounterpartyListScreen`): te same kafelki co w oknie wyboru kontrahenta na fakturze (`CounterpartyText`), „Dodaj kontrahenta”, pusta lista — podpowiedź
- [x] Formularz kontrahenta (`CounterpartyFormScreen`, `CounterpartyFormViewModel`): pierwszy element NIP z ikoną pobierania (`NipField`); pola: nazwa, adres linia 1 i 2, kraj (domyślnie „Polska”), e-mail; walidacja per pole po pierwszym „Zapisz” (suma kontrolna NIP, duplikat — „Kontrahent z tym NIP-em już istnieje.”); „Porzucić zmiany?” przy wyjściu
- [x] Pobieranie jak w ZAD-023: NIP znormalizowany (bez myślników i `PL`), nazwa i adres nadpisane, e-mail bez zmian, ⚠ status VAT, „Nie znaleziono…” i błędy; bez rachunków (`CounterpartyFormFields.withLookup`)
- [x] Edycja: NIP tylko do odczytu z wyjaśnieniem („…w razie pomyłki usuń kontrahenta i dodaj nowego”), ikona pobierania zostaje (odświeżenie nazwy i adresu); usuwanie z potwierdzeniem i ostrzeżeniem, gdy kontrahent jest w szkicu lub fakturze niewysłanej (`ProfileUsage`)
- [x] Okno wyboru kontrahenta na formularzu „Nowa faktura”: „+ Nowy kontrahent” (`TileChoiceField` z akcją dodania) → formularz → po zapisie powrót z wybranym nowym kontrahentem (wynik przez `savedStateHandle` poprzedniego ekranu); szkic zachowany

## Definicja ukończenia

- [x] Testy logiki formularza (`ProfileFormsTest`: pobranie normalizuje NIP i nadpisuje nazwę i adres; pola zapisanego kontrahenta); walidacja i duplikat — `ProfileValidatorTest` (ZAD-022); `./gradlew build` zielony
- [x] Sprawdzone na emulatorze (2026-10-13, KSeF TEST): odnośnik z liczbą, lista; nowy kontrahent z testowym NIP-em — „Nie znaleziono firmy w Białej liście”, „Zapisz” → duplikat NIP-u i pola wymagane oznaczone; pobranie na prawdziwym NIP-ie z sesji tylko do podglądu (nazwa i adres wypełnione) → „Porzuć” (**nic nie zapisano**); „+ Nowy kontrahent” z faktury → zmyślony kontrahent (`docs/przyklady/kontrahenci.md`) zapisany i od razu wybrany na fakturze; edycja — NIP nie daje się zmienić, ostrzeżenie dla kontrahenta z faktury niewysłanej; usunięcie z ostrzeżeniem (kontrahent w szkicu) → szkic pokazuje „Wybierz kontrahenta”
- [x] Zaktualizowana dokumentacja / słownik

## Uwagi

- Na emulatorze Gboard poprawia wpisywany przez `adb input text` tekst (np. „Dolor” → „Dolot”) — dla danych testowych bez znaczenia.
- Do ustalenia położenia pól przy testach przez `adb`: `adb shell uiautomator dump` i atrybut `bounds` pól `EditText`.
