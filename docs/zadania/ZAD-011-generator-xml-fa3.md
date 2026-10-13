# ZAD-011. Generator XML FA(3), wyliczenia, sugestia numeru i daty dostawy

- User story: US-006
- Status: Zrobione (2026-10-12)
- Szacunek: M

## Cel

Moduł `invoice` buduje poprawny XML FA(3) faktury VAT 23%.

## Zakres

- [x] `Fa3XmlWriter` (moduł `invoice`, pakiet `issue`) — mapowanie: Podmiot1 (company), Podmiot2 (kontrahent, `KodKraju` PL, `JST`/`GV` = 2), `P_1`, `P_2`, `P_6` (tylko gdy różna od `P_1`), `FaWiersz` (`P_7`, `P_8A`, `P_8B`, `P_9A`, `P_11`, `P_12`=23), `P_13_1`, `P_14_1`, `P_15`, `RodzajFaktury` VAT, `Platnosc` (`FormaPlatnosci` 6, `TerminPlatnosci` +21 dni, `RachunekBankowy`)
- [x] `InvoiceCalculator`: kwoty `BigDecimal`, HALF_UP do groszy; netto pozycji = ilość × cena; **VAT od sumy netto** (decyzja użytkownika 2026-10-12), brutto pozycji tylko informacyjnie — możliwa różnica 1 gr względem `P_15` przy wielu pozycjach
- [x] `InvoiceNumbering`: sugestia `NN/MM/RRRR` = max NN z numerów pasujących do `/MM/RRRR` (miesiąc daty dostawy) + 1; duplikaty; ostrzeżenie o niezgodności miesiąca; `DefaultDates`: domyślna data dostawy wg reguły 1.–15. dnia
- [x] Przypadek testowy z KSeF TEST: faktura `01/10/2026` z datą dostawy 2026-09-30 (błędnie ponumerowana) → dla dostawy we wrześniu sugestia `01/09/2026`, dla dostawy w październiku `02/10/2026`
- [x] Walidacja wygenerowanego XML względem XSD FA(3) w testach + odczyt z powrotem parserem z ZAD-007 (round trip)
- [x] `DraftValidator`: numer (brak, długość, duplikat, ostrzeżenie o miesiącu), data wystawienia nie z przyszłości, pozycje (nazwa, jednostka, ilość > 0, cena ≥ 0, precyzja wg XSD), suma > 0
- [x] XML budowany tekstowo (deterministyczne bajty na JVM i Androidzie), escapowanie znaków, normalizacja spacji (`xsd:token`); `P_6` tylko gdy data dostawy ≠ data wystawienia

## Definicja ukończenia

- [x] Testy (24: kalkulator, numeracja, daty, walidacja, generator + XSD)
- [x] Sprawdzone na KSeF TEST — przy wysyłce w ZAD-012 (`KsefInvoiceSendIntegrationTest`)
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja / słownik
