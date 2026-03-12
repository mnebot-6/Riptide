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
- `deleteTask(task)` — elimina por ID
- `postponeTask(task, postponedTo)` — marca original como POSTPONED, crea nueva instancia PENDING en la nueva fecha
- `insertBlockAndReassign / updateBlockAndReassign / deleteBlockAndReassign` — CRUD de bloques con reasignación automática de categorías marinas
- `reload()` — recarga completa

---

## Ordenación en pantalla principal

**Bloques:** por hora de inicio de su slot ese día de la semana (ISO `dayOfWeek`). Bloques sin horario ese día van al final.

**Tareas dentro de un bloque:**
1. Con hora → ordenadas por hora ascendente
2. Sin hora → orden de inserción
3. Completadas al final — misma sub-ordenación (hora primero, sin hora después)

**Horas del bloque en cabecera:** solo si el bloque tiene un `WeeklySlot` con `startTime` y `endTime` para ese día de la semana exacto.

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

**PostponeSheet** — bottom sheet para elegir nueva fecha y hora. Marca la tarea original como POSTPONED y crea una nueva instancia PENDING.

**TaskFormSheet** — bottom sheet para crear y editar tareas. Soporta `existingTask` para edición y `onDelete` para eliminar desde el propio formulario.

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