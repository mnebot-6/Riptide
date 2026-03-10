# Estructura del proyecto Riptide

## Visión general

El proyecto usa **Kotlin Multiplatform con Compose Multiplatform**. Todo el código vive dentro de `composeApp/src/`, dividido en tres source sets principales:

- `commonMain` — código compartido entre Android e iOS (lógica de negocio, modelos, contratos, UI, ViewModels)
- `androidMain` — código exclusivo de Android (Room, implementaciones de repositorio, factory del ViewModel)
- `iosMain` — código exclusivo de iOS (punto de entrada UIViewController)

---

## commonMain

Ruta base: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/`

### `App.kt`
Punto de entrada de la UI compartida. Recibe el `MainViewModel` como parámetro y llama a `MainScreen`.

### `domain/model/`
Modelos de datos puros en Kotlin. Sin dependencias de Android ni de Room.

| Archivo | Qué representa |
|---|---|
| `MarineCategory.kt` | Enum con las 5 categorías marinas (FISH, FLORA, CRUSTACEAN, MOLLUSK, PELAGIC) |
| `WorkBlock.kt` | Bloque de tiempo recurrente. Contiene también `Recurrence` (sealed class) y `WeeklySlot` |
| `DayTask.kt` | Tarea concreta asignada a un bloque en un día específico |
| `DaySummary.kt` | Resumen del día: tareas completadas, score interno y mensaje de feedback |
| `BlockStreak.kt` | Racha de días consecutivos activos de un bloque concreto |
| `EcosystemState.kt` | Estado del ecosistema marino por categoría: experiencia total y nivel actual |
| `MarineCreature.kt` | Criatura individual del estanque con especie, mote, experiencia y nivel propio. Contiene también `CreatureSpecies` |

### `domain/repository/`
Interfaces que definen el contrato de acceso a datos. Sin implementación.

| Archivo | Operaciones |
|---|---|
| `WorkBlockRepository.kt` | getAll, getById, insert, update, delete |
| `DayTaskRepository.kt` | getByDate, getByBlock, insert, update, delete |
| `DaySummaryRepository.kt` | getByDate, insert |
| `EcosystemStateRepository.kt` | getByCategory, update |
| `MarineCreatureRepository.kt` | getByEcosystem, insert, update |

### `usecase/`
Reservada para use cases. Vacía por ahora.

### `presentation/main/`

| Archivo | Qué hace |
|---|---|
| `CurrentDate.kt` | Declaración `expect` de la función que devuelve la fecha actual |
| `ParseColor.kt` | Declaración `expect` de la función que convierte hex String a Color de Compose |
| `MainUiState.kt` | Estado de la UI: fecha seleccionada, bloques, tareas por bloque, loading, error |
| `MainViewModel.kt` | Carga datos de Room, expone `StateFlow<MainUiState>`, maneja eventos del usuario |
| `MainScreen.kt` | Composable raíz. Fondo oceánico, tarjetas frosted glass, FAB para ver el estanque |

---

## androidMain

Ruta base: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/`

### `MainActivity.kt`
Punto de entrada de la app. Inicializa Room, llama al DataSeeder y arranca la UI con el ViewModel.

### `DataSeeder.kt`
Rellena la base de datos con datos de prueba si está vacía. Crea 3 bloques (💼 Trabajo, 🏐 Voleibol, 💚 Salud) y 5 tareas para el día actual. Solo se ejecuta una vez.

### `data/local/entity/`

| Archivo | Tabla SQLite |
|---|---|
| `WorkBlockEntity.kt` | `work_blocks` |
| `DayTaskEntity.kt` | `day_tasks` |
| `DaySummaryEntity.kt` | `day_summaries` |
| `BlockStreakEntity.kt` | `block_streaks` |
| `EcosystemStateEntity.kt` | `ecosystem_states` |
| `MarineCreatureEntity.kt` | `marine_creatures` |

Fechas como String ISO. Enums como String. Recurrence.Weekly como JSON plano.

### `data/local/dao/`

| Archivo | DAO para |
|---|---|
| `WorkBlockDao.kt` | `work_blocks` |
| `DayTaskDao.kt` | `day_tasks` |
| `DaySummaryDao.kt` | `day_summaries` |
| `BlockStreakDao.kt` | `block_streaks` |
| `EcosystemStateDao.kt` | `ecosystem_states` |
| `MarineCreatureDao.kt` | `marine_creatures` |

### `data/local/db/`

| Archivo | Para qué sirve |
|---|---|
| `RiptideDatabase.kt` | Clase principal de Room. Versión 3. Agrupa entities y DAOs. |
| `DatabaseProvider.kt` | Singleton que construye RiptideDatabase. Usa fallbackToDestructiveMigration en desarrollo. |

### `data/local/mapper/`

| Archivo | Convierte |
|---|---|
| `WorkBlockMapper.kt` | `WorkBlock` ↔ `WorkBlockEntity` |
| `DayTaskMapper.kt` | `DayTask` ↔ `DayTaskEntity` |
| `DaySummaryMapper.kt` | `DaySummary` ↔ `DaySummaryEntity` |
| `BlockStreakMapper.kt` | `BlockStreak` ↔ `BlockStreakEntity` |
| `EcosystemStateMapper.kt` | `EcosystemState` ↔ `EcosystemStateEntity` |
| `MarineCreatureMapper.kt` | `MarineCreature` ↔ `MarineCreatureEntity` |

### `data/repository/`

| Archivo | Implementa |
|---|---|
| `WorkBlockRepositoryImpl.kt` | `WorkBlockRepository` |
| `DayTaskRepositoryImpl.kt` | `DayTaskRepository` |
| `DaySummaryRepositoryImpl.kt` | `DaySummaryRepository` |
| `EcosystemStateRepositoryImpl.kt` | `EcosystemStateRepository` |
| `MarineCreatureRepositoryImpl.kt` | `MarineCreatureRepository` |

### `presentation/main/`

| Archivo | Para qué sirve |
|---|---|
| `CurrentDate.android.kt` | Implementación `actual` de `currentDate()` para Android |
| `ParseColor.android.kt` | Implementación `actual` de `parseColor()` usando android.graphics.Color |
| `MainViewModelFactory.kt` | Construye el `MainViewModel` con las dependencias Room |

---

## iosMain

Ruta base: `composeApp/src/iosMain/kotlin/com/mnebot/riptide/`

| Archivo | Para qué sirve |
|---|---|
| `MainViewController.kt` | Punto de entrada iOS. Recibe repositorios y construye el ViewModel |
| `presentation/main/CurrentDate.ios.kt` | Implementación `actual` de `currentDate()` para iOS |
| `presentation/main/ParseColor.ios.kt` | Implementación `actual` de `parseColor()` parseando hex manualmente |

---

## Diagrama de capas

```
commonMain                          androidMain
─────────────────────────────────────────────────────
App.kt + MainScreen.kt

presentation/main/                  presentation/main/
  MainViewModel                       MainViewModelFactory
  MainUiState                         CurrentDate.android.kt
  CurrentDate (expect)                ParseColor.android.kt
  ParseColor (expect)

domain/model/                       data/local/entity/
  WorkBlock, DayTask...    ←──→       WorkBlockEntity...
  (Kotlin puro)            mapper     (Room / SQLite)

domain/repository/                  data/local/dao/
  WorkBlockRepository      ←──       WorkBlockDao...
  (interfaz)               impl      (Room queries)

usecase/                            data/local/db/
  (pendiente)                         RiptideDatabase
                                      DatabaseProvider

                                    data/repository/
                                      WorkBlockRepositoryImpl...

                                    DataSeeder.kt
```

---

## Docs

| Archivo | Contenido |
|---|---|
| `riptide_context.txt` | Contexto completo del proyecto para usar con IA |
| `project_structure.md` | Este archivo |