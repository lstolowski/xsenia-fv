# Instalacja APK na telefonie (Galaxy S23, One UI 8 / Android 16)

APK buduje się poleceniem `./gradlew releaseApk` — [README.md](../../README.md#wydanie-apk-podpisany-ksef-prod). Plik: `build/dist/xsenia-<wersja>.apk`.

## 1. Przesłanie pliku na telefon

Dowolnie: kabel USB (folder `Download`), chmura, e-mail do siebie. Plik APK to cała aplikacja — nie zawiera tokena ani danych firmy.

## 2. Auto Blocker (tylko na czas instalacji)

One UI blokuje instalację spoza sklepów, gdy włączony jest **Auto Blocker**:

**Ustawienia → Zabezpieczenia i prywatność → Auto Blocker** → wyłącz.

Po instalacji włącz go z powrotem — zainstalowana aplikacja działa normalnie.

## 3. Instalacja

1. Otwórz plik `.apk` w **Moje pliki** (Pobrane / Download).
2. Za pierwszym razem telefon poprosi o zgodę dla „Moje pliki” na **instalowanie nieznanych aplikacji** — zezwól (albo: **Ustawienia → Aplikacje → Specjalny dostęp → Instalowanie nieznanych aplikacji → Moje pliki**). Zgodę można potem cofnąć.
3. **Zainstaluj**. Google Play Protect może ostrzec, że nie zna aplikacji — wybierz instalację mimo to (aplikacja nie jest w sklepie, więc Play Protect jej nie zna).
4. Włącz ponownie Auto Blocker.

## 4. Pierwsze uruchomienie

1. Odblokowanie biometrią (odcisk palca telefonu).
2. **Token KSeF** środowiska **PROD** (z Aplikacji Podatnika KSeF) — zapisywany zaszyfrowany w Android Keystore.
   Token musi zawierać NIP firmy (`…|nip-XXXXXXXXXX|…`) — ten NIP wskazuje firmę, w imieniu której działa aplikacja ([ADR 0006](../adr/0006-dane-firmy-w-aplikacji.md)).
3. Pierwsza synchronizacja pobierze faktury od 01.04.2026.

> [US-010](../user-stories/US-010-dane-firmy-w-aplikacji.md) (w realizacji): dane firmy, rachunki i kontrahentów wpisuje się w aplikacji — formularz **„Moja firma”** (NIP z tokena, dane i rachunki pobierane z Białej listy MF) i lista kontrahentów w Ustawieniach. Folder z plikami `company.json`, `banks.json`, `counterparties.json` nie jest już używany, a danych z nich aplikacja nie importuje — po aktualizacji trzeba je wpisać raz w aplikacji. Szkic, faktury niewysłane i pobrane faktury zostają.

## 5. Aktualizacja

Nowy APK instaluje się tak samo, **na starą wersję** (bez odinstalowania) — dane zostają. Warunki:

- podpisany **tym samym kluczem** (`secrets/xsenia-release.jks`) — inaczej telefon odmówi („Aplikacja nie została zainstalowana, bo pakiet jest niezgodny z istniejącym”),
- `versionCode` nie niższy niż zainstalowany.

Odinstalowanie usuwa dane aplikacji (token, pobrane faktury, faktury niewysłane, a od US-010 także dane firmy, rachunki i kontrahentów — bez kopii zapasowej). Pliki JSON w folderze `Dokumenty` zostają.

## 6. Wersja testowa obok produkcyjnej

`xsenia TEST` (`pl.xsenia.fv.test`, KSeF TEST) i `xsenia` (`pl.xsenia.fv`, KSeF PROD) to osobne aplikacje — można mieć obie. Na telefonie z prawdziwymi danymi używaj tylko wersji produkcyjnej; wersja testowa służy do sprawdzania na emulatorze z danymi zmyślonymi.
