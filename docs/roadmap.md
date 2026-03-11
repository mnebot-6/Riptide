# Roadmap — Riptide

## Estado actual (MVP en curso)

### ✅ Completado

**Infraestructura**
- Proyecto KMP con Compose Multiplatform
- Room completo (entities, DAOs, database v5, mappers, implementaciones)
- `BlockCategory` con tabla separada y FK CASCADE
- `generateUUID` expect/actual (commonMain / androidMain / iosMain)
- `parseColor` y `currentDate` expect/actual
- Navigation Compose configurado
- `DataSeeder` con datos de prueba

**Dominio**
- Todos los modelos de dominio en commonMain
- Todas las interfaces de repositorio
- `MarineCategoryAssigner` — redistribución automática de categorías

**UI**
- Pantalla principal con fondo oceánico (gradiente + frosted glass)
- `WeekCalendar` — 7 días navegables, swipe horizontal, indicador día actual
- Lista de tareas por bloque con checkbox persistente
- Tareas tachadas al completarse, checkbox con color del bloque
- Drawer desde arriba semitransparente con gesto vertical
- Gestos unificados (vertical = drawer, horizontal = cambio de día)
- FAB 🐟 siempre visible
- `BlockFormScreen` completo (crear / editar / eliminar)
  - Campos: nombre, emoji, paleta de 12 colores, horario semanal
  - Selector de días con horarios individuales o compartidos
  - `TimeTextField` con validación (4 dígitos, rango 00:00–23:59)
  - Botón eliminar solo en modo edición

---

## Próximo — MVP restante

### Añadir tarea (bottom sheet desde el drawer)
- Campos: título, bloque (opcional), minutos estimados
- Se añade a la fecha seleccionada actualmente
- Sin bloque → sección "Sin bloque" en la pantalla principal

### Indicadores de tareas en el calendario
- Puntos bajo cada día según tareas pendientes / completadas
- Color del bloque o neutro si es sin bloque

### Resumen nocturno
- Cálculo de `DaySummary` al final del día
- Notificación a hora configurable
- Mensaje emocional según score interno

---

## v2

- Rachas por bloque (`BlockStreak`) con mensajes contextuales al completar
  - *"Llevas una semana sólida en el trabajo 🌊"*
  - *"El coral está creciendo 🪸"*
- Ecosistema visual — Canvas / Lottie
- Animaciones del estanque
- Configuración avanzada del resumen nocturno

---

## v3

- Backend Ktor + PostgreSQL
- Sincronización offline-first (IDs UUID ya preparados)
- Perfiles de usuario
- Google Sign-In
- Visitar el estanque de un amigo (solo ver, nunca competir ni rankear)

---

## Filosofía de diseño

- La puntuación **nunca** se muestra al usuario
- El abandono **pausa** el ecosistema, no lo destruye
- Sin rankings, sin comparaciones, sin presión numérica
- El feedback es siempre emocional y contextual
