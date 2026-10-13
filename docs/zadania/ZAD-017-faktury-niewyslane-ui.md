# ZAD-017. Faktury niewysłane: UI listy, edycji, zapisu i usuwania

- User story: US-007
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Zapis faktury bez wysyłki z podglądu, oznaczenie na liście sprzedażowej, edycja w formularzu i wysyłka / zapis / usunięcie.

## Zakres

- [x] Podgląd: przycisk „Zapisz bez wysyłki do KSeF” (bez biometrii — P6) obok „Wyślij do KSeF”
- [x] Lista sprzedażowa: faktury niewysłane w miesiącu daty dostawy, żółty trójkąt z wykrzyknikiem + „Niewysłana do KSeF”; poza sumami „Razem”, osobna linia „Niewysłane: n (brutto …)” (P3); wyszukiwanie działa także na nich
- [x] Dotknięcie faktury niewysłanej → formularz z jej danymi (edycja zapisywana na bieżąco w tej fakturze); tytuł „Edycja faktury niewysłanej”
- [x] Usunięcie faktury niewysłanej z formularza (potwierdzenie) (P4)
- [x] Formularz faktury odrzuconej pokazuje powód odrzucenia (Q4)
- [x] Podgląd: gdy data wystawienia < dziś — ostrzeżenie + „Ustaw dzisiejszą datę” (data dostawy bez zmian); można wysłać z zapisaną datą (Q5)
- [x] Po błędzie wysyłki (przed dotarciem do KSeF) faktura zapisana jako niewysłana; ekran błędu informuje o tym
- [x] Testy logiki listy (scalanie z niewysłanymi, sumy)

## Definicja ukończenia

- [x] Testy
- [x] Sprawdzone na emulatorze z KSeF TEST (zapis bez wysyłki → edycja → wysyłka → faktura z numerem KSeF na liście bez ostrzeżenia)
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja / słownik

## Uwagi

- Sprawdzone na emulatorze (KSeF TEST, 2026-10-12): zapis `04/09/2026` bez wysyłki, trójkąt i linia „Niewysłane” poza „Razem”, edycja z autozapisem (druga pozycja, cena), rezerwacja numeru (`05/09/2026`), ostrzeżenie o dacie wystawienia (zmiana na dziś, data dostawy bez zmian), usuwanie z potwierdzeniem, wysyłka faktury niewysłanej → zwykła faktura z numerem KSeF.
- Poprawka przy okazji: zamykanie ekranu tylko, gdy jest na wierzchu (`popIfOn`) — podwójne „Gotowe” zdejmowało listę i zostawiało pusty ekran.
