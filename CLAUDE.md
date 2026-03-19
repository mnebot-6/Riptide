# Riptide — Claude Code Reference

## Proyecto

App de productividad personal con sistema de recompensa emocional basado en un ecosistema marino que crece con la consistencia del usuario.

**Filosofía core:**
- La puntuación nunca se muestra como número — el feedback es siempre emocional (mensajes, crecimiento visual)
- La consistencia construye algo bello. El abandono lo pausa. Nunca lo destruye.
- No hay racha rota: si fallas un día, el ecosistema espera, no retrocede

**Plataforma:** Android (v3 activa). iOS previsto para v6.

---

## Documentación en /docs/

| Archivo | Qué cubre |
|---|---|
| `riptide_context.txt` | Referencia completa del proyecto: todos los sistemas, modelos, lógica de negocio, convenciones |
| `architecture.md` | MVVM, expect/actual KMP, Room schema, EcosystemProcessor, animaciones AquariumCreatures, NightSummary |
| `data-models.md` | Todos los modelos de dominio: WorkBlock, DayTask, RecurringTaskDef, MarineCreature, CreatureSpecies (24), XP curve |
| `project_structure.md` | Árbol de directorios completo con descripción de cada archivo |
| `roadmap.md` | Fases completadas y futuras (v3: testing/onboarding, v4: wallpaper dinámico, v5: backend, v6: iOS) |

**Para contexto profundo en una tarea específica, lee primero el archivo de /docs/ correspondiente.**

---

## Stack

| Tecnología | Versión |
|---|---|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.10.2 |
| Room | 2.8.4 (schema v9) |
| kotlinx-serialization | 1.7.3 |
| androidx-datastore | 1.1.7 |
| androidx-work (WorkManager) | 2.10.1 |
| Gradle | 8.12.3 |

---

## Comandos útiles

```bash
# Build debug
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# Ejecutar tests unitarios
./gradlew testDebugUnitTest

# Verificar compilación sin generar APK
./gradlew compileDebugKotlin
```

---

## Convenciones críticas

- **IDs:** siempre `UUID.randomUUID().toString()`
- **Colores:** paleta marina definida en `Theme.kt` — no usar colores hardcodeados
- **Fechas/horas:** `LocalDate`/`LocalTime` de `kotlinx-datetime`, serializadas como strings ISO
- **Room migrations:** SIEMPRE migraciones reales (nunca `fallbackToDestructiveMigration`). Ver `MIGRATION_8_9` como referencia.
- **hasBeenRewarded:** campo en `DayTask` para evitar XP duplicado — siempre respetar esta bandera al distribuir XP
- **Recurrence:** enum serializado como string (`DAILY`, `WEEKLY`, `MONTHLY`)
- **Tiempo nulo en RecurringTaskDef:** tarea sin hora asignada — el generador la omite en el DaySummary si no tiene `summaryTime`

---

## Estado actual (marzo 2026)

**Completado:**
- MVVM + Room offline-first (v9)
- Ecosistema marino: 24 especies, 9 categorías, desbloqueo por XP, crecimiento individual
- Patrones de nado orgánicos (tempo warping, variación por instancia, márgenes simétricos)
- Resumen nocturno con filtrado correcto por `summaryTime`
- Tareas EXPIRED completables con checkbox
- TimePicker y DatePicker con paleta marina
- Inputs de fecha/hora readonly (click abre picker)

**Próximo (v3):**
- Tests unitarios e instrumentados
- Onboarding
- Animaciones de transición

---

## Archivos críticos

```
composeApp/src/
├── commonMain/kotlin/com/nmarsollier/riptide/
│   ├── domain/
│   │   ├── model/          # WorkBlock, DayTask, RecurringTaskDef, MarineCreature, etc.
│   │   ├── repository/     # Interfaces de repositorios
│   │   └── processor/      # EcosystemProcessor, NightSummaryProcessor, RecurringTaskGenerator, MarineCategoryAssigner
│   └── presentation/
│       ├── MainViewModel.kt           # ViewModel principal
│       ├── main/MainScreen.kt         # Pantalla principal (Box con capas)
│       ├── aquarium/AquariumCreatures.kt  # Sistema de animación de criaturas
│       ├── ecosystem/EcosystemScreen.kt   # Grid de criaturas desbloqueadas
│       └── theme/Theme.kt             # Paleta de colores marina
└── androidMain/kotlin/com/nmarsollier/riptide/
    └── data/local/
        ├── AppDatabase.kt             # Room DB, versión actual y migraciones
        └── dao/                       # DAOs para cada entidad
```
