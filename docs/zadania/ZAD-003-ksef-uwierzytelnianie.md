# ZAD-003. Klient KSeF: uwierzytelnianie tokenem i odświeżanie

- User story: US-002
- Status: Zrobione (2026-10-11)
- Szacunek: M

## Cel

Moduł `ksef` uzyskuje i odnawia `accessToken` na podstawie tokena KSeF.

## Zakres

- [x] HTTP: Ktor Client 3.6 (silnik OkHttp — JVM i Android) + kotlinx.serialization; `KsefHttpClientFactory`. Błędy w formacie klasycznym `ExceptionResponse` (bez `X-Error-Format: problem-details`) → `KsefApiException`; HTTP 429 → `KsefRateLimitException` z `Retry-After`
- [x] Pobranie certyfikatów `/security/public-key-certificates` (wybór po `usage` i dacie ważności, najnowszy), cache w pamięci; `publicKeyId` przekazywany w żądaniu (zweryfikowane na TEST)
- [x] Szyfrowanie `token|timestampMs` RSA-OAEP z jawnym `OAEPParameterSpec` (SHA-256, MGF1 SHA-256) — `KsefCrypto`; test potwierdza niezgodność z domyślnym MGF1 SHA-1
- [x] Challenge → `/auth/ksef-token` → polling `/auth/{ref}` (co 1 s, limit 2 min, statusy ≠ 100/200 → `KsefAuthenticationException`) → `/auth/token/redeem`; odświeżanie `/auth/token/refresh` — `KsefAuthenticator`
- [x] `KsefTokenManager`: zwraca ważny `accessToken` (margines 60 s), odświeża przed wygaśnięciem, przy odrzuceniu odświeżenia lub wygaśnięciu `refreshToken` loguje się ponownie; token KSeF pobierany leniwie (`suspend () -> String`) — pod Keystore w ZAD-005
- [x] Testy jednostkowe (20, `MockEngine`) + integracyjne na TEST (logowanie, odświeżenie, menedżer tokenów)

## Uwagi

- Testy z odpytywaniem statusu używają `runBlocking`, nie `runTest`: wirtualny czas `runTest` wyzwala limit czasu, zanim `MockEngine` (osobny dispatcher) odpowie.
- Tokeny (`AuthToken`, `KsefAuthTokens`) nie ujawniają wartości w `toString()`; sprawdzono brak tokena w raportach z testów.

- Przepływ zweryfikowany ręcznie 2026-10-11 (curl + openssl) — patrz analiza 01, sekcja 3a

## Definicja ukończenia

- [x] Testy
- [x] Sprawdzone na środowisku KSeF TEST (2026-10-11, ~2,8 s na logowanie + odświeżenie)
- [x] Zaktualizowana dokumentacja / słownik
