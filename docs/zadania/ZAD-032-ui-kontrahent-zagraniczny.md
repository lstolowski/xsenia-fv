# ZAD-032. UI: formularz kontrahenta z krajem, domyślna waluta, wartości domyślne faktury

- User story: US-012
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

Kontrahenta zagranicznego dodaję w tym samym formularzu co polskiego, zaczynając od kraju; po wybraniu go w fakturze waluta i stawka „np” ustawiają się same ([ADR 0009](../adr/0009-nabywca-zagraniczny.md)).

## Zakres

- [x] Formularz kontrahenta: pierwsze pole „Kraj” (`CountryField` — okno z wyszukiwaniem po nazwie lub kodzie, Polska na początku listy); Polska — NIP z Białą listą jak dziś; UE — „Numer VAT UE” z prefiksem kraju obok pola (Grecja `EL`); inny kraj — „Identyfikator podatkowy (opcjonalnie)”; podpowiedzi „dane wpisz ręcznie”; kraj i identyfikator tylko do odczytu po zapisie (z wyjaśnieniem)
- [x] Pole „Domyślna waluta” (Brak, PLN, EUR, USD, GBP) z podpowiedzią „Ustawiana w fakturze po wybraniu tego kontrahenta.”
- [x] Kafelki kontrahenta (lista, wybór w fakturze): „Niemcy · DE999999990”, sam kraj bez identyfikatora; polscy — „NIP …” (w ZAD-030)
- [x] Formularz faktury: `InvoiceFormDefaults.selectCounterparty` — domyślna waluta kontrahenta przez `changeCurrency` (z rachunkiem w tej walucie); nabywca zagraniczny → pozycje bez nazwy i ceny dostają „np”, wypełnione bez zmian; `newLine` — „np” dla nabywcy zagranicznego; to samo przy jedynym kontrahencie w nowym formularzu i po „+ Nowy kontrahent”
- [x] Teksty po polsku w `strings.xml`

## Definicja ukończenia

- [x] Testy: wartości domyślne po wyborze kontrahenta (waluta i rachunek, „np” tylko dla pustych pozycji, polski kontrahent bez zmian), nowa pozycja, jedyny kontrahent zagraniczny; wyszukiwanie krajów (`CountryList`)
- [x] Sprawdzone na emulatorze z KSeF TEST (2026-10-14): dodany zmyślony Lorem Testowy GmbH (Niemcy, wyszukiwanie „niem”, prefiks DE, domyślnie EUR) — kafelek „Niemcy · DE999999990”, przy edycji kraj i numer zablokowane; w fakturze po wyborze: EUR, „np” w pustej i w nowej pozycji, rachunek EUR, kurs roboczy NBP. Kontrahent spoza UE — sprawdzony testami (formularz ten sam, inne pole)
- [x] Zaktualizowana dokumentacja / słownik
