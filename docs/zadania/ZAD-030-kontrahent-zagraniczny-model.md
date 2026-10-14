# ZAD-030. Kontrahent zagraniczny: model, walidacja, migracja

- User story: US-012
- Status: Do zrobienia
- Szacunek: M

## Cel

Kontrahent ma kraj, rodzaj i numer identyfikatora oraz opcjonalną domyślną walutę; aplikacja identyfikuje go wewnętrznym `id`, a nie NIP-em. Dane sprzed zmiany (kontrahenci, szkic, faktury niewysłane) działają dalej ([ADR 0009](../adr/0009-nabywca-zagraniczny.md)).

## Zakres

- [ ] `Counterparty`: `id`, `countryCode` (domyślnie `PL`), `taxId` (`NIP` / `VAT_UE` / `OTHER` / brak), `defaultCurrency: InvoiceCurrency?`; `nip` tylko dla polskich
- [ ] `Countries`: kody krajów jak `KodKraju` w FA(3), polskie nazwy (Locale), zbiór krajów UE, prefiks VAT UE (`GR` → `EL`, `XI`)
- [ ] Walidacja (`ProfileValidator`): PL — NIP (suma kontrolna); UE — numer VAT UE wg wzorca `(\d|[A-Z]|\+|\*){1,12}` (bez prefiksu; wpisany z prefiksem — prefiks usuwany); inny kraj — identyfikator opcjonalny (limit długości); unikalność kraj + numer; kraj i identyfikator niezmienne po zapisie
- [ ] Migracja `counterparties.json`: brak kraju → `PL` + NIP; `id` z NIP-u; zapis w nowym formacie
- [ ] `InvoiceForm.counterpartyNip` → `counterpartyId` (stare pole czytane przy wczytywaniu szkicu i faktur niewysłanych); usuwanie kontrahenta i „użyty w szkicu” po `id`
- [ ] `SalesInvoiceDraft.buyer` z nowym modelem (XML — ZAD-031)

## Definicja ukończenia

- [ ] Testy: walidacja dla PL / UE / spoza UE, prefiks Grecji, duplikaty, migracja starego `counterparties.json`, stary szkic i faktura niewysłana z `counterpartyNip`
- [ ] Zmyślone przykłady kontrahentów zagranicznych w [docs/przyklady/kontrahenci.md](../przyklady/kontrahenci.md)
- [ ] Zaktualizowana dokumentacja / słownik
