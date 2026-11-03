# US-016. Wesprzyj rozwój aplikacji

- Priorytet: Etap 2
- Status: Zrobiona

## Historyjka

Jako **użytkownik** chcę **móc dobrowolnie wesprzeć autora (postawić kawę, dowolny datek)**, aby **aplikacja była dalej rozwijana**.

## Kryteria akceptacji

- [x] Ustawienia → ramka „Aplikacja”: podłużny zielony przycisk „Wesprzyj rozwój aplikacji” otwiera okno „Postaw kawę” (tytuł wyśrodkowany): obrazek filiżanki kawy, jedno zdanie o dobrowolnym wsparciu, przyciski „Wesprzyj dobrowolną kwotą” i „Nie teraz”
- [x] „Wesprzyj dobrowolną kwotą” otwiera w domyślnej przeglądarce https://suppi.pl/wsparcie/lsdevelopment i zamyka okno; dalsze kroki są już poza aplikacją
- [x] To samo okno pojawia się po przyjęciu przez KSeF **drugiej** faktury wystawionej w aplikacji, dopiero po kliknięciu „Gotowe” na ekranie sukcesu; po zamknięciu okna formularz się zamyka jak zwykle
- [x] Przy kolejnych fakturach okno się nie pojawia (licznik rośnie dalej) — tylko z Ustawień

## Uwagi / pytania

- Liczone są faktury przyjęte przez KSeF (nie zapisane bez wysyłki), łącznie dla wszystkich firm (profili), w pliku `cacheDir/issued-invoices.txt`. Kryterium celowo nierestrykcyjne: gdy plik zniknie („Wyczyść cache”, system), licznik startuje od 1 i okno pojawi się ponownie przy kolejnej drugiej fakturze — to nie szkodzi.
- Opuszczenie ekranu sukcesu drugiej faktury strzałką wstecz również pokazuje okno — inaczej pojawiłoby się dopiero przy „Gotowe” którejś z kolejnych faktur.
- Gdy na telefonie nie ma przeglądarki — komunikat „Brak przeglądarki do otwarcia strony wsparcia.”

## Zadania

- ZAD-039
