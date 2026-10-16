# 0011. „Wystaw podobną” — kopia faktury jako szkic nowej faktury, wspólna podpowiedź dat i numeru

- Status: Zaakceptowany
- Data: 2026-10-16

## Kontekst

- [US-015](../user-stories/US-015-wystaw-podobna.md): z widoku szczegółów faktury sprzedażowej użytkownik chce wystawić podobną fakturę — ten sam kontrahent, pozycje, rachunek; numer i daty jak dla nowej faktury. Dalsze kroki (podgląd, zapis bez wysyłki, wysyłka z biometrią) bez zmian.
- Formularz nowej faktury opiera się na jednym automatycznie zapisywanym szkicu (`issue/draft.json`, `InvoiceForm`); przy otwarciu szkic jest odświeżany (`InvoiceFormDefaults.refresh`): daty niewybrane przez użytkownika i numer niewpisany ręcznie są liczone od nowa.
- Podpowiedź dat (R3, `DefaultDates`) i numeru (C6, `InvoiceNumbering`) była składana w formularz w kilku miejscach (`newForm`, `refresh`, `NewInvoiceViewModel`). Użytkownik wymaga, by ta logika była w jednym miejscu i używana także przez „Wystaw podobną”.
- Decyzje użytkownika (2026-10-16): szkic z wpisanymi danymi jest **zastępowany bez pytania**; nabywca, którego nie ma w Kontrahentach, jest **dopisywany do Kontrahentów** z danych faktury; testy na emulatorze — ręcznie przez adb.
- Analiza: [07-wystaw-podobna.md](../analiza/07-wystaw-podobna.md).

## Rozważane opcje

1. **Szczegóły zapisują kopię jako szkic nowej faktury, potem otwierają zwykły formularz** — formularz bez zmian; stan w pliku, odporny na odtworzenie procesu.
2. Argument trasy `new-invoice?similarTo={numer KSeF}`, kopiowanie w `NewInvoiceViewModel` — więcej odpowiedzialności w VM formularza; po odtworzeniu procesu kopia mogłaby nadpisać zmiany.
3. Kopia jako faktura niewysłana (US-007) — rezerwuje numer i zostaje na liście, gdy użytkownik zrezygnuje.

## Decyzja

**Opcja 1.**

- `SimilarInvoice.of(invoice, profile, today, taken)` — czyste mapowanie faktury z lustra (`Invoice`) na `InvoiceForm`: kontrahent (po identyfikatorze, bez identyfikatora — po kraju i nazwie), rachunek (jeśli nadal jest w „Mojej firmie”, inaczej domyślny waluty), waluta, pozycje (bez `StanPrzed`). Kurs waluty nie jest kopiowany. Brakujący nabywca → `newCounterparty`, walidowany jak w formularzu (`ProfileValidator.counterparty`); niepoprawny (np. bez adresu) — nie jest dopisywany, kontrahent do wybrania w formularzu.
- **Jedno miejsce podpowiedzi dat i numeru: `InvoiceFormDefaults`** — `withDefaultDates` (data wystawienia = dziś, data dostawy wg `DefaultDates`), `withSuggestedNumber` / `suggestedNumber` (wg `InvoiceNumbering`), `withDefaults` (oba), `changeDeliveryDate`. Korzystają z nich: `newForm`, `refresh`, `NewInvoiceViewModel` i `SimilarInvoice`. Nikt poza `InvoiceFormDefaults` nie woła `DefaultDates.deliveryDate` ani `InvoiceNumbering.suggest` dla formularza.
- `InvoiceDetailsViewModel.issueSimilar()`: zapisuje najpierw kontrahenta, potem szkic (`IssueStorage.saveDraft`, zastępując poprzedni), a następnie ekran przechodzi do `new-invoice`. Formularz otwiera szkic jak zwykle; kopia ma `datesEdited = false` i `numberEdited = false`.
- Przycisk „Wystaw podobną” na dole szczegółów — tylko faktura sprzedażowa, `RodzajFaktury = VAT`, XML odczytany. Nieaktywny, gdy trwa wysyłka nowej faktury (po przyjęciu `InvoiceSendService` usuwa szkic — skasowałby kopię).

## Konsekwencje

- Wystawienie cyklicznej faktury (ten sam klient i usługa co miesiąc) to: szczegóły → „Wystaw podobną” → ewentualna zmiana ilości → podgląd → wysyłka.
- Kopia zastępuje rozpoczęty szkic nowej faktury bez ostrzeżenia — świadomie (decyzja użytkownika).
- Faktura może dopisać kontrahenta do Kontrahentów bez wchodzenia w Ustawienia; jego dane pochodzą z XML faktury (a nie z Białej listy).
- Zmiana reguły dat lub numeru (R3, C6) wymaga zmiany tylko w `DefaultDates` / `InvoiceNumbering` i ich złożenia w `InvoiceFormDefaults`.
- Zadania: ZAD-037, ZAD-038.
