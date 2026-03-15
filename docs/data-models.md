# Modelos de datos — Riptide

Todos los modelos viven en `commonMain/kotlin/com/mnebot/riptide/domain/model/` como clases Kotlin puras, sin dependencias de Room ni de Android.

---

## WorkBlock

```kotlin
data class WorkBlock(
    val id: String,
    val name: String,
    val marineCategories: List<MarineCategory>,
    val color: String,
    val icon: String,
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
    val dayOfWeek: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)
```

---

## TaskStatus

```kotlin
enum class TaskStatus { PENDING, COMPLETED, EXPIRED, POSTPONED }
```

| Estado | Descripción |
|--------|-------------|
| PENDING | Estado por defecto |
| COMPLETED | Da 10 XP al ecosistema y a criaturas individuales |
| EXPIRED | Pendiente evaluable al hacer el resumen nocturno |
| POSTPONED | No cuenta en resumen ni barra de progreso |

---

## TaskSchedule

```kotlin
sealed class TaskSchedule {
    data class OneTime(val date: LocalDate, val time: LocalTime?) : TaskSchedule()
    data class Recurring(val time: LocalTime, val recurrence: Recurrence) : TaskSchedule()
}
```

---

## DayTask

```kotlin
data class DayTask(
    val id: String,
    val blockId: String?,
    val title: String,
    val schedule: TaskSchedule,
    val status: TaskStatus,
    val completedAt: LocalDateTime?,
    val postponedTo: LocalDateTime?,
    val sourceTaskId: String?
)
```

---

## RecurringTaskDef

```kotlin
data class RecurringTaskDef(
    val id: String,
    val blockId: String,
    val title: String,
    val time: LocalTime?,   // nullable — hora opcional
    val recurrence: Recurrence,
    val isActive: Boolean
)
```

---

## BlockCategory

```kotlin
data class BlockCategory(val blockId: String, val category: MarineCategory)
```

---

## MarineCategory

```kotlin
enum class MarineCategory(val isUnlockedByDefault: Boolean) {
    FISH(true), FLORA(true), CRUSTACEAN(true), MOLLUSK(true), PELAGIC(true),
    CEPHALOPOD(false), REPTILE(false), MAMMAL(false), DECORATION(false)
}
```

Las 5 categorías base se desbloquean desde el inicio. Las demás requieren condiciones especiales (aún por definir en código — `unlockCategory()` existe en `EcosystemProcessor`).

DECORATION no recibe XP ni participa en la redistribución de categorías.

---

## DaySummary

```kotlin
data class DaySummary(
    val id: String,
    val date: LocalDate,
    val score: Float,           // NUNCA visible
    val tasksTotal: Int,        // solo tareas evaluables
    val tasksCompleted: Int,
    val streakDay: Int,
    val feedbackMessage: String
)
```

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
    val totalExperience: Int,   // NUNCA visible
    val currentLevel: Int,
    val isUnlocked: Boolean,
    val lastUpdated: LocalDateTime
)
```

Un registro por categoría (9 en total). `isUnlocked` controla si la categoría participa en el ecosistema.

Curva de niveles:

| Nivel | XP total | Coste |
|---|---|---|
| 1 | 0 | — |
| 2 | 1 | 1 |
| 3 | 21 | 20 |
| 4 | 61 | 40 |
| 5 | 126 | 65 |
| 6 | 226 | 100 |
| 7 | 376 | 150 |
| N | — | anterior × 1.5 |

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
```

`experience` y `creatureLevel` se actualizan con cada evento XP de la categoría. La criatura recién desbloqueada empieza con `experience=0` y no recibe XP hasta el siguiente evento.

```kotlin
enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {
    // FISH
    CLOWNFISH(FISH, "Pez payaso"),
    ANGELFISH(FISH, "Pez ángel"),
    PUFFERFISH(FISH, "Pez globo"),
    // FLORA
    BRAIN_CORAL(FLORA, "Coral cerebro"),
    ANEMONE(FLORA, "Anémona"),
    KELP(FLORA, "Alga kelp"),
    // CRUSTACEAN
    LOBSTER(CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(CRUSTACEAN, "Cangrejo ermitaño"),
    SHRIMP(CRUSTACEAN, "Gamba"),
    // MOLLUSK
    SEA_URCHIN(MOLLUSK, "Erizo de mar"),
    STARFISH(MOLLUSK, "Estrella de mar"),
    OYSTER(MOLLUSK, "Ostra"),
    // PELAGIC
    MANTA_RAY(PELAGIC, "Raya manta"),
    MOON_JELLYFISH(PELAGIC, "Medusa luna"),
    WHALE_SHARK(PELAGIC, "Tiburón ballena"),
    // CEPHALOPOD
    OCTOPUS(CEPHALOPOD, "Pulpo"),
    SQUID(CEPHALOPOD, "Calamar"),
    // REPTILE
    SEA_TURTLE(REPTILE, "Tortuga marina"),
    // MAMMAL
    DOLPHIN(MAMMAL, "Delfín"),
    SEAL(MAMMAL, "Foca"),
    BLUE_WHALE(MAMMAL, "Ballena azul"),
    // DECORATION
    TREASURE_CHEST(DECORATION, "Cofre del tesoro"),
    ANCHOR(DECORATION, "Ancla"),
    SUNKEN_SHIP(DECORATION, "Barco hundido")
}
```

---

## CreatureSpec (presentation/aquarium)

```kotlin
data class CreatureSpec(
    val emoji: String,
    val species: CreatureSpecies,
    val category: MarineCategory,
    val unlockLevel: Int,
    val swimDuration: Int,
    val wobbleAmplitude: Float,
    val speedScalePerLevel: Float  // + más rápido, - más lento, 0 sin cambio
)
```

| Emoji | Especie | Categoría | Nivel | speedScalePerLevel |
|---|---|---|---|---|
| 🐟 | CLOWNFISH | FISH | 2 | +0.05 |
| 🐠 | ANGELFISH | FISH | 4 | +0.05 |
| 🐡 | PUFFERFISH | FISH | 7 | +0.03 |
| 🪸 | BRAIN_CORAL | FLORA | 2 | 0 |
| 🌿 | ANEMONE | FLORA | 4 | 0 |
| 🎋 | KELP | FLORA | 6 | 0 |
| 🦞 | LOBSTER | CRUSTACEAN | 2 | +0.04 |
| 🦀 | HERMIT_CRAB | CRUSTACEAN | 4 | +0.05 |
| 🦐 | SHRIMP | CRUSTACEAN | 6 | +0.06 |
| 🐚 | SEA_URCHIN | MOLLUSK | 2 | 0 |
| ⭐ | STARFISH | MOLLUSK | 4 | 0 |
| 🦪 | OYSTER | MOLLUSK | 7 | 0 |
| 🦈 | MANTA_RAY | PELAGIC | 2 | +0.04 |
| 🪼 | MOON_JELLYFISH | PELAGIC | 4 | -0.02 |
| 🐋 | WHALE_SHARK | PELAGIC | 8 | -0.04 |
| 🐙 | OCTOPUS | CEPHALOPOD | 2 | +0.05 |
| 🦑 | SQUID | CEPHALOPOD | 5 | +0.06 |
| 🐢 | SEA_TURTLE | REPTILE | 2 | -0.03 |
| 🐬 | DOLPHIN | MAMMAL | 2 | +0.03 |
| 🦭 | SEAL | MAMMAL | 5 | -0.02 |
| 🐳 | BLUE_WHALE | MAMMAL | 8 | -0.04 |
| 🪙 | TREASURE_CHEST | DECORATION | 1 | 0 |
| ⚓ | ANCHOR | DECORATION | 1 | 0 |
| 🚢 | SUNKEN_SHIP | DECORATION | 1 | 0 |

Efectos visuales por `creatureLevel`:
- Tamaño: `baseSize * (0.8f + (creatureLevel - 1) * 0.10f)` — empieza al 80%, +10% por nivel
- Velocidad: `swimDuration / max(0.3f, 1f + (creatureLevel - 1) * speedScalePerLevel)`

---

## Room — androidMain (v8)

| Entity | Tabla |
|--------|-------|
| `WorkBlockEntity` | `work_blocks` |
| `BlockCategoryEntity` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity` | `day_tasks` |
| `RecurringTaskDefEntity` | `recurring_task_defs` — `time String?` nullable |
| `DaySummaryEntity` | `day_summaries` |
| `BlockStreakEntity` | `block_streaks` — PK `blockId` |
| `EcosystemStateEntity` | `ecosystem_states` — `isUnlocked Boolean` |
| `MarineCreatureEntity` | `marine_creatures` |
