# 0004. Dane firmy, rachunki i kontrahenci w ręcznie edytowanych plikach JSON

- Status: Zaakceptowany
- Data: 2026-10-11

## Kontekst

- Użytkownik chce przechowywać dane kontrahentów (nabywców) w pliku JSON **edytowanym ręcznie** i wczytywanym przy każdym otwarciu aplikacji (C1). Wystawiając fakturę, wybiera kontrahenta z listy (C4).
- Tak samo mają być przechowywane dane mojej firmy (R6) oraz rachunki bankowe — w formularzu lista rozwijana z wyborem konta (R5).
- Nabywcy: wyłącznie polskie firmy z NIP.
- Pliki we wspólnym folderze, np. `Dokumenty/xsenia` (R7); nazwy pól po angielsku (R8).
- Od Androida 11 katalog `Android/data/<pakiet>` nie jest dostępny dla menedżerów plików (Samsung „Moje pliki” również), więc pliki nie mogą leżeć w prywatnym katalogu aplikacji.

## Rozważane opcje

1. Pliki w prywatnym katalogu aplikacji, wgrywane przez `adb push` — niewygodne, wymaga komputera.
2. **Pliki we wspólnym folderze wskazanym raz przez systemowy wybór folderu (Storage Access Framework, `ACTION_OPEN_DOCUMENT_TREE`)**; aplikacja zachowuje trwałe uprawnienie (`takePersistableUriPermission`) i czyta pliki przy każdym otwarciu. Edycja dowolnym edytorem tekstu na telefonie albo po skopiowaniu z komputera lub chmury.
3. Edycja danych w aplikacji (formularze) — wykracza poza życzenie użytkownika.

## Decyzja

**Opcja 2.** Folder (np. `Dokumenty/xsenia/`) zawiera trzy pliki:

### `company.json` — moja firma (sprzedawca, Podmiot1)

```json
{
  "company": {
    "name": "Jan Testowy Usługi Testowe",
    "nip": "1996845518",
    "address": {
      "line1": "ul. Testowa 1",
      "line2": "00-001 Testowo",
      "country": "Polska"
    },
    "email": "jan.testowy@example.com",
    "phone": "+48 000 000 000"
  }
}
```

### `banks.json` — rachunki bankowe (format podany przez użytkownika; dane w przykładzie zmyślone)

```json
{
  "banks": [
    { "name": "Konto testowe Toyota", "bank": "Toyota Bank", "account": "85216097583216850246002152", "currency": "PLN" },
    { "name": "Konto testowe Santander", "bank": "Santander Bank Polska", "account": "55109004081797992973253811", "currency": "PLN" }
  ]
}
```

### `counterparties.json` — kontrahenci (nabywcy, Podmiot2)

```json
{
  "counterparties": [
    {
      "name": "Testowa Sp. z o.o.",
      "nip": "6691034274",
      "address": {
        "line1": "ul. Testowa 1",
        "line2": "00-001 Testowo",
        "country": "Polska"
      },
      "email": "faktury@example.com"
    }
  ]
}
```

Zasady:

- Pola wymagane: `name`, `nip`, `address.line1` (company i counterparties); `name`, `account` (banks). Pozostałe opcjonalne (`email`, `phone`, `address.line2`, `bank`).
- `address.line1` / `address.line2` → `AdresL1` / `AdresL2` w FA(3).
- `address.country` jest informacyjne. Kod kraju w XML (`KodKraju`) to zawsze `PL` — nabywcy wyłącznie polscy (C1).
- `nip` — 10 cyfr z sumą kontrolną; akceptowane spacje, myślniki i prefiks `PL` (normalizowane).
- `account` — 26 cyfr (NRB), walidowany sumą kontrolną; akceptowane spacje i prefiks `PL` (normalizowane); na fakturze trafia do `Platnosc/RachunekBankowy/NrRB` (a `bank` do `NazwaBanku`). W formularzu wybór konta z listy, domyślnie pierwsze.
- `currency` — faktury wystawiamy tylko w PLN (C3). Konta o innej walucie są pomijane na liście wyboru.
- Walidacja przy wczytaniu: poprawność JSON, wymagane pola, suma kontrolna NIP i NRB. Błędy pokazywane czytelnie (plik, pozycja, pole); przeglądanie faktur działa dalej, a wystawienie faktury jest zablokowane do czasu poprawienia `company.json` / `banks.json`.
- Nazwa pliku i pola w JSON po angielsku (R8); wzory plików w [`docs/przyklady/`](../przyklady/README.md).

## Konsekwencje

- Brak edycji danych w aplikacji — zmiany tylko przez pliki; wczytywane przy każdym starcie (i przy powrocie do aplikacji z tła, jeśli plik się zmienił).
- Jeśli folder zostanie przeniesiony lub usunięty, aplikacja poprosi o ponowne wskazanie.
- Wersje `test` i `prod` aplikacji mogą wskazywać różne foldery (np. `Dokumenty/xsenia-test` z fikcyjnym NIP testowym).
