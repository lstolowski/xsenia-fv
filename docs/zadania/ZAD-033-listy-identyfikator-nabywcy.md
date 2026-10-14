# ZAD-033. Listy, szczegóły i wyszukiwanie: numer VAT UE / identyfikator nabywcy

- User story: US-012
- Status: Zrobione (2026-10-14)
- Szacunek: S

## Cel

Faktury dla nabywców zagranicznych pokazują i dają się wyszukać po numerze VAT UE / identyfikatorze, tak jak polskie po NIP-ie.

## Zakres

- [x] `InvoiceListLogic`: `InvoiceRow.counterpartyTaxId` (`CounterpartyTaxId`: typ + wartość) zamiast `counterpartyNip` — z metadanych KSeF (`Nip`, `VatUe` → `DE999999990`, `Other` → `US00-0000000`, `None` → brak; sprawdzone na fakturach z ZAD-031) i z faktur niewysłanych (kontrahent z profilu, w tym samym formacie); etykieta „NIP …” / „VAT UE …” / „ID …”
- [x] Wyszukiwanie po numerze VAT UE / identyfikatorze (spacje, kropki i myślniki pomijane, z prefiksem kraju lub bez); podpowiedź pola: „Szukaj: nazwa lub numer”
- [x] Szczegóły faktury: „Numer VAT UE: …” (dawniej „NIP UE”) / „Identyfikator: …” i nazwa kraju pod adresem nabywcy zagranicznego

## Definicja ukończenia

- [x] Testy: `InvoiceListLogic` dla każdego typu identyfikatora, wyszukiwanie
- [x] Sprawdzone na emulatorze z KSeF TEST (2026-10-14, faktury z ZAD-031): lista — „VAT UE DE999999990”, „ID US00-0000000”; „999 999” znajduje fakturę dla nabywcy z Niemiec; szczegóły — numer VAT UE, adres i „Niemcy”
- [x] Zaktualizowana dokumentacja / słownik
