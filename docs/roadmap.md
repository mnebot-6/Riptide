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

## ✅ v2 completada

### Rachas (`BlockStreak`)
- `BlockStreakProcessor` — calcula y persiste racha de días consecutivos por bloque
- `processDay` devuelve `Map<String, Int>` (blockId → newStreak) para uso en `NightSummaryProcessor`
- Badge `🔥 N días` en `BlockHeader` (solo si racha ≥ 2, color #FFB347)
- Día sin tareas = neutral, no toca la racha

### Mensajes contextuales
- `buildMessage` en `NightSummaryProcessor` — combina score, progreso y racha del bloque top (≥ 3 días)
- Diálogo al arrancar la app si hay resumen de ayer no visto (`pendingSummary` en `MainUiState`)
- `dismissSummary()` en `MainViewModel` para cerrar el diálogo

### Lógica de experiencia y niveles
- `EcosystemLevelCalculator` — curva exponencial suave (+50 XP por nivel desde nivel 2)
- `EcosystemProcessor` — `addXpForTask` y `addNightBonus`, XP dividida entre categorías del bloque
- 10 XP por tarea completada (tiempo real); bonus nocturno según score + bestStreak
- `EcosystemProcessor` devuelve `List<CreatureSpec>` con las criaturas recién desbloqueadas

### Ecosistema visual — AquariumBackground
- `AquariumBackground` — fondo oceánico con plantas animadas y burbujas, siempre visible detrás de la UI
- Plantas con oscilación suave (`swayAngle` con `Animatable`)
- Burbujas con trayectoria vertical + oscilación horizontal (`sin()`)
- Reemplaza el `OceanBackground` estático anterior

### Ecosistema visual — AquariumCreature
- `CreatureSpec` — modelo ligero con emoji, especie, categoría, unlockLevel, swimDuration, wobbleAmplitude
- `allCreatures` — 10 criaturas (2 por categoría marina), se desbloquean en niveles 2 y 5
- `AquariumCreatures` — composable que dibuja criaturas desbloqueadas sobre el fondo con `drawWithContent`
- Criaturas móviles nadan de lado a lado con oscilación vertical (`sin()`), espejadas según dirección
- Criaturas fijas (FLORA, MOLLUSK) ancladas en zona inferior
- `expect fun DrawScope.drawEmoji(...)` — expect/actual para renderizado de emojis en Canvas
  - androidMain: `nativeCanvas.drawText` con `android.graphics.Paint`
  - iosMain: pendiente arreglar (stub)
- Criaturas visibles siempre en el fondo detrás de las tareas; pantalla completa al pulsar FAB 🐟

### Desbloqueo de criaturas
- Al completar tarea o procesar resumen nocturno → detección automática de nivel cruzado
- `pendingUnlocks: List<CreatureSpec>` en `MainUiState`
- Persistencia en DataStore (`UserPreferencesRepository`) para sobrevivir entre sesiones (caso worker nocturno)
- Diálogo de desbloqueo: emoji animado + mensaje + campo de nombre obligatorio
- Orden de diálogos al arrancar: primero resumen nocturno, luego desbloqueos en cola
- `confirmUnlock(spec, nickname)` en `MainViewModel` — guarda `MarineCreature` con nickname
- `dismissUnlock()` — descarta sin guardar nombre (avanza al siguiente en cola)

---

## v3 — Revisión y pulido

Antes de pasar al backend, una versión dedicada a estabilizar lo construido:

### Calidad y robustez
- Arreglar `AquariumCreature.ios.kt` — implementación real con UIKit/CoreGraphics
- Revisar y unificar gestos (drawer, swipe de días, scroll de lista)
- Validar flujo completo de tareas recurrentes (generación, edición, eliminación)
- Revisar comportamiento del resumen nocturno en edge cases (app cerrada, sin tareas, cambio de hora)
- Limpiar `DataSeeder` para producción (sin XP sembrada, sin unlocks pregrabados)

### UX y feedback
- Animación de entrada de criatura al desbloquearse (entra nadando desde el borde)
- Revisar y pulir mensajes del resumen nocturno
- Feedback visual al completar tarea (animación sutil en la card o en el ecosistema)
- Revisar accesibilidad básica (tamaños de texto, contraste)

### Inputs y formularios
- Revisar `TaskFormSheet` — validaciones, UX del selector de días recurrentes
- Revisar `BlockFormScreen` — validaciones de nombre vacío, emoji vacío
- Revisar `PostponeSheet` — validación de fecha pasada

### Técnico
- Migrar de `fallbackToDestructiveMigration` a migraciones reales de Room
- Revisar memory leaks potenciales en ViewModels y coroutines
- Añadir logs de error estructurados
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
