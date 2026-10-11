# ZAD-007. Parser XML FA(2)/FA(3) → model faktury, data dostawy

- User story: US-002, US-005
- Status: Do zrobienia
- Szacunek: M

## Cel

Moduł `invoice` zamienia XML faktury na model do widoku szczegółów.

## Zakres

- [ ] Model: strony, daty, pozycje, sumy wg stawek, płatność, rodzaj faktury, waluta
- [ ] Data dostawy: `P_6` → `OkresFa/P_6_Do` → najpóźniejsza `P_6A` → `P_1`
- [ ] Tolerancja na brakujące i nieznane elementy; obsługa FA(2) i FA(3)
- [ ] Testy na przykładowych XML (w tym wygenerowanych przez ZAD-011)

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
