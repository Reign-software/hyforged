# Plugin Developer Cheatsheet

## Lifecycle
Implement a plugin by extending `JavaPlugin` and overriding:
- `setup()` — register commands, events, components
- `start()` — start runtime behavior
- `shutdown()` — cleanup

State transitions (from `PluginBase`):
- `NONE` → `SETUP` → `START` → `ENABLED`
- `SHUTDOWN` → `DISABLED`

## Registries available from `PluginBase`
- `getCommandRegistry()`
- `getEventRegistry()`
- `getTaskRegistry()`
- `getClientFeatureRegistry()`
- `getBlockStateRegistry()`
- `getEntityRegistry()`
- `getEntityStoreRegistry()` (ECS)
- `getChunkStoreRegistry()` (ECS)
- `getAssetRegistry()`
- `getCodecRegistry(...)` (codec map registries)

## Plugin config
- `withConfig(...)` must be called before `setup()`.
- `preLoad()` loads configs asynchronously prior to setup.

## Plugin manifest fields
From `PluginManifest`:
- `Group`, `Name`, `Version`, `Description`
- `Authors`, `Website`, `Main`
- `ServerVersion`, `Dependencies`, `OptionalDependencies`, `LoadBefore`
- `DisabledByDefault`, `IncludesAssetPack`

## Asset packs
- If `IncludesAssetPack=true`, `JavaPlugin` registers the embedded pack on start.

## Useful server services
- `HytaleServer.get()` for core services.
- `HytaleServer.getEventBus()` for global events (via plugin `EventRegistry`).
- `CommandManager` for server‑level commands (use via plugin `CommandRegistry`).
