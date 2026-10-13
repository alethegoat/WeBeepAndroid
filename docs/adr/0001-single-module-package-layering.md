# 0001. Single Gradle module with package-level feature layering

Status: Accepted

## Context

The app is small (about 230 main source files) and has one `:app` module. Splitting into Gradle modules (`:core:*`, `:feature:*`) gives enforced boundaries and parallel builds, but adds build-logic, convention plugins, Hilt and Room wiring across modules, and slower iteration on a small codebase.

## Decision

Keep one module. Enforce the structure by package and by convention:

- `core/*` for shared infrastructure.
- `feature/<x>/{domain,data,presentation,di}` for each feature.
- `presentation -> domain <- data`. `domain` is pure Kotlin with no `android.*` imports.
- Repository interfaces in `domain`, implementations in `data`, bound by a Hilt module in `feature/<x>/di`.

detekt and review enforce style; the layering itself is checked by review and by the `domain` package having no Android imports.

## Consequences

- Fast builds and no multi-module plumbing.
- Boundaries are not compiler-enforced. A wrong import compiles. Reviewers must check it (see CONTRIBUTING.md).
- Features already depend on each other's `domain` types (for example `fileviewer` on `course` and `download`), and `core/session` depends on features. These links are listed in ARCHITECTURE.md.
- The layout makes a later split mechanical: each `feature/<x>` and `core/<y>` is already a unit. Revisit if build times grow or a second app needs the code.
