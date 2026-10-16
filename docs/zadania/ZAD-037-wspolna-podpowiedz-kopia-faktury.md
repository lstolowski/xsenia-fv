# ZAD-037. Wspólna podpowiedź dat i numeru, kopiowanie faktury do formularza

- User story: US-015
- Status: Zrobione (2026-10-16)
- Szacunek: S

## Cel

Podpowiedź dat i numeru nowej faktury jest w jednym miejscu (`InvoiceFormDefaults`); faktura sprzedażowa z lustra (`Invoice`) zamienia się w formularz nowej faktury (`SimilarInvoice`) z tymi samymi datami i numerem co zwykła nowa faktura ([ADR 0011](../adr/0011-wystaw-podobna.md)).

## Zakres

- [x] `InvoiceFormDefaults`: `withDefaultDates` (dziś + `DefaultDates`), `withSuggestedNumber` / `suggestedNumber` (`InvoiceNumbering`), `withDefaults`, `changeDeliveryDate`
- [x] `newForm` i `refresh` korzystają z nich zamiast własnych wywołań `DefaultDates` / `InvoiceNumbering`
- [x] `NewInvoiceViewModel`: `setDeliveryDate`, `useSuggestedNumber`, `state.suggestedNumber` przez `InvoiceFormDefaults` (żadnych bezpośrednich wywołań `InvoiceNumbering.suggest` poza `InvoiceFormDefaults`)
- [x] `SimilarInvoice.of`: kontrahent (po `Counterparty.id`; `BrakID` — po kraju i nazwie), rachunek (nadal mój albo domyślny waluty), waluta, pozycje bez `StanPrzed` (ilość i cena w zapisie formularza: `2,5`, `120,00`; `np I` / `np II` → „np”; nieobsługiwana stawka → domyślna dla nabywcy); bez kursu
- [x] `SimilarInvoice.newCounterparty`: nabywca spoza Kontrahentów z danych faktury, walidowany `ProfileValidator.counterparty` (VAT UE bez prefiksu, także `EL`; `NrID` bez kodu kraju); niepoprawny — null
- [x] `SimilarInvoice.canCopy`: tylko `RodzajFaktury = VAT`

- [x] Próg reguły R3 zmieniony z 15. na 5. dzień miesiąca (`DefaultDates.LAST_DAY_FOR_PREVIOUS_MONTH = 5`, decyzja użytkownika 2026-10-16) — dzięki wspólnej logice jedna stała zmienia datę dostawy i numer w nowej fakturze, szkicu i „Wystaw podobną”

## Definicja ukończenia

- [x] Testy: `SimilarInvoiceTest` (13 — kopiowane pola, daty i numer w dniach 1–5 i 6+ (granica 5./6. dnia), zgodność z `newForm` dla kilku dni i przełomu roku, `refresh` nie zmienia kopii, waluta bez kursu — brak kursu roboczego, dzień odniesienia kursu z nowych dat jak w `newForm`, faktura zablokowana do pobrania kursu, dopisanie nabywcy PL / GR / US, `BrakID` po nazwie, rachunek spoza profilu, korekta i zaliczka, nabywca bez adresu); `InvoiceFormDefaultsTest` (+2 — `withDefaults`, `changeDeliveryDate`, `withSuggestedNumber`); `DefaultDatesTest` — granice 5./6. dnia, przełom roku, rok przestępny
- [x] Zaktualizowana dokumentacja / słownik
