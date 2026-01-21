# Hytale Server Decompile Notes

This folder is a working index of the decompiled Hytale server sources in `patcher/hytale-server`. It is intentionally high‑level and organized for plugin development workflows: navigation, lifecycle understanding, and API discovery.

## Start here
- [flows/server-startup.md](flows/server-startup.md)
- [flows/plugin-loading.md](flows/plugin-loading.md)
- [flows/event-system.md](flows/event-system.md)
- [flows/asset-loading.md](flows/asset-loading.md)
- [references/plugin-dev-cheatsheet.md](references/plugin-dev-cheatsheet.md)
- [structure/package-map.md](structure/package-map.md)

## Structure guides
- [structure/server-core.md](structure/server-core.md)
- [structure/plugin-system.md](structure/plugin-system.md)
- [structure/component-ecs.md](structure/component-ecs.md)
- [structure/assetstore.md](structure/assetstore.md)
- [structure/common.md](structure/common.md)
- [structure/builtin-modules.md](structure/builtin-modules.md)
- [structure/protocol.md](structure/protocol.md)
- [structure/protocol-notes.md](structure/protocol-notes.md)
- [structure/logger.md](structure/logger.md)
- [structure/config.md](structure/config.md)
- [structure/registry.md](structure/registry.md)
- [structure/misc-packages.md](structure/misc-packages.md)

## Reference guides
- [references/key-classes.md](references/key-classes.md)
- [references/plugin-dev-cheatsheet.md](references/plugin-dev-cheatsheet.md)
- [references/ui-modding.md](references/ui-modding.md)

## External guides (summaries)
- [external/README.md](external/README.md)

## Goals
- Provide quick, accurate entry points to large subsystems.
- Document startup and plugin lifecycles.
- Create a map of packages and key classes for fast search.

## Notes
- This is decompiled code. Names and patterns are accurate to the current jar but may shift between versions.
- When unsure, prefer source inspection in `patcher/hytale-server/src/main/java`.

- IMetaStore.putMetaObject
- EntityStatValue
- EntityStatMap
- DefaultEntityStatTypes
- EntityStatType