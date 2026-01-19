# Server Startup Flow

This is the high‑level boot sequence as implemented in the decompiled server.

## Entry point
1. `com.hypixel.hytale.Main.main(String[] args)`
   - Sets locale and JVM properties.
   - Calls `EarlyPluginLoader.loadEarlyPlugins(args)`.
   - If early class transformers exist, calls `launchWithTransformingClassLoader(args)`; otherwise calls `LateMain.lateMain(args)`.

## Early plugin transforms
- `EarlyPluginLoader` scans `earlyplugins/` and any `--early-plugins` paths for jars and uses `ServiceLoader` to find `ClassTransformer` implementations.
- `TransformingClassLoader` wraps application class loading and applies transformers to class bytecode, skipping secure packages.
- If transformers exist, the server requires explicit user acceptance unless `--accept-early-plugins` is passed.

## Late bootstrap
1. `LateMain.lateMain(String[] args)`
   - Parses CLI options via `Options.parse`.
   - Initializes logging (`HytaleLogger.init`, `HytaleFileHandler.enable`, `HytaleLogger.replaceStd`).
   - Wires log level loader logic and creates `HytaleServer`.

## Core boot sequence
`HytaleServer` constructor:
- Initializes logging, config, authentication, Netty, Sentry (unless disabled), asset registry, core plugins, and GC hooks.
- Calls `boot()`.

`HytaleServer.boot()`:
1. **Setup phase**
   - `CommandManager.registerCommands()`
   - `PluginManager.setup()`
   - `ServerAuthManager.initializeCredentialStore()`
2. **Asset validation/load**
   - Dispatches `LoadAssetEvent` on the server `EventBus`.
   - If validation fails, triggers shutdown with `ShutdownReason.VALIDATE_ERROR`.
   - If `--shutdown-after-validate` is set, exits early after validation.
3. **Plugin start**
   - `PluginManager.start()`
4. **Universe ready**
   - Waits on `Universe.get().getUniverseReady()`
5. **Boot event + boot commands**
   - Dispatches `BootEvent`
   - Executes `--boot-command` list via `CommandManager`
6. **Finalization**
   - Logs boot banner, checks auth status, and signals singleplayer readiness.

## Shutdown path
`HytaleServer.shutdown0`:
- Dispatches `ShutdownEvent`
- Shuts down plugin manager, command manager, event bus, auth manager
- Saves config if changed
- Forces JVM shutdown if needed
