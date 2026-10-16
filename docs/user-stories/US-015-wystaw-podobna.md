# US-015. Wystaw podobną fakturę

- Priorytet: MVP
- Status: Zrobiona (2026-10-16)
- Zgłoszone: 2026-10-16

## Historyjka

Jako **właściciel firmy wystawiający co miesiąc podobne faktury** chcę **z widoku szczegółów faktury sprzedażowej wystawić podobną jednym przyciskiem**, aby **nie wpisywać od nowa kontrahenta, pozycji i rachunku**.

## Kryteria akceptacji

- [x] Na dole widoku szczegółów faktury sprzedażowej jest przycisk „Wystaw podobną”. Nie ma go przy fakturach zakupowych, korektach i innych niż zwykła faktura VAT oraz gdy treści faktury nie udało się odczytać.
- [x] Przycisk otwiera formularz nowej faktury, w którym skopiowane są: kontrahent, pozycje (nazwa, ilość, jednostka, cena jedn. netto, stawka VAT), rachunek bankowy i waluta.
- [x] Numer faktury — proponowany kolejny, jak przy nowej fakturze (`NN/MM/RRRR` wg miesiąca daty dostawy).
- [x] Data wystawienia — dzisiejsza, jak przy nowej fakturze.
- [x] Data dostawy / wykonania usługi — jak przy nowej fakturze: ostatni dzień poprzedniego miesiąca w dniach 1–5, w przeciwnym razie dzisiejsza (próg zmieniony z 15 na 5 — 2026-10-16).
- [x] Logika podpowiadania numeru i dat jest w jednym miejscu w kodzie i używana zarówno przez nową fakturę, jak i „Wystaw podobną”.
- [x] Kolejne kroki bez zmian: podgląd, zapis bez wysyłki lub wysyłka do KSeF po potwierdzeniu biometrią.
- [x] Rozpoczęty szkic nowej faktury jest zastępowany kopią bez pytania.
- [x] Nabywcy z faktury, którego nie ma w Kontrahentach, aplikacja dopisuje do Kontrahentów z danych faktury (nazwa, NIP / numer VAT UE / identyfikator, adres, kraj, e-mail). Gdy dane z faktury nie wystarczają (np. brak adresu) — kontrahent do wybrania w formularzu.
- [x] Rachunek, którego nie ma już w „Mojej firmie”, zastępowany domyślnym rachunkiem waluty faktury.
- [x] Faktura w walucie obcej: waluta skopiowana, kurs NBP pobierany od nowa dla nowych dat.
- [x] Gdy trwa wysyłka nowej faktury do KSeF, przycisk jest nieaktywny z informacją „Poczekaj na wynik wysyłki nowej faktury do KSeF.”.

## Uwagi / pytania

- Decyzje użytkownika (2026-10-16): szkic zastępowany bez pytania; brakujący nabywca dopisywany do Kontrahentów; testy na emulatorze ręcznie przez adb.
- Stawka, której aplikacja nie wystawia (np. `zw` z faktury z innego programu), zamieniana na domyślną dla nabywcy (23% / „np” dla zagranicznego).
- Faktury niewysłane (US-007) nie mają widoku szczegółów — poza zakresem.
- Analiza: [07-wystaw-podobna.md](../analiza/07-wystaw-podobna.md); decyzja: [ADR 0011](../adr/0011-wystaw-podobna.md).

## Zadania

- ZAD-037 — Wspólna podpowiedź dat i numeru, kopiowanie faktury do formularza (`SimilarInvoice`)
- ZAD-038 — UI: przycisk „Wystaw podobną”, zapis szkicu i kontrahenta, przejście do formularza, test na emulatorze
