# US-006. Wystawienie nowej faktury sprzedażowej

- Priorytet: MVP
- Status: Zrobiona (2026-10-12)

> Od [US-010](US-010-dane-firmy-w-aplikacji.md) dane mojej firmy, rachunki i kontrahenci są wprowadzane w aplikacji, a nie w plikach JSON ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)). Treść poniżej opisuje stan z chwili realizacji.

## Historyjka

Jako **właściciel firmy** chcę **wystawić nową fakturę sprzedażową VAT i wysłać ją do KSeF z telefonu**, aby **zafakturować klienta w minutę, bez oficjalnej aplikacji KSeF**.

## Kryteria akceptacji

- [x] Wybieram kontrahenta z listy z `counterparties.json` i rachunek z listy z `banks.json` (domyślnie pierwszy, tylko PLN).
- [x] Data wystawienia = dziś (edytowalna). Data dostawy domyślnie: ostatni dzień poprzedniego miesiąca, jeśli dziś jest 1.–15. dzień miesiąca; w przeciwnym razie dziś (edytowalna).
- [x] Numer faktury jest sugerowany jako `NN/MM/RRRR` (MM/RRRR z daty dostawy, zero wiodące) i edytowalny; zmiana daty dostawy aktualizuje sugestię, dopóki nie edytowałem numeru ręcznie.
- [x] NN = najwyższy numer spośród faktur sprzedażowych, których **numer** kończy się na `/MM/RRRR`, + 1 (liczone po numerach, nie po dacie dostawy — błędnie ponumerowana faktura nie zaburza sugestii i nie powoduje duplikatu).
- [x] Ostrzeżenie, jeśli numer już istnieje, oraz jeśli miesiąc/rok w numerze nie zgadza się z datą dostawy.
- [x] Dodaję 1..n pozycji: nazwa, ilość, jednostka, cena jedn. netto; aplikacja wylicza wartość netto i brutto pozycji oraz sumy (VAT 23% od sumy netto; brutto pozycji informacyjnie). Od US-009: stawka i jednostka z listy w każdej pozycji.
- [x] Płatność: przelew, termin 21 dni od daty wystawienia — bez możliwości zmiany.
- [x] Szkic jest zapisywany na bieżąco i przetrwa zamknięcie aplikacji.
- [x] Przed wysyłką widzę podgląd faktury w takiej formie jak w szczegółach; „Wyślij do KSeF” wymaga potwierdzenia biometrią.
- [x] Po wysyłce widzę status (wysłana → przyjęta z numerem KSeF / odrzucona z czytelną przyczyną); status przetrwa zamknięcie aplikacji.
- [x] Po przyjęciu faktura pojawia się na liście sprzedażowej, a szkic jest usuwany; UPO zapisane lokalnie (bez udostępniania — decyzja 2026-10-12).

## Zadania

- ZAD-011
- ZAD-012
- ZAD-013
