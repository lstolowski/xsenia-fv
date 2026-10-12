# ZAD-017. Faktury niewysłane: UI listy, edycji, zapisu i usuwania

- User story: US-007
- Status: Do zrobienia
- Szacunek: M

## Cel

Zapis faktury bez wysyłki z podglądu, oznaczenie na liście sprzedażowej, edycja w formularzu i wysyłka / zapis / usunięcie.

## Zakres

- [ ] Podgląd: przycisk „Zapisz bez wysyłki do KSeF” (bez biometrii — P6) obok „Wyślij do KSeF”
- [ ] Lista sprzedażowa: faktury niewysłane w miesiącu daty dostawy, żółty trójkąt z wykrzyknikiem + „Niewysłana do KSeF”; poza sumami „Razem”, osobna linia „Niewysłane: n (brutto …)” (P3); wyszukiwanie działa także na nich
- [ ] Dotknięcie faktury niewysłanej → formularz z jej danymi (edycja zapisywana na bieżąco w tej fakturze); tytuł „Edycja faktury niewysłanej”
- [ ] Usunięcie faktury niewysłanej z formularza (potwierdzenie) (P4)
- [ ] Formularz faktury odrzuconej pokazuje powód odrzucenia (Q4)
- [ ] Podgląd: gdy data wystawienia < dziś — ostrzeżenie + „Ustaw dzisiejszą datę” (data dostawy bez zmian); można wysłać z zapisaną datą (Q5)
- [ ] Po błędzie wysyłki (przed dotarciem do KSeF) faktura zapisana jako niewysłana; ekran błędu informuje o tym
- [ ] Testy logiki listy (scalanie z niewysłanymi, sumy)

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na emulatorze z KSeF TEST (zapis bez wysyłki → edycja → wysyłka → faktura z numerem KSeF na liście bez ostrzeżenia)
- [ ] Zaktualizowana dokumentacja / słownik
