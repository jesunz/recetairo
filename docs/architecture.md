# Android Application Architecture

## Architectural Flexibility Principle

**IMPORTANT**: This document establishes the architectural norms and patterns for the project. However, if during development a point is identified that conflicts with these definitions and a technically more correct or appropriate solution exists for the specific context, you must:

1. **Identify the conflict** clearly
2. **Propose the alternative** with technical justification
3. **Consult before implementing** the change of approach
4. **Update this document** if the change is approved

Architecture should serve the project, not unnecessarily limit it.

---

## 1. General Architecture

### Main Pattern
**MVVM (Model-View-ViewModel) + Clean Architecture**

### Layer Separation

#### **Domain Layer**
- **Responsibility**: Pure business logic, independent of frameworks
- **Content**: 
  - Use Cases
  - Models (domain models)
  - Repository Interfaces (contracts)
- **Rules**:
  - Must not have Android Framework dependencies
  - Must not know implementation details of data or UI
  - Must be 100% testable with unit tests

#### **Data Layer**
- **Responsibility**: Management of data sources (local and remote)
- **Content**:
  - Repository Implementations
  - Data Sources (local and remote)
  - DTOs (Data Transfer Objects) for APIs
  - Entities for Room
  - Mappers (conversion between DTOs/Entities and Models)
- **Rules**:
  - Implements interfaces defined in Domain
  - Manages synchronization between data sources
  - Applies **offline-first** strategy

#### **UI Layer** (Presentation Layer)
- **Responsibility**: Presentation and UI state management
- **Content**:
  - ViewModels
  - Composables (Jetpack Compose)
  - UiState (screen states)
  - Navigation
- **Rules**:
  - ViewModels should only depend on Use Cases
  - Composables only observe state, do not contain business logic
  - State management via StateFlow
  - **Reusable Components**: If a UI component is identified that can be reused across multiple features, it must be extracted to the `core/ui/component/` directory instead of being duplicated

---

## 2. Technology Stack

### Language
- **Kotlin** (100% of code)

### UI
- **Jetpack Compose** (declarative UI)
- Material Design 3

### Dependency Injection
- **Hilt** (Dagger Hilt)
- Modules organized by layer and feature

### Navigation
- **Navigation Component** from Jetpack
- Type-safe navigation with Kotlin DSL

### Local Persistence
- **Room** for relational databases
- **DataStore** for preferences and simple configurations
- **Rule**: Analyze each feature and choose the simplest solution that meets requirements (avoid over-engineering)

### Networking
- **Retrofit** for REST API consumption
- **OkHttp** for interceptors and logging
- **Moshi** or **Kotlinx Serialization** for JSON

### Data Strategy
- **Offline-first**: Room as source of truth (single source of truth)
- Background synchronization
- Conflict handling and network states

### State Management
- **StateFlow** for reactive state flows
- **viewModelScope** for coroutines in ViewModels
- **lifecycleScope** for coroutines in UI

---

## 3. Error Handling

### Result Encapsulation
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

### Logging
- **Timber** for centralized logging
- Logs only in development/debug builds
- Do not expose sensitive information in logs

### Strategy by Layer
- **Domain**: Propagates errors via `Result<T>`
- **Data**: Catches network/DB exceptions and converts them to `Result.Error`
- **UI**: Shows errors to user in a friendly way

---

## 4. Concurrency and Coroutines

### Dispatchers by Layer
- **Dispatchers.IO**: Network and database operations (Data Layer)
- **Dispatchers.Main**: UI updates (UI Layer)
- **Dispatchers.Default**: Intensive calculations (if applicable)

### Scopes
- **viewModelScope**: For operations in ViewModels (automatically cancelled)
- **lifecycleScope**: For operations tied to UI lifecycle
- Avoid `GlobalScope` (not automatically cancelled)

---

## 5. Models and Layer Mapping

### Model Separation

#### Data Layer
- **DTO (Data Transfer Object)**: API responses (Retrofit)
  ```kotlin
  @JsonClass(generateAdapter = true)
  data class UserDto(...)
  ```
- **Entity**: Database tables (Room)
  ```kotlin
  @Entity(tableName = "users")
  data class UserEntity(...)
  ```

#### Domain Layer
- **Model**: Domain model (business logic)
  ```kotlin
  data class User(...)
  ```

#### UI Layer
- **UiState**: Screen state
  ```kotlin
  data class UserUiState(
      val isLoading: Boolean = false,
      val user: User? = null,
      val error: String? = null
  )
  ```

### Mappers
- Extension functions for conversion between layers
- Location: In the layer that consumes the model
  ```kotlin
  fun UserDto.toDomain(): User
  fun UserEntity.toDomain(): User
  fun User.toUiState(): UserUiState
  ```

---

## 6. Repository Pattern

### Principles
- **Single Source of Truth**: Room is the main data source
- Repositories expose `Flow<T>` for reactive data
- Transparent synchronization between local and remote

### Offline-First Strategy
1. Emit local data immediately (Room)
2. Make API request in background
3. Update Room with fresh data
4. Room automatically emits updated data

```kotlin
override fun getUsers(): Flow<Result<List<User>>> = flow {
    // 1. Emit local data
    emit(Result.Loading)
    val localData = localDataSource.getUsers().first()
    emit(Result.Success(localData.map { it.toDomain() }))
    
    // 2. Sync with API
    try {
        val remoteData = remoteDataSource.getUsers()
        localDataSource.saveUsers(remoteData.map { it.toEntity() })
    } catch (e: Exception) {
        // Local data already emitted, only log error
        Timber.e(e, "Error syncing users")
    }
}
```

---

## 7. Package Structure

### Feature-Based Organization
```
com.example.app/
├── core/                          # Shared code
│   ├── data/
│   ├── domain/
│   ├── ui/
│   │   ├── component/             # Reusable UI components
│   │   ├── theme/
│   │   └── util/
│   └── di/
├── feature/
│   ├── login/
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   ├── datasource/
│   │   │   └── mapper/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── usecase/
│   │   └── ui/
│   │       ├── viewmodel/
│   │       ├── screen/
│   │       └── component/         # Login-specific components
│   └── profile/
│       └── ...
```

### Advantages
- Scalability: Easy to add/remove features
- Modularization: Each feature can become a module
- Clarity: Everything related to a feature is together

### Reusable Components Rule
- **Feature-specific components**: Kept in `feature/{name}/ui/component/`
- **Reusable components**: Extracted to `core/ui/component/` when:
  - Used in 2 or more features
  - Have future reuse potential
  - Are generic components (buttons, cards, inputs, etc.)
- **NEVER duplicate components**: If similar functionality exists, refactor to reuse

---

## 8. Code Conventions

### Naming
- **Classes and objects**: `PascalCase`
  ```kotlin
  class UserRepository
  object NetworkModule
  ```
- **Functions and variables**: `camelCase`
  ```kotlin
  fun getUserById(userId: String)
  val userName: String
  ```
- **Constants**: `UPPER_SNAKE_CASE`
  ```kotlin
  const val MAX_RETRY_ATTEMPTS = 3
  const val API_BASE_URL = "https://api.example.com"
  ```
- **Packages**: `lowercase` without hyphens
  ```kotlin
  package com.example.app.feature.login.data
  ```

### Static Analysis
- **ktlint**: Code formatting and style
- **Detekt**: Quality analysis and code smells
- Run before each commit

---

## 9. Testing

### Target Coverage

#### Unit Tests (80-90%)
- **What to test**:
  - Use Cases (business logic)
  - ViewModels (state management)
  - Mappers
  - Utilities
- **Tools**:
  - JUnit 5
  - MockK for mocks
  - Turbine for Flow testing
  - Coroutines Test

#### Integration Tests (60-70% of critical flows)
- **What to test**:
  - Repositories with Room (in-memory database)
  - Interaction between layers
  - Complete data flows
- **Tools**:
  - Room in-memory database
  - MockWebServer for APIs

#### UI Tests (40-50% of main flows)
- **What to test**:
  - Navigation between screens
  - User interaction
  - UI states (loading, error, success)
- **Tools**:
  - Compose Testing
  - Hilt Testing

### Testing Principles
- Tests must be **fast** and **deterministic**
- Use **Given-When-Then** for structure
- Name tests descriptively: `should_returnSuccess_when_userExists()`

---

## 10. Dependency Management

### Version Catalog (Gradle)
- Centralize versions in `libs.versions.toml`
- Facilitates updates and consistency

```toml
[versions]
kotlin = "1.9.0"
compose = "1.5.0"

[libraries]
androidx-compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
```

---

## 11. Accessibility and UX

### Basic Requirements
- **TalkBack**: All interactive elements must have `contentDescription`
- **Dark Mode**: Mandatory support with dynamic themes
- **Font Sizes**: Respect system preferences
- **Contrast**: Meet WCAG AA minimum

### Compose
```kotlin
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = "Add item" // Mandatory
)
```

---

## 12. CI/CD and Quality

### Static Analysis
- **Detekt**: Kotlin code analysis
- **ktlint**: Automatic formatting
- Run in pipeline before merge

### Basic Pipeline
1. Compilation
2. Unit tests
3. Static analysis (Detekt + ktlint)
4. Integration tests
5. APK/Bundle generation

---

## 13. Additional Resources

### Official Documentation
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### Recommended Libraries
- [Accompanist](https://google.github.io/accompanist/) - Compose utilities
- [Coil](https://coil-kt.github.io/coil/) - Image loading
- [Timber](https://github.com/JakeWharton/timber) - Logging

---

## Changelog

| Date | Version | Changes |
|-------|---------|---------|
| 2026-05-25 | 1.0.1 | Added reusable UI components rule |
| 2026-05-25 | 1.0.0 | Initial document version |

---

**Last updated**: 2026-05-25
