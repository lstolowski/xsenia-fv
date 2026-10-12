# ZAD-020. Formularz nowej faktury: listy stawki VAT i jednostki, nowa kolejność sekcji

- User story: US-009
- Status: Zrobione
- Szacunek: S

## Cel

Formularz nowej faktury ma listy rozwijane „Jednostka” i „Stawka VAT” w każdej pozycji oraz układ sekcji z [ADR 0005](../adr/0005-stawki-vat-i-jednostki.md).

## Zakres

- [x] „Jednostka” — lista: szt., dni, godz., usł. (domyślnie godz.); jednostka spoza listy z zapisanego szkicu pokazywana jako dodatkowa opcja
- [x] „Stawka VAT” — lista: 23%, 8%, 5%, 0%, np (domyślnie 23%)
- [x] Wartość brutto pozycji i sumy wg stawek
- [x] „Numer faktury” nad „Datą wystawienia”
- [x] „Rachunek bankowy” pod przyciskiem „Dodaj pozycję”, przed informacją o płatności

## Definicja ukończenia

- [x] Testy (`InvoiceFormCheck`: stawka i jednostka trafiają do szkicu; domyślne wartości)
- [x] Sprawdzone na emulatorze: listy, kolejność sekcji, sumy przy mieszanych stawkach
- [x] Zaktualizowana dokumentacja / słownik
