# „Wystaw podobną” — analiza zmian (US-015)

Status: **zakończona 2026-10-16** — decyzje w [US-015](../user-stories/US-015-wystaw-podobna.md), [ADR 0011](../adr/0011-wystaw-podobna.md)

Cel: z widoku szczegółów faktury sprzedażowej jednym przyciskiem przejść do formularza nowej faktury z danymi skopiowanymi z tej faktury. Daty i numer — jak przy zwykłej nowej fakturze. Dalsze kroki (podgląd, zapis bez wysyłki, wysyłka do KSeF po biometrii) bez zmian.

## 1. Stan przed zmianą

- **Szczegóły faktury** (`InvoiceDetailsScreen`, `InvoiceDetailsViewModel`): treść z lustra — `Invoice` z `InvoiceXmlParser` (XML z KSeF). Tylko odczyt; dla sprzedażowych ikona udostępniania PDF.
- **Nowa faktura** (`NewInvoiceViewModel`, trasa `new-invoice`): jeden szkic `issue/draft.json` (`InvoiceForm`), zapisywany automatycznie po każdej zmianie. Przy otwarciu: `InvoiceFormDefaults.refresh(szkic)` — daty, których użytkownik nie wybrał (`datesEdited = false`), ustawiane na dziś; numer niewpisany ręcznie (`numberEdited = false`) — sugerowany od nowa. Brak szkicu → `InvoiceFormDefaults.newForm`.
- **Podpowiedź dat i numeru** była w kilku miejscach:
  - `InvoiceFormDefaults.newForm` — `DefaultDates.deliveryDate(today)` + `InvoiceNumbering.suggest`,
  - `InvoiceFormDefaults.refresh` — ta sama logika powtórzona,
  - `NewInvoiceViewModel.setDeliveryDate`, `useSuggestedNumber`, `state.suggestedNumber` — bezpośrednie wywołania `InvoiceNumbering.suggest`.
  Reguły (R3 — data dostawy, C6 — numer) są w module `invoice` (`DefaultDates`, `InvoiceNumbering`), ale ich złożenie w formularz było powielone.
- **Wysyłka** (`InvoiceSendService`): po przyjęciu nowej faktury usuwa szkic (`clearDraft`); po odrzuceniu / błędzie zapisuje ją jako niewysłaną i też usuwa szkic.

## 2. Co kopiujemy z faktury

| Dane | Źródło w `Invoice` | Do formularza | Uwagi |
|---|---|---|---|
| Kontrahent | `buyer` (`Podmiot2`) | `counterpartyId` | Szukany w Kontrahentach po identyfikatorze (NIP / VAT UE / inny ID → `Counterparty.id`); bez identyfikatora (`BrakID`) — po kraju i nazwie. **Brak → dopisywany do Kontrahentów** z danych faktury (decyzja użytkownika) |
| Rachunek | `payment.bankAccounts[0]` (`NrRB`) | `bankAccountNumber` | Jeśli rachunku nie ma już w „Mojej firmie” — domyślny rachunek waluty faktury |
| Waluta | `currency` (`KodWaluty`) | `currency` | Waluta spoza listy (`InvoiceCurrency`) → PLN |
| Pozycje | `lines` (bez `StanPrzed`) | `lines` | Nazwa, ilość, jednostka, cena jedn. netto, stawka. `np I` / `np II` → „np”; stawka, której aplikacja nie wystawia (np. `zw`) → domyślna dla nabywcy. Brak ceny netto (faktura liczona od brutto) — pole puste |
| Numer faktury | — | sugerowany | `InvoiceNumbering` (C6), jak w nowej fakturze |
| Data wystawienia | — | dziś | jak w nowej fakturze |
| Data dostawy | — | `DefaultDates` (R3) | ostatni dzień poprzedniego miesiąca w dniach 1–5 (do 2026-10-16: 1–15), inaczej dziś |
| Kurs waluty | — | nie kopiowany | kurs roboczy pobierany dla nowych dat (ADR 0008) |

Nie kopiujemy też danych sprzedawcy (zawsze aktualna „Moja firma”) ani terminu płatności (zawsze 21 dni — US-006).

## 3. Które faktury

- Tylko **sprzedażowe** (zakupowe — nie ma czego „wystawić podobnie”).
- Tylko zwykłe faktury **VAT** (`RodzajFaktury = VAT`) — aplikacja nie wystawia korekt ani faktur zaliczkowych.
- Tylko gdy XML udało się odczytać (przy błędzie parsowania widok pokazuje same metadane — nie ma pozycji do skopiowania).
- Faktury niewysłane (US-007) nie mają widoku szczegółów — otwierają się od razu w formularzu; poza zakresem.

## 4. Warianty przekazania danych do formularza

1. **Szczegóły zapisują kopię jako szkic, formularz otwiera szkic jak zwykle (wybrany).** `NewInvoiceViewModel` bez zmian; kopia ma `datesEdited = false`, `numberEdited = false`, więc `refresh` przy otwarciu i tak liczy daty i numer wspólną logiką. Odporne na odtworzenie procesu (stan jest w pliku, nie w argumencie trasy).
2. Argument trasy `new-invoice?similarTo={ksefNumber}`, kopiowanie w `NewInvoiceViewModel` — VM dostaje kolejną odpowiedzialność (lustro, parser, kontrahenci); po odtworzeniu procesu argument trasy zostaje i kopia mogłaby nadpisać zmiany wprowadzone w międzyczasie.
3. Kopia jako faktura niewysłana — rezerwuje numer i zaśmieca listę „Niewysłane”, jeśli użytkownik zrezygnuje.

## 5. Ryzyka i przypadki brzegowe

- **Szkic z wpisanymi danymi** — zastępowany bez pytania (decyzja użytkownika 2026-10-16).
- **Trwa wysyłka nowej faktury** — po przyjęciu `InvoiceSendService` usuwa szkic, więc skasowałby kopię. Przycisk jest wtedy nieaktywny z komunikatem „Poczekaj na wynik wysyłki nowej faktury do KSeF.”.
- **Nabywca z niepełnymi danymi** (np. brak adresu w XML) — nie przejdzie walidacji `ProfileValidator`; nie jest dopisywany, w formularzu kontrahent do wybrania.
- **Kontrahent istnieje, ale z innymi danymi** (np. zmieniony adres) — używany ten z Kontrahentów (aktualne dane mają pierwszeństwo).
- **Kolejność zapisu**: najpierw kontrahent, potem szkic — szkic nigdy nie wskazuje niezapisanego kontrahenta.

## 6. Zmiany w kodzie

- `InvoiceFormDefaults` — jedno miejsce podpowiedzi: `withDefaults` = `withDefaultDates` + `withSuggestedNumber`; `suggestedNumber`, `changeDeliveryDate`. Używane przez `newForm`, `refresh`, `NewInvoiceViewModel` i `SimilarInvoice`.
- `SimilarInvoice` (nowy, `app/.../issue/`) — czyste mapowanie `Invoice` + `CompanyProfile` → `InvoiceForm` + ewentualny nowy `Counterparty`; testy w JVM.
- `InvoiceDetailsViewModel.issueSimilar()` — zapis kontrahenta i szkicu, sygnał do nawigacji; `sendingNewInvoice`.
- `InvoiceDetailsScreen` — przycisk „Wystaw podobną” na dole; `MainScaffold` — przejście do `new-invoice`.

## 7. Testy

- Jednostkowe: `SimilarInvoiceTest` (faktura źródłowa = XML z `Fa3XmlWriter` odczytany parserem, jak z lustra), `InvoiceFormDefaultsTest` (wspólna logika dat i numeru, także porównanie z `newForm` dla kilku dni).
- Emulator (ręcznie, adb, KSeF TEST): przycisk tylko przy sprzedażowej, kopia w formularzu, podgląd, zapis bez wysyłki, wysyłka z biometrią, dopisanie brakującego kontrahenta.
