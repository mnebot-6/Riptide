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
    val blocks = loadBlocksWithCategories()
    val tasks = dayTaskRepository.getByDate(date)
    val tasksByBlock = tasks.groupBy { it.blockId }
    val streaksByBlock = ...
    val ecosystemByCategory = MarineCategory.entries.mapNotNull { ... }.toMap()

    // Niveles individuales + lista completa de criaturas para el UiState
    val allCreaturesFromDb = MarineCategory.entries.flatMap { category ->
        ecosystemStateRepository.getByCategory(category) ?: return@flatMap emptyList()
        marineCreatureRepository.getByCategory(category)
    }
    val creatureLevelBySpecies = allCreaturesFromDb.associate { it.species to it.creatureLevel }

    _uiState.update {
        it.copy(
            ...,
            creatureLevelBySpecies = creatureLevelBySpecies,
            creaturesData = allCreaturesFromDb
        )
    }
}
```

---

## AquariumCreatures — sistema de movimiento

### Tamaño y velocidad por nivel

```kotlin
val sizeScale = 0.8f + (creatureLevel - 1) * 0.10f
val speedMultiplier = max(0.3f, 1f + (creatureLevel - 1) * spec.speedScalePerLevel)
val cycleDuration = (spec.swimDuration / speedMultiplier).toInt().coerceAtLeast(2000)
```

### Posición X — continua sin saltos

`rawProgress = (t * 12000f / cycleDuration) % 1f` — SIN phase.
El phase solo afecta Y. Garantiza que X empieza y acaba en el mismo punto en cada loop.
Easing por especie: `applyEasing(localT, spec.easingType)`.

### Posición Y — cinco capas

```
personalY = zoneCenter + (personalYFraction - 0.5f) * zoneBand

Y = personalY
  + (primaryWave*(1-erraticness) + secondaryWave*erraticness) * zoneBand * wobbleAmplitude
  + sin(t * TAU * driftSpeed + phase*TAU) * zoneBand * driftAmplitude
  - sin(swimProgress * PI) * zoneBand * verticalCoupling   // arco acoplado a X
  + sin(t * TAU * 18f + phase*TAU) * zoneBand * microWobble
```

- **Onda primaria**: `waveCount` entero → `sin(1·2π·N+φ) = sin(0·2π·N+φ)` → sin salto en loop.
- **Onda secundaria**: frecuencia `waveCount·PHI` (irracional) → nunca se sincroniza con la primaria.
- **Deriva**: `driftSpeed` fraccionario → aperiódica respecto al ciclo de 12s.
- **Coupling**: delfín/foca suben en el centro del recorrido (velocidad máxima).
- **Microwobble**: ~1.5Hz, simula movimiento de aleta/cola.

### Hit-testing

Un único `pointerInput` en el `Layout`. Compara offset contra `creaturePositions` (posiciones reales del último frame). Radio mínimo 60px.

---

## CreatureDetailDialog

- Trigger: tap → `freezeState.freeze(species)` + `frozenTimeMap[species] = currentTimeMs`
- La criatura queda congelada en su posición hasta que el dialog llama `onDismiss` → `unfreeze`
- Nickname: `BasicTextField` con overlay de placeholder. Permite guardar vacío → `null`.
- `XpBar`: progreso al siguiente nivel sin números. Puntos de nivel (máx 10).
- Persistencia: `viewModel.updateCreatureNickname` → `marineCreatureRepository.update`

---

## EcosystemScreen

- Ruta: `ROUTE_ECOSYSTEM = "ecosystem"` en `Navigation.kt`
- Lee `uiState.ecosystemByCategory` y `uiState.creaturesData` del `MainViewModel` compartido
- Grid 3 columnas con `IntrinsicSize.Max` por fila → altura uniforme entre cards desbloqueadas y bloqueadas
- Cards bloqueadas: `progress = categoryLevel / spec.unlockLevel`

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
