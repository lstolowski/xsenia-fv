# Code review i audyt bezpieczeństwa aplikacji

- Data: 2026-10-15
- Zakres: statyczny przegląd kodu `app`, `ksef`, `mirror`, `invoice`, konfiguracji Androida i Gradle.
- Punkt odniesienia: OWASP MASVS / MASTG — przede wszystkim STORAGE, AUTH, PLATFORM, NETWORK i CODE.
- Ograniczenie: nie uruchamiałem aplikacji ani testów dynamicznych, nie analizowałem zbudowanego APK i ruchu sieciowego. To przegląd kodu, nie potwierdzenie zgodności z MASVS.

## Ocena w skrócie

Aplikacja ma dobry fundament: token KSeF jest szyfrowany w spoczynku, aplikacja nie ma backendu ani logów z danymi, a Androidowy sandbox i konfiguracja kopii zapasowych ograniczają dostęp do danych. Najważniejsze braki to brak szyfrowania lokalnych faktur i szkiców, biometryczne potwierdzenie wysyłki oparte wyłącznie na callbacku (z możliwością użycia PIN-u telefonu) oraz możliwość przechwycenia zawartości ekranu podczas korzystania z aplikacji. Znalazłem też ryzyko wyczerpania pamięci przy rozpakowywaniu paczki KSeF.

Zwykła, nieuprzywilejowana aplikacja Android nie powinna móc czytać `filesDir` ani `cacheDir` tej aplikacji. Brak szyfrowania plików ma znaczenie przy dostępie uprzywilejowanym, odblokowanym urządzeniu, analizie kopii danych lub przejęciu procesu aplikacji.

## Mocne strony

- Token KSeF jest szyfrowany AES-256-GCM kluczem z Android Keystore; szyfrogram trafia do DataStore, a niejawny token nie jest logowany. Kod nie eksportuje klucza poza provider Keystore. (`KeystoreCipher.kt`, `KsefTokenStore.kt`)
- Access i refresh tokeny sesji KSeF są utrzymywane w pamięci, bez zapisu do plików. Żądania autoryzowane używają nagłówka Bearer.
- `allowBackup="false"` i reguły ekstrakcji wykluczają dane aplikacji z kopii oraz transferu urządzenie–urządzenie. (`AndroidManifest.xml`, `data_extraction_rules.xml`)
- Dane firmy, faktury i szkice są w prywatnym katalogu aplikacji, nie na współdzielonej pamięci. (`ProfileManager.kt`, `InvoiceMirror.kt`, `IssueStorage.kt`)
- Aktywność w ostatnich aplikacjach ma wyłączony podgląd zawartości. FileProvider nie jest eksportowany i udostępnia wyłącznie `cache/pdf/`. (`MainActivity.kt`, `AndroidManifest.xml`, `file_paths.xml`)
- Widoczne w kodzie adresy KSeF, Białej listy i NBP używają HTTPS; klient OkHttp nie ma własnego, osłabionego `TrustManager` ani logowania żądań. Nie stwierdziłem permissive cleartext konfiguracji.
- Manifest deklaruje tylko `INTERNET`. Wewnętrzny komponent poza launcherem nie jest eksportowany.
- Token jest domyślnie maskowany, wpis używa klawiatury hasła bez autokorekty. Wydania release włączają R8; klucz podpisujący jest konfigurowany poza repozytorium.
- W parserze XML zastosowano ochronę przed DOCTYPE, a numery używane jako nazwy plików są walidowane. Eksport PDF wymaga jawnego działania użytkownika i przyznaje odbiorcy tylko uprawnienie odczytu URI.

## Ustalenia i ryzyka

### 1. Średnie — wrażliwe dane aplikacji zapisane jawnie na dysku

Lustro XML faktur i indeksy, dane firmy/kontrahentów, UPO, szkice i faktury niewysłane są serializowane do zwykłych plików. Szyfrowany jest token, ale nie pozostałe dane księgowe i osobowe.

- Lokalizacje: `mirror/.../InvoiceMirror.kt:10-19, 42-49`; `invoice/.../CompanyProfileStore.kt:11-13, 76-81`; `app/.../IssueStorage.kt:24-28, 61-69`; `app/.../UnsentInvoices.kt:43-50, 60-65`.
- Skutek: wyciek danych po uzyskaniu dostępu do prywatnego katalogu, np. na zrootowanym/odblokowanym urządzeniu lub podczas analizy jego danych. Sama izolacja sandboxa ogranicza zwykłe inne aplikacje, ale pliki nie mają ochrony kryptograficznej na poziomie aplikacji.
- Zalecenie: szyfrować zawartość wrażliwych plików uwierzytelnionym szyfrem (np. AES-GCM) kluczem z Keystore; zaprojektować obsługę braku/utraty klucza i migrację istniejących danych. Nie szyfrować własnym algorytmem.
- OWASP: [MASVS-STORAGE-1](https://mas.owasp.org/MASVS/controls/MASVS-STORAGE-1/), [MASTG-BEST-0050](https://mas.owasp.org/MASTG-BEST-0050/).

**Dodatkowa uwaga o Keystore:** kod używa Android Keystore, ale nie sprawdza poziomu bezpieczeństwa klucza (`KeyInfo` / hardware-backed). Komentarz w `KeystoreCipher.kt` deklaruje sprzętową ochronę; sam wybór providera Keystore nie gwarantuje jej na każdym zgodnym urządzeniu. Jeśli sprzętowe przechowywanie jest wymaganiem, należy je zweryfikować albo odpowiednio opisać fallback programowy.

### 2. Średnie — potwierdzenie wysyłki nie jest związane kryptograficznie z biometrią

`BiometricPrompt` działa bez `CryptoObject`, a klucz szyfrujący token nie wymaga uwierzytelnienia użytkownika. Sukces opiera się na callbacku aplikacji. Wspólny prompt dopuszcza `BIOMETRIC_STRONG` **lub** `DEVICE_CREDENTIAL`, więc przy wysyłce faktury PIN/hasło/wzór ekranu blokady zastępuje biometrię.

- Lokalizacje: `BiometricGate.kt:11-17, 26-40`; `KeystoreCipher.kt:14-15, 35-42`; wysyłka po callbacku: `NewInvoiceScreen.kt:482-488`.
- Skutek: użytkownik może zatwierdzić wystawienie faktury PIN-em, mimo wymagania biometrycznego. Przy przejęciu procesu lub hookowaniu kodu lokalna kontrola oparta na callbacku jest łatwiejsza do obejścia. Uprawnienia tokena ograniczone w KSeF zmniejszają skutki nadużycia.
- Zalecenie: rozdzielić politykę odblokowania aplikacji od polityki wysyłki. Dla wysyłki wymagać wyłącznie silnej biometrii i związać potwierdzenie z operacją przez klucz Keystore wymagający uwierzytelnienia (`CryptoObject`); potwierdzać niezmienny podgląd faktury, np. numer i kwotę. Utrzymanie PIN-u jako awaryjnej metody odblokowania aplikacji może pozostać osobną decyzją.
- Uwaga: obecny fallback do PIN-u jest celowo zapisany w `AGENTS.md`/ZAD-005. To udokumentowany kompromis, ale nie spełnia literalnego wymagania biometrii przy każdej wysyłce. OWASP odradza fallback do poświadczenia urządzenia dla operacji wysokiej wagi.
- OWASP: [MASVS-AUTH-3](https://mas.owasp.org/MASVS/07-MASVS-AUTH/), [MASTG-BEST-0031](https://mas.owasp.org/MASTG/best-practices/MASTG-BEST-0031/), [MASTG-BEST-0036](https://mas.owasp.org/MASTG/best-practices/MASTG-BEST-0036/).

### 3. Średnie — dostęp do aplikacji pozostaje odblokowany przez 60 sekund w tle

`AppLock` ponownie blokuje aplikację dopiero, gdy była w tle co najmniej 60 sekund. Powrót do niej przed upływem tego czasu nie wymaga ponownego uwierzytelnienia. Zrzut ekranu w widoku ostatnich aplikacji jest ukryty, ale nie zmienia to dostępu po szybkim powrocie.

- Lokalizacje: `AppLock.kt:9-15, 27-39`; testowany próg jest też opisany w `docs/zadania/ZAD-005-bezpieczenstwo-i-ustawienia.md`.
- Skutek: osoba, która uzyska odblokowany telefon niedługo po przełączeniu aplikacji w tło, może wrócić do otwartej sesji.
- Zalecenie: skrócić próg albo blokować aplikację natychmiast po zablokowaniu ekranu/opuszczeniu aplikacji. Jeśli 60 sekund zostaje ze względów użyteczności, nazwać ten kompromis w polityce bezpieczeństwa.

### 4. Średnie — brak ochrony przed zrzutami ekranu i nagrywaniem podczas pracy

`setRecentsScreenshotEnabled(false)` chroni podgląd w ostatnich aplikacjach, ale w kodzie nie ma `FLAG_SECURE` ani analogicznej ochrony okna. Ekrany faktur i formularz z tokenem mogą zostać utrwalone przez zrzut ekranu lub nagrywanie ekranu. Pole tokena jest domyślnie maskowane, ale użytkownik może je jawnie odsłonić przyciskiem.

- Lokalizacje: `MainActivity.kt:23-28`; `TokenSection.kt:95-116`.
- Zalecenie: włączyć `FLAG_SECURE` podczas wyświetlania wrażliwych ekranów i rozważyć usunięcie opcji odsłonięcia tokena albo wymagać ponownego uwierzytelnienia przed odsłonięciem. Sama ochrona screenshotów nie blokuje fotografowania ekranu drugim urządzeniem.
- OWASP: [MASWE-0038](https://mas.owasp.org/MASWE/MASVS-PLATFORM/MASWE-0038/).

### 5. Niskie/średnie — rozpakowanie paczki może wyczerpać pamięć

Paczka eksportu i wszystkie wpisy ZIP są wczytywane w całości do pamięci (`ByteArrayOutputStream`, `readBytes()`), bez limitu łącznego rozmiaru, liczby części, liczby wpisów ani rozmiaru po dekompresji.

- Lokalizacja: `ksef/.../KsefInvoiceClient.kt:103-115, 162-176`.
- Skutek: zbyt duża lub silnie skompresowana paczka może spowodować OOM i zamknięcie aplikacji podczas synchronizacji. Treści faktur pochodzą od zewnętrznych wystawców, choć pobieranie odbywa się z KSeF.
- Zalecenie: narzucić limity rozmiaru odpowiedzi, części, wpisów, pojedynczego XML i łącznych danych po dekompresji; przetwarzać strumieniowo i przerwać import po przekroczeniu limitów.

### 6. Niskie / do potwierdzenia — niepełna obrona parsera XML przy braku obsługi funkcji

Parser ustawia `disallow-doctype-decl`, lecz ignoruje błąd konfiguracji. Kontrola `doctype` następuje dopiero po `parse()`. Jeżeli parser Androida nie obsłuży funkcji, dokument może być przetwarzany przed odrzuceniem. Nie ustawiono też jawnie zakazu zewnętrznych encji i dostępu do zewnętrznych DTD.

- Lokalizacja: `invoice/.../InvoiceXmlParser.kt:51-60, 175-187`.
- Ocena: nie potwierdziłem exploita na docelowym Androidzie; to warunkowe ryzyko parsera danych pochodzących z KSeF i brak defensywnej konfiguracji.
- Zalecenie: jawnie wyłączyć external general/parameter entities i external DTD/schema, ustawić resolver odrzucający zewnętrzne encje, a konfigurację zweryfikować testem na Androidzie API 34+.

### 7. Niskie — adres części eksportu jest używany bez jawnej walidacji

Adres i metoda pobrania części ZIP pochodzą z odpowiedzi KSeF i są przekazywane bezpośrednio do klienta HTTP. W tym żądaniu nie jest dodawany token Bearer, co ogranicza ryzyko ujawnienia poświadczenia. Brakuje jednak jawnego sprawdzenia schematu HTTPS i hosta przed wykonaniem połączenia.

- Lokalizacja: `ksef/.../KsefInvoicesApi.kt:49-53`.
- Zalecenie: odrzucać URL-e inne niż HTTPS i walidować domeny według aktualnej dokumentacji KSeF; sprawdzić też zachowanie przy przekierowaniach.

## Udostępnianie PDF i granice ochrony

PDF faktury jest zapisywany w prywatnym `cacheDir`, a FileProvider udostępnia tylko `pdf/` z czasowym uprawnieniem odczytu wybranemu odbiorcy. To ogranicza dostęp innych aplikacji domyślnie. Po użyciu funkcji „Udostępnij” odbiorca może skopiować plik, a lokalna kopia może pozostać w cache do kolejnego eksportu lub wyczyszczenia cache.

Nie stwierdziłem pinningu certyfikatów. Klient używa HTTPS i standardowej walidacji certyfikatu OkHttp. KSeF, MF i NBP są zewnętrznymi usługami, nie domenami kontrolowanymi przez twórcę aplikacji; brak pinningu nie został więc uznany za potwierdzoną lukę. Warto dynamicznie sprawdzić TLS, przekierowania, walidację URL-i części eksportu i brak cleartext na docelowym APK.

## Zalecana kolejność prac

1. Szyfrowanie faktur, profili, UPO i szkiców na dysku.
2. Osobna, silniejsza autoryzacja wysyłki faktury: tylko biometria i powiązanie z operacją.
3. Zmniejszenie czasu odblokowania oraz ochrona okna przed zrzutami/nagrywaniem.
4. Limity pamięci i rozmiarów dla pobieranych paczek oraz twarde ustawienia parsera XML.
5. Dynamiczna weryfikacja na APK release: kopie danych, zrzuty ekranu, ścieżki FileProvider, ruch TLS, cykl blokady i obsługa błędnego/dużego XML/ZIP.

## Źródła OWASP

- [OWASP MASVS](https://mas.owasp.org/MASVS/) — grupy kontroli dla pamięci, kryptografii, uwierzytelniania, sieci, platformy, kodu, odporności i prywatności.
- [OWASP MASTG](https://mas.owasp.org/MASTG/) — przewodnik testowania aplikacji mobilnych.
- [OWASP MASTG-BEST-0050](https://mas.owasp.org/MASTG/MASTG-BEST-0050/) — szyfrowanie wrażliwych danych w sandboxie aplikacji.
