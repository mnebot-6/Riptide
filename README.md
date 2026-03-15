# 🌊 Riptide

App de productividad personal con sistema de recompensa emocional basado en un **ecosistema marino que crece con tu constancia**.

> La constancia construye algo bonito. El abandono lo pausa. Nunca lo destruye.

---

## Concepto

Riptide resuelve dos problemas reales: planificar la jornada de forma cómoda y mantener la constancia a largo plazo. La puntuación existe internamente pero **nunca se muestra como número**. El feedback es siempre emocional: mensajes, crecimiento visual del estanque, sensación de progreso sin métricas explícitas.

Inspirado en **Forest**, pero más profundo y personal — orientado a la sensación, no a los números.

---

## Cómo funciona

**Cada mañana** asignas tareas concretas a los bloques del día en 2 minutos. Cada tarea pertenece a un bloque (Trabajo, Voleibol, Salud...).

**Durante el día** simplemente marcas tareas como completadas. Sin valoraciones, sin números visibles, sin presión.

**Por la noche** recibes un resumen emocional a la hora que tú configures:
- 0% completado → *"Las corrientes cambian. Mañana el mar sigue ahí."*
- ~50% → *"Buen empuje hoy."*
- ~80% → *"El estanque está vivo."*
- 100% → *"Hoy el estanque brilló."*

Si llevas varios días seguidos activo en un bloque, el mensaje lo reconoce: *"Trabajo lleva 5 días seguidos 🔥"*

---

## El ecosistema marino

Cada bloque tiene asignada automáticamente una **categoría marina**. La constancia hace crecer esa parte del ecosistema de forma independiente y silenciosa.

| Categoría | Ejemplo de bloque |
|---|---|
| 🐟 Peces | Trabajo principal |
| 🪸 Flora | Formación / aprendizaje |
| 🦞 Crustáceos | Deporte / ejercicio |
| 🐚 Moluscos | Vida personal / hogar |
| 🦈 Pelágicos | Proyectos creativos |

Las categorías se **asignan automáticamente** y se redistribuyen al crear o eliminar bloques. El usuario nunca las configura.

Al alcanzar ciertos niveles de constancia, nuevos habitantes aparecen en el estanque. Tú les pones el nombre.

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Multiplataforma | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform |
| Persistencia | Room 2.8.4 (offline-first, v7) |
| Serialización | kotlinx-serialization-json 1.7.3 |
| Arquitectura | MVVM |
| Navegación | Navigation Compose (multiplatform) |
| Preferencias | DataStore Preferences 1.1.7 |
| Background | WorkManager 2.10.1 |
| Backend (v4) | Ktor + PostgreSQL |
| Auth (v4) | Google Sign-In |

Versiones clave (`libs.versions.toml`):
```toml
kotlin                   = "2.3.10"
agp                      = "8.12.3"
composeMultiplatform     = "1.10.2"
androidx-room            = "2.8.4"
ksp                      = "2.3.0"
kotlinxDatetime          = "0.7.1"
androidx-lifecycle       = "2.9.6"
androidx-navigation      = "2.9.2"
kotlinx-serialization    = "1.7.3"
androidx-datastore       = "1.1.7"
androidx-work            = "2.10.1"
```

---

## Estructura del proyecto

```
composeApp/src/
├── commonMain/kotlin/com/mnebot/riptide/
│   ├── UuidGenerator.kt
│   ├── Serializers.kt
│   ├── NightSummaryScheduler.kt
│   ├── domain/
│   │   ├── MarineCategoryAssigner.kt
│   │   ├── RecurringTaskGenerator.kt
│   │   ├── BlockStreakProcessor.kt
│   │   ├── NightSummaryProcessor.kt
│   │   ├── EcosystemProcessor.kt
│   │   ├── EcosystemLevelCalculator.kt
│   │   ├── model/
│   │   │   WorkBlock, DayTask, RecurringTaskDef (time nullable),
│   │   │   DaySummary, BlockStreak, EcosystemState, MarineCreature,
│   │   │   BlockCategory, MarineCategory, CreatureSpecies,
│   │   │   Recurrence, WeeklySlot, TaskStatus, TaskSchedule
│   │   └── repository/  (interfaces)
│   └── presentation/
│       ├── aquarium/
│       │   AquariumBackground, AquariumCreature (+ expect drawEmoji)
│       ├── components/
│       │   TimeInputField, DateInputField    ← input estandarizado
│       │   InputFieldDialogs (expect/actual) ← pickers con estética marina
│       ├── block/   BlockFormScreen, BlockFormViewModel
│       ├── main/    MainScreen, MainViewModel, MainUiState,
│       │            WeekCalendar, MainDrawer (BLOQUES+AJUSTES, sin TAREAS),
│       │            CurrentDate(expect), ParseColor(expect)
│       └── task/    TaskFormSheet (forceRecurring), PostponeSheet, TaskFormViewModel
│
├── androidMain/kotlin/com/mnebot/riptide/
│   ├── App.kt, MainActivity.kt, DataSeeder.kt
│   ├── NightSummaryWorker.kt
│   ├── NightSummarySchedulerImpl.android.kt
│   ├── UuidGenerator.android.kt
│   ├── data/local/
│   │   ├── entity/   (todas las entities Room; RecurringTaskDefEntity.time String?)
│   │   ├── dao/      (todos los DAOs)
│   │   ├── db/       RiptideDatabase v7, DatabaseProvider
│   │   └── mapper/   (domain ↔ entity; RecurringTaskDefMapper con time nullable)
│   ├── data/repository/
│   │   (todas las implementaciones Room + UserPreferencesRepositoryImpl
│   │    con dismissed_summary_date)
│   └── presentation/
│       ├── aquarium/    AquariumCreature.android.kt (actual drawEmoji)
│       ├── components/  InputFieldDialogs.android.kt (actual pickers marinos)
│       ├── block/       BlockFormViewModelFactory
│       ├── main/        MainViewModelFactory, CurrentDate.android, ParseColor.android
│       ├── task/        TaskFormViewModelFactory
│       └── Navigation.kt
│
└── iosMain/kotlin/com/mnebot/riptide/
    ├── MainViewController.kt
    ├── NightSummarySchedulerImpl.ios.kt
    ├── UuidGenerator.ios.kt
    ├── presentation/aquarium/    AquariumCreature.ios.kt (pendiente arreglar)
    ├── presentation/components/  InputFieldDialogs.ios.kt (stubs pendiente v3)
    └── presentation/main/        CurrentDate.ios, ParseColor.ios
```

---

## Roadmap

| Fase | Estado |
|------|--------|
| MVP — Infraestructura y CRUD | ✅ Completado |
| MVP — Tareas puntuales y recurrentes | ✅ Completado |
| MVP — Editar / posponer / eliminar | ✅ Completado |
| MVP — Indicadores de progreso | ✅ Completado |
| MVP — Resumen nocturno con WorkManager | ✅ Completado |
| v2 — Rachas y mensajes contextuales | ✅ Completado |
| v2 — Ecosistema visual con Canvas | ✅ Completado |
| v2 — Desbloqueo de criaturas con nombre | ✅ Completado |
| v2.1 — Inputs estandarizados, fix recurrentes, curva XP | ✅ Completado |
| Sprint fixes — Header, drawer, resumen, recurrentes, scroll | ✅ Completado |
| v3 — Revisión, pulido y onboarding | 🔄 Siguiente |
| v4 — Backend + sincronización + social | ⬜ Pendiente |

Ver [`docs/roadmap.md`](docs/roadmap.md) para detalle completo.

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual

---

## Repositorio

Desarrollado por [@mnebot-6](https://github.com/mnebot-6)
