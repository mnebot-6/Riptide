# Estado actual de la lógica de negocio — Riptide

> Documento de diagnóstico, no de propuesta. Describe lo que el código hace HOY
> (rama `master`, agosto 2026) para poder decidir qué simplificar.
> Cada afirmación está anclada al fichero y línea que la implementa.

---

## 1. Las piezas que definen "estado"

| Entidad | Quién la escribe | Qué representa |
|---|---|---|
| `DayTask` | Usuario + generador + resumen nocturno | Una instancia de tarea en un día concreto |
| `RecurringTaskDef` | Usuario | La plantilla de una tarea recurrente |
| `DaySummary` | Solo `NightSummaryProcessor` | El día "cerrado": score + contadores |
| `BlockStreak` | Solo `BlockStreakProcessor` | Racha por bloque |
| `EcosystemState` (×10) | `EcosystemProcessor` | XP y nivel por categoría marina |
| `MarineCreature` (×N) | Lootbox + decoraciones | XP y nivel por criatura individual |

Seis entidades mutables. **Ninguna es derivable de las otras**: `DaySummary` no
se recalcula, `BlockStreak` no se puede reconstruir, el XP es acumulativo e
irreversible. Cualquier desincronización entre ellas es permanente.

---

## 2. Ciclo de vida de una tarea

### 2.1 Estados

`TaskStatus` tiene 5 valores ([DayTask.kt:7](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/DayTask.kt#L7)):

```
PENDING ──completar──► COMPLETED ──descompletar──► PENDING   (si NO hay DaySummary)
   │                       │                    └─► EXPIRED  (si YA hay DaySummary)
   │                       │
   │                  (XP concedido una sola vez: hasBeenRewarded)
   │
   ├──resumen nocturno───► EXPIRED     (solo si la tarea es "evaluable", ver §3)
   ├──posponer──────────► POSTPONED    (+ se CREA UNA COPIA nueva en la fecha destino)
   └──borrar instancia──► CANCELLED    (soft delete de una recurrente)
```

**Puntos de confusión estructural:**

1. **`EXPIRED` se usa para dos cosas distintas**: "se te pasó la hora" (resumen
   nocturno) y "descompletaste una tarea de un día ya cerrado"
   ([MainViewModel.kt:162](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L162)).
   Semánticamente son cosas opuestas y comparten estado.

2. **Posponer duplica la fila**: la original queda `POSTPONED` visible en su día
   y nace una copia `PENDING` con ID nuevo en la fecha destino
   ([MainViewModel.kt:685](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L685)).
   La misma tarea aparece dos veces en el historial.

3. **`CANCELLED` se filtra en la UI pero `POSTPONED` no**
   ([MainViewModel.kt:546](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L546)),
   así que ves el fantasma de la tarea pospuesta en el día original.

4. **Las tareas sin hora nunca expiran.** Se quedan `PENDING` indefinidamente en
   el pasado. Existe `getPendingBefore()` en el repositorio con el comentario
   *"para expirar en resumen nocturno"* — **nunca se llama desde ningún sitio**
   ([DayTaskRepository.kt:11](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/DayTaskRepository.kt#L11)).

### 2.2 Código muerto en el modelo

- `TaskSchedule` es un sealed class con `OneTime` y `Recurring`, pero
  **`Recurring` nunca se escribe en BD**: el generador siempre crea `OneTime`
  ([RecurringTaskGenerator.kt:34](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/RecurringTaskGenerator.kt#L34)).
  Sostiene 5 ramas `when` en todo el código para un caso imposible.
- `DaySummary.streakDay` **siempre vale 0** y se propaga a Room, a los DTOs y al
  backend ([NightSummaryProcessor.kt:89](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt#L89)).

---

## 3. El resumen nocturno: la pieza central y sus reglas ocultas

`NightSummaryProcessor.processDay()` es el único sitio que "cierra" un día.
Se dispara por `NightSummaryWorker` a la hora configurada por el usuario.

### 3.1 Qué tareas entran en el cómputo ("evaluables")

Filtro real ([NightSummaryProcessor.kt:42-56](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt#L42)):

| Tarea | ¿Entra? |
|---|---|
| Completada | Sí, siempre |
| Pendiente **con hora ≤ hora del resumen** | Sí → pasa a `EXPIRED` |
| Pendiente **con hora > hora del resumen** | **No** (invisible; nunca expira) |
| Pendiente **sin hora** | **No** (invisible; nunca expira) |
| `POSTPONED` / `CANCELLED` | No |

**Consecuencia:** si tienes el resumen a las 22:00 y una tarea sin hora, esa
tarea no cuenta ni para bien ni para mal, y se queda `PENDING` para siempre.
Un día entero de tareas sin hora → **no se crea `DaySummary`**
([línea 58](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt#L58)),
lo que rompe la racha global y las decoraciones (§6).

### 3.2 Dos métricas distintas para el mismo día

El `DaySummary` guarda **dos cosas que no coinciden**:

- `tasksCompleted / tasksTotal` → recuento **plano** (cabezas).
- `score` → media **ponderada** ([línea 137](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt#L137)):
  - tarea normal: peso 1, valor 0 ó 1
  - tarea prioritaria: peso 2, valor 0 ó 2
  - tarea contable: peso 1, valor `currentCount / targetCount` (fracción)

El mensaje que ve el usuario dice *"3 de 5 tareas"* (recuento plano) pero lo que
alimenta el XP, las decoraciones y el "día perfecto" es el `score`. **Puedes
tener 3/5 y score 0.75, o 4/5 y score 0.4.** El usuario nunca ve el score.

### 3.3 Se escribe una vez y nunca se recalcula

`if (daySummaryRepository.getByDate(date) != null) return`
([línea 30](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt#L30)).

Completar una tarea después del resumen:
- **sí** te da XP (§5),
- **no** cambia el score del día,
- **no** cambia la racha del bloque.

Es decir: el ecosistema y las estadísticas divergen a partir de esa hora.

---

## 4. Tres (cuatro) sistemas de racha con criterios distintos

| Racha | Criterio | Dónde se ve | Fichero |
|---|---|---|---|
| **Por bloque** (`BlockStreak`) | **TODAS** las tareas del bloque completadas ese día | Badge en cabecera de bloque, Stats | [BlockStreakProcessor.kt:34](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/BlockStreakProcessor.kt#L34) |
| **Global** (`globalStreak`) | Días con `tasksCompleted > 0` (al menos una) | Llama en el header | [MainViewModel.kt:600](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L600) |
| **Días perfectos** | Días con `score == 1.0` exacto | Progreso de decoraciones | [DecorationUnlockChecker.kt:212](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/DecorationUnlockChecker.kt#L212) |
| **Longest streak** | Máximo histórico de la racha por bloque | Stats | `BlockStreak.longestStreak` |

Tres definiciones distintas de "he sido constante", visibles simultáneamente en
pantallas distintas, que se contradicen entre sí en casi cualquier día real.

**Además, la racha por bloque usa un universo de tareas distinto al del score**:
`getByDateAndBlock` no filtra por hora ni excluye `POSTPONED`/`CANCELLED`
([BlockStreakProcessor.kt:31](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/BlockStreakProcessor.kt#L31)),
mientras que el score sí los excluye. **Una tarea pospuesta rompe la racha del
bloque pero no penaliza el score.**

---

## 5. Recompensa: cuatro entradas de XP y cuatro orígenes de lootbox

### 5.1 Dónde se concede XP

| Momento | Cantidad | Fichero |
|---|---|---|
| Marcar tarea completada | `XP_PER_TASK = 10` | [MainViewModel.kt:181](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L181) |
| Contable llega al objetivo | 10 | [MainViewModel.kt:219](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L219) |
| **Barrido al cargar el día** (tareas completadas desde el widget) | 10 | [MainViewModel.kt:528](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L528) |
| Bonus nocturno | `score→0/10/25/50` + `racha×5` | [EcosystemLevelCalculator.kt:29](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/EcosystemLevelCalculator.kt#L29) |

`hasBeenRewarded` es la única defensa contra XP duplicado, y es un flag
irreversible: descompletar una tarea **no devuelve el XP**.

**Fuga conocida:** el barrido solo mira el día seleccionado. Si completas desde
el widget a las 23:50 y abres la app al día siguiente, ese XP se pierde.

### 5.2 El recorrido del XP (esto es lo más denso del sistema)

Al conceder 10 XP por una tarea ([EcosystemProcessor.kt:20-43](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/EcosystemProcessor.kt#L20)):

```
10 XP
 └─ se divide entre las N categorías marinas del bloque
     └─ de cada parte, el 80% va a la categoría y el 20% "spillover"
        a la categoría desbloqueada con MENOS XP total (elección aleatoria en empates)
         └─ si la categoría destino ya tiene TODAS sus especies desbloqueadas,
            el 50% "overflow" salta a la categoría de MENOR NIVEL
             └─ el XP que finalmente aterriza se reparte a partes iguales
                entre las criaturas ya desbloqueadas de esa categoría
```

Cuatro reglas de redistribución encadenadas, dos de ellas con aleatoriedad.
**El usuario no puede predecir qué crece al completar una tarea**, y el código
tiene que llevar dos flags (`fromOverflow`, `fromSpillover`) para evitar
recursión infinita.

Encima, `MarineCategoryAssigner` **reasigna las categorías de todos los bloques
por round-robin cada vez que creas, editas o borras un bloque**
([MarineCategoryAssigner.kt:13](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/MarineCategoryAssigner.kt#L13)).
El mapa bloque→categoría no es estable en el tiempo.

### 5.3 Orígenes de lootbox

1. **Umbrales de nivel de categoría** — listas por categoría (`[2,4,6,…,18]`,
   `[2,5,8,…,17]`, …) en `CATEGORY_UNLOCK_LEVELS`.
2. **Hitos de racha por bloque** — 7 / 14 / 30 días.
3. **6 condiciones de decoración** — 7 días perfectos, 100 tareas, wallpaper
   activado, login Google, 14 días perfectos, categoría completa.
4. **Easter egg Bimba** — completar una tarea cuyo título contenga "premio"/"treat".

Los tipos 3 y 4 usan `PendingLootbox.directSpecies`, que crea la criatura en BD
**antes** de que se abra la caja, y `confirmUnlock` tiene que saltarse la
inserción para no duplicar ([MainViewModel.kt:785](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L785)).
Dos flujos de desbloqueo con caminos opuestos por la misma cola.

---

## 6. El calendario

### 6.1 Lo que hay

- `WeekCalendar`: tira de 7 días con barra de progreso, swipe horizontal para
  cambiar de semana.
- Icono de calendario en el header → `DatePicker` de mes completo.
- Cambiar de día recarga todo (`loadDay`).

### 6.2 Problemas concretos

1. **Las barras de progreso de los otros 6 días están siempre vacías.**
   `MainHeader` construye `tasksByDate` agrupando `uiState.tasksByBlock`
   ([MainScreen.kt:684](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainScreen.kt#L684)),
   pero `loadDay` solo carga las tareas **del día seleccionado**
   ([MainViewModel.kt:529](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L529)).
   El mapa tiene una sola clave. La semana parece vacía siempre.

2. **Solo existen instancias 7 días hacia delante y ninguna hacia atrás.**
   `generateUpTo(hoy, 7)` corre al abrir la app y al crear/editar una recurrente.
   Si no abres la app durante 4 días, esos 4 días **no tienen instancias**, no
   tienen `DaySummary`, y la racha global se rompe por un hueco que el usuario
   no provocó.

3. **Navegar al pasado no es solo lectura**: `loadDay` sobre una fecha antigua
   dispara el barrido de XP y permite marcar/desmarcar, lo que genera `EXPIRED`
   retroactivos sin recalcular el `DaySummary` de ese día.

### 6.3 Recurrencias

`Recurrence` tiene 5 variantes: `None`, `Weekly(slots)`, `Yearly(mes, día)`,
`MonthlyDay(día, cadaNmeses)`, `NthWeekdayOfMonth(nth, díaSemana, cadaNmeses)`
([WorkBlock.kt:21](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/WorkBlock.kt#L21)).

El mismo tipo se usa para **bloques** (donde `Weekly.slots` lleva `startTime`/
`endTime` = horario del bloque) y para **tareas recurrentes** (donde los slots
solo aportan el día de la semana y la hora vive en `RecurringTaskDef.time`).
Un mismo tipo con dos semánticas.

> Nota: `CLAUDE.md` describe `Recurrence` como *"enum serializado como string
> (DAILY, WEEKLY, MONTHLY)"*. Eso ya no es cierto — la documentación está
> desactualizada respecto al código.

---

## 7. Editar recurrentes: pérdida de datos silenciosa

`updateRecurringTask` **borra todas las instancias `PENDING` de hoy en adelante**
y las regenera desde la plantilla
([MainViewModel.kt:463-484](../composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt#L463)).

Se pierden: notas escritas en la instancia, `currentCount` parcial, prioridad
puesta a mano, cambios de hora puntuales. Cambiar el título de una recurrente
borra el progreso de esa semana.

Hay además **cuatro operaciones de borrado distintas** para recurrentes
(`deleteRecurringTaskInstance`, `…FromDate`, `…All`, más `deleteTask` para las
puntuales), cada una con su propia combinación de soft-delete, hard-delete y
`isActive = false`.

---

## 8. Superficies donde el usuario ve un número

| Pantalla | Qué muestra | Fuente |
|---|---|---|
| Header | Racha global (llama, solo si ≥2) | `globalStreak` |
| Calendario semanal | Barra completadas/total por día | roto (§6.2) |
| Cabecera de bloque | Racha del bloque | `BlockStreak.currentStreak` |
| TaskCard contable | `currentCount / targetCount` | `DayTask` |
| Widget | `completadas / total` + barra | `DayTask` del día |
| Notificación nocturna | `completadas / total` | `DaySummary` |
| Diálogo de resumen | Mensaje emocional + "X de Y tareas" | `DaySummary` |
| Stats · Semana/Mes | Barras por día | `DaySummary` |
| Stats · Todo | Media diaria, días activos, total completadas, mejor semana, tendencia mensual, mes actual vs anterior | `DaySummary` agregado |
| Historial | 30/60/90 días, completadas/total por día | `DayTask` + `DaySummary` |
| Ecosistema | Nivel por categoría, nivel por criatura | `EcosystemState`, `MarineCreature` |
| Progreso decoraciones | 6 barras de condición | `DecorationProgress` |

**Al menos 12 superficies con contadores derivados de 4 fuentes distintas**, y
el "score" — la métrica que realmente gobierna las recompensas — no aparece en
ninguna. El principio de "nunca mostrar puntuación" acabó produciendo *más*
números, no menos.

---

## 9. Inconsistencias verificadas (candidatas a arreglar o a eliminar)

| # | Problema | Impacto |
|---|---|---|
| 1 | Barras del `WeekCalendar` siempre vacías salvo el día seleccionado | Visible, alto |
| 2 | Tareas sin hora nunca expiran ni cuentan | Alto: días que nunca cierran |
| 3 | Un día sin tareas evaluables no genera `DaySummary` → rompe racha global y días perfectos | Alto |
| 4 | `POSTPONED` rompe la racha del bloque pero no penaliza el score | Medio |
| 5 | XP concedido tras el resumen sin actualizar el score → ecosistema y stats divergen | Medio |
| 6 | XP del widget se pierde si no abres ese día en la app | Medio |
| 7 | Editar una recurrente borra notas/progreso de las instancias futuras | Alto |
| 8 | `NightSummaryWorker` no inyecta `DecorationUnlockChecker` → decoraciones solo se comprueban al abrir la app ([NightSummaryWorker.kt:56](../composeApp/src/androidMain/kotlin/com/mnebot/riptide/NightSummaryWorker.kt#L56)) | Bajo |
| 9 | Días no abiertos no generan instancias recurrentes hacia atrás | Alto |
| 10 | `TaskSchedule.Recurring` y `DaySummary.streakDay` son código/campos muertos | Bajo, ruido |
| 11 | `getPendingBefore()` implementado y nunca usado | Bajo, ruido |
| 12 | `MarineCategoryAssigner` reasigna categorías al tocar cualquier bloque | Medio: progreso impredecible |

---

## 10. Inventario de complejidad, en números

- **5** estados de tarea (2 con semántica solapada)
- **5** variantes de recurrencia (usadas para dos cosas distintas)
- **4** definiciones de racha
- **2** métricas de "día hecho" (`score` ponderado vs. recuento plano)
- **4** puntos de concesión de XP
- **4** reglas encadenadas de redistribución de XP (2 aleatorias)
- **4** orígenes de lootbox, con **2** caminos de inserción opuestos
- **10** categorías marinas + **70** especies + XP por criatura individual
- **6** condiciones de decoración, cada una con su propia consulta
- **12+** superficies de UI con contadores
- **845** líneas en `MainViewModel`, **1781** en `MainScreen`
- **6** entidades mutables sin posibilidad de recálculo

---

## 11. Las decisiones que hay sobre la mesa

Preguntas cuya respuesta determina cuánto se puede podar. No hay que
responderlas todas ahora, pero cada una desbloquea una simplificación grande:

1. **¿Una tarea sin hora debe poder "fallar"?** Si sí → el resumen deja de
   depender de la hora y el filtro de "evaluables" desaparece. Si no → hay que
   decir explícitamente que no cuentan (y quitarlas de todos los contadores).

2. **¿Cuál es LA definición de "hoy fue un buen día"?** Elegir una y borrar las
   otras tres. Todo lo demás (racha, decoraciones, XP) debería colgar de ella.

3. **¿El score ponderado (prioridad ×2, contables fraccionales) aporta algo si
   el usuario nunca lo ve?** Si no, `completadas/total` sirve y desaparecen 20
   líneas de scoring más la divergencia con el mensaje.

4. **¿El XP tiene que redistribuirse (spillover + overflow + reparto por
   criatura)?** Si el objetivo es "la constancia hace crecer algo bello", una
   sola bolsa de XP por bloque cumple igual y es predecible.

5. **¿Necesitas 5 tipos de recurrencia?** `Weekly` cubre el 95% del uso real de
   una app de productividad diaria.

6. **¿Posponer debe duplicar la fila, o mover la fecha?** Mover elimina
   `POSTPONED`, el fantasma en el día original y la duplicación en el historial.

7. **¿El día debe cerrarse por reloj (worker nocturno) o al abrir la app?**
   Cerrar perezosamente al abrir (procesando todos los días pendientes desde el
   último cierre) elimina los huecos de los puntos 3 y 9 del §9.

8. **¿Navegar al pasado debe ser solo lectura?** Si sí, desaparece toda la rama
   `EXPIRED`-por-descompletar y el barrido retroactivo de XP.
