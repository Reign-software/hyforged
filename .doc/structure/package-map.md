# Top‑Level Package Map

Package roots under `com.hypixel.hytale`:

- **assetstore** — asset registry and pack handling (`AssetStore`, `AssetRegistry`, `AssetPack`).
- **builtin** — built‑in gameplay systems and modules (many subfolders by feature).
- **codec** — serialization codecs, schema, builders, validation.
- **common** — shared utilities (collections, semver, threading, plugin metadata, etc.).
- **component** — ECS‑style component model (components, registries, systems, queries).
- **event** — event bus infrastructure and registrations.
- **function** — functional helpers and utilities.
- **logger** — logging backend + Sentry integration.
- **math** — math types/utilities.
- **metrics** — metrics registry + JVM metrics.
- **plugin** — early plugin loader and class transformers.
- **procedurallib** — procedural generation helpers.
- **protocol** — network protocol model and packet registry.
- **registry** — generic registry pattern.
- **server** — server core and subsystems (plugins, auth, commands, world, etc.).
- **sneakythrow** — low‑level exception utilities.
- **storage** — storage helpers/abstractions.
- **unsafe** — internal unsafe/low‑level utilities.
