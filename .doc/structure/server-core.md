# Server Core (com.hypixel.hytale.server.core)

## Key classes
- `HytaleServer` — primary server instance and lifecycle management.
- `Options` — CLI options and parsing.
- `HytaleServerConfig` — config.json model and codec.
- `ShutdownReason` — shutdown codes and messages.
- `Constants`, `Message`, `NameMatching` — core helpers.

## Notable subpackages
- **asset** — asset pipeline integration (registry loading, load events).
- **auth** — authentication/session services.
- **blocktype** — block type definitions and helpers.
- **client** — client‑specific server logic.
- **codec** — server‑level codecs and protocol glue.
- **command** — command system and command manager.
- **console** — console sender / console input integration.
- **cosmetics** — cosmetics subsystem.
- **entity** — entity systems and entity management.
- **event** — server‑level events (e.g., `BootEvent`, `ShutdownEvent`).
- **inventory** — inventory subsystem.
- **io** — networking and server IO.
- **meta** — server metadata.
- **modules** — server modules (singleplayer, etc.).
- **permissions** — permission checks and utilities.
- **plugin** — plugin system (manager, class loader, base classes).
- **prefab** — prefab system integration.
- **receiver** — network receivers.
- **registry** — registries used by server subsystems.
- **task** — task scheduling/execution.
- **ui** — UI integration.
- **universe** — worlds, universe, player data, storage.
- **util** — core server utilities.
