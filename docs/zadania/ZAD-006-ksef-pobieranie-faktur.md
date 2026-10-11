# ZAD-006. Klient KSeF: metadane, pobieranie XML, eksport paczki

- User story: US-002
- Status: Do zrobienia
- Szacunek: M

## Cel

Moduł `ksef` pobiera listy i treść faktur.

## Zakres

- [ ] `POST /invoices/query/metadata` z paginacją (`pageSize` do 250, `hasMore`, `isTruncated`), okna ≤ 100 dni
- [ ] `GET /invoices/ksef/{ksefNumber}` → XML
- [ ] `POST /invoices/exports` + polling statusu + pobranie i odszyfrowanie (AES-256-CBC) paczki ZIP z XML i `_metadata.json`
- [ ] Testy integracyjne na TEST

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
