# Roadmap — Riptide

## ✅ MVP completado

### Infraestructura
- Proyecto KMP con Compose Multiplatform
- Room completo v6 (entities, DAOs, database, mappers, repositorios)
- `BlockCategory` con tabla separada y FK CASCADE
- `generateUUID` expect/actual (commonMain / androidMain / iosMain)
- `parseColor` y `currentDate` expect/actual
- Navigation Compose configurado
- `DataSeeder` con datos de prueba (3 bloques + 5 tareas)
- Plugin `kotlin.plugin.serialization` configurado
- `LocalTimeSerializer` para serialización de `LocalTime` en KMP
- DataStore Preferences para persistencia de ajustes de usuario
- WorkManager para tareas en background

### Dominio
- Todos los modelos de dominio en commonMain
- Todas las interfaces de repositorio
- `MarineCategoryAssigner` — redistribución automática de categorías marinas
- `RecurringTaskGenerator` — generación de instancias para los próximos N días
- `NightSummaryProcessor` — expira pendientes, calcula score, genera mensaje, guarda `DaySummary`
- `TaskStatus` (PENDING, COMPLETED, EXPIRED, POSTPONED)
- `TaskSchedule` sealed class (OneTime, Recurring)
- `RecurringTaskDef` — definición de tareas recurrentes con bloque obligatorio
- `Recurrence` / `WeeklySlot` — completamente `@Serializable`

### UI — Pantalla principal
- Fondo oceánico (gradiente + frosted glass cards)
- `WeekCalendar` — 7 días navegables, swipe horizontal, indicador día actual
- Barra de progreso animada por día en el calendario (completadas vs pendientes)
- Lista de bloques ordenada: primero los que tienen horario ese día (por hora de inicio), luego el resto
- Lista de tareas: con hora primero (ordenadas), sin hora después, completadas al final
- Tareas sin bloque visibles en sección propia ("Sin bloque")
- Hora de la tarea visible en la card si la tiene
- Horas del bloque en cabecera solo si tiene horario ese día dentro de su recurrencia
- Checkbox con color del bloque, tachado al completar, ⌛ expiradas, ⏰ pospuestas
- Drawer desde arriba semitransparente con gesto vertical
- Gestos unificados (vertical = drawer, horizontal = cambio de día)
- FAB 🐟 siempre visible
- Sección AJUSTES en drawer con hora configurable del resumen nocturno

### UI — Gestión de tareas
- `TaskFormSheet` — crear y editar tareas puntuales y recurrentes
  - Toggle puntual/recurrente, fecha, hora opcional, selector de días, selector de bloque
  - Bloque obligatorio para recurrentes, opcional para puntuales
  - En modo edición: carga datos existentes y muestra botón eliminar
- Menú contextual por pulsación larga: Editar, Posponer, Eliminar
- Al eliminar tarea recurrente: diálogo con 3 opciones (solo esta / esta y futuras / todas)
- `PostponeSheet` — nueva fecha + hora; marca original como POSTPONED y crea nueva instancia PENDING
- Eliminar tarea desde menú contextual o desde el formulario de edición

### UI — Gestión de bloques
- `BlockFormScreen` completo (crear / editar / eliminar)
  - Campos: nombre, emoji, paleta 12 colores, horario semanal
  - `TimeTextField` con validación (4 dígitos, rango 00:00–23:59)
  - Botón eliminar solo en modo edición

### Resumen nocturno
- `NightSummaryProcessor` (commonMain) — lógica pura de cierre de día
- `NightSummaryWorker` (androidMain, WorkManager) — ejecuta a la hora configurada
- Al abrir la app: procesa ayer si no se procesó (fallback de seguridad)
- Hora configurable por el usuario desde el drawer (persiste en DataStore)
- `NightSummaryScheduler` como interfaz commonMain con implementaciones por plataforma

---

## v2 — En curso

### ✅ Rachas (`BlockStreak`)
- Calcular y persistir racha de días consecutivos por bloque
- Actualizar al completar tareas en el resumen nocturno
- Badge `🔥 N días` en cabecera del bloque (solo si racha ≥ 2)

### ✅ Mensajes contextuales
- `buildMessage` en `NightSummaryProcessor` — combina score, progreso y racha del mejor bloque
- Diálogo al arrancar la app si hay resumen de ayer no visto (`pendingSummary` en `MainUiState`)
- `dismissSummary()` en `MainViewModel` para cerrar el diálogo

### Ecosistema visual
- Lógica de experiencia y niveles (`EcosystemState`, `MarineCreature`)
- Vista del estanque con Canvas
- Animaciones al completar tareas

---

## v3

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