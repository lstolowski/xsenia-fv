# ZAD-008. Lokalne lustro faktur i synchronizacja

- User story: US-002
- Status: Do zrobienia
- Szacunek: M

## Cel

Implementacja ADR 0003 w aplikacji.

## Zakres

- [ ] Pliki: `invoices/{sales|purchase}/{ksefNumber}.xml`, `index.json`, `sync-state.json`
- [ ] Pierwsza synchronizacja od 2026-04-01 (eksport), kolejne przyrostowo po `PermanentStorage` (HWM)
- [ ] Synchronizacja przy starcie (jeśli minęło > N min) i ręczna; postęp i stan w UI
- [ ] Atomowy zapis plików (zapis do pliku tymczasowego + rename)

## Definicja ukończenia

- [ ] Testy
- [ ] Sprawdzone na środowisku KSeF TEST / emulatorze (jeśli dotyczy)
- [ ] Zaktualizowana dokumentacja / słownik
