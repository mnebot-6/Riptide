# Estructura del proyecto — Riptide

## Visión general

Kotlin Multiplatform con Compose Multiplatform. Todo el código vive en `composeApp/src/`:

- `commonMain` — modelos, repositorios, ViewModels, UI compartida
- `androidMain` — Room, implementaciones, factories, navegación
- `iosMain` — entrypoint, actuals (parcialmente pendientes)

---

## commonMain

### `domain/model/`

| Archivo | Qué representa |
|---|---|
| `MarineCategory.kt` | Enum con `isUnlockedByDefault`. 9 categorías: 5 base + CEPHALOPOD/REPTILE/MAMMAL/DECORATION |
| `WorkBlock.kt` | Bloque + `Recurrence` sealed `@Serializable` + `WeeklySlot` |
| `TaskStatus.kt` | PENDING, COMPLETED, EXPIRED, POSTPONED |
| `TaskSchedule.kt` | `OneTime(date, time?)` / `Recurring(time, recurrence)` |
| `DayTask.kt` | `blockId` nullable, `sourceTaskId` para recurrentes |
| `RecurringTaskDef.kt` | `time: LocalTime?` nullable |
| `DaySummary.kt` | Score interno + mensaje visible |
| `BlockStreak.kt` | PK natural = `blockId` |
| `EcosystemState.kt` | XP + nivel + `isUnlocked` por categoría |
| `MarineCreature.kt` | `experience` + `creatureLevel` individuales + `CreatureSpecies` (24 especies) |
| `BlockCategory.kt` | Relación bloque ↔ categoría (automática) |

### `domain/repository/`

| Archivo | Operaciones clave |
|---|---|
| `EcosystemStateRepository.kt` | getByCategory, getAll, **getUnlocked**, insert, update |
| `MarineCreatureRepository.kt` | getByEcosystem, **getByCategory**, insert, update |
| `UserPreferencesRepository.kt` | nightSummaryTime, pendingUnlocks, **lastDismissedSummaryDate**, hasCompletedOnboarding |
| Resto | operaciones estándar |

### `domain/`

| Archivo | Qué hace |
|---|---|
| `MarineCategoryAssigner.kt` | Redistribuye entre categorías **desbloqueadas** (requiere `EcosystemStateRepository`) |
| `RecurringTaskGenerator.kt` | Genera instancias; soporta `time` nullable |
| `BlockStreakProcessor.kt` | Rachas por bloque |
| `NightSummaryProcessor.kt` | Evaluación selectiva; excluye POSTPONED y futuras no completadas |
| `EcosystemProcessor.kt` | XP a categorías + XP a criaturas individuales; requiere `MarineCreatureRepository` |
| `EcosystemLevelCalculator.kt` | Curva de niveles compartida para ecosistemas y criaturas |

### `presentation/aquarium/`

| Archivo | Qué hace |
|---|---|
| `AquariumBackground.kt` | Canvas: degradado + plantas + burbujas |
| `AquariumCreature.kt` | `CreatureSpec` (SwimZone, EasingType, personalYFraction, verticalCoupling, microWobble, xErraticness, fixedWobbleScale), `allCreatures` (24), `AquariumCreatures`, `CreatureFreezeState`, hit-testing por posición real de frame |
| `CreatureDetailDialog.kt` | Dialog OceanMid: emoji, nombre, nickname editable, XpBar sin números, fecha desbloqueo |
| `CreatureExtensions.kt` | `displayName` y `xpRequiredForLevel` compartidos entre dialogs |
| `EcosystemScreen.kt` | Pantalla "Mi ecosistema": grid 3 col por categoría, cards desbloqueadas/bloqueadas, IntrinsicSize.Max, CreatureDetailDialog |

### `presentation/main/`

| Archivo | Qué hace |
|---|---|
| `MainUiState.kt` | + `creaturesData: List<MarineCreature>` |
| `MainViewModel.kt` | `loadDay` carga `creaturesData`; + `updateCreatureNickname` |
| `MainScreen.kt` | Long press header → `onBlockHeaderLongPress`; tap criatura → `CreatureDetailDialog` |
| `WeekCalendar.kt` | Excluye POSTPONED |
| `MainDrawer.kt` | BLOQUES + ECOSISTEMA (botón "Mi ecosistema") + AJUSTES; scroll interno con `LocalWindowInfo` |

### `presentation/task/`

| Archivo | Qué hace |
|---|---|
| `TaskFormSheet.kt` | `initialBlockId` para preseleccionar bloque; `forceRecurring`; precargar días/hora de `existingDef` |
| `PostponeSheet.kt` | Hora opcional. `onPostpone` llama a `postponingTask = null` tras confirmar. |

---

## androidMain

### `data/local/entity/`

| Entity | Cambios relevantes |
|---|---|
| `RecurringTaskDefEntity` | `time: String?` nullable |
| `EcosystemStateEntity` | + `isUnlocked: Boolean` |

### `data/local/dao/`

| DAO | Queries añadidas |
|---|---|
| `EcosystemStateDao` | `getAll()`, `getUnlocked()` |
| `MarineCreatureDao` | `getByCategory(category: String)` |

### `data/local/db/`

`RiptideDatabase` — **versión 8**. `fallbackToDestructiveMigration(true)`.

### `presentation/`

| Archivo | Nota |
|---|---|
| `MainViewModelFactory.kt` | `EcosystemProcessor(ecosystemStateRepo, marineCreatureRepo)` |
| `BlockFormViewModelFactory.kt` | `MarineCategoryAssigner(workBlockRepo, blockCategoryRepo, ecosystemStateRepo)` |
| `MainActivity.kt` | `EcosystemProcessor` con `marineCreatureRepo` |
| `NightSummaryWorker.kt` | `EcosystemProcessor` con `marineCreatureRepo` |
| `Navigation.kt` | + `ROUTE_ECOSYSTEM = "ecosystem"`; composable usa `MainViewModel` compartido |

### `DataSeeder.kt`

5 bloques genéricos. **Orden crítico**:
1. Crear `EcosystemState` para las 5 categorías base (`isUnlocked=true`)
2. Insertar bloques
3. `assigner.reassign()`
4. Insertar tareas

---

## iosMain

Todo pendiente para v6 excepto `UuidGenerator`, `CurrentDate` y `ParseColor`.

---

## Capas del Box raíz en MainScreen

```
Box (pointerInput gestos)
 ├── AquariumBackground()
 ├── AquariumCreatures(ecosystemByCategory, creatureLevelBySpecies, creaturesData,
 │                     freezeState, onCreatureLongPress)
 ├── when(showAquarium)
 │    ├── true  → FAB ✕
 │    └── false → Column
 │                 ├── MainHeader
 │                 │    ├── 🌊 Riptide + ⟳ + 📅 + ➕ + ☰
 │                 │    └── WeekCalendar
 │                 └── MainContent (LazyColumn)
 │                      ├── BlockSection (long press header → nueva tarea)
 │                      └── TaskCard (long press → menú contextual)
 ├── AlertDialog contextMenu
 ├── AlertDialog deletingRecurring
 ├── AlertDialog editingScopeTask
 ├── AlertDialog pendingSummary
 ├── AlertDialog pendingUnlocks
 ├── CreatureDetailDialog (tap criatura)
 ├── Drawer (pointerInput propio para cerrar)
 │    └── MainDrawer(onNavigateToEcosystem)
 ├── DatePickerDialogWrapper
 ├── Dialog TaskFormSheet (nueva / editar)
 └── Dialog PostponeSheet
```
