# ZAD-028. UI: lista „Waluta”, rachunek z ostrzeżeniem, kurs roboczy

- User story: US-011
- Status: Do zrobienia
- Szacunek: M

## Cel

W formularzu „Nowa faktura” wybieram walutę; dla waluty obcej widzę kurs roboczy NBP i VAT w PLN, a rachunek w innej walucie daje ostrzeżenie. Szkic i faktura niewysłana pamiętają walutę i kurs.

## Zakres

- [ ] `InvoiceForm`: `currency` (domyślnie PLN — stare szkice i faktury niewysłane), `exchangeRate` (kurs roboczy: kurs, tabela, data)
- [ ] Lista rozwijana „Waluta” (PLN, EUR, USD, GBP) — miejsce w formularzu do ustalenia przy realizacji (np. obok rachunku); kwoty w walucie faktury (`Formats.money`)
- [ ] Rachunek: lista wszystkich rachunków z walutą przy nazwie; niezgodność — ostrzeżenie „Rachunek jest w PLN, a faktura w EUR”; po zmianie waluty, gdy wybrany rachunek nie pasuje, a jest pasujący — wybór pierwszego pasującego
- [ ] Kurs roboczy: `ExchangeRateService` po zmianie waluty / daty dostawy / daty wystawienia; stany: pobieranie, „Kurs NBP EUR 4,3775 z 2026-10-09 (tabela 197/A/NBP/2026)” + VAT w PLN, „Kurs zostanie pobrany przy wysyłce” (offline)
- [ ] `InvoiceListLogic`: faktura niewysłana w walucie z formularza
- [ ] Formularz rachunku („Moja firma”, ZAD-023): lista walut PLN, EUR, USD, GBP zamiast pola tekstowego
- [ ] Braki w formularzu (ZAD-025): „brak rachunku” zamiast „brak rachunku PLN”
- [ ] Teksty po polsku w `strings.xml`

## Definicja ukończenia

- [ ] Testy: `InvoiceFormCheck` (rachunek w innej walucie — ostrzeżenie, brak kursu), wybór rachunku po zmianie waluty, odczyt starego szkicu bez waluty, `InvoiceListLogic` dla niewysłanej w EUR
- [ ] Sprawdzone na emulatorze z KSeF TEST (rachunki EUR/USD/GBP zmyślone)
- [ ] Zaktualizowana dokumentacja / słownik
