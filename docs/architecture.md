# Arquitectura — Riptide

## Patrón general

MVVM con repositorios. La UI no conoce Room, solo los ViewModels. Los repositorios abstraen el origen de datos para facilitar la migración a backend en v4 sin tocar ViewModels ni UI.

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

Todo lo independiente de plataforma vive aquí: modelos de dominio, interfaces de repositorio, ViewModels y UI.

### expect/actual

```kotlin
// commonMain
expect fun generateUUID(): String
expect fun currentDate(): LocalDate
expect fun parseColor(hex: String): Color
expect fun DrawScope.drawEmoji(emoji: String, x: Float, y: Float, sizeSp: Float, mirrored: Boolean)

// androidMain
actual fun generateUUID() = UUID.randomUUID().toString()
actual fun currentDate() = Clock.System.todayIn(TimeZone.currentSystemDefault())
actual fun parseColor(hex: String) = Color(android.graphics.Color.parseColor(hex))
actual fun DrawScope.drawEmoji(...) // nativeCanvas.drawText con android.graphics.Paint

// iosMain
actual fun generateUUID() = NSUUID().UUIDString()
actual fun DrawScope.drawEmoji(...) // pendiente arreglar en v3
// parseColor parsea el hex manualmente
```

### Serialización

`Recurrence` y `WeeklySlot` son `@Serializable`. `LocalTime` usa `LocalTimeSerializer` custom (ISO string), aplicado a nivel de fichero con `@file:UseSerializers(LocalTimeSerializer::class)` en `WorkBlock.kt`.

Los mappers usan tipos explícitos `Json.encodeToString<Recurrence>(...)` / `Json.decodeFromString<Recurrence>(...)` para evitar fallos en runtime con sealed classes.

Plugin requerido en `build.gradle.kts`:
```kotlin
alias(libs.plugins.kotlinSerialization)
```

---

## androidMain — Room (v6)

- `RiptideDatabase` — `fallbackToDestructiveMigration(true)` durante desarrollo. Migrar a migraciones reales en v3.
- `DatabaseProvider` — singleton con `lazy`
- IDs como `String` (UUID) para facilitar sincronización futura con backend

### Tablas

| Tabla | Entity |
|---|---|
| `work_blocks` | `WorkBlockEntity` |
| `block_categories` | `BlockCategoryEntity` — PK compuesta `(blockId, category)`, FK CASCADE desde `work_blocks` |
| `day_tasks` | `DayTaskEntity` |
| `recurring_task_defs` | `RecurringTaskDefEntity` — FK CASCADE desde `work_blocks` |
| `day_summaries` | `DaySummaryEntity` |
| `block_streaks` | `BlockStreakEntity` — PK `blockId` (sin campo id separado) |
| `ecosystem_states` | `EcosystemStateEntity` |
| `marine_creatures` | `MarineCreatureEntity` |

### DayTaskEntity — campos clave

`scheduleType` ("ONE_TIME" | "RECURRING"), `date` (String?), `time` (String?), `recurrence` (String? JSON), `status` (String), `completedAt` (String?), `postponedTo` (String?), `sourceTaskId` (String?), `blockId` (String? FK SET_NULL).

---

## ViewModelFactory (androidMain)

Android requiere factories para inyectar dependencias. Cada ViewModel tiene su factory que obtiene repositorios desde `DatabaseProvider`.

`MainViewModelFactory(context)` — construye `MainViewModel` con todos los repositorios + `RecurringTaskGenerator` + `MarineCategoryAssigner` + `EcosystemProcessor` + `UserPreferencesRepository` + `MarineCreatureRepository`.

---

## MainViewModel

Constructor recibe: `WorkBlockRepository`, `BlockCategoryRepository`, `DayTaskRepository`, `RecurringTaskDefRepository`, `RecurringTaskGenerator`, `MarineCategoryAssigner`, `BlockStreakRepository`, `DaySummaryRepository`, `EcosystemProcessor`, `EcosystemStateRepository`, `UserPreferencesRepository`, `MarineCreatureRepository`.

Funciones públicas:
- `selectDate(date)` — cambia día seleccionado y recarga tareas
- `toggleTaskCompleted(task)` — alterna PENDING/COMPLETED + añade XP + detecta desbloqueos
- `addOneTimeTask(title, blockId?, date, time?)` — crea tarea puntual
- `addRecurringTask(title, blockId, time, recurrence)` — crea `RecurringTaskDef` + genera instancias para los próximos 7 días
- `updateOneTimeTask(original, title, blockId?, date, time?)` — edita tarea puntual existente
- `deleteTask(task)` — elimina por ID (tareas no recurrentes)
- `deleteRecurringTaskInstance(task)` — elimina solo esta ocurrencia
- `deleteRecurringTaskFromDate(task)` — desactiva la def + elimina instancias PENDING/POSTPONED desde esta fecha
- `deleteRecurringTaskAll(task)` — desactiva la def + elimina todas las instancias
- `postponeTask(task, postponedTo)` — marca original como POSTPONED, crea nueva instancia PENDING en la nueva fecha
- `insertBlockAndReassign / updateBlockAndReassign / deleteBlockAndReassign` — CRUD de bloques con reasignación automática de categorías marinas
- `dismissSummary()` — cierra diálogo de resumen nocturno
- `confirmUnlock(spec, nickname)` — guarda `MarineCreature` con nickname y avanza la cola
- `dismissUnlock()` — descarta unlock actual y avanza la cola
- `reload()` — recarga completa + `checkPendingSummary()` + `checkPendingUnlocks()`

---

## NightSummaryProcessor (commonMain)

Lógica pura de cierre de día. Se llama desde `NightSummaryWorker` (a la hora configurada) y desde `MainActivity.onCreate` (fallback si no se ejecutó por la noche).

```
processDay(date, blockNames, blockCategories)
  1. Si ya existe DaySummary para esa fecha → no hacer nada (idempotente)
  2. Expira tareas PENDING del día → EXPIRED
  3. Calcula score = completadas / total
  4. blockStreakProcessor.processDay(date, blockIds) → Map<String, Int>
  5. ecosystemProcessor.addNightBonus(score, bestStreak, allCategories) → List<CreatureSpec>
  6. Persiste nuevos unlocks en DataStore via UserPreferencesRepository
  7. buildMessage(score, total, completed, streaks, blockNames)
  8. Guarda DaySummary
```

---

## EcosystemProcessor (commonMain)

Gestiona la XP del ecosistema marino. Devuelve las criaturas recién desbloqueadas en cada operación.

```kotlin
addXpForTask(categories): List<CreatureSpec>   // 10 XP divididas entre categorías
addNightBonus(score, bestStreak, categories): List<CreatureSpec>
```

Internamente detecta criaturas desbloqueadas comparando `oldLevel` vs `newLevel` contra `allCreatures[].unlockLevel`.

---

## EcosystemLevelCalculator (commonMain, object)

```
nivel 1 = 0 XP
nivel 2 = 100 XP  (+100)
nivel 3 = 250 XP  (+150)
nivel 4 = 450 XP  (+200)
nivel 5 = 700 XP  (+250)
...cada nivel cuesta 50 XP más que el anterior
```

Funciones: `xpForLevel(n)`, `levelForXp(xp)`, `xpInCurrentLevel(xp)`, `xpForNextLevel(level)`, `nightBonus(score, bestStreak)`.

---

## AquariumBackground / AquariumCreatures (commonMain/presentation/aquarium/)

Dos composables independientes que se apilan en el `Box` raíz de `MainScreen`:

**`AquariumBackground`** — siempre visible. Dibuja en Canvas:
- Fondo degradado oceánico
- Plantas con oscilación animada (`swayAngle` infinito)
- Burbujas con trayectoria vertical + wobble horizontal

**`AquariumCreatures`** — visible cuando hay criaturas desbloqueadas. Dibuja sobre el contenido con `drawWithContent`:
- Filtra `allCreatures` por `unlockLevel <= currentLevel` de cada categoría
- Criaturas móviles: nadan de lado a lado con oscilación vertical `sin()`
- Criaturas fijas (FLORA, MOLLUSK): posición fija en zona inferior
- `drawEmoji(emoji, x, y, sizeSp, mirrored)` — expect/actual por plataforma

---

## NightSummaryScheduler (commonMain / interfaz)

Abstrae la lectura/escritura de la hora del resumen nocturno y la planificación del worker.

```kotlin
interface NightSummaryScheduler {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    fun scheduleWorker(time: LocalTime)
}
```

| Plataforma | Implementación |
|---|---|
| androidMain | `NightSummarySchedulerImpl` — usa `UserPreferencesRepositoryImpl` (DataStore) + `NightSummaryWorker` |
| iosMain | `NightSummarySchedulerImpl` — stub vacío, devuelve `flowOf(LocalTime(23,30))` |

---

## UserPreferencesRepository (commonMain / interfaz)

```kotlin
interface UserPreferencesRepository {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    suspend fun getPendingUnlocks(): List<String>   // emojis pendientes de nombrar
    suspend fun setPendingUnlocks(emojis: List<String>)
}
```

Implementación androidMain: DataStore Preferences. Almacena hora/minuto como `Int` separados y unlocks como String con separador `|`. Valor por defecto: 23:30.

---

## Flujo de desbloqueo de criaturas

```
toggleTaskCompleted / addNightBonus
    → EcosystemProcessor.addXp detecta nivel cruzado
    → devuelve List<CreatureSpec> nuevas
    → MainViewModel: añade a pendingUnlocks en UiState
    → (caso nocturno): persiste emojis en DataStore
    → al arrancar: checkPendingUnlocks() lee DataStore → añade a UiState

MainScreen:
    → pendingSummary primero (si existe)
    → pendingUnlocks.first() → diálogo con emoji + campo nombre
    → confirmUnlock(spec, nickname) → guarda MarineCreature → drop(1)
    → siguiente unlock en cola
```

---

## NightSummaryWorker (androidMain)

`CoroutineWorker` de WorkManager. Se programa como `OneTimeWorkRequest` con delay calculado hasta la próxima ocurrencia de la hora configurada. Usa `ExistingWorkPolicy.REPLACE` para reemplazar si se cambia la hora.

---

## Ordenación en pantalla principal

**Bloques:** por hora de inicio de su slot ese día de la semana. Bloques sin horario ese día van al final.

**Tareas dentro de un bloque:**
1. Con hora → ordenadas por hora ascendente
2. Sin hora → orden de inserción
3. Completadas al final — misma sub-ordenación

**Tareas sin bloque:** sección propia "Sin bloque" con icono 📋, antes de los bloques con asignación.

---

## MarineCategoryAssigner

Redistribuye categorías marinas automáticamente al crear, editar o eliminar bloques. El usuario nunca las ve ni las configura. La XP acumulada en `EcosystemState` se mantiene intacta al redistribuir.

| Nº bloques | Distribución |
|---|---|
| 1 | 5 categorías (todas) |
| 2 | 3 + 3 (1 se repite) |
| 3 | 2 + 2 + 2 (1 se repite) |
| 4 | 2 + 1 + 1 + 1 (1 se repite) |
| 5+ | 1 por bloque sin repetición |

---

## Navegación

`NavHost` en `App.kt` con tres rutas:

```
ROUTE_MAIN         → MainScreen
ROUTE_BLOCK_CREATE → BlockFormScreen (nuevo)
ROUTE_BLOCK_EDIT   → BlockFormScreen (editar, recibe blockId)
```

Al guardar o eliminar un bloque, `BlockFormViewModel` emite `BlockFormResult.Saved` o `BlockFormResult.Deleted`, la navegación llama `mainViewModel.reload()` y hace `popBackStack()`.

---

## Gestos en MainScreen

Un único `detectDragGestures` en el `Box` exterior gestiona todo:

- **Vertical hacia abajo** → abre drawer
- **Vertical hacia arriba** → cierra drawer
- **Horizontal** (solo si drawer cerrado) → cambia día

El drawer usa `Animatable(drawerOffsetY)`. Overlay oscuro semitransparente al abrir; toque fuera cierra.

---

## Convenciones

- IDs: UUID v4 generados con `generateUUID()` (expect/actual)
- Colores: hex string `"#RRGGBB"`, parseados en androidMain con `android.graphics.Color.parseColor`
- Fechas: siempre `LocalDate` / `LocalTime` / `LocalDateTime` de `kotlinx-datetime`, nunca `java.util.*`
- Base de datos: `fallbackToDestructiveMigration()` durante desarrollo — migrar en v3
- Dependencias de plataforma (WorkManager, DataStore, Context): nunca en commonMain directamente — siempre a través de interfaces o parámetros
- Métricas internas (score, XP, totalExperience, currentLevel): **nunca visibles al usuario**
