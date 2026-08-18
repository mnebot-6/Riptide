# Plan de simplificación de la lógica de negocio

> Decidido en la sesión del 2026-08-18, a partir de
> [estado-logica-negocio.md](estado-logica-negocio.md).
> Room actual: **v15**. Este plan lleva a **v16**.

---

## 0. La regla única de la que cuelga todo

```
score del día = tareas completadas / tareas del día
día conseguido = score >= 0.80
```

- **Sin ponderación.** Prioridad deja de afectar al score (sigue existiendo como
  ordenación visual).
- **Contable = tarea normal.** Ya se auto-completa al llegar a `targetCount`
  ([MainViewModel.kt:202](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L202)),
  así que no necesita tratamiento especial: o está `COMPLETED` o no.
- **Día sin tareas = neutral.** Se guarda `DaySummary` con `tasksTotal = 0` y la
  racha lo **salta** (ni suma ni rompe). Coherente con "el ecosistema espera".

De aquí cuelgan racha, decoraciones, XP y mensajes. Nada más define "buen día".

---

## 1. Decisiones tomadas

| # | Decisión | Consecuencia |
|---|---|---|
| D1 | Cierre del día **fijo a las 23:59:59**, sin hora configurable | Desaparece el filtro de "evaluables" y el dilema de las tareas sin hora |
| D2 | `score = completadas / total` | Se borra `computeScore` + `ScoreInput` |
| D3 | Umbral de día conseguido: **0.80** (constante única) | Racha, decoraciones y bonus usan el mismo número |
| D4 | Día sin tareas = neutral, no rompe racha | Fin de los huecos de racha por días vacíos |
| D5 | **Una sola racha**, derivada de `DaySummary` | Se elimina la entidad `BlockStreak` entera |
| D6 | `TaskStatus = PENDING \| COMPLETED \| EXPIRED` | Fuera `POSTPONED` y `CANCELLED` |
| D7 | Solo se puede completar/descompletar **hoy** | Pasado y futuro: lectura + edición de contenido, no de estado |
| D8 | XP: bloque → sus categorías, **80/20 spillover** a la menos avanzada | Fuera overflow y XP por criatura |
| D9 | Recurrencias: **Weekly + MonthlyDay + Yearly** | Fuera `NthWeekdayOfMonth` e `intervalMonths` |
| D10 | Categorías de bloque se asignan **una vez** al crearlo | Fuera la reasignación round-robin global |

### El dilema de la tarea sin hora (punto 3) queda resuelto por D1

Con el cierre a las 23:59:59 no hay ninguna tarea "que aún tiene horas por
delante": el día ha terminado. **Todas las tareas del día se evalúan en el cierre
de ese mismo día**, tengan hora o no. La hora de la tarea pasa a servir solo para
dos cosas: ordenar la lista y disparar el recordatorio push. Ya no participa en
la contabilidad.

### Por qué "una sola racha" no quita nada al sistema (punto 2)

El usuario ve **una** llama. Internamente sigue habiendo incentivos:

- Hitos 7 / 14 / 30 días → lootbox (cuelgan de la racha global, no de bloques).
- "Bloque entero completado" → bonus de XP en el cierre, **sin estado
  persistente**: se calcula del día que se está cerrando y se olvida.

Incentivo sí; entidad que mantener, no.

### XP impredecible (punto 5) — mi lectura

De acuerdo con mantener la sorpresa, pero moviéndola de sitio. Hoy hay dos
capas impredecibles encadenadas y eso es lo que rompe el sistema:

- **Impredecible bueno**: *qué especie* sale de la lootbox. Es el momento de
  premio, la aleatoriedad se disfruta.
- **Impredecible malo**: *cuánto crece cada categoría* al completar una tarea.
  Rompe la relación causa→efecto ("hoy hice mucho y no vi nada crecer"), y sobre
  todo **impide depurar**: si el XP no cuadra, no hay forma de saber si es un bug
  o el spillover aleatorio.

Propuesta: contabilidad determinista, premio aleatorio.

```
10 XP por tarea
 └─ 8 XP repartidos entre las categorías del bloque
 └─ 2 XP a la categoría desbloqueada con menos XP total   ← spillover que pides
```

Dos reglas, ninguna recursiva, ningún flag anti-bucle. La sorpresa vive entera
en `LootboxResolver`.

### Recurrencias que dejaría (D9)

| Tipo | ¿Se queda? | Razón |
|---|---|---|
| `Weekly(slots)` | **Sí** | Cubre el 95% del uso real |
| `MonthlyDay(día)` | **Sí**, sin `intervalMonths` | "El 1 de cada mes": facturas, revisiones |
| `Yearly(mes, día)` | **Sí** | Cumpleaños. Cuesta 1 línea de lógica |
| `NthWeekdayOfMonth` | **No** | 18 líneas + el picker más confuso de la app. "El primer domingo" se expresa con `MonthlyDay` aproximado o como tarea puntual |
| `intervalMonths` | **No** | "Cada 3 meses" no lo pide nadie y multiplica los casos de prueba |

`Recurrence.None` se queda solo para bloques sin horario.

---

## 2. Fases de ejecución

Ordenadas por dependencia. Cada fase deja la app compilando y con tests verdes.

### Fase 1 — Núcleo del día (base de todo)
**~3 h**

1. `NightSummaryProcessor.processDay`: borrar el filtro `evaluable`. Todas las
   tareas de la fecha que no estén borradas entran.
2. Borrar `computeScore` y `ScoreInput`. `score = completed.toFloat() / total`.
3. Si `total == 0` → insertar `DaySummary(total = 0, score = 0)` igualmente
   (día neutral) en lugar de `return`.
4. Hora de cierre: constante `DAY_CLOSE_TIME = LocalTime(23, 59, 59)`.
   `NightSummaryWorker.schedule()` deja de leer preferencias.
5. **Catch-up**: al abrir la app, cerrar todos los días desde el último
   `DaySummary` hasta ayer. El reloj es el mecanismo principal (D1); esto es la
   red de seguridad para móvil apagado / Doze.
6. Borrar `nightSummaryTime` de: `UserPreferencesRepository`,
   `NightSummaryScheduler` (+ `.android`/`.ios`), `SettingsScreen`
   (`NightSummaryTimeSetting`), `Navigation.kt`, `MainViewModel`.
7. Actualizar `NightSummaryProcessorTest`.

> **Push nocturno**: a las 23:59 nadie la lee. El resumen pasa al push matutino
> (`MorningReminderWorker`, ya existe) + al diálogo que ya se muestra al abrir la
> app. Se elimina el envío desde `NightSummaryWorker`. Ver §4 si prefieres
> conservarlo.

### Fase 2 — Estados de tarea
**~4 h** · Room **v15 → v16**

1. `TaskStatus` → `PENDING | COMPLETED | EXPIRED`.
2. `postponeTask` → cambia la fecha de la fila existente. Sin copia, sin
   `POSTPONED`. Reprograma el recordatorio.
3. `deleteRecurringTaskInstance` → `isDeleted = 1` (el soft delete de sync ya
   existe) en vez de `CANCELLED`.
4. `MIGRATION_15_16`:
   - `UPDATE day_tasks SET status='PENDING' WHERE status='POSTPONED'`
     (las copias huérfanas quedan como duplicados históricos: aceptable)
   - `UPDATE day_tasks SET isDeleted=1 WHERE status='CANCELLED'`
   - `DROP TABLE block_streaks` (ver Fase 3)
   - eliminar columna `streakDay` de `day_summaries` (recrear tabla)
5. Backend: mismos valores en `DayTasksTable`, `BlockStreaksTable` fuera.

### Fase 3 — Racha única derivada
**~3 h**

1. Borrar: `BlockStreak`, `BlockStreakProcessor`, `BlockStreakRepository`(+Impl),
   `BlockStreakDao`, `BlockStreakEntity`, `BlockStreakMapper`, DTO y ruta backend.
   *(21 ficheros tocados — es la poda más grande del plan.)*
2. Nueva función pura en `domain`:
   ```kotlin
   fun streakFrom(summaries: List<DaySummary>, upTo: LocalDate): Int
   // días consecutivos hacia atrás con score >= 0.80; salta los de total == 0
   ```
   Sustituye a `calculateGlobalStreak` y a `countConsecutivePerfectDays`.
3. Hitos 7/14/30: se detectan en el cierre comparando la racha antes y después
   de insertar el `DaySummary` de hoy. Sin estado.
4. Bonus "bloque completo": en el cierre, `+5 XP` por bloque cuyas tareas del día
   estén todas completadas. Calculado y olvidado.
5. UI: quitar el badge de racha de la cabecera de bloque
   ([MainScreen.kt](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainScreen.kt)),
   quitar la sección "rachas por bloque" de `StatsScreen`. La llama del header se
   queda como única racha.
6. `DecorationUnlockChecker`: `perfectDaysStreak` y `longestPerfectStreak` pasan
   a usar `streakFrom`. TREASURE_CHEST = racha 7, CORAL_THRONE = racha 14.

### Fase 4 — Solo lectura fuera de hoy
**~2 h**

1. `MainUiState`: `isEditable = selectedDate == today`.
2. `MainScreen`: checkbox, +/-, timer y estrella deshabilitados si `!isEditable`.
   Crear / editar / borrar / posponer siguen permitidos en cualquier fecha.
3. `toggleTaskCompleted`: borrar la rama `summaryExists → EXPIRED`. Descompletar
   solo existe para hoy y siempre devuelve a `PENDING`. Igual en
   `decrementTaskCount`.
4. Barrido de XP de `loadDay`: ejecutar **solo** si `date == today`. Arregla de
   paso la fuga de XP del widget si además se ejecuta en el cierre del día.

### Fase 5 — XP predecible
**~3 h**

1. `EcosystemProcessor.addXpForTask`: 80% repartido entre las categorías del
   bloque, 20% a la categoría desbloqueada con menos XP total. Borrar
   `redistributeOverflow`, `findSpilloverTarget` (queda una versión trivial) y
   los flags `fromOverflow` / `fromSpillover`.
2. Borrar el reparto de XP por criatura. `MarineCreature.experience` y
   `creatureLevel` dejan de escribirse; el nivel visual se **deriva**:
   ```kotlin
   creatureLevel = (1 + (categoryLevel - unlockedAtLevel) / 2).coerceIn(1, 5)
   ```
   Las criaturas siguen evolucionando (nivel 3+ y 5+ en los renderers) sin un
   segundo sistema de XP que mantener.
3. `MarineCategoryAssigner`: llamarlo solo para bloques **sin** categorías
   asignadas. Quitar las llamadas desde `updateBlockAndReassign` y
   `deleteBlockAndReassign`.
4. Actualizar `EcosystemProcessorSpilloverTest`.

### Fase 6 — Calendario, recurrencias y limpieza
**~4 h**

1. **Arreglar la semana**: `loadDay` carga el rango `[lunes, domingo]` de la
   semana visible. Nuevo `DayTaskDao.getByDateRange(from, to)`. `MainUiState`
   añade `tasksByDate`; `MainHeader` lo consume en vez de derivarlo de
   `tasksByBlock`.
2. **Generación sin huecos**: guardar `lastGeneratedDate`. `generateUpTo` corre
   desde `lastGeneratedDate` (no desde hoy) hasta hoy+7, y también en el cierre
   nocturno. Un móvil apagado 4 días recupera esos 4 días al volver.
3. **Recurrencias a 3 tipos** (D9): borrar `NthWeekdayOfMonth` e
   `intervalMonths` del sealed class, del `TaskFormSheet` y del
   `BlockFormScreen`. Migrar las existentes a `MonthlyDay`.
4. **Editar recurrente sin destruir**: en vez de borrar y regenerar, actualizar
   in-place las instancias `PENDING` futuras conservando `notes`, `currentCount`
   e `isPriority`.
5. **Borrar código muerto**: `TaskSchedule.Recurring` (5 ramas `when`),
   `DaySummary.streakDay`, `DayTaskRepository.getPendingBefore`.

---

## 3. Resultado esperado

| | Antes | Después |
|---|---|---|
| Estados de tarea | 5 | 3 |
| Definiciones de racha | 4 | 1 |
| Métricas de "día hecho" | 2 | 1 |
| Entidades mutables | 6 | 4 |
| Reglas de reparto de XP | 4 (2 aleatorias) | 2 (0 aleatorias) |
| Tipos de recurrencia | 5 | 3 |
| Ajustes en Settings | +1 hora de resumen | — |
| Puntos de concesión de XP | 4 | 2 (completar + cierre) |

**Total estimado: 19 h** (≈ 3 sesiones de trabajo). Fases 1–3 son las que dan el
90% de la claridad; 4–6 son consolidación.

---

## 4. Única decisión abierta

**¿Se conserva la notificación push nocturna?**

Con el cierre a las 23:59:59, enviarla ahí es enviarla a nadie.

- **Recomendado (aplicado por defecto en el plan)**: se elimina el push nocturno.
  El resumen del día anterior llega en el push matutino que ya existe, y el
  diálogo de resumen al abrir la app sigue funcionando igual. Un worker menos,
  un canal de notificación menos.
- **Alternativa**: mantener un push "cómo va tu día" a una hora fija (ej. 21:00)
  que **no cierra nada**, solo informa del progreso parcial. Cuesta ~30 min y
  reintroduce un worker, pero no reintroduce lógica de negocio.

---

## 5. Estado de ejecución — completado el 2026-08-18

Las 6 fases están aplicadas en `master`. `./gradlew assembleDebug` y
`./gradlew testDebugUnitTest` (126 tests) pasan; el backend compila.

| Fase | Estado | Notas de lo que cambió respecto al plan |
|---|---|---|
| 1 · Núcleo del día | ✅ | Sin cambios. Push nocturno eliminado; el resumen viaja en `MorningReminderWorker`. |
| 2 · Estados de tarea | ✅ | `postponedTo` y `streakDay` salen del dominio pero **se dejan como columnas legacy** en Room/DTO/backend: recrear `day_tasks` (20 columnas) costaba más que el campo muerto. Marcado con `ponytail:`. |
| 3 · Racha única | ✅ | `BlockStreak` eliminado por completo (modelo, processor, repo, DAO, entity, mapper, DTO, tabla y rutas del backend). Nueva `DayStreak` pura + 7 tests. |
| 4 · Solo lectura fuera de hoy | ✅ | Guard en `MainViewModel` **y** en `TaskClickAction` (el widget escribía por su cuenta). `isEditable` propagado a las tarjetas. |
| 5 · XP predecible | ✅ | 80/20 sin recursión ni flags. Nivel de criatura derivado (`MarineCreature.visualLevel`); la barra del diálogo pasa a mostrar el progreso de la categoría. `reassign()` → `assignMissing()`. |
| 6 · Calendario y recurrencias | ✅ | `getByDateRange` + `tasksByDate`. La generación de recurrentes corre **antes** del cierre en `MainActivity`, cubriendo los días no abiertos. `applyDefinitionChange` conserva notas y `currentCount`. |

### Migración `MIGRATION_15_16`

```sql
UPDATE day_tasks SET isDeleted = 1, status = 'EXPIRED', updatedAt = ...
 WHERE status IN ('POSTPONED', 'CANCELLED');
DROP TABLE IF EXISTS block_streaks;
UPDATE recurring_task_defs SET isActive = 0 WHERE recurrence LIKE '%NthWeekdayOfMonth%';
```

Los mapeadores toleran valores legacy que lleguen por sync desde un cliente antiguo
(`TaskStatus` desconocido → `PENDING`, recurrencia ilegible → `None`).

### Lo que queda fuera

- Columnas legacy `day_tasks.postponedTo` y `day_summaries.streak_day` siguen en
  Room y en el backend. Se caen cuando toque recrear esas tablas.
- El canal de notificación `night_summary` sigue declarado en `NotificationHelper`
  aunque ya no se use.
