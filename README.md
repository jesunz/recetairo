# Recetairo

Android app for managing a household pantry and generating recipes with AI based on the food
you actually have on hand.

You can add food items via a manual form, barcode scanning (Open Food Facts), or OCR processing
of a purchase receipt. From that pantry, the app generates recipe proposals using Firebase AI
Logic and calculates how well each recipe matches the food already available.

## Features

- **Add food to the pantry**: manual form, barcode scanning (EAN-8/EAN-13/UPC-A) with
  autocomplete via Open Food Facts, and receipt scanning with AI-based food extraction and
  categorization (ML Kit OCR + Firebase AI Logic).
- **Browse the pantry**: listing by category, food items expiring soon, and the full pantry, with
  explicit loading/error/empty states.
- **Manage the pantry**: single and multiple deletion with confirmation, plus a food detail view.
- **AI recipe generation**: select up to 3 pantry items and a serving count (1-4), generate 3
  recipe proposals via Firebase AI Logic with ingredient quantities adapted to that serving count,
  save the chosen recipes, and view recipe details with a pantry-match percentage.

## Tech stack

- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture (Domain / Data / UI)
- **Dependency injection**: Hilt
- **Persistence**: Room (offline-first source of truth) + DataStore
- **Networking**: Retrofit + OkHttp + Moshi (Open Food Facts)
- **AI**: Firebase AI Logic (receipt extraction and recipe generation)
- **Scanning**: CameraX + ML Kit (text recognition and barcode scanning)
- **State**: Kotlin Coroutines + StateFlow
- **Logging**: Timber
- **Tests**: JUnit, Kotest (property-based), Robolectric, Mockito-Kotlin

Kotlin 2.2.10 · AGP 9.2.1 · compileSdk/targetSdk 36 · minSdk 27

## Architecture

MVVM + Clean Architecture with three strict layers:

- **Domain**: pure Kotlin. Use Cases, domain models, repository interfaces. No Android
  dependencies.
- **Data**: repository implementations, DTOs (Retrofit), Room entities, mappers. Offline-first:
  Room is the single source of truth.
- **UI**: ViewModels (StateFlow), Composables, UiState. ViewModels depend only on Use Cases, never
  directly on repositories.

Layer dependency rule: `UI → Domain ← Data`.

Feature-based package structure under `com.jesunez.recetairo`:

```
core/
  data/ domain/ ui/
    component/   ← reusable components (used in 2+ features)
    theme/
  di/
feature/
  <name>/
    data/        ← repository impl, datasource, mapper, dto, entity
    domain/      ← model, repository interface, usecase
    ui/          ← viewmodel, screen, component (feature-specific)
```

Full details in `docs/architecture.md` and `docs/conventions.md`.

## Development

This project follows a **Spec Driven Development (SDD)** workflow: features with `"sdd": true`
in `feature_list.json` go through spec authoring (`specs/<feature>/requirements.md`, `design.md`,
`tasks.md`), human approval, task-by-task implementation, and final review. Details in
`docs/specs.md`.

### Build and test

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Static analysis
./gradlew detekt
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`

## Project status

See `feature_list.json` for the list of features and their status, and `progress/current.md` for
the state of the last working session.
