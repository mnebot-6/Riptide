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

## v3 — Revisión, pulido y onboarding

- Testing automatizado (dominio: EcosystemProcessor, NightSummaryProcessor, EcosystemLevelCalculator) + manual de flujos principales
- Animación de entrada de criatura al desbloquearse (nada desde el borde)
- Revisión de gestos, formularios y edge cases del resumen nocturno
- Preparar firma de la app para distribución
- Tutorial de primera vez (onboarding al primer inicio)
  - `hasCompletedOnboarding` en `UserPreferencesRepository` (DataStore)
  - Pantalla fullscreen con fondo marino, pasos swipeables
  - Explica bloques, tareas, ecosistema, resumen nocturno
  - Termina creando el primer bloque o salta al DataSeeder

---

## v4 — Fondo de pantalla dinámico

- Live wallpaper del acuario (WallpaperService Android)

---

## v5 — Backend y social

- Backend Ktor + PostgreSQL
- Sincronización offline-first (IDs UUID ya preparados)
- Perfiles de usuario
- Google Sign-In
- Visitar el estanque de un amigo (solo ver, nunca competir)

---

## v6 — iOS completo

- Implementación real de `AquariumCreature.ios.kt`
- Pickers nativos iOS (`TimePickerDialogWrapper`, `DatePickerDialogWrapper`)
- `NightSummaryScheduler` con notificaciones locales iOS

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual
