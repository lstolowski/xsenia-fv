# ZAD-018. Wygląd w stylu „Payment UI”

- User story: US-008
- Status: Zrobione
- Szacunek: L

## Cel

Aplikacja w stylu szablonu Payment UI (Compose-Ui-Templates, Apache 2.0), bez ozdobnych kółek.

## Analiza szablonu (payment_ui)

| Element szablonu | Wartość | Zastosowanie w xsenia |
|---|---|---|
| `GreenLight` | `#20B7B4` | akcent: kwoty, przyciski, zaznaczenia, nawigacja |
| `YellowLight` | `#F7D452` | tylko ozdobne kółka — **pomijamy** |
| Tło | białe | tło ekranów |
| Karty | `RoundedCornerShape(8.dp)`, ramka 1 dp jasnoszara, białe | kafelki faktur, sekcje, kafelki kontrahentów |
| Nagłówek | 30 sp SemiBold czarny + kwota 33 sp Bold w akcencie | tytuł ekranu + suma miesiąca |
| `CustomizedButton` | pełna szerokość, akcent, róg 4 dp, padding 16 dp | przyciski główne |
| `TipButton` | pigułka z ramką, akcent gdy zaznaczona | nieużywana — rachunek, jak kontrahent, wybierany w oknie dialogowym z kafelkami |
| `Info` | etykieta szara (50%) + wartość SemiBold + podtytuł szary | szczegóły faktury, wynik wysyłki |
| `SuccessCard` | ikona w kółku nad kartą, przerywana „perforacja”, kwota w akcencie | ekran przyjęcia faktury |

## Zakres

- [x] Motyw: kolory, kształty, typografia (jasny wg szablonu; ciemny — wariant z tym samym akcentem)
- [x] Komponenty wspólne: karta-kafelek, przycisk główny, wiersz informacji, nagłówek ekranu
- [x] Listy faktur: kafelki, nagłówek z sumą miesiąca
- [x] Formularz: kafelek wybranego kontrahenta i rachunku + okno wyboru z kafelkami
- [x] Szczegóły faktury, podgląd, ekran wyniku (karta jak „bilet”)
- [x] Ustawienia, konfiguracja, blokada, nawigacja dolna

## Implementacja

- Motyw: `ui/theme/Theme.kt` — paleta szablonu (akcent `#20B7B4`, białe tło), zaokrąglenia 8 dp (karty, pola) i 4 dp (przyciski), font **Poppins** (`res/font`, licencja SIL OFL 1.1, pliki z repozytorium szablonu). Kolory dynamiczne (Material You) wyłączone — wygląd ma być jak w szablonie.
- Komponenty: `ui/components/Common.kt` — `Tile`, `SectionCard`, `ScreenHeader`, `InfoRow`, `PrimaryButton`, `SecondaryButton`.
- Lista: nagłówek z sumą brutto miesiąca (przewija się razem z listą), kafelek na fakturę, niewysłane z bursztynową ramką; pasek sum na dole usunięty (sumy w nagłówku).
- Formularz: kontrahent i rachunek bankowy — kafelek z wybraną pozycją otwierający okno dialogowe z kafelkami do wyboru (z przyciskiem wyboru; `TileChoiceField`), sumy w kafelku z wierszami „etykieta / wartość”.
- Wynik wysyłki: duże kółko ✓ (w kolorze akcentu) / ✕ (czerwone), karta „bilet” z perforacją i kwotą brutto. Wycięcia perforacji to kształt karty (`TicketShape`: zaokrąglony prostokąt minus dwa półkola na wysokości linii perforacji), więc ramka biegnie wzdłuż nich — rysowane na karcie białe kółka znikały pod ramką i na białym tle.

## Definicja ukończenia

- [x] Testy (logika bez zmian — istniejące testy zielone; przy okazji usunięty wyścig w `UnsentInvoiceRepository`: początkowe wczytanie z dysku mogło nadpisać fakturę zapisaną w międzyczasie)
- [x] Sprawdzone na emulatorze: lista, formularz (kafelki i okna wyboru kontrahenta oraz rachunku), szczegóły, ustawienia, blokada; ekran przyjęcia faktury („bilet”) po wysyłce do KSeF TEST (2026-10-12). Wycięcia perforacji (`TicketShape`) poprawione po tej wysyłce — do potwierdzenia przez użytkownika przy najbliższej wysyłce
- [x] Zaktualizowana dokumentacja
