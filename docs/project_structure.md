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
| `MarineCreature.kt` | `experience` + `creatureLevel` individuales + `CreatureSpecies` (40 especies) |
| `CreatureRarity.kt` | Enum: COMMON(0.40), UNCOMMON(0.30), RARE(0.20), EPIC(0.08), LEGENDARY(0.02) con pesos |
| `PendingLootbox.kt` | `data class PendingLootbox(category, categoryLevel)` — lootbox pendiente de abrir |
| `BlockCategory.kt` | Relación bloque ↔ categoría (automática) |

### `domain/repository/`

| Archivo | Operaciones clave |
|---|---|
| `EcosystemStateRepository.kt` | getByCategory, getAll, **getUnlocked**, insert, update |
| `MarineCreatureRepository.kt` | getByEcosystem, **getByCategory**, insert, update |
| `UserPreferencesRepository.kt` | nightSummaryTime (Flow), **pendingLootboxes**, pendingUnlocks (legacy), **lastDismissedSummaryDate**, hasCompletedOnboarding |
| Resto | operaciones estándar |

### `domain/`

| Archivo | Qué hace |
|---|---|
| `MarineCategoryAssigner.kt` | Redistribuye entre categorías **desbloqueadas** (requiere `EcosystemStateRepository`) |
| `RecurringTaskGenerator.kt` | Genera instancias; soporta `time` nullable |
| `BlockStreakProcessor.kt` | Rachas por bloque |
| `NightSummaryProcessor.kt` | Recibe `summaryTime`; evalúa tareas completadas + PENDING con hora ≤ summaryTime |
| `EcosystemProcessor.kt` | XP a categorías + XP a criaturas + lootbox detection + overflow; requiere `MarineCreatureRepository` |
| `LootboxResolver.kt` | Selección weighted-random de especie al abrir lootbox; requiere `MarineCreatureRepository` |
| `EcosystemLevelCalculator.kt` | Curva de niveles compartida para ecosistemas y criaturas |

### `presentation/aquarium/`

| Archivo | Qué hace |
|---|---|
| `AquariumBackground.kt` | Canvas: cielo dinámico por hora (7 periodos), superficie con olas animadas (cresta doble), fondo marino (arena con textura, 11 rocas 3 estilos), burbujas |
| `AquariumBounds.kt` | `SURFACE_FRACTION=0.08`, `FLOOR_FRACTION=0.88`, `surfaceY(h)`, `floorY(h)` — compartidas entre background y criaturas |
| `AquariumCreature.kt` | `CreatureSpec` (+`rarity`, `sizeMultiplier`, `instanceCount`), SwimZone, EasingType, 40 especies, `CATEGORY_UNLOCK_LEVELS`, `specBySpecies`, `AquariumCreatures`, `CreatureFreezeState`, hit-testing, tempo warping, variación por instancia, múltiples instancias de flora Canvas, crustáceos en BOTTOM diferenciados |
| `CreatureRenderer.kt` | `CreatureRenderer` interface + `rendererFor(species)` dispatch + `CreatureIcon` @Composable (Canvas animado para flora, emoji fallback) |
| `CreatureDetailDialog.kt` | Dialog OceanMid: `CreatureIcon(80dp)`, nombre, badge rareza, nickname editable, XpBar con nivel numérico, fecha desbloqueo |
| `CreatureExtensions.kt` | `displayName` y `xpRequiredForLevel` compartidos entre dialogs |
| `EcosystemScreen.kt` | Pantalla "Mi ecosistema": botón ← retroceso, grid 3 col por rareza, barra progreso por categoría, badges rareza, niveles numéricos, cards bloqueadas con emoji 10% opacity |
| `flora/BrainCoralRenderer.kt` | Canvas: domos con crestas, cluster multi-domo nivel 6+, colores coral/rosa |
| `flora/AnemoneRenderer.kt` | Canvas: tentáculos con `quadraticTo`, ondulación interna animada, 5→14 tentáculos según nivel |
| `flora/KelpRenderer.kt` | Canvas: tallos con hojas alternas, ondulación creciente, bosque multi-tallo nivel 6+ |
| `flora/PosidoniaRenderer.kt` | Canvas: cintas de hierba marina ancladas, oscilación por fase por hoja, matte de fibras nivel 5+ |
| `flora/FanCoralRenderer.kt` | Canvas: árbol bifurcado recursivo, balanceo suave, malla nivel 3+, pólipos blancos nivel 5+ |
| `fauna/MantaRayRenderer.kt` | Canvas: aleteo con onda progresiva, aletas cefálicas, cola ondulante, manchas ventrales nivel 3+ |
| `fauna/SurgeonfishRenderer.kt` | Canvas: cuerpo azul cobalto, cola amarilla en media luna, máscara negra, escalpelo blanco |
| `fauna/LionfishRenderer.kt` | Canvas: 11-13 espinas dorsales en abanico con membrana, aletas pectorales enormes, cuerpo rayado |
| `fauna/SunfishRenderer.kt` | Canvas: disco circular, aletas dorsal/ventral enormes, clavus ondulado, parches de piel nivel 3+ |
| `fauna/HammerheadRenderer.kt` | Canvas: cabeza en T con ojos en los extremos, contrasombreado, hendiduras branquiales nivel 3+ |
| `fauna/BarracudaRenderer.kt` | Canvas: cuerpo 3× elongado, mandíbula prominente con dientes, dos aletas dorsales, cola bifurcada |
| `fauna/ManateeRenderer.kt` | Canvas: cuerpo redondeado, cola paleta horizontal, aletas frontales, arrugas, bigotes |
| `fauna/SpiderCrabRenderer.kt` | Canvas: caparazón pequeño, 10 patas larguísimas articuladas con animación por fase |
| `fauna/CuttlefishRenderer.kt` | Canvas: falda de aletas ondulantes a lo largo del cuerpo, pupila en W, 8 brazos + 2 tentáculos |
| `fauna/BlueRingedOctopusRenderer.kt` | Canvas: 8 brazos con ventosas, 16 anillos azules eléctricos pulsantes con `sin(t)` |
| `fauna/SeaUrchinRenderer.kt` | Canvas: semiesfera con ~30 espinas radiales de longitud variable, 5 bandas de simetría |
| `fauna/BarnacleRenderer.kt` | Canvas: cluster de 7 volcanes/conos con placas, cirros alimenticios animados nivel 3+ |

### `presentation/main/`

| Archivo | Qué hace |
|---|---|
| `MainUiState.kt` | + `creaturesData`, `pendingLootboxes`, `revealedSpecies` |
| `MainViewModel.kt` | `loadDay` carga `creaturesData`; `toggleTaskCompleted` con lootboxes; `openLootbox`, `confirmUnlock`, `dismissLootbox`; `updateCreatureNickname` |
| `MainScreen.kt` | EXPIRED: ⌛ + checkbox; tap criatura → `CreatureDetailDialog`; diálogo lootbox bifásico (cerrada→abierta) |
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
| `MainViewModelFactory.kt` | `EcosystemProcessor(...)` + `LootboxResolver(marineCreatureRepo)` |
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
 ├── Dialog pendingLootboxes (bifásico: cerrada→abierta)
 ├── CreatureDetailDialog (tap criatura)
 ├── Drawer (pointerInput propio para cerrar)
 │    └── MainDrawer(onNavigateToEcosystem)
 ├── DatePickerDialogWrapper
 ├── Dialog TaskFormSheet (nueva / editar)
 └── Dialog PostponeSheet
```
