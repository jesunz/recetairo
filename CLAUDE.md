# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential reading before starting

1. `progress/current.md` — state of the last session
2. `feature_list.json` — all features and their status
3. `docs/architecture.md` — MVVM + Clean Architecture norms (authoritative; update it if a conflict is found)
4. `docs/conventions.md` — naming and code structure rules
5. `docs/specs.md` — Spec Driven Development (SDD) process

## Build and test commands

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Single module tests
./gradlew :app:testDebugUnitTest

# Static analysis
./gradlew detekt
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`

## Architecture

**MVVM + Clean Architecture** with three strict layers:

- **Domain** — pure Kotlin; Use Cases, domain Models, Repository interfaces. No Android framework imports.
- **Data** — Repository implementations, DTOs (Retrofit), Entities (Room), Mappers. Implements Domain interfaces. Offline-first: Room is the single source of truth.
- **UI** — ViewModels (StateFlow), Composables (Jetpack Compose), UiState. ViewModels depend only on Use Cases, never directly on repositories.

Layer dependency rule: `UI → Domain ← Data`. Never invert.

### Error handling across layers

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

Data layer catches exceptions and emits `Result.Error`. UI layer renders errors to the user.

### Package structure

Feature-based under `com.jesunez.recetairo`:

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

Rule: never duplicate UI components. If a component appears in 2+ features, move it to `core/ui/component/`.

### Tech stack

- UI: Jetpack Compose + Material Design 3
- DI: Hilt (no manual instantiation of repos or use cases)
- Persistence: Room (main store) + DataStore (preferences)
- Networking: Retrofit + OkHttp + Moshi or Kotlinx Serialization
- State: StateFlow + viewModelScope / lifecycleScope (no GlobalScope, no LiveData)
- Logging: Timber (debug builds only; no `println()` or `Log.d()` in production)

## Workflow: Spec Driven Development (SDD)

All features with `"sdd": true` in `feature_list.json` must follow this flow:

```
pending → [spec_author] → spec_ready
        → ⏸ HUMAN approves spec
        → in_progress → [implementer: one task] → ⏸ HUMAN approves task
        → repeat per task → [reviewer] → done
```

**Hard rules:**
- Only one feature `in_progress` at a time.
- Only one task implemented per turn — stop and wait for human approval before proceeding.
- No code before the spec is approved.
- Specs live in `specs/<feature-name>/`: `requirements.md` (EARS notation), `design.md` (technical decisions), `tasks.md` (checklist referencing `R<n>`).

The `spec_author` role never touches source code. The `implementer` role reads `tasks.md`, implements exactly one task, and marks it `[x]`. The `reviewer` verifies R↔test traceability and marks the feature `done`.

### Verification before marking any task done

- `./gradlew assembleDebug` passes
- `./gradlew testDebugUnitTest` passes for affected tests
- Each task references at least one `R<n>` requirement
- Each test follows Given-When-Then and is named `should_<result>_when_<condition>()`
- Full checklist in `docs/verification.md`

## Key conventions (summary)

| Artifact | Naming |
|---|---|
| ViewModel | `ProfileViewModel` |
| Use Case | `GetUserProfileUseCase` (with `operator fun invoke`) |
| Repository interface | `ProfileRepository` |
| Repository impl | `ProfileRepositoryImpl` |
| DTO | `UserProfileDto` |
| Room entity | `UserProfileEntity` |
| UiState | `ProfileUiState` |
| Screen composable | `ProfileScreen` |
| Hilt module | `ProfileModule` |
| Mapper | `fun UserProfileDto.toDomain()` |

Composables: split `ProfileScreen` (holds ViewModel, not testable) from `ProfileContent` (pure state → UI, testable). Every interactive element requires `contentDescription`.

## Self-evaluation

Use `CHECKPOINTS.md` to verify project health before closing a session. Key checkpoints: C3 (architecture compliance), C4 (test coverage and traceability), C6 (SDD completeness).
