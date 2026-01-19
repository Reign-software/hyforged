# Plugin Loading Flow

This describes how plugins are discovered, loaded, and started.

## Discovery & pending list
`PluginManager.setup()`:
- Registers a system command for plugin listing (`PluginCommand`).
- Builds a pending list from:
  - Core plugins registered by the server (`registerCorePlugin`).
  - Builtin directory next to the server jar (`.../builtin`).
  - Classpath plugins in the server jar.
  - External plugins from `mods/` and any additional `--mods` paths (unless `--bare`).
- Each plugin becomes a `PendingLoadPlugin` (Java or other type).

## Dependency validation & load order
- Validates dependencies and required versions.
- Computes load order via `PendingLoadPlugin.calculateLoadOrder(pending)`.
- Loads plugin classes into `PluginClassLoader` instances.
- Calls `PluginBase.preLoad()` for async config load if any.
- Calls `setup()` on each plugin (via `setup0`).

## Start phase
`PluginManager.start()`:
- Calls `start()` (via `start0`) for each plugin in load order.
- Verifies required mods from `HytaleServerConfig.ModConfig`.
- Sets state to `START` and promotes plugins to `ENABLED` on success.

## Lifecycle states (from `PluginBase`)
- `NONE` → `SETUP` → `START` → `ENABLED`
- On shutdown: `SHUTDOWN` → `DISABLED`
- Exceptions during `setup()` or `start()` downgrade to `DISABLED`.

## Java plugin specifics
`JavaPlugin.start0()`:
- If the manifest includes an asset pack (`includesAssetPack=true`), it registers the embedded pack with `AssetModule`.

## Plugin manifest fields
`PluginManifest` supports:
- `Group`, `Name`, `Version`, `Description`, `Authors`, `Website`
- `Main` (plugin entry class)
- `ServerVersion` constraints
- `Dependencies`, `OptionalDependencies`, `LoadBefore`
- `DisabledByDefault`, `IncludesAssetPack`

## What to override when writing plugins
- Extend `JavaPlugin`
- Override:
  - `setup()` for registration (events, commands, components)
  - `start()` for runtime start logic
  - `shutdown()` for cleanup
