# CHECKPOINTS — Evaluación del estado final (Android)

> En sistemas multi-agente no se evalúa el camino, se evalúa el destino.
> Estos son los checkpoints objetivos que un revisor (humano o agente) puede usar
> para decidir si el proyecto está sano.

---

## C1 — El arnés está completo

- [ ] Existen los archivos base: `AGENTS.md`, `feature_list.json`, `progress/current.md`
- [ ] Existen los docs de referencia: `architecture.md`, `docs/specs.md`, `docs/conventions.md`, `docs/verification.md`
- [ ] `progress/history.md` existe (puede estar vacío en la primera sesión)

---

## C2 — El estado es coherente

- [ ] Como mucho una feature en `in_progress` en `feature_list.json`
- [ ] Toda feature `done` tiene todas sus tasks marcadas `[x]` en `tasks.md`
- [ ] `progress/current.md` está vacío o describe la sesión activa (no contiene basura de sesiones anteriores)
- [ ] No hay features `in_progress` sin su carpeta `specs/<feature>/` completa

---

## C3 — El código respeta la arquitectura Android

- [ ] La estructura de paquetes sigue el patrón feature-based de `architecture.md` §7
- [ ] La lógica de negocio está en Domain Layer (Use Cases, Models, Repository interfaces)
- [ ] Los repositorios están en Data Layer (implementaciones, DTOs, Entities, Mappers)
- [ ] Las pantallas y ViewModels están en UI Layer
- [ ] No hay dependencias inversas (UI → Domain → Data)
- [ ] Se usa Hilt para todas las dependencias (no hay instanciación manual de repos o use cases)
- [ ] Los ViewModels exponen estado via `StateFlow`
- [ ] Se usa `Result<T>` sealed class para manejo de errores entre capas
- [ ] No hay `println()` o logs de debug en código de producción

---

## C4 — La verificación es real

- [ ] Cada Use Case tiene al menos un test unitario
- [ ] Cada ViewModel tiene al menos un test unitario
- [ ] Los tests usan Given-When-Then como estructura
- [ ] Los tests tienen nombres descriptivos: `should_<resultado>_when_<condición>()`
- [ ] Existe trazabilidad documentada: cada `R<n>` tiene al menos un test concreto

---

## C5 — La sesión se cerró bien

- [ ] No hay archivos temporales sin trackear
- [ ] `progress/history.md` tiene una entrada por la última sesión completada
- [ ] La última feature trabajada está en su estado correcto en `feature_list.json`

---

## C6 — Spec Driven Development

- [ ] Toda feature con `"sdd": true` en estado `spec_ready`, `in_progress` o `done` tiene su carpeta `specs/<name>/` con los 3 archivos: `requirements.md`, `design.md`, `tasks.md`
- [ ] `requirements.md` usa EARS estricto (ver `docs/specs.md`)
- [ ] `design.md` documenta decisiones técnicas alineadas con `architecture.md`
- [ ] Toda feature `done` con `"sdd": true` tiene todas sus tasks marcadas `[x]` en `tasks.md`
- [ ] Cada `R<n>` de `requirements.md` está cubierto por al menos un test concreto

---

## C7 — Arquitectura Android específica

- [ ] Todos los elementos interactivos en Compose tienen `contentDescription`
- [ ] Los Composables no contienen lógica de negocio
- [ ] Las constants están en `UPPER_SNAKE_CASE`
- [ ] Los packages están en `lowercase` sin hyphens
- [ ] Si un componente UI se usa en 2+ features, está en `core/ui/component/`
- [ ] No hay componentes UI duplicados

---

**Cómo usar este archivo**: El reviewer recorre cada checkbox, marca `[x]` o `[ ]`, y rechaza el cierre si quedan boxes vacíos en C1-C7 sin justificación documentada.
