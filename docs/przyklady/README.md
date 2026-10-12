# Przykładowe pliki konfiguracyjne

Wzory plików z [ADR 0004](../adr/0004-kontrahenci-w-pliku-json.md). Skopiuj je do folderu na telefonie (np. `Dokumenty/xsenia-test`) i uzupełnij.

| Plik | Zawartość |
|---|---|
| [company.json](company.json) | moja firma (sprzedawca) |
| [banks.json](banks.json) | rachunki bankowe (lista wyboru na fakturze; tylko `PLN`) |
| [counterparties.json](counterparties.json) | kontrahenci (nabywcy) |

Wszystkie dane w przykładach są **zmyślone** (testowe NIP-y i rachunki). Pliki są sprawdzane testami modułu `invoice` — zmiana na niepoprawny format przerwie build.

## Zasady

- Pola wymagane: `name`, `nip`, `address.line1` (firma i kontrahenci); `name`, `account` (rachunki). Pozostałe opcjonalne.
- `nip` — 10 cyfr; dopuszczalne myślniki, spacje i prefiks `PL` (aplikacja je usunie). Sprawdzana suma kontrolna.
- `account` — 26 cyfr (NRB); dopuszczalne spacje i prefiks `PL`. Sprawdzana suma kontrolna.
- `currency` — kod ISO, domyślnie `PLN`; na fakturze dostępne tylko konta `PLN`.
- Limity długości jak w FA(3): nazwa i linie adresu do 512 znaków, nazwa banku do 256, e-mail do 255, telefon do 16.
- Ten sam NIP nie może wystąpić u dwóch kontrahentów (drugi wpis zostanie pominięty).
