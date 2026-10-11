# ZAD-011. Generator XML FA(3), wyliczenia, sugestia numeru i daty dostawy

- User story: US-006
- Status: Do zrobienia
- Szacunek: M

## Cel

Moduł `invoice` buduje poprawny XML FA(3) faktury VAT 23%.

## Zakres

- [ ] Mapowanie: Podmiot1 (company), Podmiot2 (kontrahent, `KodKraju` PL), `P_1`, `P_2`, `P_6` (tylko gdy różna od `P_1`), `FaWiersz` (`P_7`, `P_8A`, `P_8B`, `P_9A`, `P_11`, `P_12`=23), `P_13_1`, `P_14_1`, `P_15`, `RodzajFaktury` VAT, `Platnosc` (`FormaPlatnosci` 6, `TerminPlatnosci` +21 dni, `RachunekBankowy`)
- [ ] Kwoty `BigDecimal`, zaokrąglenia do groszy; VAT liczony od sumy netto (wartości brutto pozycji tylko do wyświetlenia — możliwa różnica 1 gr względem `P_15`, do decyzji przy implementacji)
- [ ] Sugestia numeru `NN/MM/RRRR` na podstawie lustra: NN = max NN z numerów faktur sprzedażowych pasujących do `/MM/RRRR` + 1; walidacja zgodności miesiąca w numerze z datą dostawy; domyślna data dostawy wg reguły 1.–15. dnia
- [ ] Przypadek testowy z KSeF TEST: faktura `01/10/2026` z datą dostawy 2026-09-30 (błędnie ponumerowana) → dla dostawy we wrześniu sugestia `01/09/2026`, dla dostawy w październiku `02/10/2026`
- [ ] Walidacja wygenerowanego XML względem XSD FA(3) w testach jednostkowych

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
