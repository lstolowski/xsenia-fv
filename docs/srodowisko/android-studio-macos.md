# Instalacja Android Studio na macOS (Apple Silicon)

Data: 2026-10-11
Dotyczy: MacBook z procesorem Apple Silicon (arm64), macOS 26, 32 GB RAM, zainstalowane IntelliJ IDEA i JDK 17 (Corretto).

Cel: zbudować aplikację xsenia-fv i uruchomić ją na emulatorze odwzorowującym Samsunga Galaxy S23 z Androidem 16 (API 36), razem z obsługą odcisku palca.

## 0. Wolne miejsce na dysku — zrób to najpierw

Stan na 2026-10-11: **wolne tylko ~11 GB** (dysk zajęty w 98%). To za mało.

| Składnik | Orientacyjnie |
|---|---|
| Android Studio | ~3 GB |
| Android SDK (platforma 36, build-tools, platform-tools, emulator) | ~3 GB |
| Obraz systemu emulatora (Android 16, arm64) | ~4 GB |
| Dane emulatora (AVD) | 6–10 GB |
| Cache Gradle i zależności projektu | 2–5 GB |

**Zwolnij co najmniej 30 GB.** Przy prawie pełnym dysku macOS i emulator działają niestabilnie.
Pomoże: Ustawienia systemowe → Ogólne → Pamięć; stare cache Gradle/Maven (`~/.gradle/caches`, `~/.m2/repository`) z innych projektów; obrazy Dockera.

## 1. Instalacja Android Studio

Android Studio i IntelliJ IDEA działają obok siebie bez konfliktów: osobne aplikacje, osobne ustawienia.

**Wariant A — Homebrew (masz go zainstalowanego):**

```bash
brew install --cask android-studio
```

**Wariant B — ręcznie:**
1. Wejdź na https://developer.android.com/studio.
2. Pobierz wersję **Mac (Apple chip)**, nie Intel.
3. Otwórz plik `.dmg` i przeciągnij **Android Studio** do **Applications**.

Następnie uruchom Android Studio z Launchpada lub katalogu Aplikacje. Przy pierwszym uruchomieniu macOS zapyta o zgodę na otwarcie aplikacji pobranej z internetu — potwierdź.

## 2. Kreator pierwszego uruchomienia (Setup Wizard)

1. **Import settings** → wybierz *Do not import settings*. Ustawień IntelliJ nie przenosimy — keymapę możesz później ustawić tak samo jak w IntelliJ.
2. **Data sharing** — według uznania.
3. **Install Type** → *Standard*.
4. **UI Theme** — dowolny.
5. **Verify Settings** — sprawdź, że *SDK Folder* to `/Users/<użytkownik>/Library/Android/sdk`.
6. **License Agreement** — przeczytaj i zaakceptuj licencje (`android-sdk-license` i ewentualne pozostałe po lewej stronie). Licencje akceptujesz Ty, nie agent.
7. **Finish** — kreator pobierze SDK, platform-tools i emulator. Trwa to kilka–kilkanaście minut.

## 3. Uzupełnienie Android SDK (SDK Manager)

Na ekranie powitalnym: **More Actions → SDK Manager**. Z otwartym projektem: **Settings → Languages & Frameworks → Android SDK**.

**Zakładka SDK Platforms**
- zaznacz **Android 16.0 („Baklava”) — API Level 36**
- zaznacz *Show Package Details* i upewnij się, że zaznaczone jest *Android SDK Platform 36*

**Zakładka SDK Tools**
- Android SDK Build-Tools (najnowsze)
- **Android SDK Command-line Tools (latest)** — potrzebne do budowania z terminala
- Android Emulator
- Android SDK Platform-Tools (`adb`)

**Apply → OK.**

## 4. Zmienne środowiskowe (terminal, budowanie z linii poleceń)

Dzięki tym zmiennym agent (Claude Code) i Ty możecie budować oraz instalować aplikację z terminala. Dopisz na końcu `~/.zshrc`:

```bash
# Android SDK
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin"
```

Potem otwórz nowe okno terminala albo wykonaj:

```bash
source ~/.zshrc
```

**JDK:** Android Studio ma wbudowane JDK (JetBrains Runtime) i domyślnie używa go do Gradle. `JAVA_HOME` w `~/.zshrc` zostawiamy bez zmian, żeby nie wpływać na projekty w IntelliJ. Budując z terminala, agent wskaże JDK z Android Studio:
`/Applications/Android Studio.app/Contents/jbr/Contents/Home`.

## 5. Emulator „Galaxy S23” z Androidem 16

Samsung nie udostępnia obrazu One UI dla emulatora. Tworzymy więc profil o parametrach ekranu S23 na czystym Androidzie 16.

### 5.1. Profil sprzętowy

**Device Manager** (ikona telefonu na prawym pasku albo *More Actions → Virtual Device Manager*) → **+ → Create Virtual Device** → **New Hardware Profile**:

| Pole | Wartość |
|---|---|
| Device Name | `Galaxy S23 (xsenia)` |
| Device Type | Phone/Tablet |
| Screen size | `6.1` |
| Resolution | `1080 × 2340` |
| RAM | `4096` MB |
| Input | Has Hardware Buttons — odznacz |
| Sensors | zaznacz Accelerometer, Gyroscope, Proximity |
| Default Skin | No Skin |

→ **Finish** i wybierz nowy profil z listy → **Next**.

### 5.2. Obraz systemu

- Zakładka **Recommended** lub **ARM Images**
- **API 36 (Android 16), ABI `arm64-v8a`, Target: *Google APIs***. Wariant z Google Play nie jest potrzebny, a *Google APIs* pozwala na `adb root`.
- Kliknij ikonę pobierania przy obrazie, poczekaj, **Next**.

### 5.3. Ustawienia AVD

- AVD Name: `Galaxy_S23_API_36`
- Startup orientation: Portrait
- **Show Advanced Settings:**
  - Graphics: *Hardware*
  - Internal Storage: `6 GB` (oszczędzamy dysk)
  - SD card: brak
  - Boot option: *Quick boot*
- **Finish**

Uruchom emulator przyciskiem ▶ w Device Managerze. Pierwsze uruchomienie trwa dłużej.

## 6. Biometria (odcisk palca) w emulatorze

Aplikacja wymaga biometrii przy otwarciu i przy wysyłce faktury, więc emulator musi mieć zarejestrowany odcisk palca.

1. W emulatorze otwórz **Settings → Security & privacy → Device unlock → Screen lock** i ustaw **PIN**, np. `1111`. Bez blokady ekranu nie da się dodać odcisku.
2. **Settings → Security & privacy → Device unlock → Fingerprint Unlock** → wpisz PIN → **Add fingerprint** / *I agree*.
3. Gdy kreator prosi o dotknięcie czytnika, w panelu emulatora kliknij **⋯ (Extended controls) → Fingerprint → Touch Sensor**, z wybranym np. *Finger 1*. Powtarzaj, aż zapis dojdzie do 100%.
4. Później, gdy aplikacja wyświetli okno biometrii, uwierzytelniasz się tym samym przyciskiem **Touch Sensor** albo z terminala:

```bash
adb -e emu finger touch 1
```

Ścieżki menu mogą się nieznacznie różnić między wersjami obrazu Androida — w razie wątpliwości wpisz „fingerprint” w wyszukiwarce Ustawień emulatora.

## 7. Weryfikacja instalacji

Każde polecenie w nowym terminalu:

```bash
adb version
```

```bash
emulator -list-avds
```

Oczekiwane: na liście `Galaxy_S23_API_36`.

```bash
sdkmanager --list_installed
```

Oczekiwane: m.in. `platforms;android-36`, `platform-tools`, `emulator`, `build-tools;…`, `system-images;android-36;google_apis;arm64-v8a`.

Przy uruchomionym emulatorze:

```bash
adb devices
```

Oczekiwane: `emulator-5554   device`.

## 8. Telefon — instalacja APK

Testujemy na emulatorze. Podpisany APK buduje `./gradlew releaseApk` ([README.md](../../README.md)); instalacja na S23: [instalacja-apk-telefon.md](instalacja-apk-telefon.md).

## Rozwiązywanie problemów

| Objaw | Rozwiązanie |
|---|---|
| Emulator się nie uruchamia / „not enough space” | Zwolnij miejsce na dysku (pkt 0), zmniejsz Internal Storage AVD. |
| `adb: command not found` | Brak zmiennych z pkt 4 lub nie otwarto nowego terminala. |
| `sdkmanager: command not found` | Nie zainstalowano *Android SDK Command-line Tools (latest)* (pkt 3). |
| Gradle: „Unsupported Java version” | W Android Studio: Settings → Build, Execution, Deployment → Build Tools → Gradle → *Gradle JDK* = *jbr (bundled)*. |
| Brak opcji odcisku palca w ustawieniach emulatora | Najpierw ustaw blokadę ekranu PIN-em (pkt 6.1). |
