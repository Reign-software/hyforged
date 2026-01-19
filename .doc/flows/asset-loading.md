# Asset Loading & Validation Flow

## Boot‑time asset pipeline
During `HytaleServer` startup:
1. `AssetRegistryLoader.init()` initializes core asset registries.
2. `LoadAssetEvent` is dispatched on the server `EventBus`.
3. Asset validation failure triggers shutdown with `ShutdownReason.VALIDATE_ERROR`.
4. If `--shutdown-after-validate` is passed, the server exits after validation.

## Plugin interaction
- `JavaPlugin` can embed asset packs via the manifest (`IncludesAssetPack`).
- On plugin start, embedded packs are registered with `AssetModule`.

## Related CLI options (see `Options`)
- `--assets` (asset directory)
- `--prefab-cache` (prefab cache)
- `--validate-assets`, `--validate-prefabs`, `--validate-world-gen`
- `--shutdown-after-validate`

## Relevant classes
- `com.hypixel.hytale.server.core.asset.AssetRegistryLoader`
- `com.hypixel.hytale.server.core.asset.LoadAssetEvent`
- `com.hypixel.hytale.assetstore.*`
