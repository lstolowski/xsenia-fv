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
| Kwota netto / VAT / brutto | `netAmount` / `vatAmount` / `grossAmount` | Sumy faktury (`P_13_1` / `P_14_1` / `P_15`) | |
| Moja firma | `company` | Dane sprzedawcy z pliku `company.json` | ADR 0004 |
| Rachunek bankowy / konto | `bankAccount` | Pozycja z pliku `banks.json` (`NrRB` w FA(3)) | ADR 0004 |
| Kontrahenci (plik) | `counterparties` | Plik `counterparties.json` z danymi nabywców | ADR 0004 |
| Konfiguracja wystawiania | `InvoicingConfig` | Firma + rachunki + kontrahenci wczytane z plików JSON, z listą problemów (`ConfigIssue`) | ADR 0004 |
| Faktura (treść) | `Invoice` | Faktura odczytana z XML FA(2)/FA(3) przez `InvoiceXmlParser` | |
| Pozycja faktury | `InvoiceLine` | `FaWiersz`: nazwa `P_7`, jednostka `P_8A`, ilość `P_8B`, cena jedn. netto `P_9A`, wartość netto `P_11`, stawka `P_12` | Wartość brutto pozycji wyliczana do wyświetlenia |
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
| Szkic faktury | `invoiceDraft` | Niewysłana faktura zapisana lokalnie | |
