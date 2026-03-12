# Arquitectura — Riptide

## Patrón general

MVVM con repositorios. La UI no conoce Room, solo los ViewModels. Los repositorios abstraen el origen de datos para facilitar la migración a backend en v3 sin tocar ViewModels ni UI.

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

// androidMain
actual fun generateUUID() = UUID.randomUUID().toString()
actual fun currentDate() = Clock.System.todayIn(TimeZone.currentSystemDefault())
actual fun parseColor(hex: String) = Color(android.graphics.Color.parseColor(hex))

// iosMain
actual fun generateUUID() = NSUUID().UUIDString()
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

- `RiptideDatabase` — `fallbackToDestructiveMigration(true)` durante desarrollo. Cambiar a migraciones reales en v1.0.
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
| `block_streaks` | `BlockStreakEntity` |
| `ecosystem_states` | `EcosystemStateEntity` |
| `marine_creatures` | `MarineCreatureEntity` |

### DayTaskEntity — campos clave

`scheduleType` ("ONE_TIME" | "RECURRING"), `date` (String?), `time` (String?), `recurrence` (String? JSON), `status` (String), `completedAt` (String?), `postponedTo` (String?), `sourceTaskId` (String?), `blockId` (String? FK SET_NULL).

---

## ViewModelFactory (androidMain)

Android requiere factories para inyectar dependencias. Cada ViewModel tiene su factory que obtiene repositorios desde `DatabaseProvider`.

`MainViewModelFactory(context)` — construye `MainViewModel` con todos los repositorios + `RecurringTaskGenerator` + `MarineCategoryAssigner`.

---

## MainViewModel

Constructor recibe: `WorkBlockRepository`, `BlockCategoryRepository`, `DayTaskRepository`, `RecurringTaskDefRepository`, `RecurringTaskGenerator`, `MarineCategoryAssigner`.

Funciones públicas:
- `selectDate(date)` — cambia día seleccionado y recarga tareas
- `toggleTaskCompleted(task)` — alterna PENDING/COMPLETED
- `addOneTimeTask(title, blockId?, date, time?)` — crea tarea puntual
- `addRecurringTask(title, blockId, time, recurrence)` — crea `RecurringTaskDef` + genera instancias para los próximos 7 días
- `updateOneTimeTask(original, title, blockId?, date, time?)` — edita tarea puntual existente
- `deleteTask(task)` — elimina por ID (tareas no recurrentes)
- `deleteRecurringTaskInstance(task)` — elimina solo esta ocurrencia
- `deleteRecurringTaskFromDate(task)` — desactiva la def + elimina instancias PENDING/POSTPONED desde esta fecha
- `deleteRecurringTaskAll(task)` — desactiva la def + elimina todas las instancias
- `postponeTask(task, postponedTo)` — marca original como POSTPONED, crea nueva instancia PENDING en la nueva fecha
- `insertBlockAndReassign / updateBlockAndReassign / deleteBlockAndReassign` — CRUD de bloques con reasignación automática de categorías marinas
- `reload()` — recarga completa

---

## NightSummaryProcessor (commonMain)

Lógica pura de cierre de día. Se llama desde `NightSummaryWorker` (a la hora configurada) y desde `MainActivity.onCreate` (fallback si no se ejecutó por la noche).

```
processDay(date: LocalDate)
  1. Si ya existe DaySummary para esa fecha → no hacer nada
  2. Expira tareas PENDING del día → EXPIRED
  3. Calcula score = completadas / total
  4. Genera mensaje emocional según score
  5. Guarda DaySummary
```

---

## NightSummaryScheduler (commonMain / interfaz)

Abstrae la lectura/escritura de la hora del resumen nocturno y la planificación del worker, para que `MainScreen` y `MainDrawer` (commonMain) no dependan de androidMain.

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

El scheduler se construye en `MainActivity`, se pasa a `App` → `mainGraph` → `MainScreen` como parámetro.

---

## NightSummaryWorker (androidMain)

`CoroutineWorker` de WorkManager. Se programa como `OneTimeWorkRequest` con delay calculado hasta la próxima ocurrencia de la hora configurada. Usa `ExistingWorkPolicy.REPLACE` para reemplazar si se cambia la hora.

Se replanifica automáticamente al:
- Arrancar la app (`MainActivity.onCreate`)
- Cambiar la hora en el drawer (`onNightSummaryTimeChanged`)

---

## UserPreferencesRepository (commonMain / interfaz)

```kotlin
interface UserPreferencesRepository {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
}
```

Implementación androidMain: `UserPreferencesRepositoryImpl` usando DataStore Preferences. Almacena hora y minuto como `Int` separados. Valor por defecto: 23:30.

---

## Ordenación en pantalla principal

**Bloques:** por hora de inicio de su slot ese día de la semana (ISO `dayOfWeek`). Bloques sin horario ese día van al final.

**Tareas dentro de un bloque:**
1. Con hora → ordenadas por hora ascendente
2. Sin hora → orden de inserción
3. Completadas al final — misma sub-ordenación (hora primero, sin hora después)

**Tareas sin bloque:** sección propia "Sin bloque" con icono 📋, antes de los bloques con asignación.

**Horas del bloque en cabecera:** solo si el bloque tiene un `WeeklySlot` con `startTime` y `endTime` para ese día de la semana exacto.

---

## WeekCalendar — indicadores de progreso

Cada día muestra una barra de progreso animada de 28dp × 3dp bajo el número:
- Invisible si no hay tareas ese día
- Fondo semitransparente (pendientes) + fill azul claro (completadas)
- El día actual usa `TodayIndicator` (#7EC8E3) tanto en la inicial como en el fill de la barra
- La fracción se anima con `animateFloatAsState` (300ms)

Los datos se calculan en `MainContent` con un `remember(tasksByBlock)` que agrupa todas las tareas por fecha (`TaskSchedule.OneTime`) y se pasan a `WeekCalendar` como `Map<LocalDate, List<DayTask>>`.

---

## RecurringTaskGenerator

Genera instancias `DayTask` (como `TaskSchedule.OneTime`) a partir de `RecurringTaskDef` activos para los próximos N días (default 7), omitiendo días donde ya existe una instancia con ese `sourceTaskId`.

Se llama en `MainViewModel.init` y al crear una nueva tarea recurrente.

---

## MarineCategoryAssigner

Redistribuye categorías marinas automáticamente al crear, editar o eliminar bloques. El usuario nunca las ve ni las configura. La experiencia acumulada en `EcosystemState` se mantiene intacta al redistribuir.

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

`NightSummaryScheduler` se pasa como parámetro a través de `App` → `mainGraph` → `MainScreen`.

---

## Gestos en MainScreen

Un único `detectDragGestures` en el `Box` exterior gestiona todo:

- **Vertical hacia abajo** → abre drawer
- **Vertical hacia arriba** → cierra drawer
- **Horizontal** (solo si drawer cerrado) → cambia día

El drawer usa `Animatable(drawerOffsetY)` para la animación. Overlay oscuro semitransparente al abrir; toque fuera cierra.

---

## UI — Pantalla principal

Fondo gradiente oceánico: `OceanDeep #0A1628` → `OceanMid #1B3A6B` → `OceanLight #2E5F9E`.
Tarjetas frosted glass (blanco 20% opacidad, `RoundedCornerShape(12.dp)`).

**TaskCard** — pulsación larga abre menú contextual con: Editar, Posponer (si no está completada/expirada), Eliminar.

Al eliminar tarea recurrente (`sourceTaskId != null`) se muestra un segundo diálogo con 3 opciones: Solo esta ocurrencia / Esta y las futuras / Todas las ocurrencias.

**PostponeSheet** — bottom sheet para elegir nueva fecha y hora. Marca la tarea original como POSTPONED y crea una nueva instancia PENDING.

**TaskFormSheet** — bottom sheet para crear y editar tareas. Soporta `existingTask` para edición y `onDelete` para eliminar desde el propio formulario.

**MainDrawer** — sección AJUSTES al final con campo de hora del resumen nocturno (`DrawerTimeTextField`). Al cambiar la hora se persiste en DataStore y se replanifica el WorkManager.

---

## UI — BlockFormScreen

Campos: Nombre, Icono (emoji), Color (paleta fija 12 colores), Horario semanal.

Paleta: `#1A73E8`, `#E8711A`, `#34A853`, `#EA4335`, `#9C27B0`, `#00BCD4`, `#FF9800`, `#607D8B`, `#E91E63`, `#795548`, `#009688`, `#F5C842`.

`TimeTextField`: escribe 4 dígitos (ej: `1430` → `14:30`), validación rango 00:00–23:59.
Botón eliminar solo en modo edición (rojo semitransparente).

---

## Convenciones

- IDs: UUID v4 generados con `generateUUID()` (expect/actual)
- Colores: hex string `"#RRGGBB"`, parseados en androidMain con `android.graphics.Color.parseColor`
- Fechas: siempre `LocalDate` / `LocalTime` / `LocalDateTime` de `kotlinx-datetime`, nunca `java.util.*`
- Base de datos: `fallbackToDestructiveMigration()` durante desarrollo
- Dependencias de plataforma (WorkManager, DataStore, Context): nunca en commonMain directamente — siempre a través de interfaces o parámetros
