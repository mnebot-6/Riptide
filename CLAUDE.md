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
- Ecosistema marino: 40 especies, 9 categorías, sistema lootbox con rareza, crecimiento individual
- Patrones de nado orgánicos (tempo warping, variación por instancia, márgenes simétricos)
- Resumen nocturno con filtrado correcto por `summaryTime`
- Tareas EXPIRED completables con checkbox
- TimePicker y DatePicker con paleta marina
- Inputs de fecha/hora readonly (click abre picker)
- Superficie del agua animada (olas con `Path` + `quadraticTo`, cresta doble)
- Fondo marino elaborado (arena con textura, 11 rocas en 3 estilos)
- Cielo dinámico por hora del día (7 periodos: noche, amanecer, mañana, día, atardecer, crepúsculo)
- Flora Canvas: BrainCoral, Anemone, Kelp — crecimiento visual por nivel, múltiples instancias
- `CreatureIcon` composable reutilizable (Canvas animado para flora, emoji para el resto)
- Crustáceos diferenciados en altura (Lobster pegado al suelo, Hermit Crab algo más arriba)
- EcosystemScreen con botón de retroceso y `CreatureIcon` en cards
- Mayor opacidad en tarjetas de tarea para legibilidad
- Sistema lootbox: desbloqueo aleatorio ponderado por rareza (COMMON→LEGENDARY) al subir nivel de categoría
- `LootboxResolver` con selección weighted-random al abrir lootbox (no al ganar)
- XP overflow: categorías completas redirigen 50% XP a la categoría de menor nivel
- EcosystemScreen: ordenamiento por rareza, barra de progreso por categoría, badges de rareza
- CreatureDetailDialog: niveles numéricos, badge de rareza con color
- Diálogo lootbox bifásico: cerrada (🎁 + categoría) → abierta (especie + rareza + nombre)
- **i18n**: `composeResources/values/` (EN) + `values-es/` (ES), `LocalizationExtensions.kt` con extension functions para enums
- **17 renderers Canvas**: 13 nuevos (`fauna/`: Surgeonfish, Lionfish, Sunfish, Hammerhead, Barracuda, Manatee, SpiderCrab, Cuttlefish, BlueRingedOctopus, SeaUrchin, Barnacle; `flora/`: Posidonia, FanCoral)
- **Conversión de densidad para Canvas swimmers**: `renderSize = iconSize * density` iguala tamaño visual con emojis; `sizeMultiplier` recalibrado por especie según extensión visual del renderer

**Próximo (v3):**
- Tests unitarios e instrumentados
- Onboarding
- Animaciones de transición

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
│       ├── aquarium/CreatureRenderer.kt    # Interface + rendererFor() (17 renderers) + CreatureIcon composable
│       ├── aquarium/EcosystemScreen.kt     # Grid de criaturas desbloqueadas
│       ├── aquarium/flora/                 # BrainCoralRenderer, AnemoneRenderer, KelpRenderer, PosidoniaRenderer, FanCoralRenderer
│       ├── aquarium/fauna/                 # MantaRayRenderer + 10 nuevos renderers (Surgeonfish, Lionfish, Sunfish, Hammerhead, Barracuda, Manatee, SpiderCrab, Cuttlefish, BlueRingedOctopus, SeaUrchin, Barnacle)
│       └── theme/Theme.kt                  # Paleta de colores marina
└── androidMain/kotlin/com/mnebot/riptide/
    └── data/local/
        ├── db/RiptideDatabase.kt       # Room DB v9, migraciones reales
        └── dao/                        # DAOs para cada entidad
```
