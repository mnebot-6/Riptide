# Roadmap — Riptide

## ✅ MVP completado

- Infraestructura KMP + Room + DataStore + WorkManager
- CRUD completo: bloques, tareas puntuales y recurrentes
- Editar / posponer / eliminar con diálogos de scope para recurrentes
- Resumen nocturno con WorkManager
- Indicadores de progreso

---

## ✅ v2 completada

- Rachas por bloque (`BlockStreak`) con badge 🔥
- Mensajes emocionales contextuales
- Ecosistema visual con Canvas (AquariumBackground + AquariumCreatures)
- Desbloqueo de criaturas con nombre obligatorio
- XP por tarea y bonus nocturno

---

## ✅ v2.1 completada

- Curva XP reajustada (nivel 2 = 1 XP)
- Fix edición recurrentes con diálogo de scope
- TimeInputField + DateInputField estandarizados
- Pickers con estética marina
- DataSeeder limpio

---

## ✅ Sprint de fixes y mejoras

- Pantalla bloqueada en portrait
- Resumen nocturno no reaparece (fecha descartada en DataStore)
- Scroll mantiene posición al completar tarea
- Posponer con hora opcional
- POSTPONED excluido de resumen y barra de progreso
- Resumen nocturno evalúa solo tareas vencidas
- Nuevo Header fijo con acciones integradas
- Drawer compacto con scroll interno (85% pantalla)
- Tareas recurrentes con hora opcional
- Editar recurrente con diálogo scope + `forceRecurring`
- RiptideDatabase v7

---

## ✅ Sprint v2 ampliado

- 9 categorías marinas: FISH, FLORA, CRUSTACEAN, MOLLUSK, PELAGIC (base) + CEPHALOPOD, REPTILE, MAMMAL, DECORATION (bloqueadas)
- 24 especies con emojis, patrones de nado y `speedScalePerLevel`
- `EcosystemState.isUnlocked` — categorías desbloqueables dinámicamente
- `MarineCategoryAssigner` redistribuye entre categorías desbloqueadas
- `EcosystemProcessor` reparte XP a criaturas individuales (`experience` + `creatureLevel`)
- Efectos visuales por nivel: tamaño (+10% por nivel desde 80%) y velocidad de nado
- `creatureLevelBySpecies` en `MainUiState` y `loadDay`
- `MarineCreatureRepository.getByCategory` + `EcosystemStateRepository.getUnlocked/getAll`
- Ordenación de bloques por tarea pendiente con hora más temprana
- Long press en cabecera del bloque → crear tarea con bloque preseleccionado (`initialBlockId`)
- DataSeeder con 5 bloques y EcosystemStates pre-creados antes del reassign
- RiptideDatabase v8

---

## ✅ Sprint pre-v3

- **Tap en criatura → detalle**: `CreatureDetailDialog` con nickname editable, `XpBar` visual sin números, fecha de desbloqueo. Freeze de la criatura al pulsar, unfreeze al cerrar.
- **EcosystemScreen**: pantalla nueva "Mi ecosistema" con grid 3 columnas por categoría. Cards desbloqueadas (nick + puntos de nivel) y bloqueadas (silueta + barra de progreso al unlockLevel). `IntrinsicSize.Max` para altura uniforme. Abre `CreatureDetailDialog` al pulsar.
- **Drawer renovado**: sección ECOSISTEMA con botón de navegación a `EcosystemScreen`.
- **Patrón de nado orgánico**: `SwimZone` (5 bandas), `EasingType` (SMOOTH/BURST/CRAWL), `personalYFraction` (cota personal dentro de banda), `verticalCoupling` (delfín salta), `microWobble` (aleta/cola), `xErraticness` (microaceleraciones horizontales), `waveCount` + `erraticness` (onda secundaria irracional), `driftSpeed` + `driftAmplitude` (deriva lenta aperiódica). `fixedWobbleScale` diferencia rigidez entre flora.
- **`CreatureExtensions.kt`**: `displayName` y `xpRequiredForLevel` compartidos entre `CreatureDetailDialog` y `EcosystemScreen`.
- **Fix PostponeSheet**: se cierra correctamente tras confirmar posponer.
- **Fix hit-testing**: un único `pointerInput` con posiciones reales del frame. Elimina el bug de mostrar siempre la misma criatura.

---

## ✅ Sprint bugfixes

- **Resumen nocturno corregido**: `processDay` recibe `summaryTime`. Solo evalúa tareas COMPLETED o PENDING del día con hora ≤ summaryTime. Tareas sin hora o con hora posterior se ignoran (se evaluarán en el siguiente resumen). `NightSummaryWorker` lee la hora con `.first()`.
- **Tareas EXPIRED completables**: muestran ⌛ + checkbox. Siguen siendo completables. Al desmarcar, vuelven a EXPIRED si existe DaySummary para esa fecha, o a PENDING si no.
- **XP duplicado resuelto**: campo `hasBeenRewarded: Boolean` en `DayTask` y `DayTaskEntity`. Se marca `true` al completar por primera vez. No se resetea nunca. Controlado en `toggleTaskCompleted`.
- **Migración real Room**: `MIGRATION_8_9` (ALTER TABLE `day_tasks` ADD COLUMN `hasBeenRewarded`). Eliminado `fallbackToDestructiveMigration`. RiptideDatabase v9.
- **TimePicker mejorado**: `TimePickerDefaults.colors()` con paleta marina explícita (OceanMid, Accent, etc.), consistente con el DatePicker.
- **Inputs fecha/hora simplificados**: `TimeInputField` y `DateInputField` ahora son campos readonly. Click abre el picker. Eliminados `BasicTextField`, emojis 📅 y 🕐, y validación de input manual.
- **Patrones de nado mejorados**:
  - **Tempo warping**: nueva capa 0 que deforma el tiempo para velocidad variable continua y monotónica. `tempoVariation`: 0 = constante (ballena), 0.60 = muy variable (cangrejo).
  - **Variación por instancia**: ±12% determinista sobre swimDuration, wobbleAmplitude, driftSpeed y tempoVariation. Dos criaturas de la misma especie nunca son idénticas.
  - **Márgenes simétricos**: basados en tamaño del emoji. El emoji siempre está completamente visible en ambos extremos.
  - **Velocidades ajustadas**: Clownfish 4800→7500ms (wobble 0.88→0.40), Shrimp 3200→5500ms, Dolphin 3800→6500ms, Squid 3000→5000ms.
  - **Crustáceos con profundidad**: verticalCoupling (0.15-0.20), tempoVariation alta (0.55-0.60), microWobble y driftAmplitude aumentados.
  - **Hitbox ampliado**: radio `maxOf(iconSize * 2.5f, 75f)`.
  - **Tap en vez de long press**: `onCreatureTap` reemplaza `onCreatureLongPress`.

---

## ✅ Sprint bugfixes 2

- **Emoji centrado**: `drawText` en Android Canvas dibujaba el emoji con el borde izquierdo en `x` y la baseline en `y`, desalineado respecto al hitbox y al punto de mirror. Fix: `textAlign = CENTER` + `drawY = y - (ascent + descent) / 2f`. Hitbox y flip de mirror ahora coinciden con el centro visual real.
- **Resumen nocturno con fecha correcta**: `NightSummaryWorker` usaba `Clock.System.now().date` en el momento de ejecución. Si el móvil estaba apagado a las 23:59, el Worker ejecutaba al día siguiente y procesaba el día incorrecto. Fix: la fecha objetivo se calcula en `schedule()` y se pasa como `InputData("targetDate")`. El Worker la lee al ejecutar.
- **summaryTime en processDay de inicio**: `MainActivity` llamaba `processDay(yesterday)` sin `summaryTime`, por lo que ninguna tarea OneTime era evaluable (filtro `summaryTime != null` fallaba) y el resumen de ayer contaba 0 tareas. Fix: se lee `nightTime` antes de la llamada y se pasa como `summaryTime`.

---

## ✅ Sprint visual — Superficie, fondo marino y flora Canvas

- **Superficie del agua**: ola animada en la parte superior (8% pantalla) con `Path` + `quadraticTo`. Zona sobre la ola con tono más claro. Cresta blanca sutil animada con `swayAngle`.
- **Fondo marino**: banda de arena con gradiente en la parte inferior (88% pantalla). 5 rocas decorativas con `Path`. Línea de transición agua→arena.
- **Gradiente invertido**: más claro arriba (luz del sol) → más oscuro en la profundidad. Color stops alineados con superficie y suelo.
- **AquariumBounds**: constantes compartidas `SURFACE_FRACTION` y `FLOOR_FRACTION` usadas por background y criaturas.
- **CreatureRenderer**: interfaz de renderizado extensible. `rendererFor(species)` despacha entre Canvas y emoji. Preparado para migrar todas las especies progresivamente.
- **Flora Canvas** (3 renderers):
  - **BrainCoralRenderer**: domos con crestas, cluster multi-domo a nivel 6+. Colores coral/rosa.
  - **AnemoneRenderer**: tentáculos con `quadraticTo`, ondulación interna con `animTimeMs`. 5→14 tentáculos según nivel. Puntos brillantes en puntas.
  - **KelpRenderer**: tallos con hojas alternas, ondulación creciente hacia la punta. Bosque multi-tallo a nivel 6+.
- **Anclaje al suelo**: flora, moluscos y decoraciones se anclan a `floorY`. Renderers dibujan hacia arriba desde la base.
- **Crustáceos en el suelo**: Lobster y Hermit Crab pasan de `SwimZone.LOWER` a `SwimZone.BOTTOM`. Shrimp se mantiene nadando en LOWER.
- **Y clamping global**: ninguna criatura nadadora atraviesa superficie ni suelo. Excepción: `SwimZone.SURFACE` (delfín, ballena azul) pueden saltar por encima de la línea de agua.
- **Plantas decorativas eliminadas**: las 8 plantas hardcodeadas del AquariumBackground se eliminan — la flora Canvas las reemplaza con vida y crecimiento.
- **Burbujas ajustadas**: nacen desde `floorY`, se desvanecen antes de `surfaceY`.

---

## ✅ Sprint visual 2 — Mejoras de ecosistema

- **Cielo dinámico por hora del día**: `skyForHour(hour)` devuelve `SkyColors(top, horizon)` para 7 periodos (noche, amanecer, mañana dorada, día, atardecer, crepúsculo, noche tardía). Usa `kotlin.time.Clock.System.now()`.
- **Olas más animadas**: amplitud aumentada a 9.dp, cresta secundaria a 60% de amplitud para efecto de profundidad, 8 segmentos con fase variable.
- **Fondo marino elaborado**: 11 rocas (3 grandes, 4 medianas, 4 pequeñas) con 3 estilos distintos (suave, anguloso, irregular). Líneas de textura de arena con `quadraticTo`. Sombra de transición agua→arena.
- **Tamaños de peces ajustados**: `sizeMultiplier` por especie (Clownfish 0.72, Angelfish 0.74, MantaRay 1.45).
- **Flora con múltiples instancias**: `instanceCount` por especie (BrainCoral=4, Anemone=3, Kelp=4). Distribuidas uniformemente en 5-95% del ancho. Animación desfasada por instancia (+5000ms).
- **Anémona anclada al suelo**: flora Canvas usa `y = floorY` como base fija; el renderer dibuja hacia arriba con animación interna.
- **Tamaño de flora escalado**: Canvas renderers usan `iconSize * 3.2f` como tamaño efectivo.
- **Crustáceos diferenciados en altura**: Lobster en `personalYFraction=0.95` (pegado al suelo), Hermit Crab en `personalYFraction=0.30` (algo más arriba). Heightoffset calculado sobre banda de 6% de pantalla.
- **`CreatureIcon` composable**: reutilizable para EcosystemScreen y CreatureDetailDialog. Canvas animado para flora (anemone ondeante, kelp meciéndose, coral estático). Emoji escalado al tamaño de caja para otras especies.
- **EcosystemScreen**: botón de retroceso `←` en el header. Usa `CreatureIcon(52.dp)` en cards desbloqueadas.
- **CreatureDetailDialog**: usa `CreatureIcon(80.dp)` en lugar de emoji estático `56.sp`.
- **Opacidad de tarjetas de tarea**: `CardBackground` de `0x33` → `0x55` para mayor legibilidad sobre el fondo marino.

---

## ✅ Sprint lootbox — Sistema de rareza y desbloqueo aleatorio

- **Sistema de rareza**: `CreatureRarity` enum (COMMON 40%, UNCOMMON 30%, RARE 20%, EPIC 8%, LEGENDARY 2%) con pesos de probabilidad normalizados entre candidatos.
- **40 especies**: 16 nuevas especies añadidas (Surgeonfish, Lionfish, Sunfish, Posidonia, Fan Coral, Spider Crab, Barnacle, Nautilus, Giant Clam, Hammerhead, Barracuda, Cuttlefish, Blue-Ringed Octopus, Marine Iguana, Sea Otter, Manatee). Total: 40 especies en 9 categorías.
- **Lootbox mechanic**: `CATEGORY_UNLOCK_LEVELS` define qué niveles de categoría producen lootbox. `PendingLootbox(category, categoryLevel)` reemplaza el sistema fijo de `unlockLevel` por especie.
- **`LootboxResolver`**: selección weighted-random ejecutada al ABRIR la lootbox (no al ganarla). Normaliza pesos entre especies aún bloqueadas de la categoría.
- **XP overflow**: cuando una categoría tiene todas sus especies desbloqueadas, 50% del XP se queda (sube nivel + XP a criaturas), 50% se redistribuye a la categoría de menor nivel no completa (excluyendo DECORATION). Parámetro `fromOverflow` evita recursión infinita.
- **Diálogo lootbox bifásico** (`LootboxDialog` en `MainScreen.kt`):
  - Fase 1 (cerrada): 🎁 + nombre de categoría + nivel + botón "Abrir 🎁"
  - Fase 2 (abierta): emoji de especie + displayName + badge rareza con color + input nickname + "Bienvenido al estanque 🌊"
- **EcosystemScreen overhaul**:
  - Ordenamiento por rareza (COMMON→LEGENDARY), desbloqueados primero
  - Barra de progreso por categoría hacia siguiente lootbox (reemplaza barras individuales)
  - Badge de rareza (punto de color) en cards desbloqueadas y bloqueadas
  - Niveles numéricos "Nv. X" reemplazando dots
  - Cards bloqueadas: emoji al 10% opacity + "???" + punto de rareza tenue
- **CreatureDetailDialog**: badge de rareza con color bajo el nombre de especie, niveles numéricos
- **DataStore**: formato lootbox `"FISH:4|CRUSTACEAN:6"` reemplaza formato emoji. Migración legacy automática.
- **NightSummaryProcessor**: adaptado para guardar `List<PendingLootbox>` en vez de emojis.
- **`CreatureSpec` simplificado**: `unlockLevel` eliminado, `rarity: CreatureRarity` añadido. `baseSize` fijo `28f * 1.2f`.
- **`specBySpecies`**: mapa lazy `Map<CreatureSpecies, CreatureSpec>` reemplaza `emojiToSpecies`.
- **Rotación de crustáceos**: dirección controlada por negación de rotación (no mirror) cuando `emojiRotation != 0`.
- **Manta ray**: `sizeMultiplier` 1.45→2.4 en acuario.

---

## ✅ Sprint i18n + renderers — Traducciones y fauna Canvas

- **Sistema de traducciones (i18n)**: `composeResources/values/strings.xml` (inglés, por defecto) + `values-es/strings.xml` (español). Todos los textos hardcodeados de la UI reemplazados por `stringResource(Res.string.key)`. El idioma se detecta automáticamente del dispositivo.
- **`LocalizationExtensions.kt`**: `localizedDays()` @Composable (días de la semana localizados), `CreatureSpecies.displayNameRes()`, `MarineCategory.displayNameRes()`, `CreatureRarity.displayNameRes()` — extension functions que devuelven `StringResource`.
- **13 nuevos renderers Canvas** para especies con emoji inadecuado o compartido:
  - **Swimmers** (`fauna/`): `SurgeonfishRenderer` (azul cobalto, cola amarilla, escalpelo), `LionfishRenderer` (espinas dorsales en abanico, aletas pectorales enormes), `SunfishRenderer` (disco circular, clavus ondulado, aletas enormes), `HammerheadRenderer` (cabeza en T, ojos en los extremos), `BarracudaRenderer` (cuerpo 3× elongado, mandíbula prominente), `ManateeRenderer` (cuerpo patata, cola paleta, arrugas), `SpiderCrabRenderer` (caparazón pequeño, 10 patas larguísimas articuladas), `CuttlefishRenderer` (falda de aletas ondulantes, pupila en W), `BlueRingedOctopusRenderer` (16 anillos azules pulsantes)
  - **Fondo fijo** (`fauna/`): `SeaUrchinRenderer` (semiesfera con 30 espinas radiales, 5 bandas), `BarnacleRenderer` (cluster de volcanes, cirros animados nivel 3+)
  - **Flora** (`flora/`): `PosidoniaRenderer` (cintas de hierba marina oscilantes), `FanCoralRenderer` (árbol bifurcado recursivo, malla nivel 3+, pólipos nivel 5+)
- **Emojis actualizados** en `AquariumCreature.kt`: SURGEONFISH 🐟→🐠, HAMMERHEAD 🔨→🦈, SEA_URCHIN 🐚→🌑

---

## ✅ Sprint onboarding

- **Flujo de primera vez** (`OnboardingScreen.kt`): 4 pasos con `AnimatedContent` (slide+fade), paleta marina, indicador de puntos.
  - Paso 1 — Bienvenida
  - Paso 2 — Cómo funciona
  - Paso 3 — El ecosistema marino
  - Paso 4 — Listo para empezar
- **Persistencia**: `hasCompletedOnboarding()` / `setOnboardingCompleted()` en `UserPreferencesRepository` vía DataStore.
- **Navegación**: `App.kt` decide `startDestination` según el valor del DataStore. Hasta que emite, el `NavHost` no se crea (`collectAsState(initial=null)` con guard). Al completar, navega a `ROUTE_MAIN` sacando el onboarding del backstack.
- **`ROUTE_ONBOARDING`** añadido a `Navigation.kt`.

---

## ✅ Sprint notificaciones push

- **Canales de notificación**: `NotificationHelper` con 3 canales (`night_summary`, `morning_reminder`, `task_reminder`). `createChannels()` crea los canales Android O+.
- **Permiso `POST_NOTIFICATIONS`** declarado en `AndroidManifest.xml` (Android 13+). Solicitado en `MainActivity` en API 33+.
- **Push resumen nocturno**: `NightSummaryWorker` envía notificación con stats (`X de Y tareas completadas`) tras `processDay`. Se auto-reprograma para el día siguiente.
- **Aviso matutino configurable**: toggle + selector de hora en el Drawer (sección Ajustes). `MorningReminderWorker` (auto-reprogramado diariamente). Hora almacenada en DataStore (`morning_reminder_hour/minute`, centinela -1 = desactivado). Sin configurar = sin notificación.
- **Campo `notificationsEnabled`** en `DayTask` y `RecurringTaskDef`. Migración Room v9→v10. `RecurringTaskGenerator` propaga el valor de def a instancias generadas.
- **`TaskReminderScheduler`** (interfaz commonMain) + **`TaskReminderSchedulerImpl`** (WorkManager, `ExistingWorkPolicy.REPLACE`, `rescheduleAll()` al arrancar).
- **`TaskReminderWorker`**: one-shot a la hora exacta de la tarea. Usa `task_reminder_$taskId` como nombre único.
- **`MainViewModel`**: inyecta `TaskReminderScheduler`; programa/cancela en add, edit, postpone, delete, complete.
- **`MainActivity`**: `createChannels()`, permiso API 33+, `rescheduleAll()` tras seeding.
- **Toggle en `TaskFormSheet`**: visible solo si hay hora configurada; se resetea al borrar la hora.
- **Strings EN + ES**: `androidMain/res/values/strings.xml` (R.string canales + contenido) y `values-es/strings.xml` (traducción española), `composeResources` para UI.

---

## ✅ Sprint estadísticas + historial

- **Data layer**: `BlockStreakDao.getAll()`, `DaySummaryDao.getRange(from, to)`, `DayTaskDao.getCompletedRange(from, to)`. Interfaces de repositorio extendidas. Implementaciones en androidMain.
- **`StatsScreen`**: gráfico de barras Canvas coloreado por % completado (gris→rojo→ámbar→teal→azul), toggle Semana/Mes, etiquetas de día localizadas (iniciales de día para WEEK, cada 5 días para MONTH), tarjetas de resumen (días activos, completadas, mejor día), rachas activas por bloque ordenadas.
- **`HistoryScreen`**: LazyColumn de días en orden descendente, selector de rango 30/60/90 días, badge completadas/totales, icono ✓/✗ por tarea, bloque coloreado por punto, meses localizados EN/ES.
- **`StatsViewModel`** + **`HistoryViewModel`**: `selectRange()` reactivo, rango configurable en UiState.
- **`StatsViewModelFactory`** + **`HistoryViewModelFactory`** en androidMain.
- **Navigation**: `ROUTE_STATS = "stats"`, `ROUTE_HISTORY = "history"` integrados en `mainGraph`.
- **`MainDrawer`**: sección PROGRESO con botones "Estadísticas" e "Historial".
- **Strings EN + ES**: meses abreviados (`month_jan`…`month_dec`), `history_range_days`, `history_no_data` parametrizados, sección drawer, títulos y etiquetas de pantallas.

---

## ✅ Sprint B — Recompensas de racha

- **`BlockStreakUpdate`**: data class `(currentStreak: Int, milestonesReached: List<Int>)`. `STREAK_MILESTONES = [7, 14, 30]`.
- **`BlockStreakProcessor.processDay()`**: devuelve `Map<String, BlockStreakUpdate>` (antes `Map<String, Int>`). Detecta hitos cruzados en el rango `(oldStreak+1)..newStreak`.
- **`NightSummaryProcessor`**: adaptado al nuevo tipo. Genera `PendingLootbox(category, milestone)` por cada hito alcanzado en cualquier bloque. Combinado con `ecosystemLootboxes` en `newLootboxes`.
- **`blockCategories`** map: construido desde las categorías asignadas a cada bloque para mapear `blockId → MarineCategory`.

---

## ✅ Sprint A — Evolución visual de criaturas

Efectos discretos por nivel añadidos a los 10 renderers de fauna que solo tenían escalado continuo:

| Renderer | Nivel 3+ | Nivel 5+ |
|---|---|---|
| `DolphinRenderer` | Franja lateral amarilla | Brillo iridiscente dorsal (sin animado) |
| `SeaTurtleRenderer` | 5 parches de algas en el caparazón | 5 círculos de barnáculos en el borde frontal |
| `WhaleSharkRenderer` | Rémora compañera (oval + ventosa) | 7 fotóforos bioluminiscentes pulsantes |
| `SquidRenderer` | 4 fotóforos ventrales con brillo | Bordes luminiscentes en aletas laterales |
| `SealRenderer` | 7 manchas de foca de puerto | Silla dorsal oscura (cubicTo) |
| `SeaUrchinRenderer` | 10 puntas de espina fluorescentes (pulso) | 5 manchas inter-radiales |
| `NautilusRenderer` | Overlay de nácar iridiscente (70% concha) | Círculo naranja en el borde frontal |
| `HermitCrabRenderer` | Anémona simbiótica en la concha (6 tentáculos animados) | Overlay dorado en la concha |
| `ShrimpRenderer` | Franja dorsal blanca de limpiador | Brillo iridiscente del caparazón |
| `SpiderCrabRenderer` | 4 blobs de algas/esponja | 4 blobs de esponja naranja adicionales |

- **Fix `HermitCrabRenderer`**: añadido `import kotlin.math.cos` (faltaba antes de los tentáculos).

---

## ✅ Sprint C — Tests unitarios automatizados

- **Dependencia**: `kotlinx-coroutines-test:1.10.2` en `commonTest` (libs.versions.toml + build.gradle.kts).
- **Fakes in-memory** (sin librería de mocking):
  - `FakeDayTaskRepository` — lista mutable, implementa `getByDate`, `getByDateAndBlock`, `updateStatus`, `insert`, `update`, `delete`
  - `FakeBlockStreakRepository` — mapa `blockId→BlockStreak`, implementa 4 métodos, expone `getAll_snapshot()`
  - `FakeDaySummaryRepository` — mapa `LocalDate→DaySummary`, expone `inserted()`
- **37 tests — 0 fallos**:
  - `EcosystemLevelCalculatorTest` (16): `xpForLevel` base, `levelForXp` inversa, estrictamente creciente, `nightBonus` por umbrales (0 / 0.39 / 0.40 / 0.70 / 1.0 / con racha), constante `XP_PER_TASK`
  - `BlockStreakProcessorTest` (12): primera finalización, días consecutivos/no-consecutivos, neutral sin tareas, reset a 0, hitos 7/14/30, sin falso positivo, hito ya pasado, independencia multi-bloque, idempotencia mismo día
  - `NightSummaryProcessorTest` (9): no-op ya procesado, no-op sin tareas, score=1 todos completados, score=0.5 parcial, exclusión pospuestas, sin tareas evaluables, PENDING→EXPIRED, blockId nulo contado, score=0 todos pendientes

---

## ✅ Sprint Lucide Icons — Iconografía minimal

- **18 Vector Drawables** (`composeResources/drawable/ic_*.xml`): `pencil`, `trash`, `clock`, `plus`, `x`, `arrow_left`, `bar_chart`, `history`, `bell`, `lock`, `waves`, `fish`, `flame`, `gift`, `moon`, `sun`, `calendar`, `box`. Formato stroke-based, viewport 24×24, `strokeWidth=2`, `strokeLineCap/Join=round`, `fillColor` transparente — compatibles con `Icon(painter, tint=…)`.
- **`strings.xml` (EN + ES)**: eliminado emoji de `menu_edit`, `menu_postpone`, `menu_delete`, `msg_streak`.
- **`MainDrawer.kt`**: header 🌊→`ic_waves`, ítems de navegación `ic_fish/ic_bar_chart/ic_history`, ajustes `ic_moon/ic_sun`. `DrawerItem` acepta `Painter` en vez de `String`.
- **`MainScreen.kt`**: `HeaderIconButton` acepta `Painter` (⟳→`ic_history`, 📅→`ic_calendar`, ➕→`ic_plus`, ☰→`ic_waves`). FAB close ✕→`ic_x`, FAB acuario 🐟→`ic_fish`. Streak badge 🔥→Row con `ic_flame`. ⏰/⌛ status→`ic_clock`. Sección sin bloque 📋→`ic_box`. `ContextMenuItem` con `painter?` opcional (lápiz/reloj/papelera).
- **`TaskFormSheet.kt`**: 🔔×2 → Row `Icon(ic_bell)` + Text.
- **`StatsScreen.kt`**: 🔥 → Row `Icon(ic_flame)` naranja + número.
- **`EcosystemScreen.kt`**: 🔒 → `Icon(ic_lock, size=11.dp)`.

---

## ✅ Sprint Logo & Branding

- **Concepto "Rising Currents"**: tres corrientes ascendentes con ondulación de ola — simbolizan impulso constante, no empujón aislado. Todas fluyen en la misma dirección (arriba-derecha) con cresta y valle visible.
- **Adaptive Icon Android**: foreground vector (3 olas blancas stroke-based, 108dp viewport) + background (azul océano #1565C0 con zona superior #1E88E5)
- **Logo in-app** (`ic_riptide_logo.xml`): versión 24dp del brand mark para headers
- **Icono de notificación** (`ic_notification.xml`): versión monocroma para la barra de estado
- **Splash screen**: `core-splashscreen` 1.0.1, tema `Theme.Riptide.Splash` con fondo #1565C0 y adaptive icon, `installSplashScreen()` en `MainActivity`
- **Header con logo real**: `MainScreen` + `MainDrawer` usan `ic_riptide_logo` en header; `ic_waves` queda solo para el botón del drawer
- Feature graphic 1024×500 → diferido a Fase 3 (Store Prep)

---

## ✅ Sprint Widget Android

- **Widget Glance** (`RiptideWidget`): tareas del día agrupadas por bloque con color, barra de progreso completadas/total, indicador de estado (punto verde completada / gris pendiente), hora de la tarea, nombre del bloque coloreado.
- **`RiptideWidgetReceiver`**: broadcast receiver registrado en AndroidManifest.
- **Metadata** (`riptide_widget_info.xml`): 3×3 celdas, redimensionable, actualización cada 30 min.
- **Layout de carga** (`widget_loading.xml`): fondo oceánico con "Riptide" mientras carga.
- **`WidgetUpdater`**: utility para refrescar todos los widgets desde la app. Se ejecuta en `onResume` de `MainActivity` via `repeatOnLifecycle`.
- **Paleta marina**: fondo `OceanDeep`, barra de progreso `Accent`/`CompletedGreen`, textos blancos — consistente con la app.
- **Dependencias**: `androidx.glance:glance-appwidget:1.1.1` + `glance-material3`.
- **Strings EN + ES**: `widget_description`, `widget_no_tasks`, `widget_all_done`.

---

## 🔨 Sprint Live Wallpaper — Acuario como fondo de pantalla

- **`CanvasDrawScope` bridge**: reutiliza todo el renderizado Compose (`drawAquariumBackground`, `drawAquariumCreatures`) desde un `WallpaperService` Android, sin portar código.
- **Refactor puro de extracción**: funciones `DrawScope.drawAquariumBackground()` y `DrawScope.drawAquariumCreatures()` extraídas de los composables para uso compartido app↔wallpaper.
- **`RiptideWallpaperService`**: `WallpaperService` + `Engine` con `Choreographer` a 60fps. Cielo dinámico por hora, gradiente oceánico, fondo marino, burbujas, criaturas desbloqueadas nadando.
- **`WallpaperDataProvider`**: lee criaturas desbloqueadas de Room DB. Refresco periódico cada 5 min.
- **Registro en AndroidManifest**: servicio con `BIND_WALLPAPER`, metadata XML, strings EN/ES.

---

## 🔨 Fase 1 — Producto completo

### ✅ Sprint Backend

- **Proyecto Ktor 3.0.3** en `/backend` — proyecto Gradle independiente con fat JAR
- **PostgreSQL + Exposed 0.57.0** — mirror de Room v11 (9 tablas) + tabla `users` + campos `updatedAt`/`isDeleted` para sync
- **API REST completa** — 8 recursos CRUD: `/api/blocks`, `/api/tasks`, `/api/recurring-defs`, `/api/creatures`, `/api/ecosystem-states`, `/api/summaries`, `/api/streaks`, `/api/block-categories`
- **Autenticación Google Sign-In** — `POST /auth/google` (verificación idToken via `GoogleIdTokenVerifier`) → JWT (access 24h + refresh 30d). `POST /auth/refresh` para renovar.
- **Sync-ready** — todos los GET soportan `?updatedSince=ISO` para pull incremental. Soft delete con `isDeleted`.
- **Health check** — `GET /health` público
- **Docker** — Dockerfile multi-stage (Gradle build → JRE Alpine)
- **Hosting**: Railway (pendiente de deploy)

### ✅ Sprint Sync

- **Room v12**: `updatedAt TEXT` en las 8 tablas + `isDeleted INTEGER` en 3 (WorkBlock, DayTask, RecurringTaskDef). `MIGRATION_11_12` con 11 ALTER TABLE. Todos los DAOs con `getModifiedSince()`, `upsertAll()`, `stampUpdatedAt()`. Queries existentes filtran `isDeleted = 0`. Soft-delete en repos.
- **Ktor Client**: OkHttp engine, ContentNegotiation JSON, Bearer Auth con auto-refresh, HttpTimeout 60s. `BuildConfig.API_BASE_URL` configurable.
- **DTOs Android**: 8 DTOs de recurso (mirror de backend ApiModels), `SyncRequest`, `SyncResponse`, auth DTOs. `DtoMappers.kt` con 16 funciones Entity↔DTO.
- **Backend `POST /api/sync`**: batch endpoint que recibe todos los datos dirty del cliente y devuelve todos los cambios del servidor. Upsert con conflict resolution (`updatedAt` wins), `isDeleted` en servidor es autoritativo, `hasBeenRewarded` usa OR lógico. Orden FK-safe.
- **Auth Google Sign-In**: `AuthManager` con `getSignInIntent()` / `handleSignInResult()` / `logout()`. JWT pair + user info en DataStore. `DataStoreTokenProvider` puente entre prefs y Ktor Auth.
- **SyncManager**: push dirty → single POST /api/sync → pull server changes → upsert local (respetando `hasBeenRewarded` OR-merge). `SyncTrigger` con debounce 5s. `SyncWorker` periódico (1h, constraint CONNECTED). `InitialSyncPreparer` para primera sync.
- **UI**: sección "Cuenta" en drawer (avatar con inicial, nombre, email, "Sincronizar ahora" con status badge, "Cerrar sesión"). `SyncStatus` enum (IDLE/SYNCING/SUCCESS/ERROR/OFFLINE). 3 iconos Lucide nuevos (user, log-out, refresh-cw). Strings EN + ES.
- **Conectividad**: `ConnectivityObserver` con `ConnectivityManager.NetworkCallback` + `Flow<Boolean>`.

---

## Fase 2 — Calidad

### Sprint QA & Testing

- Tests de integración (flujos completos con fake backend)
- Edge cases UX (sin internet, primer uso, migración datos)
- Accesibilidad (`contentDescription`, contraste WCAG, touch targets 48dp)
- Ampliar cobertura de unit tests (repositories, sync logic)

### Sprint Polish

- Performance audit (recomposiciones innecesarias, lazy lists)
- Animaciones de transición entre pantallas
- Haptic feedback en interacciones clave
- Bug fixing final

---

## Fase 3 — Lanzamiento Play Store

### Sprint Store Prep

- Keystore + release build + ProGuard/R8
- Privacy policy + Terms of Service (hosted)
- Screenshots (6+ pantallas, EN + ES)
- Store listing (descripción, categoría, tags)
- Internal testing track → closed beta → production
- Cuenta de desarrollador Google Play (25$ one-time)

### 🚀 PUBLISH PLAY STORE

---

## Fase 4 — iOS (post-launch, sin prisa)

### Sprint iOS

- `expect/actual` reales: Room → alternativa iOS, WorkManager → `BGTaskScheduler`
- Pickers nativos iOS (`TimePickerDialogWrapper`, `DatePickerDialogWrapper`)
- Notificaciones locales (`UNUserNotificationCenter`)
- `NightSummaryScheduler` con background tasks iOS
- Apple Developer Account (99$/año)
- App Store listing + review

---

## Fase 5 — Post-launch

- Social: visitar el estanque de un amigo (solo ver, nunca competir)
- Iteraciones según feedback real de usuarios

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual
