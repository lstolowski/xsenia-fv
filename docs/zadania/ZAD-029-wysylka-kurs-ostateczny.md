# ZAD-029. Podgląd i wysyłka faktury w walucie: kurs ostateczny w locie, szczegóły

- User story: US-011
- Status: Do zrobienia
- Szacunek: S

## Cel

Podgląd przed potwierdzeniem biometrią pokazuje kurs ostateczny dla dnia wysyłki; przy wysyłce kurs jest sprawdzany ponownie i wpisywany do XML; szczegóły faktury pokazują kurs i VAT w PLN.

## Zakres

- [ ] Podgląd: kurs ostateczny (`ExchangeRateService`, `issueDay` = dziś), kurs i VAT w PLN; gdy różny od roboczego — „Kurs zaktualizowany: 4,3775 → 4,3889”; szkic / faktura niewysłana dostaje kurs ostateczny
- [ ] `InvoiceSendService.send`: przed `Fa3XmlWriter.write` — kurs dla dnia wysyłki; różny od kursu z podglądu (wysyłka po północy) — szkic aktualizowany w locie; przy niepowodzeniu faktura niewysłana zapisywana z tym kursem
- [ ] Brak kursu (NBP niedostępne) w podglądzie lub przy wysyłce — wysyłka nie startuje, komunikat „Nie udało się pobrać kursu NBP”; faktura zostaje szkicem / niewysłaną
- [ ] Szczegóły faktury: dla waluty obcej kurs (`KursWaluty`) i VAT w PLN (`P_14_xW`)

## Definicja ukończenia

- [ ] Testy: `InvoiceSendServiceTest` — kurs dla dnia wysyłki, aktualizacja kursu po północy, brak kursu; PLN bez zapytań o kurs
- [ ] Sprawdzone na emulatorze z KSeF TEST: faktura w EUR wysłana, kurs w podglądzie i szczegółach
- [ ] Zaktualizowana dokumentacja / słownik
