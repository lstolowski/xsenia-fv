# ZAD-040. Miesięczne przypomnienie o braku faktury sprzedażowej

- User story: US-017
- Status: Zrobione (2026-11-02)
- Szacunek: M

## Cel

Lokalne powiadomienie w ostatnim dniu miesiąca o 19:00, gdy miesiąc nie ma faktury sprzedażowej; kliknięcie otwiera listę sprzedażową na tym miesiącu.

## Zakres

- [x] `reminder/ReminderSchedule` — czysta logika: wyzwalacz, następny miesiąc, odrzucanie spóźnionych, warunek „brak faktury”
- [x] `reminder/MonthlyReminder` — kanał powiadomień, `schedule()` (unikalne zadanie WorkManager na miesiąc, `KEEP`), `Worker` (ustawienie, zgoda, raz w miesiącu, lokalna kopia, powiadomienie, kolejne zadanie)
- [x] `XseniaApplication` tworzy kanał i planuje zadanie przy każdym starcie
- [x] `POST_NOTIFICATIONS` w manifeście; prośba o zgodę przy pierwszym starcie (`MainScaffold`) i przy włączeniu przełącznika
- [x] `AppSettings`: przełącznik, ostatni miesiąc przypomnienia, flaga „zapytano o zgodę”; `SettingsScreen` → ramka „Przypomnienie”
- [x] Kliknięcie: `MainActivity` (`onNewIntent`) → `AppContainer.requestedSalesMonth` → `MainScaffold` przechodzi na zakładkę sprzedaży, a lista wybiera miesiąc

## Definicja ukończenia

- [x] Testy jednostkowe (`ReminderScheduleTest`); `./gradlew :app:testKsefTestDebugUnitTest :app:assembleKsefTestDebug`
- [ ] Sprawdzone na emulatorze (zgoda, przełącznik, powiadomienie, kliknięcie z miesiącem poprzednim)
- [x] Zaktualizowana dokumentacja
