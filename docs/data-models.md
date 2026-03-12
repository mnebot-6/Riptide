# Modelos de datos — Riptide

Todos los modelos viven en `commonMain/kotlin/com/mnebot/riptide/domain/model/` como clases Kotlin puras, sin dependencias de Room ni de Android.

---

## WorkBlock

```kotlin
data class WorkBlock(
    val id: String,                             // UUID
    val name: String,
    val marineCategories: List<MarineCategory>, // asignado automáticamente, nunca visible al usuario
    val color: String,                          // hex, ej: "#1A73E8"
    val icon: String,                           // emoji, ej: "💼"
    val recurrence: Recurrence,
    val isActive: Boolean
)
```

---

## Recurrence / WeeklySlot

```kotlin
@Serializable
sealed class Recurrence {
    @Serializable object None : Recurrence()
    @Serializable data class Weekly(val slots: List<WeeklySlot>) : Recurrence()
}

@Serializable
data class WeeklySlot(
    val dayOfWeek: Int,        // 1=Lunes … 7=Domingo
    val startTime: LocalTime?,
    val endTime: LocalTime?
)
```

Serialización via `kotlinx-serialization-json`. `LocalTime` usa `LocalTimeSerializer` (ISO string). Se aplica con `@file:UseSerializers(LocalTimeSerializer::class)` en `WorkBlock.kt`.

---

## TaskStatus

```kotlin
enum class TaskStatus { PENDING, COMPLETED, EXPIRED, POSTPONED }
```

| Estado | Descripción |
|--------|-------------|
| PENDING | Estado por defecto al crear |
| COMPLETED | Da experiencia al ecosistema marino |
| EXPIRED | Pendiente al hacer el resumen nocturno; no da experiencia |
| POSTPONED | Tarea pospuesta; se crea nueva instancia PENDING en la fecha elegida |

---

## TaskSchedule

```kotlin
sealed class TaskSchedule {
    data class OneTime(val date: LocalDate, val time: LocalTime?) : TaskSchedule()
    data class Recurring(val time: LocalTime, val recurrence: Recurrence) : TaskSchedule()
}
```

Las instancias generadas por `RecurringTaskGenerator` se crean siempre como `OneTime`.

---

## DayTask

```kotlin
data class DayTask(
    val id: String,
    val blockId: String?,           // null = tarea sin bloque (solo puntuales)
    val title: String,
    val schedule: TaskSchedule,
    val status: TaskStatus,
    val completedAt: LocalDateTime?,
    val postponedTo: LocalDateTime?,
    val sourceTaskId: String?       // apunta a RecurringTaskDef.id si es instancia generada
)
```

---

## RecurringTaskDef

Definición de una tarea recurrente. Genera instancias `DayTask` para los próximos N días.

```kotlin
data class RecurringTaskDef(
    val id: String,
    val blockId: String,    // siempre tiene bloque
    val title: String,
    val time: LocalTime,
    val recurrence: Recurrence,
    val isActive: Boolean
)
```

---

## BlockCategory

```kotlin
data class BlockCategory(
    val blockId: String,
    val category: MarineCategory
)
```

Tabla separada con PK compuesta `(blockId, category)` y FK CASCADE desde `work_blocks`. Nunca visible al usuario.

---

## MarineCategory

```kotlin
enum class MarineCategory {
    FISH,       // 🐟 Peces
    FLORA,      // 🪸 Flora
    CRUSTACEAN, // 🦞 Crustáceos
    MOLLUSK,    // 🐚 Moluscos
    PELAGIC     // 🦈 Pelágicos
}
```

---

## DaySummary

```kotlin
data class DaySummary(
    val id: String,
    val date: LocalDate,
    val score: Float,           // 0.0–1.0, NUNCA visible al usuario
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val streakDay: Int,
    val feedbackMessage: String // sí visible
)
```

Lógica de puntuación interna:
```kotlin
fun calculateDayScore(tasks: List<DayTask>): Float =
    if (tasks.isEmpty()) 0f
    else tasks.count { it.status == TaskStatus.COMPLETED }.toFloat() / tasks.size
```

Mensajes según score:
- `0.0` → "Las corrientes cambian. Mañana el mar sigue ahí."
- `~0.5` → "Buen empuje hoy."
- `~0.8` → "El estanque está vivo."
- `1.0` → "Hoy el estanque brilló."

---

## BlockStreak

```kotlin
data class BlockStreak(
    val blockId: String,
    val currentStreak: Int,
    val lastActiveDate: LocalDate
)
```

Sin campo `id` — `blockId` es la clave primaria natural de esta tabla.

Lógica de actualización (en `BlockStreakProcessor`):
- Al menos 1 tarea COMPLETED en el bloque ese día → día activo
- Sin tareas ese día → día neutral, no toca la racha
- Tareas pero ninguna COMPLETED → rompe la racha (`currentStreak = 0`)
- Día activo consecutivo al `lastActiveDate` → incrementa racha
- Día activo no consecutivo → reinicia racha a 1

UI: se muestra `🔥 N días` en `BlockHeader` solo si `currentStreak >= 2`.

---

## EcosystemState

```kotlin
data class EcosystemState(
    val id: String,
    val category: MarineCategory,
    val totalExperience: Int,   // NUNCA visible al usuario
    val currentLevel: Int,
    val lastUpdated: LocalDateTime
)
```

---

## MarineCreature / CreatureSpecies

```kotlin
data class MarineCreature(
    val id: String,
    val ecosystemId: String,
    val species: CreatureSpecies,
    val nickname: String?,
    val unlockedAtLevel: Int,
    val experience: Int,
    val creatureLevel: Int,
    val unlockedAt: LocalDateTime
)

enum class CreatureSpecies {
    // FISH
    CLOWNFISH, ANGELFISH,
    // FLORA
    BRAIN_CORAL, ANEMONE,
    // CRUSTACEAN
    HERMIT_CRAB, LOBSTER,
    // MOLLUSK
    STARFISH, SEA_URCHIN,
    // PELAGIC
    MOON_JELLYFISH, MANTA_RAY
}
```

---

## Room — androidMain (v6)

Entities principales y su tabla:

| Entity | Tabla |
|--------|-------|
| `WorkBlockEntity` | `work_blocks` |
| `BlockCategoryEntity` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity` | `day_tasks` — `scheduleType`, `date?`, `time?`, `recurrence?` (JSON), `status`, `completedAt?`, `postponedTo?`, `sourceTaskId?`, `blockId?` FK SET_NULL |
| `RecurringTaskDefEntity` | `recurring_task_defs` — `blockId` FK CASCADE, `recurrence` JSON |
| `DaySummaryEntity` | `day_summaries` |
| `BlockStreakEntity` | `block_streaks` — PK `blockId` |
| `EcosystemStateEntity` | `ecosystem_states` |
| `MarineCreatureEntity` | `marine_creatures` |

Fechas almacenadas como String ISO. Enums como String. Sealed classes como JSON con `kotlinx-serialization`.