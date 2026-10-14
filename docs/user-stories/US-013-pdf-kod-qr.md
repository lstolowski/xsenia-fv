# US-013. PDF faktury sprzedażowej i udostępnianie

- Priorytet: MVP
- Status: Gotowa
- Zgłoszone: 2026-10-14 (wydzielone z US-012, decyzja Q5)

## Historyjka

Jako **właściciel firmy wystawiający faktury także nabywcom zagranicznym** chcę **wygenerować PDF faktury sprzedażowej i udostępnić go z telefonu**, aby **dostarczyć fakturę nabywcy, który nie odbierze jej z KSeF**.

Analiza: [06-pdf-kod-qr.md](../analiza/06-pdf-kod-qr.md), decyzja: [ADR 0010](../adr/0010-pdf-faktury.md). Zmienia decyzję z [US-005](US-005-szczegoly-faktury.md) („bez udostępniania”).

## Kryteria akceptacji

### Treść PDF

- [ ] A4, prosty układ jak ekran szczegółów, bez logo: nagłówek (rodzaj, numer faktury, numer KSeF, data wystawienia, data dostawy), sprzedawca i nabywca (nazwa, NIP / numer VAT UE / identyfikator, adres, kraj), tabela pozycji (nazwa, ilość, jednostka, cena jedn. netto, stawka, wartość netto, wartość brutto), podsumowanie wg stawek (netto, VAT, brutto; VAT w PLN i kurs dla waluty obcej), płatność (forma, termin, rachunek, bank), uwagi („Reverse charge”, „Kurs waluty”).
- [ ] Długa lista pozycji — kolejne strony z numerem strony.
- [ ] **Bez kodu QR** — tylko numer KSeF.
- [ ] Faktura niewysłana — zamiast numeru KSeF napis „Faktura nie wysłana do KSeF”.
- [ ] Polski nabywca — etykiety po polsku; zagraniczny — „polski / angielski” (np. „Sprzedawca / Seller”, „Faktura nie wysłana do KSeF / Not yet submitted to KSeF”).

### Udostępnianie

- [ ] Ikona udostępniania (trzy kropki połączone liniami) otwiera systemowy arkusz „Udostępnij” z samym PDF (e-mail, komunikator, Dysk, „Zapisz w Plikach”).
- [ ] Ikona jest: w szczegółach faktury sprzedażowej (nie zakupowej), na ekranie po udanej wysyłce, w podglądzie nowej / niewysłanej faktury (PDF z tego, co w podglądzie, z kursem na dzień wysyłki).
- [ ] Plik tymczasowy, bez zapisu w Pobranych; „Wyczyść cache” go usuwa.

## Uwagi / pytania

Decyzje z sesji pytań (2026-10-14):

- Kod QR KSeF — **na razie rezygnacja**; PDF pokazuje tylko numer KSeF (specyfikacja kodu w analizie 06, na później).
- Q1 (b) + niewysłane — faktury sprzedażowe, także niewysłane; zakupowe nie.
- Q2 (a) — język wg nabywcy: polski — PL, zagraniczny — PL / EN.
- Q3 (a) — prosty własny układ A4; logo nie przewidziane.
- Q4 (a) — udostępniany tylko PDF.
- Q5 (a) — szczegóły faktury i ekran po wysyłce.
- Q6 (a) — plik tymczasowy, zapis przez arkusz „Udostępnij”; ikona udostępniania (trzy kropki połączone dwiema liniami).
- R2-Q1 (a) — niewysłana: „Faktura nie wysłana do KSeF” (PL), dla nabywcy zagranicznego z angielskim „Not yet submitted to KSeF”.
- R2-Q2 (a) — niewysłaną i nową fakturę udostępnia się z podglądu.

## Zadania

- ZAD-034 — układ PDF: bloki, tabela pozycji, strony, etykiety PL / PL+EN, nazwa pliku (czysta logika, testy JVM)
- ZAD-035 — rysowanie PDF, udostępnianie (`FileProvider`, arkusz „Udostępnij”), ikony w szczegółach, po wysyłce i w podglądzie
