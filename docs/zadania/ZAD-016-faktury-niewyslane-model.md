# ZAD-016. Faktury niewysłane: model, magazyn, numeracja

- User story: US-007
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Faktury sprzedażowe zapisane bez wysyłki przechowywane na telefonie, edytowalne i uwzględniane w numeracji.

## Zakres

- [x] Model faktury niewysłanej: identyfikator (UUID), dane formularza (`InvoiceForm`), data utworzenia i ostatniej zmiany
- [x] Magazyn plików `filesDir/unsent/{id}.json` (zapis atomowy), poza katalogiem lustra — nieusuwany przez „Wyczyść cache” (P7)
- [x] Operacje: zapisz / zaktualizuj / usuń / lista
- [x] Numeracja (P2): numery faktur niewysłanych w sugestii i wykrywaniu duplikatów; edytowana faktura pomijana przy sprawdzaniu własnego numeru
- [x] Usługa wysyłki: wysyłka faktury niewysłanej powiązana z jej id — po przyjęciu przez KSeF lokalna kopia usuwana; po błędzie przed dotarciem do KSeF **i po odrzuceniu przez KSeF** faktura zapisywana jako niewysłana (przy odrzuceniu: kod, opis i szczegóły powodu); faktura „w przetwarzaniu” nieedytowalna
- [x] Testy jednostkowe (magazyn, numeracja z fakturami niewysłanymi, przejścia stanów po wysyłce)

## Definicja ukończenia

- [x] Testy
- [x] Sprawdzone na emulatorze z KSeF TEST
- [x] Zaktualizowana dokumentacja / słownik

## Uwagi

- `UnsentInvoiceStore` (`filesDir/unsent/{id}.json`) + `UnsentInvoiceRepository` (StateFlow); `TakenNumbers` — numery z KSeF i faktur niewysłanych (bez edytowanej).
- `InvoiceSendService` z interfejsem `InvoiceSendBackend` (testy bez sieci); `PendingSend` zawiera formularz i id faktury niewysłanej.
