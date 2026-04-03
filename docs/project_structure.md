# Estructura del proyecto -- Riptide

## Vision general

Kotlin Multiplatform con Compose Multiplatform. Todo el codigo vive en `composeApp/src/`:

- `commonMain` -- modelos, repositorios, ViewModels, UI compartida
- `androidMain` -- Room, implementaciones, factories, navegacion, widget, wallpaper, sync
- `iosMain` -- entrypoint, actuals (parcialmente pendientes)
- `commonTest` -- tests unitarios (129 tests)

Backend independiente en `/backend` (Ktor + PostgreSQL).

---

## commonMain

### `domain/model/`

| Archivo | Que representa |
|---|---|
| `MarineCategory.kt` | Enum con `isUnlockedByDefault`. 10 categorias: 5 base + CEPHALOPOD/REPTILE/MAMMAL/DECORATION/COMPANION |
| `WorkBlock.kt` | Bloque + `Recurrence` sealed `@Serializable` + `WeeklySlot` |
| `TaskStatus.kt` | PENDING, COMPLETED, EXPIRED, POSTPONED |
| `TaskSchedule.kt` | `OneTime(date, time?)` / `Recurring(time, recurrence)` |
| `DayTask.kt` | `blockId` nullable, `sourceTaskId` para recurrentes, `hasBeenRewarded` para XP, `notificationsEnabled` para push, `targetCount`/`currentCount` (contable), `notes` (markdown), `timerDurationMinutes`, `isPriority` |
| `RecurringTaskDef.kt` | `time: LocalTime?` nullable, `notificationsEnabled` propagado, `targetCount`, `noteTemplate`, `timerDurationMinutes`, `isPriority` |
| `DaySummary.kt` | Score interno + mensaje visible |
| `BlockStreak.kt` | PK natural = `blockId`, + `longestStreak` |
| `EcosystemState.kt` | XP + nivel + `isUnlocked` por categoria |
| `MarineCreature.kt` | `experience` + `creatureLevel` individuales + `CreatureSpecies` (70 especies en 10 categorias) |
| `CreatureRarity.kt` | Enum: COMMON(0.40), UNCOMMON(0.30), RARE(0.20), EPIC(0.08), LEGENDARY(0.02) |
| `PendingLootbox.kt` | `data class PendingLootbox(category, categoryLevel)` |
| `BlockCategory.kt` | Relacion bloque <-> categoria (automatica) |
| `DecorationProgress.kt` | Progreso de desbloqueo de decoraciones |
| `LoggedInUser.kt` | Datos del usuario logueado (Google Sign-In) |
| `SyncStatus.kt` | Enum: IDLE, SYNCING, SUCCESS, ERROR, OFFLINE |

### `domain/repository/`

| Archivo | Operaciones clave |
|---|---|
| `EcosystemStateRepository.kt` | getByCategory, getAll, **getUnlocked**, insert, update |
| `MarineCreatureRepository.kt` | getByEcosystem, **getByCategory**, insert, update |
| `UserPreferencesRepository.kt` | nightSummaryTime (Flow), **pendingLootboxes**, **lastDismissedSummaryDate**, hasCompletedOnboarding, **morningReminderTime** (Flow), wallpaperFps, resetOnboarding, auth tokens, sync time |
| Resto | operaciones estandar + getModifiedSince, upsertAll (sync) |

### `domain/`

| Archivo | Que hace |
|---|---|
| `MarineCategoryAssigner.kt` | Redistribuye entre categorias **desbloqueadas** (requiere `EcosystemStateRepository`) |
| `RecurringTaskGenerator.kt` | Genera instancias; soporta `time` nullable; propaga `notificationsEnabled` |
| `BlockStreakProcessor.kt` | Rachas por bloque + deteccion de hitos [7, 14, 30] |
| `NightSummaryProcessor.kt` | Recibe `summaryTime`; evalua tareas completadas + PENDING con hora <= summaryTime |
| `EcosystemProcessor.kt` | XP a categorias + XP a criaturas + lootbox detection + overflow |
| `LootboxResolver.kt` | Seleccion weighted-random de especie al abrir lootbox |
| `EcosystemLevelCalculator.kt` | Curva de niveles compartida para ecosistemas y criaturas |
| `DecorationUnlockChecker.kt` | Verifica condiciones de desbloqueo de decoraciones |

### `presentation/aquarium/`

| Archivo | Que hace |
|---|---|
| `AquariumBackground.kt` | Canvas: cielo dinamico por hora (7 periodos), superficie con olas animadas, fondo marino |
| `AquariumBounds.kt` | `SURFACE_FRACTION=0.05`, `FLOOR_FRACTION=0.85`, `surfaceY(h)`, `floorY(h)` |
| `AquariumCreature.kt` | `CreatureSpec`, SwimZone, EasingType, 70 especies, `CATEGORY_UNLOCK_LEVELS`, tempo warping, hit-testing |
| `AquariumTerrain.kt` | Terreno procedural Catmull-Rom con seed |
| `AquariumTerrainConfig.kt` | Configuracion per-session del terreno |
| `AquariumTerrainDecorations.kt` | Sistema procedural de decoraciones del fondo |
| `AquariumLighting.kt` | Sistema de iluminacion dinamica |
| `AquariumWeatherEffects.kt` | Efectos meteorologicos en el acuario |
| `AquariumParticles.kt` | Sistema de particulas (burbujas, etc.) |
| `CreatureRenderer.kt` | `CreatureRenderer` interface + `rendererFor(species)` dispatch + `CreatureIcon` @Composable |
| `CreatureDetailDialog.kt` | Dialog: CreatureIcon, nombre, badge rareza, nickname editable, XpBar |
| `CreatureExtensions.kt` | `displayName` y `xpRequiredForLevel` compartidos |
| `EcosystemScreen.kt` | Pantalla "Mi ecosistema": grid 3 col, barra progreso por categoria |
| `weather/WeatherProvider.kt` | Interfaz de proveedor de clima |
| `weather/WeatherState.kt` | Estado meteorologico |
| `weather/RandomWeatherProvider.kt` | Proveedor de clima aleatorio |
| `flora/` | **9 renderers**: BrainCoral, Anemone, Kelp, Posidonia, FanCoral, TubeSponge, SeaGrass, FireCoral, StaghornCoral |
| `fauna/` | **61 renderers**: todos los peces, cefalopodos, mamiferos, reptiles, crustaceos, moluscos, pelagicos, decoraciones + Bimba |

### `presentation/main/`

| Archivo | Que hace |
|---|---|
| `MainUiState.kt` | + `creaturesData`, `pendingLootboxes`, `revealedSpecies`, sync status |
| `MainViewModel.kt` | `loadDay`, `toggleTaskCompleted` con lootboxes, `openLootbox`, `confirmUnlock`, sync |
| `MainScreen.kt` | TaskCard 52dp (contable, timer, prioridad, notas markdown); sorting estable; dialogo FPS wallpaper; dialogo lootbox bifasico |
| `WeekCalendar.kt` | Excluye POSTPONED |
| `MainDrawer.kt` | BLOQUES + ECOSISTEMA + PROGRESO + CUENTA + AJUSTES (sin boton reset tutorial) |
| `CurrentDate.kt` | expect fun currentDate() |

### `presentation/stats/`

| Archivo | Que hace |
|---|---|
| `StatsUiState.kt` | `StatsRange` enum (WEEK/MONTH/ALL_TIME) + estado |
| `StatsViewModel.kt` | Carga DaySummary por rango + BlockStreak activos |
| `StatsScreen.kt` | Grafico barras Canvas + toggle rango + tarjetas resumen + rachas |

### `presentation/history/`

| Archivo | Que hace |
|---|---|
| `HistoryUiState.kt` | `HistoryRange` enum (DAYS_30/DAYS_60/DAYS_90) + estado |
| `HistoryViewModel.kt` | Carga tareas completadas/expiradas + summaries por rango |
| `HistoryScreen.kt` | LazyColumn dias agrupados + selector rango + filtros |

### `presentation/onboarding/`

| Archivo | Que hace |
|---|---|
| `OnboardingScreen.kt` | Flujo 4 pasos con AnimatedContent, paleta marina |

### `presentation/task/`

| Archivo | Que hace |
|---|---|
| `TaskFormSheet.kt` | `initialBlockId`, `forceRecurring`, toggle `notificationsEnabled`, dialogos timer/contable con presets + campo custom, toggle prioridad |
| `PostponeSheet.kt` | Hora opcional |
| `TaskFormViewModel.kt` | Logica del formulario de tarea |

### `presentation/block/`

| Archivo | Que hace |
|---|---|
| `BlockFormScreen.kt` | Formulario de bloque |
| `BlockFormViewModel.kt` | Logica del formulario de bloque |

### `presentation/components/`

| Archivo | Que hace |
|---|---|
| `TimeInputField.kt` | Campo readonly, click abre picker |
| `DateInputField.kt` | Campo readonly, click abre picker |

### Otros

| Archivo | Que hace |
|---|---|
| `LocalizationExtensions.kt` | Extension functions para enums (displayNameRes) |

---

## androidMain

### `data/local/entity/`

8 entities Room con campos `updatedAt` (v12+) y `isDeleted` en 3 tablas (WorkBlock, DayTask, RecurringTaskDef). v13 añade campos enriquecidos (contable, timer, prioridad, notas).

### `data/local/dao/`

8 DAOs. Todos incluyen `getModifiedSince()`, `upsertAll()`, `stampUpdatedAt()` para sync.

### `data/local/db/`

`RiptideDatabase` -- **version 13**. Migraciones reales: `MIGRATION_9_10`, `MIGRATION_10_11`, `MIGRATION_11_12`, `MIGRATION_12_13`.

### `data/local/mapper/`

8 mappers Entity <-> domain model.

### `data/remote/`

| Archivo | Que hace |
|---|---|
| `ApiClient.kt` | Ktor HttpClient (OkHttp, JSON, Bearer auth, auto-refresh) |
| `RiptideApi.kt` | sync(), authGoogle(), logout() |
| `TokenProvider.kt` | Interface abstracta para JWT storage |
| `DataStoreTokenProvider.kt` | Implementacion con DataStore |
| `AuthManager.kt` | Google Sign-In flow + JWT lifecycle |
| `dto/SyncDtos.kt` | SyncRequest, SyncResponse, 8 DTOs de recurso, auth DTOs |
| `dto/DtoMappers.kt` | Entity <-> DTO conversiones (16 funciones) |

### `data/sync/`

| Archivo | Que hace |
|---|---|
| `SyncManager.kt` | Push/pull bidireccional con conflict resolution |
| `SyncTrigger.kt` | Debounce 5s tras mutacion local |
| `SyncWorker.kt` | WorkManager periodico (1h, constraint CONNECTED) |
| `InitialSyncPreparer.kt` | Stampar datos pre-existentes para primera sync |
| `ConnectivityObserver.kt` | Flow<Boolean> de estado de red |

### `data/repository/`

9 repository implementations (Room-backed).

### `presentation/`

| Archivo | Que hace |
|---|---|
| `Navigation.kt` | ROUTE_ECOSYSTEM, ROUTE_ONBOARDING, ROUTE_STATS, ROUTE_HISTORY; composable conectado a MainViewModel |
| `MainViewModelFactory.kt` | Inyecta EcosystemProcessor, LootboxResolver, TaskReminderScheduler, AuthManager, SyncManager |
| `BlockFormViewModelFactory.kt` | Inyecta MarineCategoryAssigner |
| `StatsViewModelFactory.kt` | Instancia StatsViewModel con repos de DB |
| `HistoryViewModelFactory.kt` | Instancia HistoryViewModel con repos de DB |
| `TaskFormViewModelFactory.kt` | Instancia TaskFormViewModel |
| `MainScreen.android.kt` | Platform-specific composable |

### Workers y notificaciones

| Archivo | Que hace |
|---|---|
| `NotificationHelper.kt` | 3 canales + send functions |
| `NightSummaryWorker.kt` | Resumen nocturno + push + auto-reprogramacion |
| `MorningReminderWorker.kt` | Aviso matutino + auto-reprogramacion diaria |
| `TaskReminderWorker.kt` | One-shot a la hora de la tarea |
| `TaskReminderSchedulerImpl.kt` | WorkManager REPLACE + rescheduleAll() |
| `NightSummaryScheduler.android.kt` | + getMorningReminderTime, scheduleMorningReminder |

### Widget

| Archivo | Que hace |
|---|---|
| `widget/RiptideWidget.kt` | GlanceAppWidget -- tareas del dia + progreso |
| `widget/RiptideWidgetReceiver.kt` | GlanceAppWidgetReceiver |
| `widget/WidgetUpdater.kt` | refreshAll() -- refresca per-GlanceId desde la app |
| `widget/ToggleTaskAction.kt` | Accion interactiva para toggle de tareas |

### Wallpaper

| Archivo | Que hace |
|---|---|
| `wallpaper/RiptideWallpaperService.kt` | WallpaperService + Engine, FPS configurable (15/30/60), hardware canvas API 26+ |
| `wallpaper/WallpaperDataProvider.kt` | Carga criaturas de Room, refresco cada 5min |

### Otros

| Archivo | Que hace |
|---|---|
| `App.kt` | Decide startDestination segun hasCompletedOnboarding |
| `MainActivity.kt` | createChannels(), permiso POST_NOTIFICATIONS, rescheduleAll(), splash |
| `DataSeeder.kt` | 5 bloques genericos + EcosystemStates pre-creados |

---

## iosMain

Todo pendiente para Fase 4 excepto `UuidGenerator`, `CurrentDate` y `ParseColor`.

---

## commonTest

| Archivo | Tests |
|---|---|
| `EcosystemLevelCalculatorTest.kt` | 16 tests: xpForLevel, levelForXp, nightBonus |
| `BlockStreakProcessorTest.kt` | 12 tests: rachas, hitos, reset, idempotencia |
| `NightSummaryProcessorTest.kt` | 9 tests: score, exclusiones, EXPIRED |

Fakes in-memory: `FakeDayTaskRepository`, `FakeBlockStreakRepository`, `FakeDaySummaryRepository`.

---

## Backend (`/backend`)

```
backend/src/main/kotlin/com/mnebot/riptide/backend/
  Application.kt                  -- Ktor entry point (EngineMain + module)
  plugins/
    Routing.kt                    -- Registro central de rutas + CORS + CallLogging
    Serialization.kt              -- ContentNegotiation + kotlinx.serialization JSON
    Security.kt                   -- JWT config + Google token verification + JwtConfig
    StatusPages.kt                -- Manejo global de errores (400, 401, 404, 500)
  db/
    DatabaseFactory.kt            -- HikariCP pool + Exposed + SchemaUtils.create()
    tables/                       -- 10 tablas Exposed (mirror Room v12 + users + refresh_tokens)
      UsersTable.kt               -- id, email, googleId, displayName, avatarUrl
      WorkBlocksTable.kt          -- + userId, updatedAt, isDeleted
      BlockCategoriesTable.kt
      DayTasksTable.kt            -- 14 campos -- mirror exacto de DayTaskEntity
      RecurringTaskDefsTable.kt
      DaySummariesTable.kt
      BlockStreaksTable.kt         -- + longestStreak
      EcosystemStatesTable.kt
      MarineCreaturesTable.kt
  models/
    Auth.kt                       -- GoogleAuthRequest, TokenResponse, RefreshRequest
    ApiModels.kt                  -- DTOs + SyncRequest + FullSyncResponse
  routes/
    AuthRoutes.kt                 -- POST /auth/google, POST /auth/refresh, POST /auth/logout
    SyncRoutes.kt                 -- POST /api/sync -- batch push/pull con conflict resolution
    WorkBlockRoutes.kt            -- CRUD /api/blocks
    BlockCategoryRoutes.kt        -- GET + POST(upsert) /api/block-categories
    DayTaskRoutes.kt              -- CRUD /api/tasks (?date=, ?updatedSince=)
    RecurringTaskDefRoutes.kt     -- CRUD /api/recurring-defs
    DaySummaryRoutes.kt           -- CRUD /api/summaries (?from=, ?to=)
    BlockStreakRoutes.kt          -- GET + POST(upsert) /api/streaks
    EcosystemStateRoutes.kt       -- GET + POST(upsert) /api/ecosystem-states
    MarineCreatureRoutes.kt       -- GET + POST(upsert) /api/creatures (?category=)
```
