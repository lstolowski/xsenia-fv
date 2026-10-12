# US-003. Lista faktur sprzedażowych

- Priorytet: MVP
- Status: Zrobiona (2026-10-12)

## Historyjka

Jako **właściciel firmy** chcę **przeglądać faktury sprzedażowe z wybranego miesiąca i wyszukiwać je po NIP lub nazwie kontrahenta**, aby **szybko sprawdzić, co i komu zafakturowałem**.

## Kryteria akceptacji

- [x] Domyślnie widzę bieżący miesiąc; mogę przełączać miesiące (poprzedni / następny / wybór).
- [x] Miesiąc liczony jest po **dacie dostawy**.
- [x] Wiersz listy: data dostawy, numer faktury, kontrahent (nazwa + NIP), netto, brutto.
- [x] Pole „Szukaj” filtruje listę miesiąca po fragmencie NIP lub nazwy kontrahenta (bez rozróżniania wielkości liter).
- [x] Pod listą podsumowanie miesiąca (po filtrze): netto, VAT, brutto.
- [x] Dotknięcie wiersza otwiera szczegóły faktury.
- [x] Z listy sprzedażowej mogę rozpocząć wystawianie nowej faktury.

## Zadania

- ZAD-009
