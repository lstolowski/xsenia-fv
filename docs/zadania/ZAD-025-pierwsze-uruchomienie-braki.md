# ZAD-025. Pierwsze uruchomienie (token → Moja firma) i braki w formularzu „Nowa faktura”

- User story: US-010
- Status: Zrobione (2026-10-13)
- Szacunek: S

## Cel

Konfiguracja przy pierwszym uruchomieniu bez folderu z plikami: token KSeF, potem „Moja firma” z automatycznym pobraniem danych; formularz faktury prowadzi do uzupełnienia brakujących danych.

## Zakres

- [x] Ekran konfiguracji: krok 1 — token KSeF (NIP z tokena wymagany, ZAD-022), „Dalej”; krok 2 — „Konfiguracja — Moja firma” (`CompanyFormScreen` z ZAD-023) z **automatycznym** pobraniem danych z Białej listy przy wejściu (`autoLookup`), wstępem („…u osoby prowadzącej działalność dopisz nazwę firmy…”) i oknem wyboru rachunków z rejestru; bez ręcznego dodawania rachunków (wskazówka: „Inne rachunki dodasz później w Ustawieniach → Moja firma”); „Zapisz” → ekran główny, „Wstecz” → krok 1
- [x] Aplikacja z tokenem, ale bez zapisanej firmy (np. po aktualizacji z wersji z plikami) — od razu krok 2 (`XseniaApp`: brak firmy w profilu → konfiguracja firmy, inaczej ekran główny)
- [x] Formularz „Nowa faktura”: gdy brakuje danych firmy, rachunku PLN lub kontrahenta — karta „Do wystawienia faktury brakuje:” z listą braków (`ProfileGap`) i przyciskiem „Uzupełnij” przy każdym (firma i rachunek → „Moja firma”, kontrahent → nowy kontrahent, wybierany potem automatycznie); zastępuje ogólny komunikat
- [x] Po uzupełnieniu danych w trakcie formularz sam podstawia brakujące domyślne wartości — pierwszy rachunek PLN i jedynego kontrahenta (`InvoiceFormDefaults.fillMissing`), bez zmiany dokonanych wyborów
- [x] Teksty o plikach JSON i folderze usunięte z UI (ZAD-022)

## Definicja ukończenia

- [x] Testy: `InvoiceFormDefaultsTest` (podstawianie domyślnych tylko w pustych polach); braki — `CompanyProfileTest` (ZAD-022); `./gradlew build` zielony
- [x] Sprawdzone na emulatorze (2026-10-13, KSeF TEST): token zapisany, pliki profilu odłożone na bok (jak po aktualizacji z wersji z plikami) → po odblokowaniu krok „Konfiguracja — Moja firma” z automatycznie pobranymi danymi i oknem rachunków z rejestru („Pomiń”); pobrane dane **nadpisane zmyślonymi przed zapisem** → ekran główny i synchronizacja; „Nowa faktura” → „Do wystawienia faktury brakuje: rachunku bankowego w PLN, kontrahenta” → „Uzupełnij” (rachunek zmyślony) → zostaje „kontrahenta” → „Uzupełnij” (kontrahent zmyślony) → formularz z wybranym kontrahentem i podstawionym rachunkiem → podgląd → wysyłka (biometria) → faktura `09/09/2026` **przyjęta przez KSeF TEST**, UPO zapisane. Potem przywrócone wcześniejsze dane profilu
- [x] Czysta instalacja (krok 1 z wpisaniem tokena) — niesprawdzana ponownie: token wpisuje wyłącznie użytkownik (AGENTS.md); krok 1 różni się od ZAD-005 tylko wymogiem NIP-u w tokenie (ZAD-022)
- [x] Zaktualizowana dokumentacja: `docs/srodowisko/instalacja-apk-telefon.md` (pierwsze uruchomienie), AGENTS.md (dane testowe na emulatorze), US-010; README.md nie opisywał plików JSON
