# ZAD-035. PDF faktury: rysowanie, udostępnianie, ikony w aplikacji

- User story: US-013
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

Ikona udostępniania tworzy PDF faktury sprzedażowej i otwiera systemowy arkusz „Udostępnij” ([ADR 0010](../adr/0010-pdf-faktury.md)).

## Zakres

- [x] `InvoicePdfExporter`: strony z `InvoicePdfLayout` rysowane przez `PdfDocument` na `Canvas` (systemowa czcionka z polskimi znakami, `TextMeasurer` z `Paint.measureText`)
- [x] Plik w `cacheDir/pdf/` (poprzedni usuwany), `FileProvider` (`${applicationId}.files`, `res/xml/file_paths.xml` — tylko `cache/pdf/`), `Intent.ACTION_SEND` (`application/pdf`, `FLAG_GRANT_READ_URI_PERMISSION`), tytuł arkusza „Udostępnij fakturę”
- [x] Ikona `Icons.Filled.Share` (`ShareInvoiceButton`) w górnym pasku: szczegóły faktury sprzedażowej (XML z lustra, numer KSeF; zakupowe — bez ikony), ekran po udanej wysyłce (faktura z podglądu, numer KSeF z wyniku), podgląd nowej / niewysłanej faktury (bez numeru KSeF)
- [x] Generowanie w tle (`Dispatchers.IO`, kółko w miejscu ikony); błąd — „Nie udało się utworzyć PDF faktury.”
- [x] US-005: dopisek o zmianie decyzji „bez udostępniania”. Po obejrzeniu PDF poprawione: stopka „Strona 1 z 1 / Page 1 of 1”, bez dublowania etykiet równych w obu językach („Bank”, „VAT”), uwaga „Uwagi” bez powtórzonego klucza; „Podsumowanie” wg stawek tylko przy kilku stawkach; przeliczenie sum na PLN mniejszą czcionką obok kwot w walucie obcej, zamiast wiersza „VAT w PLN” (decyzje użytkownika)

## Definicja ukończenia

- [x] Sprawdzone na emulatorze z KSeF TEST (2026-10-14): szczegóły faktury IT/UE/… (nabywca z Niemiec, PL / EN, numer KSeF), 10/09/2026 (polski nabywca, EUR, tylko PL, VAT w PLN) i podgląd faktury niewysłanej 11/09/2026 („Faktura nie wysłana do KSeF / Not yet submitted to KSeF”); arkusz „Udostępnij” z plikiem `Faktura_….pdf`; pliki pobrane z emulatora i obejrzane jako obraz. Ikona na ekranie po wysyłce — ten sam komponent, nie sprawdzana (wymagałaby kolejnej faktury w KSeF TEST)
- [x] Zaktualizowana dokumentacja / słownik
