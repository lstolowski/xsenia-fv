# 0002. Natywna aplikacja Android (Kotlin + Compose) bez backendu

- Status: Zaakceptowany
- Data: 2026-10-11

## Kontekst

- Aplikacja **wyłącznie na Androida**, dla jednego użytkownika, instalowana ręcznie (APK), bez wersji webowej i bez dostępu z komputera (decyzja B3).
- Natywny klient HTTP nie podlega CORS → komunikacja z KSeF bezpośrednio, bez backendu (unieważnia powód [ADR 0001](0001-backend-posredniczacy-ksef.md)).
- Token KSeF musi być bezpiecznie przechowywany na urządzeniu.
- Oficjalna biblioteka `ksef-client-java` nie działa na Androidzie (`java.net.http`), ale jest dobrym wzorcem implementacji.
- Wymaganie: brak bazy danych — również na telefonie (decyzja B5).
- Urządzenie docelowe: Samsung Galaxy S23, Android 16 (API 36), One UI 8.5.

Szczegóły: [analiza/02-warianty-architektury.md](../analiza/02-warianty-architektury.md).

## Rozważane opcje

1. Kotlin + Jetpack Compose (natywnie).
2. React + Capacitor (WebView + natywny HTTP).
3. Flutter.
4. React Native / Expo.

## Decyzja

**Opcja 1 — Kotlin + Jetpack Compose, Material 3.**

| Obszar | Decyzja |
|---|---|
| Moduły | `ksef` (klient API, kryptografia), `invoice` (model, XML FA(3), parser, pliki JSON), `mirror` (lokalna kopia faktur i synchronizacja), `whitelist` (Biała lista MF, ADR 0007) — czysty Kotlin/JVM; `app` — UI Android |
| SDK | `targetSdk` 36 (Android 16); `minSdk` 34 (Android 14) — jedno urządzenie docelowe, brak potrzeby wstecznej zgodności; `compileSdk` 37 — wymagane przez aktualne biblioteki AndroidX (nie zmienia zachowania aplikacji na Androidzie 16) |
| Biometria | `BiometricPrompt` przy **otwarciu aplikacji** i przy **potwierdzeniu wysyłki** nowej faktury do KSeF (B4) |
| Token KSeF | wprowadzany w aplikacji, szyfrowany kluczem z Android Keystore, wyłączony z Auto Backup |
| Wersje aplikacji | dwa *product flavors*: `ksefTest` (KSeF TEST, `applicationId` `pl.xsenia.fv.test`, nazwa „xsenia TEST”, ikona z pomarańczowym paskiem „TEST”) i `ksefProd` (KSeF PROD, `pl.xsenia.fv`, ikona bez paska) — można mieć obie zainstalowane jednocześnie (B7). Nazwy nie mogą zaczynać się od `test` (zastrzeżone w AGP dla źródeł testów) |
| Dystrybucja | APK instalowany ręcznie (B8), podpisywany stałym kluczem trzymanym poza repozytorium |
| Testy | emulator Android Studio (B6); moduły `ksef`/`invoice` testowane na JVM |
| Dane lokalne | pliki w pamięci aplikacji + DataStore, bez Room/SQLite (B5) |

## Konsekwencje

- Brak serwera do utrzymania; brak dostępu z komputera.
- Własna implementacja klienta KSeF i XML FA(3); uwaga na domyślne MGF1 = SHA-1 w RSA-OAEP na Androidzie.
- Potrzebne Android Studio — instrukcja: [srodowisko/android-studio-macos.md](../srodowisko/android-studio-macos.md).
- Token i klucze wyłączone z Auto Backup — na nowym telefonie token trzeba wprowadzić ponownie.
- Emulator nie odwzorowuje nakładki One UI — końcowe sprawdzenie na telefonie po instalacji APK.
