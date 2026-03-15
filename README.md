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

**Durante el día** simplemente marcas tareas como completadas. Sin valoraciones, sin números visibles.

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

Al alcanzar ciertos niveles, nuevos habitantes aparecen en el estanque. Tú les pones el nombre. Con el tiempo crecen — cambian de tamaño y velocidad.

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Multiplataforma | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform |
| Persistencia | Room 2.8.4 (offline-first, v8) |
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
| Testing + pulido pre-v3 | 🔄 |
| v3 — Onboarding, animaciones, migraciones Room | ⬜ |
| v4 — Fondo de pantalla dinámico | ⬜ |
| v5 — Backend + social | ⬜ |
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
