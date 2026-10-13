# Architecture Decision Records

Każda istotna decyzja architektoniczna = osobny plik `NNNN-krotki-tytul.md` wg [szablonu](0000-szablon.md).
Statusy: `Proponowany` → `Zaakceptowany` | `Odrzucony`; później ewentualnie `Zastąpiony przez NNNN`.

| Nr | Tytuł | Status |
|---|---|---|
| [0001](0001-backend-posredniczacy-ksef.md) | Backend pośredniczący w komunikacji z KSeF | Odrzucony |
| [0002](0002-natywna-aplikacja-android.md) | Natywna aplikacja Android (Kotlin + Compose) bez backendu | Zaakceptowany |
| [0003](0003-lokalne-lustro-faktur.md) | Lokalne lustro faktur w plikach i synchronizacja z KSeF | Zaakceptowany |
| [0004](0004-kontrahenci-w-pliku-json.md) | Dane firmy, rachunki i kontrahenci w plikach JSON | Zastąpiony przez 0006 (nieaktualny) |
| [0005](0005-stawki-vat-i-jednostki.md) | Stawki VAT i jednostki w pozycjach faktury; układ formularza | Zaakceptowany (wzmianki o plikach JSON nieaktualne) |
| [0006](0006-dane-firmy-w-aplikacji.md) | Dane mojej firmy, rachunki i kontrahenci w aplikacji (profil firmy) | Zaakceptowany |
| [0007](0007-biala-lista-mf.md) | Pobieranie danych firmy po NIP-ie z Białej listy MF | Zaakceptowany |
| [0008](0008-kursy-walut-nbp.md) | Faktury w walucie obcej i kursy walut z API NBP | Zaakceptowany |
