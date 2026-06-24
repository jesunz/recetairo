# Spec Driven Development (SDD) — Android

> Este proyecto sigue un flujo Kiro-style: requirements → design → tasks → code.
> El código no se escribe hasta que el spec está aprobado por un humano.
> La implementación avanza **una tarea a la vez**, con aprobación humana entre cada una.

---

## Estructura

Cada feature nueva (`"sdd": true` en `feature_list.json`) tiene una carpeta dedicada en `specs/`:

```
specs/<feature-name>/
├── requirements.md   # QUÉ se necesita (EARS notation)
├── design.md         # CÓMO se construirá (decisiones técnicas + arquitectura)
└── tasks.md          # PASOS concretos a implementar (uno a la vez)
```

El `feature-name` coincide con el campo `name` de `feature_list.json`.

---

## Estados de una feature

| Estado | Significado |
|---|---|
| `pending` | Sin spec. El spec_author es el primero en actuar. |
| `spec_ready` | Spec redactado. Esperando aprobación humana. **NO se toca código.** |
| `in_progress` | Spec aprobado. Implementando tarea a tarea con aprobación entre cada una. |
| `done` | Todas las tasks completas, reviewer aprobó, sesión cerrada. |
| `blocked` | Atascado. Razón documentada en `progress/current.md`. |

---

## Las dos puertas de aprobación humana

El flujo automático se detiene en **dos momentos**:

### Puerta 1 — Aprobación del spec
Cuando el `spec_author` termina los tres archivos, marca la feature como `spec_ready` y para.
El humano lee `specs/<feature>/` completo y dice "aprobado" (o pide cambios).
Solo entonces el agente transiciona `spec_ready → in_progress`.

### Puerta 2 — Aprobación por tarea
Una vez en `in_progress`, el implementer ejecuta **una sola tarea** y para.
El humano verifica el código producido y dice "continúa con T<n>" para avanzar.
El agente **NUNCA** avanza a la siguiente tarea sin instrucción explícita del humano.

```
pending → [spec_author] → spec_ready
       → ⏸ PUERTA 1: HUMANO aprueba spec
       → in_progress → [implementer: T1] → ⏸ PUERTA 2: HUMANO aprueba T1
       → [implementer: T2] → ⏸ PUERTA 2: HUMANO aprueba T2
       → ... → [reviewer] → done
```

---

## requirements.md — EARS estricto

Las requirements se redactan en EARS (Easy Approach to Requirements Syntax). Cada requirement es un párrafo numerado con uno de estos cinco patrones:

| Patrón | Plantilla |
|---|---|
| Ubicuo | El sistema DEBE `<acción>`. |
| Evento | CUANDO `<disparador>`, el sistema DEBE `<acción>`. |
| Estado | MIENTRAS `<estado>`, el sistema DEBE `<acción>`. |
| Opcional | DONDE `<feature opcional>`, el sistema DEBE `<acción>`. |
| No deseado | SI `<evento no deseado>` ENTONCES el sistema DEBE `<acción>`. |

**Reglas duras:**
- Cada requirement tiene un id estable: `R1`, `R2`, ...
- Cada requirement DEBE ser verificable por al menos un test concreto.
- No mezcles varios `DEBE` en un mismo requirement. Si hay más de uno, pártelo.
- No uses verbos blandos ("podría", "puede", "soporta"). Solo `DEBE` / `NO DEBE`.

**Ejemplo:**

```markdown
## R1
CUANDO el usuario navega a la pantalla de perfil, el sistema DEBE
mostrar el nombre, email y avatar del usuario autenticado.

## R2
SI la carga de datos del perfil falla ENTONCES el sistema DEBE
mostrar un mensaje de error descriptivo y un botón de reintentar.

## R3
MIENTRAS se cargan los datos del perfil, el sistema DEBE
mostrar un indicador de carga (loading state).
```

---

## design.md — decisiones técnicas Android

Captura **antes de tocar código**:

- Qué archivos/clases se crean o modifican (por capa: Domain, Data, UI).
- Qué modelos nuevos aparecen (DTO, Entity, Domain Model, UiState).
- Qué Use Cases se crean y sus firmas.
- Qué Composables se crean y si son candidatos para `core/ui/component/`.
- Qué módulo Hilt se necesita o modifica.
- Qué alternativa se descartó y por qué (mínimo una).
- Puntos donde la feature roza las normas de `architecture.md` (si los hay).

**NO es ingeniería desde primeros principios** — apóyate en `architecture.md` y `docs/conventions.md`.

**Ejemplo de estructura:**

```markdown
## Archivos afectados

### Domain Layer
- `feature/profile/domain/model/UserProfile.kt` — nuevo modelo de dominio
- `feature/profile/domain/repository/ProfileRepository.kt` — nueva interfaz
- `feature/profile/domain/usecase/GetUserProfileUseCase.kt` — nuevo use case

### Data Layer
- `feature/profile/data/dto/UserProfileDto.kt` — DTO de la API
- `feature/profile/data/repository/ProfileRepositoryImpl.kt` — implementación

### UI Layer
- `feature/profile/ui/viewmodel/ProfileViewModel.kt` — ViewModel con StateFlow
- `feature/profile/ui/screen/ProfileScreen.kt` — Composable de pantalla
- `feature/profile/ui/ProfileUiState.kt` — estado de la pantalla

## Alternativa descartada
Usar LiveData en lugar de StateFlow → descartado porque `architecture.md` §2
establece StateFlow como estándar del proyecto.
```

---

## tasks.md — checklist ejecutable

Pasos discretos en orden, cada uno con checkbox. Cada task:
- Referencia al menos un `R<n>` que cubre.
- Es lo suficientemente pequeña para implementarse y verificarse en una sesión.
- Especifica qué archivo(s) se crean o modifican.

**El implementer ejecuta una sola task por turno y espera aprobación humana.**

**Ejemplo:**

```markdown
- [ ] T1 — Crear `UserProfile.kt` en Domain Layer con los campos definidos en `design.md`. Cubre: R1.
- [ ] T2 — Crear `ProfileRepository.kt` (interfaz) en Domain Layer. Cubre: R1, R2.
- [ ] T3 — Crear `GetUserProfileUseCase.kt` con manejo de `Result<T>`. Cubre: R1, R2, R3.
- [ ] T4 — Crear `UserProfileDto.kt` y mapper `toDomain()`. Cubre: R1.
- [ ] T5 — Implementar `ProfileRepositoryImpl.kt` con estrategia offline-first. Cubre: R1, R2.
- [ ] T6 — Crear `ProfileUiState.kt` con estados loading/success/error. Cubre: R1, R2, R3.
- [ ] T7 — Crear `ProfileViewModel.kt` exponiendo estado via StateFlow. Cubre: R1, R2, R3.
- [ ] T8 — Crear `ProfileScreen.kt` con Composable que observa el estado. Cubre: R1, R2, R3.
- [ ] T9 — Configurar módulo Hilt para las dependencias de esta feature. Cubre: R1.
- [ ] T10 — Escribir tests unitarios para `GetUserProfileUseCase`. Cubre: R1, R2.
- [ ] T11 — Escribir tests unitarios para `ProfileViewModel`. Cubre: R1, R2, R3.
```

---

## Trazabilidad (regla dura)

- Cada test debe poder mapearse a un `R<n>` de su spec.
- Cada `R<n>` debe tener al menos un test concreto.
- El `reviewer` comprueba esta correspondencia explícitamente y rechaza si falta.

El implementer documenta el mapa en `progress/impl_<feature>.md`:

```markdown
## Trazabilidad
- R1 → `should_showUserProfile_when_loadSuccess`
- R2 → `should_showError_when_loadFails`, `should_showRetryButton_when_error`
- R3 → `should_showLoading_while_fetching`
```

---

## Cuándo NO aplica SDD

Las features con `"sdd": false` o sin el campo `sdd` NO tienen spec. SDD solo aplica hacia adelante para features marcadas explícitamente.
