# Analiza KSeF API 2.0 pod kątem xsenia-fv

Data: 2026-10-11
Źródła: [CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs) (stan na API 2.8.1, wdrożone na PROD 23.09.2026), OpenAPI `open-api.json` z tego repozytorium, sprawdzenie na żywo środowiska TEST.

> Strona https://ksef.mf.gov.pl/ to aplikacja (SPA) bez treści do czytania — właściwa dokumentacja integracyjna jest w repozytorium GitHub Ministerstwa Finansów (CIRFMF) oraz w OpenAPI pod `https://api[-test|-demo].ksef.mf.gov.pl/docs/v2`.

## 1. Środowiska

| Skrót | Bazowy URL API | Uwagi |
|---|---|---|
| TEST | `https://api-test.ksef.mf.gov.pl/v2` | Dane współdzielone między integratorami, losowe NIP-y, certyfikaty self-signed dozwolone, limity ×10 |
| DEMO | `https://api-demo.ksef.mf.gov.pl/v2` | Konfiguracja jak PROD, limity jak PROD |
| PROD | `https://api.ksef.mf.gov.pl/v2` | Faktury z mocą prawną |

Na TEST/DEMO nie wolno wysyłać prawdziwych danych. Na TEST codziennie 16:00–18:00 możliwe prace serwisowe.

Obowiązujący schemat faktury na DEMO/PROD: **FA(3)** (FA(2) już tylko na TEST).

## 2. Kluczowe ustalenie: brak CORS

Sprawdzone 2026-10-11 na TEST: `OPTIONS /v2/auth/challenge` → `405`, odpowiedzi nie zawierają nagłówków `Access-Control-Allow-*`.

**Wniosek: przeglądarka nie może wywoływać API KSeF bezpośrednio.** Sama aplikacja React (SPA) nie wystarczy — potrzebny jest pośrednik po stronie serwera (backend / proxy) **albo** natywny klient HTTP (aplikacja mobilna, gdzie CORS nie obowiązuje).

> Aktualizacja 2026-10-11: przyjęto założenie „aplikacja tylko na Androida” — natywny klient HTTP nie podlega CORS, więc **backend nie jest potrzebny**. Patrz [02-warianty-architektury.md](02-warianty-architektury.md).

## 3. Uwierzytelnianie

Dwie metody:

1. **Podpis XAdES** — dokument `AuthTokenRequest` podpisany certyfikatem kwalifikowanym, pieczęcią, Profilem Zaufanym lub **certyfikatem KSeF**.
2. **Token KSeF** — wcześniej wygenerowany sekret (uprawnienia np. `InvoiceRead`, `InvoiceWrite`). Wygenerować go można dopiero po jednorazowym uwierzytelnieniu podpisem (XAdES / Profil Zaufany), np. w Aplikacji Podatnika KSeF.

Dla jednego użytkownika najprostszy jest **token KSeF** (wygenerowany raz, przechowywany jako sekret w konfiguracji). Przebieg:

```
POST /auth/challenge                  → challenge, timestampMs
   encrypt("{tokenKSeF}|{timestampMs}") RSA-OAEP SHA-256 kluczem publicznym MF
   (klucz z GET /security/public-key-certificates)
POST /auth/ksef-token                 → authenticationToken, referenceNumber
GET  /auth/{referenceNumber}          → polling aż do sukcesu
POST /auth/token/redeem               → accessToken (JWT, minuty) + refreshToken (do 7 dni)
POST /auth/token/refresh              → nowy accessToken
```

Kontekst logowania: `ContextIdentifier` typu `Nip` = mój NIP.

## 3a. Weryfikacja na środowisku TEST (2026-10-11)

Uwierzytelnienie tokenem KSeF sprawdzone ręcznie (curl + openssl) — działa zgodnie z opisem:
`/security/public-key-certificates` (certyfikat z `usage = KsefTokenEncryption`) → `/auth/challenge` → RSA-OAEP (SHA-256, MGF1 SHA-256) `token|timestampMs` → `/auth/ksef-token` → `/auth/{ref}` (status 200) → `/auth/token/redeem` (accessToken ważny ~15 min).
`/invoices/query/metadata` dla `Subject1` i `Subject2` odpowiada poprawnie; zakres > 100 dni zwraca błąd `21405 'dateRange' must not exceed 100 days`.

Faktura testowa wystawiona w Aplikacji Podatnika KSeF (TEST) i pobrana przez API (`GET /invoices/ksef/{ksefNumber}`) — obserwacje do generatora i parsera FA(3):

- Aplikacja Podatnika zapisuje cały adres (ulica + kod i miasto) w jednym polu `AdresL1` ze znakiem nowej linii; `AdresL2` puste → parser musi obsługiwać adres wieloliniowy w `AdresL1`.
- Wymagane elementy, które musi zawierać nasz generator: `Podmiot2/JST` i `Podmiot2/GV` (2 = nie), cała sekcja `Fa/Adnotacje` (`P_16`, `P_17`, `P_18`, `P_18A`, `Zwolnienie/P_19N`, `NoweSrodkiTransportu/P_22N`, `P_23`, `PMarzy/P_PMarzyN`).
- Kwoty bez zbędnych zer (`233`, `53.59`); `P_6` obecne, gdy data dostawy ≠ data wystawienia.
- Metadane faktury: `invoiceType = Vat`, `formCode.value = FA`, `buyer.identifier = {type: Nip, value: …}`.

## 4. Wystawienie faktury (sesja interaktywna)

```
1. wygeneruj klucz AES-256 + IV (128 bit), zaszyfruj klucz RSA-OAEP kluczem publicznym MF
2. POST /sessions/online                          (formCode FA(3), zaszyfrowany klucz, IV) → referenceNumber sesji, ważna 12 h
3. zbuduj XML FA(3), zaszyfruj AES-256-CBC/PKCS#7
4. POST /sessions/online/{ref}/invoices           (hash SHA-256 + rozmiar oryginału i szyfrogramu, treść Base64) → referenceNumber faktury
5. GET  /sessions/{ref}/invoices/{invoiceRef}     → polling: status weryfikacji, po sukcesie numer KSeF
6. POST /sessions/online/{ref}/close              → generowanie zbiorczego UPO
7. GET  .../upo                                   → UPO (opcjonalnie)
```

Weryfikacja faktury jest **asynchroniczna** — UI musi pokazać stan „wysłana, oczekuje na numer KSeF” i obsłużyć odrzucenie (błędy walidacji XSD / biznesowe).

Największy nakład pracy po naszej stronie: **budowa poprawnego XML FA(3)** (schemat `schemat_FA(3)_v1-0E.xsd`) — pola `Podmiot1`, `Podmiot2`, `Fa` (P_1 data wystawienia, P_2 numer, P_13_x / P_14_x sumy wg stawek, P_15 brutto, `FaWiersz` pozycje, `Platnosc` itd.).

## 5. Lista faktur (sprzedażowe i zakupowe)

`POST /invoices/query/metadata?pageOffset=&pageSize=(10..250)&sortOrder=Asc|Desc`

| Filtr | Znaczenie dla nas |
|---|---|
| `subjectType` (wymagany) | `Subject1` = jestem sprzedawcą → **sprzedażowe**; `Subject2` = jestem nabywcą → **zakupowe** |
| `dateRange` (wymagany) | `dateType`: `Issue` (data wystawienia), `Invoicing` (przyjęcie w KSeF), `PermanentStorage`; **maks. 100 dni** — filtr miesięczny mieści się w jednym zapytaniu |
| `sellerNip`, `buyerIdentifier` | tylko **dokładne** dopasowanie |
| — | **brak filtra po nazwie firmy** |

Metadane zawierają: numer KSeF, numer faktury, daty, sprzedawca (NIP, nazwa), nabywca (identyfikator, nazwa), netto/VAT/brutto, waluta, rodzaj faktury (`Vat`, `Kor`, `Zal`…), `hasAttachment`.

**Wniosek:** filtr NIP / nazwa firmy realizujemy **po naszej stronie** na pobranej liście miesiąca (prosty „contains” po NIP i nazwie).

**Data dostawy:** metadane **nie zawierają** daty dostawy (`P_6`) i KSeF nie filtruje po niej. Filtr miesiąca po dacie dostawy (decyzja D2) wymaga treści XML każdej faktury → rozwiązanie: [ADR 0003](../adr/0003-lokalne-lustro-faktur.md).

## 6. Szczegóły faktury

`GET /invoices/ksef/{ksefNumber}` → surowy XML faktury (`application/xml`). Musimy go sparsować (FA(2)/FA(3)) i wyrenderować czytelnie. Faktury zakupowe mogą przychodzić w różnych wariantach (zaliczkowe, korygujące, z załącznikiem) — wizualizacja musi tolerować brakujące/nieznane pola.

## 7. Limity API (PROD, per kontekst + adres IP)

| Operacja | req/s | req/min | req/h |
|---|---|---|---|
| `POST /invoices/query/metadata` (lista) | 8 | 16 | **20** |
| `GET /invoices/ksef/{ksefNumber}` (szczegóły) | 8 | 16 | **64** |
| `POST /sessions/online` | 10 | 30 | 120 |
| `POST /sessions/online/{ref}/invoices` | 10 | 30 | 180 |

W godz. 20:00–06:00 limity pobierania są wyższe. Po przekroczeniu: `429` z informacją, kiedy ponowić.

**Wniosek:** 20 zapytań o listę na godzinę to mało przy „klikaniu” po miesiącach i odświeżaniu. Potrzebny **cache** (pamięć serwera lub urządzenia) — lista miesiąca i XML faktury są praktycznie niezmienne. Ręczny przycisk „Odśwież z KSeF”.

## 8. Kod QR (KOD I)

Na wizualizacji faktury online umieszcza się link weryfikacyjny:
`https://qr.ksef.mf.gov.pl/invoice/{NIP sprzedawcy}/{DD-MM-RRRR}/{SHA-256 pliku XML w Base64URL}`
Przydatne, jeśli będziemy generować PDF/wydruk faktury dla kontrahenta.

## 9. Numer KSeF

35 znaków: `NIP-RRRRMMDD-XXXXXXXXXXXX-CC` (CC = CRC-8). Można go walidować lokalnie.

## 10. Biblioteki klienckie

| Biblioteka | Status | Uwagi |
|---|---|---|
| [CIRFMF/ksef-client-java](https://github.com/CIRFMF/ksef-client-java) | Oficjalna (MF), MIT | Java 11, `java.net.http.HttpClient`; dystrybucja przez GitHub Packages (wymaga PAT `read:packages`); szyfrowanie, XAdES, buildery żądań. **Nie zadziała wprost na Androidzie** (brak `java.net.http`). |
| [CIRFMF/ksef-client-csharp](https://github.com/CIRFMF/ksef-client-csharp) | Oficjalna (MF) | .NET |
| `@ksefnik/*` (npm) | Społecznościowa, wersje 0.x | Niedojrzała — nie polecam jako fundamentu. |

Kryptografia potrzebna do integracji (RSA-OAEP SHA-256, AES-256-CBC, SHA-256) jest dostępna natywnie w Node.js (`node:crypto`), w Web Crypto API i w JCA (Java/Kotlin/Android) — **własna implementacja w TypeScript lub Kotlinie jest realna** (ok. 10 endpointów). Biblioteka Java daje przewagę głównie w uwierzytelnianiu XAdES (którego przy tokenie KSeF nie potrzebujemy).

**Android:** `Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")` domyślnie używa na Androidzie **SHA-1 w MGF1**. KSeF wymaga MGF1 z SHA-256 — trzeba jawnie przekazać `OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)`.

## 11. Dane pomocnicze spoza KSeF (do rozważenia)

- **Biała lista podatników VAT** (`wl-api.mf.gov.pl`, publiczne, bez klucza) — po wpisaniu NIP nabywcy można pobrać nazwę, adres, status VAT, rachunki. Ułatwia wystawianie faktury bez bazy kontrahentów.
