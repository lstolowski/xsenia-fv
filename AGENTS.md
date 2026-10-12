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
- Wszystkie dane wprowadzane do KSeF TEST, a także dane w testach, przykładach, fixture'ach i dokumentacji muszą być **zmyślone**:
  - NIP-y wyłącznie testowe (z poprawną sumą kontrolną), np.: `6691034274`, `1996845518`, `9945082500`, `9579102904`, `6973432790`, `4824062973`, `3563998415`, `8771617926`; NIP kontekstu testowego użytkownika jest w `secrets/ksef-test.properties`,
  - nazwy i osoby: „Test”, „Testowy”, „Testowa”, „Jan Testowy”, „Testowa Sp. z o.o.” albo Lorem ipsum,
  - adresy: np. „ul. Testowa 1”, „00-001 Testowo”; e-maile w domenie `example.com`; telefony `+48 000 000 000`,
  - numery rachunków: wyłącznie testowe (poprawna suma kontrolna) — **nigdy prawdziwy rachunek użytkownika**:
    - `PL71101075753723914707418989` — Narodowy Bank Polski
    - `PL85216097583216850246002152` — Toyota Bank
    - `PL22161029092633573189646018` — SGB-Bank
    - `PL55109004081797992973253811` — Santander Bank Polska

    (w plikach JSON i polu `NrRB` faktury — 26 cyfr bez prefiksu `PL`),
  - nazwy towarów/usług: „Usługa testowa”, Lorem ipsum.
- Nie kopiuj do repozytorium ani do KSeF TEST żadnych prawdziwych danych użytkownika lub jego kontrahentów (także z faktur PROD).
- Jeśli zauważysz w repozytorium dane wyglądające na prawdziwe — zgłoś to użytkownikowi.

## Struktura

- `docs/analiza/` — analizy (KSeF API, warianty architektury, pytania warsztatowe)
- `docs/adr/` — Architecture Decision Records
- `docs/user-stories/` — historyjki użytkownika
- `docs/zadania/` — rozpisane zadania implementacyjne
- `docs/srodowisko/` — przygotowanie środowiska deweloperskiego (Android Studio, emulator)
- `docs/slownik.md` — słownik pojęć (obowiązująca terminologia)
- `secrets/` — lokalne dane dostępowe (token KSeF TEST itp.), **ignorowane przez git**; wzór w `*.example`

## Sekrety

- Nigdy nie commituj tokenów, certyfikatów ani kluczy podpisu. Nie wypisuj ich wartości w logach ani odpowiedziach.
- Testy integracyjne korzystają wyłącznie ze środowiska KSeF **TEST** (`secrets/ksef-test.properties`).

## Budowanie i uruchamianie

JDK: wbudowane w Android Studio (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`). Android SDK wskazany w `local.properties` (poza gitem).

- `./gradlew build` — kompilacja wszystkich modułów i wariantów, testy jednostkowe, lint (ostrzeżenia = błędy)
- `./gradlew :ksef:test :invoice:test` — szybkie testy modułów JVM
- `./gradlew integrationTest` — testy z prawdziwym KSeF **TEST** (wymaga `secrets/ksef-test.properties`, bez niego testy są pomijane; nie wchodzi w `build`). Unikaj 16:00–18:00 (prace serwisowe TEST)
- `./gradlew :app:assembleKsefTestDebug` — APK wersji testowej (KSeF TEST); `installKsefTestDebug` instaluje na emulatorze
- Wersje aplikacji (product flavors): `ksefTest` → `pl.xsenia.fv.test`, `ksefProd` → `pl.xsenia.fv`
- Emulator: AVD `Samsung_S23_xsenia` (Android 16, API 36)
  - PIN blokady ekranu emulatora: `1111` (wyłącznie emulator testowy, nie dotyczy prawdziwego telefonu)
  - Odcisk palca zarejestrowany jako *Finger 1* (Extended controls → Fingerprint); z terminala: `adb -e emu finger touch 1` a potem `adb -e emu finger remove 1` (bez `remove` kolejne dotknięcia nie są rozpoznawane). Po restarcie emulatora pierwsze odblokowanie ekranu wymaga PIN-u — poproś użytkownika
  - Testowe pliki danych na emulatorze: `/sdcard/Documents/xsenia-test/` (`company.json` z NIP-em kontekstu testowego)

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
