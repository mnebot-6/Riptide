# Modelos de datos -- Riptide

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

| Estado | Descripcion |
|--------|-------------|
| PENDING | Estado por defecto |
| COMPLETED | Da 10 XP al ecosistema (solo la primera vez, controlado por `hasBeenRewarded`) |
| EXPIRED | Marcada por resumen nocturno. Sigue siendo completable |
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

`notificationsEnabled`: si `true` y la tarea tiene hora, se programa un `TaskReminderWorker` a esa hora. Solo relevante si `schedule` incluye `time != null`.

---

## RecurringTaskDef

```kotlin
data class RecurringTaskDef(
    val id: String,
    val blockId: String,
    val title: String,
    val time: LocalTime?,   // nullable -- hora opcional
    val recurrence: Recurrence,
    val isActive: Boolean,
    val notificationsEnabled: Boolean = false
)
```

`notificationsEnabled`: se propaga a cada `DayTask` generado por `RecurringTaskGenerator`.

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
    CEPHALOPOD(false), REPTILE(false), MAMMAL(false), DECORATION(false),
    COMPANION(false)  // Easter egg -- no se muestra hasta desbloquear; no sale por lootbox
}
```

Las 5 categorias base se desbloquean desde el inicio. Las demas requieren condiciones especiales.

DECORATION no recibe XP regular ni participa en la redistribucion de categorias.
COMPANION es una categoria oculta para easter eggs (Bimba).

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
    val lastActiveDate: LocalDate,
    val longestStreak: Int = 0
)
```

`longestStreak`: racha mas larga historica del bloque (Room v11).

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

Un registro por categoria (10 en total). `isUnlocked` controla si la categoria participa en el ecosistema.

Curva de niveles:

| Nivel | XP total | Coste |
|---|---|---|
| 1 | 0 | -- |
| 2 | 1 | 1 |
| 3 | 21 | 20 |
| 4 | 61 | 40 |
| 5 | 126 | 65 |
| 6 | 226 | 100 |
| 7 | 376 | 150 |
| N | -- | anterior x 1.5 |

---

## CreatureRarity

```kotlin
enum class CreatureRarity(val weight: Float, val displayName: String) {
    COMMON(0.40f, "Comun"),
    UNCOMMON(0.30f, "Poco comun"),
    RARE(0.20f, "Raro"),
    EPIC(0.08f, "Epico"),
    LEGENDARY(0.02f, "Legendario")
}
```

Los pesos son probabilidades relativas, normalizadas entre las especies aun bloqueadas de la categoria al resolver una lootbox.

---

## PendingLootbox

```kotlin
data class PendingLootbox(
    val category: MarineCategory,
    val categoryLevel: Int
)
```

Se genera cuando una categoria alcanza un nivel definido en `CATEGORY_UNLOCK_LEVELS`. La especie se resuelve al ABRIR (no al ganar).

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

`experience` y `creatureLevel` se actualizan con cada evento XP de la categoria.

```kotlin
enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {
    // FISH (9)
    CLOWNFISH(FISH, "Pez payaso"),
    ANGELFISH(FISH, "Pez angel"),
    PUFFERFISH(FISH, "Pez globo"),
    SURGEONFISH(FISH, "Pez cirujano"),
    LIONFISH(FISH, "Pez leon"),
    SUNFISH(FISH, "Pez luna"),
    BUTTERFLYFISH(FISH, "Pez mariposa"),
    SEAHORSE(FISH, "Caballito de mar"),
    MORAY_EEL(FISH, "Morena"),

    // FLORA (9)
    BRAIN_CORAL(FLORA, "Coral cerebro"),
    ANEMONE(FLORA, "Anemona"),
    KELP(FLORA, "Alga kelp"),
    POSIDONIA(FLORA, "Posidonia"),
    FAN_CORAL(FLORA, "Coral abanico"),
    TUBE_SPONGE(FLORA, "Esponja tubular"),
    SEA_GRASS(FLORA, "Hierba marina"),
    FIRE_CORAL(FLORA, "Coral de fuego"),
    STAGHORN_CORAL(FLORA, "Coral cuerno de ciervo"),

    // CRUSTACEAN (9)
    LOBSTER(CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(CRUSTACEAN, "Cangrejo ermitano"),
    SHRIMP(CRUSTACEAN, "Gamba"),
    SPIDER_CRAB(CRUSTACEAN, "Cangrejo arana"),
    BARNACLE(CRUSTACEAN, "Percebes"),
    KRILL(CRUSTACEAN, "Krill"),
    HORSESHOE_CRAB(CRUSTACEAN, "Cangrejo herradura"),
    MANTIS_SHRIMP(CRUSTACEAN, "Gamba mantis"),
    COCONUT_CRAB(CRUSTACEAN, "Cangrejo cocotero"),

    // MOLLUSK (9)
    SEA_URCHIN(MOLLUSK, "Erizo de mar"),
    STARFISH(MOLLUSK, "Estrella de mar"),
    OYSTER(MOLLUSK, "Ostra"),
    NAUTILUS(MOLLUSK, "Nautilus"),
    GIANT_CLAM(MOLLUSK, "Almeja gigante"),
    CONCH(MOLLUSK, "Caracola"),
    SCALLOP(MOLLUSK, "Vieira"),
    SEA_SLUG(MOLLUSK, "Nudibranquio"),
    SEA_CUCUMBER(MOLLUSK, "Pepino de mar"),

    // PELAGIC (9)
    MANTA_RAY(PELAGIC, "Raya manta"),
    MOON_JELLYFISH(PELAGIC, "Medusa luna"),
    WHALE_SHARK(PELAGIC, "Tiburon ballena"),
    HAMMERHEAD(PELAGIC, "Pez martillo"),
    BARRACUDA(PELAGIC, "Barracuda"),
    BLUEFIN_TUNA(PELAGIC, "Atun rojo"),
    FLYING_FISH(PELAGIC, "Pez volador"),
    LIONSMANE_JELLYFISH(PELAGIC, "Medusa melena de leon"),
    SWORDFISH(PELAGIC, "Pez espada"),

    // CEPHALOPOD (6)
    OCTOPUS(CEPHALOPOD, "Pulpo"),
    SQUID(CEPHALOPOD, "Calamar"),
    CUTTLEFISH(CEPHALOPOD, "Sepia"),
    BLUE_RINGED_OCTOPUS(CEPHALOPOD, "Pulpo anillado"),
    CHAMBERED_NAUTILUS(CEPHALOPOD, "Nautilus camara"),
    GIANT_PACIFIC_OCTOPUS(CEPHALOPOD, "Pulpo gigante del Pacifico"),

    // REPTILE (6)
    SEA_TURTLE(REPTILE, "Tortuga marina"),
    MARINE_IGUANA(REPTILE, "Iguana marina"),
    GREEN_SEA_TURTLE(REPTILE, "Tortuga verde"),
    SEA_SNAKE(REPTILE, "Serpiente marina"),
    LEATHERBACK_TURTLE(REPTILE, "Tortuga laud"),
    SALTWATER_CROCODILE(REPTILE, "Cocodrilo marino"),

    // MAMMAL (6)
    DOLPHIN(MAMMAL, "Delfin"),
    SEAL(MAMMAL, "Foca"),
    BLUE_WHALE(MAMMAL, "Ballena azul"),
    SEA_OTTER(MAMMAL, "Nutria marina"),
    MANATEE(MAMMAL, "Manati"),
    NARWHAL(MAMMAL, "Narval"),

    // DECORATION (6)
    TREASURE_CHEST(DECORATION, "Cofre del tesoro"),
    ANCHOR(DECORATION, "Ancla"),
    SUNKEN_SHIP(DECORATION, "Barco hundido"),
    DIVING_HELMET(DECORATION, "Escafandra"),
    CORAL_THRONE(DECORATION, "Trono de coral"),
    GOLDEN_TRIDENT(DECORATION, "Tridente dorado"),

    // COMPANION -- easter egg oculto
    BIMBA(COMPANION, "Bimba")
}
```

Total: **70 especies** en 10 categorias. Rareza asignada en `CreatureSpec` (codigo, no DB).

---

## Room -- androidMain (v12)

| Entity | Tabla |
|--------|-------|
| `WorkBlockEntity` | `work_blocks` -- + `updatedAt`, `isDeleted` |
| `BlockCategoryEntity` | `block_categories` -- PK `(blockId, category)`, FK CASCADE |
| `DayTaskEntity` | `day_tasks` -- + `hasBeenRewarded`, `notificationsEnabled`, `updatedAt`, `isDeleted` |
| `RecurringTaskDefEntity` | `recurring_task_defs` -- `time String?` nullable, + `notificationsEnabled`, `updatedAt`, `isDeleted` |
| `DaySummaryEntity` | `day_summaries` -- + `updatedAt` |
| `BlockStreakEntity` | `block_streaks` -- PK `blockId`, + `longestStreak`, `updatedAt` |
| `EcosystemStateEntity` | `ecosystem_states` -- `isUnlocked Boolean`, + `updatedAt` |
| `MarineCreatureEntity` | `marine_creatures` -- + `updatedAt` |

### Migraciones

| Migracion | Cambio |
|---|---|
| 8 -> 9 | `ALTER TABLE day_tasks ADD COLUMN hasBeenRewarded INTEGER NOT NULL DEFAULT 0` |
| 9 -> 10 | `+ notificationsEnabled` en `day_tasks` y `recurring_task_defs` |
| 10 -> 11 | `+ longestStreak INTEGER` en `block_streaks` |
| 11 -> 12 | `+ updatedAt TEXT` en 8 tablas + `+ isDeleted INTEGER` en 3 tablas (11 ALTER TABLE) |

### DAOs -- queries de sync

Todos los DAOs incluyen:
- `getModifiedSince(timestamp: String)`: entities modificadas desde una fecha ISO
- `upsertAll(entities: List<Entity>)`: insert o update batch
- `stampUpdatedAt(id: String, timestamp: String)`: actualizar timestamp tras mutacion

Queries existentes filtran `isDeleted = 0` (soft delete transparente).
