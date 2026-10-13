# Przykładowe odpowiedzi API kursów NBP

Do mocków klienta NBP (ZAD-026, [ADR 0008](../adr/0008-kursy-walut-nbp.md)). Struktura **odwzorowuje prawdziwe odpowiedzi** `GET https://api.nbp.pl/api/exchangerates/rates/a/{code}/{date}/?format=json` (tabela A, waluty `eur`, `usd`, `gbp`) sprawdzone 2026-10-13. Kursy walut to dane publiczne (nie dane użytkownika), ale w testach używaj wartości z tego pliku — nie pobieraj ich na bieżąco. Bierzemy pole `mid` (4 miejsca po przecinku).

Kalendarz w przykładach: 2026-10-09 — piątek, 2026-10-10/11 — weekend, 2026-10-12 — poniedziałek.

## 1. Kurs z dnia — `rateOn(EUR, 2026-10-12)`

`GET /api/exchangerates/rates/a/eur/2026-10-12/?format=json` → `200`

```json
{"table":"A","currency":"euro","code":"EUR","rates":[{"no":"198/A/NBP/2026","effectiveDate":"2026-10-12","mid":4.3889}]}
```

Wynik: `ExchangeRate(EUR, 4.3889, "198/A/NBP/2026", 2026-10-12)`.

## 2. Dzień bez tabeli — `rateOn(EUR, 2026-10-11)` (niedziela)

`GET /api/exchangerates/rates/a/eur/2026-10-11/?format=json` → `404`, treść (tekst, nie JSON):

```
404 NotFound - Not Found - Brak danych
```

Wynik: `NotPublished`.

## 3. Ostatni dzień roboczy przed — `lastRateBefore(EUR, 2026-10-12)`

Kolejne zapytania wstecz od dnia poprzedniego:

| Zapytanie | Odpowiedź |
|---|---|
| `/rates/a/eur/2026-10-11/` | `404` (niedziela) |
| `/rates/a/eur/2026-10-10/` | `404` (sobota) |
| `/rates/a/eur/2026-10-09/` | `200` — poniżej |

```json
{"table":"A","currency":"euro","code":"EUR","rates":[{"no":"197/A/NBP/2026","effectiveDate":"2026-10-09","mid":4.3775}]}
```

Wynik: `ExchangeRate(EUR, 4.3775, "197/A/NBP/2026", 2026-10-09)`.

## 4. USD i GBP — kurs z 2026-10-09

```json
{"table":"A","currency":"dolar amerykański","code":"USD","rates":[{"no":"197/A/NBP/2026","effectiveDate":"2026-10-09","mid":3.9022}]}
```

```json
{"table":"A","currency":"funt szterling","code":"GBP","rates":[{"no":"197/A/NBP/2026","effectiveDate":"2026-10-09","mid":5.1655}]}
```

Przykład z dokumentacji użytkownika (stały, do testu integracyjnego): `/rates/a/usd/2016-04-04/` → `"no":"064/A/NBP/2016"`, `"mid":3.7254`.

## 5. Błędy

- `5xx` lub odpowiedź nie-JSON przy `200` → `Failure.Server`,
- brak sieci / przekroczenie czasu 15 s → `Failure.Network`,
- 10 kolejnych dni bez tabeli w `lastRateBefore` → `Failure` (nie powinno wystąpić).

## 6. Faktura w EUR — wyliczenie (do testów XML)

Pozycje: 10 godz. × 100,00 EUR, 23% oraz 1 usł. × 500,00 EUR, „np”. Kurs 4,3775 (tabela 197/A/NBP/2026 z 2026-10-09).

| Pole | Wartość |
|---|---|
| `KodWaluty` | `EUR` |
| `P_13_1` / `P_14_1` | `1000.00` / `230.00` |
| `P_14_1W` | `1006.83` (230,00 × 4,3775 = 1006,825 → HALF_UP) |
| `P_13_9` | `500.00` |
| `P_15` | `1730.00` |
| `KursWaluty` (każda pozycja) | `4.3775` |
| `DodatkowyOpis` | `Uwagi` — `Reverse charge`; `Kurs waluty` — `1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09` |
