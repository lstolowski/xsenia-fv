# ZAD-001. Szkielet projektu Android (Gradle, moduły, wersje test/prod)

- User story: wszystkie
- Status: Zrobione (2026-10-11)
- Szacunek: M

## Cel

Projekt budujący się z terminala i w Android Studio, z modułami i dwiema wersjami aplikacji.

## Zakres

- [x] Gradle 9.8.1 (Kotlin DSL) + version catalog, wrapper w repozytorium (z sumą SHA-256 dystrybucji)
- [x] Moduły: `ksef` i `invoice` (Kotlin/JVM, bez zależności od Androida), `app` (Android, Compose, Material 3); AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00
- [x] `minSdk` 34, `targetSdk` 36, `compileSdk` 37 (wymóg aktualnych bibliotek AndroidX)
- [x] Product flavors `ksefTest` (KSeF TEST, `applicationId` z sufiksem `.test`, nazwa „xsenia TEST”, pomarańczowa ikona) i `ksefProd` (KSeF PROD, zielona ikona); środowisko jako `BuildConfig.KSEF_ENVIRONMENT` → `KsefEnvironment` w module `ksef`
- [x] Ekran startowy z nazwą aplikacji, środowiskiem i wersją; obie wersje zainstalowane i uruchomione na emulatorze `Samsung_S23_xsenia` (`am start` → OK)
- [x] Wizualne potwierdzenie: ekrany „xsenia TEST / Środowisko KSeF: TEST / 0.1.0-test” i „xsenia / PROD / 0.1.0”, dwie ikony w szufladzie aplikacji
- [x] JUnit (Jupiter 6) w modułach JVM; Android lint z `warningsAsErrors`; `.editorconfig`
- [ ] ktlint/detekt — **przeniesione na później** (zgodność wtyczek z Gradle 9.8 / AGP 9 do sprawdzenia)
- [x] Backup wyłączony (`allowBackup=false` + `dataExtractionRules` wykluczające wszystkie dane)

## Definicja ukończenia

- [x] Testy (`KsefEnvironmentTest`)
- [x] Sprawdzone na emulatorze
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja (ADR 0002, AGENTS.md)
