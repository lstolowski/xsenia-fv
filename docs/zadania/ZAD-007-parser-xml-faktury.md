# ZAD-007. Parser XML FA(2)/FA(3) → model faktury, data dostawy

- User story: US-002, US-005
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Moduł `invoice` zamienia XML faktury na model do widoku szczegółów.

## Zakres

- [x] Model `Invoice` (moduł `invoice`, pakiet `model`): strony (`Party`, adres wieloliniowy), daty, pozycje (`InvoiceLine` z wartością brutto wyliczaną, gdy brak `P_11A`), sumy wg kategorii VAT (`VatSummaryEntry`, 13 kategorii `P_13_x`/`P_14_x`), płatność (terminy, forma, rachunki, zapłacono), adnotacje (MPP, odwrotne obciążenie, zwolnienie z podstawą…), dane korekty, `DodatkowyOpis`, stopka, `Podmiot3`
- [x] Data dostawy (`Invoice.deliveryDate`): `P_6` → `OkresFa/P_6_Do` → najpóźniejsza `P_6A` → `P_1`
- [x] `InvoiceXmlParser` (DOM `javax.xml` — działa na JVM i Androidzie): elementy dopasowywane po nazwie lokalnej (FA(2) i FA(3)), nieznane elementy ignorowane, brak wymaganych → `InvoiceParseException`; ochrona przed XXE (zakaz DOCTYPE, także gdy parser nie obsługuje odpowiedniej flagi)
- [x] Testy na 4 przykładowych fakturach z danymi zmyślonymi (`invoice/src/test/resources/invoices/`), **walidowanych oficjalnym XSD FA(2)/FA(3)** (`FaSchemaValidator`, schematy w `src/test/resources/xsd/`) — VAT, wiele stawek z `P_6A`, korekta z `OkresFa`, FA(2) od cen brutto
- [x] Test integracyjny: parser na prawdziwym XML z KSeF TEST (wygenerowanym przez Aplikację Podatnika) zgadza się z metadanymi KSeF (numer, data, NIP-y, netto/VAT/brutto)

## Definicja ukończenia

- [x] Testy (10 parsera + 5 walidacji XSD)
- [x] Sprawdzone na środowisku KSeF TEST
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja

## Uwagi

- Walidator XSD z JDK nie radzi sobie z `maxOccurs="10000"` w schemacie MF (limit bezpieczeństwa, a po jego zdjęciu brak pamięci). W testach duże limity (≥ 100) zamieniane są na `unbounded` przy wczytaniu schematu — struktura, kolejność i typy są walidowane w pełni. Ten sam walidator posłuży w ZAD-011 do sprawdzania generowanych faktur.
- `P_12` w FA(2): `0`, `np`; w FA(3): `0 KR`, `0 WDT`, `0 EX`, `np I`, `np II` — model przechowuje tekst, `vatRatePercent` wyciąga liczbę.
