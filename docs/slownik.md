# Słownik pojęć

Obowiązująca terminologia projektu. UI — po polsku, kod — po angielsku.
Zasada: nomenklatura KSeF, chyba że użytkownik zdecyduje inaczej (kolumna „Uwagi”).

| Pojęcie (UI / dokumentacja) | Nazwa w kodzie | Znaczenie | Uwagi |
|---|---|---|---|
| KSeF | `ksef` | Krajowy System e-Faktur | |
| Numer KSeF | `ksefNumber` | 35-znakowy identyfikator faktury nadany przez KSeF | |
| Numer faktury | `invoiceNumber` | Numer nadany przez wystawcę (pole `P_2`); u mnie format `NN/MM/RRRR` wg miesiąca daty dostawy | |
| Data wystawienia | `issueDate` | Pole `P_1` | |
| Data dostawy | `deliveryDate` | Data dokonania dostawy / wykonania usługi: `P_6` → `OkresFa/P_6_Do` → najpóźniejsza `P_6A` → `P_1` | Podstawa filtra miesiąca i numeracji |
| Waluta faktury | `InvoiceCurrency` (`PLN`, `EUR`, `USD`, `GBP`) | `KodWaluty`; domyślnie PLN; rachunek w innej walucie — ostrzeżenie | ADR 0008 |
| Kurs waluty (kurs NBP) | `ExchangeRate` (`rate`, `tableNumber`, `effectiveDate`); w szkicu `SalesInvoiceDraft.exchangeRate` / `effectiveExchangeRate` | Kurs średni (`mid`) z tabeli A NBP, 4 miejsca po przecinku; w FA(3) `KursWaluty` w każdej pozycji i uwaga „Kurs waluty” z numerem tabeli | ADR 0008 |
| Kurs roboczy / kurs ostateczny | `InvoiceForm.exchangeRate` (`SavedExchangeRate`), `InvoiceForm.workingRate` / kurs przy wysyłce | Roboczy — wg daty wystawienia z formularza (szkic, niewysłana); ostateczny — wg dnia wysyłki do KSeF, pokazany w podglądzie | ADR 0008 |
| Dzień odniesienia kursu | `ExchangeRateDateRule.referenceDate` | Wcześniejsza z dat: data dostawy, dzień wystawienia; kurs = tabela NBP z ostatniego dnia roboczego przed nim | ADR 0008 |
| Serwis kursów walut | `ExchangeRateService` (aplikacja) / `NbpClient`, `CachedExchangeRateSource` (moduł `exchangerates`) | Kurs z `api.nbp.pl/api/exchangerates/rates/a/{code}/{date}/`; pamięć podręczna dni z przeszłości w `cacheDir` | ADR 0008 |
| VAT w PLN | `RateTotals.vatPln`, `InvoiceTotals.vatPln` | `P_14_1W`/`P_14_2W`/`P_14_3W` — VAT stawek 23/8/5 przeliczony na PLN na fakturze w walucie obcej: VAT × kurs | ADR 0008 |
| Kwota netto / VAT / brutto | `netAmount` / `vatAmount` / `grossAmount` | Sumy faktury (`P_13_1` / `P_14_1` / `P_15`) | |
| Moja firma | `company` | Dane sprzedawcy (Podmiot1) wprowadzane w Ustawieniach → „Moja firma”; NIP = NIP z tokena KSeF | ADR 0006 (dawniej plik `company.json`, ADR 0004) |
| Profil firmy | `CompanyProfile` (dane), `ProfileSession` (kontekst), `ProfileManager` (aktywny profil) | Wszystkie dane jednej mojej firmy w `profiles/{nip}/`: firma, rachunki, kontrahenci (`CompanyProfileStore`, `ProfileRepository`), szkic, niewysłane, UPO, lustro; aktywny profil = NIP z tokena | ADR 0006; w UI bez przełączania firm |
| Braki do wystawienia faktury | `ProfileGap` (`COMPANY`, `BANK_ACCOUNT`, `COUNTERPARTY`) | Czego brakuje w profilu, by wystawić fakturę: dane firmy, rachunek PLN, kontrahent | ADR 0006 |
| Walidacja danych profilu | `ProfileValidator` → `FieldIssue` (`ProfileField`, `FieldIssueType`) | Sprawdzenie formularza firmy / rachunku / kontrahenta przed zapisem (pola wymagane, NIP, NRB, limity FA(3), e-mail, duplikaty) | ADR 0006 |
| Rachunek bankowy / konto | `bankAccount` | Rachunek mojej firmy (`NrRB` w FA(3)); wiele na firmę; na fakturze domyślny pierwszy w walucie faktury (`defaultBankAccount`), inaczej pierwszy; rachunek w innej walucie — ostrzeżenie | ADR 0006 (dawniej plik `banks.json`), ADR 0008 |
| Kontrahenci | `counterparties` | Lista nabywców w Ustawieniach → „Kontrahenci”; NIP niezmienny po utworzeniu | ADR 0006 (dawniej plik `counterparties.json`) |
| Biała lista (wykaz podatników VAT) | `whitelist` (`WhiteListClient`, `WhiteListResult`, `WhiteListSubject`) | API MF `wl-api.mf.gov.pl` — dane firmy po NIP-ie: nazwa, adres, status VAT, rachunki | ADR 0007 |
| Pobieranie danych po NIP-ie | `CompanyLookupService` → `LookedUpCompany` | Wypełnienie formularza „Moja firma” / „Kontrahent” danymi z Białej listy (ikona pobierania obok NIP-u); komunikaty: `CompanyLookupMessages` | ADR 0007 |
| Status VAT | `vatStatus` / `isActiveVatPayer` | `statusVat` z Białej listy: „Czynny”, „Zwolniony”, …; inny niż czynny → „Firma nie jest czynnym podatnikiem” | ADR 0007 |
| Faktura (treść) | `Invoice` | Faktura odczytana z XML FA(2)/FA(3) przez `InvoiceXmlParser` | |
| Pozycja faktury | `InvoiceLine` | `FaWiersz`: nazwa `P_7`, jednostka `P_8A`, ilość `P_8B`, cena jedn. netto `P_9A`, wartość netto `P_11`, stawka `P_12` | Wartość brutto pozycji wyliczana do wyświetlenia |
| Stawka VAT | `VatRate` (`RATE_23`, `RATE_8`, `RATE_5`, `RATE_0`, `NOT_TAXABLE`) | Stawka pozycji (`P_12`): `23`, `8`, `5`, `0 KR`, `np II` | ADR 0005 |
| np (niepodlegające, reverse charge) | `VatRate.NOT_TAXABLE` | Pozycja, w której VAT rozlicza nabywca — w KSeF `np II` (art. 100 ust. 1 pkt 4), adnotacja `P_18 = 1` i uwaga „Reverse charge” | Dawniej „nd”; nie mylić z 0% ani ze zwolnieniem (`zw`) |
| Uwagi | `DodatkowyOpis` (`REMARKS_KEY`) | Klucz–wartość w FA(3); „Uwagi: Reverse charge” na fakturach ze stawką „np” | ADR 0005 |
| Jednostka | `unit` (`LineUnits`) | `P_8A`: szt., dni, godz., usł.; domyślnie godz. | ADR 0005 |
| Szkic faktury (do wystawienia) | `SalesInvoiceDraft` / `DraftLine` | Dane nowej faktury sprzedażowej przed wygenerowaniem XML | |
| Generator XML | `Fa3XmlWriter` | Tworzy XML FA(3) ze szkicu | |
| Termin płatności | `paymentDueDate` | `Platnosc/TerminPlatnosci/Termin` — zawsze 21 dni od daty wystawienia | |
| Synchronizacja | `InvoiceSynchronizer` / `InvoiceSyncService` | Pobranie nowych faktur z KSeF do lokalnego lustra | ADR 0003 |
| Lustro faktur (lokalna kopia) | `InvoiceMirror` / `MirrorEntry` | Faktury z KSeF zapisane w plikach na telefonie | ADR 0003 |
| Punkt kontrolny | `checkpoint` | HWM, do którego lustro jest kompletne | ADR 0003 |
| Kategoria faktur | `InvoiceCategory` (`SALES`, `PURCHASE`) | Sprzedażowe / zakupowe | |
| Faktura sprzedażowa | `salesInvoice` | Faktura, na której jestem sprzedawcą (Podmiot1, `Subject1`) | |
| Faktura zakupowa | `purchaseInvoice` | Faktura, na której jestem nabywcą (Podmiot2, `Subject2`) | Decyzja E1: „zakupowa”, jak w KSeF |
| Podmiot1 / sprzedawca | `seller` | Wystawca faktury | |
| Podmiot2 / nabywca | `buyer` | Odbiorca faktury | |
| Kontrahent | `counterparty` | Druga strona faktury (nabywca na sprzedażowej, sprzedawca na zakupowej) | |
| Token KSeF | `ksefToken` | Sekret do uwierzytelnienia w API KSeF | |
| Token dostępowy / odświeżający | `accessToken` / `refreshToken` (`AuthToken`) | JWT z `/auth/token/redeem`; dostępowy ~15 min, odświeżający do 7 dni | |
| Uwierzytelnianie | `KsefAuthenticator` | Logowanie tokenem KSeF i odświeżanie tokena dostępowego | |
| Menedżer tokenów | `KsefTokenManager` | Zwraca ważny token dostępowy, sam odświeża / loguje ponownie | |
| Sesja interaktywna | `KsefInvoiceSender` | Sesja KSeF do wysyłki pojedynczych faktur | |
| Faktura w przetwarzaniu | `SubmittedInvoice` | Numery referencyjne sesji i faktury — do wznowienia sprawdzania statusu | |
| Faktura przyjęta | `SentInvoice` | Numer KSeF, data przyjęcia, UPO | |
| UPO | `upo` | Urzędowe Poświadczenie Odbioru | |
| FA(3) | `fa3` | Obowiązujący schemat XML faktury ustrukturyzowanej | |
| Metadane faktury | `InvoiceMetadata` | Dane faktury z listy KSeF (bez pełnej treści XML) | |
| Eksport paczki | `exportInvoices` / `InvoiceExport` | Asynchroniczne pobranie wielu faktur (XML + `_metadata.json`) w zaszyfrowanej paczce ZIP | |
| Rola podmiotu | `InvoiceSubjectType` (`SELLER` = Podmiot1, `BUYER` = Podmiot2) | Sprzedażowe = `SELLER`, zakupowe = `BUYER` | |
| Szkic faktury | `InvoiceForm` (`issue/draft.json`) | Automatycznie zapisywany formularz nowej faktury | |
| Faktura niewysłana | `UnsentInvoice` | Faktura sprzedażowa zapisana na telefonie bez wysyłki do KSeF, edytowalna (US-007) | Oznaczenie: żółty trójkąt z wykrzyknikiem |
