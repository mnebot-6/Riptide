# Riptide — Indice Completo de Pantallas, Componentes y Funcionalidades

> **Proposito:** Indice de referencia para QA & Polish pre-Play Store. Cada seccion es una pantalla o componente mayor. Usar para revisar funcionalidades, detectar bugs, y planificar mejoras.

---

## 1. App Entry Point & Navegacion

- **App.kt** (androidMain)
  - NavHost con dos grafos:
    - `ROUTE_ONBOARDING` -> OnboardingScreen (solo primer lanzamiento, controlado por DataStore key `onboarding_completed`)
    - `ROUTE_MAIN` -> MainShellScreen (contenedor con bottom nav)
  - Transiciones horizontales con slide animation (300ms)
- **Navigation.kt** — Rutas disponibles:
  - `"main"` -> MainShellScreen (contiene las 3 tabs principales)
  - `"block/create"` -> BlockFormScreen (crear bloque, slide vertical)
  - `"block/edit/{blockId}"` -> BlockFormScreen (editar bloque, slide vertical)
  - `"ecosystem"` -> EcosystemScreen (galeria completa de criaturas)
  - `"settings"` -> SettingsScreen (pantalla completa de ajustes)

---

## 2. MainShellScreen — Contenedor Principal con Bottom Nav

- **Archivo:** `presentation/main/MainShellScreen.kt`
- **Funcion:** Contenedor raiz que gestiona tabs, acuario compartido, y navegacion inferior
- **Layout:**
  - **Fondo condicional:** AquariumBackground + AquariumCreatures solo en tabs TODAY y POND (no en PROGRESS)
  - **AnimatedContent:** crossfade 200ms entre tabs
  - **RiptideBottomBar:** barra de navegacion inferior fija

### 2.1 RiptideBottomBar (barra inferior)
- **Archivo:** `presentation/navigation/RiptideBottomBar.kt`
- **Altura:** 56dp
- **Fondo:** gradiente vertical (transparente arriba -> 0xCC0A1628 abajo)
- **3 tabs (BottomNavTab enum):**
  - **TODAY** — icono `ic_check_square`, label "Today"
  - **POND** — icono `ic_fish`, label "Pond"
  - **PROGRESS** — icono `ic_bar_chart`, label "Progress"
- **Estilo por tab:**
  - Seleccionado: icono blanco 20dp, texto blanco SemiBold 11sp, fondo cyan glow (0x337EC8E3, rounded 8dp)
  - No seleccionado: icono 0x80FFFFFF, texto 0x80FFFFFF Normal 11sp, sin fondo
- **Interaccion:** click sin ripple, actualiza `selectedTab`
- **Persistencia:** `rememberSaveable` mantiene tab seleccionada

### 2.2 Gestion de criaturas compartida
- `creatureFreezeState` — congela criaturas al abrir CreatureDetailDialog
- En tab POND: tap en criatura -> abre detalle, al dismiss descongelar
- `pondSelectedCreature` — estado compartido para criatura seleccionada

---

## 3. Tab TODAY — MainScreen (Gestion de Tareas)

- **Archivo:** `presentation/main/MainScreen.kt`
- **Layout general:**
  - Fondo: AquariumBackground (renderizado por MainShellScreen)
  - Criaturas animadas (renderizadas por MainShellScreen)

### 3.1 MainHeader (barra superior)
- **Fila izquierda:**
  - Logo Riptide (ic_riptide_logo, 24dp) + "Riptide" (bold 20sp)
- **Fila derecha — botones circulares (40dp, CardBackground #44FFFFFF):**
  - Boton "Ir a hoy" (ic_history) — solo visible si `selectedDate != today`
  - Racha global (icono llama + numero) — solo visible si `selectedDate == today` Y `globalStreak >= 2`
  - Boton calendario (ic_calendar) -> abre DatePicker
  - Boton "+" (ic_plus) -> abre TaskFormSheet (crear tarea)
  - Boton bloques (ic_sliders) -> abre BlocksBottomSheet
  - Boton ajustes (ic_settings) -> navega a SettingsScreen
- **Debajo:** Calendario semanal horizontal con seleccion de fecha y navegacion semana ant/sig

### 3.2 MainContent (lista de tareas)
- LazyColumn con bloques ordenados
- **BlockSection** por cada WorkBlock:
  - **BlockHeader:**
    - Icono emoji + nombre del bloque
    - Rango horario (si tiene schedule)
    - Badge de racha del bloque
    - Long press -> creacion rapida de tarea para ese bloque
  - **SwipeableTaskCard** por cada tarea:
    - Swipe derecho -> abre context menu
    - Swipe izquierdo -> reservado (sin accion actual)
  - **TaskCard** (52dp altura fija, single-row):
    - Titulo (strikethrough si completada)
    - Hora (si tiene)
    - Estrella de prioridad (si `isPriority`)
    - Indicador de notas (si tiene notas)
    - Contador `currentCount/targetCount` (si contable, sin checkbox)
    - Boton timer play/pause filled (si tiene `timerDurationMinutes`)
    - Estilos por estado:
      - PENDING -> normal
      - COMPLETED -> gris, strikethrough
      - EXPIRED -> strikethrough
      - POSTPONED -> dimmed
    - **Interacciones:**
      - Tap -> toggle completar/descompletar
      - Long press -> context menu
      - Botones +/- (contable) -> incrementar/decrementar
      - Boton timer -> iniciar/pausar timer
- Seccion "Sin bloque" para tareas sin blockId

### 3.3 BlocksBottomSheet (gestion de bloques)
- **Trigger:** boton ic_sliders en header
- **Forma:** dialog modal fullscreen con sheet en la parte inferior
- **Fondo:** gradiente OceanDeep -> OceanMid, bordes redondeados arriba (20dp)
- **Layout:**
  - Drag handle (36x4dp)
  - Titulo "Blocks" (16sp, semibold)
  - Lista de bloques:
    - Fila por bloque: icono emoji (18sp) + nombre (15sp) + chevron ">" (20sp)
    - Click -> navega a editar bloque (ROUTE_BLOCK_EDIT)
  - Fila "Add Block": icono plus (18dp) + "Add Block" (15sp, TextSecondary)
    - Click -> navega a crear bloque (ROUTE_BLOCK_CREATE)
- **Dismiss:** tap fuera del sheet

### 3.4 Context Menu (bottom sheet)
- **Trigger:** long press en TaskCard
- **Opciones:**
  - Editar (icono lapiz) -> abre TaskFormSheet o dialogo de alcance (si recurrente)
  - Posponer (icono reloj) -> abre PostponeSheet (solo si no completada/expirada)
  - Eliminar (icono papelera, rojo) -> dialogo de eliminacion (si recurrente: opciones de alcance)
- Fondo: gradiente oscuro, dismissible por drag-down

### 3.5 Dialogos de alcance (recurrentes)
- **Edicion recurrente:**
  - "Editar solo esta"
  - "Editar esta y futuras"
  - "Editar todas las instancias"
- **Eliminacion recurrente:**
  - "Eliminar solo esta"
  - "Eliminar esta y futuras"
  - "Eliminar todas las instancias"

### 3.6 Night Summary Dialog
- **Trigger:** aparece tras procesar resumen nocturno
- Mensaje de feedback (localizado)
- Estadisticas: "5/10 completadas"
- Boton dismiss

### 3.7 Lootbox Dialog (2 estados)
- **Estado cerrado:**
  - Icono (regalo normal, decoracion especial)
  - Nombre de categoria + info de nivel
  - Boton "Abrir Lootbox"
- **Estado revelado:**
  - Icono grande de criatura (96dp)
  - Nombre de especie
  - Badge de rareza (coloreado: COMMON gris, UNCOMMON verde, RARE azul, EPIC purpura, LEGENDARY naranja)
  - Si fauna/flora: campo de nickname + boton confirmar
  - Si decoracion: confirmacion inmediata
- **Flujo:** Abrir -> resolucion weighted-random -> revelar -> nickname -> insertar en DB -> siguiente lootbox o cerrar

### 3.8 CreatureDetailDialog
- Icono grande de criatura (96dp+)
- Nombre de especie (localizado)
- Badge de rareza
- Indicador de nivel
- Editor de nickname (TextField + boton update)
- Fecha de desbloqueo
- Barra de progreso XP
- Boton cerrar (X)

### 3.9 DatePicker Dialog
- Selector de fecha con paleta marina
- Input readonly (click abre picker)

---

## 4. Tab POND — PondTabContent (Acuario Completo)

- **Archivo:** `presentation/pond/PondTabContent.kt`
- **Layout:** pantalla completa ocupada por el acuario (background renderizado por MainShellScreen)
- **UI overlay minima:**
  - **Boton "My Ecosystem"** (esquina superior derecha):
    - Posicion: top-right con statusBarsPadding + 16dp padding
    - Fondo: CardBackground (#44FFFFFF), bordes redondeados 16dp
    - Contenido: icono ic_box (18dp) + "My Ecosystem" (14sp, medium)
    - Click -> navega a EcosystemScreen (ROUTE_ECOSYSTEM)
- **Interaccion con criaturas:**
  - Tap en criatura -> abre CreatureDetailDialog (gestionado por MainShellScreen)
  - Criatura se congela durante el dialogo
  - Al dismiss -> descongela criatura, limpia seleccion
  - Edicion de nickname -> `onCreatureNicknameChanged(creatureId, nickname)`

---

## 5. Tab PROGRESS — ProgressTabContent (Estadisticas + Historial)

- **Archivo:** `presentation/progress/ProgressTabContent.kt`
- **Fondo:** sin acuario (MainShellScreen no renderiza background para esta tab)
- **Layout:**
  - **Toggle superior (sub-tabs):**
    - Posicion: top con statusBarsPadding + 12dp vertical, 16dp horizontal
    - 2 botones centrados con 8dp spacing:
      - "Stats" -> muestra StatsScreen
      - "History" -> muestra HistoryScreen
    - **Estilo toggle:**
      - Seleccionado: fondo #1A73E8, texto blanco SemiBold
      - No seleccionado: fondo CardBackground (#44FFFFFF), texto #80FFFFFF Normal
      - Bordes redondeados 12dp, padding 20dp horiz / 8dp vert
  - **Contenido condicional:**
    - Sub-tab STATS -> StatsScreen (sin boton back, sin header propio)
    - Sub-tab HISTORY -> HistoryScreen (sin boton back, sin header propio)

---

## 6. SettingsScreen — Pantalla de Ajustes (pantalla completa)

- **Archivo:** `presentation/settings/SettingsScreen.kt`
- **Acceso:** boton ic_settings en MainHeader (tab TODAY)
- **Ruta:** `ROUTE_SETTINGS`
- **Fondo:** gradiente OceanDeep -> OceanMid
- **Header:** boton back circular + "Settings" (bold 20sp) + divider
- **Contenido (scroll vertical, 20dp padding):**
  - **Seccion SETTINGS:**
    - Label "SETTINGS" (all-caps, 11sp, semibold, 1.5sp letter-spacing)
    - **Night Summary Time:**
      - Icono luna (18dp) + "Night Summary"
      - TimeInputField compacto (no nullable)
    - **Morning Reminder:**
      - Icono sol (18dp) + "Morning Reminder"
      - Switch (toggle on/off):
        - ON -> muestra TimeInputField, default 8:00 AM
        - OFF -> oculta time, envia null
      - Colores switch: checked thumb blanco, checked track #4A90D9, unchecked thumb #99FFFFFF, unchecked track #33FFFFFF
  - **Divider + spacing**
  - **Seccion ECOSYSTEM:**
    - Label "ECOSYSTEM"
    - **Live Wallpaper:** icono olas (18dp), clickable -> abre chooser wallpaper del sistema
  - **Divider + spacing**
  - **Seccion ACCOUNT:**
    - Label "ACCOUNT"
    - **Si NO logueado:**
      - "Sign in with Google" con icono user
    - **Si logueado:**
      - **Avatar:** circulo 32dp con inicial (14sp bold blanco sobre #4A90D9)
      - Display name (14sp, medium) + email (12sp, secondary)
      - **Sync Now:** icono refresh (18dp) + "Sync Now" + SyncStatusBadge:
        - IDLE -> oculto
        - SYNCING -> "Syncing" (#B3FFFFFF)
        - SUCCESS -> "Success" (#81C784 verde)
        - ERROR -> "Error" (#E57373 rojo)
        - OFFLINE -> "Offline" (#80FFFFFF)
      - **Sign Out:** icono logout (18dp) + "Sign Out"

---

## 7. MainDrawer (legacy — definido pero no integrado actualmente)

- **Archivo:** `presentation/main/MainDrawer.kt`
- **Estado:** El composable existe en el codigo pero NO esta conectado a MainShellScreen
- **Contenido previo:** blocks, ecosystem, progress, settings, account
- **Nota:** Sus funcionalidades fueron redistribuidas a:
  - Blocks -> BlocksBottomSheet (header tab TODAY)
  - Settings -> SettingsScreen (pantalla completa)
  - Ecosystem -> Boton en tab POND
  - Stats/History -> Tab PROGRESS con sub-tabs
  - Account/Sync -> SettingsScreen

---

## 8. TaskFormSheet — Crear/Editar Tarea (bottom sheet)

- **Forma:** bottom sheet con bordes redondeados arriba (20dp)
- **Fondo:** gradiente OceanDeep -> OceanMid
- **Layout:**
  - **Drag handle** (36x4dp)
  - **Titulo:** "New Task" o "Edit Task"
  - **Campo nombre:**
    - Label "NAME"
    - TextField con placeholder "Task title"
    - Validacion: error si vacio
  - **Toggle recurrente:**
    - Switch "Recurring task?"
    - Deshabilitado si `forceRecurring = true`
  - **Si NO recurrente (one-time):**
    - DateInputField (obligatorio)
    - TimeInputField (opcional)
    - Toggle notificaciones (visible solo si hay hora): icono campana + switch
  - **Si recurrente:**
    - TimeInputField (opcional)
    - Toggle notificaciones (visible solo si hay hora)
    - Selector de dias (7 botones circulares 36dp, Lun-Dom):
      - Seleccionado: fondo azul (#1A73E8)
      - No seleccionado: fondo gris transparente
      - Al menos 1 dia requerido
  - **Feature toggles (grid 2x2):**
    - **Prioridad:** icono estrella, color naranja (#FFB347) cuando activo
    - **Contable:** icono #, click abre dialogo:
      - Presets: 2, 3, 5, 10
      - Input custom (1-999)
      - Opcion "Remove config" (rojo)
      - Boton "ACCEPT"
    - **Timer:** icono timer, click abre dialogo:
      - Presets: 5m, 10m, 15m, 25m, 45m
      - Input custom (1-999 min)
      - Opcion "Remove config" (rojo)
      - Boton "ACCEPT"
    - **Notas:** icono file, click abre dialogo:
      - Templates: Checklist, Description, Journal, Key-value
      - TextArea multilinea (min 100dp)
      - Opcion "Remove config" (rojo)
      - Boton "ACCEPT"
  - **Seleccion de bloque:**
    - Label "BLOCK" o "BLOCK (optional)"
    - Grid 2 columnas de chips:
      - "No Block" (solo one-time, gris)
      - Chip por bloque: icono + nombre, borde coloreado si seleccionado
    - Validacion: error si recurrente sin bloque
  - **Botones de accion:**
    - Cancelar (fondo CardBackground)
    - Guardar (fondo #1A73E8, valida campos)
    - Eliminar (solo edicion, fondo rojo, pide confirmacion)

---

## 9. PostponeSheet — Posponer Tarea (bottom sheet)

- **Fondo:** gradiente OceanDeep -> OceanMid
- **Layout:**
  - Drag handle
  - Titulo "Postpone Task"
  - Subtitulo: nombre de la tarea
  - DateInputField (obligatorio)
  - TimeInputField (opcional)
  - Mensaje de error (rojo si falta fecha)
  - Botones: Cancelar + Posponer (azul #1A73E8)

---

## 10. BlockFormScreen — Crear/Editar Bloque (pantalla completa)

- **Acceso:** desde BlocksBottomSheet (editar bloque o crear nuevo)
- **Transicion:** slide vertical (300ms)
- **Fondo:** gradiente OceanDeep -> OceanMid -> OceanLight
- **Header:** boton back (circular 48dp) + titulo + boton Save
- **Formulario:**
  - **Nombre:**
    - Label "NAME"
    - TextField "Block name" (obligatorio)
  - **Icono:**
    - Label "ICON"
    - TextField "Emoji icon" (max 2 chars, default caja)
  - **Color:**
    - Label "COLOR"
    - LazyRow horizontal con 12 colores marinos:
      - #4A90D9, #5BB5A2, #E8896B, #D4A84B, #8C7AE6, #5DADE2
      - #6BCB77, #F48FB1, #78909C, #F0A050, #7E57C2, #26A69A
    - Circulos 36dp, borde blanco 3dp si seleccionado
  - **Schedule (opcional):**
    - Toggle "Add weekly schedule?"
    - Si habilitado:
      - 7 botones de dia (36dp circulos, Mon-Sun)
      - Toggle "Same time for all?"
        - Si si: fila compartida Start/End time
        - Si no: fila por dia con Start/End time individuales
  - **Boton eliminar (solo edicion):**
    - Fondo rojo (#33EA4335), texto rojo
    - Click -> `onDelete(block.id)`

---

## 11. EcosystemScreen — Coleccion de Criaturas (pantalla completa)

- **Acceso:** boton "My Ecosystem" en tab POND
- **Ruta:** `ROUTE_ECOSYSTEM`
- **Fondo:** OceanDeep solido
- **Header:** boton back + "Ecosystem" centrado
- **Contenido (scroll vertical):**
  - **EcosystemCategorySection** por categoria (FISH, INVERTEBRATES, MAMMALS, REPTILES, DECORATION, COMPANION):
    - **Header de categoria:**
      - Nombre en MAYUSCULAS (11sp)
      - Icono candado si categoria no desbloqueada
      - "Level X" si desbloqueada (excepto decoracion/companion)
    - **Barra de progreso (si desbloqueada):**
      - 3dp altura, extremos circulares
      - Fondo: 0x22FFFFFF, relleno: accent color (0.55 alpha)
      - Progreso = currentLevel / nextUnlockLevel
    - **Grid de especies (filas de 3, 120dp minimo):**
      - **Criatura desbloqueada (UnlockedCreatureCard):**
        - Fondo: 0x22FFFFFF
        - Icono 52dp con nivel visual
        - Nombre o nickname (12sp, max 1 linea)
        - Chip de rareza (coloreado, 8sp bold):
          - COMMON: #9E9E9E (gris)
          - UNCOMMON: #4CAF50 (verde)
          - RARE: #2196F3 (azul)
          - EPIC: #9C27B0 (purpura)
          - LEGENDARY: #FF9800 (naranja)
        - Click -> CreatureDetailDialog
      - **Criatura bloqueada (LockedCreatureCard):**
        - Fondo: 0x11FFFFFF (mas oscuro)
        - Icono blur 4dp + alpha 0.22
        - Chip de rareza dimmed (0.5 alpha)
        - Pista de desbloqueo (9sp):
          - Treasure Chest: "Perfect X-day streak"
          - Anchor: "X completed tasks"
          - Coral Throne: "X-day perfect streak"
          - Otros: hint generico o icono candado
    - Ordenamiento: desbloqueadas primero (por rareza), luego bloqueadas

---

## 12. StatsScreen — Estadisticas (embebida en tab PROGRESS)

- **Acceso:** sub-tab "Stats" dentro de tab PROGRESS
- **Nota:** sin header propio ni boton back (integrada en ProgressTabContent)
- **Header interno:** toggle modo display (% / suma)
- **Range Selector:** 3 botones (WEEK, MONTH, ALL_TIME):
  - Seleccionado: fondo azul (#1A73E8)
  - No seleccionado: transparente
- **Contenido por rango:**
  - **WEEK / MONTH:**
    - **CompletionBarChart (Canvas):**
      - Eje Y: 100%/50%/0% (o conteo absoluto)
      - Barras por dia con colores dinamicos:
        - Vacio: 0x44FFFFFF
        - <50%: #E57373 (rojo)
        - 50-80%: #FFB74D (naranja)
        - 80-100%: #4DD0E1 (cyan)
        - 100%: #1A73E8 (azul)
      - Lineas de referencia dashed
      - Eje X: abreviaturas de dia (semana) o numeros cada 5 dias (mes)
      - Hoy en bold blanco
    - **Leyenda:** 4 chips coloreados (Low, Medium, High, Perfect)
    - **StatCards (grid 2 columnas):**
      - Dias activos
      - Total completadas
      - Mejor dia
      - Dias perfectos
      - Avg completion %
      - Total tareas
      - Perfect rate %
  - **ALL_TIME:**
    - **MonthlyTrendChart:** barras por mes
    - **StatCards:**
      - Racha mas larga jamas
      - Avg daily completion
      - Total dias activos
      - Total completadas
      - Dias perfectos (total)
      - Avg completion rate
      - Total tareas
      - Perfect days rate %
    - **MonthComparisonRow:**
      - "This Month" -> rate (20sp bold azul)
      - Flecha arriba/abajo + % cambio (cyan up, rojo down)
    - Best week label con avg %
  - **Seccion STREAKS (todos los rangos):**
    - Label "STREAKS" (all-caps)
    - StreakRow por bloque:
      - Dot coloreado 10dp (color del bloque)
      - Icono + nombre (14sp)
      - Icono llama + contador (14sp bold)

---

## 13. HistoryScreen — Historial (embebida en tab PROGRESS)

- **Acceso:** sub-tab "History" dentro de tab PROGRESS
- **Nota:** sin header propio ni boton back (integrada en ProgressTabContent)
- **SearchField:**
  - Icono busqueda (18dp) + TextField "Search tasks..." + boton X para limpiar
  - Filtro case-insensitive por titulo
- **BlockFilterChips (scroll horizontal):**
  - Chip "All" (siempre primero):
    - Seleccionado: fondo OceanMid
    - No seleccionado: borde 1dp
  - Chip por bloque: dot de color 8dp + nombre
  - Click -> filtra por bloque
- **Timeline (LazyColumn):**
  - **DayHistoryCard por fecha (descendente):**
    - **Header:** fecha "DD MMM YYYY" (15sp) + badge completadas "X/Y":
      - Color badge por %: gris (0%), rojo (<50%), naranja (50-80%), cyan (80-100%), azul (100%)
    - **Divider** (1dp, 0x1AFFFFFF)
    - **HistoryTaskRow por tarea:**
      - Check o X (13sp bold, cyan si completada, dim si no)
      - Dot de color del bloque (7dp, opcional)
      - Titulo (14sp, blanco si completada, secundario si no)
      - Hora HH:MM (12sp, secundario, alineado a derecha)
  - **Empty states:**
    - Sin datos: "No completed tasks yet"
    - Con filtros sin resultados: "No tasks matching your filters"

---

## 14. OnboardingScreen — Flujo Inicial (4 paginas)

- **Fondo:** gradiente OceanDeep -> OceanMid -> OceanLight
- **Boton Skip:** top-right (visible paginas 0-2, oculto en la ultima)
- **Contenido por pagina:**
  - **Pagina 0 — Welcome:**
    - Emoji ola (72sp)
    - "Welcome to Riptide"
    - Texto introductorio
  - **Pagina 1 — How it Works:**
    - Emoji calendario (72sp)
    - "How it Works"
    - 5 extras en cards:
      - "Morning: Plan your day"
      - "Day: Complete tasks"
      - "Night: Review progress"
      - "Summary: Track streaks"
      - "Sync: Backup data"
  - **Pagina 2 — Ecosystem:**
    - Emoji pez (72sp)
    - "Your Ecosystem"
    - 3 extras en cards:
      - "Loot boxes unlock creatures"
      - "Each species is unique"
      - "No pressure - collect at your pace"
  - **Pagina 3 — Ready:**
    - Emoji estrella (72sp)
    - "You're Ready!"
    - Sin extras
- **Transicion:** AnimatedContent (280ms tween), slide + fade segun direccion
- **Dot indicators:** 4 dots (activo 10dp blanco, inactivo 7dp 0.35 alpha)
- **Navegacion:**
  - Flecha atras (visible si paso > 0)
  - Boton "NEXT" (pasos 0-2) o "START" (paso 3)
  - Color: AccentBlue (#4A90D9), RoundedCorner(24dp)
  - Skip -> onComplete(), Start -> onComplete()

---

## 15. Sistema de Acuario — Rendering

### 15.1 AquariumBackground
- **Cielo dinamico:** interpolacion de color entre 9 keyframes (0:00 -> 23:00)
  - Midnight: #060B1A, Pre-dawn: #0A1030, Sunrise: #4A3060/#B85C2A/#E8A048
  - Morning: #6A80C0, Midday: #3A78CC, Late afternoon: #4A88D0
  - Sunset: #6A3050/#CC5530, Twilight: #3A1848, Early night: #0D0820
- **Gradiente de agua:** 3 capas (superficie cyan -> medio azul -> profundo azul oscuro)
- **Fondo marino:** arena con textura de ondulaciones, variacion de altura (0.80-0.90 Y-range)
- **Vineta:** oscurecimiento en bordes izquierdo/derecho y abajo
- **Superficie:** ola animada con sway
- **Burbujas:** 7 particulas deterministas ascendentes con fade in/out

### 15.2 AquariumLighting
- **Rayos solares (5 rayos):**
  - Activos 6:30-19:00
  - Fade in 6:30-8:00, full 8:00-17:00, fade out 17:00-19:00
  - Nubes reducen visibilidad hasta 85%
  - Oscilacion angular lenta (~0.15 rad/sec)
- **Causticas (8 lineas sinusoidales):**
  - Activas 7:00-18:00
  - Dual-frequency para no repetir patron exacto
  - Fade por profundidad

### 15.3 AquariumParticles
- **Plankton (20 particulas):** color cyan-azul, drift browniano, double-circle glow
- **Sedimento (12 particulas):** color arena, caida continua con wobble horizontal, ciclo 25s
- **Corrientes (3-6 dinamicas):** solo si windStrength >= 0.15, lineas horizontales con undulacion

### 15.4 AquariumWeatherEffects
- **Nubes (5 formas):** ellipses superpuestas, 1-5 visibles segun cloudCoverage, drift por windStrength
- **Lluvia (40 gotas):** activa si rainIntensity > 0.05, trayectoria con viento, efecto ripple en superficie

### 15.5 AquariumCreature — Sistema de Animacion
- **Oscilador:** `(elapsedMs % periodMs) / periodMs` — wrap sin teleportacion
- **Tipos de movimiento:**
  - SMOOTH (coseno): peces, tortugas, mamiferos
  - BURST: calamares, camarones, pulpos (75% distancia en 25% tiempo)
  - CRAWL: langosta, cangrejo ermitano (93% lineal + 7% coseno)
- **Zonas de nado:** SURFACE, UPPER, MID, LOWER, BOTTOM
- **Parametros por criatura:**
  - swimDuration, wobbleAmplitude, waveCount, erraticness
  - driftSpeed, pauseFraction, verticalCoupling, microWobble
  - xErraticness, tempoVariation, sizeMultiplier

### 15.6 Renderers (70 especies, cobertura total)
- **Flora (9):** BrainCoral, Anemone, Kelp, Posidonia, FanCoral, TubeSponge, SeaGrass, FireCoral, StaghornCoral
- **Fauna — Fish (9):** Clownfish, Angelfish, Pufferfish, Surgeonfish, Lionfish, Sunfish, Butterflyfish, Seahorse, Moray Eel
- **Fauna — Crustacean (9):** Lobster, Hermit Crab, Shrimp, Spider Crab, Barnacle, Krill, Horseshoe Crab, Mantis Shrimp, Coconut Crab
- **Fauna — Mollusk (9):** Sea Urchin, Starfish, Oyster, Nautilus, Giant Clam, Conch, Scallop, Sea Slug, Sea Cucumber
- **Fauna — Pelagic (9):** Manta Ray, Moon Jellyfish, Whale Shark, Hammerhead, Barracuda, Bluefin Tuna, Flying Fish, Lionsmane Jellyfish, Swordfish
- **Fauna — Cephalopod (6):** Octopus, Squid, Cuttlefish, Blue Ringed Octopus, Chambered Nautilus, Giant Pacific Octopus
- **Fauna — Reptile (6):** Sea Turtle, Marine Iguana, Green Sea Turtle, Sea Snake, Leatherback Turtle, Saltwater Crocodile
- **Fauna — Mammal (6):** Dolphin, Seal, Blue Whale, Sea Otter, Manatee, Narwhal
- **Decoration (6):** Treasure Chest, Anchor, Sunken Ship, Diving Helmet, Coral Throne, Golden Trident
- **Easter Egg (1):** Bimba (companion especial)
- **Evolucion visual:** efectos discretos nivel 3+ y 5+ (marcas, brillos, companeros, bioluminiscencia) en 10 renderers

### 15.7 CreatureIcon (composable reutilizable)
- Canvas animado para todas las 70 especies
- Parametro de nivel afecta apariencia visual
- Usado en: EcosystemScreen, LootboxDialog, CreatureDetailDialog

---

## 16. Widget Android (Glance)

- **Layout:**
  - Header: "Riptide" + dia de semana + fecha (e.g., "Mon 5")
  - Indicador de progreso: "3 / 8" o "All Done!" (verde)
  - Barra de progreso (cyan incompleto, verde completo, 6dp alto)
  - Lista scrollable de tareas del dia
- **Fila de tarea:**
  - Check cyan si completada, espacio si pendiente
  - Stripe de color del bloque (4dp ancho)
  - Titulo + progreso contador opcional ("Morning Exercise 2/3")
  - Nombre del bloque (10sp) + hora HH:MM (11sp, alineada derecha)
- **Interacciones:**
  - Tap en tarea -> toggle completar (ToggleTaskAction)
  - Tap en header -> abre MainActivity
- **Data:** Room DB, filtra POSTPONED, ordena por: completado -> hora -> titulo
- **Actualizacion:**
  - `WidgetUpdater.refreshAll()` tras cambios + en `onResume`
  - Auto-update cada 30 min
  - Metadata: 3x3 celdas, redimensionable
- **Estilo:** fondo #CC0A1628, accent #4FC3F7, check #4DD0E1, rows #22FFFFFF

---

## 17. Live Wallpaper

- **RiptideWallpaperService:**
  - Renderiza escena completa del acuario continuamente
  - Orden de render: background -> particulas -> rayos sol -> causticas -> criaturas
  - **FPS configurable:** 15/30/60 (desde preferencias, seleccion en SettingsScreen)
  - Frame pacing: Choreographer vsync-aligned, solo dibuja si paso intervalo objetivo
  - Hardware canvas (`lockHardwareCanvas()`) para API 26+
  - `GLOBAL_SPEED_MULTIPLIER = 2.0f`
  - Bridge `CanvasDrawScope` reutiliza todo el rendering Compose
- **WallpaperDataProvider:**
  - Lee criaturas de Room con `@Volatile`
  - Refresco cada 5 min (DATA_REFRESH_INTERVAL_MS = 300000)
  - Retorna: unlockedCreatures, fixedCreatures, creatureLevelBySpecies
- **Lifecycle:**
  - `onVisibilityChanged(true)` -> marca wallpaper activado, verifica desbloqueo Sunken Ship
  - `onVisibilityChanged(false)` -> para rendering (ahorro bateria)
- **No interactivo** (solo lectura visual)

---

## 18. Sistema de Notificaciones

### 18.1 Canales (NotificationHelper)
- **night_summary** (IMPORTANCE_DEFAULT): "Night Summary" — resumen diario
- **morning_reminder** (IMPORTANCE_DEFAULT): "Morning Reminder" — aviso matutino
- **task_reminder** (IMPORTANCE_HIGH): "Task Reminder" — alarma por tarea

### 18.2 Tipos de notificacion
- **Night Summary (ID: 1001):**
  - "Completed X out of Y tasks today"
  - Priority: DEFAULT
  - Trigger: hora configurada por usuario cada noche
- **Morning Reminder (ID: 1002):**
  - "Time to start your day!"
  - Priority: DEFAULT
  - Trigger: hora configurada, opcional (toggle en SettingsScreen)
- **Task Reminder (ID: taskId.hashCode()):**
  - Titulo = nombre de tarea, "Time for your task"
  - Priority: HIGH (sonido + vibracion)
  - Trigger: hora programada de la tarea

### 18.3 Workers (WorkManager)
- **NightSummaryWorker:**
  - Calcula estadisticas de completado del dia
  - Procesa rachas de bloques
  - Actualiza ecosistema (desbloqueos de criaturas)
  - Crea DaySummary en DB
  - Envia push de resumen
  - Se auto-reprograma para la misma hora al dia siguiente
- **MorningReminderWorker:**
  - Envia notificacion matutina
  - Se auto-reprograma al dia siguiente
  - Si hora = null -> se cancela
- **TaskReminderWorker:**
  - One-shot a la hora de la tarea
  - Input: taskId + taskTitle
- **TaskReminderSchedulerImpl:**
  - `scheduleReminder(taskId, title, scheduledAt)` — programa notificacion
  - `cancelReminder(taskId)` — cancela pending
  - `rescheduleAll()` — recarga todas las tareas pendientes desde DB
  - Work name: `"task_reminder_$taskId"`, policy REPLACE

---

## 19. MainViewModel — Acciones del Estado

### 19.1 Gestion de tareas
- `toggleTaskCompleted(task)` — toggle COMPLETED/PENDING, otorga XP si nueva completada, cancela reminder
- `incrementTaskCount(task)` — +1 hacia target, auto-completa si alcanza target
- `decrementTaskCount(task)` — -1, revierte completado si estaba completada
- `toggleTaskPriority(task)` — toggle isPriority
- `updateTaskNotes(task, notes)` — actualiza notas

### 19.2 Timer
- `startTimer(taskId, durationMinutes)` — countdown, auto-completa al llegar a 0
- `pauseTimer(taskId)` — pausa sin cancelar
- `resumeTimer(taskId)` — reanuda
- `cancelTimer(taskId)` — detiene y limpia

### 19.3 CRUD one-time
- `addOneTimeTask(title, blockId?, date, time?, notifications, targetCount?, notes?, timerMinutes?, isPriority)`
- `updateOneTimeTask(...)` — actualiza tarea existente
- `deleteTask(task)` — elimina + cancela reminder

### 19.4 CRUD recurrente
- `addRecurringTask(title, blockId, time?, recurrence, notifications, targetCount?, noteTemplate?, timerMinutes?, isPriority)` — crea def + genera instancias 7 dias
- `updateRecurringTask(...)` — actualiza def + regenera instancias + reprograma reminders
- `deleteRecurringTaskInstance(task)` — marca como CANCELLED
- `deleteRecurringTaskFromDate(task)` — desactiva def + elimina desde fecha
- `deleteRecurringTaskAll(task)` — desactiva def + elimina todas

### 19.5 Posponer
- `postponeTask(task, date, time?)` — marca original POSTPONED, crea nueva instancia

### 19.6 Lootbox
- `openLootbox()` — weighted-random resolve
- `confirmUnlock(spec, nickname)` — inserta criatura en DB
- `dismissLootbox()` — salta sin desbloquear

### 19.7 Sync & Auth
- `onSignInCompleted(user?)` — actualiza tras Google Sign-In
- `signOut()` — limpia auth + reset sync
- `syncNow()` — trigger sync manual

### 19.8 Settings
- `updateNightSummaryTime(time)` — cambia hora resumen nocturno
- `setWallpaperFps(fps)` — calidad wallpaper
- `updateCreatureNickname(creatureId, nickname)` — edita nickname

---

## 20. Tema y Paleta de Colores (Theme.kt)

- **Fondos:** OceanDeep #0A1628, OceanMid #1B3A6B, OceanLight #2E5F9E
- **Cards:** CardBg 0x33FFFFFF (33% blanco), CardBackground 0x44FFFFFF (acciones header)
- **Texto:** Primary #FFFFFF, Secondary 0xB3FFFFFF, SectionLabel #80FFFFFF
- **Accent:** #7EC8E3 (cyan)
- **Boton principal:** #1A73E8 (azul)
- **Error/Delete:** #EA4335 (rojo)
- **Prioridad:** #FFB347 (naranja)
- **Bloques:** 12 colores marinos en BlockFormScreen
- **Rarezas:** COMMON #9E9E9E, UNCOMMON #4CAF50, RARE #2196F3, EPIC #9C27B0, LEGENDARY #FF9800
- **Bottom bar glow (tab seleccionada):** 0x337EC8E3

---

## 21. Internacionalizacion (i18n)

- **Idiomas:** EN + ES
- **LocalizationExtensions.kt:** extension functions para enums (dias, meses, estados, etc.)
- Strings localizados para todas las pantallas
- Labels de tabs: `tab_today`, `tab_pond`, `tab_progress`

---

## 22. Sync Offline-First

- **Room v13** con `updatedAt` en 8 tablas + `isDeleted` en 3 (soft delete)
- **Ktor Client** (OkHttp) con auto-refresh JWT
- **`POST /api/sync`** — batch push+pull en una llamada
- **SyncManager:** conflict resolution (`updatedAt` wins, `hasBeenRewarded` OR-merge, server deletion autoritativo)
- **SyncTrigger:** debounce 5s tras mutacion local
- **SyncWorker:** periodico cada 1h (WorkManager, constraint CONNECTED)
- **InitialSyncPreparer:** stampa datos pre-existentes para primera sync
- **ConnectivityObserver:** Flow de estado de red

---

## Mapa de Navegacion (resumen visual)

```
App.kt
 |-- Onboarding (primer lanzamiento) -> navega a Main
 |
 '-- MainShellScreen [ROUTE_MAIN]
      |
      |-- Bottom Nav Bar (3 tabs)
      |    |-- TODAY -> MainScreen (tareas del dia)
      |    |-- POND -> PondTabContent (acuario completo)
      |    '-- PROGRESS -> ProgressTabContent
      |                      |-- sub-tab Stats -> StatsScreen
      |                      '-- sub-tab History -> HistoryScreen
      |
      |-- Pantallas externas (navegacion por ruta):
      |    |-- ROUTE_BLOCK_CREATE -> BlockFormScreen
      |    |-- ROUTE_BLOCK_EDIT -> BlockFormScreen
      |    |-- ROUTE_ECOSYSTEM -> EcosystemScreen
      |    '-- ROUTE_SETTINGS -> SettingsScreen
      |
      '-- Bottom sheets / dialogos (modales):
           |-- TaskFormSheet (crear/editar tarea)
           |-- PostponeSheet (posponer tarea)
           |-- BlocksBottomSheet (gestionar bloques)
           |-- Context Menu (acciones sobre tarea)
           |-- Night Summary Dialog
           |-- Lootbox Dialog
           |-- CreatureDetailDialog
           '-- DatePicker Dialog
```

---

## QA Checklist de Verificacion

1. **Bottom Navigation:**
   - Verificar las 3 tabs cambian correctamente con crossfade
   - Tab seleccionada persiste al rotar pantalla
   - Estilos: glow cyan en seleccionada, dimmed en no seleccionadas
   - Acuario visible en TODAY y POND, ausente en PROGRESS
2. **Tab TODAY:** abrir cada pantalla, probar cada interaccion (taps, swipes, long press, toggles)
3. **Tab POND:** verificar acuario fullscreen, boton ecosistema, tap en criaturas, dialogo detalle
4. **Tab PROGRESS:** verificar toggle Stats/History, datos correctos en ambas sub-tabs
5. **SettingsScreen:** night summary time, morning toggle, wallpaper, account/sync
6. **BlocksBottomSheet:** lista de bloques, editar, crear nuevo
7. **Verificar dialogos y bottom sheets** se abren/cierran correctamente
8. **Comprobar estados vacios** (sin tareas, sin criaturas, sin historial)
9. **Verificar notificaciones** (night summary, morning, task reminder)
10. **Probar widget** (instalar, tap en tarea, refresh)
11. **Probar live wallpaper** (activar desde Settings, verificar FPS, criaturas)
12. **Probar sync** (login Google, sync manual, badge de estado en Settings)
13. **Probar onboarding** (desinstalar/limpiar datos, verificar flujo completo)
14. **Verificar i18n** cambiando idioma del dispositivo (EN <-> ES)
15. **Verificar drawer legacy** no aparece en ningun flujo
