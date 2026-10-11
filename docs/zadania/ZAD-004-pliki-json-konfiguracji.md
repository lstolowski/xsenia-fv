# ZAD-004. Pliki JSON: firma, rachunki, kontrahenci — model i walidacja

- User story: US-001, US-006
- Status: Do zrobienia
- Szacunek: S

## Cel

Czysty Kotlin: wczytanie i walidacja `company.json`, `banks.json`, `counterparties.json` wg ADR 0004.

## Zakres

- [ ] Modele + parser (kotlinx.serialization), wymagane/opcjonalne pola
- [ ] Walidacja NIP (suma kontrolna) i NRB (26 cyfr, mod 97 z prefiksem PL)
- [ ] Raport błędów: plik, indeks pozycji, pole, komunikat po polsku
- [ ] Przykładowe pliki w `docs/przyklady/` (dane fikcyjne / NIP testowy)

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
