# ZAD-030. Kontrahent zagraniczny: model, walidacja, migracja

- User story: US-012
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

Kontrahent ma kraj, rodzaj i numer identyfikatora oraz opcjonalną domyślną walutę; aplikacja identyfikuje go wewnętrznym `id`, a nie NIP-em. Dane sprzed zmiany (kontrahenci, szkic, faktury niewysłane) działają dalej ([ADR 0009](../adr/0009-nabywca-zagraniczny.md)).

## Zakres

- [x] `Counterparty`: `id`, `countryCode` (domyślnie `PL`), `nip` (tylko polscy), `taxNumber` (numer VAT UE bez prefiksu albo inny identyfikator), `defaultCurrency: InvoiceCurrency?`; wyliczane `taxIdType` (`NIP` / `VAT_UE` / `OTHER` / `NONE`), `taxIdText` (np. `DE999999990`, `EL000000000`), `isForeign`; `id` = NIP, `{kraj}-{numer}` albo `{kraj}-{losowy UUID}` bez numeru
- [x] `Countries`: kody ISO 3166 (`Locale.getISOCountries`), polskie nazwy, kraje UE, prefiks VAT UE (`GR` → `EL`). Bez `XI` (Irlandia Płn. — prefiks tylko dla towarów; przy usługach to GB)
- [x] Walidacja (`ProfileValidator`): PL — NIP (suma kontrolna); UE — numer VAT UE wg wzorca `(\d|[A-Z]|\+|\*){1,12}` (bez prefiksu; wpisany z prefiksem — prefiks usuwany); inny kraj — identyfikator opcjonalny (limit długości); unikalność kraj + numer; kraj i identyfikator niezmienne po zapisie
- [x] Migracja `counterparties.json` bez przepisywania pliku: brak kraju → `PL`, `id` = NIP; nowy format (`countryCode`, `id`) zapisywany przy następnej zmianie
- [x] `InvoiceForm.counterpartyNip` → `counterpartyId`, zapisywane nadal pod nazwą `counterpartyNip` w JSON — dla polskich kontrahentów `id` = NIP, więc stare szkice i faktury niewysłane wskazują tego samego kontrahenta; usuwanie, „użyty w szkicu”, wybór w fakturze, nawigacja (`?id=`, kodowane) po `id`
- [x] `SalesInvoiceDraft.buyer` z nowym modelem; `Fa3XmlWriter` do czasu ZAD-031 wymaga NIP-u nabywcy. Formularz kontrahenta bez pola tekstowego „Kraj” (wybór kraju — ZAD-032); kafelki pokazują NIP albo kraj i numer

## Definicja ukończenia

- [x] Testy: walidacja dla PL / UE / spoza UE, prefiks Grecji, duplikaty, nieznany kraj, edycja (kraj i numer bez zmian), migracja starego `counterparties.json`, stary szkic z `counterpartyNip`, wybór kontrahenta zagranicznego po `id`; sprawdzone na emulatorze: dotychczasowi kontrahenci wczytani i do wyboru w fakturze
- [x] Zmyślone przykłady kontrahentów zagranicznych w [docs/przyklady/kontrahenci.md](../przyklady/kontrahenci.md)
- [x] Zaktualizowana dokumentacja / słownik
