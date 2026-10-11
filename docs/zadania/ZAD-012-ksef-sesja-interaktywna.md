# ZAD-012. Klient KSeF: sesja interaktywna, status, UPO

- User story: US-006
- Status: Do zrobienia
- Szacunek: M

## Cel

Wysyłka pojedynczej faktury do KSeF.

## Zakres

- [ ] Klucz AES-256 + IV, szyfrowanie klucza RSA-OAEP (certyfikat `SymmetricKeyEncryption`)
- [ ] `POST /sessions/online` (FA(3)) → `POST .../invoices` (hashe SHA-256, rozmiary) → polling statusu → numer KSeF lub błędy
- [ ] Zamknięcie sesji, pobranie UPO
- [ ] Test integracyjny na TEST: wysyłka faktury i odczyt jej przez ZAD-006

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
