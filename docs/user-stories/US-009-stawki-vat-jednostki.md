# US-009. Stawki VAT, jednostki z listy i nowy układ formularza faktury

- Priorytet: MVP
- Status: Zrobiona (2026-10-12)
- Zgłoszone: 2026-10-12

> Od [US-010](US-010-dane-firmy-w-aplikacji.md) dane mojej firmy, rachunki i kontrahenci są wprowadzane w aplikacji, a nie w plikach JSON ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)). Treść poniżej opisuje stan z chwili realizacji.

## Historyjka

Jako **właściciel firmy** chcę **wybierać stawkę VAT i jednostkę każdej pozycji z listy oraz mieć numer faktury i rachunek w wygodniejszych miejscach formularza**, aby **wystawiać też faktury z obniżonym VAT lub bez VAT i szybciej wypełniać formularz**.

## Kryteria akceptacji

- [x] Każda pozycja ma listę rozwijaną „Stawka VAT”: 23%, 8%, 5%, 0%, np. Domyślnie 23%.
- [x] „np” to stawka dla faktur reverse charge (niepodlegające, VAT rozlicza nabywca), a nie stawka 0%. W KSeF zapisywana jako `np II` z adnotacją „odwrotne obciążenie” (`P_18 = 1`) — [ADR 0005](../adr/0005-stawki-vat-i-jednostki.md).
- [x] Faktura z pozycją „np” ma w uwagach „Reverse charge” (`DodatkowyOpis`: „Uwagi” — „Reverse charge”).
- [x] Wartość brutto pozycji i sumy faktury (netto, VAT, brutto) uwzględniają stawkę każdej pozycji; VAT liczony od sumy netto każdej stawki.
- [x] Jednostka jest wybierana z listy: szt., dni, godz., usł. Domyślnie godz.
- [x] Szkice i faktury niewysłane zapisane wcześniej otwierają się ze stawką 23% i swoją jednostką.
- [x] Pole „Numer faktury” jest nad „Datą wystawienia”.
- [x] Sekcja „Rachunek bankowy” jest pod pozycjami (pod przyciskiem „Dodaj pozycję”), przed informacją o terminie płatności.
- [x] Faktura z pozycjami w różnych stawkach (w tym „np”) jest przyjmowana przez KSeF TEST.

## Uwagi / pytania

- 2026-10-12: stawkę wybieramy w każdej pozycji — decyzja użytkownika.
- 2026-10-12: stawka „nd” (pierwotnie zwolnienie `zw`) zmieniona na „np” = `np II` + `P_18 = 1` + uwaga „Reverse charge” — decyzja użytkownika; nazwa „nd” wycofana.
- Nabywca „np II” to zwykle firma z innego kraju UE (numer VAT UE), a `counterparties.json` przyjmuje tylko polski NIP — obsługa nabywców zagranicznych poza zakresem (do osobnej decyzji).
- 0% to zawsze 0% krajowe (`0 KR`), bo nabywcy są polscy.

## Zadania

- ZAD-019 — model: stawki VAT, kalkulator, XML FA(3), reverse charge
- ZAD-020 — UI: listy stawki i jednostki, nowa kolejność sekcji formularza
