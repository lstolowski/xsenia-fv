# ZAD-006. Klient KSeF: metadane, pobieranie XML, eksport paczki

- User story: US-002
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Moduł `ksef` pobiera listy i treść faktur.

## Zakres

- [x] `POST /invoices/query/metadata` — `KsefInvoiceClient.queryMetadata`: strony po 250, sortowanie rosnąco, przy `isTruncated` kontynuacja od daty ostatniego rekordu, okna po 90 dni (limit KSeF 100 dni), deduplikacja po numerze KSeF
- [x] `GET /invoices/ksef/{ksefNumber}` → XML (`downloadInvoiceXml`), weryfikacja SHA-256 z nagłówkiem `x-ms-meta-hash`
- [x] `POST /invoices/exports` (`exportInvoices`): klucz AES-256 + IV szyfrowany RSA-OAEP kluczem `SymmetricKeyEncryption`, polling statusu co 2 s (limit 5 min), pobranie części z adresów bez tokena, weryfikacja skrótów (zaszyfrowanej i odszyfrowanej części), odszyfrowanie AES-256-CBC każdej części osobno, rozpakowanie ZIP: `{ksefNumber}.xml` + `_metadata.json`
- [x] Testy jednostkowe (`MockEngine`, 8) i integracyjne na TEST (lista sprzedażowa/zakupowa, XML, eksport)
- [x] Ponowne logowanie i powtórzenie żądania po HTTP 401
- [x] Kwoty jako `BigDecimal` czytane z tekstu JSON (bez `double`); `InvoiceMetadata` serializowalne do lokalnego lustra

## Definicja ukończenia

- [x] Testy
- [x] Sprawdzone na środowisku KSeF TEST
- [x] Zaktualizowana dokumentacja / słownik

## Uwagi

- Testy integracyjne nie wypisują treści faktur (środowisko TEST jest publiczne); sprawdzono brak treści faktur w raportach z testów.
