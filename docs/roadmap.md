# Roadmap — Riptide

## ✅ MVP completado

### Infraestructura
- Proyecto KMP con Compose Multiplatform
- Room completo v7 (entities, DAOs, database, mappers, repositorios)
- `BlockCategory` con tabla separada y FK CASCADE
- `generateUUID` expect/actual (commonMain / androidMain / iosMain)
- `parseColor` y `currentDate` expect/actual
- Navigation Compose configurado
- `DataSeeder` con datos para primer contacto real
- Plugin `kotlin.plugin.serialization` configurado
- `LocalTimeSerializer` para serialización de `LocalTime` en KMP
- DataStore Preferences para persistencia de ajustes de usuario
- WorkManager para tareas en background

### Dominio
- Todos los modelos de dominio en commonMain
- Todas las interfaces de repositorio
- `MarineCategoryAssigner` — redistribución automática de categorías marinas
- `RecurringTaskGenerator` — generación de instancias para los próximos N días
- `NightSummaryProcessor` — expira pendientes evaluables, calcula score, genera mensaje, guarda `DaySummary`
- `TaskStatus` (PENDING, COMPLETED, EXPIRED, POSTPONED)
- `TaskSchedule` sealed class (OneTime, Recurring)
- `RecurringTaskDef` — definición de tareas recurrentes con bloque obligatorio, hora opcional
- `Recurrence` / `WeeklySlot` — completamente `@Serializable`

### UI — Pantalla principal
- Fondo oceánico (AquariumBackground con Canvas + frosted glass cards)
- `WeekCalendar` — 7 días navegables, swipe horizontal, indicador día actual
- Barra de progreso animada por día en el calendario (excluye POSTPONED)
- Lista de bloques ordenada: primero los que tienen horario ese día, luego el resto
- Lista de tareas: con hora primero, sin hora después, completadas al final
- Tareas sin bloque visibles en sección propia
- Checkbox con color del bloque, tachado al completar, ⌛ expiradas, ⏰ pospuestas
- Drawer desde arriba con gesto vertical (cierre con drag hacia arriba desde el propio drawer)
- Gestos unificados (vertical = drawer, horizontal = cambio de día)
- FAB 🐟 siempre visible; AquariumFullScreen al pulsarlo
- Header fijo con nombre app + DatePicker + volver a hoy + añadir tarea + abrir drawer
- Drawer compacto: solo BLOQUES + AJUSTES; lista de bloques con scroll interno (85% pantalla)

### UI — Gestión de tareas
- `TaskFormSheet` — crear y editar tareas puntuales y recurrentes; hora opcional en recurrentes; `forceRecurring` para forzar modo al editar
- Menú contextual por pulsación larga: Editar, Posponer, Eliminar
- Al editar tarea recurrente: diálogo scope (solo esta / esta y futuras / todas)
- Al eliminar tarea recurrente: diálogo scope (solo esta / esta y futuras / todas)
- `PostponeSheet` — nueva fecha + hora opcional; marca original como POSTPONED y crea nueva instancia PENDING

### UI — Gestión de bloques
- `BlockFormScreen` completo (crear / editar / eliminar)
- Campos: nombre, emoji, paleta 12 colores, horario semanal

### Resumen nocturno
- `NightSummaryProcessor` (commonMain) — lógica pura de cierre de día
- Evaluación selectiva: solo tareas con `date <= fecha resumen` o completadas sin fecha
- Tareas POSTPONED excluidas completamente del resumen y de la barra de progreso
- `NightSummaryWorker` (androidMain, WorkManager) — ejecuta a la hora configurada
- Fallback al arrancar la app (procesa ayer si no se procesó)
- Hora configurable desde el drawer (persiste en DataStore)
- `NightSummaryScheduler` como interfaz commonMain con implementaciones por plataforma
- No reaparece si ya fue cerrado (fecha descartada en DataStore)

---

## ✅ v2 completada

### Rachas (`BlockStreak`)
- `BlockStreakProcessor` — calcula y persiste racha de días consecutivos por bloque
- Badge `🔥 N días` en `BlockHeader` (solo si racha ≥ 2, color #FFB347)
- Día sin tareas = neutral, no toca la racha

### Mensajes contextuales
- `buildMessage` — combina score, progreso y racha del bloque top (≥ 3 días)
- Diálogo al arrancar si hay resumen de ayer no visto

### Lógica de experiencia y niveles
- `EcosystemLevelCalculator` — curva de niveles con costes crecientes
- `EcosystemProcessor` — XP por tarea y bonus nocturno, dividida entre categorías
- Devuelve `List<CreatureSpec>` con criaturas recién desbloqueadas

### Ecosistema visual
- `AquariumBackground` — fondo oceánico animado con plantas y burbujas (Canvas)
- `AquariumCreatures` — 10 criaturas con natación animada (`Animatable`)
- `expect fun DrawScope.drawEmoji(...)` — expect/actual por plataforma

### Desbloqueo de criaturas
- Detección automática de nivel cruzado al completar tarea o resumen nocturno
- Cola `pendingUnlocks` en `MainUiState`
- Persistencia en DataStore para sobrevivir entre sesiones
- Diálogo: emoji + mensaje + nombre obligatorio
- `confirmUnlock(spec, nickname)` → guarda `MarineCreature` con nickname

---

## ✅ v2.1 completada

- Curva XP reajustada — nivel 1→2 cuesta 1 XP
- Fix edición de tareas recurrentes con diálogo de scope
- `updateRecurringTask` en `MainViewModel`
- `DataSeeder` limpio para distribución
- `TimeInputField` y `DateInputField` estandarizados
- Pickers con estética marina
- `MainDrawer` con `TimeInputField` compact

---

## ✅ Sprint de fixes y mejoras (pre-v3)

- Bloquear rotación de pantalla (portrait only)
- Resumen nocturno no reaparece al reabrir — fecha descartada en DataStore
- Scroll mantiene posición al completar tarea — `rememberLazyListState` fuera del `when`
- Posponer tarea — hora opcional
- Tareas POSTPONED excluidas de resumen y barra de progreso
- Resumen nocturno evalúa solo tareas vencidas o completadas sin fecha
- Nuevo Header fijo con acciones integradas
- Drawer con scroll interno en lista de bloques (85% pantalla con `LocalWindowInfo`)
- Tareas recurrentes — hora opcional (`RecurringTaskDef.time: LocalTime?`)
- Editar tarea recurrente — diálogo scope igual que al borrar; `forceRecurring` en `TaskFormSheet`
- `RiptideDatabase` versión 7

---

## 🔄 v3 — Revisión, pulido y onboarding

### Pendientes del sprint de fixes
- Ordenación bloques por tarea no completada con hora más temprana
- Long press en cabecera del bloque → crear tarea rápida con bloque preseleccionado
- Comprobar XP tareas sin bloque
- Pulsar criatura → ver nombre, nivel, etc. (solo con tareas ocultas)
- Resumen de criaturas accesible desde el Drawer (nueva pantalla NavHost)
- Patrón de nado de criaturas más orgánico y variado

### Onboarding
- Tutorial de primera vez — pantalla fullscreen que aparece solo en el primer inicio
  - Estado persistido en DataStore: `hasCompletedOnboarding` en `UserPreferencesRepository`
  - Pasos swipeables con fondo marino
  - Explica conceptos clave: bloques, tareas, ecosistema, resumen nocturno
  - Permite crear el primer bloque al final (o continúa directamente con el DataSeeder)
  - Una vez completado, nunca vuelve a aparecer

### Calidad y robustez
- Arreglar `AquariumCreature.ios.kt` — implementación real con UIKit/CoreGraphics
- Revisar y unificar gestos (drawer, swipe de días, scroll de lista)
- Validar flujo completo de tareas recurrentes (generación, edición, eliminación)
- Revisar comportamiento del resumen nocturno en edge cases

### UX y feedback
- Animación de entrada de criatura al desbloquearse (entra nadando desde el borde)
- Feedback visual al completar tarea

### Técnico
- Migrar de `fallbackToDestructiveMigration` a migraciones reales de Room
- Preparar firma de la app para distribución

### Fase final
- Estanque como fondo de pantalla dinámico (WallpaperService)

---

## v4 — Backend y social

- Backend Ktor + PostgreSQL
- Sincronización offline-first (IDs UUID ya preparados)
- Perfiles de usuario
- Google Sign-In
- Visitar el estanque de un amigo (solo ver, nunca competir ni rankear)

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual
