# Zmyślone odpowiedzi API Białej listy MF

Do mocków klienta Białej listy (ZAD-021, [ADR 0007](../adr/0007-biala-lista-mf.md)). Struktura i pisownia (wielkie litery, adres w jednym napisie, brak „ul.” u części podmiotów) **odwzorowują prawdziwe odpowiedzi** `GET https://wl-api.mf.gov.pl/api/search/nip/{nip}?date={yyyy-MM-dd}` sprawdzone 2026-10-13; wszystkie wartości są zmyślone.

Prawdziwe API nie zna testowych NIP-ów (zwraca `subject: null`), a środowisko `wl-test.mf.gov.pl` nie ma danych — dlatego logikę testujemy wyłącznie na tych odpowiedziach.

## 1. Osoba fizyczna (JDG), czynny podatnik, adres zamieszkania, dwa rachunki

`GET /api/search/nip/1996845518?date=2026-10-13` → `200`

```json
{
  "result": {
    "subject": {
      "name": "JAN TESTOWY",
      "nip": "1996845518",
      "statusVat": "Czynny",
      "regon": "000000000",
      "pesel": null,
      "krs": null,
      "residenceAddress": "TESTOWA 1, 00-001 TESTOWO",
      "workingAddress": null,
      "representatives": [],
      "authorizedClerks": [],
      "partners": [],
      "registrationLegalDate": "2017-01-02",
      "registrationDenialBasis": null,
      "registrationDenialDate": null,
      "restorationBasis": null,
      "restorationDate": null,
      "removalBasis": null,
      "removalDate": null,
      "exemptionSmeDate": null,
      "accountNumbers": ["85216097583216850246002152", "55109004081797992973253811"],
      "hasVirtualAccounts": false
    },
    "requestId": "Test1-000aaaa",
    "requestDateTime": "13-10-2026 15:00:00"
  }
}
```

Oczekiwany wynik: nazwa „JAN TESTOWY” (dla JDG to imię i nazwisko, nazwę firmy użytkownik dopisuje ręcznie), adres `line1` = „TESTOWA 1”, `line2` = „00-001 TESTOWO”, dwa rachunki do wyboru, bez ostrzeżenia.

## 2. Spółka, czynny podatnik, adres siedziby, jeden rachunek

`GET /api/search/nip/6691034274?date=2026-10-13` → `200`; pola jak w 1., różnice:

```json
{
  "name": "TESTOWA SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ",
  "nip": "6691034274",
  "statusVat": "Czynny",
  "krs": "0000000000",
  "residenceAddress": null,
  "workingAddress": "UL. TESTOWA 6, 00-002 TESTOWO",
  "accountNumbers": ["22161029092633573189646018"]
}
```

Oczekiwany wynik: adres z `workingAddress` — `line1` = „UL. TESTOWA 6”, `line2` = „00-002 TESTOWO”.

## 3. Obie formy adresu — pierwszeństwo `workingAddress`

`GET /api/search/nip/9579102904?date=2026-10-13` → `200`; różnice:

```json
{
  "name": "TESTOWY DOLOR SPÓŁKA JAWNA",
  "nip": "9579102904",
  "statusVat": "Czynny",
  "residenceAddress": "TESTOWA 99, 00-099 TESTOWO",
  "workingAddress": "TESTOWA 8, 00-004 TESTOWO",
  "accountNumbers": []
}
```

Oczekiwany wynik: adres z `workingAddress` („TESTOWA 8” / „00-004 TESTOWO”), brak rachunków.

## 4. Podatnik zwolniony — ostrzeżenie

`GET /api/search/nip/9945082500?date=2026-10-13` → `200`; różnice:

```json
{
  "name": "LOREM IPSUM TESTOWY",
  "nip": "9945082500",
  "statusVat": "Zwolniony",
  "residenceAddress": null,
  "workingAddress": "DOLOR SIT AMET 12/3, 00-003 TESTOWO",
  "accountNumbers": ["71101075753723914707418989"]
}
```

Oczekiwany wynik: dane wypełnione, ikona ostrzeżenia i „Firma nie jest czynnym podatnikiem”.

## 5. Adres bez przecinka

`GET /api/search/nip/6973432790?date=2026-10-13` → `200`; różnice: `"statusVat": "Czynny"`, `"workingAddress": "TESTOWO 15"`, `"residenceAddress": null`.

Oczekiwany wynik: `line1` = „TESTOWO 15”, `line2` puste.

## 6. Nie znaleziono

`GET /api/search/nip/4824062973?date=2026-10-13` → `200`

```json
{"result": {"subject": null, "requestId": "Test6-000aaaa", "requestDateTime": "13-10-2026 15:00:00"}}
```

Oczekiwany wynik: „Nie znaleziono firmy w Białej liście”, pola formularza bez zmian.

## 7. Błędy API

| Żądanie | Odpowiedź | Oczekiwany wynik |
|---|---|---|
| `/api/search/nip/12345?date=2026-10-13` | `400` `{"code":"WL-113","message":"Pole 'NIP' ma nieprawidłową długość. Wymagane 10 znaków."}` | aplikacja nie wysyła takiego żądania (walidacja NIP przed pobraniem); klient zwraca błąd z kodem `WL-113` |
| `/api/search/nip/6691034274?date=2030-10-13` | `400` `{"code":"WL-103","message":"Data nie może być datą przyszłą."}` | błąd z kodem `WL-103` (nie powinien wystąpić — data „dziś” w strefie Europe/Warsaw) |
| dowolne | `500`, treść nie-JSON | błąd serwera: „Biała lista nie odpowiada, spróbuj później” |
| dowolne | brak sieci / przekroczenie czasu | „Brak połączenia z Białą listą” |
| dowolne | `429` lub kod błędu limitu | „Wyczerpany dzienny limit zapytań do Białej listy” (limit 100 zapytań dziennie) |
