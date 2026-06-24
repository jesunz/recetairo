# AGENTS.md — Mapa de navegación para agentes de IA (Android)

> Este archivo es el punto de entrada para cualquier agente que trabaje en este proyecto Android.
> NO es una biblia de reglas: es un mapa. Lee solo lo que necesites cuando lo necesites (divulgación progresiva).

---

## 1. Antes de empezar (obligatorio)

1. Lee `progress/current.md` para entender en qué estado quedó la última sesión.
2. Lee `feature_list.json`. Toda feature nueva (`"sdd": true`) pasa por Spec Driven Development — ver `docs/specs.md` y §4 de este archivo.
3. Lee `architecture.md` antes de tocar cualquier código Android.
4. Lee `docs/specs.md` antes de redactar o leer cualquier spec.

---

## 2. Mapa del repositorio

| Archivo / Carpeta | Contenido | Cuándo leerlo |
|---|---|---|
| `feature_list.json` | Lista de features con estado (pending / spec_ready / in_progress / done / blocked) | Siempre, al empezar |
| `progress/current.md` | Estado de la sesión actual | Siempre, al empezar |
| `progress/history.md` | Bitácora append-only de sesiones anteriores | Si necesitas contexto histórico |
| `specs/<feature>/` | requirements.md + design.md + tasks.md (Kiro-style) | Antes de implementar cualquier feature con `"sdd": true` |
| `architecture.md` | Normas arquitectónicas Android (MVVM + Clean Architecture) | Antes de implementar |
| `docs/specs.md` | Proceso SDD: EARS notation, los 3 archivos, puerta de aprobación humana | Antes de redactar o leer un spec |
| `docs/conventions.md` | Reglas de estilo, nombres, estructura Kotlin/Android | Antes de escribir código |
| `docs/verification.md` | Cómo verificar que el trabajo funciona (trazabilidad requirements) | Antes de declarar una tarea como done |
| `CHECKPOINTS.md` | Criterios objetivos de "estado final correcto" | Para auto-evaluarte |

---

## 3. Reglas duras (no negociables)

- **Una sola tarea a la vez.** En `tasks.md`, implementa únicamente la tarea que el humano indique explícitamente. Para antes de la siguiente y espera aprobación.
- **No declares una tarea `done`** sin verificación del humano.
- **No saltes la fase de spec.** Toda feature con `"sdd": true` debe pasar por la fase de redacción de spec y obtener aprobación humana antes de tocar código.
- **No saltes la puerta de aprobación humana.** El flujo se detiene en `spec_ready` y espera confirmación del humano.
- **Una sola feature `in_progress`** en `feature_list.json` en cualquier momento.
- **Cumple `architecture.md`** en todo momento. Si detectas conflicto, consulta al humano antes de proceder.
- **Documenta mientras trabajas** en `progress/current.md`, no al final.
- Si no sabes algo, busca en `docs/` antes de inventarlo.

---

## 4. Flujo de trabajo (SDD)

```
pending → [spec_author] → spec_ready → ⏸ HUMANO aprueba spec
       → in_progress → [implementer: tarea por tarea] → ⏸ HUMANO aprueba cada tarea
       → [reviewer] → done
```

1. El agente detecta la primera feature `pending` con `"sdd": true`.
2. Actúa como `spec_author`: crea `specs/<name>/{requirements.md, design.md, tasks.md}` y marca el status como `spec_ready`.
3. **Pausa.** El humano lee el spec y aprueba (o pide cambios).
4. Una vez aprobado el spec, cambia el status a `in_progress`.
5. Actúa como `implementer`: implementa **una sola tarea** de `tasks.md` indicada por el humano, la marca `[x]` al completarla.
6. **Pausa.** El humano verifica la tarea implementada y aprueba (o pide cambios).
7. Repite el paso 5-6 para cada tarea restante.
8. Una vez todas las tareas completadas, actúa como `reviewer`: verifica trazabilidad `R<n>` ↔ test y tasks completas.
9. Si el reviewer aprueba, marca la feature como `done` y registra en `progress/history.md`.

---

## 5. Roles del agente

### spec_author
- Redacta `specs/<feature>/requirements.md` en EARS notation estricta
- Redacta `specs/<feature>/design.md` con decisiones técnicas alineadas a `architecture.md`
- Redacta `specs/<feature>/tasks.md` con checklist ejecutable referenciando `R<n>`
- Marca la feature como `spec_ready` en `feature_list.json`
- **NO toca código fuente**

### implementer
- Lee `specs/<feature>/tasks.md` y espera que el humano indique qué tarea implementar
- Implementa **exactamente una tarea** por turno
- Verifica compliance con `architecture.md` antes y después de implementar
- Marca la tarea como `[x]` en `tasks.md` al completarla
- Documenta en `progress/current.md` los archivos tocados
- **NO avanza a la siguiente tarea sin aprobación humana**

### reviewer
- Verifica que todas las tasks estén marcadas `[x]`
- Verifica trazabilidad `R<n>` ↔ test para cada requirement
- Verifica compliance con `architecture.md`
- Verifica que los tests pasen
- Genera `progress/review_<feature>.md` con el resultado
- Aprueba o rechaza (con razones concretas)

---

## 6. Cierre de sesión (lifecycle)

Antes de terminar:

1. Si la feature está acabada: marca `status: "done"` en `feature_list.json`.
2. Mueve el resumen de `progress/current.md` al final de `progress/history.md`.
3. Vacía `progress/current.md` dejando solo la plantilla.
4. No dejes archivos temporales, ni código de debug, ni TODOs sin contexto.

---

## 7. Si te bloqueas

- Relee la sección relevante de `docs/` o `architecture.md`.
- Si detectas un conflicto con `architecture.md`, documenta el bloqueo en `progress/current.md` y consulta al humano.
- Nunca inventes una solución arquitectónica sin consultar.
