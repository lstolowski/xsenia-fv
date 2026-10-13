# Testowi kontrahenci

Nabywcy (Podmiot2) do testów, fixture'ów i wpisania na emulatorze.

| Nazwa (`name`) | NIP (`nip`) | Adres, linia 1 | Adres, linia 2 | Kraj | E-mail (`email`) |
|---|---|---|---|---|---|
| Testowa Sp. z o.o. | `6691034274` | ul. Testowa 6 | 00-002 Testowo | Polska | faktury@example.com |
| Lorem Ipsum Testowy S.A. | `9945082500` | ul. Dolor Sit Amet 12/3 | 00-003 Testowo | — | — |
| Testowy Dolor Sp. j. | `9579102904` | ul. Testowa 8 | 00-004 Testowo | Polska | biuro@example.com |

Przypadki do testów walidacji: brak nazwy, brak `address.line1`, NIP `1234567890` (zła suma kontrolna), ten sam NIP drugi raz (duplikat), nazwa dłuższa niż 512 znaków.
