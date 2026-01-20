# Hyforged Modding Documentation

Welcome to the Hyforged modding documentation. This folder contains guides for extending Hyforged with your own content.

## Available Guides

| Guide | Description |
|-------|-------------|
| [Affix System](Affixes/README.md) | Add ARPG-style affixes (prefixes, suffixes, forged) to equipment |
| [Stats System](Stats/README.md) | Add custom stats, modifiers, and integrate with the ARPG stat framework |
| [Progression System](Progression/README.md) | XP, leveling, classes, and progression configuration |

## Getting Started

Hyforged is a Hytale plugin that adds RPG mechanics. When creating mods that extend Hyforged:

1. **Use namespaced IDs** — All IDs follow the `namespace:name` format to prevent conflicts
2. **Place assets in the right folders** — Server-side data goes in `Server/<YourMod>/`
3. **Follow ECS patterns** — Hyforged uses an Entity Component System architecture

## Folder Structure for Your Mod

```
YourMod/
├── src/main/java/                    # Java code (optional)
│   └── com/yourname/yourmod/
├── src/main/resources/
│   ├── manifest.json                 # Mod manifest
│   └── Server/
│       └── YourMod/
│           └── Stats/                # Custom stat definitions
│               └── CustomStat.json
└── build.gradle
```
