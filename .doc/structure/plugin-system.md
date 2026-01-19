# Plugin System Structure

## common.plugin
- `PluginManifest` — plugin metadata and dependency graph.
- `PluginIdentifier` — group/name identifier.
- `AuthorInfo` — author metadata.

## server.core.plugin
- `PluginManager` — discovery, load order, lifecycle control.
- `PluginBase` — lifecycle base class with registries and configs.
- `JavaPlugin` — Java plugin specialization.
- `PluginClassLoader` — class loading and isolation.
- `PluginState` / `PluginType` — lifecycle state and plugin type.
- `commands`, `event`, `pending`, `registry` — internal plugin modules.

## plugin.early
- `EarlyPluginLoader` — loads early transformers from `earlyplugins/`.
- `TransformingClassLoader` — transforms bytecode on load (unsafe).
- `ClassTransformer` — transform interface loaded via `ServiceLoader`.

## Plugin‑side registries (from `PluginBase`)
- `CommandRegistry`
- `EventRegistry`
- `TaskRegistry`
- `ClientFeatureRegistry`
- `BlockStateRegistry`
- `EntityRegistry`
- `ComponentRegistryProxy` (EntityStore + ChunkStore)
- `AssetRegistry` (plugin‑local)
- Codec map registries via `getCodecRegistry(...)`
