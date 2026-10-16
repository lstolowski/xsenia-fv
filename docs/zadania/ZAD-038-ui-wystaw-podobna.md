# ZAD-038. UI: przycisk „Wystaw podobną”, szkic i kontrahent, przejście do formularza

- User story: US-015
- Status: Zrobione (2026-10-16)
- Szacunek: S

## Cel

Na dole szczegółów faktury sprzedażowej przycisk „Wystaw podobną” otwiera formularz nowej faktury z kopią ([ZAD-037](ZAD-037-wspolna-podpowiedz-kopia-faktury.md)); dalej podgląd, zapis bez wysyłki lub wysyłka z biometrią — bez zmian ([ADR 0011](../adr/0011-wystaw-podobna.md)).

## Zakres

- [x] `InvoiceDetailsViewModel.issueSimilar()`: numery zajęte (lustro + niewysłane, `TakenNumbers`), `SimilarInvoice.of`, zapis `newCounterparty` (`ProfileRepository.saveCounterparty`), potem szkicu (`IssueStorage.saveDraft` — zastępuje poprzedni); `similarReady` / `similarFailed`; ochrona przed podwójnym kliknięciem
- [x] `sendingNewInvoice`: przycisk nieaktywny z informacją, gdy trwa wysyłka nowej faktury (`SendState.Sending` bez `unsentId`)
- [x] `InvoiceDetailsScreen`: `PrimaryButton` „Wystaw podobną” pod treścią faktury — tylko sprzedażowa, `SimilarInvoice.canCopy`, XML odczytany
- [x] `MainScaffold`: po `similarReady` przejście do `new-invoice`; błąd zapisu — Toast „Nie udało się przygotować podobnej faktury. Spróbuj ponownie.”
- [x] Sprawdzone na emulatorze z KSeF TEST

## Definicja ukończenia

- [x] Testy jednostkowe logiki (ZAD-037); `./gradlew build` (testy, lint)
- [x] Sprawdzone na emulatorze z KSeF TEST
- [x] Zaktualizowana dokumentacja / słownik

## Uwagi

- Sprawdzone na emulatorze `Samsung_S23_xsenia` (KSeF TEST, 2026-10-16, ręcznie przez adb):
  - faktura sprzedażowa `05/10/2026` (Testowa Sp. z o.o., 1 godz. × 100,00 zł, 23%) → „Wystaw podobną” na dole szczegółów → formularz: kontrahent, pozycja, PLN i rachunek Toyota skopiowane; numer `07/10/2026` (kolejny po `06/10/2026`), data wystawienia i dostawy 16.10.2026 (dzień 16. → dziś) → podgląd → „Wyślij do KSeF” → biometria → **przyjęta** (numer KSeF, UPO zapisane); szkic usunięty, faktura na liście października;
  - faktura `06/10/2026` (EUR, „np”) dla nabywcy „Niemiecki” spoza Kontrahentów → nabywca **dopisany do Kontrahentów** (DE, VAT UE, adres z faktury), waluta EUR, stawka „np”, rachunek EUR, numer `08/10/2026`, kurs roboczy NBP pobrany na nowo (tabela z 15.10, nie z faktury źródłowej);
  - szkic z kopią „Niemiecki” → „Wystaw podobną” z faktury `07/10/2026` → szkic **zastąpiony bez pytania** → podgląd → „Zapisz bez wysyłki do KSeF” → faktura niewysłana `08/10/2026` na liście (potem usunięta w aplikacji), szkic wyczyszczony;
  - faktura zakupowa — brak przycisku; w logach bez błędów;
  - **kurs NBP przy kopii faktury w walucie** (prośba użytkownika): źródło `11/09/2026` (Lorem Testowy GmbH, EUR, dostawa 30.09, `KursWaluty` 4,3728) → „Wystaw podobną” → formularz: kurs roboczy **4,3691 z 15.10.2026 (tabela 201/A/NBP/2026)**, pobrany dla nowych dat (wystawienie i dostawa 16.10) → podgląd: kurs na dzień wysyłki 4,3691 → wysyłka → **przyjęta**; XML z KSeF (lustro): `KursWaluty` 4.3691, uwaga „Kurs waluty: 1 EUR = 4,3691 PLN, tabela NBP 201/A/NBP/2026 z 2026-10-15” — kurs starej faktury nie został użyty.
- Po zamknięciu formularza (zapis, „Gotowe”, strzałka wstecz) aplikacja wraca do szczegółów faktury źródłowej — jak każdy ekran w nawigacji.
- Kurs: `SimilarInvoice` nie czyta `KursWaluty` faktury źródłowej (`InvoiceForm.exchangeRate = null`), dalej bez zmian logika nowej faktury (ADR 0008): kurs roboczy `NewInvoiceViewModel.refreshRate` (`ExchangeRateService.rateFor(waluta, data dostawy, data wystawienia)`), kurs ostateczny w podglądzie (dzień wysyłki) i ponownie w `InvoiceSendService` przy wysyłce.
- Zauważone przy teście (istniało wcześniej, dotyczy też zwykłej nowej faktury): gdy kurs przychodzi z pamięci podręcznej od razu po otwarciu formularza, autozapis (`drop(1)` w `NewInvoiceViewModel`) pomija tę zmianę i kurs roboczy nie trafia do `draft.json` do następnej edycji. Bez wpływu na wybór kursu (formularz pobiera go ponownie po otwarciu, podgląd i wysyłka — zawsze).
- Stan „przycisk nieaktywny w trakcie wysyłki nowej faktury” — tylko w kodzie (wysyłka na TEST trwa kilka sekund, a przejście do szczegółów w tym czasie wymaga opuszczenia ekranu statusu, który blokuje „wstecz”).
