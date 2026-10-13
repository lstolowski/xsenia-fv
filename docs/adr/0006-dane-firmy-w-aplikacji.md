# 0006. Dane mojej firmy, rachunki i kontrahenci w aplikacji (profil firmy)

- Status: Zaakceptowany
- Data: 2026-10-13
- Zastępuje [ADR 0004](0004-kontrahenci-w-pliku-json.md) (dane w ręcznie edytowanych plikach JSON)

## Kontekst

- Po testach na telefonie (2026-10-13) użytkownik rezygnuje z plików `company.json`, `banks.json`, `counterparties.json` we wspólnym folderze (ADR 0004). Dane mojej firmy, jej rachunki i kontrahentów chce wprowadzać i edytować **w aplikacji, w Ustawieniach**.
- Formularze mają umieć pobrać dane firmy po NIP-ie z Białej listy MF ([ADR 0007](0007-biala-lista-mf.md)).
- Na razie jest jedna „moja firma”, ale aplikacja ma działać „w kontekście / profilu firmy”, tak aby w przyszłości dało się dodać kolejne firmy i przełączać kontekst. W UI **nie** pokazujemy ani nie sugerujemy przełączania firm.
- NIP kontekstu KSeF jest zaszyty w tokenie KSeF (`…|nip-XXXXXXXXXX|…`); dziś NIP z `company.json` i NIP z tokena mogą się różnić (aplikacja tylko ostrzega).
- Szkic faktury i faktury niewysłane wskazują kontrahenta po NIP-ie, a rachunek po numerze; dane pobierane są z konfiguracji dopiero przy wysyłce.
- Kopia zapasowa Androida jest wyłączona (`allowBackup=false`, `dataExtractionRules`) — dane aplikacji giną przy odinstalowaniu.
- Założenie projektu: bez backendu i bez bazy danych (ADR 0002, ADR 0003).

## Rozważane opcje

Przechowywanie:

1. **Pliki JSON w pamięci wewnętrznej aplikacji** (zapis atomowy, jak szkic i faktury niewysłane) — spójne z ADR 0003, łatwe do testów na JVM, kilka–kilkadziesiąt rekordów.
2. Room / SQLite — nadmiarowe przy tej skali, łamie założenie „bez bazy danych”.
3. DataStore (Preferences / Proto) — niewygodne dla list i zagnieżdżonych obiektów.

NIP mojej firmy:

1. **NIP z tokena KSeF, tylko do odczytu** — jedno źródło prawdy, niezgodność NIP-u firmy i tokena przestaje być możliwa.
2. NIP wpisywany ręcznie i edytowalny; zmiana czyści lustro faktur.

Migracja dotychczasowych plików JSON:

1. **Bez migracji** — użytkownik wpisuje kilka rekordów ręcznie (z pomocą Białej listy).
2. Jednorazowy import z folderu.
3. Import i eksport (kopia zapasowa).

## Decyzja

Opcja 1 we wszystkich trzech punktach (decyzje użytkownika Q1, Q5 z 2026-10-13).

### Profil firmy

- **Profil** = wszystkie dane jednej mojej firmy, identyfikowany NIP-em. **Aktywny profil = NIP z tokena KSeF.** Token bez NIP-u kontekstu jest odrzucany przy zapisie („Token nie zawiera NIP-u firmy”).
- Katalog profilu w pamięci wewnętrznej aplikacji: `profiles/{nip}/`

| Ścieżka | Zawartość |
|---|---|
| `company.json` | moja firma (sprzedawca, Podmiot1) |
| `bank-accounts.json` | rachunki bankowe mojej firmy |
| `counterparties.json` | kontrahenci (nabywcy, Podmiot2) |
| `issue/` | szkic (`draft.json`) i faktura w przetwarzaniu (`pending-send.json`) |
| `unsent/` | faktury niewysłane (US-007) |
| `upo/` | UPO wysłanych faktur |
| `invoices/` | lustro faktur (ADR 0003); „Wyczyść cache” czyści lustro aktywnego profilu |

- Zmiana tokena na token z innym NIP-em przełącza profil — niczego nie usuwa (stare dane czekają w swoim katalogu). Ustawienia pokazują wyłącznie aktywny profil, bez listy profili.
- Jednorazowa migracja przy pierwszym starcie po aktualizacji: istniejące `issue/`, `unsent/`, `upo/`, `invoices/` z katalogu głównego przenoszone do `profiles/{nip z tokena}/`.

### Dane i walidacja

- Pola i reguły walidacji **jak w ADR 0004** (te same nazwy pól w JSON, ten sam zapis w FA(3)):
  - moja firma: `name`, `address.line1` wymagane; `address.line2`, `address.country` (domyślnie „Polska”), `email`, `phone` opcjonalne; `nip` z tokena,
  - rachunek: `name`, `account` (NRB, suma kontrolna) wymagane; `bank` opcjonalny; `currency` domyślnie `PLN` (na fakturze tylko PLN),
  - kontrahent: `name`, `nip` (suma kontrolna), `address.line1` wymagane; `address.line2`, `address.country`, `email` opcjonalne,
  - limity długości z FA(3) (nazwa i adres 512, bank 256, e-mail 255, telefon 16).
- Walidacja **w formularzu, przed zapisem** — niepoprawnych danych nie da się zapisać, więc przy odczycie nie ma już raportu błędów plików (`ConfigIssue`).
- Rachunki: wiele na firmę, w kolejności dodania; pierwszy jest domyślny na nowej fakturze (jak dotąd). Numer unikalny w profilu.
- Kontrahenci: NIP unikalny w profilu i **niezmienny po utworzeniu** (szkic i faktury niewysłane wskazują kontrahenta po NIP-ie); zmiana NIP-u = usunięcie i dodanie nowego kontrahenta.
- Usunięcie kontrahenta lub rachunku użytego w szkicu albo fakturze niewysłanej — ostrzeżenie w oknie potwierdzenia; taka faktura pokaże potem „wybierz kontrahenta” / „wybierz rachunek”.
- Wystawienie faktury wymaga: moja firma (nazwa, adres), co najmniej jeden rachunek PLN i jeden kontrahent; brakujące elementy pokazuje formularz „Nowa faktura” z przyciskiem „Uzupełnij”.

### Usunięte

- Wybór folderu (Storage Access Framework) w konfiguracji i Ustawieniach, wczytywanie plików, raport błędów plików i znaczek z liczbą błędów na zakładce Ustawienia. Trwałe uprawnienie do folderu jest zwalniane przy migracji; pliki w folderze użytkownika zostają nietknięte.
- Wzory plików w `docs/przyklady/` — zastąpione zmyślonymi danymi w plikach `.md` ([docs/przyklady/](../przyklady/README.md)).

### Moduły

- Model, walidacja i magazyn profilu (pliki JSON) — czysty Kotlin w module `invoice` (pakiet `profile`), testowany na JVM.
- W aplikacji `ProfileRepository` (aktywny profil, `StateFlow` danych) zastępuje `ConfigRepository`.

## Konsekwencje

- Dane wprowadzane wygodnie na telefonie, z pobieraniem z Białej listy; bez ręcznej edycji plików.
- **Odinstalowanie aplikacji usuwa dane firmy, rachunki i kontrahentów** (kopia zapasowa wyłączona, brak eksportu) — świadoma decyzja użytkownika (Q1, 2026-10-13). Eksport można dodać później osobną historyjką.
- Przyszłe dodanie kolejnych firm sprowadza się do UI wyboru profilu i osobnych tokenów — struktura danych jest gotowa.
- Zmiana NIP-u firmy wymaga tokena innej firmy; literówka w NIP-ie jest niemożliwa.
- Zadania: ZAD-022 (profil i magazyn), ZAD-023 (UI „Moja firma”), ZAD-024 (UI „Kontrahenci”), ZAD-025 (pierwsze uruchomienie). Historyjka: [US-010](../user-stories/US-010-dane-firmy-w-aplikacji.md).
