# do_zrobienia.md — Strategia rozwoju wtyczki CPLEX OPL Support

Celem projektu jest stworzenie lekkiego, ale funkcjonalnego środowiska do pracy z językiem OPL w IDE JetBrains, eliminując konieczność używania CPLEX Studio.

## Legenda statusów
- `DONE` — funkcja zaimplementowana i dostępna
- `PARTIAL` — funkcja wdrożona częściowo lub z ograniczeniami
- `TODO` — funkcja planowana

---

## ✅ Już zaimplementowane

- `DONE` **Szablony plików:** Akcja "New -> OPL File" w menu kontekstowym
- `DONE` Rejestracja języka OPL (pliki `.mod` i `.dat`)
- `DONE` Syntax Highlighting (kolorowanie składni)
- `DONE` Gramatyka BNF + Lexer JFlex
- `DONE` Podstawowy Annotator (walidacja w locie)
- `DONE` Code Completion (autouzupełnianie słów kluczowych)
- `DONE` Live Templates (skróty: `model`, `rng`, `dv`, `st`, `fa`, `sm`, `tup`, `exec`)
- `DONE` Run Configuration (integracja z `oplrun`)
- `DONE` Ikony SVG dla plików `.mod` i `.dat`
- `DONE` Obsługa skrótu `Ctrl+/` (komentowanie)
- `DONE` Podświetlanie par nawiasów `{}`, `()`, `[]`
- `DONE` **File Type Support:** automatyczne łączenie par plików `.mod` i `.dat` przy uruchamianiu
- `DONE` **Integracja CPLEX (0.2):** Auto-wykrywanie ścieżek, zmienna `CPLEX_STUDIO_DIR`, panel ustawień UI.
- `DONE` **Contextual Autocomplete:** Semantyczne podpowiadanie zmiennych z drzewa PSI.
- `DONE` **Zaawansowany Annotator (2.1):** Wykrywanie duplikatów i brakujących średników.
- `DONE` **Structure View (3.1):** Interaktywne drzewo nawigacji (zmienne, cele, sekcje).
- `DONE` **Formatter:** podstawowe formatowanie kodu OPL

---

## Poziom 0: Execution Engine (Krytyczne — MVP)

Warstwa odpowiedzialna za uruchamianie modeli i integrację z solverem CPLEX.
Bez tego plugin nie spełnia swojego głównego celu.

### 0.1 Run Configuration
- `DONE` Uruchamianie modeli OPL z poziomu IDE: `oplrun model.mod data.dat`
- `DONE` Obsługa wyboru plików `.mod` i `.dat`
- `DONE` Integracja z konsolą IDE (stdout / stderr)
- `DONE` **Asynchroniczność (UI Freezes):** Odklejenie procesu rozwiązywania od wątku EDT za pomocą `ProcessHandler` lub `Task.Backgroundable`, aby IDE nie zawieszało się przy trudnych modelach.

### 0.2 Integracja środowiska CPLEX
- `DONE` Automatyczne wykrywanie instalacji (heurystyka ścieżek systemowych)
- `DONE` Obsługa zmiennej środowiskowej `CPLEX_STUDIO_DIR`
- `DONE` Ręczna konfiguracja ścieżki do `oplrun` (w Run Configuration)
- `DONE` Panel ustawień: `Settings -> Tools -> CPLEX OPL`

### 0.3 Obsługa wyników
- `DONE` Wyświetlanie wyników solvera w konsoli
- `DONE` Obsługa błędów wykonania

---

## Poziom 1: Fundamenty i Estetyka (Bardzo łatwe)

### 1.1 Ikony i Branding
- `DONE` Ikona pluginu gotowa na JetBrains Marketplace
- `DONE` Weryfikacja i użycie ikon SVG dla plików `.mod` i `.dat`

### 1.2 Formatowanie (Code Style)
- `DONE` Inteligentne wcięcia (Indentation) w formaterze, aby wyeliminować błędne tabulatory w nowych liniach.
- `DONE` **Zaawansowany Formatter:** Układanie klamer i wcięć dla zagnieżdżonych bloków ograniczeń (`constraints`) i skryptów (`execute`) za pomocą `FormattingModelBuilder`.
---

## Poziom 2: Automatyzacja i Walidacja (Średnio łatwe)

### 2.1 Poprawa Annotatora
- `DONE` Usunięcie błędnej reguły wielkich liter
- `DONE` Walidacja duplikatów nazw zmiennych
- `DONE` Wykrywanie brakujących średników
- `DONE` Wykrywanie niezdefiniowanych zmiennych
- `DONE` **Eliminacja fałszywych błędów (False Positives):** Ciągłe dostrajanie parsera, aby nie podkreślał na czerwono poprawnego kodu.
- **Inżynierski zysk:** Zgodność ze standardem OPL i czystość logiczna modelu

### 2.2 Rozpoznawanie bloków skryptowych (OPLScript)
- `DONE` Obsługa bloków `execute { ... }` używanych do implementacji metaheurystyk (SA, Tabu Search).
- **Mechanizm:** Złagodzenie reguł w pliku `OplGrammar.bnf` poprzez akceptowanie dowolnej sekwencji znanych tokenów oraz dodanie operatorów skryptowych (`&&`, `||`, `!`, `%`). Takie podejście zapobiega błędom analizatora, zachowując jednocześnie pełne możliwości inteligentnego formatowania kodu.
---

## Poziom 3: Inteligencja Edytora (Wymagające)

### 3.1 Structure View — Widok struktury
- `DONE` Wizualna hierarchia modelu w bocznym panelu IDE
- `DONE` Pokazuje zmienne decyzyjne, ograniczenia, funkcję celu
- `DONE` **Implementacja:** mapowanie drzewa PSI na komponenty UI
- **Inżynierski zysk:** Szybka nawigacja po dużych modelach

### 3.2 Rozszerzony Annotator
- `DONE` Walidacja typów (np. `dvar boolean` nie może mieć zakresu `0..100`)
- `DONE` Scope-aware analysis (zasięg zmiennych wewnątrz `forall`)

### 3.3 Obsługa Programowania w Ograniczeniach (CP) i Szeregowania
- `DONE` Obsługa słów kluczowych szeregowania (`interval`, `sequence`, `pulse`, `step`).
- `DONE` Obsługa dyrektywy silnika (`using cp;`) oraz globalnych ograniczeń (np. `allDifferent`, `pack`).
- **Mechanizm:** Definicje tokenów i funkcji w `OplGrammar.bnf` oraz integracja w obrębie reguły `factor`. `CompletionContributor` automatycznie wspiera autouzupełnianie po wygenerowaniu parsera/lexera.
---

## Poziom 4: Zaawansowana Analiza Systemowa (Trudne)

### 4.1 Inspekcja nieliniowości: `min`, `max`, `abs`
- `DONE` Analiza wpływu na złożoność obliczeniową (MIP)
- `DONE` **Logika:** weryfikacja typu parametrów (`dvar` vs `float/int`)
- `DONE` Ostrzeganie o niejawnej linearyzacji przez solver

### 4.2 Walidacja funkcji celu dla szeregowania
- `DONE` Inspekcja poprawnego wyznaczania długości uszeregowania $C_{max}$.
- **Logika:** Weryfikacja węzłów drzewa PSI wewnątrz bloku `minimize`. Ostrzeganie, jeśli przy zmiennych przedziałowych (`interval`) brakuje niezbędnych funkcji operujących na czasie zakończenia (np. `endOf()`).

### 4.3 Find Usages
- `DONE` Wyszukiwanie wszystkich użyć zmiennej w projekcie
- `DONE` Integracja z oknem "Find" IntelliJ

---

## Poziom 5: Architektura Referencji (Bardzo trudne)

### 5.1 Go to Declaration — Skakanie do definicji
- `PARTIAL` Globalna nawigacja po identyfikatorach (`Ctrl+Click`)
- `DONE` **Mechanizm:** `PsiReference`
- `TODO` **Wyzwanie:** Poprawne budowanie drzewa AST i rozwiązywanie referencji ściśle w obrębie zadeklarowanych zasięgów (Scope), aby zmienne lokalne nie nadpisywały globalnych.

### 5.2 Rename Refactoring
- `DONE` Bezpieczna zmiana nazw identyfikatorów we wszystkich plikach projektu (`Shift+F6`) przy użyciu interfejsu `PsiNamedElement`.

---

## Poziom 6: Produkcja i Dystrybucja

### 6.1 Publikacja na JetBrains Marketplace
- `PARTIAL` Opis pluginu po angielsku
- `TODO` Screenshoty pokazujące działanie
- `PARTIAL` Uzupełniony CHANGELOG.md
- `DONE` README z instrukcją instalacji i wymaganiami
- `TODO` Wygenerowanie i dodanie certyfikatów oraz klucza prywatnego do podpisywania wtyczki (`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`) w GitHub Secrets


### 6.2 Stabilność i kompatybilność
- `DONE` Testy jednostkowe (IntelliJ Platform Test Framework)
- `PARTIAL` Obsługa błędów runtime

### 6.3 Ekspansja poza środowiska JetBrains (Wizja długoterminowa)
- `TODO` **Language Server Protocol (LSP):** Stworzenie niezależnego serwera językowego opartego na obecnej logice lexera i parsera. Umożliwi to udostępnienie funkcji OPL (autouzupełnianie, błędy) w VS Code, Neovim, Sublime Text i innych edytorach wspierających LSP.
- `TODO` Wydanie prostego rozszerzenia dla VS Code komunikującego się z serwerem LSP.
- `DONE` Kompatybilność z PyCharm, IntelliJ IDEA, CLion (odblokowane przez `com.intellij.modules.lang` oraz obniżenie bazowej wersji do 2024.3)
- `DONE` **Przegląd API (Deprecations):** Aktualizacja przestarzałych metod w kodzie (np. wymuszenie `getActionUpdateThread()` w klasach `AnAction`), aby uniknąć problemów w przyszłych wersjach IDE.
---

##  Priorytety — największa wartość dla użytkownika

1. **Run Configuration** (`oplrun`) — bez tego wtyczka jest tylko edytorem tekstu
2. **Integracja z CPLEX** (ścieżki, auto-wykrywanie)
3. **Mapowanie błędów solvera** — klikalne błędy w edytorze
4. **Autocomplete kontekstowy** — przyspiesza pisanie modeli
5. **Structure View** — nawigacja po dużych modelach

---

## Zasady projektowe

- **User-first:** minimalizowanie potrzeby używania CPLEX Studio
- **Zero-Config:** automatyczne wykrywanie środowiska gdy możliwe
- **Pragmatyzm:** lepsze proste działające rozwiązanie niż idealne nieukończone
- **Iteracyjność:** rozwój od MVP do zaawansowanych funkcji

---

## Słowniczek techniczny

- **Boilerplate code:** Fragmenty kodu powielane w wielu miejscach przy niewielkich zmianach
- **Heurystyka ścieżek:** Algorytm szukający danych w najbardziej prawdopodobnych lokalizacjach zamiast skanowania całego systemu
- **Zero-Config:** Cel projektowy, w którym oprogramowanie nie wymaga konfiguracji początkowej
- **MIP:** Mixed Integer Programming — mieszane programowanie całkowitoliczbowe. Klasa złożonych problemów z nieliniowością
- **CP (Constraint Programming):** Programowanie w ograniczeniach. Paradygmat (i odrębny silnik w CPLEX) stosowany do problemów dyskretnych i szeregowania zadań
- **Global Constraints:** Wbudowane w silnik CP wydajne algorytmy (np. `allDifferent`), modelujące złożone zależności kombinatoryczne
- **Cmax (Makespan):** Długość uszeregowania, czas zakończenia wszystkich procesów
- **PSI:** Program Structure Interface — reprezentacja kodu jako drzewo w pamięci IntelliJ
- **AST:** Abstract Syntax Tree — abstrakcyjne drzewo składniowe, wewnętrzna struktura kodu po sparsowaniu
- **EDT:** Event Dispatch Thread — główny wątek interfejsu graficznego IDE. Wykonywanie w nim ciężkich operacji powoduje zawieszenie edytora.
- **FQCN:** Fully Qualified Class Name — pełna ścieżka klasy z pakietem (np. `com.github.cplexopl.OplLanguage`)
- **Scope:** Zasięg zmiennej — obszar kodu w którym zmienna jest widoczna i dostępna