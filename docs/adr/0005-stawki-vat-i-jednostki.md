# 0005. Stawki VAT i jednostki w pozycjach faktury; układ formularza nowej faktury

> ⚠️ **Częściowo nieaktualne** (od 2026-10-13): wzmianki o plikach `company.json` i `counterparties.json` — dane firmy i kontrahentów są w aplikacji ([ADR 0006](0006-dane-firmy-w-aplikacji.md)). Decyzje o stawkach VAT, jednostkach i układzie formularza obowiązują.

- Status: Zaakceptowany
- Data: 2026-10-12
- Zastępuje decyzję C2 („stawka VAT zawsze 23%”) i częściowo R10 („bez uwag”) z [warsztatu](../analiza/03-pytania-warsztat.md)
- Zmieniony 2026-10-12: stawka „nd” (zwolnienie `zw`) zastąpiona stawką **„np”** (niepodlegające, reverse charge)

## Kontekst

- Dotąd każda wystawiana faktura miała stałą stawkę 23% (C2), a jednostka była polem tekstowym z domyślnym „szt.”.
- Użytkownik potrzebuje wybierać stawkę VAT: **23%, 8%, 5%, 0% oraz „np”**. „np” to stawka dla faktur **reverse charge** (VAT rozlicza nabywca), a nie stawka 0%. Wcześniej nazwana przez użytkownika „nd” — nazwa ujednolicona na „np” (2026-10-12).
- Jednostka ma być wybierana z listy: **szt., dni, godz., usł.**, domyślnie **godz.**
- Schemat FA(3) (`TStawkaPodatku`, pole `P_12`) ma dwa warianty „np”: `np I` (poza terytorium kraju, bez art. 100 ust. 1 pkt 4) i `np II` (usługi z art. 100 ust. 1 pkt 4, czyli art. 28b — usługi dla podatnika z innego kraju UE). Inne wartości to m.in. `23`, `8`, `5`, `0 KR` / `0 WDT` / `0 EX`, `zw`, `oo`.
- Przy reverse charge faktura musi mieć wyrazy „odwrotne obciążenie” — w FA(3) adnotacja `P_18 = 1`.
- Użytkownik chce na takiej fakturze uwagę **„Reverse charge”**.
- Nabywcy w `counterparties.json` mają polski NIP (C1); 0% dotyczy sprzedaży krajowej.
- Zmiany w układzie formularza zgłoszone razem z powyższymi: numer faktury nad datą wystawienia, rachunek bankowy pod pozycjami.

## Rozważane opcje

Zapis stawki „np” w FA(3):

1. **`np II`** — usługi z art. 100 ust. 1 pkt 4 (art. 28b) dla podatnika z UE; suma w `P_13_9`.
2. `np I` — dostawy/usługi poza terytorium kraju z wyłączeniem art. 100 ust. 1 pkt 4 (np. dla firm spoza UE); suma w `P_13_8`.
3. (pierwotnie) `zw` — zwolnienie z podstawą prawną w `P_19A`. Odrzucone 2026-10-12: użytkownikowi chodziło o „np”, a nie o zwolnienie.

Zapis uwagi „Reverse charge”:

1. **`DodatkowyOpis`** (klucz–wartość): `Klucz` = „Uwagi”, `Wartosc` = „Reverse charge”. Aplikacja już wyświetla te pola w szczegółach.
2. `Stopka/Informacje/StopkaFaktury` — tekst w stopce.

Zakres stawki:

1. **Stawka w każdej pozycji** (`P_12` jest polem wiersza `FaWiersz`). Sumy osobno dla każdej stawki.
2. Jedna stawka dla całej faktury. Prostsze w UI, ale nie da się łączyć stawek.

## Decyzja

### Stawki VAT

- Stawka jest wybierana **w każdej pozycji** z listy rozwijanej, **domyślnie 23%**.
- Mapowanie na FA(3):

| UI | `VatRate` (kod) | `P_12` | Suma netto | Suma VAT |
|---|---|---|---|---|
| 23% | `RATE_23` | `23` | `P_13_1` | `P_14_1` |
| 8% | `RATE_8` | `8` | `P_13_2` | `P_14_2` |
| 5% | `RATE_5` | `5` | `P_13_3` | `P_14_3` |
| 0% | `RATE_0` | `0 KR` | `P_13_6_1` | — |
| np | `NOT_TAXABLE` | `np II` | `P_13_9` | — |

- **„np” = `np II`** (decyzja użytkownika 2026-10-12). Jeśli faktura ma choć jedną pozycję „np”:
  - `Adnotacje/P_18 = 1` (odwrotne obciążenie); w przeciwnym razie `P_18 = 2`, jak dotąd,
  - `DodatkowyOpis`: `Klucz` = „Uwagi”, `Wartosc` = „Reverse charge” (raz na fakturę, przed pozycjami).
- Zwolnienie z VAT (`zw`, `P_19`) nie jest używane: zawsze `P_19N = 1`. Bez pola `vatExemptionBasis` w `company.json`.
- 0% to zawsze `0 KR` (sprzedaż krajowa). `0 WDT` i `0 EX` nie występują, bo nabywcy są polscy (C1).
- VAT liczony **od sumy netto każdej stawki** (jak dotąd dla 23%, teraz osobno dla każdej grupy), zaokrąglenie do grosza „half up”. `P_15` = suma netto wszystkich stawek + suma VAT. Brutto pozycji jest informacyjne (netto pozycji + VAT pozycji wg jej stawki).
- W formularzu „np”; podgląd i szczegóły faktury pokazują „np.” i kategorię „Niepodlegające (art. 100)” oraz adnotację „Odwrotne obciążenie” i „Uwagi: Reverse charge”. Brutto pozycji „np” = netto (sprzedawca nie nalicza VAT).

### Jednostki

- Jednostka (`P_8A`) wybierana z listy: **`szt.`, `dni`, `godz.`, `usł.`**, domyślnie **`godz.`**
- Zapisane wcześniej szkice i faktury niewysłane z inną jednostką zachowują ją (pokazywana na liście jako dodatkowa opcja), dopóki użytkownik jej nie zmieni.

### Układ formularza nowej faktury (od góry)

1. Kontrahent
2. **Daty i numer**: **numer faktury**, data wystawienia, data dostawy
3. Pozycje (nazwa, ilość, **jednostka**, cena jedn. netto, **stawka VAT**) i przycisk „Dodaj pozycję”
4. **Rachunek bankowy**
5. Informacja o płatności (termin 21 dni), sumy, „Podgląd”

## Konsekwencje

- Model: `DraftLine` dostaje stawkę (`VatRate`), `InvoiceCalculator` liczy sumy per stawka, `Fa3XmlWriter` zapisuje pola `P_13_x` / `P_14_x` dla użytych stawek, a przy „np” — `P_18 = 1` i `DodatkowyOpis` „Uwagi: Reverse charge”.
- Formularz (`LineForm`) zapisuje stawkę i jednostkę. Szkice i faktury niewysłane zapisane przed zmianą wczytują się ze stawką 23% i swoją dotychczasową jednostką.
- Sumy faktur niewysłanych na liście są liczone tym samym kalkulatorem, czyli też per stawka.
- `np II` dotyczy zwykle nabywców z innych krajów UE, identyfikowanych numerem VAT UE (`KodUE` + `NrVatUE` w `Podmiot2`), a `counterparties.json` przyjmuje tylko polski NIP (C1). Obsługa nabywców zagranicznych wymaga osobnej decyzji (zmiana ADR 0004).
- Faktura z pozycjami 23%, 8%, 5%, 0% i „np” przyjęta przez KSeF TEST (2026-10-12).
- Zadania: [ZAD-019](../zadania/ZAD-019-stawki-vat-model.md) (model, kalkulator, XML), [ZAD-020](../zadania/ZAD-020-formularz-stawki-jednostki-uklad.md) (UI formularza). Historyjka: [US-009](../user-stories/US-009-stawki-vat-jednostki.md).
