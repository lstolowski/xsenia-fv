# PDF faktury z kodem QR KSeF i udostępnianie — analiza (US-013)

Status: **zakończona 2026-10-14** — decyzje w [US-013](../user-stories/US-013-pdf-kod-qr.md), [ADR 0010](../adr/0010-pdf-faktury.md)

> Decyzja użytkownika (2026-10-14): **na razie bez kodu QR** — PDF pokazuje tylko numer KSeF. Sekcja 1 zostaje jako materiał na później.

Kontekst: decyzja Q5 przy [US-012](../user-stories/US-012-nabywca-zagraniczny.md) — nabywca bez polskiego NIP-u nie odbierze faktury z KSeF, trzeba mu ją przesłać poza KSeF (PDF z kodem QR). Przy [US-005](../user-stories/US-005-szczegoly-faktury.md) (2026-10-12) użytkownik zrezygnował z udostępniania „przynajmniej na razie”.

## 1. Kod QR KSeF (KOD I) — odłożone

Źródło: [ksef-api/kody-qr.md](https://github.com/CIRFMF/ksef-api/blob/main/kody-qr.md) (dawniej ksef-docs; stan z 21.08.2025, sprawdzone 2026-10-14).

- Faktura przekazywana poza KSeF (PDF, wydruk, e-mail) ma kod QR z linkiem do weryfikacji i pobrania jej z KSeF. Kod generuje aplikacja wystawcy, lokalnie; zgodny z ISO/IEC 18004.
- Tryb online (nasz) — **jeden kod: KOD I**. KOD II (certyfikat KSeF typu Offline) dotyczy tylko trybu offline — poza zakresem.
- Link: `{adres QR}/invoice/{NIP sprzedawcy}/{P_1 jako DD-MM-RRRR}/{SHA-256 pliku XML w Base64URL}`:
  - adres QR: TEST `https://qr-test.ksef.mf.gov.pl`, PROD `https://qr.ksef.mf.gov.pl` — już w `KsefEnvironment.qrBaseUrl`,
  - skrót pliku: `invoiceHash` z metadanych KSeF (Base64) zamieniony na Base64URL (`+` → `-`, `/` → `_`, bez `=`),
  - przykład: `https://qr-test.ksef.mf.gov.pl/invoice/1111111111/01-02-2026/UtQp9Gpc51y-u3xApZjIjgkpZ01js-J8KflSPW8WzIE`.
- Pod kodem: **numer KSeF** faktury (dla faktury bez numeru — napis „OFFLINE”, u nas nie występuje: PDF tylko dla faktur już w KSeF).
- Działa także dla faktur zakupowych (NIP sprzedawcy, data i skrót są w metadanych).

## 2. PDF na Androidzie

- `android.graphics.pdf.PdfDocument` (w systemie, bez bibliotek): rysowanie strony A4 (595×842 pt) na `Canvas` — tekst, linie, obraz kodu QR; wiele stron przy dużej liczbie pozycji.
- Bez kodu QR nie potrzeba żadnej biblioteki (ZXing — dopiero gdy wrócimy do kodu QR).
- Treść z modelu `Invoice` (parser XML, jak ekran szczegółów i podgląd): faktura w KSeF — XML z lustra; faktura niewysłana — XML wygenerowany z formularza, jak w podglądzie (z kursem na dzień wysyłki).
- Testy: układ (podział na strony, pola) jako czysta logika w JVM; rysowanie — na emulatorze.

## 3. Udostępnianie

- Systemowy arkusz „Udostępnij” (`Intent.ACTION_SEND`, `FileProvider`) — e-mail, komunikatory, Dysk, „Zapisz w Plikach”. Plik tymczasowy w `cacheDir` (usuwany przez „Wyczyść cache”).
- Tylko PDF (decyzja Q4 a); ikona udostępniania (trzy kropki połączone liniami — `Icons.Filled.Share`), Q6.
- Nazwa pliku np. `Faktura_10-09-2026_9511909992-20261014-495EE8C00000-64.pdf` (bez `/`).

## 4. Wpływ na aplikację

| Obszar | Zmiana |
|---|---|
| Nowy kod w `app` | generator PDF (`PdfDocument`), układ stron (czysta logika, testy JVM), etykiety PL / PL+EN, nazwa pliku |
| UI | ikona „Udostępnij” w szczegółach faktury sprzedażowej, na ekranie po wysłaniu i dla faktury niewysłanej (Q5, runda 2) |
| `AndroidManifest.xml` | `FileProvider` dla plików w `cacheDir` |
| Zależności | brak nowych |

## 5. Poza zakresem (propozycja)

- Kod QR (KOD I i KOD II), tryb offline, faktury zakupowe (Q1 b).
- Wysyłka e-mailem z aplikacji (robi to aplikacja wybrana w arkuszu „Udostępnij”).
- Wierne odwzorowanie oficjalnej wizualizacji MF (generator PDF MF działa w przeglądarce / TypeScript).
