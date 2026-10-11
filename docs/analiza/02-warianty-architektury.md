# Warianty architektury

Data: 2026-10-11
Status: do decyzji na warsztacie (patrz [03-pytania-warsztat.md](03-pytania-warsztat.md))

## Zmiana założeń (2026-10-11)

Pierwotnie: interfejs webowy + ewentualnie Android. **Nowe założenie: aplikacja wyłącznie na Androida**, instalowana ręcznie (APK, bez Google Play), dla jednego użytkownika. Brak wersji przeglądarkowej.

## Co ta zmiana upraszcza

| Problem w wersji webowej | W aplikacji Android |
|---|---|
| Brak CORS w KSeF → wymagany backend | **Nie dotyczy** — natywny klient HTTP nie podlega CORS. **Backend niepotrzebny.** |
| Token KSeF nie może leżeć w przeglądarce | Token w **Android Keystore** (szyfrowany kluczem sprzętowym) |
| Hosting backendu, VPN, zabezpieczenie dostępu do serwera | Brak serwera. Dostęp chroniony blokadą telefonu / biometrią w aplikacji |
| Cache w pamięci serwera, ginie po restarcie | Cache na urządzeniu (pliki w pamięci aplikacji), przetrwa restart |
| `localStorage` dla szkiców | Pliki / DataStore aplikacji |
| `.env` z danymi firmy | Ekran **Ustawienia** w aplikacji |

Architektura sprowadza się do:

```
[Aplikacja Android: UI + klient KSeF + budowa/parsowanie XML FA(3) + cache lokalny] --HTTPS--> [KSeF API]
                                                  └--HTTPS--> [Biała lista VAT (opcjonalnie)]
```

## Co tracimy / nowe ryzyka

- **Brak dostępu z komputera** (podgląd na dużym ekranie, wygodne wpisywanie faktury z klawiatury). Do zaakceptowania świadomie.
- **Oficjalna biblioteka `ksef-client-java` nie zadziała wprost** — używa `java.net.http.HttpClient`, którego Android nie ma. Klienta KSeF piszemy sami (ok. 10 endpointów) — to i tak było założenie w wariancie TS.
- **Pułapka kryptograficzna na Androidzie:** `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` na Androidzie domyślnie używa **SHA-1 w MGF1**, a KSeF wymaga MGF1 z SHA-256 → trzeba jawnie podać `OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)`. Do pokrycia testem na środowisku TEST.
- **Limity API liczone per kontekst + adres IP** — telefon zmienia IP (Wi-Fi / sieć komórkowa), co raczej pomaga niż szkodzi; cache i tak potrzebny.
- **Aktualizacje** — każda nowa wersja to ręczna instalacja APK (lub `adb install`). Trzeba trzymać ten sam klucz podpisu, inaczej aktualizacja wymaga odinstalowania (utrata ustawień i tokena).
- **Kopia zapasowa** — token KSeF i klucze Keystore należy wyłączyć z Auto Backup Androida (inaczej po przywróceniu kopii na nowym telefonie token będzie nieodszyfrowywalny).
- **Środowisko deweloperskie** — na tym Macu jest JDK 17 i IntelliJ IDEA, **brak Android SDK** (Android Studio lub command-line tools do zainstalowania). Testy na emulatorze lub telefonie przez `adb`.

## Warianty technologiczne aplikacji Android

### W1 — Kotlin + Jetpack Compose (natywnie) ⭐ rekomendowany

- Standard Androida, najlepsza integracja: Keystore, BiometricPrompt, udostępnianie (share sheet), `PdfDocument`, powiadomienia.
- Kotlin naturalny dla programisty Javy; można podglądać kod oficjalnej biblioteki Java i przenosić fragmenty (szyfrowanie, buildery żądań, modele).
- Stos: Compose + Material 3, Ktor Client lub OkHttp/Retrofit, kotlinx.serialization (JSON), `XmlPullParser`/`XmlSerializer` z Androida lub `xmlutil` (XML FA(3)), JCA (kryptografia), ZXing (QR), DataStore + pliki (cache, ustawienia).
- Jedna baza kodu, jeden język, brak WebView.

### W2 — React + Capacitor (WebView + natywny HTTP)

- Sens miał głównie przy wspólnym kodzie web + Android. Bez wersji webowej ta przewaga znika.
- Plus: jeśli wolisz React/TypeScript. Minus: warstwa pluginów (secure storage, HTTP, pliki), debugowanie WebView, mniej natywny wygląd.

### W3 — Flutter (Dart)

- Istnieje nieoficjalny pakiet `ksef` w Dart (wersje 0.x). Nowy język i ekosystem — bez wyraźnej korzyści.

### W4 — React Native / Expo

- RSA-OAEP z SHA-256 i AES-CBC wymagają natywnych modułów — więcej tarcia niż w W1/W2. Odradzam.

## Porównanie

| Kryterium | W1 Kotlin/Compose | W2 React+Capacitor | W3 Flutter |
|---|---|---|---|
| Natywne API (Keystore, biometria, PDF, share) | ✅ bezpośrednio | ⚠️ przez pluginy | ⚠️ przez pluginy |
| Kryptografia KSeF | ✅ JCA (uwaga na MGF1) | ✅ Web Crypto / plugin | ⚠️ pakiety |
| Wykorzystanie kodu `ksef-client-java` jako wzorca | ✅ łatwe przeniesienie | ❌ | ❌ |
| Bliskość Twojego doświadczenia (Java) | ✅ | ⚠️ | ❌ |
| Nakład na MVP | niski–średni | średni | średni |

## Rekomendacja

**W1: natywna aplikacja Kotlin + Jetpack Compose**, bez backendu, instalowana jako APK. Moduły:

- `ksef` — klient API KSeF (auth tokenem, sesja interaktywna, zapytania o metadane, pobieranie XML, UPO), kryptografia,
- `invoice` — model faktury, budowa XML FA(3), parser XML FA(2)/FA(3) do widoku szczegółów, wyliczenia sum VAT,
- `app` (UI) — listy sprzedażowe/zakupowe z filtrem, szczegóły, formularz wystawienia, ustawienia.

Moduły `ksef` i `invoice` jako czysty Kotlin/JVM (bez zależności od Androida tam, gdzie się da) → testy jednostkowe na JVM i testy integracyjne z KSeF TEST uruchamiane z komputera, bez emulatora.

## Przechowywanie danych bez bazy danych

| Dane | Gdzie |
|---|---|
| Token KSeF | zaszyfrowany kluczem z Android Keystore, wyłączony z backupu |
| Mój NIP, środowisko (TEST/DEMO/PROD), dane firmy, rachunek | DataStore (ekran Ustawienia) |
| Listy faktur (metadane miesiąca), XML faktur | cache w plikach aplikacji (JSON / XML), ręczne „Odśwież z KSeF” |
| Szkic nowej faktury | DataStore / plik |
| Kontrahenci | brak bazy: podpowiedzi z dotychczasowych faktur (cache) + Biała lista VAT po NIP |
| Kolejny numer faktury | wyliczany z listy faktur sprzedażowych z KSeF |
| Wysyłka w toku (sesja, numer referencyjny) | plik stanu — przetrwa zamknięcie aplikacji, polling po ponownym otwarciu |

Bez Room/SQLite — chyba że uznasz lokalną bazę SQLite na telefonie za dopuszczalną (pytanie B5).

---

## Archiwum: warianty webowe (przed zmianą założeń)

Rozważane wcześniej: React SPA + backend Node/TS (rekomendowany dla weba), React + Spring Boot z `ksef-client-java`, React + Capacitor, natywny Kotlin. Odrzucone wraz z rezygnacją z wersji webowej — patrz [ADR 0001](../adr/0001-backend-posredniczacy-ksef.md). Do powrotu, jeśli kiedyś będzie potrzebny dostęp z komputera (wtedy moduły `ksef` i `invoice` w Kotlinie/JVM da się użyć w backendzie Ktor/Spring).
