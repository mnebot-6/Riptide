# Arquitectura -- Riptide

## Patron general

MVVM con repositorios. La UI no conoce Room, solo los ViewModels.

```
UI (Compose)
    |
ViewModel (commonMain)
    |
Repository interface (commonMain)
    |
Repository impl (androidMain) -> Room DAOs -> SQLite
                               -> Ktor Client -> Backend API
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

### Serializacion

`Recurrence` y `WeeklySlot` son `@Serializable`. `LocalTime` usa `LocalTimeSerializer` (ISO string) con `@file:UseSerializers` en `WorkBlock.kt`. Mappers usan `Json.encodeToString<Recurrence>(...)` con tipo explicito.

---

## androidMain -- Room (v12)

Migraciones reales desde v9. Sin `fallbackToDestructiveMigration`.

### Historial de versiones de esquema

| Version | Cambio |
|---|---|
| 6 | Esquema base |
| 7 | `RecurringTaskDefEntity.time` -> `String?` nullable |
| 8 | `EcosystemStateEntity` + `isUnlocked: Boolean` |
| 9 | `DayTaskEntity` + `hasBeenRewarded: Boolean` (migracion real) |
| 10 | `DayTaskEntity` + `notificationsEnabled: Boolean`; `RecurringTaskDefEntity` + `notificationsEnabled: Boolean` |
| 11 | `BlockStreakEntity` + `longestStreak: Int` |
| 12 | `updatedAt TEXT` en 8 tablas + `isDeleted INTEGER` en 3 (WorkBlock, DayTask, RecurringTaskDef) para sync. 11 ALTER TABLE. DAOs con `getModifiedSince()`, `upsertAll()`, `stampUpdatedAt()`. Queries filtran `isDeleted = 0`. |

---

## EcosystemProcessor

Dependencias: `EcosystemStateRepository` + `MarineCreatureRepository`.

```
addXp(category, xp, fromOverflow=false):
  1. Obtener EcosystemState existente
  2. Si categoria completa (todas las especies desbloqueadas):
     - kept = xp / 2 (sube nivel + reparte a criaturas)
     - overflow = xp - kept -> redistributeOverflow() (si !fromOverflow)
  3. oldLevel, calcular newXp y newLevel
  4. Crear o actualizar EcosystemState
  5. Detectar lootboxes: CATEGORY_UNLOCK_LEVELS[category] filtrado por (oldLevel+1)..newLevel
  6. Repartir XP entre criaturas desbloqueadas de la categoria
  7. Devolver List<PendingLootbox>
```

XP overflow:
- `redistributeOverflow(category, overflow)`: encuentra la categoria de menor nivel entre las NO completas (excluyendo DECORATION), llama `addXp(lowestCategory, overflow, fromOverflow=true)`
- `fromOverflow=true` evita recursion infinita

Casos especiales:
- `categories` vacio en `addXpForTask` -> reparte entre todas las desbloqueadas (tareas sin bloque)
- Criatura recien desbloqueada: `experience=0`, no recibe XP hasta el siguiente evento
- XP sobrante (xp % N) se pierde
- XP solo se otorga si `!task.hasBeenRewarded` (controlado en `MainViewModel`)

---

## LootboxResolver

Dependencias: `MarineCreatureRepository`.

```
resolve(lootbox: PendingLootbox): CreatureSpec
  1. Obtener allSpecs de la categoria
  2. Filtrar especies ya desbloqueadas -> candidates
  3. weightedRandom(candidates): seleccion ponderada por rarity.weight
```

La resolucion ocurre cuando el usuario ABRE la lootbox, no cuando se gana.

---

## MarineCategoryAssigner

Redistribuye entre categorias con `isUnlocked=true`, excluyendo DECORATION y COMPANION.

**Orden critico en DataSeeder**: los `EcosystemState` deben existir ANTES de llamar a `reassign()`.

---

## MainViewModel -- toggleTaskCompleted

```kotlin
fun toggleTaskCompleted(task: DayTask) {
    // Desmarcar:
    //   Si DaySummary existe para la fecha -> EXPIRED
    //   Si no -> PENDING
    // Marcar: COMPLETED + hasBeenRewarded = true
    // XP solo si !task.hasBeenRewarded (objeto original, inmutable)
}
```

---

## AquariumCreatures -- sistema de movimiento

### Tamano y velocidad por nivel

```kotlin
val sizeScale = 0.8f + (creatureLevel - 1) * 0.10f
val speedMultiplier = max(0.3f, 1f + (creatureLevel - 1) * spec.speedScalePerLevel)
val cycleDuration = (variedDuration / speedMultiplier).toLong().coerceAtLeast(2000L)
```

### Capa 0 -- Tempo warping

Deforma el tiempo `t` en `t'` para que la velocidad varie continuamente:
```
t' = t + (k*P/TAU) * (cos(phi) - cos(TAU*t/P + phi))
```
- Continua y monotonica (k < 1 -> derivada > 0 siempre)
- Periodo ~37s variado por phase
- `tempoVariation`: 0 = constante (ballena), 0.60 = muy variable (cangrejo)
- tSwim (tiempo deformado) -> capas de nado. tRaw -> drift y microwobble.

### Variacion por instancia

```kotlin
fun instanceNoise(index: Int, seed: Int): Float  // [-1, 1] determinista
fun vary(base: Float, index: Int, seed: Int, pct: Float = 0.12f): Float
```
Aplica +-12% sobre swimDuration, wobbleAmplitude, driftSpeed, tempoVariation.

### Posicion X -- continua sin saltos

Margenes simetricos basados en tamano del emoji:
```kotlin
val halfIcon = iconSize / 2f
val xMin = halfIcon + w * 0.01f
val xMax = w - halfIcon - w * 0.01f
x = (xMin + (xMax - xMin) * swimProgress + w * xPert).coerceIn(xMin, xMax)
```
Easing por especie: `applyEasing(localT, spec.easingType)`.

### Posicion Y -- cinco capas

```
personalY = zoneCenter + (personalYFraction - 0.5f) * zoneBand

Y = personalY
  + waveY (primaria + secundaria, con tSwim)
  + drift (corriente ambiental, con tRaw)
  + coupledArc (arco acoplado a X, con tSwim)
  + micro (aleta/cola, con tRaw)
```

- **Onda primaria**: `waveCount` entero -> sin salto en loop.
- **Onda secundaria**: frecuencia `waveCount*PHI` (irracional) -> nunca se sincroniza.
- **Deriva**: `driftSpeed` fraccionario -> aperiodica. Usa tRaw.
- **Coupling**: delfin/foca suben en el centro del recorrido (velocidad maxima).
- **Microwobble**: ~1.5Hz, simula movimiento de aleta/cola. Usa tRaw.

### Hit-testing

Un unico `pointerInput` con `detectTapGestures(onTap = ...)`. Compara offset contra `creaturePositions` (posiciones reales del ultimo frame). Radio: `maxOf(iconSize * 2.5f, 75f)`.

---

## AquariumBackground -- cielo, superficie y fondo marino

- **Cielo dinamico**: `skyForHour(hour)` -> 7 periodos (noche, amanecer, manana dorada, dia, atardecer, crepusculo, noche tardia). Usa `kotlin.time.Clock.System.now()`.
- **Superficie del agua**: ola animada con `Path` + `quadraticTo` (8 segmentos, 9.dp amplitud). Cresta principal + cresta secundaria (60% amplitud) para efecto de profundidad.
- **Terreno** (`AquariumTerrain`): curva suave procedural con Catmull-Rom. Amplitud reducida a `0.022f` para ondulaciones casi imperceptibles. Y-range: `[0.80, 0.90]`.
- **Fondo marino**: banda de arena con gradiente + lineas de textura ondulada. Guijarros sutiles dispersos. Sistema procedural de decoraciones.
- **AquariumBounds**: `SURFACE_FRACTION = 0.08f`, `FLOOR_FRACTION = 0.85f`, compartidas entre background y criaturas.

---

## CreatureRenderer + CreatureIcon

- `CreatureRenderer`: interfaz con `fun DrawScope.render(x, y, size, level, animTimeMs, mirrored)`.
- `rendererFor(species)`: despacha entre Canvas renderers.
- **70 renderers** registrados -- cobertura total de todas las 70 especies:
  - **`flora/`** (9 fijos, sin mirror, escala reducida): BrainCoral, Anemone, Kelp, Posidonia, FanCoral, TubeSponge, SeaGrass, FireCoral, StaghornCoral
  - **`fauna/`** (61 renderers): peces, cefalopodos, mamiferos, reptiles, crustaceos, moluscos, pelagicos, decoraciones, companion (Bimba)
- `CreatureIcon`: `@Composable` reutilizable. Canvas animado (60fps via `withFrameNanos`) para cualquier especie con renderer.
- Usado en `EcosystemScreen` (52.dp) y `CreatureDetailDialog` (80.dp).

---

## Sync -- arquitectura offline-first

### Flujo de datos

```
App (Room v12)                         Backend (Ktor + PostgreSQL)
     |                                       |
     |-- SyncManager.sync() --------------->|
     |   push: entities con updatedAt       |
     |   POST /api/sync                     |
     |<-- pull: server changes -------------|
     |   upsert local con conflict res.     |
     |                                       |
```

### Conflict resolution

- `updatedAt` wins: el registro mas reciente prevalece
- `hasBeenRewarded` OR-merge: si cualquiera de los dos es true, se mantiene true
- Server deletion autoritativo: `isDeleted` del servidor siempre se respeta
- Orden FK-safe en upsert

### Triggers de sync

- `SyncTrigger`: debounce 5s tras cualquier mutacion local
- `SyncWorker`: WorkManager periodico cada 1h (constraint CONNECTED)
- Manual: boton "Sincronizar ahora" en drawer
- `InitialSyncPreparer`: stampa datos pre-existentes para primera sync

### Auth

- Google Sign-In -> `POST /auth/google` (verificacion idToken) -> JWT pair
- Access token: 24h. Refresh token: 30d con theft detection
- Auto-refresh transparente via Ktor Client Bearer Auth
- `DataStoreTokenProvider`: puente entre prefs y Ktor Auth

---

## Widget Android (Glance)

- `RiptideWidget` (GlanceAppWidget): tareas del dia agrupadas por bloque, barra de progreso
- `WidgetUpdater.refreshAll()`: refresca widgets desde la app en `onResume`
- Metadata: 3x3 celdas, redimensionable, auto-update 30min
- Widget interactivo: toggle de tareas completadas

---

## Live Wallpaper

- `RiptideWallpaperService`: WallpaperService + Engine con Choreographer 30fps vsync-aligned
- `CanvasDrawScope` bridge: reutiliza `drawAquariumBackground()` y `drawAquariumCreatures()` sin portar codigo
- `WallpaperDataProvider`: lee criaturas de Room DB, refresco cada 5min
- `GLOBAL_SPEED_MULTIPLIER = 2.0f` para velocidad mas natural

---

## Resumen nocturno -- evaluacion selectiva

`processDay` recibe `summaryTime: LocalTime?`.

Solo se evaluan:
- Tareas con `status == COMPLETED` (siempre)
- Tareas PENDING del dia actual con `time != null && time <= summaryTime`

Se ignoran:
- `status == POSTPONED`
- Tareas sin hora (se evaluaran en el siguiente resumen)
- Tareas con hora posterior al summaryTime
- Tareas de otros dias

`NightSummaryWorker` lee la hora con `getNightSummaryTime().first()`.

---

## Push Notifications

### Canales (Android O+)

| Canal | ID | Descripcion |
|---|---|---|
| Resumen nocturno | `night_summary` | Push tras `processDay` con stats del dia |
| Aviso matutino | `morning_reminder` | Recordatorio configurable por el usuario |
| Recordatorio de tarea | `task_reminder` | Notificacion a la hora exacta de una tarea |

### Workers

```
NightSummaryWorker.doWork()
  -> processDay(targetDate, summaryTime)
  -> sendNightSummaryNotification(completedCount, totalCount)
  -> schedule(context, summaryTime)   // se reprograma para manana

MorningReminderWorker.doWork()
  -> sendMorningReminderNotification()
  -> schedule(context, time)          // se reprograma para manana

TaskReminderWorker.doWork()
  -> sendTaskReminderNotification(taskTitle, taskId)
  // one-shot, no se reprograma
```

### TaskReminderScheduler

```kotlin
interface TaskReminderScheduler {
    fun scheduleReminder(task: DayTask)
    fun cancelReminder(taskId: String)
    fun rescheduleAll()   // llamar al arrancar la app
}
```

`rescheduleAll()` obtiene tareas PENDING con `notificationsEnabled = true` y hora futura.

---

## Inputs de fecha y hora

`TimeInputField` y `DateInputField` son campos **readonly**. Click sobre el campo abre el picker correspondiente.

---

## Gestos en MainScreen

- **Vertical hacia abajo** (drawer cerrado) -> abre drawer
- **Horizontal** (drawer cerrado) -> cambia dia
- **Vertical hacia arriba** (drawer) -> cierra drawer

---

## Convenciones

- IDs: UUID v4
- Colores: hex `"#RRGGBB"`, parseados en androidMain
- Fechas: `LocalDate` / `LocalTime` / `LocalDateTime` de `kotlinx-datetime`
- Metricas internas (score, XP, totalExperience, currentLevel): **nunca visibles al usuario**
- Pantalla bloqueada en portrait (`AndroidManifest`)
- Categorias DECORATION y COMPANION: nunca reciben XP regular, nunca participan en redistribucion
