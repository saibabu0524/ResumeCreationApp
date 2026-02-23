# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-20

### Added
- **Foundational Architecture**: Built using MVVM + Clean Architecture with strict separation of modules (`app`, `core:*`, `feature:*`).
- **UI Framework**: Fully implemented in Jetpack Compose, emphasizing performance (skipping unnecessary recompositions) and readability.
- **Asynchronous Processing**: Integrated Kotlin Coroutines with StateFlow and SharedFlow to manage UI state and one-off events.
- **Dependency Injection**: Integrated Dagger-Hilt globally with targeted dispatcher injection (`@IoDispatcher`).
- **CI/CD Pipelines**: Automated GitHub Actions workflows for Building, Testing, Linting (Ktlint & Detekt), and Signed Release.
- **Performance Tooling**: Implemented Macrobenchmarking and Baseline Profile Generators to significantly reduce cold startup times.
- **Robust Documentation**: Detailed `ARCHITECTURE_DECISIONS`, `CODING_STANDARDS`, and complete Multi-Phase Roadmap available in the `docs` directory.

### Changed
- Phase 8: Finalized automated deployment actions, accessibility checks, and benchmark additions.
- Optimized Compose compiler parameters to gather skipped/restartable metrics when `composeCompilerReports` property is provided.

### Removed
- Removed legacy XML resources; App explicitly forbids LiveData and `Fragment`s.

[1.0.0]: https://github.com/saibabu0524/Resumecreationapp/releases/tag/v1.0.0
