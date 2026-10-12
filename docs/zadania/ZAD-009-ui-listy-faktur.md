# ZAD-009. UI: listy sprzedażowa i zakupowa z filtrem i podsumowaniem

- User story: US-003, US-004
- Status: W toku — do potwierdzenia na emulatorze: odmiana „0 faktur” po przeniesieniu do kodu
- Szacunek: M

## Cel

Dwie zakładki z listą faktur wybranego miesiąca.

## Zakres

- [x] Nawigacja dolna: Sprzedażowe / Zakupowe / Ustawienia (`MainScaffold`); znaczek z liczbą problemów w plikach danych na zakładce Ustawienia; stan list zachowany przy przełączaniu zakładek
- [x] Wybór miesiąca wg daty dostawy: strzałki + okno z listą miesięcy (od 04.2026 do bieżącego oraz miesiące z fakturami) z liczbą faktur; pole „Szukaj (NIP lub nazwa)” — fragment nazwy bez rozróżniania wielkości liter, NIP z pominięciem spacji i myślników
- [x] Wiersz: data dostawy, kontrahent (nazwa + NIP), numer, brutto i netto; rodzaj faktury (Korekta, Zaliczkowa, …) gdy inny niż VAT; najnowsze na górze
- [x] Podsumowanie miesiąca (po filtrze) netto / VAT / brutto, osobno dla każdej waluty; kwoty `1 234,56 zł`; polska odmiana „1 faktura / 2 faktury / 5 faktur” liczona w kodzie (`PolishPlural`) niezależnie od języka urządzenia
- [x] „Odśwież z KSeF”: przycisk w pasku i pociągnięcie listy w dół
- [ ] Przycisk „Nowa faktura” na liście sprzedażowej — w ZAD-013
- [ ] Otwarcie szczegółów po dotknięciu wiersza — w ZAD-010
- [x] Stan pusty (miesiąc / wyszukiwanie / przed pierwszą synchronizacją), błąd synchronizacji, postęp synchronizacji nad listą

## Definicja ukończenia

- [x] Testy (`InvoiceListLogicTest` 5, `PolishPluralTest` 15)
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [x] Zaktualizowana dokumentacja

## Uwagi

- Język aplikacji ustawiany na polski (`LocaleManager`, `localeConfig`) — UI istnieje tylko po polsku.
- Tymczasowy ekran główny z ZAD-005/008 usunięty; ustawienia są zakładką.
