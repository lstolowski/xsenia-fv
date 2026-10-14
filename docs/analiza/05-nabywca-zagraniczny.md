# Nabywca zagraniczny — analiza (US-012)

Status: **zakończona 2026-10-14** — decyzje Q1–Q6 w [US-012](../user-stories/US-012-nabywca-zagraniczny.md), [ADR 0009](../adr/0009-nabywca-zagraniczny.md)

Kontekst: [US-011](../user-stories/US-011-faktury-w-walucie-obcej.md) dodało faktury w EUR/USD/GBP z kursem NBP, a [US-009](../user-stories/US-009-stawki-vat-jednostki.md) stawkę „np” (reverse charge). Nabywca nadal musi mieć polski NIP — decyzja Q1 przy US-011: nabywca zagraniczny to osobna historyjka.

## 1. FA(3) — nabywca (Podmiot2)

Sprawdzone w `schemat_FA(3)_v1-0E.xsd` (kopia: `invoice/src/test/resources/xsd/FA3.xsd`), typ `TPodmiot2`:

| Wariant identyfikatora (`xsd:choice`) | Pola | Kiedy |
|---|---|---|
| NIP | `NIP` | polski nabywca — dziś jedyny wariant |
| VAT UE | `KodUE` (prefiks kraju UE, np. `DE`) + `NrVatUE` (numer bez prefiksu) | nabywca z innego kraju UE z numerem VAT UE (art. 106e ust. 1 pkt 24) |
| Inny identyfikator | `KodKraju` (opcjonalny) + `NrID` | nabywca spoza UE (np. GB, US, CH) z identyfikatorem podatkowym swojego kraju |
| Brak identyfikatora | `BrakID` = 1 | nabywca bez identyfikatora (np. osoba prywatna) — raczej nie nasz przypadek |

- `Nazwa` — jak dziś. `Adres/KodKraju` — kraj nabywcy (dziś zawsze `PL`), `AdresL1`/`AdresL2` — dowolny tekst.
- `JST`, `GV` — jak dziś (`2`).

## 2. Stawka „np” a nabywca zagraniczny

| Kod `P_12` | Pole sumy | Znaczenie (XSD) |
|---|---|---|
| `np II` | `P_13_9` | usługi z art. 100 ust. 1 pkt 4 — świadczone na rzecz podatników z **innych krajów UE** (wykazywane w VAT-UE) |
| `np I` | `P_13_8` | dostawa towarów i usługi **poza terytorium kraju**, z wyłączeniem art. 100 ust. 1 pkt 4 — m.in. usługi dla firm **spoza UE** |

Dziś „np” = zawsze `np II` + `P_18 = 1` + uwaga „Reverse charge” ([ADR 0005](../adr/0005-stawki-vat-i-jednostki.md)). Decyzja Q2 (a): nabywca spoza UE → `np I`, z UE → `np II`; „odwrotne obciążenie” i „Reverse charge” w obu przypadkach (warto potwierdzić z księgowym).

## 3. Dane kontrahenta zagranicznego — skąd

- **Biała lista MF** ([ADR 0007](../adr/0007-biala-lista-mf.md)) zna tylko polskie NIP-y.
- **VIES** (Komisja Europejska), REST bez klucza, sprawdzone 2026-10-14:
  - `GET https://ec.europa.eu/taxation_customs/vies/rest-api/ms/{kodKraju}/vat/{numer}` → `isValid`, `name`, `address` (część krajów, np. DE, nie zwraca nazwy ani adresu — `"---"`),
  - `GET …/rest-api/check-status` — dostępność rejestrów poszczególnych krajów,
  - tylko kraje UE (+ XI — Irlandia Płn.); dla GB, US, CH nie ma odpowiednika.
- Spoza UE — dane wpisywane ręcznie.
- Decyzja Q3 (b): **bez VIES** — także dla UE dane ręcznie, numer sprawdzany tylko formatem.
- Kody krajów: `KodUE` ma Grecję jako `EL` i Irlandię Płn. jako `XI`, a `KodKraju` (adres) — `GR`, `GB`, `XI`; `NrVatUE` — wzorzec `(\d|[A-Z]|\+|\*){1,12}` (bez prefiksu).

## 4. KSeF a nabywca zagraniczny

- Faktura dla nabywcy zagranicznego też jest wystawiana w KSeF (Podmiot2 bez polskiego NIP-u jest dozwolony).
- Nabywca bez polskiego NIP-u **nie odbierze** faktury z KSeF — trzeba mu ją dostarczyć poza KSeF (wizualizacja / PDF z kodem QR KSeF). Aplikacja dziś nie udostępnia faktur ([US-005](../user-stories/US-005-szczegoly-faktury.md): „bez udostępniania”) — decyzja Q5 (a): osobna historyjka US-013 (PDF z kodem QR, udostępnianie).
- Metadane faktur z KSeF mają `buyer.identifier.type` (`Nip`, `VatUe`, `Other`, `None`) — lista faktur pokazuje dziś NIP nabywcy; dla zagranicznego trzeba pokazać numer VAT UE / ID.

## 5. Wpływ na aplikację

| Obszar | Dziś | Zmiana |
|---|---|---|
| `Counterparty` (profil) | `nip` wymagany, klucz niezmienny | kraj + rodzaj identyfikatora (NIP / VAT UE / inny / brak) i numer; klucz kontrahenta niezależny od NIP-u |
| Formularz kontrahenta | NIP + pobieranie z Białej listy | kraj na początku; PL → jak dziś; UE → numer VAT UE (bez VIES, Q3); poza UE → identyfikator ręcznie |
| `InvoiceForm.counterpartyNip` | NIP wybranego kontrahenta | klucz kontrahenta (migracja szkiców i faktur niewysłanych) |
| `Fa3XmlWriter` | `Podmiot2/NIP`, `KodKraju = PL` | wariant identyfikatora i kraj adresu wg kontrahenta |
| Stawka „np” | zawsze `np II` | `np I` / `np II` wg nabywcy (Q2) |
| Listy, szczegóły, wyszukiwanie | NIP | także VAT UE / ID |

## 6. Poza zakresem (propozycja)

- Dostawa towarów (WDT, eksport) — użytkownik świadczy usługi.
- Rozliczenia VAT-UE, JPK — poza aplikacją.
