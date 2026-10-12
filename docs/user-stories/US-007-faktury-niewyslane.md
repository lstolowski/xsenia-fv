# US-007. Faktury sprzedażowe zapisane bez wysyłki do KSeF

- Priorytet: MVP (przed ZAD-015)
- Status: Szkic — do potwierdzenia pytania P1–P7
- Zgłoszone: 2026-10-12

## Historyjka

Jako **właściciel firmy** chcę **zapisać fakturę sprzedażową na telefonie bez wysyłania jej do KSeF**, aby **móc ją dalej edytować (pozycje, daty, ceny, numer, kontrahenta) i wysłać dopiero, gdy będzie gotowa**.

## Obecne zachowanie (US-006)

Wystawienie faktury zawsze kończy się wysyłką do KSeF. Nieudana wysyłka (błąd KSeF lub sieci) zostawia szkic, do którego można wrócić — ten mechanizm jest punktem wyjścia.

## Kryteria akceptacji

- [ ] Ścieżka „Wyślij do KSeF” (podgląd → biometria → wysyłka) działa jak dotychczas; faktura przyjęta przez KSeF jest na liście sprzedażowej jak dotychczas.
- [ ] Na ekranie podglądu, obok „Wyślij do KSeF”, jest przycisk **„Zapisz bez wysyłki do KSeF”** — zapisuje fakturę na telefonie i wraca do listy.
- [ ] Faktura niewysłana jest widoczna na **liście faktur sprzedażowych** w miesiącu swojej daty dostawy, oznaczona **żółtym trójkątem z wykrzyknikiem** (ostrzeżenie: „Niewysłana do KSeF”).
- [ ] Dotknięcie faktury niewysłanej otwiera **formularz** wypełniony jej danymi (zamiast widoku szczegółów); można zmienić wszystko: kontrahenta, rachunek, daty, numer, pozycje (dodawać, usuwać, zmieniać ceny).
- [ ] Z formularza: „Podgląd” → na końcu **„Wyślij do KSeF”** albo **„Zapisz bez wysyłki do KSeF”**.
- [ ] Po przyjęciu przez KSeF faktura przestaje być niewysłana: znika ostrzeżenie, jest zwykłą fakturą z numerem KSeF (z lustra), a jej lokalna kopia robocza jest usuwana.
- [ ] Faktury, której wysyłka nie powiodła się przed dotarciem do KSeF (błąd sieci / KSeF), nie traci się — zostaje jako niewysłana (do edycji i ponownej wysyłki).
- [ ] Faktura już przyjęta do przetwarzania przez KSeF (czekamy na numer KSeF) **nie** jest edytowalna — jej status jest dokańczany jak dotychczas.

## Propozycje do potwierdzenia

- **P1. Wiele faktur niewysłanych naraz** — tak, dowolna liczba (każda z własnym identyfikatorem).
- **P2. Numeracja** — numery faktur niewysłanych **rezerwują** numer: są brane pod uwagę przy sugestii kolejnego numeru i przy wykrywaniu duplikatu (np. niewysłana `04/09/2026` → następna sugestia `05/09/2026`). Edytowana faktura nie koliduje sama ze sobą.
- **P3. Podsumowanie miesiąca** — faktury niewysłane **nie są wliczane** do sum „Razem” (to nie są jeszcze faktury w KSeF); pod podsumowaniem osobna informacja „Niewysłane: n (brutto …)”.
- **P4. Usuwanie** — fakturę niewysłaną można usunąć (przycisk w formularzu, z potwierdzeniem).
- **P5. Szkic „Nowej faktury”** — automatyczny zapis szkicu nowej faktury zostaje (jak dziś, jeden szkic); „Zapisz bez wysyłki” zamienia go w fakturę niewysłaną. Edycja faktury niewysłanej zapisuje się na bieżąco w niej samej.
- **P6. Biometria** — wymagana tylko przy wysyłce do KSeF; zapis bez wysyłki — bez biometrii (aplikacja jest już odblokowana).
- **P7. „Wyczyść cache”** w ustawieniach **nie usuwa** faktur niewysłanych (to nie są dane z KSeF — nie dałoby się ich odtworzyć).

## Zadania

- ZAD-016 — model i magazyn faktur niewysłanych, numeracja
- ZAD-017 — UI: zapis bez wysyłki, oznaczenie na liście, edycja i usuwanie
