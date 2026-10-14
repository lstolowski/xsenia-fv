# ZAD-031. FA(3): nabywca z numerem VAT UE lub innym identyfikatorem, „np I” / „np II”

- User story: US-012
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

Faktura dla nabywcy zagranicznego ma właściwe `Podmiot2` i kod stawki „np” i jest przyjmowana przez KSeF TEST ([ADR 0009](../adr/0009-nabywca-zagraniczny.md)).

## Zakres

- [x] `Fa3XmlWriter`: `Podmiot2/DaneIdentyfikacyjne` — `NIP` | `KodUE` + `NrVatUE` | `KodKraju` + `NrID` | `BrakID = 1`; `Podmiot2/Adres/KodKraju` = kraj kontrahenta
- [x] Kod „np”: `VatRate.codeFor(nabywca)` — nabywca z UE (poza PL) i polski → `np II` / `P_13_9`; spoza UE (`Counterparty.isOutsideEu`) → `np I` / `P_13_8`; `P_18 = 1` i „Reverse charge” zawsze; kwoty bez zmian (stawka ta sama, zmienia się tylko kod i pole sumy)
- [x] Parser: odczyt identyfikatora nabywcy (już czytał `KodUE` + `NrVatUE`, `KodKraju` + `NrID`, `P_13_8`) — sprawdzony w testach
- [x] Test integracyjny (`-PsendInvoices`): faktura dla nabywcy z UE (DE, EUR, „np II”) i spoza UE (US, USD, „np I”), kurs z NBP — dwie faktury, dane zmyślone

## Definicja ukończenia

- [x] Testy: XML zgodny ze schematem dla każdego wariantu identyfikatora (DE, GR z `EL`, US z `NrID`, GB z `BrakID`), `np I` / `np II`, PL bez zmian; każdy kraj z listy jest w słowniku `TKodKraju`, każdy kraj UE ma prefiks z `TKodyKrajowUE`
- [x] Sprawdzone na środowisku KSeF TEST (2026-10-14): obie faktury przyjęte z UPO, XML w KSeF identyczny, identyfikator nabywcy i kod „np” odczytane z powrotem
- [x] Zaktualizowana dokumentacja / słownik
