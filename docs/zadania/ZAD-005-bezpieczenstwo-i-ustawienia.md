# ZAD-005. Aplikacja: token w Keystore, biometria, wybór folderu, ustawienia

- User story: US-001
- Status: Do zrobienia
- Szacunek: M

## Cel

Pierwsze uruchomienie, ekran Ustawienia i blokada biometryczna.

## Zakres

- [ ] Szyfrowanie tokena kluczem AES z Android Keystore; zapis w DataStore
- [ ] `BiometricPrompt` przy starcie i powrocie z tła (próg czasu do ustalenia, np. 1 min)
- [ ] Wybór folderu `ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission`; wczytanie plików przy starcie (ZAD-004)
- [ ] Ekran Ustawienia: zmiana tokena (maskowany), zmiana folderu, „Wyczyść cache”, wersja i środowisko
- [ ] `dataExtractionRules`/`fullBackupContent` — wyłączenie tokena i cache z backupu
- [ ] Test na emulatorze z odciskiem palca

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
