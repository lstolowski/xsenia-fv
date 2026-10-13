# Testowe NIP-y

Zmyślone NIP-y z poprawną sumą kontrolną. W Białej liście MF **nie występują** (API zwraca `subject: null`).

| NIP | Użycie |
|---|---|
| `1996845518` | testowa „Moja firma” ([firma.md](firma.md)) |
| `6691034274` | kontrahent „Testowa Sp. z o.o.” ([kontrahenci.md](kontrahenci.md)) |
| `9945082500` | kontrahent „Lorem Ipsum Testowy S.A.” ([kontrahenci.md](kontrahenci.md)) |
| `9579102904` | wolny |
| `6973432790` | wolny |
| `4824062973` | wolny |
| `3563998415` | wolny |
| `8771617926` | wolny |

NIP kontekstu KSeF TEST użytkownika (i drugiego kontekstu — dostawcy faktur zakupowych) jest tylko w `secrets/ksef-test.properties` — nie przepisuj go do repozytorium.

Niepoprawne NIP-y do testów walidacji: `1234567890` (zła suma kontrolna), `12345` (zła długość).
