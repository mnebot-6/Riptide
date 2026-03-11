# Modelos de datos — Riptide

Todos los modelos viven en `commonMain` como clases Kotlin puras (sin anotaciones de Room).

---

## WorkBlock

```kotlin
data class WorkBlock(
    val id: String,                          // UUID
    val name: String,
    val marineCategories: List<MarineCategory>, // asignado automáticamente
    val color: String,                       // hex, ej: "#1A73E8"
    val icon: String,                        // emoji, ej: "💼"
    val recurrence: Recurrence,
    val isActive: Boolean
)
```

`marineCategories` nunca se muestra al usuario. Se asigna vía `MarineCategoryAssigner`.

---

## Recurrence

```kotlin
sealed class Recurrence {
    object None : Recurrence()
    data class Weekly(val slots: List<WeeklySlot>) : Recurrence()
}

data class WeeklySlot(
    val dayOfWeek: Int,        // 1=Lunes, 7=Domingo
    val startTime: LocalTime?,
    val endTime: LocalTime?
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

Tabla separada con PK compuesta `(blockId, category)` y FK CASCADE desde `WorkBlock`.

---

## DayTask

```kotlin
data class DayTask(
    val id: String,
    val blockId: String,
    val date: LocalDate,
    val title: String,
    val estimatedMinutes: Int?,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime?,
    val order: Int
)
```

---

## DaySummary

```kotlin
data class DaySummary(
    val id: String,
    val date: LocalDate,
    val score: Float,          // 0.0-1.0, NUNCA visible al usuario
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
    else tasks.count { it.isCompleted }.toFloat() / tasks.size
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

---

## EcosystemState

```kotlin
data class EcosystemState(
    val id: String,
    val category: MarineCategory,
    val totalExperience: Int,  // NUNCA visible al usuario
    val currentLevel: Int,
    val lastUpdated: LocalDateTime
)
```

---

## MarineCreature

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
```

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
