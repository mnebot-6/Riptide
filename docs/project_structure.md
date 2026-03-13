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
| `Serializers.kt` | `LocalTimeSerializer` — serializa `LocalTime` como ISO string |
| `NightSummaryScheduler.kt` | Interfaz: `getNightSummaryTime`, `setNightSummaryTime`, `scheduleWorker` |

### `domain/model/`

| Archivo | Qué representa |
|---|---|
| `MarineCategory.kt` | Enum: FISH, FLORA, CRUSTACEAN, MOLLUSK, PELAGIC |
| `WorkBlock.kt` | Bloque de actividad + `Recurrence` (sealed `@Serializable`) + `WeeklySlot` |
| `TaskStatus.kt` | Enum: PENDING, COMPLETED, EXPIRED, POSTPONED |
| `TaskSchedule.kt` | Sealed class: `OneTime(date, time?)` / `Recurring(time, recurrence)` |
| `DayTask.kt` | Instancia de tarea. `blockId` nullable, `sourceTaskId` para recurrentes |
| `RecurringTaskDef.kt` | Definición de tarea recurrente. Siempre tiene bloque |
| `DaySummary.kt` | Resumen del día: score interno + mensaje emocional |
| `BlockStreak.kt` | Racha de días consecutivos. PK natural = `blockId` |
| `EcosystemState.kt` | Estado XP + nivel por categoría marina |
| `MarineCreature.kt` | Criatura individual con nickname + `CreatureSpecies` enum |
| `BlockCategory.kt` | Relación bloque ↔ categoría marina (asignación automática) |

### `domain/repository/`

| Archivo | Operaciones |
|---|---|
| `WorkBlockRepository.kt` | getAll, getById, insert, update, delete |
| `DayTaskRepository.kt` | getByDate, getByBlock, getById, insert, update, updateStatus, delete, deleteBySourceId, deleteBySourceIdFromDate |
| `RecurringTaskDefRepository.kt` | getAll, getActive, getById, insert, update, delete |
| `BlockCategoryRepository.kt` | getCategoriesForBlocks, setCategories |
| `BlockStreakRepository.kt` | getByBlockId, insert, update |
| `DaySummaryRepository.kt` | getByDate, insert |
| `EcosystemStateRepository.kt` | getByCategory, insert, update |
| `MarineCreatureRepository.kt` | getByEcosystem, insert, update |
| `UserPreferencesRepository.kt` | getNightSummaryTime (Flow), setNightSummaryTime, getPendingUnlocks, setPendingUnlocks, hasCompletedOnboarding (Flow), setOnboardingCompleted |

### `domain/`

| Archivo | Qué hace |
|---|---|
| `MarineCategoryAssigner.kt` | Redistribuye categorías marinas al cambiar bloques |
| `RecurringTaskGenerator.kt` | Genera instancias `DayTask` a partir de defs activos para los próximos N días |
| `BlockStreakProcessor.kt` | Calcula y persiste rachas por bloque |
| `NightSummaryProcessor.kt` | Expira pendientes, calcula score, XP nocturna, persiste unlocks, guarda DaySummary |
| `EcosystemProcessor.kt` | Gestiona XP; devuelve `List<CreatureSpec>` desbloqueadas |
| `EcosystemLevelCalculator.kt` | Curva de niveles, funciones de XP, bonus nocturno |

### `presentation/aquarium/`

| Archivo | Qué hace |
|---|---|
| `AquariumBackground.kt` | Canvas: fondo degradado + plantas animadas + burbujas |
| `AquariumCreature.kt` | `CreatureSpec`, `allCreatures`, `AquariumCreatures` composable + `expect fun DrawScope.drawEmoji(...)` |

### `presentation/components/`

| Archivo | Qué hace |
|---|---|
| `TimeInputField.kt` | Input hora estandarizado. `BasicTextField` invisible + overlay `HH:mm`. Params: `value`, `onValueChange`, `nullable`, `compact`, `showPickerIcon`. `expect fun TimePickerDialogWrapper` |
| `DateInputField.kt` | Input fecha estandarizado. `BasicTextField` invisible + overlay `YYYY-MM-DD`. Dígitos rellenan desde la derecha sobre fecha de hoy. `expect fun DatePickerDialogWrapper` |

### `presentation/main/`

| Archivo | Qué hace |
|---|---|
| `CurrentDate.kt` | `expect fun currentDate(): LocalDate` |
| `ParseColor.kt` | `expect fun parseColor(hex: String): Color` |
| `MainUiState.kt` | Estado de UI: fecha, bloques, tareas, streaks, ecosistema, pendingSummary, pendingUnlocks |
| `MainViewModel.kt` | Toda la lógica: tareas, bloques, XP, desbloqueos, resumen nocturno |
| `MainScreen.kt` | Composable raíz. Capas: AquariumBackground → AquariumCreatures → contenido → diálogos |
| `WeekCalendar.kt` | Calendario semanal navegable con barra de progreso animada |
| `MainDrawer.kt` | Drawer desde arriba: TAREAS + BLOQUES + AJUSTES (hora con `TimeInputField`) |

### `presentation/block/`

| Archivo | Qué hace |
|---|---|
| `BlockFormScreen.kt` | Formulario crear/editar/eliminar bloque. Usa `TimeInputField(compact=true)` |
| `BlockFormViewModel.kt` | Emite `BlockFormResult.Saved` / `.Deleted` |

### `presentation/task/`

| Archivo | Qué hace |
|---|---|
| `TaskFormSheet.kt` | Bottom sheet crear/editar tareas. Usa `DateInputField` + `TimeInputField` |
| `PostponeSheet.kt` | Bottom sheet posponer tarea. Usa `DateInputField` + `TimeInputField` |
| `TaskFormViewModel.kt` | Lógica del formulario de tarea |

---

## androidMain

Ruta base: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/`

| Archivo | Para qué sirve |
|---|---|
| `App.kt` | NavHost con las tres rutas |
| `MainActivity.kt` | Punto de entrada. Inicializa DB, DataSeeder, NightSummaryProcessor fallback, WorkManager |
| `DataSeeder.kt` | `seedIfEmpty(db, assigner)` — 3 bloques genéricos + 4 tareas para hoy; sin XP; solo si BD vacía |
| `NightSummaryWorker.kt` | CoroutineWorker WorkManager |
| `NightSummarySchedulerImpl.android.kt` | DataStore + WorkManager |
| `UuidGenerator.android.kt` | `actual fun generateUUID()` |

### `data/local/entity/`

| Archivo | Tabla |
|---|---|
| `WorkBlockEntity.kt` | `work_blocks` |
| `BlockCategoryEntity.kt` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity.kt` | `day_tasks` |
| `RecurringTaskDefEntity.kt` | `recurring_task_defs` |
| `DaySummaryEntity.kt` | `day_summaries` |
| `BlockStreakEntity.kt` | `block_streaks` — PK `blockId` |
| `EcosystemStateEntity.kt` | `ecosystem_states` |
| `MarineCreatureEntity.kt` | `marine_creatures` |

### `data/local/dao/`

Un DAO por entity. Operaciones estándar con `@Query`, `@Insert(onConflict = REPLACE)`, `@Update`, `@Delete`.

### `data/local/db/`

| Archivo | Para qué sirve |
|---|---|
| `RiptideDatabase.kt` | Clase Room principal. Versión 6. `fallbackToDestructiveMigration(true)` |
| `DatabaseProvider.kt` | Singleton lazy |

### `data/local/mapper/`

Un mapper por entidad. Convierte dominio ↔ entity. Usan `Json.encodeToString<Recurrence>(...)` con tipo explícito.

### `data/repository/`

| Archivo | Implementa |
|---|---|
| `WorkBlockRepositoryImpl.kt` | `WorkBlockRepository` |
| `BlockCategoryRepositoryImpl.kt` | `BlockCategoryRepository` |
| `DayTaskRepositoryImpl.kt` | `DayTaskRepository` |
| `RecurringTaskDefRepositoryImpl.kt` | `RecurringTaskDefRepository` |
| `BlockStreakRepositoryImpl.kt` | `BlockStreakRepository` |
| `DaySummaryRepositoryImpl.kt` | `DaySummaryRepository` |
| `EcosystemStateRepositoryImpl.kt` | `EcosystemStateRepository` |
| `MarineCreatureRepositoryImpl.kt` | `MarineCreatureRepository` |
| `UserPreferencesRepositoryImpl.kt` | `UserPreferencesRepository` — DataStore: hora resumen, pending unlocks, hasCompletedOnboarding |

### `presentation/`

| Archivo | Para qué sirve |
|---|---|
| `Navigation.kt` | Define rutas, conecta `BlockFormResult` con `mainViewModel.reload()` |
| `main/MainViewModelFactory.kt` | Construye `MainViewModel` con todos los repositorios |
| `main/CurrentDate.android.kt` | `actual fun currentDate()` |
| `main/ParseColor.android.kt` | `actual fun parseColor(hex)` |
| `aquarium/AquariumCreature.android.kt` | `actual fun DrawScope.drawEmoji(...)` con nativeCanvas |
| `components/InputFieldDialogs.android.kt` | `actual TimePickerDialogWrapper` + `actual DatePickerDialogWrapper` — Dialog propio con estética marina (OceanMid + Accent #7EC8E3) |
| `block/BlockFormViewModelFactory.kt` | Construye `BlockFormViewModel` |
| `task/TaskFormViewModelFactory.kt` | Construye `TaskFormViewModel` |

---

## iosMain

Ruta base: `composeApp/src/iosMain/kotlin/com/mnebot/riptide/`

| Archivo | Para qué sirve |
|---|---|
| `MainViewController.kt` | Punto de entrada iOS |
| `NightSummarySchedulerImpl.ios.kt` | Stub vacío, devuelve `flowOf(LocalTime(23,30))` |
| `UuidGenerator.ios.kt` | `actual fun generateUUID()` usando `NSUUID` |
| `presentation/main/CurrentDate.ios.kt` | `actual fun currentDate()` |
| `presentation/main/ParseColor.ios.kt` | `actual fun parseColor(hex)` parseando hex manualmente |
| `presentation/aquarium/AquariumCreature.ios.kt` | `actual fun DrawScope.drawEmoji(...)` — pendiente arreglar en v3 |
| `presentation/components/InputFieldDialogs.ios.kt` | `actual TimePickerDialogWrapper` + `actual DatePickerDialogWrapper` — stubs que llaman `onDismiss()` (pendiente en v3) |

---

## Capas del Box raíz en MainScreen

```
Box (fillMaxSize, pointerInput gestos)
 ├── AquariumBackground()
 ├── AquariumCreatures(...)
 ├── when(showAquarium)
 │    ├── true  → botón cerrar (FAB ✕)
 │    └── false → MainContent(...)
 ├── AlertDialog contextMenu
 ├── AlertDialog deletingRecurring
 ├── AlertDialog editingRecurringScope     ← diálogo "solo esta / todas las futuras"
 ├── AlertDialog pendingSummary
 ├── AlertDialog pendingUnlocks
 ├── Drawer overlay + MainDrawer
 ├── Dialog TaskFormSheet
 └── Dialog PostponeSheet
```
