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
| COMPLETED | Da XP al ecosistema marino (tiempo real) |
| EXPIRED | Pendiente al hacer el resumen nocturno; no da XP |
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

Mensajes según score (con prefijo de progreso si score parcial y sufijo de racha si top ≥ 3 días):
- `0.0` → "Las corrientes cambian. Mañana el mar sigue ahí."
- `< 0.4` → "Algo se movió hoy. Eso cuenta."
- `< 0.7` → "Buen empuje hoy."
- `< 1.0` → "El estanque está vivo."
- `1.0` → "Hoy el estanque brilló."

---

## BlockStreak

```kotlin
data class BlockStreak(
    val blockId: String,        // PK natural, sin campo id separado
    val currentStreak: Int,
    val lastActiveDate: LocalDate
)
```

Lógica de actualización (en `BlockStreakProcessor`):
- Al menos 1 tarea COMPLETED en el bloque ese día → día activo → incrementa racha si consecutivo
- Sin tareas ese día → día neutral, no toca la racha
- Tareas pero ninguna COMPLETED → rompe la racha (`currentStreak = 0`)

UI: `🔥 N días` en `BlockHeader` solo si `currentStreak >= 2` (color #FFB347).

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

Un registro por categoría marina (5 en total). Curva de niveles:

| Nivel | XP total requerida |
|---|---|
| 1 | 0 |
| 2 | 100 |
| 3 | 250 |
| 4 | 450 |
| 5 | 700 |
| N | nivel anterior + (N-1)*50 |

Fuentes de XP:
- Completar tarea (tiempo real): 10 XP divididas entre las categorías del bloque
- Bonus nocturno: `score≥1.0→+50` | `score≥0.7→+25` | `score≥0.4→+10` + `bestStreak*5`, dividido entre categorías

---

## MarineCreature / CreatureSpecies

```kotlin
data class MarineCreature(
    val id: String,
    val ecosystemId: String,        // id del EcosystemState de su categoría
    val species: CreatureSpecies,
    val nickname: String?,          // nombre dado por el usuario al desbloquear
    val unlockedAtLevel: Int,
    val experience: Int,
    val creatureLevel: Int,
    val unlockedAt: LocalDateTime
)

enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {
    CLOWNFISH(FISH, "Pez payaso"),
    ANGELFISH(FISH, "Pez ángel"),
    BRAIN_CORAL(FLORA, "Coral cerebro"),
    ANEMONE(FLORA, "Anémona"),
    HERMIT_CRAB(CRUSTACEAN, "Cangrejo ermitaño"),
    LOBSTER(CRUSTACEAN, "Langosta"),
    STARFISH(MOLLUSK, "Estrella de mar"),
    SEA_URCHIN(MOLLUSK, "Erizo de mar"),
    MOON_JELLYFISH(PELAGIC, "Medusa luna"),
    MANTA_RAY(PELAGIC, "Raya manta")
}
```

---

## CreatureSpec (presentation/aquarium)

Modelo ligero usado en la capa de presentación para el renderizado y detección de desbloqueos:

```kotlin
data class CreatureSpec(
    val emoji: String,
    val species: CreatureSpecies,
    val category: MarineCategory,
    val unlockLevel: Int,
    val swimDuration: Int,      // ms del ciclo de natación; 0 = criatura fija
    val wobbleAmplitude: Float  // amplitud de oscilación vertical (fracción de pantalla)
)
```

| Emoji | Especie | Categoría | Nivel desbloqueo |
|---|---|---|---|
| 🐟 | CLOWNFISH | FISH | 2 |
| 🪸 | BRAIN_CORAL | FLORA | 2 |
| 🦞 | LOBSTER | CRUSTACEAN | 2 |
| 🐚 | SEA_URCHIN | MOLLUSK | 2 |
| 🦈 | MANTA_RAY | PELAGIC | 2 |
| 🐠 | ANGELFISH | FISH | 5 |
| 🌿 | ANEMONE | FLORA | 5 |
| 🦀 | HERMIT_CRAB | CRUSTACEAN | 5 |
| ⭐ | STARFISH | MOLLUSK | 5 |
| 🪼 | MOON_JELLYFISH | PELAGIC | 5 |

---

## Room — androidMain (v6)

| Entity | Tabla |
|--------|-------|
| `WorkBlockEntity` | `work_blocks` |
| `BlockCategoryEntity` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity` | `day_tasks` — `scheduleType`, `date?`, `time?`, `recurrence?` JSON, `status`, `completedAt?`, `postponedTo?`, `sourceTaskId?`, `blockId?` FK SET_NULL |
| `RecurringTaskDefEntity` | `recurring_task_defs` — `blockId` FK CASCADE, `recurrence` JSON |
| `DaySummaryEntity` | `day_summaries` |
| `BlockStreakEntity` | `block_streaks` — PK `blockId` |
| `EcosystemStateEntity` | `ecosystem_states` |
| `MarineCreatureEntity` | `marine_creatures` |

Fechas almacenadas como String ISO. Enums como String. Sealed classes como JSON con `kotlinx-serialization`.
