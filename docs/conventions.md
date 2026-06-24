# Conventions — Estilo y normas de código Android

> Complementa `architecture.md`. Si hay conflicto, `architecture.md` tiene precedencia.

---

## 1. Nomenclatura

### Clases y objetos
```kotlin
// PascalCase siempre
class UserProfileViewModel
object NetworkModule
data class UserProfile(...)
sealed class ProfileUiState
interface ProfileRepository
```

### Funciones y variables
```kotlin
// camelCase siempre
fun getUserProfile(userId: String): Flow<Result<UserProfile>>
val isLoading: Boolean
var currentUser: User?
```

### Constantes
```kotlin
// UPPER_SNAKE_CASE
const val MAX_RETRY_ATTEMPTS = 3
const val API_BASE_URL = "https://api.example.com"
const val DATABASE_NAME = "app_database"
```

### Packages
```kotlin
// lowercase sin hyphens ni underscores
package com.example.app.feature.profile.domain.usecase
package com.example.app.core.ui.component
```

### Archivos
- Un archivo = una clase/interfaz principal (mismos nombre)
- Los archivos de UiState pueden agrupar sealed classes relacionadas: `ProfileUiState.kt`
- Los mappers van en archivos separados: `UserProfileMapper.kt`

---

## 2. Nombrado por tipo de artefacto

| Tipo | Sufijo / Patrón | Ejemplo |
|---|---|---|
| ViewModel | `ViewModel` | `ProfileViewModel` |
| Use Case | `UseCase` | `GetUserProfileUseCase` |
| Repository (interfaz) | `Repository` | `ProfileRepository` |
| Repository (impl) | `RepositoryImpl` | `ProfileRepositoryImpl` |
| Data Source (local) | `LocalDataSource` | `ProfileLocalDataSource` |
| Data Source (remote) | `RemoteDataSource` | `ProfileRemoteDataSource` |
| DTO | `Dto` | `UserProfileDto` |
| Entity (Room) | `Entity` | `UserProfileEntity` |
| UiState | `UiState` | `ProfileUiState` |
| Composable (pantalla) | `Screen` | `ProfileScreen` |
| Composable (componente) | ninguno o descriptivo | `AvatarImage`, `ErrorBanner` |
| Módulo Hilt | `Module` | `ProfileModule`, `NetworkModule` |
| Mapper | `Mapper` o extensión `toDomain()` | `fun UserProfileDto.toDomain()` |

---

## 3. Estructura de un ViewModel

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserProfileUseCase().collect { result ->
                _uiState.update { state ->
                    when (result) {
                        is Result.Success -> state.copy(
                            isLoading = false,
                            profile = result.data
                        )
                        is Result.Error -> state.copy(
                            isLoading = false,
                            error = result.message ?: "Error desconocido"
                        )
                        is Result.Loading -> state.copy(isLoading = true)
                    }
                }
            }
        }
    }
}
```

---

## 4. Estructura de un Use Case

```kotlin
class GetUserProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    operator fun invoke(userId: String): Flow<Result<UserProfile>> =
        profileRepository.getUserProfile(userId)
}
```

Reglas:
- Un Use Case = una operación de negocio
- El nombre describe la acción: `Get`, `Save`, `Delete`, `Update`, `Validate`
- Usar `operator fun invoke(...)` para que sea llamable como función
- No contiene lógica de UI ni referencias a Android Framework

---

## 5. Estructura de un Composable

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onRetry = viewModel::loadProfile,
        onNavigateBack = onNavigateBack
    )
}

// Separar Screen (con ViewModel) de Content (sin ViewModel, testeable)
@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // UI pura: solo observa estado, no contiene lógica
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorBanner(
            message = uiState.error,
            onRetry = onRetry
        )
        uiState.profile != null -> ProfileData(profile = uiState.profile)
    }
}
```

---

## 6. Estructura de tests

### Nombrado
```kotlin
// Patrón: should_<resultado>_when_<condición>
@Test
fun should_returnUserProfile_when_repositorySucceeds() { ... }

@Test
fun should_emitError_when_networkFails() { ... }

@Test
fun should_showLoadingState_while_fetchingData() { ... }
```

### Estructura Given-When-Then
```kotlin
@Test
fun should_returnUserProfile_when_repositorySucceeds() {
    // Given
    val expectedProfile = UserProfile(id = "1", name = "Test User")
    coEvery { mockRepository.getUserProfile("1") } returns flowOf(Result.Success(expectedProfile))

    // When
    val results = mutableListOf<Result<UserProfile>>()
    useCase("1").test {
        results.add(awaitItem())
        awaitComplete()
    }

    // Then
    assertThat(results.first()).isInstanceOf(Result.Success::class.java)
    assertThat((results.first() as Result.Success).data).isEqualTo(expectedProfile)
}
```

---

## 7. Manejo de errores

```kotlin
// En Data Layer: captura excepciones y convierte a Result.Error
override fun getUserProfile(userId: String): Flow<Result<UserProfile>> = flow {
    emit(Result.Loading)
    try {
        val data = remoteDataSource.getProfile(userId)
        emit(Result.Success(data.toDomain()))
    } catch (e: HttpException) {
        emit(Result.Error(e, "Error de servidor: ${e.code()}"))
    } catch (e: IOException) {
        emit(Result.Error(e, "Sin conexión a internet"))
    }
}.flowOn(Dispatchers.IO)
```

---

## 8. Accessibility

Todo elemento interactivo en Compose DEBE tener `contentDescription`:

```kotlin
// ✅ Correcto
IconButton(onClick = onDelete) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.delete_note) // Obligatorio
    )
}

// ❌ Incorrecto
Icon(imageVector = Icons.Default.Delete, contentDescription = null)
```

---

## 9. Lo que NO se hace

- ❌ No usar `GlobalScope` para coroutines
- ❌ No tener lógica de negocio en Composables
- ❌ No acceder directamente a repositorios desde ViewModels (solo via Use Cases)
- ❌ No usar `LiveData` (usar `StateFlow`)
- ❌ No duplicar componentes UI (verificar `core/ui/component/` primero)
- ❌ No dejar `println()` ni `Log.d()` en código de producción (usar Timber)
- ❌ No instanciar repositorios o use cases manualmente (usar Hilt)
