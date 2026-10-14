# ZAD-033. Listy, szczegóły i wyszukiwanie: numer VAT UE / identyfikator nabywcy

- User story: US-012
- Status: Do zrobienia
- Szacunek: S

## Cel

Faktury dla nabywców zagranicznych pokazują i dają się wyszukać po numerze VAT UE / identyfikatorze, tak jak polskie po NIP-ie.

## Zakres

- [ ] `InvoiceListLogic`: identyfikator kontrahenta z metadanych KSeF (`buyer.identifier.type`: `Nip`, `VatUe`, `Other`, `None`) i z faktur niewysłanych (kontrahent z profilu); etykieta „NIP …” / „VAT UE DE…” / „ID …”
- [ ] Wyszukiwanie po numerze VAT UE / identyfikatorze (bez separatorów, z prefiksem kraju lub bez)
- [ ] Szczegóły faktury: nabywca z numerem VAT UE / identyfikatorem i krajem

## Definicja ukończenia

- [ ] Testy: `InvoiceListLogic` dla każdego typu identyfikatora, wyszukiwanie
- [ ] Sprawdzone na emulatorze z KSeF TEST (faktury z ZAD-031)
- [ ] Zaktualizowana dokumentacja / słownik
