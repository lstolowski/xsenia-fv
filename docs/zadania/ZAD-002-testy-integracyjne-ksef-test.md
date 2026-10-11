# ZAD-002. Infrastruktura testów integracyjnych z KSeF TEST

- User story: US-002, US-006
- Status: Do zrobienia
- Szacunek: S

## Cel

Testy w modułach JVM mogą łączyć się z KSeF TEST danymi z `secrets/ksef-test.properties`.

## Zakres

- [ ] Odczyt `secrets/ksef-test.properties` w testach (pominięcie testów, gdy pliku brak)
- [ ] Osobne zadanie Gradle `integrationTest` (nieuruchamiane przy zwykłym `test`)
- [ ] Ochrona przed przypadkowym użyciem adresu PROD w testach

## Uwagi

- Testy integracyjne uruchamiać poza oknem prac serwisowych TEST (16:00–18:00)

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
