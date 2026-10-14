# ZAD-032. UI: formularz kontrahenta z krajem, domyślna waluta, wartości domyślne faktury

- User story: US-012
- Status: Do zrobienia
- Szacunek: M

## Cel

Kontrahenta zagranicznego dodaję w tym samym formularzu co polskiego, zaczynając od kraju; po wybraniu go w fakturze waluta i stawka „np” ustawiają się same ([ADR 0009](../adr/0009-nabywca-zagraniczny.md)).

## Zakres

- [ ] Formularz kontrahenta: pierwsze pole „Kraj” (lista z wyszukiwaniem, domyślnie Polska); Polska — NIP z Białą listą jak dziś; UE — „Numer VAT UE” z prefiksem kraju; inny kraj — „Identyfikator podatkowy” (opcjonalny); kraj i identyfikator tylko do odczytu po zapisie
- [ ] Pole „Domyślna waluta” (brak, PLN, EUR, USD, GBP)
- [ ] Kafelki kontrahenta (lista, wybór w fakturze): kraj i numer VAT UE / identyfikator
- [ ] Formularz faktury: wybór kontrahenta z domyślną walutą → `InvoiceFormDefaults.changeCurrency`; nabywca zagraniczny → nowe pozycje i puste pozycje ze stawką „np”
- [ ] Teksty po polsku w `strings.xml`

## Definicja ukończenia

- [ ] Testy: wartości domyślne po wyborze kontrahenta (waluta, „np” tylko dla pustych pozycji), logika pól formularza wg kraju
- [ ] Sprawdzone na emulatorze z KSeF TEST (kontrahenci zmyśleni z UE i spoza UE)
- [ ] Zaktualizowana dokumentacja / słownik
