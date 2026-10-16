# Code review 1 — analiza uwag i plan poprawek

- Data: 2026-10-16
- Źródło: komentarze `// TODO CODE REVIEWED` dodane przez recenzenta w 22 plikach `.kt` (31 uwag).
- Zakres tego dokumentu: ocena zasadności każdej uwagi i propozycja poprawki. **Kod nie jest jeszcze zmieniany.**
- Powiązany dokument: [audyt-security.md](audyt-security.md) — część uwag pokrywa się z ustaleniami audytu (zaznaczone przy zadaniu).

## Jak czytać

- **Ocena:** *Zasadna* — problem istnieje i warto go poprawić; *Częściowo zasadna* — problem istnieje, ale opis przecenia skutki albo proponowana poprawka wymaga korekty; *Niezasadna* — opis jest błędny, ewentualnie zostaje drobna poprawka stylu.
- **Priorytet:** *Wysoki* — łatwy do wywołania crash albo utrata/niespójność danych; *Średni* — realny, ale rzadki scenariusz; *Niski* — scenariusz brzegowy, wydajność, porządki; *Brak* — bez zmian w kodzie.
- Przy realizacji każdego zadania: usunąć komentarz `// TODO CODE REVIEWED`, dopisać test jednostkowy (o ile wskazany), `./gradlew build` bez ostrzeżeń. Teksty w UI po polsku, kod i komentarze po angielsku.

## Podsumowanie

| Zrobione | Nr | Miejsce                            | Temat | Ocena | Priorytet |
|----------|---|------------------------------------|---|---|---|
| [x]      | [REV-01](#rev-01) | `InvoiceSendService.kt:106`        | Wyjątek przy pobieraniu kursu ostatecznego | Częściowo zasadna | Niski |
| [x]      | [REV-02](#rev-02) | `InvoiceSendService.kt:156`        | Kolejność sprzątania po przyjęciu faktury | Zasadna | Średni |
| [x]      | [REV-03](#rev-03) | `IssueStorage.kt:38`               | Ignorowany wynik usunięcia szkicu | Zasadna | Niski |
| [x]      | [REV-04](#rev-04) | `UnsentInvoices.kt:69`             | Ignorowany wynik usunięcia faktury niewysłanej | Zasadna | Niski |
| []       | [REV-05](#rev-05) | `KsefInvoiceSender.kt:72`          | Utracona odpowiedź po wysłaniu faktury | Częściowo zasadna | Średni |
| [x]      | [REV-06](#rev-06) | `KsefInvoiceSender.kt:98`          | Status 200 bez numeru KSeF traktowany jako odrzucenie | Zasadna | Niski |
| [x]      | [REV-07](#rev-07) | `KsefInvoiceSender.kt:115`         | UPO tracone przy każdym błędzie HTTP | Zasadna | Średni |
| [x]      | [REV-08](#rev-08) | `MainViewModel.kt:36`              | Nowy token zapisany przed weryfikacją | Zasadna | Średni |
| [x]      | [REV-09](#rev-09) | `KsefTokenStore.kt:41`             | Nieczytelny szyfrogram tokena | Zasadna | Średni |
| [x]      | [REV-10](#rev-10) | `KsefTokenManager.kt:63`           | Pełne uwierzytelnienie po każdym błędzie odświeżenia | Zasadna | Niski |
| [x]      | [REV-11](#rev-11) | `KsefPublicKeyProvider.kt:38`      | Certyfikaty MF w pamięci na cały czas życia procesu | Zasadna | Niski |
| []       | [REV-12](#rev-12) | `InvoiceSyncService.kt:70`         | Nieobsłużony wyjątek synchronizacji | Zasadna | Wysoki |
| [x]      | [REV-13](#rev-13) | `MainViewModel.kt:48`              | Czyszczenie cache na wątku UI i wyścig z synchronizacją | Zasadna | Średni |
| [x]      | [REV-14](#rev-14) | `KsefInvoiceClient.kt:103`         | Cała paczka eksportu w pamięci | Zasadna | Niski |
| [x]      | [REV-15](#rev-15) | `KsefInvoiceClient.kt:175`         | Rozszerzenie `.XML` usuwane z rozróżnianiem wielkości liter | Zasadna | Niski |
| []       | [REV-16](#rev-16) | `InvoiceMirror.kt:78`              | Lustro faktur nieszyfrowane na dysku | Zasadna (ryzyko) | Średni — decyzja |
| []       | [REV-18](#rev-18) | `InvoiceNumbering.kt:45`           | `toInt()` na długim numerze — crash formularza | Zasadna | Wysoki |
| [x]      | [REV-19](#rev-19) | `InvoiceNumbering.kt:24`           | Przepełnienie `Int` w sugerowanym numerze | Zasadna | Niski |
| [x]      | [REV-20](#rev-20) | `DraftValidator.kt:73`             | Brak limitów liczby cyfr FA(3) | Zasadna | Niski |
| [x]      | [REV-21](#rev-21) | `NipValidator.kt:16`               | `isDigit` przepuszcza cyfry spoza ASCII (NIP) | Zasadna | Niski |
| [x]      | [REV-22](#rev-22) | `BankAccountNumberValidator.kt:18` | `isDigit` przepuszcza cyfry spoza ASCII (NRB) | Zasadna | Niski |
| []       | [REV-23](#rev-23) | `NewInvoiceViewModel.kt:140`       | Przeliczanie numeracji przy każdej zmianie formularza | Częściowo zasadna | Niski |
| [x]      | [REV-24](#rev-24) | `NewInvoiceViewModel.kt:230`       | Podgląd odrzucany po zapisaniu kursu roboczego | Zasadna | Niski |
| [x]      | [REV-25](#rev-25) | `Fa3XmlWriter.kt:229`              | Znaki niedozwolone w XML 1.0 | Zasadna | Średni |
| []       | [REV-26](#rev-26) | `InvoiceXmlParser.kt:186`          | Ochrona przed DOCTYPE/XXE na Androidzie | Częściowo zasadna | Niski |
| []       | [REV-27](#rev-27) | `InvoiceListViewModel.kt:53`       | Filtrowanie i sumy listy na wątku Main | Częściowo zasadna | Niski |
| [x]      | [REV-28](#rev-28) | `InvoiceListViewModel.kt:62`       | `YearMonth.now()` w strefie urządzenia | Zasadna | Niski |
| [x]      | [REV-29](#rev-29) | `InvoiceListLogic.kt:86`           | Zaniżone kwoty niekompletnej faktury niewysłanej | Zasadna | Niski |
| [x]      | [REV-30](#rev-30) | `InvoiceDetailsContent.kt:62`      | „Zbędne” `!!` w podsumowaniu VAT | Niezasadna (styl) | Niski |
| [x]      | [REV-31](#rev-31) | `InvoiceDetailsContent.kt:171`     | „Zbędne” `!!` w cenach pozycji | Niezasadna (styl) | Niski |

Bilans: 22 zasadne, 6 częściowo zasadnych, 3 niezasadne (w tym 2 z sensowną poprawką stylu).

---

## Wysyłka faktury

<a id="rev-01"></a>
### REV-01 — Wyjątek przy pobieraniu kursu ostatecznego zostawia stan `Sending`

- **Miejsce:** [InvoiceSendService.kt:106](../app/src/main/kotlin/pl/xsenia/fv/issue/InvoiceSendService.kt#L106) — `send()`, blok `finalRates.rateFor(...)` przed `run(...)`.
- **Uwaga recenzenta:** jeśli `rateFor` rzuci wyjątek (zamiast zwrócić `Failure`), `run()` się nie wykona, stan zostanie `Sending` na zawsze i zablokuje ponowienie.
- **Ocena:** częściowo zasadna · **Priorytet:** niski.
- **Analiza:** `NbpClient` zamienia `IOException` i błędy parsowania na `ExchangeRateResult.Failure`, a `CachedExchangeRateSource` łapie błędy pliku, więc wyjątek jest mało prawdopodobny (np. `RuntimeException` silnika HTTP). Skutek jest jednak gorszy niż opisany: korutyna działa w `applicationScope` (`SupervisorJob() + Dispatchers.Main`, bez `CoroutineExceptionHandler`), więc nieobsłużony wyjątek **zamyka aplikację**, a nie tylko zawiesza stan.
- **Propozycja:** objąć pobranie kursu tym samym `try` co wysyłkę, np. wydzielić `suspend fun finalDraft(...)`, a w `catch (e: Exception)` (po przepuszczeniu `CancellationException`) ustawić `SendState.RateUnavailable(draft.invoiceNumber, ExchangeRateResult.Failure(Reason.NETWORK), unsentId)`. Faktura zostaje tym, czym była (szkic albo niewysłana), więc to zgodne z ADR 0008 / Q3.
- **Testy:** `InvoiceSendServiceTest` — `FinalRateSource` rzucający `IllegalStateException` → stan `RateUnavailable`; kolejne `send()` działa.

<a id="rev-02"></a>
### REV-02 — Usunięcie `pending-send.json` przed usunięciem źródła faktury

- **Miejsce:** [InvoiceSendService.kt:156](../app/src/main/kotlin/pl/xsenia/fv/issue/InvoiceSendService.kt#L156) — `run()`, `storage.clearPending()` przed `unsent.delete()` / `storage.clearDraft()`.
- **Uwaga recenzenta:** zamknięcie procesu między tymi krokami sprawi, że przyjęta faktura po restarcie znów wygląda na gotową do wysłania.
- **Ocena:** zasadna · **Priorytet:** średni.
- **Analiza:** okno czasowe jest krótkie, ale skutek mylący: wraca szkic albo faktura niewysłana, chociaż faktura jest już w KSeF. Ponowna wysyłka zostałaby najpewniej odrzucona przez KSeF jako duplikat (440), a po synchronizacji numer trafi do zajętych (`NUMBER_DUPLICATE`), więc podwójnej faktury raczej nie będzie. Zostaje jednak niespójny stan i konieczność ręcznego sprzątania.
- **Propozycja:** odwrócić kolejność: najpierw UPO, potem usunięcie źródła (`unsent.delete` / `clearDraft`), **na końcu** `clearPending()`. Jeśli proces zginie po usunięciu źródła, `pending-send.json` zostaje, a `resumeIfPending()` przy starcie ponownie pobierze status (operacja idempotentna), powtórzy sprzątanie (usunięcie nieistniejącego pliku nic nie robi) i pokaże `Accepted`. Dodatkowy znacznik nie jest potrzebny.
- **Testy:** test kolejności operacji na fałszywym magazynie; test `resumeIfPending()`, gdy szkic/niewysłana już nie istnieje → `Accepted`.

<a id="rev-03"></a>
### REV-03 — `clearDraft()` ignoruje nieudane usunięcie pliku

- **Miejsce:** [IssueStorage.kt:38](../app/src/main/kotlin/pl/xsenia/fv/issue/IssueStorage.kt#L38) — `clearDraft()`; to samo dotyczy `clearPending()` (linia 47).
- **Uwaga recenzenta:** `File.delete()` może zwrócić `false` bez wyjątku; po udanej wysyłce stary szkic może wrócić przy następnym uruchomieniu.
- **Ocena:** zasadna · **Priorytet:** niski (w prywatnym katalogu aplikacji usunięcie praktycznie zawsze się udaje, ale koszt poprawki jest minimalny).
- **Propozycja:** prywatny helper `deleteIfExists(file)`: `if (!file.delete() && file.exists()) throw IOException("Cannot delete ${file.name}")`, użyty w `clearDraft()` i `clearPending()`. Razem z REV-02 wyjątek w sprzątaniu trafia do `catch (e: Exception)` w `run()`, a ponieważ `pending-send.json` jeszcze istnieje, stan to `Failed(pendingInKsef = true)`. „Ponów” tylko sprawdza status i powtarza sprzątanie, bez ponownej wysyłki.
- **Testy:** `IssueStorageTest` — usunięcie nieistniejącego pliku nie rzuca; plik, którego nie da się usunąć (np. katalog o tej nazwie), rzuca `IOException`.

<a id="rev-04"></a>
### REV-04 — `UnsentInvoiceStore.delete()` ignoruje nieudane usunięcie pliku

- **Miejsce:** [UnsentInvoices.kt:69](../app/src/main/kotlin/pl/xsenia/fv/issue/UnsentInvoices.kt#L69) — `UnsentInvoiceStore.delete()`.
- **Uwaga recenzenta:** repozytorium usuwa fakturę z pamięci, a po restarcie wraca ona z dysku i może zostać wysłana ponownie.
- **Ocena:** zasadna · **Priorytet:** niski.
- **Analiza:** `UnsentInvoiceRepository.delete()` aktualizuje stan dopiero po `store.delete(id)`, więc wyjątek z magazynu wystarczy, żeby pamięć i dysk się nie rozjechały.
- **Propozycja:** jak w REV-03: `if (!file.delete() && file.exists()) throw IOException(...)`. Wywołujący: `InvoiceSendService.run()` (wtedy `Failed(pendingInKsef = true)`, por. REV-02) i `NewInvoiceViewModel.deleteUnsent()`. Ten drugi powinien złapać wyjątek i pokazać komunikat („Nie udało się usunąć faktury”), zamiast zamykać ekran.
- **Testy:** `UnsentInvoiceStoreTest` — jak w REV-03; test repozytorium: po nieudanym usunięciu faktura zostaje w `invoices`.

<a id="rev-05"></a>
### REV-05 — Utracona odpowiedź na `POST` faktury może skończyć się ponowną wysyłką

- **Miejsce:** [KsefInvoiceSender.kt:72](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/sending/KsefInvoiceSender.kt#L72) — `send()`, `api.sendInvoice(...)` przed `onSubmitted(...)`.
- **Uwaga recenzenta:** jeśli KSeF przyjmie żądanie, a odpowiedź zginie, `SubmittedInvoice` nie zostanie zapisany i ponowienie może wysłać tę samą fakturę jeszcze raz.
- **Ocena:** częściowo zasadna · **Priorytet:** średni.
- **Analiza:** scenariusz jest realny (timeout lub zerwane połączenie po stronie odpowiedzi). `InvoiceSendService` dostaje wtedy `IOException`, nie ma `pending`, więc zapisuje fakturę jako niewysłaną (`Failed`, `pendingInKsef = false`). Podwójnej faktury w KSeF najpewniej jednak nie będzie: KSeF wykrywa duplikat (status 440, `originalKsefNumber` w `extensions`) po NIP-ie sprzedawcy, rodzaju faktury i numerze `P_2`. Ta reguła jest do potwierdzenia na KSeF TEST. Rzeczywisty problem jest inny: faktura jest w KSeF, a aplikacja pokazuje ją jako odrzuconą lub niewysłaną.
- **Propozycja (dwa kroki):**
  1. *Minimum:* w `InvoiceSendService.run()` odrzucenie 440 z `originalKsefNumber` przy tym samym numerze faktury traktować jako „faktura już jest w KSeF”: stan `Accepted(ksefNumber = originalKsefNumber, upoSaved = false)`, usunięcie źródła, `onAccepted()` (synchronizacja). Opcjonalnie najpierw dialog z potwierdzeniem.
  2. *Pełne uzgodnienie:* zapisać `PendingSend` z numerem referencyjnym sesji **przed** `POST` faktury (`invoiceReferenceNumber` jako nullable), a przy niepewnym błędzie odpytać listę faktur sesji (`GET /sessions/{ref}/invoices`) i kontynuować od znalezionej faktury. To wymaga zmiany `SubmittedInvoice`/`PendingSend` z migracją zapisanego JSON-a.
- **Testy:** fałszywy backend: `IOException` przy wysyłce, a przy ponowieniu `KsefInvoiceRejectedException(440, originalKsefNumber = …)` → `Accepted`. Regułę duplikatu sprawdzić jednym testem integracyjnym na KSeF TEST (zmyślone dane; każde uruchomienie tworzy nieusuwalną fakturę).

<a id="rev-06"></a>
### REV-06 — Status sukcesu bez numeru KSeF zapisywany jako odrzucenie

- **Miejsce:** [KsefInvoiceSender.kt:98](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/sending/KsefInvoiceSender.kt#L98) — `awaitResult()`, warunek `final.status.code != SUCCESS || ksefNumber == null`.
- **Uwaga recenzenta:** status 200 bez numeru KSeF to niespójność protokołu, a nie potwierdzone odrzucenie. Zapisanie go jako odrzuconego zachęca do ponownej wysyłki.
- **Ocena:** zasadna · **Priorytet:** niski (KSeF zawsze podaje numer przy 200, ale reakcja na niespójność powinna być bezpieczna).
- **Propozycja:** rozdzielić warunki: `status != SUCCESS` → `KsefInvoiceRejectedException`; `SUCCESS && ksefNumber == null` → `KsefProtocolException("Invoice accepted without a KSeF number")`. W `InvoiceSendService` trafia to do ogólnego `catch`, `pending-send.json` zostaje, więc mamy `Failed(pendingInKsef = true)`, a „Ponów” tylko odpytuje status.
- **Testy:** `KsefInvoiceSenderTest` — odpowiedź 200 bez `ksefNumber` → `KsefProtocolException`; test serwisu: pending nie jest czyszczony.

<a id="rev-07"></a>
### REV-07 — Każdy błąd HTTP przy pobieraniu UPO kończy się brakiem UPO na stałe

- **Miejsce:** [KsefInvoiceSender.kt:115](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/sending/KsefInvoiceSender.kt#L115) — `downloadUpo()`.
- **Uwaga recenzenta:** tłumione są wszystkie błędy HTTP, także chwilowe 5xx. Faktura jest przyjęta, ale bez UPO. Trzeba ponawiać albo zachować stan pozwalający pobrać UPO później.
- **Ocena:** zasadna · **Priorytet:** średni.
- **Analiza:** KDoc mówi „it can be fetched later”, ale w aplikacji nie ma mechanizmu ponownego pobrania. `SendState.Accepted(upoSaved = false)` jest stanem końcowym, a `SubmittedInvoice` zostaje usunięty razem z `pending-send.json`. UPO jest urzędowym potwierdzeniem przyjęcia, więc jego brak ma znaczenie dla użytkownika.
- **Propozycja:**
  1. W `downloadUpo()` kilka ponowień z odstępem (np. 3 × 2 s) dla 404 (UPO jeszcze nie wygenerowane) i 5xx. Inne 4xx dalej dają `null`.
  2. Przy `upo == null` zapisać w `IssueStorage` wpis „brakujące UPO” (`issue/upo-missing.json`: `ksefNumber` + `SubmittedInvoice`) i ponawiać pobranie przy starcie aplikacji / po synchronizacji. Po sukcesie zapisać UPO i usunąć wpis.
  3. Opcjonalnie: przycisk „Pobierz UPO” w szczegółach faktury sprzedażowej bez UPO.
- **Testy:** `KsefInvoiceSenderTest` — 503, 503, 200 → UPO pobrane; 404 trzy razy → `null`. Test serwisu: `Accepted(upoSaved = false)` zapisuje wpis do ponowienia; ponowienie usuwa wpis.

---

## Token KSeF i uwierzytelnianie

<a id="rev-08"></a>
### REV-08 — Nowy token nadpisuje działający przed weryfikacją

- **Miejsce:** [MainViewModel.kt:36](../app/src/main/kotlin/pl/xsenia/fv/ui/MainViewModel.kt#L36) — `saveToken()`.
- **Uwaga recenzenta:** kandydat zastępuje zapisany token, zanim uwierzytelnienie się powiedzie, więc literówka albo unieważniony token niszczy działającą konfigurację.
- **Ocena:** zasadna · **Priorytet:** średni.
- **Analiza:** skutek jest poważniejszy, niż wynika z uwagi. (1) Token KSeF jest pokazywany tylko raz przy generowaniu, więc użytkownik może nie mieć jak przywrócić poprzedniego. (2) Token z innym NIP-em natychmiast przełącza aktywny profil (`ProfileManager` obserwuje `tokenNip`). Dodatkowo `KsefAccess` trzyma `KsefTokenManager` per NIP z tokenami sesji, więc po wymianie tokena przy tym samym NIP-ie stara sesja jest używana do wygaśnięcia.
- **Propozycja:**
  - `saveToken(candidate)`: najpierw `connectionChecker.check(KsefTokenStore.nipOf(candidate), candidate)`, a `tokenStore.save(candidate)` tylko przy `ConnectionCheckResult.Success`.
  - Przy `AuthenticationFailed` zostaje stary token i komunikat w UI („Token odrzucony przez KSeF — zapisany token bez zmian”). Przy błędzie sieci, limicie lub błędzie API tokena też nie zapisujemy; komunikat „Nie udało się sprawdzić tokena — spróbuj ponownie”. Ewentualne „Zapisz mimo to” to decyzja użytkownika.
  - Po zapisie wywołać `ksefAccess.tokenManager(nip).invalidate()`, żeby nie używać sesji starego tokena.
  - `ConnectionUiState` rozszerzyć o rozróżnienie „wynik sprawdzenia kandydata” i „wynik sprawdzenia zapisanego tokena”.
- **Testy:** test view-modelu z fałszywym `KsefConnectionChecker`: odrzucony kandydat → `tokenStore` bez zmian; sukces → zapis.

<a id="rev-09"></a>
### REV-09 — Nieczytelny szyfrogram tokena zawiesza lub zamyka aplikację

- **Miejsce:** [KsefTokenStore.kt:41](../app/src/main/kotlin/pl/xsenia/fv/security/KsefTokenStore.kt#L41) — `read()`.
- **Uwaga recenzenta:** uszkodzony szyfrogram albo przywrócona preferencja bez klucza w Keystore powoduje wyjątek przy odszyfrowaniu. Wywołujący zostawiają stan połączenia lub synchronizacji zawieszony, zamiast poprosić o nowy token.
- **Ocena:** zasadna · **Priorytet:** średni.
- **Analiza:** przywrócenie z kopii jest mało prawdopodobne (`allowBackup="false"`), ale utrata lub uszkodzenie klucza Keystore zdarza się na niektórych urządzeniach (aktualizacja systemu, zmiana blokady ekranu). Skutki w wywołujących:
  - `MainViewModel.runConnectionCheck()` — wyjątek w `viewModelScope` zamyka aplikację, a `checking = true` zostaje,
  - `InvoiceSyncService.synchronize()` — wyjątek spoza mapowania `toKsefFailure()`, czyli crash (REV-12),
  - wysyłka — trafia do ogólnego `catch`, więc `Failed` z niejasnym komunikatem.
- **Propozycja:** w `read()` łapać `GeneralSecurityException` i `IllegalArgumentException` (zły Base64) i rzucać dedykowany `KsefFailureException(KsefFailure.TokenUnreadable)`. Nowy wariant `KsefFailure.TokenUnreadable` z komunikatem „Nie można odczytać zapisanego tokena — wpisz go ponownie w Ustawieniach”. `KsefConnectionChecker` / `runConnectionCheck()` mapują go na wynik zamiast wyjątku. Nie kasować szyfrogramu automatycznie, tylko skierować użytkownika do wpisania tokena.
- **Testy:** `KsefTokenStore` z `KeystoreCipher`, który rzuca `AEADBadTagException` → `TokenUnreadable`; test synchronizacji: stan `error = TokenUnreadable`, `running = false`.

<a id="rev-10"></a>
### REV-10 — Każdy błąd odświeżenia tokena wymusza pełne uwierzytelnienie

- **Miejsce:** [KsefTokenManager.kt:63](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/auth/KsefTokenManager.kt#L63) — `refreshOrAuthenticate()`.
- **Uwaga recenzenta:** każdy błąd HTTP (także chwilowe 5xx) uruchamia pełne uwierzytelnienie. Ponownie uwierzytelniać należy tylko przy nieważnym lub unieważnionym refresh tokenie.
- **Ocena:** zasadna · **Priorytet:** niski.
- **Analiza:** `KsefApiException` obejmuje 4xx/5xx poza 429 (429 to osobny `KsefRateLimitException`, który nie jest łapany, więc ten przypadek jest już poprawny). Przy 5xx pełne uwierzytelnienie to kilka kolejnych wywołań (challenge, uwierzytelnienie tokenem, odpytywanie statusu, odbiór tokenów), które przy awarii KSeF też się nie udadzą, a niepotrzebnie odszyfrowują token i zużywają limity.
- **Propozycja:** `catch (e: KsefApiException) { if (e.httpStatus in setOf(400, 401, 403)) authenticate() else throw e }`. Listę statusów potwierdzić w dokumentacji `POST /auth/token/refresh` (ksef-docs).
- **Testy:** `KsefTokenManagerTest` — refresh 503 → wyjątek, `authenticate` niewywołane; refresh 401 → `authenticate`.

<a id="rev-11"></a>
### REV-11 — Certyfikaty kluczy publicznych MF w pamięci bez odświeżania

- **Miejsce:** [KsefPublicKeyProvider.kt:38](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/auth/KsefPublicKeyProvider.kt#L38) — `ApiPublicKeyProvider.publicKey()`.
- **Uwaga recenzenta:** lista jest pamiętana przez cały czas życia procesu, więc certyfikat opublikowany później nie zostanie użyty i po wygaśnięciu zapamiętanych kluczy aplikacja przestanie działać.
- **Ocena:** zasadna · **Priorytet:** niski (proces Androida żyje zwykle krótko, a certyfikaty są ważne długo; problemem byłaby wcześniejsza rotacja kluczy przez MF).
- **Propozycja:** zapamiętać czas pobrania; odświeżać listę, gdy (a) minął TTL (np. 24 h) albo (b) nie ma ważnego certyfikatu dla danego zastosowania. Przed rzuceniem `KsefProtocolException` jedno ponowne pobranie. `Clock` jest już wstrzykiwany.
- **Testy:** fałszywe `KsefAuthApi` + zegar: po wygaśnięciu certyfikatu jest ponowne pobranie i nowy klucz; w ramach TTL tylko jedno wywołanie API.

---

## Synchronizacja i lustro faktur

<a id="rev-12"></a>
### REV-12 — Nieobsłużony wyjątek synchronizacji

- **Miejsce:** [InvoiceSyncService.kt:70](../app/src/main/kotlin/pl/xsenia/fv/sync/InvoiceSyncService.kt#L70) — `syncNow()`, `e.toKsefFailure() ?: throw e`.
- **Uwaga recenzenta:** niezmapowane wyjątki są rzucane dalej przed zresetowaniem `running`; np. uszkodzony indeks lustra zostawia serwis na stałe w stanie „trwa” i blokuje kolejne synchronizacje.
- **Ocena:** zasadna · **Priorytet:** wysoki.
- **Analiza:** jak w REV-01, skutek jest poważniejszy niż opisany. Korutyna działa w `applicationScope` bez `CoroutineExceptionHandler`, więc wyjątek **zamyka aplikację**. Uszkodzony `index.json` albo `sync-state.json` (`SerializationException` w `InvoiceMirror.entries()` / `syncState()`) psuje też `refreshFromMirror()` w `init`, które wywołuje się przy każdym starcie profilu. Plus `syncIfStale()` przy starcie daje pętlę crashy. Zapisy są atomowe, więc uszkodzenie jest mało prawdopodobne, ale skutek byłby dotkliwy. Do tej ścieżki trafia też wyjątek z REV-09.
- **Propozycja:**
  - `syncNow()`: `try { … } catch (CancellationException) { throw } catch (e: Exception) { e.toKsefFailure() ?: KsefFailure.Ksef(e.message) } finally { _state.update { it.copy(running = false, progress = null) } }`. Ewentualnie osobny wariant `KsefFailure.Unexpected` z komunikatem „Nieoczekiwany błąd synchronizacji”.
  - `refreshFromMirror()` odporne na błąd odczytu: uszkodzony indeks traktować jak brak lustra (lustro wolno skasować i pobrać ponownie — ADR 0003), czyli w `InvoiceMirror` przy błędzie dekodowania usunąć indeks i `sync-state.json`, żeby następna synchronizacja pobrała dane od początku.
  - Rozważyć `CoroutineExceptionHandler` w `applicationScope` jako ostatnią linię obrony (bez logowania danych faktur).
- **Testy:** `InvoiceSyncServiceTest` — synchronizator rzucający `IllegalStateException` → `running = false`, `error != null`, kolejne `syncNow()` działa; `InvoiceMirrorTest` — uszkodzony `index.json` → pusta lista i wyczyszczony stan synchronizacji.

<a id="rev-13"></a>
### REV-13 — Czyszczenie cache na wątku UI i wyścig z trwającą synchronizacją

- **Miejsce:** [MainViewModel.kt:48](../app/src/main/kotlin/pl/xsenia/fv/ui/MainViewModel.kt#L48) — `clearCache()`; [ProfileSession.kt:56](../app/src/main/kotlin/pl/xsenia/fv/profile/ProfileSession.kt#L56) — `clearMirror()`.
- **Uwaga recenzenta:** oba czyszczenia rekurencyjnie usuwają pliki na wątku wywołującym (UI) i ścigają się z synchronizacją, która może odtworzyć dane lustra po wyczyszczeniu.
- **Ocena:** zasadna · **Priorytet:** średni.
- **Analiza:** usuwanie setek plików XML na Main może zablokować UI. Wyścig jest groźniejszy: synchronizacja w toku po `clear()` dopisze do pustego lustra tylko bieżącą porcję faktur i zapisze `sync-state.json` z nowym znacznikiem. Wcześniejsze faktury nie zostaną już pobrane, bo stan mówi, że są zsynchronizowane, więc lustro pozostaje niekompletne.
- **Propozycja:**
  - W `InvoiceSyncService` dodać `Mutex` obejmujący `synchronize()` i nową metodę `suspend fun clearMirror()`, która anuluje trwającą synchronizację (zapamiętany `Job`, `cancelAndJoin()`), w sekcji krytycznej czyści lustro na `Dispatchers.IO` i odświeża stan.
  - `ProfileSession.clearMirror()` deleguje do serwisu; `MainViewModel.clearCache()` uruchamia całość w `viewModelScope.launch`, a `localCache.clear()` na `Dispatchers.IO`.
  - W UI zablokować przycisk „Wyczyść cache” na czas synchronizacji (prosta dodatkowa bariera).
- **Testy:** `InvoiceSyncServiceTest` — synchronizacja zawieszona na fałszywym źródle; `clearMirror()` → synchronizacja anulowana, lustro puste, stan bez `lastSyncAt`.

<a id="rev-14"></a>
### REV-14 — Cała paczka eksportu i wszystkie pliki XML w pamięci

- **Miejsce:** [KsefInvoiceClient.kt:103](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/invoices/KsefInvoiceClient.kt#L103) — `export()` i `unpack()`.
- **Uwaga recenzenta:** odszyfrowane części są składane w pamięci, kopiowane przez `toByteArray()`, a potem każdy wpis ZIP jest czytany w całości i trzymany, więc duży eksport może wyczerpać pamięć.
- **Ocena:** zasadna · **Priorytet:** niski. Pokrywa się z [audytem, pkt 5](audyt-security.md).
- **Analiza:** szczyt pamięci to ok. 3× rozmiar paczki (części, kopia archiwum, wpisy). Dla jednej firmy i okna 90 dni to zwykle kilka MB, więc dziś bez problemu. Brak limitów nie chroni jednak przed nietypową paczką (np. „zip bomb”).
- **Propozycja:**
  - Odszyfrowane części zapisywać strumieniowo do pliku tymczasowego (katalog wstrzykiwany do `KsefInvoiceClient`, w aplikacji `cacheDir`) i czytać `ZipInputStream` z pliku. Plik usuwać w `finally`.
  - Limity: liczba wpisów, rozmiar pojedynczego XML po dekompresji (np. 5 MB, z zapasem ponad limity rozmiaru faktury w KSeF) i łączny rozmiar po dekompresji. Przekroczenie daje `KsefProtocolException`.
  - Docelowo: przekazywać XML do lustra strumieniowo, zamiast budować `Map<String, ByteArray>` (większa zmiana w `InvoiceExport` / `InvoiceSynchronizer`; można odłożyć).
- **Testy:** ZIP z wpisem przekraczającym limit → `KsefProtocolException`; plik tymczasowy usunięty po sukcesie i po błędzie.

<a id="rev-15"></a>
### REV-15 — `.XML` wielkimi literami nie jest obcinane z nazwy

- **Miejsce:** [KsefInvoiceClient.kt:175](../ksef/src/main/kotlin/pl/xsenia/fv/ksef/invoices/KsefInvoiceClient.kt#L175) — `unpack()`.
- **Uwaga recenzenta:** sprawdzenie rozszerzenia ignoruje wielkość liter, a usunięcie już nie, więc wpis `….XML` trafia pod klucz z `.XML`, wyszukiwanie po numerze KSeF chybia i faktura jest pobierana osobno.
- **Ocena:** zasadna · **Priorytet:** niski (KSeF używa `.xml`, a skutkiem jest tylko zbędne pobranie).
- **Propozycja:** `xmlFiles[name.dropLast(".xml".length)] = bytes`.
- **Testy:** paczka z wpisem `NUMER.XML` → klucz `NUMER`.

<a id="rev-16"></a>
### REV-16 — Lustro faktur (i inne dane) nieszyfrowane na dysku

- **Miejsce:** [InvoiceMirror.kt:78](../mirror/src/main/kotlin/pl/xsenia/fv/mirror/InvoiceMirror.kt#L78) — `writeAtomically()`.
- **Uwaga recenzenta:** XML i metadane faktur są zapisywane jawnie w prywatnych plikach aplikacji. Sandbox chroni przed zwykłymi aplikacjami, ale nie przed odczytem z urządzenia zrootowanego lub odblokowanego. Należy ocenić szyfrowanie w spoczynku i wykluczenie z kopii zapasowych.
- **Ocena:** zasadna jako ryzyko · **Priorytet:** średni, **wymaga decyzji**. Pokrywa się z [audytem, pkt 1](audyt-security.md).
- **Analiza:**
  - Wykluczenie z kopii zapasowych jest już zrobione (`allowBackup="false"`, `data_extraction_rules.xml`), więc ta część uwagi jest spełniona.
  - Jawne pliki dotyczą nie tylko lustra: także profil firmy i kontrahenci, szkic, faktury niewysłane i UPO.
  - Android ma szyfrowanie plików (FBE), więc dane są chronione na wyłączonym lub zablokowanym telefonie. Szyfrowanie w aplikacji kluczem z Keystore **bez** wymogu uwierzytelnienia niewiele zmienia przy rootcie, bo atakujący z rootem może użyć klucza przez proces aplikacji. Realnie chroni przed kopiami plików (analiza obrazu danych, błędne udostępnienie katalogu).
- **Propozycja:** nie poprawiać punktowo. Przygotować ADR „Szyfrowanie danych aplikacji w spoczynku” z decyzjami: zakres plików, klucz (AES-GCM z Keystore, z wymogiem uwierzytelnienia czy bez), utrata klucza (lustro można pobrać ponownie, ale szkic, niewysłane i profil przepadają), migracja jawnych plików, wpływ na wydajność listy. Potem osobne zadania ZAD.
- **Testy:** w ramach przyszłych ZAD.

## Formularz faktury, numeracja i walidacja

<a id="rev-18"></a>
### REV-18 — `toInt()` na długim numerze zamyka aplikację

- **Miejsce:** [InvoiceNumbering.kt:45](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/issue/InvoiceNumbering.kt#L45) — `parse()`.
- **Uwaga recenzenta:** istniejące numery mogą mieć dowolnie długi numer kolejny; `toInt()` rzuca wtedy wyjątek i psuje numerację oraz walidację całego formularza.
- **Ocena:** zasadna · **Priorytet:** wysoki.
- **Analiza:** potwierdzona ścieżka crasha. Użytkownik wpisuje w polu numeru np. `12345678901/10/2026`. `DraftValidator.validate()` → `monthMismatch()` → `parse()` → `NumberFormatException` w `combine` stanu `NewInvoiceViewModel` (`viewModelScope`, Main), więc **aplikacja się zamyka**. Ten sam problem przy `suggest()`, gdy taki numer jest wśród faktur niewysłanych lub sprzedażowych z KSeF. Wtedy formularz wykłada się przy każdym otwarciu.
- **Propozycja:** ograniczyć numer kolejny w wyrażeniu do `\d{1,9}` (`^(\d{1,9})/(\d{2})/(\d{4})$`) i dodatkowo `sequence.toIntOrNull() ?: return null`. Dłuższe numery są wtedy traktowane jak numery w niestandardowym formacie (pomijane przy sugestii, bez ostrzeżenia o miesiącu). Rozwiązuje też REV-19.
- **Testy:** `InvoiceNumberingTest` — `monthMismatch("12345678901/10/2026", …)` → `false` bez wyjątku; `suggest(…, listOf("99999999999/10/2026"))` → `01/10/2026`; `DraftValidatorTest` z takim numerem.

<a id="rev-19"></a>
### REV-19 — Przepełnienie `Int` w sugerowanym numerze

- **Miejsce:** [InvoiceNumbering.kt:24](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/issue/InvoiceNumbering.kt#L24) — `suggest()`, `highest + 1`.
- **Uwaga recenzenta:** gdy największy numer to `Int.MAX_VALUE`, inkrementacja daje liczbę ujemną.
- **Ocena:** zasadna (formalnie) · **Priorytet:** niski — wymaga ręcznie wpisanego numeru `2147483647/MM/RRRR`.
- **Propozycja:** po REV-18 numer kolejny ma najwyżej 9 cyfr, więc `highest + 1 ≤ 1 000 000 000` mieści się w `Int` i przepełnienie jest niemożliwe. Wystarczy dopisać to w KDoc i dodać test brzegowy. Osobna zmiana kodu nie jest potrzebna.
- **Testy:** `suggest(…, listOf("999999999/10/2026"))` → `1000000000/10/2026`; `"2147483647/10/2026"` jest pomijany.

<a id="rev-20"></a>
### REV-20 — Brak limitów liczby cyfr FA(3) dla ilości, cen i kwot

- **Miejsce:** [DraftValidator.kt:73](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/issue/DraftValidator.kt#L73) — `validate()`.
- **Uwaga recenzenta:** sprawdzana jest skala, ale FA(3) ogranicza też liczbę cyfr `TIlosci`, `TKwotowy2` i `TKwotowy`. Duże wartości lub wyliczone sumy przechodzą walidację, a KSeF odrzuca XML.
- **Ocena:** zasadna · **Priorytet:** niski. Wartości są praktycznie nieosiągalne, a skutkiem jest odrzucenie przez KSeF (faktura zostaje jako niewysłana z powodem), bez utraty danych.
- **Analiza (limity z `FA3.xsd`):** `TIlosci` — do 16 cyfr całkowitych, 6 po przecinku (ilość `P_8B`, kurs); `TKwotowy2` — do 14 cyfr całkowitych, 8 po przecinku (cena `P_9A`); `TKwotowy` — do 16 cyfr całkowitych, 2 po przecinku (wartości pozycji, sumy `P_13_x`, `P_14_x`, `P_14_xW`, `P_15`).
- **Propozycja:** nowe typy błędów `LINE_QUANTITY_TOO_LARGE`, `LINE_PRICE_TOO_LARGE`, `AMOUNT_TOO_LARGE` (pozycja, sumy stawek, VAT w PLN, brutto). Sprawdzenie przez `precision() - scale()` po `stripTrailingZeros()`. Komunikaty w UI np. „Za duża ilość”, „Za duża cena”, „Za duża kwota faktury”.
- **Testy:** `DraftValidatorTest` — cena `100000000000000` (15 cyfr) → błąd; wartości graniczne przechodzą; suma przekraczająca limit → `AMOUNT_TOO_LARGE`.

<a id="rev-21"></a>
### REV-21 — `NipValidator` przyjmuje cyfry spoza ASCII

- **Miejsce:** [NipValidator.kt:16](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/validation/NipValidator.kt#L16) — `isValid()`.
- **Uwaga recenzenta:** `Char.isDigit` przyjmuje też cyfry dziesiętne spoza ASCII (np. arabskie, pełnej szerokości). Przechodzą sumę kontrolną, ale nie są poprawnymi cyframi NIP w FA(3)/KSeF.
- **Ocena:** zasadna · **Priorytet:** niski (realne tylko przy wklejeniu takiego tekstu).
- **Analiza:** `digitToInt()` także obsługuje cyfry Unicode, więc taki NIP przejdzie walidację i trafi do profilu, XML-a i zapytań do Białej listy. Ten sam wzorzec występuje w `ProfileManager.kt:58`, `InvoiceListLogic.kt:102`, `Formats.kt:50` i `whitelist/…/WhiteListClient.kt:32`.
- **Propozycja:** `nip.all { it in '0'..'9' }`. Najlepiej wspólny helper `isAsciiDigits()` w module `invoice` (np. `validation/AsciiDigits.kt`) i użycie go we wszystkich wymienionych miejscach (`whitelist` nie zależy od `invoice`, więc tam zmiana lokalna).
- **Testy:** `NipValidatorTest` — poprawny NIP zapisany cyframi `U+0660–U+0669` → `false`.

<a id="rev-22"></a>
### REV-22 — `BankAccountNumberValidator` przyjmuje cyfry spoza ASCII

- **Miejsce:** [BankAccountNumberValidator.kt:18](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/validation/BankAccountNumberValidator.kt#L18) — `isValid()`.
- **Uwaga recenzenta:** jak w REV-21; suma kontrolna przechodzi, a NRB nie spełnia formatu ASCII wymaganego przez KSeF/schemat.
- **Ocena:** zasadna · **Priorytet:** niski.
- **Propozycja:** jak w REV-21 (`isAsciiDigits()`); poprawić też `Formats.kt:50` (formatowanie NRB).
- **Testy:** `BankAccountNumberValidatorTest` — poprawny NRB cyframi pełnej szerokości (`U+FF10–U+FF19`) → `false`.

<a id="rev-23"></a>
### REV-23 — Numeracja i walidacja przeliczane przy każdej zmianie formularza na Main

- **Miejsce:** [NewInvoiceViewModel.kt:140](../app/src/main/kotlin/pl/xsenia/fv/ui/issue/NewInvoiceViewModel.kt#L140) — `combine` budujący `state`.
- **Uwaga recenzenta:** `combine` działa w `viewModelScope` na Main i przy każdej edycji ponownie parsuje całą historię numerów z KSeF i niewysłanych, więc duża historia może spowalniać pisanie.
- **Ocena:** częściowo zasadna · **Priorytet:** niski.
- **Analiza:** przy każdym znaku wykonują się `TakenNumbers.of`, `InvoiceFormCheck.check` (z `isDuplicate`) i `InvoiceNumbering.suggest`, czyli O(n) dopasowań wyrażenia regularnego. Dla jednej firmy n to setki numerów, więc dziś to ułamki milisekundy. **Uwaga do proponowanej poprawki:** przeniesienie całego `combine` poza Main (`flowOn(Dispatchers.Default)`) jest ryzykowne, bo pola tekstowe formularza biorą wartość z `state.form` (np. `NewInvoiceScreen.kt:245`). Asynchroniczna pętla stanu w Compose daje gubienie znaków i skaczący kursor.
- **Propozycja:** zostać na Main, ale nie liczyć od nowa:
  - osobny przepływ z `ksefNumbers` + `unsentInvoices`, który przy ich zmianie buduje `TakenNumberIndex` (`Set<String>` do `isDuplicate` + `Map<YearMonth, Int>` z najwyższym numerem w miesiącu),
  - `InvoiceNumbering.suggest(deliveryDate, index)` i `DraftValidator` korzystają z indeksu, więc koszt na znak to O(1).
  Realizować dopiero, gdy pomiar (np. test z 5000 numerów) pokaże problem; w przeciwnym razie zamknąć jako „nie teraz”.
- **Testy:** testy jednostkowe indeksu; istniejące testy numeracji bez zmian zachowania.

<a id="rev-24"></a>
### REV-24 — Wynik kursu ostatecznego odrzucany, gdy w międzyczasie zapisał się kurs roboczy

- **Miejsce:** [NewInvoiceViewModel.kt:230](../app/src/main/kotlin/pl/xsenia/fv/ui/issue/NewInvoiceViewModel.kt#L230) — `openPreview()`, `if (form.value != current) return@launch`.
- **Uwaga recenzenta:** porównanie całego formularza traktuje automatyczny zapis kursu roboczego jak edycję użytkownika, więc poprawna odpowiedź z kursem ostatecznym jest odrzucana i trzeba ponownie poprosić o podgląd.
- **Ocena:** zasadna · **Priorytet:** niski.
- **Analiza:** scenariusz: faktura w walucie, użytkownik zmienia walutę lub datę i od razu stuka „Podgląd”, gdy kurs roboczy jeszcze się pobiera. `refreshRate()` robi `update { it.copy(exchangeRate = …) }`, więc `form.value != current`, a podgląd znika bez komunikatu (`previewLoading = false`, brak `previewRateProblem`). Precyzując uwagę: winny jest zapis kursu do formularza w `refreshRate()`, nie autozapis szkicu na dysk.
- **Propozycja:** porównywać formularz bez kursu roboczego: `form.value?.copy(exchangeRate = null) != current.copy(exchangeRate = null)`. Do `showPreview` przekazać **aktualny** formularz (`form.value`), żeby `PreviewRate.working` pokazywał właśnie pobrany kurs roboczy. Jeśli zmiana pochodzi od użytkownika, dalej odrzucać, ale wtedy ponowić `openPreview()` albo pokazać komunikat, zamiast milczeć.
- **Testy:** test view-modelu z fałszywym źródłem kursów: kurs roboczy zapisany w trakcie pobierania kursu ostatecznego → podgląd pokazany.

---

## Generowanie i parsowanie XML

<a id="rev-25"></a>
### REV-25 — Znaki niedozwolone w XML 1.0 w generowanej fakturze

- **Miejsce:** [Fa3XmlWriter.kt:229](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/issue/Fa3XmlWriter.kt#L229) — `escape()` / `normalize()`.
- **Uwaga recenzenta:** zamiana znaków znacznikowych na encje nie czyni legalnymi znaków sterujących ani niesparowanych surogatów; tekst wklejony do profilu lub pozycji może dać niepoprawny XML.
- **Ocena:** zasadna · **Priorytet:** średni.
- **Analiza:** `normalize()` zamienia na spację tylko `\s` (spacja, `\t \n \x0B \f \r`). Pozostałe znaki C0 (`U+0000–U+0008`, `U+000E–U+001F`), `U+FFFE/U+FFFF` i niesparowane surogaty przechodzą. Źródło: tekst wklejony z PDF-a lub innej aplikacji. Skutek w aplikacji: podgląd czyta wygenerowany XML parserem (`showPreview` → `InvoiceXmlParser.parse`), który rzuca `InvoiceParseException` na Main, więc **aplikacja się zamyka** po „Podgląd”.
- **Propozycja:** w `normalize()` usuwać punkty kodowe spoza zbioru `Char` XML 1.0 (`#x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]`), iterując po punktach kodowych, a nie po `Char`, żeby poprawne pary surogatów zostały. Usuwanie po cichu jest tu przyjazne użytkownikowi (znaki są niewidoczne), a podgląd pokazuje dokładnie to, co pójdzie do KSeF. Dodatkowo `showPreview` powinien łapać `InvoiceParseException` i pokazywać błąd zamiast crasha. *(Wykonano: usuwanie znaków w `Fa3XmlWriter`. Przechwycenie `InvoiceParseException` w `showPreview` świadomie pominięto: po poprawce generator zawsze zwraca poprawny XML, więc taki wyjątek oznaczałby błąd programisty, a nie dane użytkownika.)*
- **Testy:** `Fa3XmlWriterTest` — nazwa pozycji z `\u0001` i samotnym `\uD800` → XML zgodny z `FA3.xsd`, parser czyta go bez błędu; para surogatów (emoji) zachowana.

<a id="rev-26"></a>
### REV-26 — Ochrona parsera przed DOCTYPE/XXE, gdy funkcja nie jest obsługiwana

- **Miejsce:** [InvoiceXmlParser.kt:186](../invoice/src/main/kotlin/pl/xsenia/fv/invoice/xml/InvoiceXmlParser.kt#L186) — `setFeatureIfSupported()`; kontrola `doctype` po `parse()` w liniach 51–56.
- **Uwaga recenzenta:** jeśli `disallow-doctype-decl` nie jest obsługiwane, kontrola DOCTYPE po parsowaniu jest za późno, żeby zatrzymać rozwiązanie encji zewnętrznej. Trzeba jawnie wyłączyć encje zewnętrzne i dostęp do zewnętrznych DTD/schematów albo przerwać parsowanie.
- **Ocena:** częściowo zasadna · **Priorytet:** niski. Pokrywa się z [audytem, pkt 6](audyt-security.md).
- **Analiza:** na JVM (testy) Xerces obsługuje funkcję, więc DOCTYPE jest odrzucany przed przetwarzaniem. Na Androidzie `DocumentBuilderFactory` (oparty na KXmlParser) obsługuje tylko część funkcji, więc `disallow-doctype-decl` i `FEATURE_SECURE_PROCESSING` są po cichu pomijane. KXmlParser według dostępnej wiedzy nie pobiera zewnętrznych encji, ale rozwija encje zdefiniowane w wewnętrznym podzbiorze DTD (ryzyko „billion laughs”). Źródłem XML-a jest KSeF (faktury walidowane schematem przy przyjęciu) i nasz generator, więc ryzyko praktyczne jest niskie, ale kontrola po fakcie jest słabą obroną.
- **Propozycja (przerwanie przed parsowaniem):**
  - przed `parse()` odrzucić dane, jeśli prolog zawiera `<!DOCTYPE` (wyszukanie w bajtach zdekodowanych jako UTF-8, bez rozróżniania wielkości liter; FA(2)/FA(3) z KSeF są w UTF-8), czyli `InvoiceParseException`,
  - dodatkowo ustawić (gdzie obsługiwane) `external-general-entities=false`, `external-parameter-entities=false`, `load-external-dtd=false`, `isXIncludeAware = false`, a na `DocumentBuilder` `EntityResolver` rzucający `SAXException`,
  - kontrolę `doctype` po parsowaniu zostawić jako drugą linię obrony.
- **Testy:** `InvoiceXmlParserTest` — XML z `<!DOCTYPE …>` (z encją wewnętrzną i zewnętrzną) → `InvoiceParseException`, także gdy fabryka nie obsługuje funkcji (test z fabryką, której `setFeature` zawsze rzuca). Weryfikacja ręczna na emulatorze (API 36).

---

## Listy i szczegóły faktur

<a id="rev-27"></a>
### REV-27 — Filtrowanie, sumy i liczniki miesięcy na wątku Main

- **Miejsce:** [InvoiceListViewModel.kt:53](../app/src/main/kotlin/pl/xsenia/fv/ui/invoices/InvoiceListViewModel.kt#L53) — `combine` budujący `state`.
- **Uwaga recenzenta:** filtrowanie, sortowanie, sumy i liczniki miesięcy przechodzą przez całe lustro na Main po każdej zmianie zapytania lub miesiąca; duże lustro może powodować przycięcia.
- **Ocena:** częściowo zasadna · **Priorytet:** niski.
- **Analiza:** odczyt lustra z dysku jest już na `Dispatchers.IO`; na Main jest tylko praca O(n) w pamięci. Dla jednej firmy od 2026-04 to setki lub tysiące wierszy, czyli pomijalny koszt. Jak w REV-23: pole wyszukiwania bierze wartość z `state.query` (`InvoiceListScreen.kt:107`), więc proste `flowOn(Dispatchers.Default)` na całym `combine` grozi gubieniem znaków w polu.
- **Propozycja:** jeśli pomiar pokaże potrzebę: `query` udostępnić polu wyszukiwania bezpośrednio (osobny `StateFlow`), a pochodne (`visible`, `totals`, `unsentSummary`) liczyć w osobnym przepływie z `flowOn(Dispatchers.Default)`. Od razu i tanio: `months` liczyć tylko przy zmianie wierszy (nie zależą od zapytania ani wybranego miesiąca).
- **Testy:** istniejące testy `InvoiceListLogic`; test view-modelu, że zmiana zapytania nie przelicza `months`.

<a id="rev-28"></a>
### REV-28 — Bieżący miesiąc liczony w strefie czasowej urządzenia

- **Miejsce:** [InvoiceListViewModel.kt:62](../app/src/main/kotlin/pl/xsenia/fv/ui/invoices/InvoiceListViewModel.kt#L62) — także linie 26 i 41.
- **Uwaga recenzenta:** `YearMonth.now()` używa strefy urządzenia, a domyślne wartości faktury — `Europe/Warsaw`; na przełomie miesiąca w podróży wybrany lub bieżący miesiąc może nie zgadzać się z datami faktur.
- **Ocena:** zasadna · **Priorytet:** niski.
- **Propozycja:** `YearMonth.now(AppContainer.POLAND)` we wszystkich trzech miejscach, najlepiej przez jedną funkcję `currentMonth()` (jak `today()` w `NewInvoiceViewModel`). Inne wywołania `now()` bez strefy w `app` i `invoice` nie występują.
- **Testy:** brak sensownego testu jednostkowego bez wstrzykiwania zegara. Opcjonalnie wstrzyknąć `Clock` do view-modelu i sprawdzić 2026-10-31 23:30 UTC → listopad.

<a id="rev-29"></a>
### REV-29 — Niekompletna faktura niewysłana pokazuje zaniżone kwoty

- **Miejsce:** [InvoiceListLogic.kt:86](../app/src/main/kotlin/pl/xsenia/fv/invoices/InvoiceListLogic.kt#L86) — `toRow(unsent, profile)`.
- **Uwaga recenzenta:** niepoprawne pozycje są po cichu pomijane, więc edytowalna lub niekompletna faktura ma zaniżone sumy. Należy pokazać sumę nieznaną albo wyraźnie oznaczyć, że jest częściowa.
- **Ocena:** zasadna · **Priorytet:** niski.
- **Analiza:** „Zapisz bez wysyłania” nie waliduje formularza (US-007, decyzja P6), więc faktura niewysłana może mieć pozycje z pustą lub błędną ilością lub ceną. Lista i podsumowanie niewysłanych (`unsentSummary`) pokazują wtedy kwotę bez tych pozycji, bez żadnego sygnału.
- **Propozycja:** w `InvoiceRow` dodać `amountsIncomplete: Boolean` = istnieje pozycja niepusta (jakiekolwiek pole wypełnione), której ilości lub ceny nie da się odczytać. Całkowicie puste pozycje dalej pomijać. W UI przy takiej kwocie dopisek „(niepełna)” albo kwota z „≈”; `UnsentSummary` z flagą, jeśli którakolwiek faktura jest niekompletna.
- **Testy:** `InvoiceListLogicTest` — pozycja z pustą ceną → `amountsIncomplete = true`; pozycja całkowicie pusta → `false`.

<a id="rev-30"></a>
### REV-30 — „Zbędne” `!!` w podsumowaniu VAT

- **Miejsce:** [InvoiceDetailsContent.kt:62](../app/src/main/kotlin/pl/xsenia/fv/ui/details/InvoiceDetailsContent.kt#L62) — `entry.vat!!`.
- **Uwaga recenzenta:** warunek już robi smart cast tej niezmiennej właściwości, więc `!!` jest zbędne.
- **Ocena:** niezasadna (w opisanej formie) · **Priorytet:** niski (styl).
- **Analiza:** `VatSummaryEntry` jest w module `:invoice`, a ekran w `:app`. Kotlin nie robi smart castu publicznej właściwości z innego modułu („Smart cast to 'BigDecimal' is impossible, because 'entry.vat' is a public API property declared in different module”), więc samo usunięcie `!!` się nie skompiluje. Intencja (unikać `!!`) jest jednak słuszna.
- **Propozycja:** `entry.vat?.let { vat -> stringResource(R.string.details_summary_rate, label, money(entry.net), money(vat)) } ?: stringResource(R.string.details_summary_rate_no_vat, label, money(entry.net))` albo lokalne `val vat = entry.vat` i `if (vat != null)`.
- **Testy:** nie dotyczy (kompilacja + `./gradlew build`).

<a id="rev-31"></a>
### REV-31 — „Zbędne” `!!` w cenach pozycji

- **Miejsce:** [InvoiceDetailsContent.kt:171](../app/src/main/kotlin/pl/xsenia/fv/ui/details/InvoiceDetailsContent.kt#L171) — `line.unitNetPrice!!`, `line.unitGrossPrice!!`.
- **Uwaga recenzenta:** warunki gałęzi już robią smart cast tych cen, więc `!!` jest zbędne.
- **Ocena:** niezasadna (w opisanej formie) · **Priorytet:** niski (styl).
- **Analiza:** jak w REV-30: `InvoiceLine` jest w module `:invoice`, więc smart cast nie zadziała.
- **Propozycja:** lokalne zmienne przed `when`: `val netPrice = line.unitNetPrice` i `val grossPrice = line.unitGrossPrice`, a w gałęziach `netPrice != null` / `grossPrice != null` (smart cast zmiennych lokalnych działa).
- **Testy:** nie dotyczy.

---

## Proponowana kolejność realizacji

1. **Crashe:** REV-18 (+ REV-19), REV-12, REV-25, REV-09, REV-01.
2. **Spójność wysyłki faktury:** REV-02, REV-03, REV-04, REV-06, REV-07, REV-05 (krok 1; krok 2 po decyzji).
3. **Token i sesja KSeF:** REV-08, REV-10, REV-11.
4. **Synchronizacja:** REV-13, REV-15, REV-14.
5. **Drobne poprawki:** REV-20, REV-21, REV-22, REV-24, REV-26, REV-28, REV-29, REV-30, REV-31.
6. **Decyzje / bez zmian teraz:** REV-16 (ADR o szyfrowaniu), REV-17 (komentarz zamiast zmiany), REV-23 i REV-27 (tylko po pomiarze).
