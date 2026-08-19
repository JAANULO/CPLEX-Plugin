# Dokumentacja Funkcjonalności Pluginu CPLEX OPL dla JetBrains

Niniejszy dokument podsumowuje wszystkie zaimplementowane dotychczas funkcjonalności dla wtyczki wspierającej język CPLEX OPL (Optimization Programming Language) w środowiskach bazujących na IntelliJ Platform (np. IntelliJ IDEA, PyCharm, itp.).

> Zapewniam Cię na wstępie – **wszystkie wprowadzone przez nas zmiany w kodzie są zapisane na dysku**, przetestowane (przechodzą skrypt `gradle check`) i gotowe do ewentualnego wdrożenia (zbudowania ostatecznej paczki `.zip` pluginu).

---

## 1. Wsparcie Językowe (Edytor kodu)
Plugin dodaje pełnoprawną obsługę plików z rozszerzeniami `.mod`, `.dat` oraz `.ops`.

* **Podświetlanie Składni (Syntax Highlighting)**: Słowa kluczowe OPL, typy danych (`int`, `float`, `dvar`), komentarze i ciągi znaków posiadają własne kolory spójne z używanym motywem IDE. Zaimplementowano specjalny Lexer (plik `opl.flex`).
* **Autouzupełnianie (Code Completion)**: Edytor podpowiada kluczowe słowa strukturalne języka podczas pisania (np. `maximize`, `minimize`, `subject to`, typy zmiennych).
* **Formatowanie Kodu (Code Formatter)**: Zaimplementowano automatyczne wcięcia (indentację) kodu zgodnie z regułami języka (np. zawartość w klamrach `{ ... }` jest automatycznie wyrównywana).
* **Nawigacja (References & Go To Definition)**: Możliwość kliknięcia na zmienną z wciśniętym `Ctrl` (lub `Cmd`), aby przeskoczyć do miejsca jej definicji w obrębie pliku.
* **Widok Struktury (Structure View)**: Po lewej stronie w zakładce *Structure* IDE generuje "drzewo" pliku – pokazuje listę zadeklarowanych zmiennych decyzyjnych (`dvar`), celów (np. `maximize`) i sekcji ograniczeń. Ułatwia to nawigację po ogromnych modelach matematycznych.
* **Szablony (Live Templates)**: Wpisanie krótkich skrótów automatycznie rozwija się w większe bloki kodu (np. powtarzalne deklaracje pętli `forall` lub sekcje `execute { ... }`).
* **Komentowanie kodu**: Skrót `Ctrl+/` prawidłowo komentuje i odkomentowuje linie lub bloki za pomocą komentarzy `//` lub `/* */`.

## 2. Uruchamianie (Run Configurations)
Plugin dodaje możliwość uruchamiania modeli OPL prosto z IDE przy pomocy wbudowanego "zielonego trójkąta" (Run). Zbudowaliśmy cały potężny system konfiguracji:

* **OPL Run Configuration**: Nowy typ konfiguracji pozwalający podpiąć plik modelu (`.mod`), plik z danymi (`.dat`) oraz plik z ustawieniami (`.ops`).
* **Automatyczne wiązanie plików**: Gdy wybierzesz plik `model.mod`, plugin automatycznie przeszuka folder i jeśli znajdzie `model.dat` lub `model.ops`, przypisze je od razu do konfiguracji uruchomienia.
* **Auto-wykrywanie `oplrun`**: Przycisk "Auto-Detect" w konfiguracji uruchomienia potrafi automatycznie przeszukać standardowe lokalizacje instalacji IBM CPLEX na dysku i odnaleźć plik wykonywalny `oplrun.exe`. Zapisze to ustawienie globalnie dla całego środowiska.
* **Dynamiczne parametry `.ops`**: Ponieważ CLI środowiska `oplrun` ignoruje pliki `.ops`, zbudowaliśmy parser XML z zabezpieczeniami (ochrona przed atakami XXE). Plugin przed uruchomieniem dekoduje ustawienia z pliku `.ops` (np. limit pamięci) i wstrzykuje je jako wygenerowany w locie blok `execute { ... }` do pliku uruchomieniowego w pamięci tymczasowej (temp).
* **Limit czasu (Watchdog/Timeout)**: Zabezpieczenie chroniące przed zawieszeniem komputera przez nieskończenie długie obliczenia solvera. Użytkownik w ustawieniach konfiguracji może określić np. 60 sekund. Jeśli `oplrun` przekroczy ten czas, plugin wstrzykuje czerwony komunikat błędu do konsoli i bezpiecznie ubija proces solvera.
* **Dodatkowe flagi (Additional CLI Args)**: Możliwość wstrzyknięcia dowolnych, specjalnych parametrów wywołania bezpośrednio do CLI polecenia (np. flagi tuningu).
* **Conflict Refiner**: Checkbox pozwalający na łatwe "włączenie" analizatora konfliktów.

## 3. Konsola, Logi i Debugowanie
Ogromny nacisk położyliśmy na polepszenie tzw. "Developer Experience" przy szukaniu błędów w modelach.

* **Filtry linków (OplLinkFilter)**: Kiedy zwykły błąd wyskakuje na konsoli, podajemy odpowiednie koordynaty błędu, a wideo/konsola generuje niebieski, klikalny link (Hyperlink). Jego kliknięcie ustawia kursor IDE w dokładnie zepsutej linijce w odpowiednim pliku.
* **Inteligentne parsowanie Infeasibility (OplInfeasibilityFilter)**: Gdy model matematyczny jest "sprzeczny" (infeasible), zwykły `oplrun` drukuje długi i skomplikowany blok logów.
  * Nasz filtr potrafi to przechwycić, analizuje przy pomocy wyrażeń regularnych precyzyjne koordynaty (np. `ctInfeasible at 4:17-25 model.mod`) i generuje bezpośrednie linki do ograniczających (sprzecznych) równań.
  * Obsługiwane są zarówno pliki modeli (`.mod`), jak i pliki danych (`.dat`).
* **Pro-aktywne wskazówki (Hints)**: Kiedy plugin zauważy na wyjściu konsoli komunikat `<<< no solution`, ale zobaczy, że nie użyłeś Conflict Refinera, to wstrzyknie żółty komunikat (na STDERR) o treści przypominającej Ci: `[Hint: To diagnose infeasibility, enable 'Run conflict refiner' in Run Configuration settings and label your constraints]`.

## 4. Ustawienia Globalne
* **Global Configuration (Settings -> Tools -> CPLEX OPL)**: Konfiguracja IDE przechowuje zadaną przez Ciebie ścieżkę do instalacji CPLEX-a globalnie. Jeśli tworzysz nowy projekt z modelem lub nową konfigurację Run, system automatycznie zaimportuje to ustawienie. Nie musisz szukać ścieżki za każdym razem.

## 5. Informacje zwrotne i Raportowanie Błędów (Feedback & Error Reporting)
* **Automatyczne Raportowanie Błędów (OplErrorReportSubmitter)**: Integracja z natywnym dialogiem błędów JetBrains IDE. W przypadku wystąpienia nieobsłużonego wyjątku w pluginie, użytkownik otrzymuje przycisk "Report Issue on GitHub", który bezpośrednio otwiera przeglądarkę ze sformatowanym zgłoszeniem (stacktrace, wersja IDE, system operacyjny).
* **System Oceniania Pluginu (OplRateAction & OplRatePrompt)**:
  * Dedykowana opcja w menu: `Help -> Rate CPLEX OPL Plugin...`.
  * Inteligentne powiadomienie (Balloon): Po 5. pomyślnym uruchomieniu modelu wtyczka dyskretnie wyświetla powiadomienie z prośbą o ocenę na JetBrains Marketplace.

## Podsumowanie stanu technicznego
Wszystkie nowo-wdrożone systemy – w tym parsowanie konsoli z filtrem `OplInfeasibilityFilter`, silnik watchdoga, wykrywanie plików `data.dat`, raportowanie błędów `OplErrorReportSubmitter` oraz budowanie dynamicznych komend CLI – posiadają napisane testy jednostkowe (`OplConsoleFilterTest.kt`, `OplRunConfigurationTest.kt`, `OplErrorReportSubmitterTest.kt`). Komenda weryfikująca cały build przeszła pomyślnie.

Pliki są zapisane w kodzie źródłowym, a cały plugin jest połączony i skompilowany.
Możesz być spokojny - praca wykonana podczas naszych ostatnich sesji nie zginęła.
