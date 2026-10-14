# ZAD-034. PDF faktury: układ, strony, etykiety PL / PL+EN

- User story: US-013
- Status: Do zrobienia
- Szacunek: M

## Cel

Czysta logika (testowana w JVM), która z faktury (`Invoice`) i numeru KSeF (albo jego braku) układa strony A4 PDF: bloki tekstu, tabelę pozycji, podsumowanie, płatność i uwagi, z etykietami po polsku albo „polski / angielski” ([ADR 0010](../adr/0010-pdf-faktury.md)).

## Zakres

- [ ] `InvoicePdfLabels`: komplet etykiet PL i PL / EN (rodzaj faktury, sprzedawca / nabywca, numer KSeF, „Faktura nie wysłana do KSeF / Not yet submitted to KSeF”, daty, kolumny tabeli, podsumowanie, VAT w PLN, kurs, płatność, uwagi, „Strona x z y”); wybór wg kraju nabywcy (`Party.countryCode` ≠ PL → PL / EN)
- [ ] `InvoicePdfLayout`: model strony (A4 595×842 pt, marginesy) z elementami do narysowania (tekst z pozycją, czcionką i wyrównaniem, linie); łamanie długich tekstów (nazwy pozycji, adresy) wg szerokości — miara tekstu przekazywana z zewnątrz (`TextMeasurer`), w testach — prosta miara stałej szerokości
- [ ] Sekcje: nagłówek, strony, tabela pozycji (nagłówek tabeli powtarzany na kolejnych stronach), podsumowanie wg stawek (VAT w PLN, kurs), płatność, uwagi; stopka z numerem strony
- [ ] Formatowanie kwot, dat i ilości jak w aplikacji (`Formats`)
- [ ] Nazwa pliku: `Faktura_{numer z / zamienionym na -}.pdf`

## Definicja ukończenia

- [ ] Testy: etykiety PL i PL / EN, faktura w KSeF i niewysłana, faktura w EUR (VAT w PLN, kurs, uwagi), 60 pozycji → kilka stron z powtórzonym nagłówkiem tabeli i numeracją, łamanie długiej nazwy pozycji, nazwa pliku
- [ ] Zaktualizowana dokumentacja / słownik
