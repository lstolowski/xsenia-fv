# ZAD-034. PDF faktury: układ, strony, etykiety PL / PL+EN

- User story: US-013
- Status: Zrobione (2026-10-14)
- Szacunek: M

## Cel

Czysta logika (testowana w JVM), która z faktury (`Invoice`) i numeru KSeF (albo jego braku) układa strony A4 PDF: bloki tekstu, tabelę pozycji, podsumowanie, płatność i uwagi, z etykietami po polsku albo „polski / angielski” ([ADR 0010](../adr/0010-pdf-faktury.md)).

## Zakres

- [x] `InvoicePdfLabels` (pakiet `pl.xsenia.fv.pdf`, teksty dokumentu w kodzie, nie w `strings.xml`): komplet etykiet PL i PL / EN (rodzaj faktury, sprzedawca / nabywca, numer KSeF, „Faktura nie wysłana do KSeF / Not yet submitted to KSeF”, daty, kolumny tabeli, podsumowanie, VAT w PLN, kurs, płatność, uwagi, „Strona x z y”); wybór wg kraju nabywcy (`Party.countryCode` ≠ PL → PL / EN)
- [x] `InvoicePdfLayout`: `PdfPage` (A4 595×842 pt, margines 40 pt) z `PdfElement.Text` (pozycja linii bazowej, `PdfTextStyle`, wyrównanie) i `PdfElement.Line`; `TextMeasurer.wrap` — łamanie po słowach, cięcie zbyt długich słów, zachowane `\n`; miara tekstu z zewnątrz (w testach — stała szerokość)
- [x] Sekcje: nagłówek (rodzaj, numer, numer KSeF albo „Faktura nie wysłana do KSeF”, daty, waluta), sprzedawca i nabywca obok siebie (z krajem PL / EN, np. „Niemcy / Germany”), tabela pozycji (8 kolumn, nagłówek PL nad EN, powtarzany na kolejnych stronach, nagłówek sekcji nie zostaje sam na dole strony), podsumowanie wg stawek (VAT w PLN, kurs), płatność, uwagi; stopka „Strona x z y”
- [x] Formatowanie kwot, dat i ilości jak w aplikacji (`Formats`, nowe `amount` i `unitPrice`); dla nabywcy zagranicznego kwoty z kodem waluty (`PLN` zamiast „zł”)
- [x] Nazwa pliku: `Faktura_{numer}.pdf` — znaki spoza `[A-Za-z0-9._-]` zamienione na `-` (`Faktura_01-09-2026.pdf`)

## Definicja ukończenia

- [x] Testy: etykiety PL i PL / EN, faktura w KSeF i niewysłana, faktura w EUR (VAT w PLN, kurs, uwagi), 60 pozycji → kilka stron z powtórzonym nagłówkiem tabeli i numeracją, łamanie długiej nazwy pozycji, nazwa pliku
- [x] Zaktualizowana dokumentacja / słownik
