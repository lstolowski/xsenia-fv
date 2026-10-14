# ZAD-031. FA(3): nabywca z numerem VAT UE lub innym identyfikatorem, „np I” / „np II”

- User story: US-012
- Status: Do zrobienia
- Szacunek: M

## Cel

Faktura dla nabywcy zagranicznego ma właściwe `Podmiot2` i kod stawki „np” i jest przyjmowana przez KSeF TEST ([ADR 0009](../adr/0009-nabywca-zagraniczny.md)).

## Zakres

- [ ] `Fa3XmlWriter`: `Podmiot2/DaneIdentyfikacyjne` — `NIP` | `KodUE` + `NrVatUE` | `KodKraju` + `NrID` | `BrakID = 1`; `Podmiot2/Adres/KodKraju` = kraj kontrahenta
- [ ] Kod „np”: nabywca z UE (poza PL) i polski → `np II` / `P_13_9`; spoza UE → `np I` / `P_13_8`; `P_18 = 1` i „Reverse charge” zawsze; `InvoiceCalculator` / suma stawek z kodem zależnym od nabywcy
- [ ] Parser: odczyt identyfikatora nabywcy (już czyta `NrVatUE`, `NrID`) — sprawdzenie w testach
- [ ] Test integracyjny (`-PsendInvoices`): faktura dla nabywcy z UE (EUR, „np II”) i spoza UE (USD, „np I”) — dwie faktury, dane zmyślone

## Definicja ukończenia

- [ ] Testy: XML zgodny ze schematem dla każdego wariantu identyfikatora, Grecja (`EL`), `np I` / `np II`, PL bez zmian
- [ ] Sprawdzone na środowisku KSeF TEST
- [ ] Zaktualizowana dokumentacja / słownik
