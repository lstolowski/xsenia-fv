# Przykładowe dane do developmentu

Zmyślone dane do testów, fixture'ów, dokumentacji i wprowadzania na emulatorze oraz w KSeF TEST (zasady w [AGENTS.md](../../AGENTS.md)).
Wszystkie NIP-y i numery rachunków mają poprawną sumę kontrolną, ale nie należą do nikogo, kogo znamy — **nie dopisuj tu prawdziwych danych** (np. z Białej listy, z faktur PROD).

| Plik | Zawartość |
|---|---|
| [nip.md](nip.md) | testowe NIP-y |
| [firma.md](firma.md) | testowa „Moja firma” (sprzedawca) |
| [kontrahenci.md](kontrahenci.md) | testowi kontrahenci (nabywcy) |
| [rachunki.md](rachunki.md) | testowe rachunki bankowe (NRB) |
| [biala-lista.md](biala-lista.md) | zmyślone odpowiedzi API Białej listy MF (do mocków, ADR 0007) |

Dane osobowe i adresowe:

- nazwy i osoby: „Test”, „Testowy”, „Testowa”, „Jan Testowy”, „Testowa Sp. z o.o.” albo Lorem ipsum,
- adresy: „ul. Testowa 1”, „00-001 Testowo”; e-maile w domenie `example.com`; telefony `+48 000 000 000`,
- nazwy towarów i usług: „Usługa testowa”, Lorem ipsum.

Do 2026-10-13 w tym katalogu były wzory plików `company.json`, `banks.json`, `counterparties.json` (ADR 0004). Od US-010 dane firmy, rachunki i kontrahenci są wprowadzane w aplikacji ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)); pliki i ich parser usunięto w ZAD-022.
