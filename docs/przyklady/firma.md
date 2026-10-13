# Testowa „Moja firma”

Sprzedawca (Podmiot1) do testów i fixture'ów. Pola jak w formularzu „Moja firma” ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)).

| Pole | Wartość |
|---|---|
| Nazwa (`name`) | Jan Testowy Usługi Testowe |
| NIP (`nip`) | `1996845518` |
| Adres, linia 1 (`address.line1`) | ul. Testowa 1 |
| Adres, linia 2 (`address.line2`) | 00-001 Testowo |
| Kraj (`address.country`) | Polska |
| E-mail (`email`) | jan.testowy@example.com |
| Telefon (`phone`) | +48 000 000 000 |

Rachunki: [rachunki.md](rachunki.md).

Na emulatorze i w KSeF TEST NIP „Mojej firmy” pochodzi z tokena KSeF TEST (kontekst z `secrets/ksef-test.properties`), więc jest inny niż powyżej; pozostałe pola wpisuj z tej tabeli. Jeśli do sprawdzenia pobierania z Białej listy użyjesz prawdziwego NIP-u, **przed wysłaniem jakiejkolwiek faktury do KSeF TEST nadpisz pobrane dane tymi z tabeli**.
