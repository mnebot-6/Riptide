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

## androidMain — Room (v9)

Migraciones reales desde v9. `MIGRATION_8_9` añade `hasBeenRewarded` a `day_tasks`.

### Historial de versiones de esquema

| Versión | Cambio |
|---|---|
| 6 | Esquema base |
| 7 | `RecurringTaskDefEntity.time` → `String?` nullable |
| 8 | `EcosystemStateEntity` + `isUnlocked: Boolean` |
| 9 | `DayTaskEntity` + `hasBeenRewarded: Boolean` (migración real) |

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
- XP solo se otorga si `!task.hasBeenRewarded` (controlado en `MainViewModel`)

---

## MarineCategoryAssigner

Redistribuye entre categorías con `isUnlocked=true`, excluyendo DECORATION.

**Orden crítico en DataSeeder**: los `EcosystemState` deben existir ANTES de llamar a `reassign()`. Si no existen, `getUnlocked()` devuelve lista vacía y los bloques no reciben categorías.

---

## MainViewModel — toggleTaskCompleted

```kotlin
fun toggleTaskCompleted(task: DayTask) {
    // Desmarcar:
    //   Si DaySummary existe para la fecha → EXPIRED (el resumen ya procesó)
    //   Si no → PENDING
    // Marcar: COMPLETED + hasBeenRewarded = true
    // XP solo si !task.hasBeenRewarded (objeto original, inmutable)
}
```

---

## MainViewModel — loadDay

```kotlin
private fun loadDay(date: LocalDate) {
    val blocks = loadBlocksWithCategories()
    val tasks = dayTaskRepository.getByDate(date)
    val tasksByBlock = tasks.groupBy { it.blockId }
    val streaksByBlock = ...
    val ecosystemByCategory = MarineCategory.entries.mapNotNull { ... }.toMap()

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
val cycleDuration = (variedDuration / speedMultiplier).toLong().coerceAtLeast(2000L)
```

### Capa 0 — Tempo warping

Deforma el tiempo `t` en `t'` para que la velocidad varíe continuamente:
```
t' = t + (k·P/TAU) · (cos(φ) − cos(TAU·t/P + φ))
```
- Continua y monotónica (k < 1 → derivada > 0 siempre)
- Período ~37s variado por phase → cambio gradual e imperceptible
- `tempoVariation`: 0 = constante (ballena), 0.60 = muy variable (cangrejo)
- tSwim (tiempo deformado) → capas de nado. tRaw → drift y microwobble.

### Variación por instancia

```kotlin
fun instanceNoise(index: Int, seed: Int): Float  // [-1, 1] determinista
fun vary(base: Float, index: Int, seed: Int, pct: Float = 0.12f): Float
```
Aplica ±12% sobre swimDuration, wobbleAmplitude, driftSpeed, tempoVariation.

### Posición X — continua sin saltos

Márgenes simétricos basados en tamaño del emoji:
```kotlin
val halfIcon = iconSize / 2f
val xMin = halfIcon + w * 0.01f
val xMax = w - halfIcon - w * 0.01f
x = (xMin + (xMax - xMin) * swimProgress + w * xPert).coerceIn(xMin, xMax)
```
Easing por especie: `applyEasing(localT, spec.easingType)`.

### Posición Y — cinco capas

```
personalY = zoneCenter + (personalYFraction - 0.5f) * zoneBand

Y = personalY
  + waveY (primaria + secundaria, con tSwim)
  + drift (corriente ambiental, con tRaw)
  + coupledArc (arco acoplado a X, con tSwim)
  + micro (aleta/cola, con tRaw)
```

- **Onda primaria**: `waveCount` entero → sin salto en loop.
- **Onda secundaria**: frecuencia `waveCount·PHI` (irracional) → nunca se sincroniza.
- **Deriva**: `driftSpeed` fraccionario → aperiódica. Usa tRaw.
- **Coupling**: delfín/foca suben en el centro del recorrido (velocidad máxima).
- **Microwobble**: ~1.5Hz, simula movimiento de aleta/cola. Usa tRaw.

### Hit-testing

Un único `pointerInput` con `detectTapGestures(onTap = ...)`. Compara offset contra `creaturePositions` (posiciones reales del último frame). Radio: `maxOf(iconSize * 2.5f, 75f)`.

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
- Grid 3 columnas con `IntrinsicSize.Max` por fila → altura uniforme
- Cards bloqueadas: `progress = categoryLevel / spec.unlockLevel`

---

## Flujo de desbloqueo de criaturas

```
toggleTaskCompleted (si !hasBeenRewarded)
    → addXpForTask(categories)
        → addXp por categoría
            → detecta nivel cruzado → List<CreatureSpec>
            → reparte XP a criaturas existentes
    → si newUnlocks.isNotEmpty → pendingUnlocks en UiState

NightSummaryProcessor.processDay(date, blockNames, blockCategories, summaryTime)
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

`processDay` recibe `summaryTime: LocalTime?`.

Solo se evalúan:
- Tareas con `status == COMPLETED` (siempre)
- Tareas PENDING del día actual con `time != null && time <= summaryTime`

Se ignoran:
- `status == POSTPONED`
- Tareas sin hora (se evaluarán en el siguiente resumen)
- Tareas con hora posterior al summaryTime
- Tareas de otros días

`NightSummaryWorker` lee la hora con `getNightSummaryTime().first()`.

---

## Inputs de fecha y hora

`TimeInputField` y `DateInputField` son campos **readonly** (sin `BasicTextField`, sin emojis).
Click sobre el campo abre el picker correspondiente.
`TimePicker` usa `TimePickerDefaults.colors()` con paleta marina explícita.
`DatePicker` usa `DatePickerDefaults.colors()` con la misma paleta.

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
