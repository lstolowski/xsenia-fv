# US-001. Bezpieczne uruchomienie i konfiguracja

- Priorytet: MVP
- Status: Gotowa

## Historyjka

Jako **właściciel firmy** chcę **jednorazowo skonfigurować aplikację (token KSeF, folder z plikami JSON) i otwierać ją biometrią**, aby **nikt poza mną nie miał dostępu do moich faktur ani możliwości ich wystawiania**.

## Kryteria akceptacji

- [ ] Przy pierwszym uruchomieniu aplikacja prosi o token KSeF i zapisuje go zaszyfrowany kluczem z Android Keystore; token nigdy nie jest wyświetlany w całości.
- [ ] Przy pierwszym uruchomieniu wskazuję folder (np. `Dokumenty/xsenia`) z plikami `company.json`, `banks.json`, `counterparties.json`; aplikacja pamięta wybór.
- [ ] Przy każdym starcie aplikacja wczytuje pliki JSON; błędy (plik, pozycja, pole) są pokazane czytelnie, a przeglądanie faktur działa dalej.
- [ ] Każde otwarcie aplikacji (także powrót z tła po dłuższym czasie) wymaga biometrii; bez niej nie widać żadnych danych.
- [ ] Wersja testowa i produkcyjna różnią się nazwą i ikoną, mają osobne ustawienia i mogą być zainstalowane jednocześnie.
- [ ] W ustawieniach mogę zmienić token, folder oraz wyczyścić cache faktur.
- [ ] Token i klucze nie trafiają do kopii zapasowej Androida.

## Zadania

- ZAD-001
- ZAD-004
- ZAD-005
