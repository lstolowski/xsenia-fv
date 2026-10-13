# ZAD-025. Pierwsze uruchomienie (token → Moja firma) i braki w formularzu „Nowa faktura”

- User story: US-010
- Status: Do zrobienia
- Szacunek: S

## Cel

Konfiguracja przy pierwszym uruchomieniu bez folderu z plikami: token KSeF, potem „Moja firma” z automatycznym pobraniem danych; formularz faktury prowadzi do uzupełnienia brakujących danych.

## Zakres

- [ ] Ekran konfiguracji: krok 1 — token KSeF (NIP z tokena wymagany); krok 2 — formularz „Moja firma” z ZAD-023 z **automatycznym** pobraniem danych z Białej listy przy wejściu i wyborem rachunków z rejestru; „Zapisz” → ekran główny
- [ ] Aplikacja z tokenem, ale bez zapisanej firmy (np. po migracji z wersji z plikami) — otwiera krok 2 konfiguracji
- [ ] Formularz „Nowa faktura”: gdy brakuje danych firmy, rachunku PLN lub kontrahenta — lista braków i przycisk „Uzupełnij” (do „Moja firma” lub „Kontrahenci”); zamiast dotychczasowych komunikatów o plikach JSON
- [ ] Teksty o plikach JSON i folderze usunięte z UI (`strings.xml`)

## Definicja ukończenia

- [ ] Testy (logika braków)
- [ ] Sprawdzone na emulatorze: czysta instalacja (token → Moja firma → ekran główny) i aktualizacja z wersji z plikami; faktura wystawiona z danymi zmyślonymi wprowadzonymi w aplikacji przyjęta przez KSeF TEST
- [ ] Zaktualizowana dokumentacja: `docs/srodowisko/instalacja-apk-telefon.md`, README.md, US-010
