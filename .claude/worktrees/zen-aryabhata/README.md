# 🌊 Riptide

App de productividad personal con sistema de recompensa emocional basado en un **ecosistema marino que crece con tu constancia**.

> La constancia construye algo bonito. El abandono lo pausa. Nunca lo destruye.

---

## Concepto

Riptide resuelve dos problemas reales: planificar la jornada de forma cómoda y mantener la constancia a largo plazo. La puntuación existe internamente pero **nunca se muestra como número**. El feedback es siempre emocional: mensajes, crecimiento visual del estanque, sensación de progreso sin métricas explícitas.

Inspirado en **Forest**, pero más profundo y personal — orientado a la sensación, no a los números.

---

## Cómo funciona

**Cada mañana** asignas tareas concretas a los bloques del día. Cada tarea pertenece a un bloque (Trabajo, Salud, Ocio...).

**Durante el día** simplemente marcas tareas como completadas. Sin valoraciones, sin números visibles. Si se te pasa la hora, la tarea queda marcada como fuera de plazo pero sigue siendo completable.

**Opcionalmente** puedes activar un aviso matutino para planificar el día, y notificaciones push a la hora de cada tarea que quieras recordar.

**Por la noche** recibes un resumen emocional a la hora que configures:
- 0% → *"Las corrientes cambian. Mañana el mar sigue ahí."*
- ~50% → *"Buen empuje hoy."*
- ~80% → *"El estanque está vivo."*
- 100% → *"Hoy el estanque brilló."*

---

## El ecosistema marino

Cada bloque tiene asignada automáticamente una **categoría marina**. La constancia hace crecer esa parte del ecosistema de forma independiente y silenciosa.

| Categoría | Emoji | Estado |
|---|---|---|
| Peces | 🐟 | Base |
| Flora | 🪸 | Base |
| Crustáceos | 🦞 | Base |
| Moluscos | 🐚 | Base |
| Pelágicos | 🦈 | Base |
| Cefalópodos | 🦑 | Bloqueada |
| Reptiles | 🐢 | Bloqueada |
| Mamíferos | 🐬 | Bloqueada |
| Decoración | 🪙 | Especial |

Al alcanzar ciertos niveles, se obtiene una **lootbox** de la categoría correspondiente. Al abrirla, se revela una especie aleatoria ponderada por rareza (Común → Legendario). Tú le pones el nombre. Con el tiempo crecen — cambian de tamaño y velocidad. 40 especies repartidas en 5 rarezas.

Cada criatura tiene su propia personalidad de nado: los peces payaso zigzaguean nerviosos, los delfines saltan en arcos gráciles, los cangrejos exploran el fondo a ritmo variable, las medusas derivan con la corriente. No hay dos criaturas exactamente iguales.

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Multiplataforma | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform |
| Persistencia | Room 2.8.4 (offline-first, v10 con migraciones reales) |
| Serialización | kotlinx-serialization-json 1.7.3 |
| Arquitectura | MVVM |
| Navegación | Navigation Compose (multiplatform) |
| Preferencias | DataStore Preferences 1.1.7 |
| Background | WorkManager 2.10.1 |
| Backend (v5) | Ktor + PostgreSQL |

---

## Roadmap

| Fase | Estado |
|------|--------|
| MVP | ✅ |
| v2 — Rachas, ecosistema visual, desbloqueos | ✅ |
| v2.1 — Inputs, fixes, curva XP | ✅ |
| Sprint fixes — Header, drawer, resumen | ✅ |
| Sprint v2 ampliado — 9 categorías, 24 especies, XP individual | ✅ |
| Sprint pre-v3 — Detalle criatura, ecosistema, nado orgánico | ✅ |
| Sprint bugfixes — Resumen nocturno, XP, nado, inputs | ✅ |
| Sprint bugfixes 2 — Emoji centrado, fecha resumen, summaryTime | ✅ |
| Sprint visual — Superficie, fondo marino, flora Canvas | ✅ |
| Sprint visual 2 — Cielo dinámico, CreatureIcon, UI pulida | ✅ |
| Sprint lootbox — Rareza, 40 especies, lootbox, EcosystemScreen overhaul | ✅ |
| Sprint i18n + renderers — Traducciones EN/ES, 13 nuevos renderers Canvas | ✅ |
| Sprint renderers total — 23 renderers adicionales, cobertura 100% (40/40 especies) | ✅ |
| Sprint onboarding — Flujo 4 pasos, DataStore, AnimatedContent, i18n EN/ES | ✅ |
| Sprint notificaciones push — Resumen nocturno, aviso matutino, recordatorio por tarea | ✅ |
| Sprint estadísticas + historial — StatsScreen, HistoryScreen, rango configurable, meses localizados | ✅ |
| v3 — Racha global, evolución criaturas, recompensas de racha, testing | ⬜ |
| v4 — Widget Android, fondo de pantalla dinámico | ⬜ |
| v5 — Backend + social + backup | ⬜ |
| v6 — iOS completo | ⬜ |

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual

---

## Repositorio

Desarrollado por [@mnebot-6](https://github.com/mnebot-6)
