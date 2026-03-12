# Roadmap — Riptide

## Estado actual (MVP en curso)

### ✅ Completado

**Infraestructura**
- Proyecto KMP con Compose Multiplatform
- Room completo v6 (entities, DAOs, database, mappers, repositorios)
- `BlockCategory` con tabla separada y FK CASCADE
- `generateUUID` expect/actual (commonMain / androidMain / iosMain)
- `parseColor` y `currentDate` expect/actual
- Navigation Compose configurado
- `DataSeeder` con datos de prueba (3 bloques + 5 tareas)
- Plugin `kotlin.plugin.serialization` configurado
- `LocalTimeSerializer` para serialización de `LocalTime` en KMP

**Dominio**
- Todos los modelos de dominio en commonMain
- Todas las interfaces de repositorio
- `MarineCategoryAssigner` — redistribución automática de categorías marinas
- `RecurringTaskGenerator` — generación de instancias para los próximos N días
- `TaskStatus` (PENDING, COMPLETED, EXPIRED, POSTPONED)
- `TaskSchedule` sealed class (OneTime, Recurring)
- `RecurringTaskDef` — definición de tareas recurrentes con bloque obligatorio
- `Recurrence` / `WeeklySlot` — completamente `@Serializable`

**UI — Pantalla principal**
- Fondo oceánico (gradiente + frosted glass cards)
- `WeekCalendar` — 7 días navegables, swipe horizontal, indicador día actual
- Lista de bloques ordenada: primero los que tienen horario ese día (por hora de inicio), luego el resto
- Lista de tareas: con hora primero (ordenadas), sin hora después, completadas al final — orden consistente en ambos grupos
- Hora de la tarea visible en la card si la tiene
- Horas del bloque en cabecera solo si tiene horario ese día dentro de su recurrencia
- Checkbox con color del bloque, tachado al completar, ⌛ expiradas, ⏰ pospuestas
- Drawer desde arriba semitransparente con gesto vertical
- Gestos unificados (vertical = drawer, horizontal = cambio de día)
- FAB 🐟 siempre visible

**UI — Gestión de tareas**
- `TaskFormSheet` — crear y editar tareas puntuales y recurrentes desde el drawer
  - Toggle puntual/recurrente, fecha, hora opcional, selector de días, selector de bloque
  - Bloque obligatorio para recurrentes, opcional para puntuales
  - En modo edición: carga datos existentes y muestra botón eliminar
- Menú contextual por pulsación larga: Editar, Posponer, Eliminar
- `PostponeSheet` — nueva fecha + hora; marca original como POSTPONED y crea nueva instancia PENDING
- Eliminar tarea desde menú contextual o desde el formulario de edición

**UI — Gestión de bloques**
- `BlockFormScreen` completo (crear / editar / eliminar)
  - Campos: nombre, emoji, paleta 12 colores, horario semanal
  - `TimeTextField` con validación (4 dígitos, rango 00:00–23:59)
  - Botón eliminar solo en modo edición

---

## Próximo — MVP restante

### Indicadores de tareas en el calendario
- Puntos bajo cada día según estado de tareas (pendientes / completadas)
- Color del bloque predominante o neutro si son sin bloque

### Resumen nocturno
- Expirar tareas PENDING de días pasados (→ EXPIRED)
- Cálculo de `DaySummary` (score interno, mensaje emocional)
- Notificación a hora configurable
- WorkManager para ejecutar en background

---

## v2

- Rachas por bloque (`BlockStreak`) con mensajes contextuales al completar
  - *"Llevas una semana sólida en el trabajo 🌊"*
  - *"El coral está creciendo 🪸"*
- Ecosistema visual — Canvas / Lottie
- Animaciones del estanque al completar tareas
- Configuración avanzada del resumen nocturno

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