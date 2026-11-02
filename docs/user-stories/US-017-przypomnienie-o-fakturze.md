# US-017. Przypomnienie o braku faktury w miesiącu

- Priorytet: Etap 2
- Status: Zrobiona

## Historyjka

Jako **użytkownik** chcę **dostać powiadomienie w ostatnim dniu miesiąca, jeśli nie wystawiłem w nim żadnej faktury sprzedażowej**, aby **nie zapomnieć o fakturowaniu**.

## Kryteria akceptacji

- [x] Powiadomienie (standardowy szablon, ikona aplikacji) o treści „Nie wystawiłeś żadnej faktury w tym miesiącu” pojawia się w ostatnim dniu kalendarzowym miesiąca około 19:00 czasu lokalnego, najwyżej raz w miesiącu
- [x] Nie pojawia się, gdy miesiąc ma co najmniej jedną fakturę sprzedażową w KSeF (miesiąc wg daty dostawy, jak na liście)
- [x] Bez serwera powiadomień (Firebase itp.) — zadanie lokalne WorkManager, przybliżona godzina (Doze może opóźnić)
- [x] Aplikacja prosi o zgodę na powiadomienia przy pierwszym uruchomieniu; bez zgody powiadomienia się nie wyświetlają
- [x] Ustawienia → „Przypomnienie”: przełącznik (domyślnie włączony); brak zgody systemowej jest tam zasygnalizowany z przyciskiem do ustawień telefonu
- [x] Kliknięcie otwiera listę faktur sprzedażowych z filtrem miesiąca, którego dotyczy powiadomienie — także gdy kliknięto już w kolejnym miesiącu

## Uwagi / pytania

- Decyzja: sprawdzana jest wyłącznie lokalna kopia (bez synchronizacji w tle), więc przy dawno nieotwieranej aplikacji lub po „Wyczyść cache” przypomnienie może być fałszywe. Faktury niewysłane nie liczą się jako wystawione.
- Opóźnione przez system powiadomienie po północy jest pomijane (mówiłoby o „tym miesiącu” w złym miesiącu).

## Zadania

- ZAD-040
