# ZAD-036. BIC (SWIFT) rachunku: model, formularz, XML FA(3), PDF, ostrzeżenie

- User story: US-014
- Status: Zrobione (2026-10-14)
- Szacunek: S

## Cel

Rachunek bankowy mojej firmy ma opcjonalny BIC (SWIFT), który trafia do faktury (XML, PDF, szczegóły); przy nabywcy zagranicznym bez BIC podgląd ostrzega przed wysyłką.

## Zakres

- [x] `BankAccount.swift` (JSON `swift`, opcjonalne — stare pliki wczytują się bez zmian); `BankAccountInput.swift`, `ProfileField.SWIFT`, `FieldIssueType.INVALID_SWIFT`
- [x] `ProfileValidator.bankAccount`: BIC bez spacji, wielkimi literami, wzorzec FA(3) `SWIFT_Type` (8 albo 11 znaków)
- [x] Formularz rachunku: pole „BIC (SWIFT)” pod „Bank”, klawiatura z wielkimi literami, podpowiedź, komunikat błędu
- [x] `Fa3XmlWriter`: `<SWIFT>` po `<NrRB>` w `RachunekBankowy` (sprawdzone ze schematem `schemat_FA(3)_v1-0E.xsd`)
- [x] PDF (`InvoicePdfLayout`): „BIC (SWIFT): …” pod numerem rachunku, przed bankiem; szczegóły faktury — osobny wiersz pod rachunkiem
- [x] `SalesInvoiceDraft.swiftMissingForForeignBuyer`; podgląd (`NewInvoiceViewModel.previewSwiftMissing`) — żółta ramka z ikoną ostrzeżenia nad „Wyślij”

## Definicja ukończenia

- [x] Testy: walidacja (normalizacja, błędne formaty), zapis i odczyt w profilu, XML zgodny ze schematem FA(3) z `SWIFT` i bez, PDF (kolejność wierszy), warunek ostrzeżenia (nabywca PL / DE / US)
- [x] Zaktualizowana dokumentacja / słownik
