# ZAD-015. Budowa i podpisanie APK produkcyjnego

- User story: wszystkie
- Status: Do zrobienia
- Szacunek: S

## Cel

Podpisany APK `prod` gotowy do ręcznej instalacji na S23.

## Zakres

- [ ] Klucz podpisu (`.jks`) i `keystore.properties` poza gitem; instrukcja wygenerowania i backupu klucza
- [ ] `assembleKsefProdRelease`, R8/minifikacja z regułami dla serializacji
- [ ] Instrukcja instalacji na telefonie (Auto Blocker, nieznane źródła) w `docs/srodowisko/`

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
