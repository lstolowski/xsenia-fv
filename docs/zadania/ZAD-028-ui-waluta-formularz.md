# ZAD-028. UI: lista „Waluta”, rachunek z ostrzeżeniem, kurs roboczy

- User story: US-011
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

W formularzu „Nowa faktura” wybieram walutę; dla waluty obcej widzę kurs roboczy NBP i VAT w PLN, a rachunek w innej walucie daje ostrzeżenie. Szkic i faktura niewysłana pamiętają walutę i kurs.

## Zakres

- [x] `InvoiceForm`: `currency` (domyślnie PLN — stare szkice i faktury niewysłane), `exchangeRate` (`SavedExchangeRate`: kurs, tabela, data tabeli i dzień odniesienia, dla którego go wybrano); `workingRate` — zapisany kurs, dopóki pasuje do waluty i dat
- [x] Lista rozwijana „Waluta” (PLN, EUR, USD, GBP) w każdej pozycji obok „Cena jedn. netto” (cena 3/5, waluta 2/5 — decyzja użytkownika 2026-10-14; pierwotnie 2/3 i 1/3, poszerzone, bo kod waluty był przycięty); waluta dotyczy całej faktury, zmiana w jednej pozycji zmienia ją wszędzie; etykieta ceny bez „(zł)”; kwoty pozycji i sumy w walucie faktury (`Formats.money`), w sumach wiersz „VAT w PLN”
- [x] Rachunek: lista wszystkich rachunków z walutą przy nazwie; niezgodność — ostrzeżenie „Rachunek jest w PLN, a faktura w EUR”; po zmianie waluty, gdy wybrany rachunek nie pasuje, a jest pasujący — wybór pierwszego pasującego
- [x] Kurs roboczy (pod pozycjami, nad rachunkiem): `ExchangeRateService.rateFor` po zmianie waluty lub dnia odniesienia; stany: „Pobieranie kursu NBP…”, „Kurs NBP EUR 4,3728 z 29.09.2026 (tabela 189/A/NBP/2026)” z dopiskiem, że przy wysyłce kurs zostanie ustalony ponownie, brak kursu — komunikat, „Kurs zostanie pobrany przy podglądzie i wysyłce.” i „Pobierz kurs ponownie”; „Podgląd” bez kursu najpierw próbuje go pobrać
- [x] `InvoiceListLogic`: faktura niewysłana w walucie z formularza
- [x] Formularz rachunku („Moja firma”, ZAD-023): lista walut PLN, EUR, USD, GBP (była już na main; teraz z `InvoiceCurrency`)
- [x] Braki w formularzu (ZAD-025): „brak rachunku” zamiast „brak rachunku PLN” (w ZAD-027)
- [x] Teksty po polsku w `strings.xml`

## Definicja ukończenia

- [x] Testy: `InvoiceFormCheck` (rachunek w innej walucie — ostrzeżenie, brak kursu), wybór rachunku po zmianie waluty, odczyt starego szkicu bez waluty, `InvoiceListLogic` dla niewysłanej w EUR
- [x] Sprawdzone na emulatorze z KSeF TEST (2026-10-14, zmyślony rachunek EUR): EUR — kurs roboczy z 29.09.2026 dla dostawy 30.09.2026, rachunek EUR wybrany automatycznie, VAT w PLN 230,00 × 4,3728 = 1005,74 zł; GBP — ostrzeżenie „Rachunek jest w EUR, a faktura w GBP.”, VAT w PLN 1173,23 zł; PLN — powrót rachunku PLN, bez kursu; faktura w EUR z testu integracyjnego widoczna na liście z sumą w EUR
- [x] Zaktualizowana dokumentacja / słownik
