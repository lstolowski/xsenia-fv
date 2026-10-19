# Dokumentacja xsenia-fv

| Katalog / plik | Zawartość |
|---|---|
| [analiza/](analiza/) | Analizy: KSeF API 2.0, warianty architektury, warsztat |
| [adr/](adr/README.md) | Architecture Decision Records — decyzje architektoniczne |
| [user-stories/](user-stories/README.md) | Historyjki użytkownika (co i po co) |
| [zadania/](zadania/README.md) | Zadania implementacyjne (jak) — rozpisywane z user stories |
| [srodowisko/](srodowisko/) | Przygotowanie środowiska deweloperskiego |
| [przyklady/](przyklady/README.md) | Zmyślone dane do developmentu: NIP-y, firma, kontrahenci, rachunki, odpowiedzi Białej listy i NBP |
| [slownik.md](slownik.md) | Słownik pojęć i mapowanie na nazwy w kodzie |
| [audyt-security.md](audyt-security.md) | Code review i statyczny audyt bezpieczeństwa aplikacji |
| [code-review-1.md](code-review-1.md) | Code review 1: ocena uwag `TODO CODE REVIEWED` i plan poprawek REV-01..31 |

## Analiza

1. [01-ksef-api.md](analiza/01-ksef-api.md) — co wynika z dokumentacji KSeF API 2.0 dla naszych funkcji
2. [02-warianty-architektury.md](analiza/02-warianty-architektury.md) — web, backend, Android
3. [03-pytania-warsztat.md](analiza/03-pytania-warsztat.md) — warsztat: ustalenia i pytania otwarte
4. [04-faktury-w-walucie-obcej.md](analiza/04-faktury-w-walucie-obcej.md) — faktury w EUR/USD/GBP: pola FA(3), reguła kursu, API NBP
5. [05-nabywca-zagraniczny.md](analiza/05-nabywca-zagraniczny.md) — nabywca bez polskiego NIP-u: Podmiot2 w FA(3), np I / np II, VIES
6. [06-pdf-kod-qr.md](analiza/06-pdf-kod-qr.md) — PDF faktury sprzedażowej i udostępnianie (kod QR KSeF odłożony)
7. [07-wystaw-podobna.md](analiza/07-wystaw-podobna.md) — „Wystaw podobną”: kopia faktury sprzedażowej do formularza, wspólna podpowiedź dat i numeru

## Środowisko

- [android-studio-macos.md](srodowisko/android-studio-macos.md) — instalacja Android Studio, SDK i emulatora z biometrią
- [instalacja-apk-telefon.md](srodowisko/instalacja-apk-telefon.md) — instalacja podpisanego APK na telefonie (Auto Blocker, nieznane źródła, aktualizacje)

## Download
- [build](build/dist) - apk w wersji testowej (ksef testowy) i produkcyjnej [do instalacji](srodowisko/instalacja-apk-telefon.md)
