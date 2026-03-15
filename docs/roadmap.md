# Roadmap — Riptide

## ✅ MVP completado

- Infraestructura KMP + Room + DataStore + WorkManager
- CRUD completo: bloques, tareas puntuales y recurrentes
- Editar / posponer / eliminar con diálogos de scope para recurrentes
- Resumen nocturno con WorkManager
- Indicadores de progreso

---

## ✅ v2 completada

- Rachas por bloque (`BlockStreak`) con badge 🔥
- Mensajes emocionales contextuales
- Ecosistema visual con Canvas (AquariumBackground + AquariumCreatures)
- Desbloqueo de criaturas con nombre obligatorio
- XP por tarea y bonus nocturno

---

## ✅ v2.1 completada

- Curva XP reajustada (nivel 2 = 1 XP)
- Fix edición recurrentes con diálogo de scope
- TimeInputField + DateInputField estandarizados
- Pickers con estética marina
- DataSeeder limpio

---

## ✅ Sprint de fixes y mejoras

- Pantalla bloqueada en portrait
- Resumen nocturno no reaparece (fecha descartada en DataStore)
- Scroll mantiene posición al completar tarea
- Posponer con hora opcional
- POSTPONED excluido de resumen y barra de progreso
- Resumen nocturno evalúa solo tareas vencidas
- Nuevo Header fijo con acciones integradas
- Drawer compacto con scroll interno (85% pantalla)
- Tareas recurrentes con hora opcional
- Editar recurrente con diálogo scope + `forceRecurring`
- RiptideDatabase v7

---

## ✅ Sprint v2 ampliado

- 9 categorías marinas: FISH, FLORA, CRUSTACEAN, MOLLUSK, PELAGIC (base) + CEPHALOPOD, REPTILE, MAMMAL, DECORATION (bloqueadas)
- 24 especies con emojis, patrones de nado y `speedScalePerLevel`
- `EcosystemState.isUnlocked` — categorías desbloqueables dinámicamente
- `MarineCategoryAssigner` redistribuye entre categorías desbloqueadas
- `EcosystemProcessor` reparte XP a criaturas individuales (`experience` + `creatureLevel`)
- Efectos visuales por nivel: tamaño (+10% por nivel desde 80%) y velocidad de nado
- `creatureLevelBySpecies` en `MainUiState` y `loadDay`
- `MarineCreatureRepository.getByCategory` + `EcosystemStateRepository.getUnlocked/getAll`
- Ordenación de bloques por tarea pendiente con hora más temprana
- Long press en cabecera del bloque → crear tarea con bloque preseleccionado (`initialBlockId`)
- DataSeeder con 5 bloques y EcosystemStates pre-creados antes del reassign
- RiptideDatabase v8

---

## 🔄 Próximos pasos (antes de v3)

- **Testing** — automatizado para dominio (EcosystemProcessor, NightSummaryProcessor, EcosystemLevelCalculator) + manual de flujos principales
- **Patrón de nado más orgánico** — movimiento menos lineal, más variado entre especies
- **Pulsar criatura → ver info** — nombre, nivel, especie (solo con tareas ocultas por FAB)
- **Resumen de criaturas desde el Drawer** — nueva pantalla NavHost con todas las especies (desbloqueadas con info, bloqueadas como silueta)

---

## v3 — Revisión, pulido y onboarding

- Animación de entrada de criatura al desbloquearse (nada desde el borde)
- Revisión de gestos, formularios y edge cases del resumen nocturno
- Migrar `fallbackToDestructiveMigration` a migraciones reales de Room
- Preparar firma de la app para distribución
- Tutorial de primera vez (onboarding al primer inicio)
  - `hasCompletedOnboarding` en `UserPreferencesRepository` (DataStore)
  - Pantalla fullscreen con fondo marino, pasos swipeables
  - Explica bloques, tareas, ecosistema, resumen nocturno
  - Termina creando el primer bloque o salta al DataSeeder

---

## v4 — Fondo de pantalla dinámico

- Live wallpaper del acuario (WallpaperService Android)

---

## v5 — Backend y social

- Backend Ktor + PostgreSQL
- Sincronización offline-first (IDs UUID ya preparados)
- Perfiles de usuario
- Google Sign-In
- Visitar el estanque de un amigo (solo ver, nunca competir)

---

## v6 — iOS completo

- Implementación real de `AquariumCreature.ios.kt`
- Pickers nativos iOS (`TimePickerDialogWrapper`, `DatePickerDialogWrapper`)
- `NightSummaryScheduler` con notificaciones locales iOS

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual
