# Arquitectura — Riptide

## Patrón general

MVVM con repositorios. La UI no conoce Room, solo los ViewModels. Los repositorios abstraen el origen de datos para facilitar la migración a backend en v3 sin tocar ViewModels ni UI.

```
UI (Compose)
    ↕
ViewModel (commonMain)
    ↕
Repository interface (commonMain)
    ↕
Repository impl (androidMain) → Room DAOs
```

---

## commonMain

Todo lo que es independiente de plataforma vive aquí:

- **Modelos de dominio** — `WorkBlock`, `DayTask`, `DaySummary`, `BlockStreak`, `EcosystemState`, `MarineCreature`, `BlockCategory`
- **Interfaces de repositorio** — `WorkBlockRepository`, `DayTaskRepository`, etc.
- **ViewModels** — `MainViewModel`, `BlockFormViewModel`
- **Lógica de negocio** — `MarineCategoryAssigner`
- **UI** — `MainScreen`, `BlockFormScreen`, `WeekCalendar`, `MainDrawer`

### expect/actual

```kotlin
// commonMain
expect fun generateUUID(): String

// androidMain
actual fun generateUUID(): String = UUID.randomUUID().toString()

// iosMain
actual fun generateUUID(): String = NSUUID().UUIDString()
```

Lo mismo para `parseColor` (hex → Color) y `currentDate` (fecha actual del sistema).

---

## androidMain

### Room (v5)

- `RiptideDatabase` — `fallbackToDestructiveMigration(true)` durante desarrollo
- `DatabaseProvider` — singleton con `lazy`
- IDs como `String` (UUID) para facilitar sincronización futura

Tablas principales:
- `work_blocks` — bloques de trabajo
- `block_categories` — relación bloque↔categoría marina (PK compuesta, FK CASCADE)
- `day_tasks` — tareas del día
- `day_summaries` — resúmenes diarios
- `block_streaks` — rachas por bloque
- `ecosystem_states` — estado del ecosistema por categoría
- `marine_creatures` — criaturas desbloqueadas

### ViewModelFactory

Android requiere factories para inyectar dependencias en ViewModels. Cada ViewModel tiene su factory en androidMain que obtiene los repositorios desde `DatabaseProvider` vía `LocalContext`.

---

## MarineCategoryAssigner

Redistribuye categorías marinas automáticamente al crear, editar o eliminar bloques. El usuario nunca las ve ni las configura.

| Nº bloques | Distribución |
|---|---|
| 1 | 5 categorías (todas) |
| 2 | 3 + 3 (1 se repite) |
| 3 | 2 + 2 + 2 (1 se repite) |
| 4 | 2 + 1 + 1 + 1 (1 se repite) |
| 5+ | 1 por bloque sin repetición |

La experiencia acumulada en `EcosystemState` se mantiene intacta al redistribuir.

---

## Navegación

`Navigation Compose` con un `NavHost` en `App.kt`:

```
ROUTE_MAIN        → MainScreen
ROUTE_BLOCK_CREATE → BlockFormScreen (nuevo)
ROUTE_BLOCK_EDIT   → BlockFormScreen (editar, recibe blockId)
```

Al guardar o eliminar un bloque, `BlockFormViewModel` emite `BlockFormResult.Saved` o `BlockFormResult.Deleted`, la navegación llama `mainViewModel.reload()` y hace `popBackStack()`.

---

## Gestos en MainScreen

Un único `detectDragGestures` en el `Box` exterior gestiona todo:

- **Vertical hacia abajo** → abre drawer
- **Vertical hacia arriba** → cierra drawer
- **Horizontal** (solo si drawer cerrado) → cambia día
