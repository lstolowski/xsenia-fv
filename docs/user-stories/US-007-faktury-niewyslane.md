# US-007. Faktury sprzedażowe zapisane bez wysyłki do KSeF

- Priorytet: MVP (przed ZAD-015)
- Status: Zrobiona (2026-10-12)
- Zgłoszone: 2026-10-12

## Historyjka

Jako **właściciel firmy** chcę **zapisać fakturę sprzedażową na telefonie bez wysyłania jej do KSeF**, aby **móc ją dalej edytować (pozycje, daty, ceny, numer, kontrahenta) i wysłać dopiero, gdy będzie gotowa**.

## Obecne zachowanie (US-006)

Wystawienie faktury zawsze kończy się wysyłką do KSeF. Nieudana wysyłka (błąd KSeF lub sieci) zostawia szkic, do którego można wrócić — ten mechanizm jest punktem wyjścia.

## Kryteria akceptacji

- [x] Ścieżka „Wyślij do KSeF” (podgląd → biometria → wysyłka) działa jak dotychczas; faktura przyjęta przez KSeF jest na liście sprzedażowej jak dotychczas.
- [x] Na ekranie podglądu, obok „Wyślij do KSeF”, jest przycisk **„Zapisz bez wysyłki do KSeF”** — zapisuje fakturę na telefonie i wraca do listy.
- [x] Faktura niewysłana jest widoczna na **liście faktur sprzedażowych** w miesiącu swojej daty dostawy, oznaczona **żółtym trójkątem z wykrzyknikiem** (ostrzeżenie: „Niewysłana do KSeF”).
- [x] Dotknięcie faktury niewysłanej otwiera **formularz** wypełniony jej danymi (zamiast widoku szczegółów); można zmienić wszystko: kontrahenta, rachunek, daty, numer, pozycje (dodawać, usuwać, zmieniać ceny).
- [x] Z formularza: „Podgląd” → na końcu **„Wyślij do KSeF”** albo **„Zapisz bez wysyłki do KSeF”**.
- [x] Po przyjęciu przez KSeF faktura przestaje być niewysłana: znika ostrzeżenie, jest zwykłą fakturą z numerem KSeF (z lustra), a jej lokalna kopia robocza jest usuwana.
- [x] Faktury, której wysyłka nie powiodła się przed dotarciem do KSeF (błąd sieci / KSeF) albo którą KSeF odrzucił, nie traci się — zostaje jako niewysłana (do edycji i ponownej wysyłki); przy odrzuconej formularz pokazuje powód odrzucenia.
- [x] Faktury niewysłane rezerwują numer (sugestia i duplikaty) i nie są wliczane do „Razem” (osobna linia „Niewysłane: …”).
- [x] Przy wysyłce z datą wystawienia wcześniejszą niż dziś — ostrzeżenie z wyborem: „Ustaw dzisiejszą datę” albo wysyłka z zapisaną datą; data dostawy bez zmian.
- [x] Faktura już przyjęta do przetwarzania przez KSeF (czekamy na numer KSeF) **nie** jest edytowalna — jej status jest dokańczany jak dotychczas.

## Decyzje (warsztat 2026-10-12)

| Nr | Decyzja |
|---|---|
| P1 | Dowolna liczba faktur niewysłanych naraz (każda z własnym identyfikatorem). |
| P2 / Q1 | Faktura niewysłana **rezerwuje numer**: jest brana pod uwagę w sugestii kolejnego numeru i w wykrywaniu duplikatów (np. niewysłana `04/09/2026` → sugestia `05/09/2026`); edytowana faktura nie koliduje sama ze sobą. |
| P3 / Q2 | Faktury niewysłane **nie są wliczane** do „Razem”; pod podsumowaniem osobna linia „Niewysłane: n (brutto …)”. |
| P4 | Fakturę niewysłaną można usunąć z formularza (z potwierdzeniem). |
| P5 / Q3 | Faktura niewysłana powstaje **tylko** po „Zapisz bez wysyłki do KSeF” na podglądzie. Szkic „Nowej faktury” zapisuje się w tle jak dotychczas (jeden, niewidoczny na liście). Edycja faktury niewysłanej zapisuje się na bieżąco w niej samej. |
| P6 | Biometria tylko przy wysyłce do KSeF; zapis bez wysyłki — bez biometrii. |
| P7 | „Wyczyść cache” nie usuwa faktur niewysłanych. |
| Q4 | Faktura, której wysyłka nie powiodła się przed dotarciem do KSeF (sieć / KSeF) **oraz** faktura **odrzucona** przez KSeF (np. 450, 440) — zapisywana automatycznie jako niewysłana; przy odrzuconej zapisany **powód odrzucenia**, widoczny w formularzu edycji. |
| Q5 | Przy wysyłce faktury z datą wystawienia wcześniejszą niż dziś podgląd **ostrzega** i proponuje „Ustaw dzisiejszą datę”; można też **zostawić zapisaną datę** i wysłać. **Data dostawy nie jest zmieniana.** |

## Zadania

- ZAD-016 — model i magazyn faktur niewysłanych, numeracja
- ZAD-017 — UI: zapis bez wysyłki, oznaczenie na liście, edycja i usuwanie
