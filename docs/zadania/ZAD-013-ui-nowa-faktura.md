# ZAD-013. UI: formularz nowej faktury, podgląd, biometria, śledzenie wysyłki

- User story: US-006
- Status: Zrobione (2026-10-12)
- Szacunek: L

## Cel

Kompletny przepływ wystawienia faktury w aplikacji.

## Zakres

- [x] Formularz (`NewInvoiceScreen`, przycisk „Nowa faktura” na liście sprzedażowej): kontrahent i rachunek z list (okna wyboru), daty z kalendarza, numer sugerowany i edytowalny („Użyj sugerowanego”), pozycje 1..n (nazwa, ilość, jednostka, cena jedn. netto — przecinek i spacje dozwolone) z netto/brutto pozycji, termin płatności i suma; błędy przy polach po pierwszym „Podgląd”, ostrzeżenie o miesiącu w numerze
- [x] Szkic zapisywany na bieżąco (`issue/draft.json`); przy wczytaniu daty i numer niezmieniane ręcznie są przeliczane na dziś (`InvoiceFormDefaults.refresh`)
- [x] Podgląd = wygenerowany XML FA(3) odczytany parserem i pokazany komponentem z ZAD-010 (dokładnie to, co pójdzie do KSeF) + informacja o środowisku (TEST / PROD); „Wyślij do KSeF” z potwierdzeniem biometrią („Potwierdź wysyłkę faktury”, numer i kwota)
- [x] `InvoiceSendService` (zakres aplikacji): ekran statusu (wysyłanie / przetwarzanie / przyjęta / odrzucona / błąd); `issue/pending-send.json` od przyjęcia do przetwarzania — po ponownym otwarciu aplikacji status jest dokańczany, ponowienie po błędzie nie wysyła faktury drugi raz
- [x] Duży znak wyniku: zielony okrąg z ✓ (przyjęta), czerwony okrąg z ✕ (odrzucona / błąd) — prośba użytkownika; sprawdzone na emulatorze: błąd w trybie samolotowym (✕), „Spróbuj ponownie” po przywróceniu sieci → faktura `03/09/2026` przyjęta (✓)
- [x] „Spróbuj ponownie” po błędzie nie wymaga ponownej biometrii — dotyczy tej samej, już potwierdzonej faktury
- [x] Po sukcesie: UPO w `upo/{numerKSeF}.xml`, szkic usunięty, synchronizacja → faktura od razu na liście; kolejny formularz sugeruje następny numer

## Definicja ukończenia

- [x] Testy (`DecimalInputTest` 13, `InvoiceFormCheckTest` 4, `InvoiceFormDefaultsTest` 4)
- [x] Sprawdzone na emulatorze z KSeF TEST: faktura `01/09/2026` (Testowa Sp. z o.o., 123,00 zł) przyjęta, numer KSeF, UPO zapisane, faktura na liście września, następna sugestia `02/09/2026`
- [x] Zaktualizowana dokumentacja

## Uwagi

- Wspólny dostęp do KSeF w aplikacji (`KsefAccess`: jeden menedżer tokenów na NIP dla synchronizacji i wysyłki) i wspólny typ błędów `KsefFailure`.
- Emulator: klawiatura Gboard może pokazać samouczek „Wypróbuj rysik”, który przechwytuje tekst wpisywany przez `adb` — zamknąć „Anuluj”.
