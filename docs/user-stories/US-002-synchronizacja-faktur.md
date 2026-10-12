# US-002. Synchronizacja faktur z KSeF

- Priorytet: MVP
- Status: Zrobiona (2026-10-12)

## Historyjka

Jako **właściciel firmy** chcę, **aby aplikacja sama pobierała z KSeF moje faktury sprzedażowe i zakupowe od 01.04.2026**, aby **przeglądać je szybko, także bez internetu, bez ręcznego pobierania**.

## Kryteria akceptacji

- [x] Przy pierwszym uruchomieniu aplikacja pobiera wszystkie faktury sprzedażowe i zakupowe od 01.04.2026 i pokazuje postęp.
- [x] Przy każdym otwarciu (jeśli od ostatniej synchronizacji minęło co najmniej 15 minut — zalecenie MF, ZAD-008) i po przycisku „Odśwież z KSeF” pobierane są tylko nowe faktury.
- [x] Widzę datę i godzinę ostatniej udanej synchronizacji.
- [x] Brak internetu lub błąd KSeF (w tym przekroczenie limitu) nie blokuje przeglądania już pobranych faktur; widzę zrozumiały komunikat po polsku.
- [x] Dla każdej faktury wyliczana jest data dostawy: `P_6` → `P_6_Do` → najpóźniejsza `P_6A` → data wystawienia.

## Zadania

- ZAD-003
- ZAD-006
- ZAD-007
- ZAD-008
