# ZAD-015. Budowa i podpisanie APK produkcyjnego

- User story: wszystkie
- Status: Zrobione
- Szacunek: S

## Cel

Podpisany APK `prod` gotowy do ręcznej instalacji na S23.

## Zakres

- [x] Klucz podpisu (`.jks`) i `keystore.properties` poza gitem (`secrets/`, wzór `secrets/keystore.properties.example`); instrukcja wygenerowania (`keytool`, PKCS12, RSA 4096) i backupu klucza w [README.md](../../README.md)
- [x] `assembleKsefProdRelease`, R8/minifikacja (`isMinifyEnabled`, `isShrinkResources`, `proguard-android-optimize.txt`); kotlinx.serialization, Ktor, OkHttp i AndroidX mają własne reguły — `app/proguard-rules.pro` bez dodatkowych reguł
- [x] Instrukcja instalacji na telefonie (Auto Blocker, nieznane źródła, Play Protect, aktualizacje) — [docs/srodowisko/instalacja-apk-telefon.md](../srodowisko/instalacja-apk-telefon.md)
- [x] Wykonanie taska w gradle który będzie przygotowoywał apk i podpisywał apk za pomocą polecenia w konsoli, instrukcja wykonanaia w README.md — `./gradlew releaseApk` (KSeF PROD) i `./gradlew releaseApkTest` (KSeF TEST): budują wariant release, podpisują kluczem z `secrets/keystore.properties` (hasła opcjonalnie z `XSENIA_STORE_PASSWORD` / `XSENIA_KEY_PASSWORD`), sprawdzają podpis `apksigner verify`, kopiują do `build/dist/xsenia-<wersja>.apk` i wypisują SHA-256 certyfikatu; bez klucza — błąd z instrukcją

## Implementacja

- `app/build.gradle.kts`: `signingConfigs.release` z pliku wskazanego przez `-Pxsenia.signingProperties` (domyślnie `secrets/keystore.properties`; odczyt przez `providers.fileContents`, zgodny z configuration cache), zadanie `ReleaseApkTask` rejestrowane w `androidComponents.onVariants` dla wariantów release.
- Klucza produkcyjnego nie generuje agent — tworzy go użytkownik (hasło zna tylko on).

## Definicja ukończenia

- [x] Testy: `./gradlew build` (z wariantami release i R8) zielony; `releaseApk` bez klucza kończy się czytelnym błędem
- [x] Sprawdzone na emulatorze: `releaseApkTest` podpisany kluczem debug (podpis v2 zweryfikowany), zainstalowany jako aktualizacja — biometria, synchronizacja z KSeF TEST (także pełna po wyczyszczeniu cache: eksport, szyfrowanie, ZIP), listy, szczegóły (korekta, EUR), formularz, podgląd, zapis szkicu; bez błędów w logach (2026-10-13). Wersji PROD nie uruchamiano (zakaz łączenia z KSeF PROD w developmencie)
- [x] Sprawdzone przez użytkownika na urządzeniu fizycznym z KSeF TEST (2026-10-13): działa i wygląda poprawnie
- [x] Zaktualizowana dokumentacja: README.md, AGENTS.md, docs/srodowisko/
