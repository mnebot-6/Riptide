# Riptide

App de productividad personal con sistema de recompensa emocional basado en un **ecosistema marino que crece con tu constancia**.

> La constancia construye algo bonito. El abandono lo pausa. Nunca lo destruye.

---

## Concepto

Riptide resuelve dos problemas reales: planificar la jornada de forma comoda y mantener la constancia a largo plazo. La puntuacion existe internamente pero **nunca se muestra como numero**. El feedback es siempre emocional: mensajes, crecimiento visual del estanque, sensacion de progreso sin metricas explicitas.

Inspirado en **Forest**, pero mas profundo y personal -- orientado a la sensacion, no a los numeros.

---

## Como funciona

**Cada manana** asignas tareas concretas a los bloques del dia. Cada tarea pertenece a un bloque (Trabajo, Salud, Ocio...).

**Durante el dia** simplemente marcas tareas como completadas. Sin valoraciones, sin numeros visibles. Si se te pasa la hora, la tarea queda marcada como fuera de plazo pero sigue siendo completable.

**Opcionalmente** puedes activar un aviso matutino para planificar el dia, y notificaciones push a la hora de cada tarea que quieras recordar.

**Por la noche** recibes un resumen emocional a la hora que configures:
- 0% -- *"Las corrientes cambian. Manana el mar sigue ahi."*
- ~50% -- *"Buen empuje hoy."*
- ~80% -- *"El estanque esta vivo."*
- 100% -- *"Hoy el estanque brillo."*

---

## El ecosistema marino

Cada bloque tiene asignada automaticamente una **categoria marina**. La constancia hace crecer esa parte del ecosistema de forma independiente y silenciosa.

| Categoria | Estado | Especies |
|---|---|---|
| Peces | Base | 9 |
| Flora | Base | 9 |
| Crustaceos | Base | 9 |
| Moluscos | Base | 9 |
| Pelagicos | Base | 9 |
| Cefalopodos | Bloqueada | 6 |
| Reptiles | Bloqueada | 6 |
| Mamiferos | Bloqueada | 6 |
| Decoracion | Especial | 6 |
| Companion | Easter egg | 1 |

**70 especies** repartidas en 5 rarezas (Comun, Poco comun, Raro, Epico, Legendario). Al alcanzar ciertos niveles, se obtiene una **lootbox** de la categoria correspondiente. Al abrirla, se revela una especie aleatoria ponderada por rareza. Tu le pones el nombre. Con el tiempo crecen -- cambian de tamano y velocidad.

Cada criatura tiene su propia personalidad de nado: los peces payaso zigzaguean nerviosos, los delfines saltan en arcos graciles, los cangrejos exploran el fondo a ritmo variable, las medusas derivan con la corriente. No hay dos criaturas exactamente iguales. **70 renderers Canvas** con cobertura total -- ninguna especie usa emoji fallback.

---

## Funcionalidades

- **Ecosistema marino completo**: 70 especies, 10 categorias, sistema lootbox con rareza, crecimiento individual, evolucion visual por nivel
- **Estadisticas e historial**: graficos de barras Canvas, toggle Semana/Mes/Todo, selector 30/60/90 dias, rachas por bloque
- **Notificaciones push**: resumen nocturno, aviso matutino configurable, recordatorio por tarea
- **Widget Android**: tareas del dia con barra de progreso, paleta marina, auto-update 30min
- **Live Wallpaper**: acuario animado como fondo de pantalla, 30fps vsync-aligned, criaturas desbloqueadas nadando
- **Onboarding**: flujo de 4 pasos con animaciones
- **Internacionalizacion**: EN + ES, deteccion automatica del idioma
- **Iconografia Lucide**: vector drawables stroke-based, sin emojis de control
- **Logo & Branding "Rising Currents"**: adaptive icon, splash screen, icono de notificacion
- **Backend + Sync**: Ktor 3.0.3 + PostgreSQL, auth Google Sign-In + JWT, sync offline-first bidireccional
- **Tests automatizados**: 37 tests unitarios en commonTest (EcosystemLevelCalculator, BlockStreakProcessor, NightSummaryProcessor)

---

## Stack tecnico

| Capa | Tecnologia |
|---|---|
| Multiplataforma | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform 1.10.2 |
| Persistencia | Room 2.8.4 (offline-first, v12 con migraciones reales) |
| Serializacion | kotlinx-serialization-json 1.7.3 |
| Arquitectura | MVVM |
| Navegacion | Navigation Compose (multiplatform) |
| Preferencias | DataStore Preferences 1.1.7 |
| Background | WorkManager 2.10.1 |
| Widget | Glance 1.1.1 |
| Backend | Ktor 3.0.3 + Exposed 0.57.0 + PostgreSQL |
| Auth | Google Sign-In + JWT (access 24h + refresh 30d) |
| Sync | Ktor Client (OkHttp) + batch endpoint + conflict resolution |
| Kotlin | 2.3.10 |
| Gradle | 8.14.3 |

---

## Roadmap

| Fase | Estado |
|------|--------|
| MVP | Done |
| v2 -- Rachas, ecosistema visual, desbloqueos | Done |
| Sprints visuales, lootbox, i18n, renderers | Done |
| Sprint onboarding + notificaciones push | Done |
| Sprint estadisticas + historial | Done |
| Sprint rachas, evolucion visual, tests | Done |
| Sprint Lucide Icons + Logo & Branding | Done |
| Sprint Widget Android + Live Wallpaper | Done |
| Sprint Backend + Sync offline-first | Done |
| Expansion ecosistema (40 -> 70 especies) | Done |
| Fase 2 -- QA & Polish | Pendiente |
| Fase 3 -- Play Store | Pendiente |
| Fase 4 -- iOS | Pendiente |

---

## Filosofia de diseno

- La puntuacion **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presion numerica
- El feedback es siempre emocional y contextual

---

## Documentacion

| Archivo | Que cubre |
|---|---|
| `docs/riptide_context.txt` | Referencia completa del proyecto: todos los sistemas, modelos, logica de negocio |
| `docs/architecture.md` | MVVM, Room schema, procesadores, animaciones, sync, widget, wallpaper |
| `docs/data-models.md` | Todos los modelos de dominio: WorkBlock, DayTask, MarineCreature, 70 especies |
| `docs/project_structure.md` | Arbol de directorios completo con descripcion de cada archivo |
| `docs/roadmap.md` | Fases completadas y futuras |
| `docs/setup-guide.md` | Setup backend + sync + Google Cloud |
| `docs/privacy-policy.md` | Politica de privacidad |

---

## Repositorio

Desarrollado por [@mnebot-6](https://github.com/mnebot-6)
