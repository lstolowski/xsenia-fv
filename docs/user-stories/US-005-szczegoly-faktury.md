# US-005. Szczegóły faktury

- Priorytet: MVP
- Status: Zrobiona (2026-10-12)

## Historyjka

Jako **właściciel firmy** chcę **zobaczyć fakturę (sprzedażową lub zakupową) w czytelnej formie na telefonie**, aby **nie musieć czytać XML ani logować się do KSeF**.

## Kryteria akceptacji

- [x] Widok zawiera: numer faktury, numer KSeF, rodzaj, daty (wystawienia, dostawy), sprzedawcę i nabywcę (nazwa, NIP, adres), pozycje (nazwa, ilość, jednostka, cena jedn. netto, wartość netto, wartość brutto), podsumowanie wg stawek (netto, VAT, brutto), płatność (forma, termin, rachunek).
- [x] Pola nieobecne w fakturze są pomijane (bez pustych etykiet); nieznane elementy nie powodują błędu.
~~Mogę udostępnić plik XML faktury~~ / ~~UPO~~ — **rezygnacja** (decyzja użytkownika 2026-10-12: brak funkcji udostępniania, przynajmniej na razie). Od 2026-10-14 faktury sprzedażowe można udostępnić jako PDF — [US-013](US-013-pdf-kod-qr.md); XML i UPO nadal nie.
- [x] Widok działa offline dla faktur w lustrze.

## Zadania

- ZAD-007
- ZAD-010
