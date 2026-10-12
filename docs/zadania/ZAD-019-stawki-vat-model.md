# ZAD-019. Stawki VAT w modelu faktury: kalkulator, XML FA(3), reverse charge

- User story: US-009
- Status: Zrobione
- Szacunek: M

## Cel

Szkic faktury (`SalesInvoiceDraft`) obsługuje stawki 23%, 8%, 5%, 0% i „np” (niepodlegające, reverse charge) w każdej pozycji. Sumy i XML FA(3) są zgodne z [ADR 0005](../adr/0005-stawki-vat-i-jednostki.md).

## Zakres

- [x] `VatRate` (moduł `invoice`): `RATE_23`, `RATE_8`, `RATE_5`, `RATE_0` (`0 KR`), `NOT_TAXABLE` (`np II`) z kodem `P_12` i procentem
- [x] `DraftLine.vatRate` (domyślnie 23%)
- [x] `InvoiceCalculator`: VAT pozycji wg jej stawki; sumy per stawka (VAT od sumy netto stawki) i sumy faktury
- [x] `Fa3XmlWriter`: `P_12` wg stawki; `P_13_1`/`P_14_1`, `P_13_2`/`P_14_2`, `P_13_3`/`P_14_3`, `P_13_6_1`, `P_13_9` dla użytych stawek; przy pozycji „np”: `P_18 = 1` i `DodatkowyOpis` „Uwagi: Reverse charge”
- [x] `LineForm` (aplikacja): stawka zapisywana w szkicu; stare szkice → 23%
- [x] Sumy faktur niewysłanych na liście (`InvoiceListLogic`) per stawka
- [x] Brutto pozycji bez VAT (`np`, `zw`, `oo`) w szczegółach = netto (`InvoiceLine.displayGrossValue`)

## Definicja ukończenia

- [x] Testy: kalkulator (mieszane stawki, zaokrąglenia), XML zgodny z XSD FA(3) i odczytany parserem (kategorie, adnotacja odwrotnego obciążenia, uwagi)
- [x] Sprawdzone na środowisku KSeF TEST: jedna faktura z pozycjami 23%, 8%, 5%, 0% i „np” przyjęta
- [x] Zaktualizowana dokumentacja / słownik
