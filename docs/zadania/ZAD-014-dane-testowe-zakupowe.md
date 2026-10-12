# ZAD-014. Dane testowe: faktury zakupowe na KSeF TEST

- User story: US-004
- Status: Zrobione (2026-10-12)
- Szacunek: S

## Cel

Na NIP testowym muszą istnieć faktury zakupowe, aby przetestować listę zakupową.

## Zakres

- [x] Drugi kontekst TEST (zmyślony dostawca, NIP 5450242131) — token od użytkownika w `secrets/ksef-test.properties` (`ksef.supplier.nip`, `ksef.supplier.token`), obsługa w `KsefTestConfig.supplier`
- [x] Generator `PurchaseTestDataGenerator` (testy integracyjne `ksef`, tylko z `-PcreatePurchaseInvoices`) — wysłano 5 faktur zakupowych (wszystkie przyjęte, z UPO):
  - `ZAK/TEST/101` — VAT 23%, `P_6` 30.09 ≠ `P_1` 02.10 (wrzesień)
  - `ZAK/TEST/102` — 2 pozycje, ilość ułamkowa (październik)
  - `ZAK/TEST/103` — 23% / 8% / zw z podstawą zwolnienia, `P_6A` na pozycjach, zapłacona kartą (wrzesień, dostawa 28.09)
  - `ZAK/TEST/104` — EUR z `P_14_1W` i kursem (październik, osobna suma EUR)
  - `ZAK/TEST/101/KOR` — korekta 101 (stan przed / po, kwoty ujemne, numer KSeF korygowanej)

## Uwagi

- Uruchomienie: `./gradlew :ksef:integrationTest --tests '*PurchaseTestDataGenerator*' -PcreatePurchaseInvoices` — każde tworzy 5 nowych, nieusuwalnych faktur

## Definicja ukończenia

- [x] Testy
- [x] Sprawdzone na emulatorze: lista zakupowa wrzesień (3 faktury, korekta oznaczona) i październik (PLN + osobno EUR), szczegóły korekty i faktury wielostawkowej
- [x] Zaktualizowana dokumentacja (AGENTS.md)
