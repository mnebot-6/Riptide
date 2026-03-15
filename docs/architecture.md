# Arquitectura — Riptide

## Patrón general

MVVM con repositorios. La UI no conoce Room, solo los ViewModels.

```
UI (Compose)
    ↕
ViewModel (commonMain)
    ↕
Repository interface (commonMain)
    ↕
Repository impl (androidMain) → Room DAOs → SQLite
```

---

## commonMain

### expect/actual

```kotlin
expect fun generateUUID(): String
expect fun currentDate(): LocalDate
expect fun parseColor(hex: String): Color
expect fun DrawScope.drawEmoji(emoji: String, x: Float, y: Float, sizeSp: Float, mirrored: Boolean)
expect fun TimePickerDialogWrapper(initial: LocalTime?, onConfirm: (LocalTime?) -> Unit, onDismiss: () -> Unit)
expect fun DatePickerDialogWrapper(initial: LocalDate, onConfirm: (LocalDate?) -> Unit, onDismiss: () -> Unit)
```

### Serialización

`Recurrence` y `WeeklySlot` son `@Serializable`. `LocalTime` usa `LocalTimeSerializer` (ISO string) con `@file:UseSerializers` en `WorkBlock.kt`. Mappers usan `Json.encodeToString<Recurrence>(...)` con tipo explícito.

---

## androidMain — Room (v8)

`fallbackToDestructiveMigration(true)` durante desarrollo.

### Historial de versiones de esquema

| Versión | Cambio |
|---|---|
| 6 | Esquema base |
| 7 | `RecurringTaskDefEntity.time` → `String?` nullable |
| 8 | `EcosystemStateEntity` + `isUnlocked: Boolean` |

---

## EcosystemProcessor

Dependencias: `EcosystemStateRepository` + `MarineCreatureRepository`.

```
addXp(category, xp):
  1. Obtener EcosystemState existente
  2. oldLevel = existing?.currentLevel ?: 1
  3. Calcular newXp y newLevel
  4. Crear o actualizar EcosystemState
  5. Detectar criaturas desbloqueadas: unlockLevel in (oldLevel+1)..newLevel
  6. Obtener criaturas desbloqueadas de la categoría
  7. Repartir xp/N entre ellas (floor), actualizar experience y creatureLevel
  8. Devolver List<CreatureSpec> desbloqueadas
```

Casos especiales:
- `categories` vacío en `addXpForTask` → reparte entre todas las desbloqueadas (tareas sin bloque)
- Criatura recién desbloqueada: `experience=0`, no recibe XP hasta el siguiente evento
- XP sobrante (xp % N) se pierde

---

## MarineCategoryAssigner

Redistribuye entre categorías con `isUnlocked=true`, excluyendo DECORATION.

**Orden crítico en DataSeeder**: los `EcosystemState` deben existir ANTES de llamar a `reassign()`. Si no existen, `getUnlocked()` devuelve lista vacía y los bloques no reciben categorías.

---

## MainViewModel — loadDay

```kotlin
private fun loadDay(date: LocalDate) {
    // blocks, tasks, streaks, ecosystemByCategory (como antes)
    
    // NUEVO: niveles individuales de criaturas
    val creatures = MarineCategory.entries.flatMap { category ->
        ecosystemStateRepository.getByCategory(category) ?: return@flatMap emptyList()
        marineCreatureRepository.getByCategory(category)
    }
    val creatureLevelBySpecies = creatures.associate { it.species to it.creatureLevel }
    
    // actualizar UiState con creatureLevelBySpecies
}
```

---

## AquariumCreatures — efectos por nivel

```kotlin
val creatureLevel = creatureLevelBySpecies[spec.species] ?: 1

// Tamaño
val sizeScale = 0.8f + (creatureLevel - 1) * 0.10f
val iconSize = baseSize * sizeScale

// Velocidad
val speedMultiplier = max(0.3f, 1f + (creatureLevel - 1) * spec.speedScalePerLevel)
val adjustedDuration = (spec.swimDuration / speedMultiplier).toInt().coerceAtLeast(2000)
```

---

## Flujo de desbloqueo de criaturas

```
toggleTaskCompleted
    → addXpForTask(categories)
        → addXp por categoría
            → detecta nivel cruzado → List<CreatureSpec>
            → reparte XP a criaturas existentes
    → si newUnlocks.isNotEmpty → pendingUnlocks en UiState

NightSummaryProcessor.processDay
    → addNightBonus
        → mismo flujo
        → persiste emojis en DataStore

MainActivity.onCreate
    → checkPendingUnlocks → DataStore → UiState
    → pendingSummary primero, luego pendingUnlocks
    → diálogo: emoji + nombre obligatorio
    → confirmUnlock → MarineCreature(experience=0, creatureLevel=1)
```

---

## Resumen nocturno — evaluación selectiva

Solo se evalúan tareas donde:
- `TaskSchedule.OneTime` con `date <= fecha del resumen`, **o**
- Sin fecha pero con `status == COMPLETED`

Se excluyen:
- `status == POSTPONED`
- Tareas futuras no completadas (se evaluarán en su día)

---

## Gestos en MainScreen

El `Box` raíz tiene un `detectDragGestures`:
- **Vertical hacia abajo** (drawer cerrado) → abre drawer
- **Horizontal** (drawer cerrado) → cambia día

El `Box` del drawer tiene su propio `detectDragGestures`:
- **Vertical hacia arriba** → cierra drawer

---

## Convenciones

- IDs: UUID v4
- Colores: hex `"#RRGGBB"`, parseados en androidMain
- Fechas: `LocalDate` / `LocalTime` / `LocalDateTime` de `kotlinx-datetime`
- Métricas internas (score, XP, totalExperience, currentLevel): **nunca visibles al usuario**
- Pantalla bloqueada en portrait (`AndroidManifest`)
- Categorías DECORATION: nunca reciben XP, nunca participan en redistribución
