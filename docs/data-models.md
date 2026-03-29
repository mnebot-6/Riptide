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
    val hasBeenRewarded: Boolean = false,
    val notificationsEnabled: Boolean = false
)
```

`hasBeenRewarded`: se pone a `true` al completar la tarea por primera vez. Evita dar XP duplicado al desmarcar y volver a marcar. Una vez `true`, nunca se resetea.

`notificationsEnabled`: si `true` y la tarea tiene hora, se programa un `TaskReminderWorker` a esa hora. Por defecto `false`. Solo relevante si `schedule` incluye `time != null`.

---

## RecurringTaskDef

```kotlin
data class RecurringTaskDef(
    val id: String,
    val blockId: String,
    val title: String,
    val time: LocalTime?,   // nullable — hora opcional
    val recurrence: Recurrence,
    val isActive: Boolean,
    val notificationsEnabled: Boolean = false
)
```

`notificationsEnabled`: se propaga a cada `DayTask` generado por `RecurringTaskGenerator`. Equivalente al campo del mismo nombre en `DayTask`.

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

## CreatureRarity

```kotlin
enum class CreatureRarity(val weight: Float, val displayName: String) {
    COMMON(0.40f, "Común"),
    UNCOMMON(0.30f, "Poco común"),
    RARE(0.20f, "Raro"),
    EPIC(0.08f, "Épico"),
    LEGENDARY(0.02f, "Legendario")
}
```

Los pesos son probabilidades relativas, normalizadas entre las especies aún bloqueadas de la categoría al resolver una lootbox.

---

## PendingLootbox

```kotlin
data class PendingLootbox(
    val category: MarineCategory,
    val categoryLevel: Int
)
```

Representa una lootbox pendiente de abrir. Se genera cuando una categoría alcanza un nivel definido en `CATEGORY_UNLOCK_LEVELS`. La especie se resuelve al ABRIR (no al ganar).

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
    // FISH (6)
    CLOWNFISH(FISH, "Pez payaso"),
    ANGELFISH(FISH, "Pez ángel"),
    PUFFERFISH(FISH, "Pez globo"),
    SURGEONFISH(FISH, "Pez cirujano"),
    LIONFISH(FISH, "Pez león"),
    SUNFISH(FISH, "Pez luna"),
    // FLORA (5)
    BRAIN_CORAL(FLORA, "Coral cerebro"),
    ANEMONE(FLORA, "Anémona"),
    KELP(FLORA, "Alga kelp"),
    POSIDONIA(FLORA, "Posidonia"),
    FAN_CORAL(FLORA, "Coral abanico"),
    // CRUSTACEAN (5)
    LOBSTER(CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(CRUSTACEAN, "Cangrejo ermitaño"),
    SHRIMP(CRUSTACEAN, "Gamba"),
    SPIDER_CRAB(CRUSTACEAN, "Cangrejo araña"),
    BARNACLE(CRUSTACEAN, "Percebes"),
    // MOLLUSK (5)
    SEA_URCHIN(MOLLUSK, "Erizo de mar"),
    STARFISH(MOLLUSK, "Estrella de mar"),
    OYSTER(MOLLUSK, "Ostra"),
    NAUTILUS(MOLLUSK, "Nautilus"),
    GIANT_CLAM(MOLLUSK, "Almeja gigante"),
    // PELAGIC (5)
    MANTA_RAY(PELAGIC, "Raya manta"),
    MOON_JELLYFISH(PELAGIC, "Medusa luna"),
    WHALE_SHARK(PELAGIC, "Tiburón ballena"),
    HAMMERHEAD(PELAGIC, "Pez martillo"),
    BARRACUDA(PELAGIC, "Barracuda"),
    // CEPHALOPOD (4)
    OCTOPUS(CEPHALOPOD, "Pulpo"),
    SQUID(CEPHALOPOD, "Calamar"),
    CUTTLEFISH(CEPHALOPOD, "Sepia"),
    BLUE_RINGED_OCTOPUS(CEPHALOPOD, "Pulpo anillado"),
    // REPTILE (2)
    SEA_TURTLE(REPTILE, "Tortuga marina"),
    MARINE_IGUANA(REPTILE, "Iguana marina"),
    // MAMMAL (5)
    DOLPHIN(MAMMAL, "Delfín"),
    SEAL(MAMMAL, "Foca"),
    BLUE_WHALE(MAMMAL, "Ballena azul"),
    SEA_OTTER(MAMMAL, "Nutria marina"),
    MANATEE(MAMMAL, "Manatí"),
    // DECORATION (3)
    TREASURE_CHEST(DECORATION, "Cofre del tesoro"),
    ANCHOR(DECORATION, "Ancla"),
    SUNKEN_SHIP(DECORATION, "Barco hundido")
}
```

Total: 40 especies. Rareza asignada en `CreatureSpec` (código, no DB).

---

## CreatureSpec (presentation/aquarium)

```kotlin
data class CreatureSpec(
    val emoji: String,
    val species: CreatureSpecies,
    val category: MarineCategory,
    val rarity: CreatureRarity,
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
    val tempoVariation: Float,      // [0..1) modulación de velocidad continua
    val sizeMultiplier: Float,      // escala relativa del tamaño base (0.4–1.05)
    val instanceCount: Int,         // número de instancias Canvas (flora fija)
    val emojiRotation: Float        // rotación en grados para emojis orientados
)
```

`sizeMultiplier`: multiplicador de tamaño relativo. El tamaño final renderizado es `baseSize * sizeScale * sizeMultiplier`. Flora grande (Kelp, BrainCoral, FanCoral, Posidonia) está escalada a ~0.50 para verse proporcionada en el nuevo terreno suavizado. SeaUrchin: 0.55.

`instanceCount`: solo para criaturas fijas (flora, decoración). El Canvas renderer se dibuja este número de veces con posiciones pseudoaleatorias (round-robin).

`emojiRotation`: rotación en grados. Langosta (-90°) apunta arriba; peces y otros depredadores (0°) apuntan a la derecha naturalmente.

### SwimZone

| Zona | centerFraction | bandFraction | Especies |
|---|---|---|---|
| SURFACE | 0.16 | 0.07 | Delfín, Ballena azul |
| UPPER | 0.30 | 0.10 | Clownfish, Medusa luna, Foca |
| MID | 0.47 | 0.13 | Angelfish, Pufferfish, Manta ray, Calamar, Tortuga, Tiburón ballena |
| LOWER | 0.64 | 0.09 | Langosta, Cangrejo, Gamba, Pulpo |
| BOTTOM | 0.82 | 0.05 | Flora (BrainCoral 0.50, Anemone 0.42, Kelp 0.50, Posidonia 0.52, FanCoral 0.48), Moluscos (SeaUrchin 0.55, Starfish 0.90, Oyster 0.85, etc.), Decoración |

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

## Room — androidMain (v10)

| Entity | Tabla |
|--------|-------|
| `WorkBlockEntity` | `work_blocks` |
| `BlockCategoryEntity` | `block_categories` — PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity` | `day_tasks` — + `hasBeenRewarded: Boolean`, + `notificationsEnabled: Boolean` |
| `RecurringTaskDefEntity` | `recurring_task_defs` — `time String?` nullable, + `notificationsEnabled: Boolean` |
| `DaySummaryEntity` | `day_summaries` |
| `BlockStreakEntity` | `block_streaks` — PK `blockId` |
| `EcosystemStateEntity` | `ecosystem_states` — `isUnlocked Boolean` |
| `MarineCreatureEntity` | `marine_creatures` |

### Migraciones

| Migración | SQL |
|---|---|
| 8 → 9 | `ALTER TABLE day_tasks ADD COLUMN hasBeenRewarded INTEGER NOT NULL DEFAULT 0` |
| 9 → 10 | `ALTER TABLE day_tasks ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 0` + `ALTER TABLE recurring_task_defs ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 0` |
