# US-003. Lista faktur sprzedażowych

- Priorytet: MVP
- Status: Gotowa

## Historyjka

Jako **właściciel firmy** chcę **przeglądać faktury sprzedażowe z wybranego miesiąca i wyszukiwać je po NIP lub nazwie kontrahenta**, aby **szybko sprawdzić, co i komu zafakturowałem**.

## Kryteria akceptacji

- [ ] Domyślnie widzę bieżący miesiąc; mogę przełączać miesiące (poprzedni / następny / wybór).
- [ ] Miesiąc liczony jest po **dacie dostawy**.
- [ ] Wiersz listy: data dostawy, numer faktury, kontrahent (nazwa + NIP), netto, brutto.
- [ ] Pole „Szukaj” filtruje listę miesiąca po fragmencie NIP lub nazwy kontrahenta (bez rozróżniania wielkości liter).
- [ ] Pod listą podsumowanie miesiąca (po filtrze): netto, VAT, brutto.
- [ ] Dotknięcie wiersza otwiera szczegóły faktury.
- [ ] Z listy sprzedażowej mogę rozpocząć wystawianie nowej faktury.

## Zadania

- ZAD-009
