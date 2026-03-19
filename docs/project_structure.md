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
| `DayTask.kt` | `blockId` nullable, `sourceTaskId` para recurrentes, `hasBeenRewarded` para XP |
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
| `UserPreferencesRepository.kt` | nightSummaryTime (Flow), pendingUnlocks, **lastDismissedSummaryDate**, hasCompletedOnboarding |
| Resto | operaciones estándar |

### `domain/`

| Archivo | Qué hace |
|---|---|
| `MarineCategoryAssigner.kt` | Redistribuye entre categorías **desbloqueadas** (requiere `EcosystemStateRepository`) |
| `RecurringTaskGenerator.kt` | Genera instancias; soporta `time` nullable |
| `BlockStreakProcessor.kt` | Rachas por bloque |
| `NightSummaryProcessor.kt` | Recibe `summaryTime`; evalúa tareas completadas + PENDING con hora ≤ summaryTime |
| `EcosystemProcessor.kt` | XP a categorías + XP a criaturas individuales; requiere `MarineCreatureRepository` |
| `EcosystemLevelCalculator.kt` | Curva de niveles compartida para ecosistemas y criaturas |

### `presentation/aquarium/`

| Archivo | Qué hace |
|---|---|
| `AquariumBackground.kt` | Canvas: cielo dinámico por hora (7 periodos), superficie con olas animadas (cresta doble), fondo marino (arena con textura, 11 rocas 3 estilos), burbujas |
| `AquariumBounds.kt` | `SURFACE_FRACTION=0.08`, `FLOOR_FRACTION=0.88`, `surfaceY(h)`, `floorY(h)` — compartidas entre background y criaturas |
| `AquariumCreature.kt` | `CreatureSpec` (+`sizeMultiplier`, `instanceCount`), SwimZone, EasingType, 24 especies, `AquariumCreatures`, `CreatureFreezeState`, hit-testing, tempo warping, variación por instancia, múltiples instancias de flora Canvas, crustáceos en BOTTOM diferenciados |
| `CreatureRenderer.kt` | `CreatureRenderer` interface + `rendererFor(species)` dispatch + `CreatureIcon` @Composable (Canvas animado para flora, emoji fallback) |
| `CreatureDetailDialog.kt` | Dialog OceanMid: `CreatureIcon(80dp)`, nombre, nickname editable, XpBar sin números, fecha desbloqueo |
| `CreatureExtensions.kt` | `displayName` y `xpRequiredForLevel` compartidos entre dialogs |
| `EcosystemScreen.kt` | Pantalla "Mi ecosistema": botón ← retroceso, grid 3 col, `CreatureIcon(52dp)` en cards desbloqueadas, cards bloqueadas con barra progreso |
| `flora/BrainCoralRenderer.kt` | Canvas: domos con crestas, cluster multi-domo nivel 6+, colores coral/rosa |
| `flora/AnemoneRenderer.kt` | Canvas: tentáculos con `quadraticTo`, ondulación interna animada, 5→14 tentáculos según nivel |
| `flora/KelpRenderer.kt` | Canvas: tallos con hojas alternas, ondulación creciente, bosque multi-tallo nivel 6+ |

### `presentation/main/`

| Archivo | Qué hace |
|---|---|
| `MainUiState.kt` | + `creaturesData: List<MarineCreature>` |
| `MainViewModel.kt` | `loadDay` carga `creaturesData`; `toggleTaskCompleted` con `hasBeenRewarded` y lógica EXPIRED; `updateCreatureNickname` |
| `MainScreen.kt` | EXPIRED: ⌛ + checkbox completable; tap criatura → `CreatureDetailDialog` |
| `WeekCalendar.kt` | Excluye POSTPONED |
| `MainDrawer.kt` | BLOQUES + ECOSISTEMA (botón "Mi ecosistema") + AJUSTES; scroll interno con `LocalWindowInfo` |

### `presentation/components/`

| Archivo | Qué hace |
|---|---|
| `TimeInputField.kt` | Campo readonly, click abre `TimePickerDialogWrapper` |
| `DateInputField.kt` | Campo readonly, click abre `DatePickerDialogWrapper` |
| `InputFieldDialogs.kt` | Declaraciones `expect` de pickers |

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
| `DayTaskEntity` | + `hasBeenRewarded: Boolean` (v9) |
| `RecurringTaskDefEntity` | `time: String?` nullable |
| `EcosystemStateEntity` | + `isUnlocked: Boolean` |

### `data/local/dao/`

| DAO | Queries añadidas |
|---|---|
| `EcosystemStateDao` | `getAll()`, `getUnlocked()` |
| `MarineCreatureDao` | `getByCategory(category: String)` |

### `data/local/db/`

`RiptideDatabase` — **versión 9**. Migración real `MIGRATION_8_9`. Sin `fallbackToDestructiveMigration`.

### `presentation/`

| Archivo | Nota |
|---|---|
| `MainViewModelFactory.kt` | `EcosystemProcessor(ecosystemStateRepo, marineCreatureRepo)` |
| `BlockFormViewModelFactory.kt` | `MarineCategoryAssigner(workBlockRepo, blockCategoryRepo, ecosystemStateRepo)` |
| `MainActivity.kt` | `EcosystemProcessor` con `marineCreatureRepo` |
| `NightSummaryWorker.kt` | Lee `summaryTime` con `.first()`, lo pasa a `processDay` |
| `Navigation.kt` | + `ROUTE_ECOSYSTEM = "ecosystem"`; composable usa `MainViewModel` compartido |

### `presentation/components/`

| Archivo | Nota |
|---|---|
| `InputFieldDialogs.android.kt` | `TimePicker` con `TimePickerDefaults.colors()` explícitos; `DatePicker` con `DatePickerDefaults.colors()` |

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
 │                     freezeState, onCreatureTap)
 ├── when(showAquarium)
 │    ├── true  → FAB ✕
 │    └── false → Column
 │                 ├── MainHeader
 │                 │    ├── 🌊 Riptide + ⟳ + 📅 + ➕ + ☰
 │                 │    └── WeekCalendar
 │                 └── MainContent (LazyColumn)
 │                      ├── BlockSection (long press header → nueva tarea)
 │                      └── TaskCard (EXPIRED: ⌛ + checkbox; long press → menú contextual)
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
