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
| `data-models.md` | Todos los modelos de dominio: WorkBlock, DayTask, RecurringTaskDef, MarineCreature, CreatureSpecies (40), XP curve |
| `project_structure.md` | Árbol de directorios completo con descripción de cada archivo |
| `roadmap.md` | Fases completadas y futuras (v3: testing/onboarding, v4: wallpaper dinámico, v5: backend, v6: iOS) |

**Para contexto profundo en una tarea específica, lee primero el archivo de /docs/ correspondiente.**

---

## Stack

| Tecnología | Versión |
|---|---|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.10.2 |
| Room | 2.8.4 (schema v10) |
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
- **Room migrations:** SIEMPRE migraciones reales (nunca `fallbackToDestructiveMigration`). Ver `MIGRATION_8_9` y `MIGRATION_9_10` como referencia.
- **hasBeenRewarded:** campo en `DayTask` para evitar XP duplicado — siempre respetar esta bandera al distribuir XP
- **notificationsEnabled:** campo en `DayTask` y `RecurringTaskDef` — solo relevante si la tarea tiene hora; propagar de def a instancias en `RecurringTaskGenerator`
- **Notificaciones:** usar siempre `NotificationHelper` para enviar pushes. Tres canales: `night_summary`, `morning_reminder`, `task_reminder`. `TaskReminderWorker` usa nombre único `"task_reminder_$taskId"`.
- **Recurrence:** enum serializado como string (`DAILY`, `WEEKLY`, `MONTHLY`)
- **Tiempo nulo en RecurringTaskDef:** tarea sin hora asignada — el generador la omite en el DaySummary si no tiene `summaryTime`

---

## Estado actual (marzo 2026)

**Completado:**
- MVVM + Room offline-first (v10, migraciones reales)
- Ecosistema marino: 40 especies, 9 categorías, sistema lootbox con rareza, crecimiento individual
- Patrones de nado orgánicos (tempo warping, variación por instancia, márgenes simétricos)
- Resumen nocturno con filtrado correcto por `summaryTime`; push notification tras `processDay`
- Tareas EXPIRED completables con checkbox
- TimePicker y DatePicker con paleta marina
- Inputs de fecha/hora readonly (click abre picker)
- Superficie del agua animada, fondo marino elaborado, cielo dinámico (7 periodos)
- Flora Canvas: BrainCoral, Anemone, Kelp, Posidonia, FanCoral — crecimiento visual por nivel
- `CreatureIcon` composable reutilizable (Canvas animado para flora, emoji para el resto)
- Sistema lootbox: desbloqueo aleatorio ponderado por rareza (COMMON→LEGENDARY)
- **i18n**: EN + ES, `LocalizationExtensions.kt` con extension functions para enums
- **40 renderers Canvas** (cobertura total): todas las 40 especies tienen Canvas renderer propio
- **Onboarding**: flujo de 4 pasos con `AnimatedContent`, DataStore key `onboarding_completed`, se muestra solo en primer lanzamiento
- **Notificaciones push** (en curso): `NotificationHelper` (3 canales), push resumen nocturno, aviso matutino configurable (`MorningReminderWorker`), campo `notificationsEnabled` en `DayTask`/`RecurringTaskDef` (Room v10), strings EN/ES

**Pendiente (sprint notificaciones):**
- `TaskReminderScheduler` + `TaskReminderWorker` + toggle en `TaskFormSheet`
- Inyectar scheduler en ViewModels y factories
- `MainActivity`: `createChannels()`, permiso `POST_NOTIFICATIONS`, `rescheduleAll()`

**Próximo (v3):**
- Pantalla de estadísticas (DaySummary semanal/mensual, rachas por bloque)
- Historial de tareas completadas
- Evolución visual de criaturas por nivel en renderers
- Recompensas automáticas de racha (lootbox en hitos)
- Tests unitarios e instrumentados
- Preparar firma de la app

---

## Archivos críticos

```
composeApp/src/
├── commonMain/kotlin/com/mnebot/riptide/
│   ├── domain/
│   │   ├── model/          # WorkBlock, DayTask, RecurringTaskDef, MarineCreature, etc.
│   │   ├── repository/     # Interfaces de repositorios
│   │   │   └── processor/      # EcosystemProcessor, NightSummaryProcessor, RecurringTaskGenerator, MarineCategoryAssigner
│   │   └── LootboxResolver.kt            # Selección weighted-random de especie al abrir lootbox
│   └── presentation/
│       ├── main/MainScreen.kt              # Pantalla principal (Box con capas)
│       ├── main/MainViewModel.kt           # ViewModel principal
│       ├── aquarium/AquariumBackground.kt  # Canvas: cielo dinámico, superficie, fondo marino
│       ├── aquarium/AquariumBounds.kt      # SURFACE_FRACTION, FLOOR_FRACTION compartidas
│       ├── aquarium/AquariumCreature.kt    # CreatureSpec, animación 60fps, hit-testing
│       ├── aquarium/CreatureRenderer.kt    # Interface + rendererFor() (40 renderers, cobertura total) + CreatureIcon composable
│       ├── aquarium/EcosystemScreen.kt     # Grid de criaturas desbloqueadas
│       ├── aquarium/flora/                 # BrainCoralRenderer, AnemoneRenderer, KelpRenderer, PosidoniaRenderer, FanCoralRenderer
│       ├── aquarium/fauna/                 # 35 renderers — COBERTURA TOTAL: todas las 40 especies (5 son flora/)
│       ├── onboarding/OnboardingScreen.kt  # Flujo 4 pasos: bienvenida, cómo funciona, ecosistema, listo
│       └── theme/Theme.kt                  # Paleta de colores marina
└── androidMain/kotlin/com/mnebot/riptide/
    ├── NotificationHelper.kt           # Canales + sendNightSummary/MorningReminder/TaskReminder
    ├── NightSummaryWorker.kt           # Resumen nocturno + push + auto-reprogramación
    ├── MorningReminderWorker.kt        # Aviso matutino + auto-reprogramación diaria
    ├── TaskReminderWorker.kt           # (pendiente) One-shot a la hora de la tarea
    └── data/local/
        ├── db/RiptideDatabase.kt       # Room DB v10, migraciones reales (8_9, 9_10)
        └── dao/                        # DAOs para cada entidad
```
