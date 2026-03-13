# Roadmap — Riptide

## ✅ MVP completado

### Infraestructura
- Proyecto KMP con Compose Multiplatform
- Room completo v6 (entities, DAOs, database, mappers, repositorios)
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
- `NightSummaryProcessor` — expira pendientes, calcula score, genera mensaje, guarda `DaySummary`
- `TaskStatus` (PENDING, COMPLETED, EXPIRED, POSTPONED)
- `TaskSchedule` sealed class (OneTime, Recurring)
- `RecurringTaskDef` — definición de tareas recurrentes con bloque obligatorio
- `Recurrence` / `WeeklySlot` — completamente `@Serializable`

### UI — Pantalla principal
- Fondo oceánico (AquariumBackground con Canvas + frosted glass cards)
- `WeekCalendar` — 7 días navegables, swipe horizontal, indicador día actual
- Barra de progreso animada por día en el calendario
- Lista de bloques ordenada: primero los que tienen horario ese día, luego el resto
- Lista de tareas: con hora primero, sin hora después, completadas al final
- Tareas sin bloque visibles en sección propia
- Checkbox con color del bloque, tachado al completar, ⌛ expiradas, ⏰ pospuestas
- Drawer desde arriba con gesto vertical
- Gestos unificados (vertical = drawer, horizontal = cambio de día)
- FAB 🐟 siempre visible; AquariumFullScreen al pulsarlo
- Sección AJUSTES en drawer con hora configurable del resumen nocturno

### UI — Gestión de tareas
- `TaskFormSheet` — crear y editar tareas puntuales y recurrentes
- Menú contextual por pulsación larga: Editar, Posponer, Eliminar
- Al eliminar tarea recurrente: diálogo con 3 opciones (solo esta / esta y futuras / todas)
- `PostponeSheet` — nueva fecha + hora; marca original como POSTPONED y crea nueva instancia PENDING

### UI — Gestión de bloques
- `BlockFormScreen` completo (crear / editar / eliminar)
- Campos: nombre, emoji, paleta 12 colores, horario semanal

### Resumen nocturno
- `NightSummaryProcessor` (commonMain) — lógica pura de cierre de día
- `NightSummaryWorker` (androidMain, WorkManager) — ejecuta a la hora configurada
- Fallback al arrancar la app (procesa ayer si no se procesó)
- Hora configurable desde el drawer (persiste en DataStore)
- `NightSummaryScheduler` como interfaz commonMain con implementaciones por plataforma

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

### Correcciones y mejoras pre-distribución

- **Curva XP reajustada** — nivel 1→2 cuesta 1 XP (garantiza primer pez con 1 tarea completada, independientemente de las categorías del bloque)
- **Fix edición de tareas recurrentes** — al editar una tarea con `sourceTaskId`, aparece un diálogo previo:
  - "Solo esta ocurrencia" → `updateOneTimeTask`
  - "Esta y todas las futuras" → `updateRecurringTask` (actualiza def, borra instancias PENDING futuras, regenera)
- **`updateRecurringTask`** añadido a `MainViewModel`
- **`DataSeeder` limpio para distribución** — firma `seedIfEmpty(db, assigner)`, 3 bloques genéricos (Trabajo, Personal, Salud) + 4 tareas para hoy, sin XP sembrada ni unlocks pregrabados
- **`TimeInputField` estandarizado** — `BasicTextField` invisible + overlay formateado; cursor nunca cruza el `:`; parámetros `compact` y `showPickerIcon`
- **`DateInputField` estandarizado** — misma técnica para fechas; dígitos rellenan desde la derecha sobre la fecha de hoy como base; cursor nunca cruza los `-`
- **Pickers con estética marina** — `Dialog` propio con fondo `OceanMid` y `MaterialTheme` override (`Accent #7EC8E3`); `TimePicker` y `DatePicker` de M3 dentro del dialog custom
- **`MainDrawer`** actualizado con `TimeInputField(compact=true, showPickerIcon=true)`
- `TaskFormSheet`, `PostponeSheet` y `BlockFormScreen` migrados a los nuevos componentes de input

---

## v3 — Revisión, pulido y onboarding

Antes de pasar al backend, una versión dedicada a estabilizar y preparar la app para distribución real.

### Onboarding
- **Tutorial de primera vez** — pantalla fullscreen que aparece solo en el primer inicio
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
- Revisar accesibilidad básica (tamaños de texto, contraste)

### Técnico
- Migrar de `fallbackToDestructiveMigration` a migraciones reales de Room
- Revisar memory leaks potenciales en ViewModels y coroutines
- Preparar firma de la app para distribución

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
