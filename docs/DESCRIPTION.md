# CPLEX OPL Support for JetBrains IDEs

Native language support for IBM ILOG CPLEX Optimization Programming Language (OPL), including syntax highlighting, code completion, and a built-in runner for the *oplrun* solver.

## Features

- **Syntax Highlighting:** Keywords, model structure, and operators for `.mod` files.
- **Code Completion:** Keyword and built-in function completion.
- **Run Configuration:** Built-in "Run Opl Model" configuration for direct *oplrun* execution from the editor.
- **Auto-Pairing:** Automatic `.dat` file pairing when a matching file name exists.
- **Console Navigation:** Clickable solver error links in the run console.
- **Structure View:** Side panel listing declarations, objective, and constraints.
- **Editor Utilities:** Live templates, commenter, brace matching, and a basic code formatter.

*Requires a local installation of IBM ILOG CPLEX Studio. The path to oplrun can be set manually or auto-detected from common install locations.*
