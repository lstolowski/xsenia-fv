# ZAD-027. Faktura w walucie obcej: model, VAT w PLN, XML FA(3)

- User story: US-011
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

Szkic faktury ma walutę i kurs; generator FA(3) zapisuje `KodWaluty`, `KursWaluty`, VAT w PLN (`P_14_xW`) i uwagę o kursie — faktura w EUR jest przyjmowana przez KSeF TEST ([ADR 0008](../adr/0008-kursy-walut-nbp.md)).

## Zakres

- [x] `SalesInvoiceDraft`: `currency: InvoiceCurrency = PLN`, `exchangeRate: ExchangeRate?` (wymagany dla waluty obcej); `effectiveExchangeRate` — kurs tylko dla waluty obcej i tylko tej samej waluty
- [x] `InvoiceCalculator`: `RateTotals.vatPln` dla stawek 23/8/5 przy walucie obcej = `vat × kurs`, HALF_UP do grosza, suma `InvoiceTotals.vatPln`; `totals(draft)`; stała `CURRENCY` usunięta (faktury niewysłane do ZAD-028 w `InvoiceCurrency.DEFAULT`)
- [x] `Fa3XmlWriter`: `KodWaluty`; po `P_14_x` — `P_14_xW`; `KursWaluty` (4 miejsca) w każdej `FaWiersz` po `P_12`; `DodatkowyOpis` „Kurs waluty” — „1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09” (po „Uwagi: Reverse charge”, jeśli jest); PLN — XML bez zmian
- [x] `DraftValidator`: `EXCHANGE_RATE_MISSING` (waluta obca bez kursu — blokuje), `BANK_ACCOUNT_CURRENCY_MISMATCH` (ostrzeżenie, `blocking = false`)
- [x] `InvoiceXmlParser`: odczyt `KursWaluty` (do szczegółów faktury)
- [x] Rachunki do faktury: `CompanyProfile.invoiceBankAccounts` — wszystkie rachunki; brak tylko przy braku jakiegokolwiek rachunku; `defaultBankAccount(waluta)` — pierwszy w walucie faktury, inaczej pierwszy; `INVOICE_CURRENCY` → `DEFAULT_CURRENCY` (waluta nowego rachunku); teksty „brak rachunku bankowego” i podpowiedź w formularzu rachunku bez „tylko PLN”; komunikaty w formularzu faktury: brak kursu, rachunek w innej walucie (ostrzeżenie)
- [x] Test integracyjny wysyłki (`-PsendInvoices`): faktura w EUR z pozycjami 23% i „np” na rachunek EUR (dane zmyślone z [rachunki.md](../przyklady/rachunki.md)), kurs pobierany z prawdziwego NBP wg reguły dla wysyłki dziś — jedna faktura; przy okazji poprawione importy testów integracyjnych `ksef` po ZAD-022 (`invoice.config` → `invoice.profile`)

## Definicja ukończenia

- [x] Testy: XML w EUR/USD/GBP zgodny ze schematem FA(3) (`FaSchemaValidator`), kolejność elementów, `P_14_xW` dla mieszanych stawek (przykład z [kursy-nbp.md](../przyklady/kursy-nbp.md)), brak `P_14_xW` dla samych „np”/0%, uwaga o kursie, PLN bez zmian (dotychczasowe testy), walidacja
- [x] Sprawdzone na środowisku KSeF TEST (2026-10-14): faktura w EUR przyjęta, XML w KSeF identyczny, kurs i `P_14_1W` odczytane z powrotem
- [x] Zaktualizowana dokumentacja / słownik
