# US-002. Synchronizacja faktur z KSeF

- Priorytet: MVP
- Status: Gotowa

## Historyjka

Jako **właściciel firmy** chcę, **aby aplikacja sama pobierała z KSeF moje faktury sprzedażowe i zakupowe od 01.04.2026**, aby **przeglądać je szybko, także bez internetu, bez ręcznego pobierania**.

## Kryteria akceptacji

- [ ] Przy pierwszym uruchomieniu aplikacja pobiera wszystkie faktury sprzedażowe i zakupowe od 01.04.2026 i pokazuje postęp.
- [ ] Przy każdym otwarciu (jeśli od ostatniej synchronizacji minęło więcej niż kilka minut) i po przycisku „Odśwież z KSeF” pobierane są tylko nowe faktury.
- [ ] Widzę datę i godzinę ostatniej udanej synchronizacji.
- [ ] Brak internetu lub błąd KSeF (w tym przekroczenie limitu) nie blokuje przeglądania już pobranych faktur; widzę zrozumiały komunikat po polsku.
- [ ] Dla każdej faktury wyliczana jest data dostawy: `P_6` → `P_6_Do` → najpóźniejsza `P_6A` → data wystawienia.

## Zadania

- ZAD-003
- ZAD-006
- ZAD-007
- ZAD-008
