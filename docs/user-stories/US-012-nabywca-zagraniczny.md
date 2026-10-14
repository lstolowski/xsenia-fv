# US-012. Nabywca zagraniczny (bez polskiego NIP-u)

- Priorytet: MVP
- Status: W realizacji (ZAD-030…032 zrobione)
- Zgłoszone: 2026-10-14 (wydzielone z US-011, decyzja Q1)

## Historyjka

Jako **właściciel firmy wystawiający faktury za granicę** chcę **dodać kontrahenta z innego kraju (z numerem VAT UE lub zagranicznym identyfikatorem) i wystawić mu fakturę w KSeF**, aby **fakturować klientów zagranicznych tą samą aplikacją, z poprawnymi danymi nabywcy i stawką „np”**.

Analiza: [05-nabywca-zagraniczny.md](../analiza/05-nabywca-zagraniczny.md), decyzja: [ADR 0009](../adr/0009-nabywca-zagraniczny.md).

## Kryteria akceptacji

### Kontrahent

- [x] Formularz kontrahenta zaczyna się od pola „Kraj” (lista wszystkich krajów z wyszukiwaniem, domyślnie Polska).
- [x] Polska — NIP z pobieraniem z Białej listy, jak dziś.
- [x] Kraj UE — „Numer VAT UE” (bez prefiksu; prefiks pokazany z kraju, dla Grecji `EL`), sprawdzany formatem; dane wpisywane ręcznie.
- [x] Inny kraj — „Identyfikator podatkowy” (opcjonalny); dane wpisywane ręcznie.
- [x] Kraj i identyfikator nie zmieniają się po zapisie; ten sam kraj i numer drugi raz — błąd „Kontrahent z tym numerem już istnieje”.
- [x] Kontrahent ma opcjonalną „Domyślną walutę” (PLN, EUR, USD, GBP).
- [x] Kafelki kontrahentów pokazują kraj i numer VAT UE / identyfikator (polscy — NIP jak dziś).

### Faktura

- [x] Wybór kontrahenta z domyślną walutą ustawia walutę faktury.
- [x] Dla nabywcy zagranicznego nowe pozycje mają stawkę „np”; puste pozycje zmieniają się na „np” przy wyborze takiego nabywcy.
- [x] `Podmiot2`: numer VAT UE (`KodUE` + `NrVatUE`), inny identyfikator (`KodKraju` + `NrID`) albo `BrakID`; `Adres/KodKraju` = kraj kontrahenta.
- [x] „np” dla nabywcy z UE — `np II` (`P_13_9`), spoza UE — `np I` (`P_13_8`); zawsze z „odwrotnym obciążeniem” i uwagą „Reverse charge”.
- [x] Faktury dla nabywcy z UE i spoza UE (w EUR / USD) są przyjmowane przez KSeF TEST (dane zmyślone).

### Listy i szczegóły

- [ ] Listy, szczegóły i wyszukiwanie pokazują i znajdują numer VAT UE / identyfikator nabywcy (z metadanych KSeF i z faktur niewysłanych).

### Zgodność

- [x] Istniejący kontrahenci, szkic i faktury niewysłane działają po aktualizacji (kontrahenci → Polska, NIP).

## Uwagi / pytania

Decyzje z sesji pytań (2026-10-14):

- Q1 (c) — nabywcy z UE i spoza UE.
- Q2 (a) — kod „np” wybierany wg kraju nabywcy (UE → `np II`, spoza UE → `np I`), „odwrotne obciążenie” i „Reverse charge” w obu przypadkach; warto potwierdzić z księgowym.
- Q3 (b) — bez VIES, dane kontrahenta zagranicznego ręcznie.
- Q4 (a) — „Kraj” jako pierwsze pole formularza kontrahenta, reszta pól wg kraju.
- Q5 (a) — PDF / wizualizacja z kodem QR i udostępnianie — osobna historyjka US-013; do tego czasu fakturę dla nabywcy zagranicznego trzeba pobrać np. z Aplikacji Podatnika KSeF.
- Q6 (a) — domyślna waluta kontrahenta; stawka „np” dla nabywcy zagranicznego.

Poza zakresem: VIES, dostawa towarów (WDT, eksport), VAT-UE / JPK.

## Zadania

- ZAD-030 — model kontrahenta zagranicznego, migracja kontrahentów, szkicu i faktur niewysłanych
- ZAD-031 — FA(3): Podmiot2 z numerem VAT UE / innym identyfikatorem, „np I” / „np II”; wysyłka do KSeF TEST
- ZAD-032 — UI: formularz kontrahenta z krajem, domyślna waluta, wartości domyślne w formularzu faktury
- ZAD-033 — listy, szczegóły i wyszukiwanie: numer VAT UE / identyfikator nabywcy
