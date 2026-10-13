# Faktury w walucie obcej — analiza (US-011)

Status: **zakończona 2026-10-13** — decyzje Q1–Q6 w [US-011](../user-stories/US-011-faktury-w-walucie-obcej.md), [ADR 0008](../adr/0008-kursy-walut-nbp.md)

Kontekst: użytkownicy wystawiający faktury za granicę już mają stawkę „np” (reverse charge, [ADR 0005](../adr/0005-stawki-vat-i-jednostki.md)). Brakuje wystawiania faktury w walucie obcej (EUR, USD, GBP) z kursem waluty.

## 1. Wymagania użytkownika (2026-10-13)

1. Serwis kursów walut używany w kilku miejscach aplikacji: „kurs waluty X w PLN z dnia D”. Kurs średni NBP (`mid`) z tabeli A, **4 miejsca po przecinku** — tak jak publikuje NBP (decyzja po rundzie 1; pierwotnie było 6).
2. Formularz „Nowa faktura”: lista „Waluta” — PLN (domyślnie), EUR, USD, GBP; w przyszłości więcej.
3. Kontrola, czy wybrany rachunek bankowy (pole `currency`) pasuje do waluty faktury.
4. Automatyczne pobranie kursu (gdy waluta ≠ PLN) według zasad z ustawy o VAT (sekcja 3); przy zapisie bez wysyłki — kurs roboczy, przy wysyłce do KSeF — kurs pobierany ponownie i aktualizowany w locie.

## 2. FA(3) — pola walutowe

Sprawdzone w `schemat_FA(3)_v1-0E.xsd` (kopia: `invoice/src/test/resources/xsd/FA3.xsd`):

| Pole | Miejsce | Typ | Znaczenie / użycie u nas |
|---|---|---|---|
| `KodWaluty` | `Fa` (wymagane) | `TKodWaluty` | Waluta faktury: `PLN`, `EUR`, `USD`, `GBP`. Dziś zawsze `PLN`. |
| `P_13_x`, `P_14_x`, `P_15`, `P_11`, `P_9A` | `Fa`, `FaWiersz` | `TKwotowy` | Kwoty netto, VAT, brutto — **w walucie faktury**. |
| `P_14_1W`, `P_14_2W`, `P_14_3W` | `Fa`, po `P_14_1`/`P_14_2`/`P_14_3` | `TKwotowy` | VAT stawek 23% / 8% / 5% **przeliczony na PLN** — wypełniane tylko, gdy faktura jest w walucie obcej. Dla 0% i „np” (`P_13_6_1`, `P_13_9`) brak VAT — pole nie istnieje. |
| `KursWaluty` | `FaWiersz` (opcjonalne) | `TIlosci` — do 6 miejsc po przecinku | Kurs użyty do przeliczenia VAT (dział VI ustawy). U nas: ten sam kurs w każdej pozycji, **4 miejsca** jak w tabeli NBP (`4.3775`). |
| `KursWalutyZ` | `Fa` | `TIlosci` | Tylko faktury zaliczkowe (art. 106b ust. 1 pkt 4) — **nie dotyczy**. |
| `DodatkowyOpis` | `Fa`, do 10000 razy | `TKluczWartosc` | Uwaga „Kurs waluty” z numerem i datą tabeli NBP (Q5); obok istniejącej „Uwagi: Reverse charge”. |
| `RachunekBankowy/NrRB` | `Platnosc` | `TNrRB` (10–34 znaki) | Rachunek z formularza; niezgodność waluty — tylko ostrzeżenie (Q2). |

Wnioski:

- Netto i brutto zostają w walucie obcej; KSeF nie wymaga ich przeliczenia na PLN. Na PLN przeliczany jest tylko VAT (`P_14_xW`).
- Faktura tylko ze stawką „np” lub 0% nie ma pól `P_14_xW` — kurs i tak wpisujemy w `KursWaluty` (wymóg użytkownika: kurs obowiązkowy).
- Schemat nie wymusza `KursWaluty` ani `P_14_xW` — testy muszą sprawdzać ich obecność.
- Parser (`InvoiceXmlParser`) już czyta `P_14_xW`; `KursWaluty` — jeszcze nie.

## 3. Którego kursu użyć — reguła

Z ustawy o VAT (art. 31a), przekazane przez użytkownika:

- **Scenariusz A** — faktura wystawiona w dniu powstania obowiązku podatkowego lub po nim: średni kurs NBP z **ostatniego dnia roboczego przed dniem powstania obowiązku podatkowego**.
- **Scenariusz B** — faktura wystawiona **przed** powstaniem obowiązku: średni kurs NBP z **ostatniego dnia roboczego przed dniem wystawienia**. W KSeF dniem wystawienia jest dzień przesłania do KSeF.

Dzień powstania obowiązku podatkowego = **data dostawy** z formularza (`P_6`, data wykonania usługi; przy rozliczeniu miesięcznym — ostatni dzień miesiąca, [DefaultDates](../../invoice/src/main/kotlin/pl/xsenia/fv/invoice/issue/DefaultDates.kt)).

Oba scenariusze sprowadzają się do jednej reguły:

```
dzień odniesienia = min(data dostawy, dzień wystawienia)
kurs              = tabela A NBP z ostatniego dnia roboczego przed dniem odniesienia
```

| Moment | Dzień wystawienia w regule | Kurs |
|---|---|---|
| Edycja / zapis bez wysyłki (szkic, faktura niewysłana) | data wystawienia z formularza (`P_1`) | **roboczy** — pokazany w formularzu, zapisany w szkicu |
| Podgląd przed wysyłką i wysyłka do KSeF | **dzień wysyłki** (dziś, Europe/Warsaw) | **ostateczny** — pokazany w podglądzie, sprawdzany ponownie tuż przed utworzeniem XML |

Przykłady (tabele NBP: 2026-10-08 czw., 2026-10-09 pt., 2026-10-12 pn.):

| Data dostawy | Wysyłka | Dzień odniesienia | Kurs z tabeli |
|---|---|---|---|
| 2026-09-30 | 2026-10-13 | 2026-09-30 (A) | 2026-09-29 |
| 2026-10-12 | 2026-10-13 | 2026-10-12 (A) | 2026-10-09 (piątek — weekend pominięty) |
| 2026-10-13 | 2026-10-13 | 2026-10-13 (A) | 2026-10-12 |
| 2026-10-20 | 2026-10-13 | 2026-10-13 (B) | 2026-10-12 |

`P_1` przy wysyłce się nie zmienia — podgląd już ostrzega, że faktura z wcześniejszą datą wystawienia jest w KSeF uznawana za wystawioną w dniu wysłania (`preview_issue_date_past`).

## 4. Źródło kursów — API NBP, tabela A

Wyłącznie NBP, tabela A (kursy średnie), waluty EUR, USD, GBP. Publiczne API bez klucza; to nie KSeF, więc zakaz DEMO/PROD go nie dotyczy (jak Biała lista, [ADR 0007](../adr/0007-biala-lista-mf.md)). Sprawdzone 2026-10-13:

```
GET https://api.nbp.pl/api/exchangerates/rates/a/{code}/{date}/?format=json
```

- np. `/rates/a/usd/2016-04-04/` → `{"table":"A","currency":"dolar amerykański","code":"USD","rates":[{"no":"064/A/NBP/2016","effectiveDate":"2016-04-04","mid":3.7254}]}`,
- bierzemy `mid` (4 miejsca po przecinku), numer tabeli `no` i `effectiveDate`,
- dzień bez tabeli (weekend, święto, dziś przed publikacją ok. 12:00) → `404` z treścią tekstową `404 NotFound - Not Found - Brak danych`,
- „ostatni dzień roboczy przed D”: zapytania o kolejne dni wstecz (D−1, D−2, …) aż do pierwszej odpowiedzi `200`; najdłuższa przerwa w tabelach to kilka dni (święta) — limit 10 dni wstecz, potem błąd,
- kursy z przeszłości się nie zmieniają → pamięć podręczna w profilu (także informacja „brak tabeli w dniu D” dla dni z przeszłości).

Odrzucone: EBC (kursy względem EUR — przeliczenia krzyżowe), zapytanie o zakres dat `/rates/a/{code}/{od}/{do}/` (mniej zapytań, ale użytkownik wskazał endpoint dzienny — przy pamięci podręcznej różnica pomijalna).

## 5. VAT w PLN

`P_14_xW` = `P_14_x × kurs`, zaokrąglone do grosza (HALF_UP), osobno dla każdej stawki 23/8/5 (decyzja Q6: a).

## 6. Wpływ na aplikację

| Obszar | Dziś | Zmiana |
|---|---|---|
| `SalesInvoiceDraft` | bez waluty | `currency` + `exchangeRate` (kurs, numer tabeli, data tabeli) |
| `InvoiceCalculator` | `CURRENCY = "PLN"` | VAT w PLN per stawka; PLN tylko jako domyślna |
| `Fa3XmlWriter` | `KodWaluty` = PLN | `KodWaluty`, `P_14_xW`, `KursWaluty` w każdej pozycji, uwaga „Kurs waluty” |
| `DraftValidator` | — | brak kursu przy walucie obcej (blokuje); rachunek w innej walucie (ostrzeżenie) |
| `InvoiceForm` (szkic, niewysłane) | bez waluty | `currency` (domyślnie PLN — stare szkice), kurs roboczy |
| Rachunki (`InvoicingConfig.invoiceBankAccounts`, braki z ZAD-022/025) | tylko PLN | wszystkie rachunki; brakiem jest tylko brak jakiegokolwiek rachunku |
| Formularz rachunku (ZAD-023) | waluta domyślnie PLN | lista walut PLN/EUR/USD/GBP |
| Podgląd, `InvoiceSendService` | XML ze szkicu | kurs ostateczny dla dnia wysyłki w podglądzie; ponowne sprawdzenie przed XML |
| Listy (`InvoiceListLogic`) | faktury niewysłane zawsze w PLN | waluta z formularza; sumy per waluta już są |
| Szczegóły faktury | waluta | kurs i VAT w PLN dla faktur w walucie |
| Nowy moduł | — | `exchangerates` (klient NBP), serwis `ExchangeRateService` w aplikacji |

Zależność: US-011 buduje na US-010 (rachunki w profilu, lista braków, formularz rachunku) — realizacja po ZAD-022…025.

## 7. Poza zakresem

- Nabywca zagraniczny (Podmiot2 bez polskiego NIP-u: `KodUE` + `NrVatUE` albo `NrID`, `KodKraju` ≠ PL) — osobna historyjka (US-012, decyzja Q1).
- Faktury zaliczkowe (`KursWalutyZ`), korekty, waluty inne niż EUR/USD/GBP, kurs EBC, ręczne wpisywanie kursu (Q3), przeliczanie netto/brutto na PLN do celów PIT/CIT (Q5), waluta domyślna per kontrahent.
