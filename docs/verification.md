# Verification — Cómo demostrar que el trabajo funciona (Android)

> Antes de declarar cualquier tarea o feature como completada, el agente DEBE
> poder responder afirmativamente a todas las preguntas de este documento.

---

## 1. Verificación por tarea (Puerta 2)

Antes de presentar al humano una tarea como completada:

### Checklist de código
- [ ] El código compila sin errores (`./gradlew assembleDebug`)
- [ ] Los tests relacionados con esta tarea pasan (`./gradlew testDebugUnitTest`)
- [ ] No hay warnings de Kotlin no justificados
- [ ] El código sigue las convenciones de `docs/conventions.md`
- [ ] El código cumple con `architecture.md` para la capa afectada

### Checklist arquitectónico
- [ ] **Domain Layer**: ¿El Use Case es puro Kotlin, sin dependencias Android?
- [ ] **Data Layer**: ¿El repositorio implementa la interfaz definida en Domain?
- [ ] **UI Layer**: ¿El ViewModel solo depende de Use Cases (no de repos directamente)?
- [ ] **UI Layer**: ¿El Composable solo observa estado, sin lógica de negocio?
- [ ] **Hilt**: ¿Las nuevas dependencias están correctamente inyectadas?
- [ ] **StateFlow**: ¿El ViewModel expone estado vía `StateFlow`, no `MutableStateFlow`?
- [ ] **Result<T>**: ¿Los errores se propagan correctamente entre capas?

### Checklist de tests
- [ ] Existe al menos un test que cubre el `R<n>` de esta tarea
- [ ] El test sigue la estructura Given-When-Then
- [ ] El nombre del test sigue el patrón `should_<resultado>_when_<condición>()`
- [ ] El test es determinista (no depende de tiempo real ni estado externo)

---

## 2. Verificación de trazabilidad (antes del reviewer)

El implementer DEBE generar `progress/impl_<feature>.md` con:

```markdown
# Implementación: <feature-name>

## Archivos creados/modificados
- `path/al/archivo.kt` — descripción del cambio
- `path/al/otro.kt` — descripción del cambio

## Trazabilidad R<n> → test
- R1 → `should_<test>_when_<condicion>` en `<TestClass>`
- R2 → `should_<test>_when_<condicion>` en `<TestClass>`
- R3 → `should_<test>_when_<condicion>` en `<TestClass>`

## Compliance con architecture.md
- ✅ Domain Layer: [descripción]
- ✅ Data Layer: [descripción]
- ✅ UI Layer: [descripción]
- ✅ Hilt: [descripción]

## Tasks completadas
- [x] T1 — [descripción]
- [x] T2 — [descripción]
```

---

## 3. Verificación del reviewer (cierre de feature)

El reviewer genera `progress/review_<feature>.md` verificando:

### Trazabilidad completa
Para cada `R<n>` en `specs/<feature>/requirements.md`:
- [ ] Existe al menos un test concreto que lo cubre
- [ ] El test está documentado en `progress/impl_<feature>.md`
- [ ] El test pasa actualmente

### Tasks completas
- [ ] Todas las tasks en `specs/<feature>/tasks.md` están marcadas `[x]`
- [ ] No hay tasks pendientes sin justificación documentada

### Compliance arquitectónico
- [ ] Verificado C3 de `CHECKPOINTS.md` para esta feature
- [ ] Verificado C7 de `CHECKPOINTS.md` para esta feature
- [ ] No hay violaciones de las capas (inversión de dependencias)
- [ ] No hay componentes UI duplicados

### Veredicto del reviewer
```markdown
## Veredicto
**Estado**: APROBADO / RECHAZADO

**Razones** (si RECHAZADO):
- [Razón concreta con referencia a R<n> o tarea]

**Observaciones** (opcional):
- [Notas para el siguiente ciclo]
```

---

## 4. Comandos de verificación útiles

```bash
# Compilar módulo debug
./gradlew assembleDebug

# Ejecutar todos los unit tests
./gradlew testDebugUnitTest

# Ejecutar tests de un módulo específico
./gradlew :feature:profile:testDebugUnitTest

# Análisis estático
./gradlew detekt
./gradlew ktlintCheck

# Formateo automático
./gradlew ktlintFormat

# Ver reporte de tests
# Abre: app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 5. Lo que NO cuenta como verificación

- ❌ "El código se ve bien" → sin compilación no es válido
- ❌ "Debería funcionar" → sin tests ejecutados no es válido
- ❌ Tests que no referencian ningún `R<n>` → no cuentan para trazabilidad
- ❌ Tests que mockean todo y no prueban lógica real → no cuentan
- ❌ Tarea marcada `[x]` sin código correspondiente → no es válido
