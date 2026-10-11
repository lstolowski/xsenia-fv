# 0001. Backend pośredniczący w komunikacji z KSeF

- Status: Odrzucony (2026-10-11 — zmiana założeń: aplikacja tylko na Androida, patrz [0002](0002-natywna-aplikacja-android.md))
- Data: 2026-10-11

## Kontekst

- API KSeF 2.0 nie zwraca nagłówków CORS (sprawdzone na TEST 2026-10-11) — przeglądarka nie może go wywołać bezpośrednio.
- Token KSeF jest sekretem pozwalającym wystawiać faktury w imieniu podatnika; nie powinien być przechowywany w przeglądarce.
- Limity API (lista faktur: 20 zapytań/h) wymagają cache.
- Wymaganie: brak bazy danych.

Szczegóły: [analiza/01-ksef-api.md](../analiza/01-ksef-api.md), [analiza/02-warianty-architektury.md](../analiza/02-warianty-architektury.md).

## Rozważane opcje

1. Sam frontend React (SPA) — niemożliwe (CORS).
2. React + cienki backend TypeScript/Node.js — jeden język, prosty, własny klient KSeF.
3. React + backend Java/Spring Boot z oficjalnym `ksef-client-java` — oficjalna biblioteka, ale dwa ekosystemy.
4. React opakowany Capacitorem (APK) z natywnym HTTP — działa bez serwera tylko na telefonie.

## Decyzja

Proponowana: **opcja 2** — React (Vite, TypeScript) + cienki backend Node.js/TypeScript trzymający token KSeF, wykonujący uwierzytelnianie, szyfrowanie, budowę XML FA(3) i cache w pamięci. Logika klienta KSeF w module niezależnym od Node, by umożliwić późniejsze przejście na opcję 4.

## Konsekwencje

- Trzeba uruchomić i zabezpieczyć backend dostępny z telefonu (VPN/hasło).
- Własna implementacja ok. 10 endpointów KSeF i budowy XML FA(3) — konieczne testy na środowisku TEST.
- Brak bazy danych: cache ginie przy restarcie, szkice w `localStorage`.
