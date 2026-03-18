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
| COMPLETED | Da 10 XP al ecosistema (solo la primera vez, controlado por `hasBeenRewarded`) |
| EXPIRED | Marcada por resumen nocturno. Sigue siendo completable (⌛ + checkbox) |
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
    val sourceTaskId: String?,
    val hasBeenRewarded: Boolean = false
)
```

`hasBeenRewarded`: se pone a `true` al completar la tarea por primera vez. Evita dar XP duplicado al desmarcar y volver a marcar. Una vez `true`, nunca se resetea.

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

Las 5 categorías base se desbloquean desde el inicio. Las demás requieren condiciones especiales (`unlockCategory()` existe en `EcosystemProcessor`).

DECORATION no recibe XP ni participa en la redistribución de categorías.

---

## DaySummary

```kotlin
data class DaySummary(
    val id: String,
    val date: LocalDate,
    val score: Float,           // NUNCA visible
    val tasksTotal: Int,
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
    CLOWNFISH(FISH, "Pez payaso"),
    ANGELFISH(FISH, "Pez ángel"),
    PUFFERFISH(FISH, "Pez globo"),
    BRAIN_CORAL(FLORA, "Coral cerebro"),
    ANEMONE(FLORA, "Anémona"),
    KELP(FLORA, "Alga kelp"),
    LOBSTER(CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(CRUSTACEAN, "Cangrejo ermitaño"),
    SHRIMP(CRUSTACEAN, "Gamba"),
    SEA_URCHIN(MOLLUSK, "Erizo de mar"),
    STARFISH(MOLLUSK, "Estrella de mar"),
    OYSTER(MOLLUSK, "Ostra"),
    MANTA_RAY(PELAGIC, "Raya manta"),
    MOON_JELLYFISH(PELAGIC, "Medusa luna"),
    WHALE_SHARK(PELAGIC, "Tiburón ballena"),
    OCTOPUS(CEPHALOPOD, "Pulpo"),
    SQUID(CEPHALOPOD, "Calamar"),
    SEA_TURTLE(REPTILE, "Tortuga marina"),
    DOLPHIN(MAMMAL, "Delfín"),
    SEAL(MAMMAL, "Foca"),
    BLUE_WHALE(MAMMAL, "Ballena azul"),
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
    val swimDuration: Int,          // ms; 0 = fija
    val wobbleAmplitude: Float,     // amplitud onda Y primaria (fracción de banda)
    val speedScalePerLevel: Float,  // + más rápido, - más lento al crecer
    val swimZone: SwimZone,         // banda vertical asignada
    val personalYFraction: Float,   // [0..1] cota personal dentro de la banda
    val waveCount: Int,             // entero → sin salto en loop
    val erraticness: Float,         // [0..1] peso onda secundaria (PHI·waveCount)
    val driftSpeed: Float,          // velocidad deriva lenta del eje Y (fraccionario)
    val driftAmplitude: Float,      // cuánto se desplaza el centro de nado
    val pauseFraction: Float,       // fracción del ciclo en pausa en cada extremo
    val easingType: EasingType,     // SMOOTH / BURST / CRAWL
    val verticalCoupling: Float,    // [0..1] arco vertical acoplado a X
    val microWobble: Float,         // amplitud oscilación alta frecuencia (aleta/cola)
    val xErraticness: Float,        // perturbación aperiódica de X
    val fixedWobbleScale: Float,    // para criaturas fijas: escala de ondeo de corriente
    val tempoVariation: Float       // [0..1) modulación de velocidad continua
)
```

### SwimZone

| Zona | centerFraction | bandFraction | Especies |
|---|---|---|---|
| SURFACE | 0.16 | 0.07 | Delfín, Ballena azul |
| UPPER | 0.30 | 0.10 | Clownfish, Medusa luna, Foca |
| MID | 0.47 | 0.13 | Angelfish, Pufferfish, Manta ray, Calamar, Tortuga, Tiburón ballena |
| LOWER | 0.64 | 0.09 | Langosta, Cangrejo, Gamba, Pulpo |
| BOTTOM | 0.82 | 0.05 | Flora, Moluscos, Decoración |

### EasingType

| Tipo | Comportamiento | Especies |
|---|---|---|
| SMOOTH | Coseno estándar — entrada y salida suaves | Peces, tortuga, mamíferos, manta |
| BURST | 75% en el primer 25%, luego planea | Gamba, calamar, pulpo |
| CRAWL | 93% lineal | Langosta, cangrejo ermitaño |

### Efectos visuales por `creatureLevel`

- Tamaño: `baseSize * (0.8f + (creatureLevel - 1) * 0.10f)`
- Velocidad: `swimDuration / max(0.3f, 1f + (creatureLevel - 1) * speedScalePerLevel)`

---

## Room — androidMain (v9)

| Entity | Tabla |
|--------|-------|
| `WorkBlockEntity` | `work_blocks` |
| `BlockCategoryEntity` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity` | `day_tasks` — + `hasBeenRewarded: Boolean` |
| `RecurringTaskDefEntity` | `recurring_task_defs` — `time String?` nullable |
| `DaySummaryEntity` | `day_summaries` |
| `BlockStreakEntity` | `block_streaks` — PK `blockId` |
| `EcosystemStateEntity` | `ecosystem_states` — `isUnlocked Boolean` |
| `MarineCreatureEntity` | `marine_creatures` |

### Migraciones

| Migración | SQL |
|---|---|
| 8 → 9 | `ALTER TABLE day_tasks ADD COLUMN hasBeenRewarded INTEGER NOT NULL DEFAULT 0` |
