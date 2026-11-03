# ZAD-039. „Wesprzyj rozwój aplikacji”: przycisk w Ustawieniach i okno po drugiej fakturze

- User story: US-016
- Status: Zrobione (2026-10-28)
- Szacunek: S

## Cel

Okno wsparcia autora z linkiem do https://suppi.pl/wsparcie/lsdevelopment — z Ustawień w każdej chwili oraz jednorazowo po „Gotowe” przy drugiej wystawionej fakturze.

## Zakres

- [x] `SupportDialog` / `SupportButton` (`ui/components/SupportDialog.kt`): wyśrodkowany tytuł „Postaw kawę”, obrazek filiżanki kawy (`drawable-nodpi/coffee_cup.png`, tło przezroczyste), tekst, „Wesprzyj dobrowolną kwotą” (`Intent.ACTION_VIEW`), „Nie teraz”
- [x] `SettingsScreen`: zielony przycisk (`PrimaryButton`) w ramce „Aplikacja”
- [x] `IssuedInvoiceCounter` — licznik przyjętych faktur w pliku `cacheDir/issued-invoices.txt`; brak / nieczytelny plik = start od 1
- [x] `ProfileSession(onInvoiceIssued)` wywoływane z `InvoiceSendService.onAccepted`; `AppContainer` zwiększa licznik (wątek IO, zasięg aplikacji)
- [x] `NewInvoiceScreen`: „Gotowe” (oraz wstecz) na ekranie sukcesu, gdy licznik właśnie osiągnął 2, pokazuje okno; po jego zamknięciu — zamknięcie formularza

## Definicja ukończenia

- [x] Testy jednostkowe (`IssuedInvoiceCounterTest`); `./gradlew build` (testy)
- [x] Sprawdzone na emulatorze: zielony przycisk i okno „Postaw kawę” w Ustawieniach, otwarcie strony w Chrome
- [x] Zaktualizowana dokumentacja

## Uwagi

- Okno po drugiej fakturze sprawdzone testem jednostkowym licznika; na emulatorze nie wysyłano w tym celu dodatkowych faktur do KSeF TEST.
- `./gradlew build` zgłasza w lincie tylko dostępność nowszych wersji Gradle, AGP, datastore-preferences i kotlinx-serialization (niezwiązane z tym zadaniem).
