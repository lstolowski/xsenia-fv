# xsenia-fv

Vibecodowana aplikacja na Androida do KSeF (API 2.0): przegląd faktur sprzedażowych i zakupowych oraz wystawianie faktur sprzedażowych VAT. APK instalowany ręcznie, bez Google Play, bez backendu.

Prostota wystawiania kilku faktur w miesiącu, idealna dla JDG

Dokumentacja projektu: [docs/](docs/README.md). Instrukcje dla agentów AI: [AGENTS.md](AGENTS.md).

## Wymagania

- Android Studio (z wbudowanym JDK) i Android SDK — [docs/srodowisko/android-studio-macos.md](docs/srodowisko/android-studio-macos.md)
- `local.properties` z `sdk.dir` (tworzy go Android Studio)
- W terminalu:

  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ```

## Budowanie

| Polecenie | Co robi |
|---|---|
| `./gradlew build` | kompilacja, testy jednostkowe, lint (ostrzeżenia = błędy) |
| `./gradlew :app:installKsefTestDebug` | instaluje wersję testową (KSeF TEST) na emulatorze |
| `./gradlew integrationTest` | testy z prawdziwym KSeF TEST (wymaga `secrets/ksef-test.properties`) |
| `./gradlew releaseApk` | **podpisany APK produkcyjny** do instalacji na telefonie — patrz niżej |

Wersje aplikacji: `ksefTest` (KSeF TEST, `pl.xsenia.fv.test`, nazwa „xsenia TEST”) i `ksefProd` (KSeF PROD, `pl.xsenia.fv`, nazwa „xsenia”).

## Wydanie APK (podpisany, KSeF PROD)

### 1. Klucz podpisu — tylko raz

Każda aktualizacja aplikacji na telefonie musi być podpisana **tym samym kluczem**. Bez niego nie da się zainstalować nowej wersji na starej — trzeba by odinstalować aplikację i stracić dane na telefonie (token, pobrane faktury, faktury niewysłane).

```bash
"$JAVA_HOME/bin/keytool" -genkeypair -v -storetype PKCS12 -keystore secrets/xsenia-release.jks -alias xsenia -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` zapyta o hasło (zapamiętaj je) i dane właściciela (wystarczy imię i nazwisko). Potem:

```bash
cp secrets/keystore.properties.example secrets/keystore.properties
```

W `secrets/keystore.properties` wpisz hasło w `storePassword` i `keyPassword` (w PKCS12 oba są takie same). Możesz też zostawić je puste i podawać przy budowaniu w zmiennych `XSENIA_STORE_PASSWORD` / `XSENIA_KEY_PASSWORD`.

Kopia zapasowa: plik `secrets/xsenia-release.jks` **i** hasło przechowuj poza komputerem (np. menedżer haseł z załącznikiem albo zaszyfrowany dysk). Katalog `secrets/` nie trafia do gita.

### 2. Budowanie podpisanego APK

```bash
./gradlew releaseApk
```

Zadanie buduje wersję `ksefProd` w trybie release (R8: zmniejszenie i zaciemnienie kodu), podpisuje ją kluczem z `secrets/keystore.properties`, sprawdza podpis (`apksigner verify`) i kopiuje plik do:

```
build/dist/xsenia-<wersja>.apk
```

Na końcu wypisuje skrót SHA-256 certyfikatu podpisu — przy każdym wydaniu powinien być taki sam. Bez skonfigurowanego klucza zadanie kończy się błędem z informacją, czego brakuje.

Hasła ze zmiennych środowiskowych (bez zapisywania ich w pliku):

```bash
XSENIA_STORE_PASSWORD='…' ./gradlew releaseApk
```

`./gradlew releaseApkTest` buduje tak samo podpisaną wersję release dla KSeF TEST (`build/dist/xsenia-test-<wersja>.apk`) — do sprawdzenia buildu release na emulatorze. Inny plik z danymi klucza: `-Pxsenia.signingProperties=<ścieżka>`.

### 3. Nowa wersja

Przed kolejnym wydaniem podnieś `versionCode` (o 1) i `versionName` w [app/build.gradle.kts](app/build.gradle.kts) — telefon nie zainstaluje wersji o niższym `versionCode`.

### 4. Instalacja na telefonie

[docs/srodowisko/instalacja-apk-telefon.md](docs/srodowisko/instalacja-apk-telefon.md) — przesłanie pliku, nieznane źródła, Auto Blocker, pierwsze uruchomienie.
