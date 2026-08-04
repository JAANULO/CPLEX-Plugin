<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# CPLEX-Plugin Changelog

## [Unreleased]

## [1.4.9] - 2026-08-04

### Added

- **Language Parser:** Added full support in the lexer and grammar for the `key` modifier in tuples and the logical implication operator `=>`.

## [1.4.8] - 2026-07-31

### Added

- **Language Parser:** Added full support for Constraint Programming (`using CP;`, `dexpr`, `size`, `in all`).
- **Language Parser:** Added support for signed numeric types (`float+`, `int+`), chained inequalities (`1 <= u[i] <= n`), and multi-iterator loops (`i, j in Cities`).
- **Annotator:** Added CP built-in functions (`endOf`, `noOverlap`, etc.) to static analyzer built-ins and fixed `dvar` declaration identifier resolution.

## [1.4.7] - 2026-07-25

### Added

- **Language Parser:** Added `main` keyword and `main { ... }` block syntax parsing support.

### Changed

- **Language Parser:** Relaxed tuple declaration requirements (semicolon is now optional).
- **Testing:** Consolidated `testData/` directories. Tiny test files for code completion, formatting, commenter, live templates, references, and structure view were inlined directly into Kotlin test classes, reducing repository clutter for cross-device development.
- **Testing:** Consolidated all highlighting tests into a single `highlighting_tests.mod` file.
- **Run Configuration:** Changed temp model file creation logic to use the system temp directory (`%TMP%` / `java.io.tmpdir`) instead of creating temp files directly inside the project directory.

### Fixed

- **Annotator:** Fixed false positive "Missing semicolon" and "Undefined variable" errors for tuple fields and property access (e.g. `i.weight`).
- **Annotator:** Fixed false positive "Variable is already defined" errors caused by improper resolution of nested type names.
- **Testing:** Fixed LiveTemplates formatting flattening in automated tests.
- **Performance:** Optimized `OplAnnotator` to run more efficiently by using `PsiModificationTracker.MODIFICATION_COUNT` for caching and caching the `hasInterval` check to prevent repetitive heavy file-traversal on every inspection run.
- **Performance:** Moved pip dependency installation execution in `GeneratePythonRunnerAction` to a background pooled thread to prevent UI freezes.
- **Git Tracking:** Removed the generated `gen/` directory from Git tracking and added `gen/` to `.gitignore`.

## [1.4.6] - 2026-07-19

### Added

- **Python Integration:** Added `Generate Python Runner` action to create executable `doopl` scripts for `.mod` files.
- **Python Integration:** Added autocompletion support for `doopl`/`docplex` functions inside Python files.
- **Python Integration:** Added an interactive notification action to install the `doopl` library automatically via PIP.

## [1.4.5] - 2026-07-12

### Fixed

- **Testing:** Fixed path assertion issue in `testCommandLineConstruction` when running on Unix-based OS (GitHub Actions CI/CD).

## [1.4.4] - 2026-07-09

### Added

- **Testing:** Implemented comprehensive automated test suite (Unit Tests, Parsing, Highlighting, Completion, Formatting, Commenter) using IntelliJ Platform Test Framework.
- **IDE Features:** Implemented `Find Usages` for OPL variables and parameters.
- **IDE Features:** Added `Rename Refactoring` (Shift+F6) support for variables, tuples, and constraints.
- **Inspections:** Added real-time CP/MIP semantic inspections (e.g. warning about `min`/`max` nonlinearity, missing `endOf()` in scheduling).
- **Examples:** Created a new `examples/` directory containing sample models (MIP knapsack, CP job-shop scheduling) and a self-contained English demo model (`knapsack_demo.mod`) for JetBrains Marketplace.

### Changed

- **API Modernization:** Migrated action classes (`RunOplModelAction`, `OplCreateFileAction`) to `ActionUpdateThread.BGT` to prevent UI freezes.
- **Architectural Cleanup (Light Services):** Migrated `OplSettingsState` to a modern JetBrains Light Service (`@Service`) to improve IDE startup time and dynamic loading support.
- **Language Parser:** Split the compound token `subject to` into two distinct tokens (`SUBJECT` and `TO`). This ensures proper spacing and indentation tolerance (e.g. multiple spaces or newlines) and resolves syntax parsing failures.

### Fixed

- **Qodana & Settings:** Translated settings strings to English and corrected path exclusions for generated parser/lexer files to resolve Qodana warnings.
- **Performance:** Optimized `OplAnnotator` variable scope checks using `CachedValuesManager` to cache declarations, reducing complexity from $O(N^2)$ to $O(N)$ and preventing UI lags on large `.mod` files.
- **Windows Support:** Standardized file path separators in `OplLinkFilter` to ensure clickable console links work correctly on Windows systems.

## [1.4.2] - 2026-05-02

### Changed

- **Compatibility:** Lowered minimum required IDE version to 2024.3 (build 243) to support older IDE installations.

### Fixed

- **Formatter:** Fixed indentation logic in `OplBlock` to correctly indent nested structures within constraint blocks.
- **Annotator:** Fixed false positive "Undefined variable" errors for multiple iterators in loops (e.g., `forall(i, j in ...)`).

## [1.4.1] - 2026-05-02

### Fixed

- **PyCharm Compatibility:** Added missing `com.intellij.modules.lang` dependency to `plugin.xml` to fix installation issues in PyCharm and other JetBrains IDEs.
- **File Icons:** Added `icon=` attributes to `fileType` definitions in `plugin.xml` to enforce rendering of dedicated SVG icons for `.mod` and `.dat` files.

## [1.4.0] - 2026-05-02

### Added

- **File Templates:** Added "New -> OPL File" action to generate `.mod` and `.dat` files from templates.
- **Execute Blocks:** Extended parser support for `execute` blocks and script-style tokens/operators.
- **CP Keywords:** Added lexer/parser support for `pulse`, `step`, `allDifferent`, and `pack`.
- **Iterators:** Added reusable iterators for `forall`/`sum`, including multiple iterators.
- **Operators:** Added lexer/parser support for `==`, `&&`, `||`, `!`, and `%`.
- **Array Literals:** Added grammar support for bracketed array literals in expressions.

### Fixed

- **Greedy Lexer Bug:** Fixed lexer rules for minus signs and variables (e.g., `r-1` without spaces) preventing false syntax errors.
- **Parser Completeness:** Added missing `==` (double equals) operator to the Lexer and Parser.
- **Formatter:** Added spacing for `==` and `!=`, plus improved indentation in constraint/execute blocks and nested loops.
- **Annotator:** Skip `execute` blocks and improve iterator scope checks to avoid false errors.

## [1.3.0] - 2026-04-13

### Added

- Initial support for IBM ILOG CPLEX OPL.
- Syntax highlighting for .mod and .dat files.
- Run configuration for oplrun solver with .dat file support.
- Auto-Detect mechanism for automatic oplrun executable path discovery (Zero-Config).
- Global settings state to persistently save the CPLEX path across configurations.
- XML-based Live Templates for quick OPL code generation (model skeleton, forall, sum, etc.).
- **Console Issue Navigation:** Clickable file links in execution console that map solver errors directly to specific lines in the code.
- **File Type Support:** Auto-pairing of `.mod` and `.dat` files with the same name during run configuration setup.
- **Code Completion:** Basic autocompletion for OPL keywords and operators via `CompletionContributor`.
- **Structure View:** Interactive side-panel tree view for navigating variables, objectives, and constraints.
- **Code Formatter:** Automatic code formatting (`Ctrl + Alt + L`) for correct indentation and spacing in constraints and loops.
- **Custom Icons:** Unique SVG icons to visually distinguish `.mod` and `.dat` files in the project tree.

### Fixed

- Qodana linting issues.
- Plugin description in README.md.
- **Annotator:** Removed an incorrect Java-style naming validation rule that flagged uppercase OPL variable names (like `NbItems`) as errors.
- Removed duplicated `configurationType` registration from `plugin.xml`.

### Changed

- Updated `README.md` feature lists (EN/PL) to match current implementation.
- Updated `do_zrobienia.md` with explicit `DONE` / `PARTIAL` / `TODO` status markers.

[Unreleased]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.9...HEAD
[1.4.9]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.8...1.4.9
[1.4.8]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.7...1.4.8
[1.4.7]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.6...1.4.7
[1.4.6]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.5...1.4.6
[1.4.5]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.4...1.4.5
[1.4.4]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.2...1.4.4
[1.4.2]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.1...1.4.2
[1.4.1]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.4.0...1.4.1
[1.4.0]: https://github.com/JAANULO/CPLEX-Plugin/compare/1.3.0...1.4.0
[1.3.0]: https://github.com/JAANULO/CPLEX-Plugin/commits/1.3.0
[1.2.0]: https://github.com/JAANULO/CPLEX-Plugin/commits/1.2.0
