# Testowe rachunki bankowe

Zmyślone numery NRB (26 cyfr, poprawna suma kontrolna mod 97, bez prefiksu `PL`) — tak zapisywane w danych aplikacji i w polu `NrRB` faktury.
**Nigdy nie używaj prawdziwego rachunku użytkownika** (np. pobranego z Białej listy).

| Nazwa (`name`) | Bank (`bank`) | Numer (`account`) | Waluta (`currency`) |
|---|---|---|---|
| Konto testowe Toyota | Toyota Bank | `85216097583216850246002152` | PLN |
| Konto testowe Santander | Santander Bank Polska | `55109004081797992973253811` | PLN |
| Konto testowe SGB-Bank | SGB-Bank | `22161029092633573189646018` | PLN |
| Konto testowe NBP | NBP | `71101075753723914707418989` | PLN |
| Konto testowe EUR | Toyota Bank | `83216097587877893287921742` | EUR |
| Konto testowe USD | Santander Bank Polska | `14109004081809679290810033` | USD |
| Konto testowe GBP | SGB-Bank | `07161029099075793834701746` | GBP |

Przypadki do testów walidacji: `1` (za krótki), `85216097583216850246002153` (zła suma kontrolna), waluta `EUR` (do US-011 rachunek pomijany na fakturze — faktury tylko w PLN; od US-011 — rachunek do faktur w EUR).
