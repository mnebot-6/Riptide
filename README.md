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

**Por la noche** recibes un resumen emocional:
- 0% completado → *"Las corrientes cambian. Mañana el mar sigue ahí."*
- ~50% → *"Buen empuje hoy."*
- ~80% → *"El estanque está vivo."*
- 100% → *"Hoy el estanque brilló."*

---

## El ecosistema marino

Cada bloque tiene asignada automáticamente una **categoría marina**. La constancia en cada bloque hace crecer esa parte del ecosistema de forma independiente.

| Categoría | Ejemplo de bloque |
|---|---|
| 🐟 Peces | Trabajo principal |
| 🪸 Flora | Formación / aprendizaje |
| 🦞 Crustáceos | Deporte / ejercicio |
| 🐚 Moluscos | Vida personal / hogar |
| 🦈 Pelágicos | Proyectos creativos |

Las categorías se **asignan automáticamente** y se redistribuyen al crear o eliminar bloques. El usuario nunca las configura.

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Multiplataforma | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform |
| Persistencia | Room 2.8.4 (offline-first, v6) |
| Serialización | kotlinx-serialization-json 1.7.3 |
| Arquitectura | MVVM |
| Navegación | Navigation Compose (multiplatform) |
| Backend (v3) | Ktor + PostgreSQL |
| Auth (v3) | Google Sign-In |

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
```

---

## Estructura del proyecto

```
composeApp/src/
├── commonMain/kotlin/com/mnebot/riptide/
│   ├── UuidGenerator.kt              (expect fun generateUUID())
│   ├── Serializers.kt                (LocalTimeSerializer)
│   ├── domain/
│   │   ├── MarineCategoryAssigner.kt
│   │   ├── RecurringTaskGenerator.kt
│   │   ├── model/                    (WorkBlock, DayTask, DaySummary, BlockStreak,
│   │   │                              EcosystemState, MarineCreature, BlockCategory,
│   │   │                              MarineCategory, CreatureSpecies, Recurrence,
│   │   │                              WeeklySlot, TaskStatus, TaskSchedule, RecurringTaskDef)
│   │   └── repository/               (interfaces)
│   └── presentation/
│       ├── block/  BlockFormScreen, BlockFormViewModel
│       ├── main/   MainScreen, MainViewModel, MainUiState, WeekCalendar,
│       │           MainDrawer, CurrentDate(expect), ParseColor(expect)
│       └── task/   TaskFormSheet, PostponeSheet, TaskFormViewModel
│
├── androidMain/kotlin/com/mnebot/riptide/
│   ├── App.kt, MainActivity.kt, DataSeeder.kt
│   ├── UuidGenerator.android.kt      (actual)
│   ├── data/local/
│   │   ├── entity/                   (todas las entities Room)
│   │   ├── dao/                      (todos los DAOs)
│   │   ├── db/                       (RiptideDatabase v6, DatabaseProvider)
│   │   └── mapper/                   (domain ↔ entity)
│   ├── data/repository/              (implementaciones Room)
│   └── presentation/
│       ├── block/  BlockFormViewModelFactory
│       ├── main/   MainViewModelFactory, CurrentDate.android, ParseColor.android
│       ├── task/   TaskFormViewModelFactory
│       └── Navigation.kt
│
└── iosMain/kotlin/com/mnebot/riptide/
    ├── MainViewController.kt
    ├── UuidGenerator.ios.kt          (actual)
    └── presentation/main/  CurrentDate.ios, ParseColor.ios
```

Ver [`docs/architecture.md`](docs/architecture.md) para detalle completo.

---

## Roadmap

| Fase | Estado |
|------|--------|
| MVP — Infraestructura y CRUD | ✅ Completado |
| MVP — Tareas puntuales y recurrentes | ✅ Completado |
| MVP — Editar / posponer / eliminar tareas | ✅ Completado |
| MVP — Indicadores en calendario | ⬜ Pendiente |
| MVP — Resumen nocturno | ⬜ Pendiente |
| v2 — Rachas y mensajes contextuales | ⬜ Pendiente |
| v2 — Ecosistema visual (Canvas/Lottie) | ⬜ Pendiente |
| v3 — Backend + sincronización | ⬜ Pendiente |

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
