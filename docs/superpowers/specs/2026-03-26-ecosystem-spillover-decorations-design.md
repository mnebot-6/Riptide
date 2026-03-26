# Diseño: XP Spillover 80/20 + Decoration Unlock Conditions

**Fecha:** 2026-03-26
**Estado:** Aprobado

---

## Contexto

Dos mejoras independientes al sistema de ecosistema marino de Riptide:

1. **XP Spillover 80/20** — El 20% del XP ganado en cada tarea se desvía automáticamente a la categoría marina desbloqueada con menos experiencia total, evitando que el acuario sea unidireccional.
2. **Decoration Unlock Conditions** — Las 3 especies de DECORATION (antes sin mecanismo de desbloqueo) ahora tienen condiciones específicas y temáticas que el usuario debe cumplir.

---

## Feature 1: XP Spillover 80/20

### Objetivo

Cuando se completa una tarea y se distribuye XP, el 80% va a las categorías objetivo del bloque y el 20% va a la categoría desbloqueada con menor `totalExperience` (excluyendo DECORATION y las propias categorías objetivo).

### Reglas

- El split 80/20 se aplica **por unidad de XP por categoría objetivo**, no sobre el total.
- Si el candidato débil ya está entre las categorías objetivo (o no hay candidatos), se da el 100% a los objetivos sin split.
- El XP de spillover llega con `fromSpillover = true` para **no generar otro spillover** (evitar cascada).
- El mecanismo de **overflow existente** (50% cuando todas las especies de una categoría están desbloqueadas) se mantiene intacto e independiente. Ambos mecanismos pueden coexistir: el spillover se aplica en la capa de distribución externa; el overflow en la capa `addXp()` interna.
- DECORATION **siempre excluida** del spillover (no recibe XP por ninguna vía).

### Algoritmo

```
addXpForTask(categories):
  targets = resolvedTargets(categories)       // lógica actual sin cambios
  xpEach = XP_PER_TASK / targets.size

  spilloverTarget = findSpilloverTarget(excludes = targets)

  for each target in targets:
    if spilloverTarget != null:
      actualXp  = floor(xpEach * 0.80)
      spilloverXp = xpEach - actualXp         // garantiza 80+20 = 100% exacto
      addXp(target, actualXp)
      addXp(spilloverTarget, spilloverXp, fromSpillover = true)
    else:
      addXp(target, xpEach)                   // sin cambio

addNightBonus(score, bestStreak, categories): // misma lógica de split

findSpilloverTarget(excludes: List<MarineCategory>): MarineCategory?
  candidates = ecosystemStateRepository.getUnlocked()
               .filter { !DECORATION && not in excludes }
  if candidates.isEmpty() → return null
  minXp = candidates.minOf { totalExperience }
  tied  = candidates.filter { totalExperience == minXp }
  return tied.random()
```

### Cambios de código

| Archivo | Cambio |
|---|---|
| `EcosystemProcessor.kt` | Añadir `findSpilloverTarget()`. Modificar `addXpForTask()` y `addNightBonus()` para aplicar split. Añadir parámetro `fromSpillover: Boolean = false` a `addXp()`. |

### Tests

- Split 80/20 se aplica cuando hay candidato débil válido.
- Sin split cuando el candidato débil ya está entre targets.
- Sin split cuando solo hay 1 categoría desbloqueada.
- `fromSpillover = true` no genera otro spillover.
- DECORATION nunca es candidato de spillover.
- En empate de `totalExperience`, se elige una de las empatadas (no null).

---

## Feature 2: Decoration Unlock Conditions

### Especies y condiciones

| Especie | Emoji | Condición | Temática |
|---|---|---|---|
| `TREASURE_CHEST` | 🗃️ | 7 días de calendario consecutivos, cada uno con `DaySummary.score == 1.0f` y sin huecos entre fechas | El cofre se revela tras una semana perfecta |
| `ANCHOR` | ⚓ | 100 tareas completadas (acumulado histórico) | El ancla se clava cuando has construido historia |
| `SUNKEN_SHIP` | 🚢 | Live wallpaper de Riptide activado al menos una vez | El barco descansa en el fondo de tu teléfono |

### Arquitectura

#### Nuevo: `DecorationUnlockChecker` (`commonMain/domain`)

Clase con las siguientes dependencias:
- `DaySummaryRepository`
- `DayTaskRepository`
- `MarineCreatureRepository`
- `EcosystemProcessor`
- `UserPreferencesRepository`

Métodos públicos:
```kotlin
suspend fun checkAll(): List<CreatureSpecies>          // comprueba las 3
suspend fun checkTreasureChest(): CreatureSpecies?     // 7 días perfectos
suspend fun checkAnchor(): CreatureSpecies?            // 100 tareas
suspend fun checkSunkenShip(): CreatureSpecies?        // wallpaper activo
suspend fun getProgress(): DecorationProgress          // para UI
```

Cada `checkX()` es **idempotente**: si la criatura ya existe en DB, retorna null sin escribir nada.

#### Nuevo: `DecorationProgress` (`commonMain/domain/model`)

```kotlin
data class DecorationProgress(
    val perfectDaysStreak: Int,       // días consecutivos actuales con score 1.0
    val completedTasksTotal: Int,     // acumulado histórico
    val wallpaperActivated: Boolean   // guardado en DataStore
)
```

#### Modificación: `PendingLootbox`

```kotlin
@Serializable
data class PendingLootbox(
    val category: MarineCategory,
    val categoryLevel: Int,
    val directSpecies: CreatureSpecies? = null  // nuevo; null = flujo lootbox normal
)
```

El campo tiene `default = null` → retrocompatible con entradas existentes en DataStore.

#### Interacción spillover ↔ overflow

`addXp()` actualmente acepta `fromOverflow: Boolean`. Se añade `fromSpillover: Boolean` como parámetro separado. Cuando **cualquiera** de los dos es `true`, no se evalúan redistribuciones adicionales (ni overflow ni spillover). Esto evita toda cascada. El overflow y el spillover pueden coexistir en la misma llamada raíz, pero nunca se propagan más de un nivel.

#### Modificación: `LootboxResolver`

Cuando `pendingLootbox.directSpecies != null`, retorna esa especie directamente sin weighted-random. El resto del flujo de apertura (crear `MarineCreature`, animación) permanece idéntico.

#### Nuevo: `DayTaskRepository.countCompletedAllTime(): Int`

Query simple sobre `day_tasks` WHERE `status = 'COMPLETED'`. Sin migración de esquema (solo nueva query en DAO).

#### Nuevo: `UserPreferencesRepository` — clave `wallpaper_activated`

```kotlin
suspend fun isWallpaperActivated(): Boolean
suspend fun setWallpaperActivated()
```

Almacenada en DataStore. Escrita desde `RiptideWallpaperService.onVisibilityChanged(true)`.

### Flujo de desbloqueo

Cuando `checkX()` detecta condición cumplida:
1. `ecosystemProcessor.unlockCategory(DECORATION)` — idempotente.
2. Crear `MarineCreature` en DB para la especie concreta.
3. Añadir `PendingLootbox(DECORATION, 0, directSpecies = species)` a `userPrefsRepo.getPendingLootboxes()`.
4. Retornar la especie desbloqueada.

### Puntos de disparo

| Dónde | Qué llama | Condiciones comprobadas |
|---|---|---|
| `NightSummaryProcessor.processDay()` final | `checker.checkAll()` | TREASURE_CHEST + ANCHOR |
| `RiptideWallpaperService.onVisibilityChanged(true)` | `checker.checkSunkenShip()` | SUNKEN_SHIP |
| `MainViewModel.init` | `checker.checkAll()` | Las 3 (safety net) |

### UI en EcosystemScreen

Las criaturas de DECORATION bloqueadas muestran su condición con progreso cuando aplica:

| Especie | Texto UI |
|---|---|
| TREASURE_CHEST | `"7 días perfectos seguidos (X/7)"` |
| ANCHOR | `"100 tareas completadas (X/100)"` |
| SUNKEN_SHIP | `"Activa el wallpaper de Riptide"` |

El ViewModel expone `DecorationProgress` para que `LockedCreatureCard` renderice estos hints específicos en lugar del texto genérico actual.

### Cambios de código

| Archivo | Cambio |
|---|---|
| `PendingLootbox.kt` | Añadir `directSpecies: CreatureSpecies? = null` |
| `LootboxResolver.kt` | Retornar `directSpecies` directamente si no es null |
| `DecorationUnlockChecker.kt` | **Nuevo** — lógica de comprobación de las 3 condiciones |
| `DecorationProgress.kt` | **Nuevo** — modelo de progreso para UI |
| `DayTaskRepository.kt` (interface) | Añadir `countCompletedAllTime(): Int` |
| `DayTaskDao.kt` (androidMain) | Implementar query `countCompletedAllTime()` |
| `UserPreferencesRepository.kt` | Añadir `isWallpaperActivated()` + `setWallpaperActivated()` |
| `UserPreferencesRepositoryImpl.kt` | Implementar con DataStore key `wallpaper_activated` |
| `NightSummaryProcessor.kt` | Aceptar `DecorationUnlockChecker?` y llamar `checkAll()` al final |
| `RiptideWallpaperService.kt` | Llamar `checkSunkenShip()` en `onVisibilityChanged(true)` |
| `MainViewModel.kt` | Llamar `checkAll()` en init |
| `EcosystemScreen.kt` | `LockedCreatureCard` muestra hint + progreso específico por especie |
| `EcosystemViewModel.kt` | Exponer `DecorationProgress` desde `DecorationUnlockChecker` |
| `AquariumCreature.kt` | Actualizar comentario de DECORATION (ya no es automático) |

### Tests

- `checkTreasureChest()`: 7 días perfectos consecutivos sin hueco → desbloquea; 6 días perfectos → no desbloquea; 7 días perfectos con un hueco → no desbloquea; especie ya existente → idempotente.
- `checkAnchor()`: ≥100 → desbloquea; 99 → no; idempotente.
- `checkSunkenShip()`: wallpaper activado → desbloquea; no activado → no; idempotente.
- `checkAll()`: desencadena las 3 comprobaciones.
- `LootboxResolver`: con `directSpecies` → retorna esa especie; sin él → weighted-random normal.
- `DecorationUnlockChecker.getProgress()`: retorna conteos correctos.

---

## Sin Room migration

- `PendingLootbox` vive en DataStore (JSON). El nuevo campo tiene default null → retrocompatible.
- `countCompletedAllTime()` es una nueva query, sin cambio de esquema.
- `wallpaper_activated` es una nueva clave DataStore, sin afectar Room.

---

## Orden de implementación recomendado

1. Spillover 80/20 en `EcosystemProcessor` + tests unitarios.
2. `PendingLootbox.directSpecies` + `LootboxResolver` + test.
3. `DecorationUnlockChecker` + `DecorationProgress` + repos + tests.
4. Puntos de disparo (NightSummaryProcessor, WallpaperService, MainViewModel).
5. UI: hints específicos en `LockedCreatureCard`.
