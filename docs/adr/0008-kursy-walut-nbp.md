# 0008. Faktury w walucie obcej i kursy walut z API NBP

- Status: Zaakceptowany
- Data: 2026-10-13

## Kontekst

- [US-011](../user-stories/US-011-faktury-w-walucie-obcej.md): faktura sprzedażowa w PLN, EUR, USD lub GBP; dla waluty obcej kurs jest obowiązkowy (`KursWaluty`), a VAT musi być wykazany w PLN (`P_14_xW`). Szczegóły pól i reguły — [analiza 04](../analiza/04-faktury-w-walucie-obcej.md).
- KSeF nie pobiera kursów — robi to program wystawiający fakturę.
- Ustawa o VAT: średni kurs z ostatniego dnia roboczego przed dniem powstania obowiązku podatkowego (scenariusz A) albo przed dniem wystawienia, gdy faktura jest wystawiona wcześniej (scenariusz B; w KSeF dzień wystawienia = dzień przesłania).
- Serwis kursów ma być używany w kilku miejscach aplikacji.

## Rozważane opcje

1. **API NBP, tabela A** — publiczne, bez klucza, JSON, kursy średnie w PLN (`mid`, 4 miejsca po przecinku).
2. EBC (`data-api.ecb.europa.eu`) — kursy względem EUR; PLN wymagałby przeliczeń krzyżowych i zaokrągleń.
3. Ręczne wpisywanie kursu — bez zależności od sieci, ale łatwo o pomyłkę i niezgodność z regułą.

## Decyzja

**Opcja 1 — wyłącznie NBP, tabela A**; bez kursu ręcznego (Q3) i bez EBC (decyzja użytkownika 2026-10-13).

### Klient i serwis

- Nowy moduł JVM `exchangerates` (pakiet `pl.xsenia.fv.exchangerates`), klient Ktor + kotlinx.serialization (jak `whitelist`), adres `https://api.nbp.pl` w obu wersjach aplikacji.
- Endpoint: `GET /api/exchangerates/rates/a/{code}/{date}/?format=json`, `code` = `eur` / `usd` / `gbp`; z odpowiedzi bierzemy `rates[0].mid`, `no`, `effectiveDate`.
- `NbpClient`:
  - `rateOn(currency, date)` → kurs z dnia `date` albo `NotPublished` (`404`, dzień bez tabeli),
  - `lastRateBefore(currency, date)` → `rateOn` dla `date − 1`, `date − 2`, … do pierwszego kursu, maks. 10 dni wstecz,
  - wynik: `ExchangeRate(currency, rate: BigDecimal, tableNumber, effectiveDate)` — `rate` z **4 miejscami po przecinku**, jak `mid` w tabeli NBP; błędy `Failure` (`Network`, `Server`); limit czasu 15 s.
- `ExchangeRateService` w aplikacji — jedyny punkt dostępu do kursów (formularz, podgląd, wysyłka, w przyszłości inne miejsca): pamięć podręczna w profilu dla dni z przeszłości (kursy i dni bez tabeli się nie zmieniają).
- Waluty: `InvoiceCurrency` = `PLN` (domyślna, bez kursu), `EUR`, `USD`, `GBP`. Kolejna waluta = nowa pozycja enuma (o ile jest w tabeli A).

### Reguła daty kursu

`ExchangeRateDateRule.referenceDate(deliveryDate, issueDay) = min(deliveryDate, issueDay)`; kurs = `lastRateBefore(currency, referenceDate)`.

- dzień obowiązku podatkowego = data dostawy (`P_6`),
- formularz / zapis bez wysyłki: `issueDay` = data wystawienia z formularza → **kurs roboczy**, zapisany w szkicu lub fakturze niewysłanej,
- podgląd i wysyłka: `issueDay` = dzień wysyłki (Europe/Warsaw) → **kurs ostateczny**, pokazany w podglądzie przed potwierdzeniem biometrią i sprawdzany ponownie tuż przed utworzeniem XML (zmieni się tylko po północy) — Q4.

### FA(3)

- `KodWaluty` = waluta faktury; kwoty `P_9A`, `P_11`, `P_13_x`, `P_14_x`, `P_15` w walucie faktury.
- Waluta obca:
  - `KursWaluty` w każdej `FaWiersz` (ten sam kurs, 4 miejsca: `4.3775`),
  - `P_14_1W`/`P_14_2W`/`P_14_3W` = `P_14_x × kurs`, zaokrąglone do grosza (HALF_UP) — dla stawek 23/8/5 obecnych na fakturze (Q6),
  - `DodatkowyOpis`: `Klucz` „Kurs waluty”, `Wartosc` „1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09” (Q5); bez netto/brutto w PLN.
- PLN: XML bez zmian względem dotychczasowego (bez `KursWaluty`, `P_14_xW` i uwagi o kursie).

### Rachunek a waluta (Q2)

- Na fakturze można wybrać każdy rachunek; rachunek w innej walucie niż faktura daje **ostrzeżenie** (nieblokujące) w formularzu i podglądzie.
- Po zmianie waluty, gdy wybrany rachunek nie pasuje, a istnieje rachunek w nowej walucie — wybierany jest pierwszy pasujący.
- Wymóg z US-010 „co najmniej jeden rachunek PLN” zmienia się na „co najmniej jeden rachunek”.

## Konsekwencje

- Wystawienie faktury w walucie obcej wymaga dostępu do NBP — jak sama wysyłka do KSeF; bez kursu wysyłka nie startuje, faktura zostaje szkicem lub niewysłaną (Q3). Zapis bez wysyłki działa offline (bez kursu roboczego).
- Kurs na fakturze może się różnić od roboczego, jeśli wysyłka jest innego dnia niż data wystawienia — podgląd pokazuje zmianę.
- Faktury w PLN — bez zmian w XML; testy regresji na dotychczasowych fixture'ach.
- Zadania: ZAD-026…ZAD-029.
