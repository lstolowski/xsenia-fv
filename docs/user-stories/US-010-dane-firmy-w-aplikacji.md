# US-010. Moja firma, rachunki i kontrahenci w Ustawieniach, z pobieraniem danych po NIP-ie

- Priorytet: MVP
- Status: Zrobiona (2026-10-13)
- Zgłoszone: 2026-10-13 (po testach na telefonie)

## Historyjka

Jako **właściciel firmy** chcę **wprowadzać i edytować dane mojej firmy, jej rachunki bankowe i kontrahentów w Ustawieniach aplikacji, z automatycznym pobraniem danych po NIP-ie**, aby **nie edytować ręcznie plików JSON na telefonie i nie przepisywać nazw, adresów ani numerów rachunków**.

## Kryteria akceptacji

### Pobieranie danych po NIP-ie (Biała lista MF)

- [x] Jeden serwis pobiera dane firmy po NIP-ie z `https://wl-api.mf.gov.pl/api/search/nip/{nip}?date={yyyy-MM-dd}` (NIP bez myślników, data „dziś”) i jest używany w formularzach „Moja firma”, „Kontrahent” i przy pierwszym uruchomieniu — [ADR 0007](../adr/0007-biala-lista-mf.md).
- [x] Obok pola NIP jest ikona pobierania. Po dotknięciu: nazwa i adres zostają **nadpisane** danymi z rejestru (pisownia jak w rejestrze), adres rozbity na dwie linie; pozostałe pola bez zmian i nadal edytowalne.
- [x] Gdy firma nie jest czynnym podatnikiem VAT — ikona ostrzeżenia i komunikat „Firma nie jest czynnym podatnikiem”.
- [x] Gdy rejestr nie zna NIP-u — „Nie znaleziono firmy w Rejestrze WL”; niepoprawny NIP (suma kontrolna) — komunikat bez wysyłania zapytania; brak sieci, limit zapytań, błąd serwera — czytelne komunikaty po polsku.

### Moja firma

- [x] Ustawienia mają sekcję „Moja firma”: podgląd danych firmy i jej rachunków, na dole przycisk „Edytuj” prowadzący do formularza.
- [x] Formularz ma pola jak dotychczasowy `company.json`: nazwa, NIP, adres (linia 1, linia 2, kraj), e-mail, telefon. NIP pochodzi z tokena KSeF i jest tylko do odczytu; pierwszy element formularza to NIP z ikoną pobierania.
- [x] Firma może mieć wiele rachunków bankowych (pola jak w `banks.json`: nazwa, bank, numer, waluta); można je dodawać, edytować i usuwać.
- [x] Po pobraniu danych firmy z Białej listy mogę zaznaczyć rachunki z rejestru, które zostaną dodane do firmy (nazwę i bank uzupełniam ręcznie).
- [x] Aplikacja działa w kontekście jednej mojej firmy (profil = NIP z tokena); w UI nie ma i nie jest sugerowana zmiana firmy — [ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md).

### Kontrahenci

- [x] Ustawienia mają pozycję „Kontrahenci” — odnośnik do listy kontrahentów (te same kafelki co przy wyborze kontrahenta w formularzu „Nowa faktura”).
- [x] Mogę dodać kontrahenta: formularz z polami jak w `counterparties.json` (nazwa, NIP, adres, e-mail); pierwszy element to NIP z ikoną pobierania.
- [x] Mogę edytować i usunąć kontrahenta (usunięcie z potwierdzeniem; ostrzeżenie, gdy kontrahent jest użyty w szkicu lub fakturze niewysłanej). NIP istniejącego kontrahenta nie jest edytowalny.
- [x] W oknie wyboru kontrahenta na formularzu „Nowa faktura” jest „+ Nowy kontrahent”; po zapisie nowy kontrahent jest od razu wybrany.

### Pierwsze uruchomienie i pliki JSON

- [x] Pierwsze uruchomienie: token KSeF → „Moja firma” (NIP z tokena, dane z Białej listy pobierane automatycznie, wybór rachunków z rejestru, zapis) → ekran główny. Kontrahentów dodaję później.
- [x] Gdy do wystawienia faktury czegoś brakuje (dane firmy, rachunek PLN, kontrahent), formularz „Nowa faktura” to pokazuje i ma przycisk „Uzupełnij”.
- [x] Aplikacja nie czyta już plików `company.json`, `banks.json`, `counterparties.json`; wybór folderu znika z konfiguracji i Ustawień. Danych z plików nie importujemy.
- [x] Istniejący szkic, faktury niewysłane i pobrane faktury zostają zachowane po aktualizacji.
- [x] Wystawienie faktury z danymi wprowadzonymi w aplikacji jest przyjmowane przez KSeF TEST (dane zmyślone) — faktura `09/09/2026` przyjęta 2026-10-13 (ZAD-025).

## Uwagi / pytania

Decyzje z sesji pytań (2026-10-13):

- Q1 — pliki JSON i wybór folderu usunięte, bez importu i eksportu. Odinstalowanie aplikacji usuwa dane firmy i kontrahentów (kopia zapasowa Androida wyłączona).
- Q2 — pobranie nadpisuje nazwę i adres; pisownia jak w rejestrze; adres z `workingAddress`, a gdy brak — `residenceAddress`; ostrzeżenie „Firma nie jest czynnym podatnikiem”.
- Q3 — rachunki mojej firmy z Białej listy do zaznaczenia.
- Q4 — kontrahenci: dodawanie, edycja, usuwanie, NIP niezmienny, ostrzeżenie przy usuwaniu użytego, „+ Nowy kontrahent” w formularzu faktury.
- Q5 — NIP mojej firmy = NIP z tokena (tylko do odczytu); dane per profil `profiles/{nip}/`.
- Q6 — testy na mockach API ze zmyślonymi odpowiedziami ([docs/przyklady/biala-lista.md](../przyklady/biala-lista.md)); prawdziwy NIP użytkownika tylko do podejrzenia formatu w trakcie sesji, nigdy w kodzie ani dokumentacji.
- Q7 — pierwsze uruchomienie: token → Moja firma (automatyczne pobranie) → ekran główny.
- Dla JDG Biała lista zwraca imię i nazwisko, nie nazwę firmy — nazwę dopisuje się ręcznie.
- 2026-10-13: komunikat dla NIP-u spoza rejestru zmieniony na „Nie znaleziono firmy w Rejestrze WL” (zgłoszenie użytkownika).
- 2026-10-13: waluta rachunku wybierana z listy: PLN, EUR, USD, GBP, domyślnie PLN (zgłoszenie użytkownika); na fakturze nadal tylko rachunki PLN.

## Zadania

- ZAD-021 — klient Białej listy MF (moduł `whitelist`), mock i zmyślone odpowiedzi
- ZAD-022 — profil firmy: model, magazyn per NIP, migracja, usunięcie plików JSON i folderu
- ZAD-023 — UI: sekcja „Moja firma” (podgląd, edycja, pobieranie, rachunki)
- ZAD-024 — UI: „Kontrahenci” (lista, formularz, pobieranie) i „+ Nowy kontrahent” w formularzu faktury
- ZAD-025 — pierwsze uruchomienie (token → Moja firma) i braki w formularzu „Nowa faktura”
