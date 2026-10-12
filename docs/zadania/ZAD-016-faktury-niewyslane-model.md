# ZAD-016. Faktury niewysłane: model, magazyn, numeracja

- User story: US-007
- Status: Do zrobienia (po potwierdzeniu P1–P7)
- Szacunek: M

## Cel

Faktury sprzedażowe zapisane bez wysyłki przechowywane na telefonie, edytowalne i uwzględniane w numeracji.

## Zakres

- [ ] Model faktury niewysłanej: identyfikator (UUID), dane formularza (`InvoiceForm`), data utworzenia i ostatniej zmiany
- [ ] Magazyn plików `filesDir/unsent/{id}.json` (zapis atomowy), poza katalogiem lustra — nieusuwany przez „Wyczyść cache” (P7)
- [ ] Operacje: zapisz / zaktualizuj / usuń / lista
- [ ] Numeracja (P2): numery faktur niewysłanych w sugestii i wykrywaniu duplikatów; edytowana faktura pomijana przy sprawdzaniu własnego numeru
- [ ] Usługa wysyłki: wysyłka faktury niewysłanej powiązana z jej id — po przyjęciu przez KSeF lokalna kopia usuwana; po błędzie przed dotarciem do KSeF faktura zostaje niewysłana; faktura „w przetwarzaniu” nieedytowalna
- [ ] Testy jednostkowe (magazyn, numeracja z fakturami niewysłanymi, przejścia stanów po wysyłce)

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na emulatorze z KSeF TEST
- [ ] Zaktualizowana dokumentacja / słownik
