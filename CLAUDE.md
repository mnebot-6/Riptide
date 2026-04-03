# Riptide — Claude Code Reference

## Reglas de trabajo (OBLIGATORIO)

- **NUNCA uses `isolation: "worktree"` al lanzar sub-agentes (Agent tool).** Todos los cambios deben hacerse directamente sobre la rama `master` del repositorio principal. Los worktrees causan confusión y complican el flujo de trabajo.
- **NUNCA hagas commits en ramas auxiliares.** Siempre trabaja y commitea en `master`.

## Proyecto

App de productividad personal con sistema de recompensa emocional basado en un ecosistema marino que crece con la consistencia del usuario.

**Filosofía core:**
- La puntuación nunca se muestra como número — el feedback es siempre emocional (mensajes, crecimiento visual)
- La consistencia construye algo bello. El abandono lo pausa. Nunca lo destruye.
- No hay racha rota: si fallas un día, el ecosistema espera, no retrocede

**Plataforma:** Android (v3 activa). iOS previsto para v6.

---

## Documentación en /docs/

| Archivo | Qué cubre |
|---|---|
| `riptide_context.txt` | Referencia completa del proyecto: todos los sistemas, modelos, lógica de negocio, convenciones |
| `architecture.md` | MVVM, expect/actual KMP, Room schema, EcosystemProcessor, animaciones AquariumCreatures, NightSummary |
| `data-models.md` | Todos los modelos de dominio: WorkBlock, DayTask, RecurringTaskDef, MarineCreature, CreatureSpecies (70), XP curve |
| `project_structure.md` | Árbol de directorios completo con descripción de cada archivo |
| `roadmap.md` | Fases completadas y futuras (siguiente: Play Store, después iOS) |

**Para contexto profundo en una tarea específica, lee primero el archivo de /docs/ correspondiente.**

---

## Stack

### Android (KMP)

| Tecnología | Versión |
|---|---|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.10.2 |
| Room | 2.8.4 (schema v13) |
| Ktor Client | 3.0.3 |
| Google Play Services Auth | 21.3.0 |
| kotlinx-serialization | 1.7.3 |
| androidx-datastore | 1.1.7 |
| androidx-work (WorkManager) | 2.10.1 |
| Gradle | 8.14.3 |

### Backend (`/backend`)

| Tecnología | Versión |
|---|---|
| Kotlin | 2.0.21 |
| Ktor | 3.0.3 |
| Exposed ORM | 0.57.0 |
| HikariCP | 6.2.1 |
| PostgreSQL JDBC | 42.7.4 |
| google-api-client | 2.7.2 |
| Logback | 1.5.12 |

---

## Comandos útiles

### Android

```bash
# Build debug
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# Ejecutar tests unitarios
./gradlew testDebugUnitTest

# Verificar compilación sin generar APK
./gradlew compileDebugKotlin
```

### Backend

```bash
# Compilar backend
cd backend && bash gradlew compileKotlin

# Generar fat JAR
cd backend && bash gradlew buildFatJar

# Ejecutar localmente (requiere PostgreSQL)
cd backend && bash gradlew run

# Ejecutar con variables de entorno
DATABASE_URL=jdbc:postgresql://localhost:5432/riptide \
DATABASE_USER=riptide DATABASE_PASSWORD=riptide \
JWT_SECRET=your-secret GOOGLE_CLIENT_ID=your-client-id \
cd backend && bash gradlew run
```

---

## Convenciones críticas

- **IDs:** siempre `UUID.randomUUID().toString()`
- **Colores:** paleta marina definida en `Theme.kt` — no usar colores hardcodeados
- **Fechas/horas:** `LocalDate`/`LocalTime` de `kotlinx-datetime`, serializadas como strings ISO
- **Room migrations:** SIEMPRE migraciones reales (nunca `fallbackToDestructiveMigration`). Ver `MIGRATION_8_9` y `MIGRATION_9_10` como referencia.
- **hasBeenRewarded:** campo en `DayTask` para evitar XP duplicado — siempre respetar esta bandera al distribuir XP
- **notificationsEnabled:** campo en `DayTask` y `RecurringTaskDef` — solo relevante si la tarea tiene hora; propagar de def a instancias en `RecurringTaskGenerator`
- **Notificaciones:** usar siempre `NotificationHelper` para enviar pushes. Tres canales: `night_summary`, `morning_reminder`, `task_reminder`. `TaskReminderWorker` usa nombre único `"task_reminder_$taskId"`.
- **Recurrence:** enum serializado como string (`DAILY`, `WEEKLY`, `MONTHLY`)
- **Tiempo nulo en RecurringTaskDef:** tarea sin hora asignada — el generador la omite en el DaySummary si no tiene `summaryTime`

---

## Estado actual (abril 2026)

**Completado:**
- MVVM + Room offline-first (v13, migraciones reales)
- Ecosistema marino: 70 especies, 10 categorías, sistema lootbox con rareza, crecimiento individual
- Patrones de nado orgánicos (tempo warping, variación por instancia, márgenes simétricos)
- Resumen nocturno con filtrado correcto por `summaryTime`; push notification tras `processDay`
- Tareas EXPIRED completables con checkbox
- TimePicker y DatePicker con paleta marina
- Inputs de fecha/hora readonly (click abre picker)
- Superficie del agua animada, fondo marino elaborado, cielo dinámico (7 periodos)
- Flora Canvas: BrainCoral, Anemone, Kelp, Posidonia, FanCoral — crecimiento visual por nivel
- `CreatureIcon` composable reutilizable (Canvas animado para todas las especies)
- Sistema lootbox: desbloqueo aleatorio ponderado por rareza (COMMON→LEGENDARY)
- **i18n**: EN + ES, `LocalizationExtensions.kt` con extension functions para enums
- **70 renderers Canvas** (cobertura total): todas las 70 especies tienen Canvas renderer propio
- **Onboarding**: flujo de 4 pasos con `AnimatedContent`, DataStore key `onboarding_completed`, se muestra solo en primer lanzamiento
- **Notificaciones push**: `NotificationHelper` (3 canales), push resumen nocturno + matutino + por tarea, `TaskReminderSchedulerImpl` (WorkManager, `rescheduleAll()`), `notificationsEnabled` en `DayTask`/`RecurringTaskDef` (Room v10), permiso `POST_NOTIFICATIONS`
- **Estadísticas**: `StatsScreen` con gráfico de barras Canvas, toggle Semana/Mes/Todo, toggle absoluto/porcentaje, tarjetas de resumen, rachas por bloque
- **Historial**: `HistoryScreen` con selector 30/60/90 días, LazyColumn de días agrupados, badge completadas/totales
- **Recompensas de racha**: `BlockStreakUpdate` con `milestonesReached`, hitos [7, 14, 30], `PendingLootbox` generados en `NightSummaryProcessor`
- **Evolución visual de criaturas**: efectos discretos en nivel 3+ y nivel 5+ en 10 renderers de fauna (marcas, brillos, compañeros, bioluminiscencia)
- **Tests automatizados**: 129 tests en `commonTest` — 13 test suites. Fakes in-memory, `kotlinx-coroutines-test`
- **Iconografía Lucide**: 26 vector drawables (`ic_*.xml`, incluyendo filled play/pause). Todos los emojis de control reemplazados por `Icon(painterResource(...))` en MainDrawer, MainScreen, TaskFormSheet, StatsScreen, EcosystemScreen
- **Logo & Branding "Rising Currents"**: 3 olas ascendentes con ondulación, adaptive icon (foreground + background azul océano), logo in-app 24dp, icono notificación, splash screen (`core-splashscreen` 1.0.1, tema `Theme.Riptide.Splash`)
- **Widget Android**: Glance widget con tareas del día, barra de progreso, paleta marina. `WidgetUpdater.refreshAll()` en `onResume`. Metadata 3×3 celdas, redimensionable, auto-update 30min.
- **Live Wallpaper**: `RiptideWallpaperService` (WallpaperService + Choreographer, FPS configurable 15/30/60). Bridge `CanvasDrawScope` reutiliza todo el renderizado Compose sin portar código. `WallpaperDataProvider` lee criaturas de Room con `@Volatile` y refresco cada 5min (IO dispatchers). Hardware canvas (`lockHardwareCanvas()`) para API 26+. Diálogo FPS en MainScreen antes de aplicar. `GLOBAL_SPEED_MULTIPLIER = 2.0f`.
- **Terreno suavizado**: amplitud de ondulaciones reducida 72% (`0.08f → 0.022f`), Y-range `[0.73-0.93] → [0.80-0.90]` — superficie de arena con cambios casi imperceptibles
- **Flora del fondo escalada**: Kelp (1.0→0.50), Anemone (0.65→0.42), BrainCoral (1.0→0.50), FanCoral (1.0→0.48), Posidonia (1.0→0.52), SeaUrchin (1.0→0.55)
- **Rediseño BrainCoral**: reemplaza líneas rectas por cúpula hemisférica con gradiente + grooves laberínticos sinusoidales + highlight especular + glow secundario (nivel 3+)

- **Backend Ktor**: proyecto `/backend` independiente — Ktor 3.0.3 + Exposed 0.57.0 + PostgreSQL. 10 tablas (mirror Room v13 + users + refresh_tokens), API REST CRUD completa (8 recursos), auth Google Sign-In + JWT con refresh token rotation y theft detection, Dockerfile multi-stage, health check.
- **Sync offline-first**: Room v13 con `updatedAt` en 8 tablas + `isDeleted` en 3 (soft delete). Ktor Client (OkHttp) con auto-refresh JWT. `POST /api/sync` batch endpoint (push+pull en una llamada). `SyncManager` con conflict resolution (`updatedAt` wins, `hasBeenRewarded` OR-merge, server deletion autoritativo). `SyncTrigger` (5s debounce) + `SyncWorker` (1h periodic via WorkManager). Google Sign-In en drawer con sección "Cuenta" (avatar, sync status badge, logout). `InitialSyncPreparer` para primera sincronización de datos pre-existentes.
- **Tareas enriquecidas** (Room v13): contable (`targetCount`/`currentCount`), notas markdown con checkboxes interactivos, timer (`timerDurationMinutes`), prioridad (`isPriority`). Campos propagados de `RecurringTaskDef` a `DayTask`. `MIGRATION_12_13` con 9 ALTER TABLE.
- **TaskCard rediseñado**: altura fija 52dp, single-row. Contables muestran `currentCount/targetCount` sin checkbox. Timer con filled play/pause. Prioridad con estrella. Sorting estable (no cambia al completar tareas).
- **Notas markdown**: diálogo con botones helper (☐, •, B, H). Checkboxes interactivos en modo lectura.
- **Rediseño Bimba**: labrador amarilla senior 3 patas con collar rosa, colgante corazón bézier, morro gris, ojo marrón cálido, aura rosa.
- **Paleta bloques**: 12 colores marinos ajustados en BlockFormScreen.
- **Widget**: refresh per-GlanceId para recomposición fiable.
- **Superficie acuario**: `SURFACE_FRACTION` bajada a 0.05.
- **Ecosistema**: blur 4dp + alpha 0.22f en criaturas bloqueadas.

**Próximo (→ Play Store):**
- Sprint Store Prep (firma, privacy policy, screenshots, listing)
- 🚀 Play Store
- Sprint iOS (post-launch, sin prisa)

---

## Archivos críticos

```
composeApp/src/
├── commonMain/kotlin/com/mnebot/riptide/
│   ├── domain/
│   │   ├── model/          # WorkBlock, DayTask (+ contable/timer/prioridad/notas), RecurringTaskDef, MarineCreature, etc.
│   │   ├── repository/     # Interfaces de repositorios
│   │   │   └── processor/      # EcosystemProcessor, NightSummaryProcessor, RecurringTaskGenerator, MarineCategoryAssigner
│   │   └── LootboxResolver.kt            # Selección weighted-random de especie al abrir lootbox
│   └── presentation/
│       ├── main/MainScreen.kt              # Pantalla principal: TaskCard 52dp (contable/timer/prioridad/notas), sorting estable, diálogo FPS
│       ├── main/MainViewModel.kt           # ViewModel principal
│       ├── stats/StatsScreen.kt            # Gráfico barras Canvas + toggle Semana/Mes + rachas
│       ├── stats/StatsViewModel.kt         # Carga DaySummary + BlockStreak por rango
│       ├── history/HistoryScreen.kt        # LazyColumn días agrupados + selector 30/60/90d
│       ├── history/HistoryViewModel.kt     # Carga tareas completadas + summaries por rango
│       ├── aquarium/AquariumBackground.kt  # Canvas: cielo dinámico, superficie, fondo marino; drawAquariumBackground() extraída
│       ├── aquarium/AquariumBounds.kt      # SURFACE_FRACTION=0.05, FLOOR_FRACTION=0.85
│       ├── aquarium/AquariumCreature.kt    # CreatureSpec, animación, hit-testing; GLOBAL_SPEED_MULTIPLIER; drawAquariumCreatures() extraída
│       ├── aquarium/CreatureRenderer.kt    # Interface + rendererFor() (70 renderers, cobertura total) + CreatureIcon composable
│       ├── aquarium/EcosystemScreen.kt     # Grid de criaturas desbloqueadas
│       ├── aquarium/flora/                 # 9 renderers: BrainCoral, Anemone, Kelp, Posidonia, FanCoral, TubeSponge, SeaGrass, FireCoral, StaghornCoral
│       ├── aquarium/fauna/                 # 61 renderers — COBERTURA TOTAL: todas las 70 especies (9 son flora/)
│       ├── onboarding/OnboardingScreen.kt  # Flujo 4 pasos: bienvenida, cómo funciona, ecosistema, listo
│       └── theme/Theme.kt                  # Paleta de colores marina
└── androidMain/kotlin/com/mnebot/riptide/
    ├── NotificationHelper.kt           # Canales + sendNightSummary/MorningReminder/TaskReminder
    ├── NightSummaryWorker.kt           # Resumen nocturno + push + auto-reprogramación
    ├── MorningReminderWorker.kt        # Aviso matutino + auto-reprogramación diaria
    ├── TaskReminderWorker.kt           # One-shot a la hora de la tarea
    ├── TaskReminderSchedulerImpl.kt    # WorkManager REPLACE + rescheduleAll()
    ├── widget/RiptideWidget.kt         # GlanceAppWidget — tareas del día + progreso
    ├── widget/RiptideWidgetReceiver.kt # GlanceAppWidgetReceiver
    ├── widget/WidgetUpdater.kt         # refreshAll() — refresca widgets desde la app
    ├── wallpaper/RiptideWallpaperService.kt  # WallpaperService + Engine, FPS configurable (15/30/60), hardware canvas API 26+
    ├── wallpaper/WallpaperDataProvider.kt    # Carga criaturas de Room, refresco cada 5min
    ├── presentation/stats/StatsViewModelFactory.kt
    ├── presentation/history/HistoryViewModelFactory.kt
    ├── data/local/
    │   ├── db/RiptideDatabase.kt       # Room DB v13, migraciones reales (8_9..12_13)
    │   ├── dao/                        # DAOs para cada entidad (+getModifiedSince, upsertAll, stampUpdatedAt)
    │   └── SyncTimestamp.kt            # nowIso() utility
    ├── data/remote/
    │   ├── ApiClient.kt               # Ktor HttpClient (OkHttp, JSON, Bearer auth, auto-refresh)
    │   ├── RiptideApi.kt              # sync(), authGoogle(), logout()
    │   ├── TokenProvider.kt           # Interface abstracta para JWT storage
    │   ├── DataStoreTokenProvider.kt  # Implementación con DataStore
    │   ├── AuthManager.kt             # Google Sign-In flow + JWT lifecycle
    │   └── dto/
    │       ├── SyncDtos.kt            # SyncRequest, SyncResponse, 8 DTOs de recurso, auth DTOs
    │       └── DtoMappers.kt          # Entity↔DTO conversiones (16 funciones)
    └── data/sync/
        ├── SyncManager.kt             # Push/pull bidireccional con conflict resolution
        ├── SyncTrigger.kt             # Debounce 5s tras mutación local
        ├── SyncWorker.kt              # WorkManager periódico (1h, constraint CONNECTED)
        ├── InitialSyncPreparer.kt     # Stampar datos pre-existentes para primera sync
        └── ConnectivityObserver.kt    # Flow<Boolean> de estado de red

backend/src/main/kotlin/com/mnebot/riptide/backend/
├── Application.kt                  # Ktor entry point (EngineMain + module)
├── plugins/
│   ├── Routing.kt                  # Registro central de rutas + CORS + CallLogging
│   ├── Serialization.kt           # ContentNegotiation + kotlinx.serialization JSON
│   ├── Security.kt                # JWT config + Google token verification + JwtConfig object
│   └── StatusPages.kt             # Manejo global de errores (400, 401, 404, 500)
├── db/
│   ├── DatabaseFactory.kt         # HikariCP pool + Exposed + SchemaUtils.create()
│   └── tables/                    # 10 tablas Exposed (mirror Room v13 + users + refresh_tokens)
│       ├── UsersTable.kt          # id, email, googleId, displayName, avatarUrl
│       ├── WorkBlocksTable.kt     # + userId, updatedAt, isDeleted
│       ├── BlockCategoriesTable.kt
│       ├── DayTasksTable.kt       # 19 campos — mirror exacto de DayTaskEntity (v13)
│       ├── RecurringTaskDefsTable.kt
│       ├── DaySummariesTable.kt
│       ├── BlockStreaksTable.kt   # + longestStreak (Room v11)
│       ├── EcosystemStatesTable.kt
│       └── MarineCreaturesTable.kt
├── models/
│   ├── Auth.kt                    # GoogleAuthRequest, TokenResponse, RefreshRequest
│   └── ApiModels.kt              # DTOs + SyncRequest + FullSyncResponse
└── routes/
    ├── AuthRoutes.kt              # POST /auth/google, POST /auth/refresh, POST /auth/logout
    ├── SyncRoutes.kt              # POST /api/sync — batch push/pull con conflict resolution
    ├── WorkBlockRoutes.kt         # CRUD /api/blocks
    ├── BlockCategoryRoutes.kt     # GET + POST(upsert) /api/block-categories
    ├── DayTaskRoutes.kt           # CRUD /api/tasks (?date=, ?updatedSince=)
    ├── RecurringTaskDefRoutes.kt  # CRUD /api/recurring-defs
    ├── DaySummaryRoutes.kt        # CRUD /api/summaries (?from=, ?to=)
    ├── BlockStreakRoutes.kt       # GET + POST(upsert) /api/streaks
    ├── EcosystemStateRoutes.kt    # GET + POST(upsert) /api/ecosystem-states
    └── MarineCreatureRoutes.kt    # GET + POST(upsert) /api/creatures (?category=)
```
