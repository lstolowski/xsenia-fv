# Warsztat — pytania i ustalenia

Status: **rundy 1 i 2 zakończone (2026-10-11); otwarte: R13**

## Ustalenia z rundy 1

### Kontekst i dostęp do KSeF

| Nr | Ustalenie |
|---|---|
| A2 | Użytkownik ma certyfikat i token KSeF (PROD). |
| A3 | Wersja `test` → środowisko KSeF TEST (fikcyjny NIP), wersja `prod` → KSeF PROD. |
| A4 | Jeden NIP. |

### Aplikacja Android — [ADR 0002](../adr/0002-natywna-aplikacja-android.md) (zaakceptowany)

| Nr | Ustalenie |
|---|---|
| B1 | Kotlin + Jetpack Compose. |
| B2 | Samsung Galaxy S23, Android 16, One UI 8.5. |
| B3 | Tylko aplikacja na telefon, bez dostępu z komputera — zaakceptowane. |
| B4 | Biometria przy otwarciu aplikacji; wysyłka nowej faktury do KSeF potwierdzana biometrią. |
| B5 | Cache w plikach, bez bazy danych (również na telefonie). |
| B6 | Android Studio, wszystkie testy na emulatorze — [instrukcja instalacji](../srodowisko/android-studio-macos.md). |
| B7 | Osobne wersje aplikacji: testowa i produkcyjna. |
| B8 | Instalacja APK ręcznie. |

### Wystawianie faktury sprzedażowej

| Nr | Ustalenie |
|---|---|
| C1 | Nabywcy wyłącznie polscy (NIP). Kontrahenci w pliku JSON edytowanym ręcznie, wczytywanym przy otwarciu aplikacji — [ADR 0004](../adr/0004-kontrahenci-w-pliku-json.md). |
| C2 | Stawka VAT zawsze 23%. |
| C3 | Waluta PLN (nie wskazano innych). |
| C4 | Bez „wystaw na podstawie poprzedniej”. Nowa faktura + wybór kontrahenta z listy. |
| C5 | Przy wystawianiu wpisuję cenę **netto**. Na listach i w szczegółach (sprzedażowe i zakupowe) pokazujemy **netto i brutto**. |
| C6 | Numer `NN/MM/RRRR`: NN — kolejny numer w miesiącu, MM — miesiąc, RRRR — rok (np. `01/10/2026`). Numer kolejny wyliczany automatycznie z faktur w KSeF. |
| C10 | Dane nabywcy z pliku kontrahentów (zamiast Białej listy VAT). |
| C13 | Bez PDF. XML + UPO oraz czytelny, wygodny widok szczegółów faktury. |

### Przeglądanie faktur

| Nr | Ustalenie |
|---|---|
| D2 | Filtr miesiąca po **dacie dostawy** (może być wcześniejsza niż data wystawienia, np. faktura wystawiona 1. dnia miesiąca za poprzedni miesiąc). Miesiąc i rok w numerze faktury (C6) wynikają z **daty dostawy**. Numer jest **sugerowany**, ale edytowalny (pole tekstowe). Konsekwencje techniczne: [ADR 0003](../adr/0003-lokalne-lustro-faktur.md). |

### Terminologia

| Nr | Ustalenie |
|---|---|
| E1 | „Faktury sprzedażowe” i „Faktury zakupowe” — jak w KSeF. |

---

## Ustalenia z rundy 2

| Nr | Ustalenie |
|---|---|
| R1 | Filtr po dacie dostawy dotyczy **obu** list (sprzedażowej i zakupowej). |
| R2 | Skala: zwykle 7–10 faktur zakupowych i 1 sprzedażowa miesięcznie (może wzrosnąć). Historia **od 01.04.2026**. |
| R3 | Jedna data dostawy (`P_6`), bez okresów od–do. Domyślnie: ostatni dzień poprzedniego miesiąca, jeśli dziś jest 1.–15. dzień miesiąca; później dzisiejsza data. Edytowalna. |
| R4 | Pozycja: nazwa, ilość, jednostka, cena jednostkowa netto, wartość netto (ilość × cena), wartość brutto. 1..n pozycji. |
| R5 | Płatność zawsze przelewem, termin zawsze **21 dni**. Rachunki bankowe w pliku JSON; w formularzu lista rozwijana z wyborem konta. |
| R6 | Dane mojej firmy w pliku JSON (format podobny do kontrahentów), wczytywane zawsze przy starcie. |
| R7 | Pliki we wspólnym folderze, np. `Dokumenty/xsenia`. |
| R8 | Nazwy pól w JSON po angielsku. |
| R9 | Numer z zerem wiodącym: `02/03/2025`. |
| R10 | Bez mechanizmu podzielonej płatności i bez uwag/stopki. |
| R11 | Użytkownik ma token KSeF dla środowiska TEST (NIP testowy) i certyfikat testowy — zapisane lokalnie w `secrets/ksef-test.properties` (poza gitem). Uwierzytelnienie tokenem zweryfikowane na TEST 2026-10-11. |
| R12 | Zaakceptowane propozycje domyślne (C12, D1, D3–D7, E2, F1). |
| Zaokrąglenia (2026-10-12) | VAT liczony od sumy netto pozycji (`P_14_1` = 23% × `P_13_1`); brutto pozycji tylko informacyjnie — przy wielu pozycjach suma brutto pozycji może różnić się o 1 gr od kwoty do zapłaty. |
| D5 (zmiana 2026-10-12) | Bez udostępniania XML / UPO z widoku faktury — przynajmniej na razie. |

Format rachunków podany przez użytkownika (dane w przykładzie zastąpione zmyślonymi):

```json
{"banks": [{"name": "Konto testowe Toyota", "bank": "Toyota Bank", "account": "85216097583216850246002152", "currency": "PLN"}]}
```

## Pytania otwarte

- **R13.** (F2) Coś, czego na pewno nie chcesz w aplikacji? — odpowiedź później.
