# ZAD-029. Podgląd i wysyłka faktury w walucie: kurs ostateczny w locie, szczegóły

- User story: US-011
- Status: Zrobione (2026-10-14)
- Szacunek: S

## Cel

Podgląd przed potwierdzeniem biometrią pokazuje kurs ostateczny dla dnia wysyłki; przy wysyłce kurs jest sprawdzany ponownie i wpisywany do XML; szczegóły faktury pokazują kurs i VAT w PLN.

## Zakres

- [x] Podgląd: kurs ostateczny (`ExchangeRateService.rateFor`, `issueDay` = dziś) pobierany po „Podgląd” (przycisk z kółkiem ładowania); w podglądzie „Kurs NBP EUR na dzień wysyłki: …”, a gdy różny od roboczego — „Kurs zaktualizowany na dzień wysyłki: 4,3775 → 4,3889 (tabela …)”; `InvoiceFormCheck.check(…, exchangeRate)` z kursem ostatecznym; wysyłany jest dokładnie szkic z podglądu. Kurs ostateczny **nie** jest zapisywany w szkicu ani w fakturze niewysłanej — przy każdej wysyłce i tak liczy się od nowa, a w formularzu zostaje kurs roboczy
- [x] `InvoiceSendService.send`: przed `Fa3XmlWriter.write` — kurs dla dnia wysyłki w Polsce (`FinalRateSource`, z `ExchangeRateService`); różny od kursu z podglądu (wysyłka po północy) — podmieniany w locie; przy niepowodzeniu wysyłki faktura niewysłana zapisywana z formularzem (kurs roboczy — kolejna wysyłka znów pobiera kurs ostateczny)
- [x] Brak kursu (NBP niedostępne): w formularzu „Podgląd wymaga kursu NBP na dzień wysyłki.” z przyczyną; przy wysyłce — nowy stan `SendState.RateUnavailable` („Nie wysłano faktury … — brak kursu NBP na dzień wysyłki.”, „Spróbuj ponownie”, „Popraw fakturę”, „Zamknij”); nic nie trafia do KSeF, faktura zostaje szkicem / niewysłaną
- [x] Szczegóły faktury (i podgląd): „Kurs waluty: 4,3728” w nagłówku (`Invoice.exchangeRate`), „VAT w PLN” w podsumowaniu (`Invoice.vatInPlnTotal`)

## Definicja ukończenia

- [x] Testy: `InvoiceSendServiceTest` — kurs dla dnia wysyłki w Polsce (22:30 UTC = następny dzień), podmiana kursu roboczego na ostateczny w XML, brak kursu (nic nie wysłane, szkic zostaje); PLN bez zapytań o kurs; `Fa3XmlWriterTest` — `exchangeRate`, `vatInPlnTotal`
- [x] Sprawdzone na emulatorze z KSeF TEST (2026-10-14): faktura 10/09/2026 w EUR (dane zmyślone) — w podglądzie kurs na dzień wysyłki 4,3728 (tabela 189/A/NBP/2026 z 29.09.2026, dostawa 30.09), VAT w PLN i uwaga „Kurs waluty”; przyjęta przez KSeF TEST z UPO; w szczegółach po synchronizacji kurs i VAT w PLN. Komunikat „Kurs zaktualizowany” i stan braku kursu — sprawdzone tylko testami
- [x] Zaktualizowana dokumentacja / słownik
