# AGENTS.md

Instrukcje dla agentów AI (Claude Code i innych) pracujących w tym repozytorium.

## O projekcie

**xsenia-fv** — prosta, prywatna aplikacja na Androida (APK instalowany ręcznie, bez Google Play) do KSeF
(Krajowy System e-Faktur, API 2.0) dla jednego użytkownika: przegląd faktur sprzedażowych i zakupowych,
wystawianie nowej faktury sprzedażowej VAT (bez korekt), filtrowanie list. Bez backendu i bez bazy danych.

Stan: implementacja — postęp w [docs/zadania/](docs/zadania/README.md). Szczegóły w [docs/](docs/README.md).

## ⚠️ BARDZO WAŻNE: tylko środowisko testowe i tylko dane zmyślone

Środowisko KSeF TEST jest **publiczne** — dane w nim są współdzielone między integratorami i mogą wyciec.

- Podczas developmentu używaj **wyłącznie środowiska KSeF TEST** (testy integracyjne, wersja aplikacji `ksefTest`). Nigdy nie wywołuj DEMO ani PROD.
- Wszystkie dane wprowadzane do KSeF TEST, a także dane w testach, przykładach, fixture'ach i dokumentacji muszą być **zmyślone**: testowe NIP-y i numery rachunków (z poprawną sumą kontrolną), zmyślone nazwy, osoby, adresy, e-maile, telefony i nazwy usług. **Gotowe przykłady są w [docs/przyklady/](docs/przyklady/README.md)** — korzystaj z nich i tam dopisuj nowe; nie umieszczaj przykładów danych w AGENTS.md. NIP kontekstu testowego użytkownika jest w `secrets/ksef-test.properties`.
- **Nigdy nie używaj prawdziwego rachunku ani NIP-u użytkownika** w repozytorium (kod, testy, dokumentacja). Prawdziwe NIP-y wolno sprawdzać w API Białej listy MF tylko w trakcie sesji (np. do podejrzenia formatu odpowiedzi) — do repozytorium trafiają wyłącznie zmyślone odpowiedzi.
- Nie kopiuj do repozytorium ani do KSeF TEST żadnych prawdziwych danych użytkownika lub jego kontrahentów (także z faktur PROD).
- Jeśli zauważysz w repozytorium dane wyglądające na prawdziwe — zgłoś to użytkownikowi.
- Zgoda użytkownika (2026-10-12): testy integracyjne i testy na emulatorze **mogą wysyłać faktury do KSeF TEST** (kontekst NIP z `secrets/ksef-test.properties`), wyłącznie z danymi zmyślonymi jak wyżej. Faktur w KSeF nie da się usunąć — wysyłaj tylko tyle, ile potrzeba.

## Struktura

- `docs/analiza/` — analizy (KSeF API, warianty architektury, pytania warsztatowe)
- `docs/adr/` — Architecture Decision Records
- `docs/user-stories/` — historyjki użytkownika
- `docs/zadania/` — rozpisane zadania implementacyjne
- `docs/przyklady/` — zmyślone dane do developmentu (NIP-y, firma, kontrahenci, rachunki, odpowiedzi Białej listy)
- `docs/srodowisko/` — przygotowanie środowiska deweloperskiego (Android Studio, emulator)
- `docs/slownik.md` — słownik pojęć (obowiązująca terminologia)
- `secrets/` — lokalne dane dostępowe (token KSeF TEST itp.), **ignorowane przez git**; wzór w `*.example`

## Sekrety

- Nigdy nie commituj tokenów, certyfikatów ani kluczy podpisu. Nie wypisuj ich wartości w logach ani odpowiedziach.
- Testy integracyjne korzystają wyłącznie ze środowiska KSeF **TEST** (`secrets/ksef-test.properties`).

## Budowanie i uruchamianie

JDK: wbudowane w Android Studio (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`). Android SDK wskazany w `local.properties` (poza gitem).

- `./gradlew build` — kompilacja wszystkich modułów i wariantów, testy jednostkowe, lint (ostrzeżenia = błędy)
- `./gradlew :ksef:test :invoice:test :mirror:test :whitelist:test` — szybkie testy modułów JVM
- `./gradlew integrationTest -PsendInvoices` — dodatkowo test **wysyłający fakturę** do KSeF TEST (każde uruchomienie tworzy nową, nieusuwalną fakturę testową)
- `./gradlew :ksef:integrationTest --tests '*PurchaseTestDataGenerator*' -PcreatePurchaseInvoices` — tworzy 5 zmyślonych faktur **zakupowych** od drugiego kontekstu TEST (`ksef.supplier.*` w secrets)
- `./gradlew integrationTest` — testy z prawdziwym KSeF **TEST** (wymaga `secrets/ksef-test.properties`, bez niego testy są pomijane; nie wchodzi w `build`). Unikaj 16:00–18:00 (prace serwisowe TEST). Obejmuje też `:whitelist:integrationTest` — jedno zapytanie do prawdziwej Białej listy MF z testowym NIP-em (limit API: 100 zapytań dziennie)
- `./gradlew :app:assembleKsefTestDebug` — APK wersji testowej (KSeF TEST); `installKsefTestDebug` instaluje na emulatorze
- `./gradlew releaseApk` — podpisany APK produkcyjny (R8) w `build/dist/`; klucz w `secrets/keystore.properties` (poza gitem, instrukcja w README.md). `releaseApkTest` — to samo dla KSeF TEST. Klucza produkcyjnego nie generuj ani nie odczytuj — do sprawdzenia buildu release użyj klucza debug: `-Pxsenia.signingProperties=<plik poza repo>`
- Wersje aplikacji (product flavors): `ksefTest` → `pl.xsenia.fv.test`, `ksefProd` → `pl.xsenia.fv`
- Emulator: AVD `Samsung_S23_xsenia` (Android 16, API 36)
  - PIN blokady ekranu emulatora: `1111` (wyłącznie emulator testowy, nie dotyczy prawdziwego telefonu)
  - Odcisk palca zarejestrowany jako *Finger 1* (Extended controls → Fingerprint); z terminala: `adb -e emu finger touch 1` a potem `adb -e emu finger remove 1`. **Bywa zawodne** (odrzucenia) — po 1–2 nieudanych próbach przestań (ryzyko blokady czytnika) i poproś użytkownika o odblokowanie przez Extended controls → Touch Sensor. Po restarcie emulatora pierwsze odblokowanie ekranu wymaga PIN-u — poproś użytkownika
  - Dane testowe na emulatorze: do czasu ZAD-022 pliki w `/sdcard/Documents/xsenia-test/` (`company.json` z NIP-em kontekstu testowego); od US-010 — „Moja firma”, rachunki i kontrahenci wpisani w aplikacji (wartości z [docs/przyklady/](docs/przyklady/README.md))

## Konwencje językowe

- Dokumentację pisz **po polsku**.
- Kod aplikacji oraz komentarze w kodzie zawsze **po angielsku**.
- Nazwy zmiennych, funkcji, klas, plików z kodem zawsze **po angielsku**.
- Interfejs użytkownika (teksty widoczne w UI) **po polsku**.

## Terminologia

- Używaj nomenklatury KSeF (np. numer KSeF, Podmiot1/Podmiot2, UPO, sesja interaktywna, token KSeF).
- Jeśli użytkownik zaproponuje własną nazwę — używaj terminologii użytkownika.
- Obowiązujące nazwy i ich odpowiedniki w kodzie są w [docs/slownik.md](docs/slownik.md) — sprawdź go przed nazwaniem nowego pojęcia i aktualizuj przy zmianach.

## Źródła wiedzy o KSeF

- Dokumentacja integracji KSeF API 2.0: https://github.com/CIRFMF/ksef-docs
- OpenAPI (TEST): https://api-test.ksef.mf.gov.pl/docs/v2
- Schemat faktury FA(3): `faktury/schemy/FA/schemat_FA(3)_v1-0E.xsd` w repozytorium ksef-docs
- API Białej listy MF (wykaz podatników VAT, pobieranie danych firmy po NIP): https://wl-api.mf.gov.pl — [ADR 0007](docs/adr/0007-biala-lista-mf.md)
