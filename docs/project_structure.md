# Estructura del proyecto — Riptide

## Visión general

El proyecto usa **Kotlin Multiplatform con Compose Multiplatform**. Todo el código vive dentro de `composeApp/src/`, dividido en tres source sets:

- `commonMain` — código compartido Android + iOS (modelos, repositorios, ViewModels, UI)
- `androidMain` — exclusivo Android (Room, implementaciones, factories, navegación)
- `iosMain` — exclusivo iOS (entrypoint UIViewController, actuals)

---

## commonMain

Ruta base: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/`

| Archivo | Qué hace |
|---|---|
| `UuidGenerator.kt` | `expect fun generateUUID(): String` |
| `Serializers.kt` | `LocalTimeSerializer` — serializa `LocalTime` como ISO string para Room |

### `domain/model/`

| Archivo | Qué representa |
|---|---|
| `MarineCategory.kt` | Enum: FISH, FLORA, CRUSTACEAN, MOLLUSK, PELAGIC |
| `WorkBlock.kt` | Bloque de actividad. Contiene también `Recurrence` (sealed `@Serializable`) y `WeeklySlot` (`@Serializable`) |
| `TaskStatus.kt` | Enum: PENDING, COMPLETED, EXPIRED, POSTPONED |
| `TaskSchedule.kt` | Sealed class: `OneTime(date, time?)` / `Recurring(time, recurrence)` |
| `DayTask.kt` | Instancia de tarea para un día. `blockId` nullable, `sourceTaskId` para recurrentes |
| `RecurringTaskDef.kt` | Definición de tarea recurrente. Siempre tiene bloque |
| `DaySummary.kt` | Resumen del día: score interno + mensaje emocional |
| `BlockStreak.kt` | Racha de días consecutivos de un bloque |
| `EcosystemState.kt` | Estado del ecosistema por categoría marina |
| `MarineCreature.kt` | Criatura individual. Contiene también `CreatureSpecies` |
| `BlockCategory.kt` | Relación bloque ↔ categoría marina (asignación automática) |

### `domain/repository/`

| Archivo | Operaciones |
|---|---|
| `WorkBlockRepository.kt` | getAll, getById, insert, update, delete |
| `DayTaskRepository.kt` | getByDate, getByBlock, getById, insert, update, delete |
| `RecurringTaskDefRepository.kt` | getAll, getActive, getById, insert, update, delete |
| `BlockCategoryRepository.kt` | getByBlockId, insertAll, deleteByBlockId, deleteAll |
| `DaySummaryRepository.kt` | getByDate, insert |
| `EcosystemStateRepository.kt` | getByCategory, update |
| `MarineCreatureRepository.kt` | getByEcosystem, insert, update |

### `domain/`

| Archivo | Qué hace |
|---|---|
| `MarineCategoryAssigner.kt` | Redistribuye categorías marinas automáticamente al cambiar bloques |
| `RecurringTaskGenerator.kt` | Genera instancias `DayTask` a partir de `RecurringTaskDef` activos para los próximos N días |

### `presentation/main/`

| Archivo | Qué hace |
|---|---|
| `CurrentDate.kt` | `expect fun currentDate(): LocalDate` |
| `ParseColor.kt` | `expect fun parseColor(hex: String): Color` |
| `MainUiState.kt` | Estado de UI: fecha, bloques, tareas por bloque, loading, error |
| `MainViewModel.kt` | Carga datos, expone `StateFlow<MainUiState>`, maneja toda la lógica de tareas y bloques |
| `MainScreen.kt` | Composable raíz. Gestiona drawer, menú contextual, sheets de tarea y posponer |
| `WeekCalendar.kt` | Calendario semanal navegable con swipe |
| `MainDrawer.kt` | Drawer desde arriba con lista de bloques y acceso a crear tarea/bloque |

### `presentation/block/`

| Archivo | Qué hace |
|---|---|
| `BlockFormScreen.kt` | Formulario crear/editar/eliminar bloque |
| `BlockFormViewModel.kt` | Lógica del formulario. Emite `BlockFormResult.Saved` / `.Deleted` |

### `presentation/task/`

| Archivo | Qué hace |
|---|---|
| `TaskFormSheet.kt` | Bottom sheet crear/editar tareas puntuales y recurrentes. Soporta `existingTask` y `onDelete` |
| `PostponeSheet.kt` | Bottom sheet para elegir nueva fecha/hora al posponer |
| `TaskFormViewModel.kt` | Lógica del formulario de tarea |

---

## androidMain

Ruta base: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/`

| Archivo | Para qué sirve |
|---|---|
| `App.kt` | NavHost con las tres rutas de la app |
| `MainActivity.kt` | Punto de entrada. Inicializa DB, llama DataSeeder y arranca UI |
| `DataSeeder.kt` | Rellena BD si está vacía: 3 bloques (💼🏐💚) + 5 tareas para hoy |
| `UuidGenerator.android.kt` | `actual fun generateUUID()` usando `UUID.randomUUID()` |

### `data/local/entity/`

| Archivo | Tabla |
|---|---|
| `WorkBlockEntity.kt` | `work_blocks` |
| `BlockCategoryEntity.kt` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity.kt` | `day_tasks` — scheduleType, date?, time?, recurrence? JSON, status, completedAt?, postponedTo?, sourceTaskId?, blockId? FK SET_NULL |
| `RecurringTaskDefEntity.kt` | `recurring_task_defs` — blockId FK CASCADE, recurrence JSON |
| `DaySummaryEntity.kt` | `day_summaries` |
| `BlockStreakEntity.kt` | `block_streaks` |
| `EcosystemStateEntity.kt` | `ecosystem_states` |
| `MarineCreatureEntity.kt` | `marine_creatures` |

### `data/local/dao/`

Un DAO por entity. Operaciones estándar con `@Query`, `@Insert`, `@Update`, `@Delete`.

### `data/local/db/`

| Archivo | Para qué sirve |
|---|---|
| `RiptideDatabase.kt` | Clase Room principal. Versión 6. `fallbackToDestructiveMigration(true)` en desarrollo |
| `DatabaseProvider.kt` | Singleton que construye `RiptideDatabase` |

### `data/local/mapper/`

Un mapper por entidad. Convierte dominio ↔ entity. Los mappers de `DayTask` y `RecurringTaskDef` usan `Json.encodeToString<Recurrence>(...)` con tipo explícito.

### `data/repository/`

Implementaciones de Room de todas las interfaces de repositorio de commonMain.

### `presentation/`

| Archivo | Para qué sirve |
|---|---|
| `Navigation.kt` | Define rutas y composable de navegación. Conecta `BlockFormResult` con `mainViewModel.reload()` |
| `main/MainViewModelFactory.kt` | Construye `MainViewModel` con todos los repositorios + generator + assigner |
| `main/CurrentDate.android.kt` | `actual fun currentDate()` |
| `main/ParseColor.android.kt` | `actual fun parseColor(hex)` usando `android.graphics.Color.parseColor` |
| `block/BlockFormViewModelFactory.kt` | Construye `BlockFormViewModel` |
| `task/TaskFormViewModelFactory.kt` | Construye `TaskFormViewModel` |

---

## iosMain

Ruta base: `composeApp/src/iosMain/kotlin/com/mnebot/riptide/`

| Archivo | Para qué sirve |
|---|---|
| `MainViewController.kt` | Punto de entrada iOS |
| `UuidGenerator.ios.kt` | `actual fun generateUUID()` usando `NSUUID` |
| `presentation/main/CurrentDate.ios.kt` | `actual fun currentDate()` |
| `presentation/main/ParseColor.ios.kt` | `actual fun parseColor(hex)` parseando hex manualmente |

---

## Diagrama de capas

```
commonMain                              androidMain
────────────────────────────────────────────────────────────
App.kt (NavHost)

presentation/task/                      presentation/task/
  TaskFormSheet, PostponeSheet            TaskFormViewModelFactory
  TaskFormViewModel

presentation/block/                     presentation/block/
  BlockFormScreen                         BlockFormViewModelFactory
  BlockFormViewModel

presentation/main/                      presentation/main/
  MainScreen, WeekCalendar                MainViewModelFactory
  MainDrawer, MainViewModel               CurrentDate.android.kt
  MainUiState                             ParseColor.android.kt
  CurrentDate (expect)
  ParseColor (expect)

domain/model/                           data/local/entity/
  WorkBlock, DayTask,         ←──→        WorkBlockEntity,
  RecurringTaskDef...         mapper      DayTaskEntity...
  (Kotlin puro)                           (Room / SQLite)

domain/repository/                      data/local/dao/
  interfaces               ←── impl ──   DAOs (Room queries)

domain/                                 data/local/db/
  MarineCategoryAssigner                  RiptideDatabase v6
  RecurringTaskGenerator                  DatabaseProvider

                                        data/repository/
                                          implementaciones Room

                                        DataSeeder.kt
                                        MainActivity.kt
```