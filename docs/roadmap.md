# Roadmap -- Riptide

## Done - MVP completado

- Infraestructura KMP + Room + DataStore + WorkManager
- CRUD completo: bloques, tareas puntuales y recurrentes
- Editar / posponer / eliminar con dialogos de scope para recurrentes
- Resumen nocturno con WorkManager
- Indicadores de progreso

---

## Done - v2 completada

- Rachas por bloque (`BlockStreak`) con badge
- Mensajes emocionales contextuales
- Ecosistema visual con Canvas (AquariumBackground + AquariumCreatures)
- Desbloqueo de criaturas con nombre obligatorio
- XP por tarea y bonus nocturno

---

## Done - v2.1 completada

- Curva XP reajustada (nivel 2 = 1 XP)
- Fix edicion recurrentes con dialogo de scope
- TimeInputField + DateInputField estandarizados
- Pickers con estetica marina
- DataSeeder limpio

---

## Done - Sprint de fixes y mejoras

- Pantalla bloqueada en portrait
- Resumen nocturno no reaparece (fecha descartada en DataStore)
- Scroll mantiene posicion al completar tarea
- Posponer con hora opcional
- POSTPONED excluido de resumen y barra de progreso
- Resumen nocturno evalua solo tareas vencidas
- Nuevo Header fijo con acciones integradas
- Drawer compacto con scroll interno (85% pantalla)
- Tareas recurrentes con hora opcional
- Editar recurrente con dialogo scope + `forceRecurring`
- RiptideDatabase v7

---

## Done - Sprint v2 ampliado

- 9 categorias marinas: FISH, FLORA, CRUSTACEAN, MOLLUSK, PELAGIC (base) + CEPHALOPOD, REPTILE, MAMMAL, DECORATION (bloqueadas)
- 24 especies con emojis, patrones de nado y `speedScalePerLevel`
- `EcosystemState.isUnlocked` -- categorias desbloqueables dinamicamente
- `MarineCategoryAssigner` redistribuye entre categorias desbloqueadas
- `EcosystemProcessor` reparte XP a criaturas individuales (`experience` + `creatureLevel`)
- Efectos visuales por nivel: tamano (+10% por nivel desde 80%) y velocidad de nado
- Ordenacion de bloques por tarea pendiente con hora mas temprana
- Long press en cabecera del bloque -> crear tarea con bloque preseleccionado
- RiptideDatabase v8

---

## Done - Sprint pre-v3

- Tap en criatura -> detalle: `CreatureDetailDialog` con nickname editable, `XpBar` visual, fecha de desbloqueo
- `EcosystemScreen`: pantalla "Mi ecosistema" con grid 3 columnas por categoria
- Drawer renovado: seccion ECOSISTEMA con boton de navegacion
- Patron de nado organico: `SwimZone` (5 bandas), `EasingType` (SMOOTH/BURST/CRAWL), tempo warping, variacion por instancia
- Fix hit-testing: un unico `pointerInput` con posiciones reales del frame

---

## Done - Sprint bugfixes

- Resumen nocturno corregido: `processDay` recibe `summaryTime`. Solo evalua tareas COMPLETED o PENDING del dia con hora <= summaryTime
- Tareas EXPIRED completables: muestran indicador + checkbox
- XP duplicado resuelto: campo `hasBeenRewarded: Boolean` en `DayTask`
- Migracion real Room: `MIGRATION_8_9`. RiptideDatabase v9
- TimePicker mejorado con paleta marina explicita
- Inputs fecha/hora simplificados: campos readonly, click abre picker
- Patrones de nado mejorados: tempo warping, variacion por instancia, margenes simetricos

---

## Done - Sprint bugfixes 2

- Emoji centrado: fix `drawText` con `textAlign = CENTER`
- Resumen nocturno con fecha correcta: fecha objetivo calculada en `schedule()` y pasada como `InputData`
- summaryTime en processDay de inicio: se lee `nightTime` antes de la llamada

---

## Done - Sprint visual -- Superficie, fondo marino y flora Canvas

- Superficie del agua: ola animada con `Path` + `quadraticTo`
- Fondo marino: banda de arena con gradiente, rocas decorativas
- AquariumBounds: constantes compartidas `SURFACE_FRACTION` y `FLOOR_FRACTION`
- CreatureRenderer: interfaz de renderizado extensible
- Flora Canvas (3 renderers): BrainCoral, Anemone, Kelp
- Anclaje al suelo para flora, moluscos y decoraciones
- Crustaceos reubicados en BOTTOM
- Burbujas ajustadas

---

## Done - Sprint visual 2 -- Mejoras de ecosistema

- Cielo dinamico por hora del dia: 7 periodos (noche -> noche tardia)
- Olas mas animadas: amplitud 9.dp, cresta secundaria
- Fondo marino elaborado: 11 rocas, 3 estilos
- Flora con multiples instancias (`instanceCount`)
- `CreatureIcon` composable reutilizable
- Opacidad de tarjetas de tarea mejorada

---

## Done - Sprint lootbox -- Sistema de rareza y desbloqueo aleatorio

- Sistema de rareza: `CreatureRarity` enum (COMMON 40%, UNCOMMON 30%, RARE 20%, EPIC 8%, LEGENDARY 2%)
- 40 especies (16 nuevas). Total 40 en 9 categorias
- Lootbox mechanic: `CATEGORY_UNLOCK_LEVELS` + `PendingLootbox`
- `LootboxResolver`: seleccion weighted-random al ABRIR
- XP overflow: 50% se queda, 50% redistribuido a categoria de menor nivel
- Dialogo lootbox bifasico (cerrada -> abierta)
- EcosystemScreen overhaul: ordenamiento por rareza, barra progreso por categoria, badges
- DataStore formato lootbox con migracion legacy

---

## Done - Sprint i18n + renderers -- Traducciones y fauna Canvas

- Sistema de traducciones (i18n): `strings.xml` EN (defecto) + `values-es/strings.xml` ES
- `LocalizationExtensions.kt`: extension functions para enums
- 13 nuevos renderers Canvas para fauna

---

## Done - Sprint renderers total -- Cobertura 100%

- 23 renderers adicionales para completar las 40 especies originales
- Cobertura 100% (40/40 especies con Canvas renderer)

---

## Done - Sprint onboarding

- Flujo de 4 pasos con `AnimatedContent` (slide+fade), paleta marina
- Persistencia con DataStore (`hasCompletedOnboarding`)
- Navegacion: `App.kt` decide `startDestination`

---

## Done - Sprint notificaciones push

- 3 canales: `night_summary`, `morning_reminder`, `task_reminder`
- Permiso `POST_NOTIFICATIONS` (API 33+)
- Push resumen nocturno + aviso matutino configurable + recordatorio por tarea
- Campo `notificationsEnabled` en `DayTask` y `RecurringTaskDef` (Room v10)
- `TaskReminderScheduler` interfaz + `TaskReminderSchedulerImpl` (WorkManager)

---

## Done - Sprint estadisticas + historial

- `StatsScreen`: grafico barras Canvas, toggle Semana/Mes, tarjetas resumen, rachas por bloque
- `HistoryScreen`: LazyColumn dias agrupados, selector 30/60/90 dias, badge completadas/totales
- ViewModels con `selectRange()` reactivo
- Navigation con `ROUTE_STATS` y `ROUTE_HISTORY`

---

## Done - Sprint recompensas de racha

- `BlockStreakUpdate` con `milestonesReached`, hitos [7, 14, 30]
- `NightSummaryProcessor` genera `PendingLootbox` por cada hito alcanzado

---

## Done - Sprint evolucion visual de criaturas

- Efectos discretos por nivel en 10 renderers de fauna (marcas, brillos, companeros, bioluminiscencia)
- Nivel 3+ y nivel 5+ con efectos diferenciados

---

## Done - Sprint tests unitarios automatizados

- `kotlinx-coroutines-test:1.10.2` en commonTest
- Fakes in-memory (sin libreria de mocking)
- 37 tests, 0 fallos: `EcosystemLevelCalculatorTest` (16), `BlockStreakProcessorTest` (12), `NightSummaryProcessorTest` (9)

---

## Done - Sprint Lucide Icons -- Iconografia minimal

- 18 Vector Drawables stroke-based (`ic_*.xml`)
- Todos los emojis de control reemplazados por `Icon(painterResource(...))`
- MainDrawer, MainScreen, TaskFormSheet, StatsScreen, EcosystemScreen actualizados

---

## Done - Sprint Logo & Branding

- Concepto "Rising Currents": tres corrientes ascendentes
- Adaptive Icon Android (foreground vector + background azul oceano)
- Logo in-app (`ic_riptide_logo.xml`), icono de notificacion
- Splash screen (`core-splashscreen` 1.0.1, tema `Theme.Riptide.Splash`)

---

## Done - Sprint Widget Android

- Widget Glance con tareas del dia, barra de progreso, paleta marina
- `WidgetUpdater.refreshAll()` en `onResume`
- Metadata 3x3 celdas, redimensionable, auto-update 30min
- Widget interactivo: toggle de tareas, fondo translucido

---

## Done - Sprint Live Wallpaper

- `RiptideWallpaperService`: WallpaperService + Choreographer 30fps vsync-aligned
- `CanvasDrawScope` bridge reutiliza todo el renderizado Compose sin portar codigo
- `WallpaperDataProvider`: lee criaturas de Room, refresco cada 5min
- `GLOBAL_SPEED_MULTIPLIER = 2.0f` para velocidad mas natural
- Boton en drawer -> `ACTION_CHANGE_LIVE_WALLPAPER`

---

## Done - Sprint Backend

- Proyecto Ktor 3.0.3 en `/backend` -- Gradle independiente con fat JAR
- PostgreSQL + Exposed 0.57.0 -- mirror de Room v11 + tabla `users` + campos sync
- API REST completa: 8 recursos CRUD
- Autenticacion Google Sign-In -> JWT (access 24h + refresh 30d)
- Health check + Docker multi-stage

---

## Done - Sprint Sync

- Room v12: `updatedAt` en 8 tablas + `isDeleted` en 3. `MIGRATION_11_12`
- Ktor Client (OkHttp) con auto-refresh JWT
- `POST /api/sync` batch endpoint (push+pull)
- `SyncManager` con conflict resolution (`updatedAt` wins, `hasBeenRewarded` OR-merge)
- `SyncTrigger` (5s debounce) + `SyncWorker` (1h periodic)
- Google Sign-In en drawer con seccion "Cuenta"
- `ConnectivityObserver` para estado de red

---

## Done - Sprint XP spillover + decoraciones

- XP overflow 80/20 a categorias de menor nivel
- Sistema de desbloqueo de decoraciones por condiciones especiales
- Mejoras UX/UI generales

---

## Done - Sprint expansion ecosistema (40 -> 70 especies)

- 30 especies nuevas distribuidas en todas las categorias
- FISH: +3 (Butterflyfish, Seahorse, Moray Eel)
- FLORA: +4 (Tube Sponge, Sea Grass, Fire Coral, Staghorn Coral)
- CRUSTACEAN: +4 (Krill, Horseshoe Crab, Mantis Shrimp, Coconut Crab)
- MOLLUSK: +4 (Conch, Scallop, Sea Slug, Sea Cucumber)
- PELAGIC: +4 (Bluefin Tuna, Flying Fish, Lionsmane Jellyfish, Swordfish)
- CEPHALOPOD: +2 (Chambered Nautilus, Giant Pacific Octopus)
- REPTILE: +4 (Green Sea Turtle, Sea Snake, Leatherback Turtle, Saltwater Crocodile)
- MAMMAL: +1 (Narwhal)
- DECORATION: +3 (Diving Helmet, Coral Throne, Golden Trident)
- COMPANION: +1 (Bimba -- easter egg)
- Nueva categoria COMPANION (oculta, no sale por lootbox)
- 70 renderers Canvas (cobertura total)
- Terreno procedural con decoraciones
- Rediseno visual de 6 criaturas marinas con paletas de referencia SVG

---

## Done - Sprint mejoras acuario

- Weather/lighting/particles en el acuario
- Stats All Time tab
- History filters
- Fondo marino procedural con sistema de decoraciones
- Terreno suavizado (amplitud 0.022f, Y-range [0.80-0.90])

---

## Fase 2 -- Calidad

### Sprint QA & Testing

- Tests de integracion (flujos completos con fake backend)
- Edge cases UX (sin internet, primer uso, migracion datos)
- Accesibilidad (`contentDescription`, contraste WCAG, touch targets 48dp)
- Ampliar cobertura de unit tests (repositories, sync logic)

### Sprint Polish

- Performance audit (recomposiciones innecesarias, lazy lists)
- Animaciones de transicion entre pantallas
- Haptic feedback en interacciones clave
- Bug fixing final

---

## Fase 3 -- Lanzamiento Play Store

### Sprint Store Prep

- Keystore + release build + ProGuard/R8
- Privacy policy + Terms of Service (hosted)
- Screenshots (6+ pantallas, EN + ES)
- Store listing (descripcion, categoria, tags)
- Internal testing track -> closed beta -> production
- Cuenta de desarrollador Google Play (25$ one-time)

### PUBLISH PLAY STORE

---

## Fase 4 -- iOS (post-launch, sin prisa)

### Sprint iOS

- `expect/actual` reales: Room -> alternativa iOS, WorkManager -> `BGTaskScheduler`
- Pickers nativos iOS (`TimePickerDialogWrapper`, `DatePickerDialogWrapper`)
- Notificaciones locales (`UNUserNotificationCenter`)
- `NightSummaryScheduler` con background tasks iOS
- Apple Developer Account (99$/ano)
- App Store listing + review

---

## Fase 5 -- Post-launch

- Social: visitar el estanque de un amigo (solo ver, nunca competir)
- Iteraciones segun feedback real de usuarios

---

## Filosofia de diseno

- La puntuacion **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presion numerica
- El feedback es siempre emocional y contextual
