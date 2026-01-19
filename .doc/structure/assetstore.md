# Asset Store (com.hypixel.hytale.assetstore)

## Key classes
- `AssetStore` — central asset loading and lookup.
- `AssetRegistry` — registry of assets by type and key.
- `AssetPack` — container for asset sets (including plugin packs).
- `AssetMap` / `AssetReferences` — indexing and references.
- `DecodedAsset`, `JsonAsset`, `RawAsset` — asset representations.
- `AssetValidationResults` — validation output.
- `MissingAssetException` — missing asset error.

## Subpackages
- **codec** — asset codecs and schema.
- **event** — asset events.
- **iterator** — asset iteration utilities.
- **map** — specialized asset maps.

## Plugin integration
`JavaPlugin` can auto‑register an embedded asset pack when the manifest sets `IncludesAssetPack`.
