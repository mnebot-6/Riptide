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
| Persistencia | Room (offline-first, v5) |
| Arquitectura | MVVM |
| Navegación | Navigation Compose 2.8.0-alpha10 |
| Backend (v3) | Ktor + PostgreSQL |
| Auth (v3) | Google Sign-In |

---

## Estructura del proyecto

```
composeApp/src/
├── commonMain/kotlin/com/mnebot/riptide/
│   ├── UuidGenerator.kt                  (expect fun generateUUID())
│   ├── domain/
│   │   ├── MarineCategoryAssigner.kt
│   │   ├── model/                        (WorkBlock, DayTask, DaySummary,
│   │   │                                  BlockStreak, EcosystemState,
│   │   │                                  MarineCreature, BlockCategory,
│   │   │                                  Recurrence, WeeklySlot, ...)
│   │   └── repository/                   (interfaces)
│   └── presentation/
│       ├── block/   (BlockFormScreen, BlockFormViewModel)
│       └── main/    (MainScreen, MainViewModel, WeekCalendar,
│                     MainDrawer, MainUiState, CurrentDate, ParseColor)
├── androidMain/kotlin/com/mnebot/riptide/
│   ├── App.kt
│   ├── MainActivity.kt
│   ├── DataSeeder.kt
│   ├── UuidGenerator.android.kt
│   ├── data/local/
│   │   ├── entity/   (WorkBlockEntity, DayTaskEntity, DaySummaryEntity,
│   │   │              BlockStreakEntity, EcosystemStateEntity,
│   │   │              MarineCreatureEntity, BlockCategoryEntity)
│   │   ├── dao/
│   │   ├── db/       (RiptideDatabase v5, DatabaseProvider)
│   │   └── mapper/
│   ├── data/repository/
│   └── presentation/
│       ├── block/    (BlockFormViewModelFactory)
│       ├── main/     (MainViewModelFactory, CurrentDate.android,
│       │              ParseColor.android)
│       └── Navigation.kt
└── iosMain/kotlin/com/mnebot/riptide/
    ├── MainViewController.kt
    ├── UuidGenerator.ios.kt
    └── presentation/main/
        ├── CurrentDate.ios.kt
        └── ParseColor.ios.kt
```

---

## Versiones clave (libs.versions.toml)

```toml
kotlin                    = "2.3.10"
agp                       = "8.12.3"
composeMultiplatform      = "1.10.2"
androidx-room             = "2.8.4"
ksp                       = "2.3.0"
kotlinxDatetime           = "0.7.1"
androidx-lifecycle        = "2.9.6"
androidx-navigation-compose = "2.8.0-alpha10"
```

---

## Roadmap

**MVP** — 100% local con Room
- [x] Calendario semanal navegable con swipe
- [x] Gestión de bloques (crear / editar / eliminar)
- [x] Tareas por bloque con checkbox persistente
- [x] Drawer desde arriba con gestos unificados
- [x] MarineCategoryAssigner automático
- [x] Navigation Compose configurado
- [ ] Añadir tareas desde el drawer (bottom sheet)
- [ ] Indicadores de tareas por día en el calendario
- [ ] Resumen nocturno

**v2**
- Rachas por bloque con mensajes contextuales
- Ecosistema visual (Canvas / Lottie)
- Animaciones del estanque

**v3**
- Backend Ktor + PostgreSQL
- Perfiles de usuario
- Visitar el estanque de un amigo (solo ver, nunca competir)
- Google Sign-In

---

## Repositorio

Desarrollado por [@mnebot-6](https://github.com/mnebot-6)
