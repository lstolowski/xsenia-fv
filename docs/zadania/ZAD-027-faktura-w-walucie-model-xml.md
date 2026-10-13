# ZAD-027. Faktura w walucie obcej: model, VAT w PLN, XML FA(3)

- User story: US-011
- Status: Do zrobienia
- Szacunek: M

## Cel

Szkic faktury ma walutę i kurs; generator FA(3) zapisuje `KodWaluty`, `KursWaluty`, VAT w PLN (`P_14_xW`) i uwagę o kursie — faktura w EUR jest przyjmowana przez KSeF TEST ([ADR 0008](../adr/0008-kursy-walut-nbp.md)).

## Zakres

- [ ] `SalesInvoiceDraft`: `currency: InvoiceCurrency = PLN`, `exchangeRate: ExchangeRate?` (wymagany dla waluty obcej)
- [ ] `InvoiceCalculator`: `RateTotals.vatPln` dla stawek 23/8/5 przy walucie obcej = `vat × kurs`, HALF_UP do grosza; stała `CURRENCY` zastąpiona walutą szkicu
- [ ] `Fa3XmlWriter`: `KodWaluty`; po `P_14_x` — `P_14_xW`; `KursWaluty` (4 miejsca) w każdej `FaWiersz` po `P_12`; `DodatkowyOpis` „Kurs waluty” — „1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09” (po „Uwagi: Reverse charge”, jeśli jest); PLN — XML bez zmian
- [ ] `DraftValidator`: `EXCHANGE_RATE_MISSING` (waluta obca bez kursu — blokuje), `BANK_ACCOUNT_CURRENCY_MISMATCH` (ostrzeżenie, `blocking = false`)
- [ ] `InvoiceXmlParser`: odczyt `KursWaluty` (do szczegółów faktury)
- [ ] Rachunki do faktury: wszystkie rachunki zamiast filtra PLN (odpowiednik `InvoicingConfig` z ZAD-022); brak tylko przy braku jakiegokolwiek rachunku
- [ ] Test integracyjny wysyłki (`-PsendInvoices`): faktura w EUR z pozycjami 23% i „np” na rachunek EUR (dane zmyślone z [rachunki.md](../przyklady/rachunki.md)) — jedna faktura

## Definicja ukończenia

- [ ] Testy: XML w EUR/USD/GBP zgodny ze schematem FA(3) (`FaSchemaValidator`), kolejność elementów, `P_14_xW` dla mieszanych stawek (przykład z [kursy-nbp.md](../przyklady/kursy-nbp.md)), brak `P_14_xW` dla samych „np”/0%, uwaga o kursie, PLN bez zmian (dotychczasowe testy), walidacja
- [ ] Sprawdzone na środowisku KSeF TEST: faktura w EUR przyjęta
- [ ] Zaktualizowana dokumentacja / słownik
