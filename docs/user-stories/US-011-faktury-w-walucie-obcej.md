# US-011. Faktury w walucie obcej (EUR, USD, GBP) z kursem NBP

- Priorytet: MVP
- Status: W realizacji (ZAD-026…028 zrobione)
- Zgłoszone: 2026-10-13

## Historyjka

Jako **właściciel firmy wystawiający faktury za granicę** chcę **wystawić fakturę w EUR, USD lub GBP z automatycznie pobranym kursem NBP i VAT-em przeliczonym na PLN**, aby **nie szukać kursu ręcznie, nie liczyć VAT-u w złotych i wysyłać do KSeF faktury zgodne z ustawą o VAT**.

Uzupełnia stawkę „np” (reverse charge, [US-009](US-009-stawki-vat-jednostki.md)). Analiza: [04-faktury-w-walucie-obcej.md](../analiza/04-faktury-w-walucie-obcej.md), decyzja: [ADR 0008](../adr/0008-kursy-walut-nbp.md).

## Kryteria akceptacji

### Serwis kursów walut

- [x] Jeden serwis w kodzie zwraca kurs średni NBP z tabeli A (`mid`) waluty EUR, USD lub GBP w PLN z danego dnia — `https://api.nbp.pl/api/exchangerates/rates/a/{code}/{date}/?format=json`, np. „EUR z 2026-10-12” → `4.3889` — oraz kurs z ostatniego dnia roboczego przed danym dniem. Kurs z 4 miejscami po przecinku, z numerem i datą tabeli.
- [x] Dzień bez tabeli (weekend, święto) — „kurs z dnia” zwraca brak tabeli, „kurs przed dniem” pomija takie dni.
- [x] Brak sieci, błąd serwera — czytelne komunikaty po polsku; kursy z przeszłości nie są pobierane drugi raz.

### Formularz „Nowa faktura”

- [x] Lista „Waluta”: PLN (domyślnie), EUR, USD, GBP — w każdej pozycji obok ceny jednostkowej netto (cena 3/5, waluta 2/5); waluta jest wspólna dla całej faktury. Stare szkice i faktury niewysłane otwierają się w PLN.
- [x] Kwoty pozycji i sumy pokazywane w walucie faktury.
- [x] Rachunek bankowy: do wyboru wszystkie rachunki; rachunek w innej walucie niż faktura — ostrzeżenie „Rachunek jest w PLN, a faktura w EUR” (nie blokuje). Po zmianie waluty, gdy wybrany rachunek nie pasuje, a jest rachunek w nowej walucie — wybierany jest pierwszy pasujący.
- [x] Dla waluty obcej formularz pokazuje kurs roboczy: „Kurs NBP EUR 4,3775 z 2026-10-09 (tabela 197/A/NBP/2026)” i VAT w PLN; kurs wg reguły z sekcji „Reguła kursu”, odświeżany po zmianie waluty lub dat.
- [x] Zapis bez wysyłki (szkic, faktura niewysłana) zapisuje walutę i kurs roboczy; bez sieci zapis działa, a formularz pokazuje „Kurs zostanie pobrany przy podglądzie i wysyłce.”

### Reguła kursu

- [x] Dzień odniesienia = wcześniejsza z dat: data dostawy, dzień wystawienia. Kurs = tabela A NBP z ostatniego dnia roboczego **przed** dniem odniesienia.
- [x] Przy zapisie bez wysyłki dzień wystawienia = data wystawienia z formularza (kurs roboczy).
- [ ] Przy wysyłce do KSeF dzień wystawienia = dzień wysyłki; kurs ostateczny pobierany ponownie i aktualizowany w locie.

### Podgląd i wysyłka

- [ ] Podgląd przed potwierdzeniem biometrią pobiera kurs ostateczny (dzień wysyłki = dziś) i pokazuje kurs oraz VAT w PLN; gdy różni się od roboczego — „Kurs zaktualizowany: 4,3775 → 4,3889”.
- [ ] Tuż przed utworzeniem XML kurs jest sprawdzany ponownie (zmieni się tylko, gdy między podglądem a wysyłką minęła północ).
- [ ] Nie da się pobrać kursu — wysyłka nie startuje, komunikat „Nie udało się pobrać kursu NBP”, faktura zostaje szkicem lub niewysłaną. Kursu nie wpisuje się ręcznie.

### Faktura w KSeF (FA(3))

- [x] `KodWaluty` = waluta faktury; netto, VAT i brutto w walucie faktury.
- [x] Waluta obca: `KursWaluty` (4 miejsca) w każdej pozycji; `P_14_1W`/`P_14_2W`/`P_14_3W` = VAT stawki × kurs, zaokrąglony do grosza.
- [x] Waluta obca: uwaga `DodatkowyOpis` „Kurs waluty” — „1 EUR = 4,3775 PLN, tabela NBP 197/A/NBP/2026 z 2026-10-09”.
- [x] Faktura w PLN — XML bez zmian.
- [x] Faktura w EUR (23% oraz „np”) jest przyjmowana przez KSeF TEST (dane zmyślone).

### Pozostałe

- [ ] Szczegóły wysłanej faktury w walucie obcej — kurs i VAT w PLN.
- [x] Listy: faktura niewysłana pokazuje kwotę w swojej walucie; sumy miesiąca liczone osobno per waluta (już działa dla faktur z KSeF).
- [x] Formularz rachunku w „Moja firma” ma listę walut PLN, EUR, USD, GBP.
- [x] Do wystawienia faktury wystarczy dowolny rachunek (dotąd wymagany rachunek PLN — US-010).

## Uwagi / pytania

Decyzje z sesji pytań (2026-10-13):

- Q1 (a) — nabywca zagraniczny (bez polskiego NIP-u: numer VAT UE, kraj ≠ PL) to osobna historyjka US-012; US-011 dotyczy tylko waluty.
- Q2 (c) — do wyboru wszystkie rachunki, niezgodność waluty tylko ostrzeżeniem.
- Q3 (a) — bez kursu wysyłka nie startuje; bez ręcznego kursu.
- Q4 (a) — kurs ostateczny w podglądzie przed potwierdzeniem, ponowne sprawdzenie przed XML.
- Q5 (a) — uwaga „Kurs waluty” z numerem i datą tabeli NBP; bez netto/brutto w PLN.
- Q6 (a) — `P_14_xW` = VAT w walucie × kurs.
- Po rundzie 1: kurs z **4 miejscami** po przecinku (jak `mid` NBP), nie 6; wyłącznie NBP, tabela A, endpoint dzienny; waluty obce tylko EUR, USD, GBP.
- 2026-10-14: lista „Waluta” obok ceny jednostkowej netto w pozycji — decyzja użytkownika przy ZAD-028; najpierw 2/3 cena i 1/3 waluta, potem poszerzona do 3/5 i 2/5 (kod waluty był przycięty).
- Założenia potwierdzone: dzień obowiązku podatkowego = data dostawy; `P_1` przy wysyłce bez zmian; numeracja wspólna dla wszystkich walut; termin płatności 21 dni; brak waluty domyślnej per kontrahent.

## Zadania

- ZAD-026 — moduł `exchangerates`: klient NBP, serwis kursów walut, reguła daty kursu
- ZAD-027 — model i XML FA(3): waluta, kurs, VAT w PLN, uwaga o kursie; test wysyłki faktury w EUR do KSeF TEST
- ZAD-028 — UI: lista „Waluta”, rachunek z ostrzeżeniem, kurs roboczy, szkic i faktury niewysłane
- ZAD-029 — podgląd i wysyłka: kurs ostateczny w locie, szczegóły faktury
