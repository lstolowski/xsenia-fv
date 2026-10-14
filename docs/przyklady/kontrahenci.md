# Testowi kontrahenci

Nabywcy (Podmiot2) do testów, fixture'ów i wpisania na emulatorze.

| Nazwa (`name`) | NIP (`nip`) | Adres, linia 1 | Adres, linia 2 | Kraj | E-mail (`email`) |
|---|---|---|---|---|---|
| Testowa Sp. z o.o. | `6691034274` | ul. Testowa 6 | 00-002 Testowo | Polska | faktury@example.com |
| Lorem Ipsum Testowy S.A. | `9945082500` | ul. Dolor Sit Amet 12/3 | 00-003 Testowo | — | — |
| Testowy Dolor Sp. j. | `9579102904` | ul. Testowa 8 | 00-004 Testowo | Polska | biuro@example.com |

## Kontrahenci zagraniczni (US-012)

Numery VAT UE sprawdzone 2026-10-14 w VIES jako nieistniejące (`isValid: false`); identyfikatory spoza UE celowo niepoprawne.

| Nazwa | Kraj (`countryCode`) | Identyfikator (`taxNumber`) | Na fakturze | Adres, linia 1 | Adres, linia 2 | Domyślna waluta |
|---|---|---|---|---|---|---|
| Lorem Testowy GmbH | `DE` | `999999990` (VAT UE) | `KodUE` DE, `NrVatUE` 999999990 | Teststraße 1 | 00000 Testdorf | EUR |
| Dolor Testowy A.E. | `GR` | `000000000` (VAT UE, prefiks `EL`) | `KodUE` EL, `NrVatUE` 000000000 | Odos Test 1 | 00000 Testopoli | EUR |
| Lorem Test Inc. | `US` | `00-0000000` (EIN) | `KodKraju` US, `NrID` 00-0000000 | 1 Test Street | Testville, TS 00000 | USD |
| Ipsum Test Ltd | `GB` | — | `BrakID` | 1 Test Lane | Testbury TE1 1ST | GBP |

Przypadki do testów walidacji kontrahentów zagranicznych: `DE` bez numeru (wymagany), `9999999900000` (za długi), `ab/12` (niedozwolone znaki), ten sam numer drugi raz, kraj `ZZ` (nieznany), identyfikator spoza UE dłuższy niż 50 znaków.

Przypadki do testów walidacji: brak nazwy, brak `address.line1`, NIP `1234567890` (zła suma kontrolna), ten sam NIP drugi raz (duplikat), nazwa dłuższa niż 512 znaków.
