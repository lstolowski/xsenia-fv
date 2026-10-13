# 0003. Lokalne lustro faktur w plikach i synchronizacja z KSeF

- Status: Zaakceptowany
- Data: 2026-10-11
- Zmieniony 2026-10-13: lustro leży w katalogu profilu firmy `profiles/{nip}/invoices/` ([ADR 0006](0006-dane-firmy-w-aplikacji.md))

## Kontekst

- Obie listy (sprzedażowa i zakupowa, R1) mają być filtrowane po miesiącu **daty dostawy** (D2), a nie daty wystawienia. Fakturę wystawioną 1. dnia miesiąca za poprzedni miesiąc pokazujemy w poprzednim miesiącu.
- Skala (R2): zwykle 7–10 faktur zakupowych i 1 sprzedażowa miesięcznie; historia od **01.04.2026**.
- KSeF (`POST /invoices/query/metadata`) filtruje tylko po dacie wystawienia (`Issue`), przyjęcia (`Invoicing`) i trwałego zapisu (`PermanentStorage`). **Metadane nie zawierają daty dostawy.**
- Data dostawy jest tylko w treści XML faktury:
  - `Fa/P_6` — wspólna data dostawy/wykonania usługi, **tylko gdy różni się od daty wystawienia**,
  - `Fa/OkresFa/P_6_Od`, `P_6_Do` — okres, którego dotyczy faktura,
  - `FaWiersz/P_6A` — data dla pojedynczej pozycji (gdy pozycje mają różne daty),
  - brak powyższych → data dostawy = data wystawienia `P_1`.
- Limity PROD: lista metadanych 20/h, pobranie XML 64/h, eksport paczki 20/h (jedna paczka = wiele faktur XML).
- Faktury w KSeF są niezmienne — raz pobrany XML nigdy się nie zmieni.
- Numer kolejnej faktury wyliczamy z faktur sprzedażowych danego miesiąca dostawy (C6) — potrzebujemy ich numerów niezależnie od daty wystawienia.

## Rozważane opcje

1. **Zapytanie na żądanie** dla każdego oglądanego miesiąca: metadane z zakresu dat wystawienia [M, M+1], potem XML każdej faktury i filtr po dacie dostawy. Proste, ale każdy miesiąc to wiele żądań; szybko wyczerpuje limit XML (64/h).
2. **Lokalne lustro w plikach z synchronizacją przyrostową**: aplikacja przechowuje metadane + XML wszystkich faktur (sprzedażowych i zakupowych) od daty początkowej; synchronizacja przyrostowa po `PermanentStorage` (zalecana przez MF); pierwsze pobranie przez eksport paczki. Wszystkie filtry (miesiąc dostawy, NIP, nazwa) działają lokalnie i natychmiast.

## Decyzja

**Opcja 2.**

- Data początkowa lustra: **2026-04-01** (stała w konfiguracji aplikacji).
- Zapytania o metadane dzielone na okna ≤ 100 dni (limit API, zweryfikowany na TEST: `dateRange must not exceed 100 days`).
- Przy obecnej skali (~10 faktur/mies.) pierwsze pobranie to ok. 60–70 faktur — eksportem paczki (jedno żądanie zamiast ~70 pobrań XML przy limicie 64/h). Synchronizacje przyrostowe pobierają pojedyncze XML.
- Synchronizacja przyrostowa: zapytanie o metadane (`PermanentStorage`) od punktu kontrolnego do „teraz” — także zakres po HWM, by najnowsze faktury były widoczne od razu (scenariusz 2 z dokumentacji HWM); punkt kontrolny = HWM z odpowiedzi; duplikaty pomijane po numerze KSeF.
- Automatyczna synchronizacja najwyżej co 15 minut; ręczna zawsze.
- Implementacja: moduł `mirror` (`InvoiceMirror`, `InvoiceSynchronizer`), w aplikacji `InvoiceSyncService`.

- Struktura plików (pamięć wewnętrzna aplikacji): `invoices/{sales|purchase}/{ksefNumber}.xml` + indeks `invoices/{sales|purchase}/index.json` (metadane + wyliczona data dostawy) + `sync-state.json` (znacznik ostatniej synchronizacji).
- Synchronizacja: przy otwarciu aplikacji (jeśli minęło > N minut) i ręcznie („Odśwież z KSeF”); po wysłaniu nowej faktury jej XML trafia od razu do lustra.
- Data dostawy dla filtra: `P_6` → `P_6_Do` → najpóźniejsza `P_6A` → `P_1`.
- To nadal „bez bazy danych”: pliki JSON/XML, możliwe do usunięcia i odtworzenia z KSeF w każdej chwili („Wyczyść cache” w ustawieniach).

## Konsekwencje

- Pierwsze uruchomienie pobiera historię — może potrwać (eksport asynchroniczny).
- Filtrowanie, wyszukiwanie, podsumowania i numeracja działają offline i natychmiast.
- Faktury zakupowe wystawione przez kontrahenta długo po dacie dostawy pojawią się po synchronizacji w „starym” miesiącu — to poprawne zachowanie.
- Trzeba obsłużyć parsowanie FA(2) i FA(3) (faktury zakupowe od innych systemów).
