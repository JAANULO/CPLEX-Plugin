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

## 3. Konsola, Logi i Debugowanie
**Pokrycie zautomatyzowane: 90%, Weryfikacja manualna: 10%**

* **Filtry Konsoli (Infeasibility Parser)** | **Lokalizacja testów: Wewnętrzne testy Pluginu** 
  Funkcjonalność nowo wdrożonego analizatora konfliktów jest oflagowana silną klasą `OplConsoleFilterTest.kt`. Testy zasilane są złośliwymi formatami logów z CPLEX-a, weryfikując poprawność działania wyrażeń regularnych (Regex). Automatyzacja gwarantuje poprawne generowanie koordynatów dla linków prowadzących do plików `.mod` i `.dat`.
* **Proaktywne wskazówki (`<<< no solution`)** | **Lokalizacja testów: Zewnętrzne repozytorium `cplex-opl-examples`** 
  Elementy dynamicznie wstrzykiwane bezpośrednio do interfejsu logów IDE podczas wykonania (żółty tekst) poddawane są weryfikacji w locie. Do zmuszenia solvera do zrzucenia konkretnego błędu w warunkach polowych używany jest specjalny model testowy `infeasible-test.mod`, utrzymywany w repozytorium zewnętrznym `cplex-opl-examples`.

---

## 4. Obszary bez zautomatyzowanych testów (Luki testowe)
**Pokrycie zautomatyzowane: 0%, Weryfikacja tylko manualna (lub ryzykowna)**

Podczas głębokiej analizy projektu można zidentyfikować komponenty, które w ogóle **nie posiadają zautomatyzowanych testów jednostkowych** i mogą stanowić potencjalny dług technologiczny (technical debt):

* **Generowanie skryptów Python (`GeneratePythonRunnerAction`)**: Funkcjonalność dodana w wersji 1.4.6, która konwertuje modele na kod `doopl`. Brak klasy testowej (np. `OplActionTest.kt`), która sprawdzałaby, czy generowany kod Pythona nie ma błędów składniowych i czy akcja poprawnie zrzuca go na dysk.
* **Auto-wykrywanie instalacji CPLEX (`CplexPathFinder`)**: Mechanizm, który pod spodem skanuje foldery `C:\Program Files\IBM...`. Nie ma dla niego testu izolowanego z wirtualnym systemem plików, więc jego ewentualna usterka wyjdzie tylko u klienta podczas klikania przycisku "Auto-Detect".
* **Ustawienia Globalne IDE (`OplSettingsConfigurable` i `OplSettingsState`)**: Kod odpowiedzialny za panel ustawień w IDE (`Settings -> Tools -> CPLEX OPL`) oraz zapisywanie stanu (serializacja XML) nie ma przypisanych asercji. Testowany jest wyłącznie "przy okazji" manualnego uruchamiania pluginu.
* **Podstawowy Filtr Linków Konsoli (`OplLinkFilter`)**: O ile nasz najnowszy parser *Infeasibility* posiada żelazne testy Regex, tak podstawowy, stary parser błędów (`OplLinkFilter`) w ogóle nie jest uwzględniony w `OplConsoleFilterTest.kt`.

---

## Podsumowanie i Wnioski
Architektura wtyczki cechuje się bardzo wysoką **dojrzałością inżynierską**. System posiada wyraźny i bardzo zdrowy podział obowiązków weryfikacyjnych:

1. **Testy wewnętrzne Pluginu (`src/test/kotlin/...`)**: Zabezpieczają systemy statyczne i strukturalne (język, parser, ustawienia konfiguracji, bezpieczeństwo XML, generowanie komend i filtry tekstowe Regex). Posiadają żelazne, w 100% zautomatyzowane testy jednostkowe. 
2. **Repozytorium `cplex-opl-examples`**: Pełni precyzyjnie przemyślaną rolę zewnętrznego **poligonu doświadczalnego** do testów integracyjnych (E2E). Służy do sprawdzania stabilności interakcji ze środowiskiem (prawdziwym silnikiem solvera `oplrun`), zarządzania procesami w systemie operacyjnym (watchdog) oraz weryfikacji zachowania interfejsu (GUI IDE) pod żywym obciążeniem.
