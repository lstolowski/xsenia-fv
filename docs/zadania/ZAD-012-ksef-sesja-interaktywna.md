# ZAD-012. Klient KSeF: sesja interaktywna, status, UPO

- User story: US-006
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Wysyłka pojedynczej faktury do KSeF.

## Zakres

- [x] Klucz AES-256 + IV na sesję, szyfrowanie klucza RSA-OAEP (certyfikat `SymmetricKeyEncryption`, z `publicKeyId`)
- [x] `KsefInvoiceSender.send`: `POST /sessions/online` (FA(3)) → `POST .../invoices` (skróty SHA-256 i rozmiary oryginału i szyfrogramu) → polling statusu co 2 s (100/150 → czekaj, 200 → numer KSeF, inne → `KsefInvoiceRejectedException` z kodem, opisem, szczegółami i `extensions`, np. `originalKsefNumber` dla duplikatu 440), limit 3 min
- [x] `onSubmitted(SubmittedInvoice)` zaraz po przyjęciu do przetwarzania + `awaitResult(submitted)` — wznowienie po zamknięciu aplikacji (ZAD-013)
- [x] Pobranie UPO faktury (`GET /sessions/{ref}/invoices/{invoiceRef}/upo`; brak UPO nie unieważnia przyjętej faktury); zamknięcie sesji zawsze (także po odrzuceniu)
- [x] Test integracyjny na TEST (tylko z `-PsendInvoices`): faktura z `Fa3XmlWriter` (dane zmyślone, numer `IT/…`) → numer KSeF, UPO z tym numerem, pobrany z KSeF XML identyczny (SHA-256) z wysłanym — pierwsza wysyłka 2026-10-12, ok. 4,5 s
- [x] Ponowienie po HTTP 401 wydzielone do `KsefTokenManager.withAccessToken` (wspólne dla pobierania i wysyłki)

## Definicja ukończenia

- [x] Testy (5 jednostkowych na `MockEngine`: przebieg z odszyfrowaniem wysłanej faktury, odrzucenie, duplikat, przekroczenie czasu + `awaitResult`, brak UPO)
- [x] Sprawdzone na środowisku KSeF TEST
- [x] Zaktualizowana dokumentacja (AGENTS.md — zgoda na wysyłkę na TEST, `-PsendInvoices`)

## Uwagi

- Nie używamy nagłówka `X-KSeF-Feature: subject-identifier-validation` — NIP-y testowe nabywców są zmyślone.
