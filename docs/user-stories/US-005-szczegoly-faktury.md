# US-005. Szczegóły faktury

- Priorytet: MVP
- Status: Gotowa

## Historyjka

Jako **właściciel firmy** chcę **zobaczyć fakturę (sprzedażową lub zakupową) w czytelnej formie na telefonie**, aby **nie musieć czytać XML ani logować się do KSeF**.

## Kryteria akceptacji

- [ ] Widok zawiera: numer faktury, numer KSeF, rodzaj, daty (wystawienia, dostawy), sprzedawcę i nabywcę (nazwa, NIP, adres), pozycje (nazwa, ilość, jednostka, cena jedn. netto, wartość netto, wartość brutto), podsumowanie wg stawek (netto, VAT, brutto), płatność (forma, termin, rachunek).
- [ ] Pola nieobecne w fakturze są pomijane (bez pustych etykiet); nieznane elementy nie powodują błędu.
- [ ] Mogę udostępnić plik XML faktury (systemowe „Udostępnij”).
- [ ] Dla faktur wystawionych w aplikacji mogę udostępnić UPO.
- [ ] Widok działa offline dla faktur w lustrze.

## Zadania

- ZAD-007
- ZAD-010
