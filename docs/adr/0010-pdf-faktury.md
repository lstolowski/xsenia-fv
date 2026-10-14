# 0010. PDF faktury sprzedażowej generowany w aplikacji i udostępniany przez Androida

- Status: Zaakceptowany
- Data: 2026-10-14

## Kontekst

- [US-013](../user-stories/US-013-pdf-kod-qr.md): nabywca zagraniczny nie odbierze faktury z KSeF ([US-012](../user-stories/US-012-nabywca-zagraniczny.md)) — trzeba mu wysłać PDF. Przy [US-005](../user-stories/US-005-szczegoly-faktury.md) użytkownik zrezygnował z udostępniania „przynajmniej na razie” — ta decyzja zmienia to dla PDF faktur sprzedażowych.
- Brak backendu ([ADR 0002](0002-natywna-aplikacja-android.md)) — PDF musi powstać na telefonie, także bez sieci.
- Decyzje użytkownika (2026-10-14): bez kodu QR (na razie), faktury sprzedażowe wysłane i niewysłane, język wg nabywcy, prosty układ A4 bez logo, udostępniany sam PDF, plik tymczasowy, ikona udostępniania; niewysłana — „Faktura nie wysłana do KSeF”, udostępniana z podglądu ([analiza 06](../analiza/06-pdf-kod-qr.md)).

## Rozważane opcje

1. **`android.graphics.pdf.PdfDocument`** — w systemie, bez bibliotek; rysowanie na `Canvas`.
2. HTML → PDF przez `WebView` / `PrintManager` — łatwy układ, ale asynchroniczne, zależne od WebView, trudne do testowania.
3. Biblioteka PDF (np. iText, OpenPDF) — duże zależności, licencje (AGPL / LGPL).

## Decyzja

**Opcja 1.**

- Treść z modelu `Invoice` (parser XML): faktura w KSeF — XML z lustra; faktura niewysłana i nowa — XML z podglądu (`Fa3XmlWriter`, kurs na dzień wysyłki). PDF pokazuje to samo co ekran szczegółów / podgląd.
- Układ jako czysta logika (`InvoicePdfLayout`: bloki tekstu, tabela pozycji, podział na strony A4), testowany w JVM; rysowanie (`InvoicePdfRenderer`) — cienka warstwa na `Canvas`, sprawdzana na emulatorze.
- Etykiety: polski nabywca — po polsku; zagraniczny (kraj nabywcy ≠ PL) — „polski / angielski”.
- Numer KSeF w nagłówku; faktura bez numeru — „Faktura nie wysłana do KSeF” (dla zagranicznego: „Faktura nie wysłana do KSeF / Not yet submitted to KSeF”).
- Plik w `cacheDir/pdf/` (czyści go „Wyczyść cache”), udostępniany przez `FileProvider` i `Intent.ACTION_SEND` (`application/pdf`); nazwa np. `Faktura_10-09-2026.pdf`.
- Ikona `Icons.Filled.Share` (trzy kropki połączone liniami): szczegóły faktury sprzedażowej, ekran po udanej wysyłce, podgląd (nowa / niewysłana).
- Bez nowych zależności. Kod QR KSeF (KOD I) — odłożony; gdy wróci: ZXing core i link z [analizy 06](../analiza/06-pdf-kod-qr.md).

## Konsekwencje

- Fakturę dla nabywcy zagranicznego można wysłać z telefonu dowolną aplikacją (e-mail, komunikator, Dysk).
- Układ jest własny, nie oficjalna wizualizacja MF; bez kodu QR nabywca nie zweryfikuje faktury w KSeF skanem.
- Zadania: ZAD-034, ZAD-035.
