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

// commonMain/presentation/components/
expect fun TimePickerDialogWrapper(initial: LocalTime?, onConfirm: (LocalTime?) -> Unit, onDismiss: () -> Unit)
expect fun DatePickerDialogWrapper(initial: LocalDate, onConfirm: (LocalDate?) -> Unit, onDismiss: () -> Unit)

// androidMain
actual fun generateUUID() = UUID.randomUUID().toString()
actual fun currentDate() = Clock.System.todayIn(TimeZone.currentSystemDefault())
actual fun parseColor(hex: String) = Color(android.graphics.Color.parseColor(hex))
actual fun DrawScope.drawEmoji(...) // nativeCanvas.drawText con android.graphics.Paint
actual fun TimePickerDialogWrapper(...) // Dialog propio con estética marina + TimePicker M3
actual fun DatePickerDialogWrapper(...) // Dialog propio con estética marina + DatePicker M3

// iosMain
actual fun generateUUID() = NSUUID().UUIDString()
actual fun DrawScope.drawEmoji(...) // pendiente arreglar en v3
actual fun TimePickerDialogWrapper(...) // stub: llama onDismiss
actual fun DatePickerDialogWrapper(...) // stub: llama onDismiss
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

## androidMain — Room (v7)

- `RiptideDatabase` — versión 7. `fallbackToDestructiveMigration(true)` durante desarrollo. Migrar a migraciones reales en v3.
- `DatabaseProvider` — singleton con `lazy`
- IDs como `String` (UUID) para facilitar sincronización futura con backend

### Tablas

| Tabla | Entity |
|---|---|
| `work_blocks` | `WorkBlockEntity` |
| `block_categories` | `BlockCategoryEntity` — PK compuesta `(blockId, category)`, FK CASCADE desde `work_blocks` |
| `day_tasks` | `DayTaskEntity` |
| `recurring_task_defs` | `RecurringTaskDefEntity` — FK CASCADE desde `work_blocks`, `time String?` nullable |
| `day_summaries` | `DaySummaryEntity` |
| `block_streaks` | `BlockStreakEntity` — PK `blockId` (sin campo id separado) |
| `ecosystem_states` | `EcosystemStateEntity` |
| `marine_creatures` | `MarineCreatureEntity` |

### DayTaskEntity — campos clave

`scheduleType` ("ONE_TIME" | "RECURRING"), `date` (String?), `time` (String?), `recurrence` (String? JSON), `status` (String), `completedAt` (String?), `postponedTo` (String?), `sourceTaskId` (String?), `blockId` (String? FK SET_NULL).

### RecurringTaskDefEntity — campos clave

`blockId` (String FK CASCADE), `title` (String), `time` (String? — nullable, hora opcional), `recurrence` (String JSON), `isActive` (Boolean).

---

## Componentes de input estandarizados (commonMain/presentation/components/)

### TimeInputField

```kotlin
@Composable
fun TimeInputField(
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    nullable: Boolean = true,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showPickerIcon: Boolean = true
)
```

- `BasicTextField` invisible captura solo dígitos (máx 4); overlay `Text` muestra `HH:mm`
- El cursor nunca cruza el `:` — problema resuelto separando input de visualización
- Dígitos rellenados con `--` por la izquierda mientras se escribe
- `compact=true` — campo reducido para `BlockFormScreen` y `MainDrawer`
- `showPickerIcon=false` — oculta el icono del reloj en contextos muy compactos

### DateInputField

```kotlin
@Composable
fun DateInputField(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    nullable: Boolean = false,
    modifier: Modifier = Modifier
)
```

- Misma técnica: `BasicTextField` invisible + overlay formateado como `YYYY-MM-DD`
- Los dígitos del usuario reemplazan los de la fecha de hoy desde la derecha:
  - escribir `"14"` → `YYYY-MM-14`
  - escribir `"70601"` → `2027-06-01`

### InputFieldDialogs (expect/actual)

Los pickers son `Dialog` propios (no `AlertDialog` de M3) para controlar el fondo y el tema:

- Fondo `OceanMid (#1B3A6B)`
- `MaterialTheme` override con `primary = Accent (#7EC8E3)`, `onPrimary = OceanDeep`
- `TimePicker` y `DatePicker` de M3 coloreados con `DatePickerDefaults.colors`
- iOS: stubs que llaman `onDismiss()` directamente (pendiente en v3)

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
- `addOneTimeTask(title, blockId?, date, time?)`
- `addRecurringTask(title, blockId, time: LocalTime?, recurrence)` — crea `RecurringTaskDef` + genera instancias 7 días
- `updateOneTimeTask(original, title, blockId?, date, time?)`
- `updateRecurringTask(sourceId, title, blockId, time: LocalTime?, recurrence)` — actualiza def + borra instancias PENDING futuras + regenera
- `deleteTask / deleteRecurringTaskInstance / deleteRecurringTaskFromDate / deleteRecurringTaskAll`
- `postponeTask(task, date: LocalDate, time: LocalTime?)` — marca POSTPONED, crea nueva instancia PENDING con hora opcional
- `insertBlockAndReassign / updateBlockAndReassign / deleteBlockAndReassign`
- `dismissSummary()` — persiste fecha descartada en DataStore + limpia UiState
- `confirmUnlock(spec, nickname)` — guarda `MarineCreature` + `drop(1)` de la cola
- `dismissUnlock()` — descarta sin nombre + `drop(1)`
- `reload()` — recarga completa + `checkPendingSummary()` + `checkPendingUnlocks()`
- `updateNightSummaryTime(time)` — persiste hora en DataStore

### checkPendingSummary

Compara la fecha del summary de ayer con `getLastDismissedSummaryDate()` en DataStore. Si coinciden, no muestra el diálogo. Evita que el resumen reaparezca al reabrir la app o rotar la pantalla.

---

## NightSummaryProcessor (commonMain)

Lógica pura de cierre de día. Se llama desde `NightSummaryWorker` (a la hora configurada) y desde `MainActivity.onCreate` (fallback).

```
processDay(date, blockNames, blockCategories)
  1. Si ya existe DaySummary para esa fecha → no hacer nada (idempotente)
  2. Filtra tareas evaluables:
     - Excluye POSTPONED
     - Incluye OneTime con date <= fecha del resumen
     - Incluye sin fecha si están COMPLETED
     - Excluye sin fecha y PENDING (se evalúan en el resumen de otro día)
  3. Expira tareas PENDING evaluables → EXPIRED
  4. Calcula score = completadas / total (sobre evaluables)
  5. blockStreakProcessor.processDay(date, blockIds) → Map<String, Int>
  6. ecosystemProcessor.addNightBonus(score, bestStreak, allCategories) → List<CreatureSpec>
  7. Persiste nuevos unlocks en DataStore via UserPreferencesRepository
  8. buildMessage(score, total, completed, streaks, blockNames)
  9. Guarda DaySummary
```

---

## EcosystemProcessor (commonMain)

Gestiona la XP del ecosistema marino. Devuelve las criaturas recién desbloqueadas.

```kotlin
addXpForTask(categories): List<CreatureSpec>   // 10 XP divididas entre categorías
addNightBonus(score, bestStreak, categories): List<CreatureSpec>
```

Detecta desbloqueos comparando `oldLevel` vs `newLevel` contra `allCreatures[].unlockLevel`.

---

## EcosystemLevelCalculator (commonMain, object)

```
nivel 1 = 0 XP
nivel 2 = 1 XP    ← garantiza primer pez con 1 sola tarea
nivel 3 = 21 XP   (+20)
nivel 4 = 61 XP   (+40)
nivel 5 = 126 XP  (+65)
nivel 6 = 226 XP  (+100)
nivel 7 = 376 XP  (+150)
... cada nivel cuesta ×1.5 que el anterior
```

Funciones: `xpForLevel(n)`, `levelForXp(xp)`, `xpInCurrentLevel(xp)`, `xpForNextLevel(level)`, `nightBonus(score, bestStreak)`.

---

## AquariumBackground / AquariumCreatures (commonMain/presentation/aquarium/)

**`AquariumBackground`** — siempre visible. Dibuja en Canvas:
- Fondo degradado oceánico (OceanDeep → OceanMid → OceanLight)
- Plantas con oscilación animada (`swayAngle` infinito)
- Burbujas con trayectoria vertical + wobble horizontal

**`AquariumCreatures`** — visible cuando hay criaturas desbloqueadas. Dibuja sobre el contenido con `drawWithContent`:
- Filtra `allCreatures` por `unlockLevel <= currentLevel` de cada categoría
- Criaturas móviles: nadan de lado a lado con oscilación vertical `sin()`
- Criaturas fijas (FLORA, MOLLUSK): posición fija en zona inferior
- `drawEmoji(emoji, x, y, sizeSp, mirrored)` — expect/actual

---

## NightSummaryScheduler (commonMain / interfaz)

```kotlin
interface NightSummaryScheduler {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    fun scheduleWorker(time: LocalTime)
}
```

| Plataforma | Implementación |
|---|---|
| androidMain | DataStore + WorkManager |
| iosMain | stub vacío, devuelve `flowOf(LocalTime(23,30))` |

---

## UserPreferencesRepository (commonMain / interfaz)

```kotlin
interface UserPreferencesRepository {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    suspend fun getPendingUnlocks(): List<String>
    suspend fun setPendingUnlocks(emojis: List<String>)
    suspend fun getLastDismissedSummaryDate(): LocalDate?
    suspend fun setLastDismissedSummaryDate(date: LocalDate)
    fun hasCompletedOnboarding(): Flow<Boolean>
    suspend fun setOnboardingCompleted()
}
```

Implementación androidMain: DataStore Preferences. Hora/minuto como `Int` separados, unlocks como String con separador `|`, dismissed_summary_date como String ISO, onboarding como `Boolean`. Valor por defecto hora: 23:30.

---

## Flujo de desbloqueo de criaturas

```
toggleTaskCompleted / addNightBonus
    → EcosystemProcessor detecta nivel cruzado
    → devuelve List<CreatureSpec> nuevas
    → MainViewModel: añade a pendingUnlocks en UiState
    → (caso nocturno): persiste emojis en DataStore
    → al arrancar: checkPendingUnlocks() lee DataStore → añade a UiState

MainScreen:
    → pendingSummary primero (si existe y no fue descartado)
    → pendingUnlocks.first() → diálogo con emoji + campo nombre
    → confirmUnlock(spec, nickname) → guarda MarineCreature → drop(1)
    → siguiente unlock en cola
```

---

## NightSummaryWorker (androidMain)

`CoroutineWorker` de WorkManager. Se programa como `OneTimeWorkRequest` con delay hasta la próxima ocurrencia de la hora configurada. `ExistingWorkPolicy.REPLACE` para reemplazar si cambia la hora.

---

## Pantalla principal — estructura de capas

```
Box (fillMaxSize, pointerInput gestos)
 ├── AquariumBackground()
 ├── AquariumCreatures(...)
 ├── when(showAquarium)
 │    ├── true  → botón cerrar (FAB ✕)
 │    └── false → Column
 │                 ├── MainHeader (fijo)
 │                 │    ├── Fila: 🌊 Riptide + ⟳ + 📅 + ➕ + ☰
 │                 │    └── WeekCalendar
 │                 └── MainContent (scrollable)
 ├── AlertDialog contextMenu
 ├── AlertDialog deletingRecurring
 ├── AlertDialog editingScopeTask    ← diálogo scope edición recurrente
 ├── AlertDialog pendingSummary
 ├── AlertDialog pendingUnlocks
 ├── Drawer overlay + MainDrawer
 ├── DatePickerDialogWrapper         ← para ir a día concreto desde header
 ├── Dialog TaskFormSheet (nueva tarea)
 ├── Dialog TaskFormSheet (editar tarea)
 └── Dialog PostponeSheet
```

---

## Ordenación en pantalla principal

**Bloques:** por hora de inicio de su slot ese día. Bloques sin horario ese día van al final.

**Tareas:**
1. Con hora → ordenadas ascendente
2. Sin hora → orden de inserción
3. Completadas al final

**Tareas sin bloque:** sección propia "Sin bloque" con icono 📋, antes de los bloques con asignación.

---

## Gestos en MainScreen

Un único `detectDragGestures` en el Box raíz gestiona todo:
- **Vertical hacia abajo** → abre drawer
- **Horizontal** (solo si drawer cerrado) → cambia día

El Box del drawer tiene su propio `detectDragGestures`:
- **Vertical hacia arriba** → cierra drawer

Drawer usa `Animatable(drawerOffsetY)`. Overlay oscuro al abrir; toque fuera cierra.

---

## Edición de tareas recurrentes — flujo de scope

Al pulsar "Editar" en el menú contextual de una tarea con `sourceTaskId != null`:

1. Se muestra diálogo: "Solo esta ocurrencia" / "Esta y las futuras" / "Todas las ocurrencias"
2. "Solo esta ocurrencia" → abre `TaskFormSheet` normal → `updateOneTimeTask`
3. "Esta y las futuras" / "Todas las ocurrencias" → abre `TaskFormSheet` con `forceRecurring=true`
   → switch bloqueado en modo recurrente → `updateRecurringTask`

Convención interna: se añade sufijo `_future` o `_all` al `sourceTaskId` en el estado de UI para
transportar el scope. Se limpia antes de pasar al formulario y nunca se persiste en base de datos.

---

## MarineCategoryAssigner

Redistribuye categorías marinas automáticamente al crear, editar o eliminar bloques.

| Nº bloques | Distribución |
|---|---|
| 1 | 5 categorías (todas) |
| 2 | 3 + 3 |
| 3 | 2 + 2 + 2 |
| 4 | 2 + 1 + 1 + 1 |
| 5+ | 1 por bloque |

---

## Navegación

`NavHost` en `App.kt` con tres rutas:

```
ROUTE_MAIN         → MainScreen
ROUTE_BLOCK_CREATE → BlockFormScreen (nuevo)
ROUTE_BLOCK_EDIT   → BlockFormScreen (editar, recibe blockId)
```

`BlockFormViewModel` emite `BlockFormResult.Saved / .Deleted` → Navigation llama `mainViewModel.reload()` + `popBackStack()`.

---

## Convenciones

- IDs: UUID v4 con `generateUUID()` (expect/actual)
- Colores: hex string `"#RRGGBB"`, parseados en androidMain
- Fechas: siempre `LocalDate` / `LocalTime` / `LocalDateTime` de `kotlinx-datetime`
- Base de datos: `fallbackToDestructiveMigration()` durante desarrollo — migrar en v3
- Dependencias de plataforma (WorkManager, DataStore, Context): nunca en commonMain — siempre a través de interfaces
- Métricas internas (score, XP, totalExperience, currentLevel): **nunca visibles al usuario**
- Pantalla bloqueada en portrait (AndroidManifest, `screenOrientation="portrait"`)
