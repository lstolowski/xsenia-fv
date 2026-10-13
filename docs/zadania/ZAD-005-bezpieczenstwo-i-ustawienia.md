# ZAD-005. Aplikacja: token w Keystore, biometria, wybór folderu, ustawienia

- User story: US-001
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Pierwsze uruchomienie, ekran Ustawienia i blokada biometryczna.

## Zakres

- [x] Szyfrowanie tokena AES-256-GCM kluczem z Android Keystore (`KeystoreCipher`); szyfrogram w DataStore (`KsefTokenStore`); w UI tylko status i NIP z tokena, nigdy wartość
- [x] `BiometricPrompt` (odcisk palca, zapasowo PIN telefonu) przy starcie i po ≥ 60 s w tle (`AppLock` + `ProcessLifecycleOwner`); monit pokazuje się sam raz na blokadę, potem przycisk „Odblokuj”; miniatura w „Ostatnich aplikacjach” ukryta (`setRecentsScreenshotEnabled(false)`)
- [x] Wybór folderu `ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission`; wczytanie plików przy każdym odblokowaniu i przyciskiem „Odśwież” (`ConfigRepository`); polskie komunikaty błędów (`ConfigIssueFormatter`)
- [x] Ekran konfiguracji przy pierwszym uruchomieniu (folder → token → „Dalej”) i ekran Ustawienia: zmiana tokena (pole maskowane), „Sprawdź połączenie z KSeF”, ostrzeżenie o niezgodności NIP tokena z `company.json`, zmiana folderu, „Wyczyść cache”, wersja i środowisko; ekran główny tymczasowy (do ZAD-009)
- [x] `dataExtractionRules` — wyłączenie tokena i cache z backupu (ZAD-001)
- [x] Test na emulatorze z odciskiem palca: blokada przy starcie, brak blokady po 20 s w tle, blokada i automatyczny monit po 62 s, wybór folderu przez okno systemowe, wyświetlanie błędów w plikach
- [x] Zapis tokena (wprowadzony przez użytkownika) i „Połączenie z KSeF działa — token poprawny.” na emulatorze (KSeF TEST); w DataStore brak tokena w postaci jawnej (sprawdzone `run-as`)

## Definicja ukończenia

- [x] Testy (`AppLockTest`)
- [x] Sprawdzone na środowisku KSeF TEST / emulatorze
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja (AGENTS.md — obsługa odcisku na emulatorze, dane testowe)

## Decyzje

- Klucz Keystore **nie** wymaga uwierzytelnienia użytkownika przy każdym użyciu: dostęp do aplikacji chroni blokada biometryczna, a token musi dać się odszyfrować do ponownego logowania w KSeF w trakcie sesji (`KsefTokenManager`).
- Biometria: `BIOMETRIC_STRONG` z zapasowym PIN-em/hasłem telefonu (`DEVICE_CREDENTIAL`) — brak odcisku nie blokuje dostępu na stałe.
- NIP kontekstu KSeF = NIP z `company.json`; NIP zaszyty w tokenie (`…|nip-XXXXXXXXXX|…`) służy tylko do ostrzeżenia o niezgodności.
- Token wprowadza wyłącznie użytkownik (agent nie wpisuje sekretów w aplikacji).
