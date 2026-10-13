# US-001. Bezpieczne uruchomienie i konfiguracja

- Priorytet: MVP
- Status: Zrobiona (2026-10-12)

> Od [US-010](US-010-dane-firmy-w-aplikacji.md) dane mojej firmy, rachunki i kontrahenci są wprowadzane w aplikacji, a nie w plikach JSON ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)). Treść poniżej opisuje stan z chwili realizacji.

## Historyjka

Jako **właściciel firmy** chcę **jednorazowo skonfigurować aplikację (token KSeF, folder z plikami JSON) i otwierać ją biometrią**, aby **nikt poza mną nie miał dostępu do moich faktur ani możliwości ich wystawiania**.

## Kryteria akceptacji

- [x] Przy pierwszym uruchomieniu aplikacja prosi o token KSeF i zapisuje go zaszyfrowany kluczem z Android Keystore; token nigdy nie jest wyświetlany w całości.
- [x] Przy pierwszym uruchomieniu wskazuję folder (np. `Dokumenty/xsenia`) z plikami `company.json`, `banks.json`, `counterparties.json`; aplikacja pamięta wybór.
- [x] Przy każdym starcie aplikacja wczytuje pliki JSON; błędy (plik, pozycja, pole) są pokazane czytelnie, a przeglądanie faktur działa dalej.
- [x] Każde otwarcie aplikacji (także powrót z tła po dłuższym czasie) wymaga biometrii; bez niej nie widać żadnych danych.
- [x] Wersja testowa i produkcyjna różnią się nazwą i ikoną, mają osobne ustawienia i mogą być zainstalowane jednocześnie.
- [x] W ustawieniach mogę zmienić token, folder oraz wyczyścić cache faktur.
- [x] Token i klucze nie trafiają do kopii zapasowej Androida.

## Zadania

- ZAD-001
- ZAD-004
- ZAD-005
