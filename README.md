# Riptide 🌊

A personal productivity app with an emotional reward system based on a **marine ecosystem that grows with your consistency**.

## Concept

Most productivity apps rely on streaks, percentages, and numbers to motivate you. Riptide takes a different approach: your consistency builds something beautiful. A marine aquarium that comes to life the more you stick to your routines.

The score exists internally but is never shown as a number. Feedback is always emotional — messages, visual growth of the tank, a sense of progress without explicit metrics.

**Core philosophy:** Consistency builds something beautiful. Abandonment pauses it. It never destroys it. If you have a bad week, the ecosystem goes quiet. When you pick up the rhythm again, it flourishes.

## Features (MVP)

- 📅 Weekly calendar with time blocks
- ✅ Daily task assignment to blocks
- 🌊 Marine ecosystem that grows with your consistency
- 🐟 Individual creatures with species, nickname, and level
- 🌙 Nightly summary with emotional feedback messages
- 📴 Fully offline — local first with Room

## Marine Categories

Each work block is assigned a marine category that determines which species grow in your tank:

| Category | Species | Example block |
|---|---|---|
| 🐟 Fish | Clownfish, Angelfish | Work |
| 🪸 Flora | Brain coral, Anemone | Learning |
| 🦞 Crustaceans | Hermit crab, Lobster | Sport / Exercise |
| 🐚 Mollusks | Starfish, Sea urchin | Personal / Home |
| 🦈 Pelagic | Moon jellyfish, Manta ray | Creative projects |

## Tech Stack

| Layer | Technology |
|---|---|
| Multiplatform | Kotlin Multiplatform (KMP) |
| UI | Compose Multiplatform |
| Android | Kotlin + Jetpack Compose |
| iOS | Compose Multiplatform |
| Persistence (MVP) | Room (local, offline-first) |
| Future backend (v3) | Ktor + PostgreSQL |
| Auth (v3) | Google Sign-In |

## Architecture

The project follows **MVVM** with a clean separation between layers:

```
commonMain                          androidMain
─────────────────────────────────────────────────────
UI (Compose) + ViewModels

domain/model/                       data/local/entity/
  WorkBlock, DayTask...    ←──→       Room entities
  (pure Kotlin)            mapper

domain/repository/                  data/local/dao/
  interfaces               ←──       Room DAOs
                           impl
```

- `commonMain` — shared logic, models, repository interfaces, UI, ViewModels
- `androidMain` — Room implementation, Android-specific platform code
- `iosMain` — iOS-specific platform code

## Roadmap

**MVP** — Local only with Room: weekly calendar, task assignment to blocks, mark as complete, basic nightly summary, ecosystem with 2 species per category.

**v2** — Block streaks, contextual messages on streak completion, richer ecosystem with more species and animations, advanced nightly summary configuration.

**v3** — Ktor + PostgreSQL backend, user profiles, add friends, visit a friend's tank (view only, never compete or rank).

## Project Status

🚧 Active development — MVP in progress.