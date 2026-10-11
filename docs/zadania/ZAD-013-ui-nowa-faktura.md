# ZAD-013. UI: formularz nowej faktury, podgląd, biometria, śledzenie wysyłki

- User story: US-006
- Status: Do zrobienia
- Szacunek: L

## Cel

Kompletny przepływ wystawienia faktury w aplikacji.

## Zakres

- [ ] Formularz: kontrahent (lista), rachunek (lista), daty, numer (sugerowany, edytowalny, ostrzeżenie o duplikacie), pozycje 1..n z wyliczeniami
- [ ] Szkic zapisywany na bieżąco (DataStore/plik)
- [ ] Podgląd (komponent z ZAD-010) → „Wyślij do KSeF” z potwierdzeniem biometrią
- [ ] Ekran statusu wysyłki; stan w pliku, wznowienie pollingu po ponownym otwarciu
- [ ] Po sukcesie: faktura w lustrze, szkic usunięty, UPO dostępne

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
