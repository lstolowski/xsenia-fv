# US-014. BIC (SWIFT) rachunku bankowego

- Priorytet: MVP
- Status: Zrobiona (2026-10-14)
- Zgłoszone: 2026-10-14

## Historyjka

Jako **właściciel firmy wystawiający faktury nabywcom zagranicznym** chcę **zapisać przy rachunku bankowym kod BIC (SWIFT) banku**, aby **kontrahent z zagranicy mógł bez problemu zrobić przelew**.

## Kryteria akceptacji

- [x] Formularz rachunku (Ustawienia → Moja firma → rachunek) ma pole „BIC (SWIFT)” — niewymagane, litery i cyfry; zapisywane wielkimi literami, bez spacji.
- [x] Format zgodny z FA(3) (`SWIFT_Type`): 8 albo 11 znaków — `[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?`; inny — błąd przy polu.
- [x] XML faktury: `<SWIFT>` w `Platnosc/RachunekBankowy`, zaraz po `<NrRB>` (kolejność ze schematu FA(3)); puste pole — bez elementu.
- [x] PDF: „BIC (SWIFT): …” pod numerem rachunku (etykieta taka sama po polsku i po angielsku).
- [x] Szczegóły i podgląd faktury: „BIC (SWIFT): …” pod numerem rachunku.
- [x] Podgląd nowej / niewysłanej faktury (przed wysyłką do KSeF), nabywca zagraniczny i rachunek bez BIC: żółte ostrzeżenie z ikoną „Nie wprowadziłeś BIC (SWIFT) do danych banku. Kontrahent może mieć problem z płatnością”. Wysyłka nie jest blokowana.
- [x] Nabywca polski — bez ostrzeżenia (BIC nie jest wymagany).

## Uwagi / pytania

- Rachunki zapisane wcześniej nie mają BIC — wczytują się bez zmian (pole opcjonalne w `bank-accounts.json`).
- W kodzie pole nazywa się `swift` — jak element FA(3) i `BankAccountInfo.swift` z parsera.

## Zadania

- ZAD-036 — BIC (SWIFT): model, walidacja, formularz, XML FA(3), PDF, ostrzeżenie w podglądzie
