# ZAD-010. UI: szczegóły faktury i udostępnianie XML/UPO

- User story: US-005
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Czytelny widok faktury na telefonie.

## Zakres

- [x] Sekcje: nagłówek (rodzaj, numer, numer KSeF, daty / okres, waluta), sprzedawca, nabywca, podmioty trzecie, korekta, pozycje (ilość × cena, netto / stawka / brutto, „Stan przed korektą”), podsumowanie wg kategorii VAT i razem, płatność (forma, termin, zapłacono, rachunki w grupach cyfr), adnotacje, dodatkowe informacje i stopka — sekcje bez danych pomijane
- [x] Gdy XML nie daje się odczytać: komunikat + dane z metadanych KSeF
- [x] Otwieranie z listy (dotknięcie wiersza), powrót strzałką; działa offline (lustro)
- [ ] ~~Udostępnianie XML i UPO~~ — **rezygnacja** (decyzja użytkownika 2026-10-12, przynajmniej na razie); zaimplementowane i usunięte przed commitem
- [x] Komponent `InvoiceDetailsContent(invoice, ksefNumber)` gotowy do użycia jako podgląd przed wysyłką (ZAD-013)

## Definicja ukończenia

- [x] Testy (`FormatsTest` — kwoty, ilości, rachunki, daty)
- [x] Sprawdzone na emulatorze na fakturze z KSeF TEST
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja (US-005, decyzja o udostępnianiu)
