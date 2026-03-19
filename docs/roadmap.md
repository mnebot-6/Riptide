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
