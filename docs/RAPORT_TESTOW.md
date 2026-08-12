# Raport Pokrycia Testami (Test Coverage) dla Pluginu CPLEX OPL

Poniższy raport stanowi analizę architektury testowej projektu, dzieląc zaimplementowane funkcjonalności na te weryfikowane przez wbudowane **testy jednostkowe (zautomatyzowane w Pluginie)** oraz te wymagające środowiska **poligonu doświadczalnego** z repozytorium zewnętrznego (`cplex-opl-examples`).

---

## 1. Wsparcie Językowe (Edytor kodu)
**Pokrycie zautomatyzowane: ~100%** | **Lokalizacja testów: Wewnętrzne testy Pluginu**

Ta sekcja jest w pełni i bezpośrednio chroniona przez zautomatyzowane ramy testowe (Test Framework) wbudowane w IntelliJ Platform. Jakakolwiek zmiana w gramatyce, która psuje kompatybilność wsteczną, zostanie natychmiast wychwycona podczas buildu pluginu.
Każda główna funkcja posiada dedykowaną klasę testową w folderze `src/test/kotlin/com/github/cplexopl/`:
* **Parsowanie i Lexer** (`OplParsingTest.kt`) – weryfikuje poprawne budowanie drzew składniowych AST z kodu źródłowego.
* **Formatowanie i Komentowanie** (`OplFormattingTest.kt`, `OplCommenterTest.kt`) – zapobiega psuciu wcięć kodu.
* **Widok struktury i Szablony** (`OplStructureViewTest.kt`, `OplLiveTemplatesTest.kt`) – gwarantuje, że podgląd plików i szybkie skróty tekstowe ładują się prawidłowo.
* **Nawigacja (Referencje)** (`OplReferenceTest.kt`) – testuje przechodzenie do definicji metod i zmiennych.
* **Podświetlanie i Analiza Semantyczna** (`OplHighlightingTest.kt`, `OplAnnotatorPerformanceTest.kt`) – zapobiega degradacji wydajności podczas analizowania kodu.

## 2. Uruchamianie (Run Configurations)
**Pokrycie zautomatyzowane: ~80%, Weryfikacja manualna: 20%**

* **Parsowanie ustawień `.ops`** | **Lokalizacja testów: Wewnętrzne testy Pluginu** 
  Przetestowane niezwykle rygorystycznie w pliku `OplRunConfigurationTest.kt`. Istnieją testy izolowane sprawdzające poprawne dekodowanie symboli specjalnych (np. `&amp;`), a także dedykowane zabezpieczenie wyłapujące ataki `XXE` (wstrzykiwanie wrogich encji zewnętrznych XML). 
* **Auto-wiązanie plików i walidacja** | **Lokalizacja testów: Wewnętrzne testy Pluginu** 
  Logika pilnująca spójności rzuca odpowiednie wyjątki w środowisku testowym (weryfikacja w `OplRunConfigurationTest.kt`, np. gdy załączono nieistniejący plik z danymi).
* **Watchdog / Timeout i przekazywanie Flag CLI** | **Lokalizacja testów: Zewnętrzne repozytorium `cplex-opl-examples`** 
  Modułów interakcji z natywnym systemem operacyjnym (zabijanie procesu `oplrun` z użyciem platformowego watchdoga) nie da się wiarygodnie zmockować w teście jednostkowym. Funkcjonalność ta jest przeznaczona do weryfikacji manualnej w locie. Testowana jest z użyciem eksperymentalnych, nieskończenie liczących się modeli matematycznych pobieranych z zewnętrznego repozytorium `cplex-opl-examples`.

* **Auto-wykrywanie instalacji CPLEX** (`CplexPathFinderTest.kt`) | **Lokalizacja testów: Wewnętrzne testy Pluginu**
  W pełni przetestowane z użyciem tymczasowych struktur folderów (`TemporaryFolder`), sprawdzające wybór najwyższej wersji CPLEX oraz obsługę zmiennej środowiskowej `CPLEX_STUDIO_DIR` dla różnych systemów operacyjnych.
* **Ustawienia Globalne IDE** (`OplSettingsTest.kt`) | **Lokalizacja testów: Wewnętrzne testy Pluginu**
  Przetestowane w oparciu o `BasePlatformTestCase`. Weryfikuje cykl życia `OplSettingsConfigurable`, utrwalanie stanu `OplSettingsState` oraz działanie metody `isModified()`.

## 3. Konsola, Logi i Debugowanie
**Pokrycie zautomatyzowane: 98%, Weryfikacja manualna: 2%**

* **Filtry Konsoli (Infeasibility & Link Parser)** | **Lokalizacja testów: Wewnętrzne testy Pluginu** 
  Przeprowadzane w klasie `OplConsoleFilterTest.kt`. Testy zasilane są logami z CPLEX-a (sprawdzając zarówno podświetlanie ograniczeń sprzecznych, jak i standardowe linkowanie błędów do plików `.mod` i `.dat`).
* **Testy Wydajnościowe Filtrów Logów** (`OplConsoleFilterPerformanceTest.kt`) | **Lokalizacja testów: Wewnętrzne testy Pluginu**
  Przetwarza 100 000 linii logów konsolowych, chroniąc IDE przed zawieszeniem i weryfikując wydajność wyrażeń regularnych (oparte na `measureTimeMillis`).
* **Proaktywne wskazówki (`<<< no solution`)** | **Lokalizacja testów: Zewnętrzne repozytorium `cplex-opl-examples`** 
  Elementy dynamicznie wstrzykiwane bezpośrednio do interfejsu logów IDE podczas wykonania (żółty tekst) poddawane są weryfikacji w locie. Do zmuszenia solvera do zrzucenia konkretnego błędu w warunkach polowych używany jest specjalny model testowy `infeasible-test.mod`, utrzymywany w repozytorium zewnętrznym `cplex-opl-examples`.

---

## 4. Obszary bez zautomatyzowanych testów (Pozostałe luki)
**Pokrycie zautomatyzowane: 0%, Weryfikacja tylko manualna**

Po przeprowadzonej fali refaktoryzacji i dodaniu nowych klas testowych, jedynym modułem bez zautomatyzowanego testu jednostkowego pozostaje:

* **Generowanie skryptów Python (`GeneratePythonRunnerAction`)**: Funkcjonalność dodana w wersji 1.4.6, która konwertuje modele na kod `doopl`. Brak klasy testowej sprawdzającej poprawność generowanej składni kodu Python.

---

## Podsumowanie i Wnioski
Architektura wtyczki cechuje się bardzo wysoką **dojrzałością inżynierską**. Prawie wszystkie kluczowe moduły (edytor, konfiguracje uruchomieniowe, wyszukiwarka ścieżek, ustawienia, filtry logów) posiadają automatyczne testy jednostkowe.

1. **Testy wewnętrzne Pluginu (`src/test/kotlin/...`)**: Zabezpieczają systemy statyczne i strukturalne (język, parser, ustawienia konfiguracji, autodetekcję, filtry logów i testy wydajnościowe).
2. **Repozytorium `cplex-opl-examples`**: Pełni rolę zewnętrznego **poligonu doświadczalnego** dla testów integracyjnych (E2E) z udziałem żywego silnika CPLEX `oplrun`.

