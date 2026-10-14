# 0009. Nabywca zagraniczny: kraj i identyfikator kontrahenta, „np I” / „np II”

- Status: Zaakceptowany
- Data: 2026-10-14

## Kontekst

- [US-012](../user-stories/US-012-nabywca-zagraniczny.md): faktury dla firm z innych krajów UE (numer VAT UE) i spoza UE (np. GB, US, CH). Dziś kontrahent musi mieć polski NIP, identyfikuje go NIP (niezmienny), a w XML zawsze jest `Podmiot2/NIP` i `KodKraju = PL` ([ADR 0006](0006-dane-firmy-w-aplikacji.md)).
- FA(3) `TPodmiot2` ma do wyboru: `NIP` | `KodUE` + `NrVatUE` | `KodKraju` (opcjonalny) + `NrID` | `BrakID` — [analiza 05](../analiza/05-nabywca-zagraniczny.md).
- Stawka „np” to dziś zawsze `np II` (usługi dla podatników z UE, art. 100 ust. 1 pkt 4). Usługi dla firm spoza UE to `np I` (`P_13_8`).
- Decyzje użytkownika (2026-10-14): Q1 c — nabywcy z UE i spoza UE; Q2 a — kod „np” wg kraju nabywcy, „odwrotne obciążenie” i „Reverse charge” w obu przypadkach; Q3 b — bez VIES, dane ręcznie; Q4 a — kraj jako pierwsze pole formularza; Q5 a — PDF / udostępnianie osobno (US-013); Q6 a — domyślna waluta kontrahenta i stawka „np” dla nabywcy zagranicznego.

## Rozważane opcje

1. **Jeden model kontrahenta z krajem i rodzajem identyfikatora**, wewnętrzny identyfikator niezależny od NIP-u.
2. Osobna lista „kontrahenci zagraniczni” — dwa formularze, dwie listy, dwa przypadki w fakturze.
3. NIP jako pole tekstowe na dowolny identyfikator — bez kraju, bez rozróżnienia UE / spoza UE w XML.

## Decyzja

**Opcja 1.**

### Model kontrahenta

- `Counterparty`: `id` (wewnętrzny, niezmienny), `name`, `countryCode` (ISO 3166, jak `KodKraju` w FA(3); domyślnie `PL`), `taxId` — rodzaj i numer, `address` (linie 1–2), `email`, `defaultCurrency` (opcjonalna).
- Rodzaj identyfikatora wynika z kraju:
  - `PL` → `NIP` (wymagany, suma kontrolna, Biała lista — jak dziś),
  - kraj UE → `VAT_UE`: numer bez prefiksu (wzorzec XSD `(\d|[A-Z]|\+|\*){1,12}`), prefiks `KodUE` z kraju — **Grecja `EL`** (adres `GR`); bez `XI` (Irlandia Płn. — prefiks tylko dla towarów, przy usługach nabywca to GB),
  - inny kraj → `OTHER` (`NrID`, opcjonalny); bez numeru → `BrakID`.
- Kraj i identyfikator są niezmienne po zapisie (jak dziś NIP); unikalność: kraj + numer (kontrahent bez numeru — bez sprawdzania).
- Migracja: istniejący kontrahenci → `countryCode = PL`, `NIP`; `id` = NIP. Szkic i faktury niewysłane wskazują kontrahenta przez `id`, zapisywane w JSON pod dotychczasową nazwą `counterpartyNip` — stare dane działają bez przepisywania.
- Bez VIES (Q3) — numer VAT UE sprawdzany tylko formatem; dane wpisywane ręcznie.

### Faktura (FA(3))

- `Podmiot2/DaneIdentyfikacyjne` wg rodzaju: `NIP` | `KodUE` + `NrVatUE` | `KodKraju` + `NrID` | `BrakID = 1`; `Podmiot2/Adres/KodKraju` = kraj kontrahenta.
- Stawka „np” (`VatRate.NOT_TAXABLE`): nabywca z UE (poza PL) → `np II` (`P_13_9`); nabywca spoza UE → `np I` (`P_13_8`); polski nabywca → `np II` jak dziś. W każdym przypadku `P_18 = 1` i uwaga „Reverse charge” (Q2 a — do potwierdzenia z księgowym; zmiana na brak adnotacji dla spoza UE to jedna reguła).

### Formularz faktury (Q6)

- Wybór kontrahenta z `defaultCurrency` ustawia walutę faktury (jak zmiana waluty w pozycji, [ADR 0008](0008-kursy-walut-nbp.md)).
- Nabywca zagraniczny: nowe pozycje dostają stawkę „np”; po wyborze nabywcy zagranicznego pozycje jeszcze puste (bez nazwy i ceny) też zmieniają się na „np”. Stawkę można zmienić.

## Konsekwencje

- Kontrahenci polscy działają jak dotąd; jeden formularz i jedna lista dla wszystkich.
- Zmiana klucza kontrahenta wymaga migracji danych profilu, szkicu i faktur niewysłanych — z testami na danych sprzed zmiany.
- Faktury dla nabywców bez polskiego NIP-u nie dotrą do nich przez KSeF — do czasu US-013 (PDF z kodem QR) trzeba je pobrać np. z Aplikacji Podatnika KSeF.
- Zadania: ZAD-030…ZAD-033.
