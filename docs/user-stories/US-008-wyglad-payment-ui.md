# US-008. Wygląd aplikacji według szablonu „Payment UI”

- Priorytet: MVP (przed ZAD-015)
- Status: Zrobiona (2026-10-12)
- Zgłoszone: 2026-10-12

## Historyjka

Jako **właściciel firmy** chcę, **aby aplikacja wyglądała nowocześnie i czytelnie** (obecnie jest bardzo ascetyczna), **zgodnie z szablonem „Payment UI”** z https://github.com/Hiten24/Compose-Ui-Templates (licencja Apache 2.0).

## Kryteria akceptacji

- [x] Styl szablonu: białe tło, czarne pogrubione nagłówki, akcent morski `#20B7B4` (kwoty, przyciski, zaznaczenia), karty z zaokrągleniem i cienką jasnoszarą ramką, przyciski na całą szerokość w kolorze akcentu.
- [x] **Bez ozdobnych kółek** szablonu (zielone i żółte w lewym górnym rogu) — decyzja użytkownika.
- [x] Lista faktur: **każda faktura w osobnym kafelku** (karta z zaokrągloną ramką).
- [x] Wybór kontrahenta: w formularzu kafelek z wybranym kontrahentem; kliknięcie otwiera okno dialogowe z kontrahentami jako **osobne kafelki** (zaznaczony — ramka w kolorze akcentu). Tak samo wybór rachunku bankowego.
- [x] Nagłówek ekranu w stylu szablonu (duży tytuł + kwota w kolorze akcentu, np. suma brutto miesiąca).
- [x] Szczegóły faktury i ekran wyniku wysyłki w stylu kart szablonu (wiersze „etykieta / wartość”, karta sukcesu jak „bilet”); zachowane duże znaki ✓ / ✕ (US-006).
- [x] Spójny wygląd pozostałych ekranów: ustawienia, konfiguracja, blokada.

## Zadania

- ZAD-018 — motyw i komponenty w stylu Payment UI, przebudowa ekranów
