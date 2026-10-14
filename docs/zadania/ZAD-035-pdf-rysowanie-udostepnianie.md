# ZAD-035. PDF faktury: rysowanie, udostępnianie, ikony w aplikacji

- User story: US-013
- Status: Do zrobienia
- Szacunek: M

## Cel

Ikona udostępniania tworzy PDF faktury sprzedażowej i otwiera systemowy arkusz „Udostępnij” ([ADR 0010](../adr/0010-pdf-faktury.md)).

## Zakres

- [ ] `InvoicePdfRenderer`: strony z `InvoicePdfLayout` rysowane przez `PdfDocument` na `Canvas` (czcionka z polskimi znakami, `TextMeasurer` z `Paint`)
- [ ] Plik w `cacheDir/pdf/` (zastępowany przy kolejnym udostępnieniu), `FileProvider` w `AndroidManifest.xml`, `Intent.ACTION_SEND` (`application/pdf`, `FLAG_GRANT_READ_URI_PERMISSION`), tytuł arkusza „Udostępnij fakturę”
- [ ] Ikona `Icons.Filled.Share`: szczegóły faktury sprzedażowej (XML z lustra, numer KSeF), ekran po udanej wysyłce (faktura z podglądu, numer KSeF z wyniku), podgląd nowej / niewysłanej faktury (bez numeru KSeF)
- [ ] Generowanie w tle (bez blokowania UI); błąd — komunikat po polsku
- [ ] US-005: dopisek o zmianie decyzji „bez udostępniania”

## Definicja ukończenia

- [ ] Sprawdzone na emulatorze z KSeF TEST: PDF faktury w PLN dla polskiego nabywcy, w EUR dla nabywcy z UE (etykiety PL / EN) i faktury niewysłanej; arkusz „Udostępnij” z PDF; podgląd PDF w przeglądarce plików
- [ ] Zaktualizowana dokumentacja / słownik
