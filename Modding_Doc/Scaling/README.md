# Monster Scaling API

This document describes how to configure monster level scaling in Hyforged.

## Overview

Monster scaling in Hyforged is fully data-driven with two components:

1. **World Scaling** - Defines how monster levels are calculated based on distance from world spawn
2. **Monster Scaling** - Defines which stats scale with level for each NPC type

## World Scaling Configuration

World scaling configs are loaded from `Server/Hyforged/Combat/WorldScaling/`.

### Schema

```json
{
  "Id": "hyforged:default-scaling",
  "Curve": "LINEAR",
  "BlocksPerLevel": 500,
  "MinLevel": 1,
  "MaxLevel": 100
}
```

### Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `Id` | string | required | Unique identifier for the config |
| `Curve` | enum | `LINEAR` | Level calculation curve type |
| `BlocksPerLevel` | int | `500` | Distance in blocks per monster level |
| `MinLevel` | int | `1` | Minimum monster level |
| `MaxLevel` | int | `100` | Maximum monster level cap |

### Curve Types

- **LINEAR**: `level = distance / blocksPerLevel + 1`
- **LOGARITHMIC**: `level = log(distance / blocksPerLevel + 1) * 10 + 1`
- **STEPPED**: `level = floor(distance / blocksPerLevel) + 1`

## Monster Scaling Configuration

Monster scaling configs are loaded from `Server/Hyforged/Combat/MonsterScaling/`.

### Schema

```json
{
  "Id": "hyforged:undead",
  "AppliesTo": [
    "Skeleton",
    "Zombie",
    "Ghoul",
    "Shadow_Knight"
  ],
  "ScaledStats": [
    {
      "StatId": "hyforged:max-health",
      "ModifierType": "INCREASED",
      "ScalePerLevel": 8
    },
    {
      "StatId": "hyforged:physical-damage-bps",
      "ModifierType": "INCREASED",
      "ScalePerLevel": 6
    },
    {
      "StatId": "hyforged:chaos-resistance-bps",
      "ModifierType": "FLAT",
      "ScalePerLevel": 100
    }
  ]
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `Id` | string | Unique identifier for the scaling config |
| `AppliesTo` | string[] | Array of NPC role names this config applies to |
| `ScaledStats` | object[] | Array of stat scaling definitions |

### ScaledStats Entry

| Field | Type | Description |
|-------|------|-------------|
| `StatId` | string | The stat ID to scale (e.g., `hyforged:max-health`) |
| `ModifierType` | enum | How the modifier stacks: `FLAT`, `INCREASED`, `MORE` |
| `ScalePerLevel` | int | Scaling value per level above minimum |

### Modifier Types

- **FLAT**: Adds `scalePerLevel` basis points directly per level
  - Example: `ScalePerLevel: 50` adds 50 bps armor per level
  
- **INCREASED**: Multiplies by `scalePerLevel` percent per level
  - Example: `ScalePerLevel: 10` gives +10% health per level
  - Internally converts to basis points: `10% = 1000 bps`

- **MORE**: Same as INCREASED but stacks multiplicatively
  - Use for powerful scaling that should compound

## Example Configurations

### Tank Monster

High health and armor, low damage scaling:

```json
{
  "Id": "mymod:tank-golem",
  "AppliesTo": ["Stone_Golem", "Iron_Guardian"],
  "ScaledStats": [
    { "StatId": "hyforged:max-health", "ModifierType": "INCREASED", "ScalePerLevel": 20 },
    { "StatId": "hyforged:armor-bps", "ModifierType": "FLAT", "ScalePerLevel": 150 },
    { "StatId": "hyforged:physical-damage-bps", "ModifierType": "INCREASED", "ScalePerLevel": 3 }
  ]
}
```

### Glass Cannon

High damage, low survivability:

```json
{
  "Id": "mymod:glass-cannon",
  "AppliesTo": ["Fire_Mage", "Shadow_Caster"],
  "ScaledStats": [
    { "StatId": "hyforged:max-health", "ModifierType": "INCREASED", "ScalePerLevel": 5 },
    { "StatId": "hyforged:fire-damage-bps", "ModifierType": "INCREASED", "ScalePerLevel": 15 }
  ]
}
```

### Custom Stats

You can scale any stat registered in the `StatDefinitionRegistry`, including custom stats from other mods:

```json
{
  "Id": "mymod:magical-creature",
  "AppliesTo": ["Arcane_Elemental"],
  "ScaledStats": [
    { "StatId": "mymod:arcane-power", "ModifierType": "INCREASED", "ScalePerLevel": 12 },
    { "StatId": "mymod:spell-resistance-bps", "ModifierType": "FLAT", "ScalePerLevel": 75 }
  ]
}
```

## Default Behavior

NPCs without a specific scaling configuration will use the default scaling, which provides:

- +10% max health per level (INCREASED)
- +5% physical damage per level (INCREASED)
- +50 bps armor per level (FLAT)

## API Access

From code, you can access the scaling service:

```java
import reign.software.hyforged.combat.scaling.MonsterScalingService;
import reign.software.hyforged.combat.scaling.ScaledStatEntry;

MonsterScalingService service = MonsterScalingService.get();

// Get scaling config for an NPC
List<ScaledStatEntry> stats = service.getScaledStats("Shadow_Knight");

// Check if NPC has specific config
boolean hasConfig = service.hasScalingConfig("Shadow_Knight");

// Calculate monster level at position
int level = service.calculateMonsterLevel(world, position);
```

## See Also

- [Stats API](../Stats/API.md) - Defining and using stats
- [Affix System](../Affixes/README.md) - Item stat modifiers
