# ZAD-003. Klient KSeF: uwierzytelnianie tokenem i odświeżanie

- User story: US-002
- Status: Do zrobienia
- Szacunek: M

## Cel

Moduł `ksef` uzyskuje i odnawia `accessToken` na podstawie tokena KSeF.

## Zakres

- [ ] HTTP: Ktor Client lub OkHttp + kotlinx.serialization; obsługa `X-Error-Format: problem-details`, błędów 4xx/5xx i 429 (`Retry-After`)
- [ ] Pobranie certyfikatów `/security/public-key-certificates` (wybór po `usage`), cache
- [ ] Szyfrowanie `token|timestampMs` RSA-OAEP z jawnym `OAEPParameterSpec` (SHA-256, MGF1 SHA-256)
- [ ] Challenge → `/auth/ksef-token` → polling `/auth/{ref}` → `/auth/token/redeem`; odświeżanie `/auth/token/refresh`
- [ ] Testy jednostkowe (szyfrowanie, mapowanie błędów) + integracyjny na TEST

## Uwagi

- Przepływ zweryfikowany ręcznie 2026-10-11 (curl + openssl) — patrz analiza 01, sekcja 3a

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
