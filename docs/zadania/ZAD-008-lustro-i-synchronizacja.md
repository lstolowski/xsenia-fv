# ZAD-008. Lokalne lustro faktur i synchronizacja

- User story: US-002
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Implementacja ADR 0003 w aplikacji.

## Zakres

- [x] Nowy moduł JVM `mirror` (zależny od `ksef` i `invoice`): `InvoiceMirror` — pliki `invoices/sync-state.json`, `invoices/{sales|purchase}/index.json` (metadane + data dostawy z XML), `invoices/{sales|purchase}/xml/{ksefNumber}.xml`
- [x] `InvoiceSynchronizer`: pierwsza synchronizacja od 2026-04-01 (czasu polskiego) eksportem paczek w oknach 90 dni do HWM (obsługa obciętych paczek); każda synchronizacja: zapytanie o metadane od punktu kontrolnego do „teraz” (także niezamknięty zakres po HWM), pobranie XML tylko nowych faktur, przesunięcie punktu kontrolnego do nowego HWM; postęp zapisywany po każdym oknie i każdej fakturze (wznowienie po przerwie)
- [x] `InvoiceSyncService` w zakresie aplikacji: automatycznie po odblokowaniu, jeśli od ostatniej synchronizacji minęło ≥ 15 min (zalecenie MF), oraz ręcznie („Odśwież z KSeF”); karta na ekranie głównym: postęp, liczba faktur, czas ostatniej synchronizacji, błędy po polsku (limit, sieć, uwierzytelnienie, brak tokena / company.json)
- [x] Atomowy zapis plików (plik tymczasowy + `ATOMIC_MOVE`); XML przed indeksem; ochrona nazw plików (format numeru KSeF)

## Definicja ukończenia

- [x] Testy (11 jednostkowych na sztucznym źródle; integracyjny na KSeF TEST: pierwsza synchronizacja + kolejna bez pobrań)
- [x] Sprawdzone na emulatorze z KSeF TEST: 1 faktura sprzedażowa, data dostawy 2026-09-30 z XML, ręczne odświeżenie bez ponownych pobrań
- [x] Zaktualizowana dokumentacja (ADR 0002, ADR 0003, AGENTS.md)

## Uwagi

- `KsefTestConfig` przeniesiony do `ksef/src/testFixtures` — wspólny dla testów integracyjnych modułów `ksef` i `mirror`.
- Limity: pierwsza synchronizacja = 3 okna × 2 kategorie = 6 eksportów (limit 20/h); kolejne = 2 zapytania o metadane + pobrania nowych XML (64/h).
