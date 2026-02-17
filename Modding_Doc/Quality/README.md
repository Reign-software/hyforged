# Hyforged Quality System

The Quality System provides data-driven item quality rolling for equipment drops and NPC quality assignment with loot bonuses.

## Overview

Hyforged extends Hytale's base item quality system with:

- **Random quality rolling** for eligible items at spawn time
- **Weight profiles** controlling quality tier distribution
- **Eligibility rules** determining which items receive quality
- **Modifiers** adjusting weights based on source level, Magic Find, and NPC quality
- **NPC quality** with stat scaling and loot bonuses

> **Known Limitation:** Hytale's vanilla UI displays the base item quality from the asset. Hyforged overrides the effective quality in metadata, which is used by all Hyforged systems. Full UI integration requires client-side modding.

## Quick Reference

| Task | Location |
|------|----------|
| Configure quality weights | `Server/Hyforged/Quality/Weights/*.json` |
| Configure eligibility rules | `Server/Hyforged/Quality/Eligibility/*.json` |
| Configure modifiers | `Server/Hyforged/Quality/Modifiers/*.json` |
| Configure NPC quality | `Server/Hyforged/Quality/NPCRules/*.json` |

## Quality Weight Profiles

Weight profiles define the probability distribution for rolling quality tiers.

**Location:** `src/main/resources/Server/Hyforged/Quality/Weights/<ProfileName>.json`

### Schema

```json
{
  "Id": "default",
  "Description": "Global default quality weights for equipment",
  "Weights": {
    "Common": 500,
    "Uncommon": 300,
    "Rare": 150,
    "Epic": 40,
    "Legendary": 10
  },
  "EligibleQualities": [
    "Common",
    "Uncommon",
    "Rare",
    "Epic",
    "Legendary"
  ]
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `Id` | string | Unique identifier (referenced by eligibility rules) |
| `Description` | string | Human-readable description |
| `Weights` | map | Quality ID → weight value (higher = more common) |
| `EligibleQualities` | array | Allowed quality IDs (empty = use all from Weights) |

### Weight Calculation

Total weight = sum of all weights. Probability of each quality = weight / total.

**Example:** With weights `Common: 500, Rare: 100` (total 600):
- Common: 500/600 = 83.3%
- Rare: 100/600 = 16.7%

## Eligibility Rules

Eligibility rules determine which items receive random quality and which weight profile to use.

**Location:** `src/main/resources/Server/Hyforged/Quality/Eligibility/<RuleName>.json`

### Schema

```json
{
  "Id": "default-weapons",
  "Priority": 100,
  "Description": "All weapons receive quality",
  "WeightProfileId": "default",
  "AppliesTo": {
    "Categories": ["Items.Weapon"],
    "Tags": [],
    "ItemIds": []
  },
  "Excludes": {
    "Tags": ["NoQuality"],
    "ItemIds": ["hytale:debug_*"]
  },
  "SourceFilter": {
    "SourceTags": [],
    "ExcludeSourceTags": []
  },
  "ModifierOverrides": {}
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `Id` | string | Unique identifier |
| `Priority` | int | Higher priority rules are checked first |
| `WeightProfileId` | string | Which weight profile to use |
| `AppliesTo.Categories` | array | Item must match at least one category |
| `AppliesTo.Tags` | array | Item must have at least one tag |
| `AppliesTo.ItemIds` | array | Item ID patterns (supports `*` wildcard) |
| `Excludes.Tags` | array | Items with these tags are excluded |
| `Excludes.ItemIds` | array | Item ID patterns to exclude |
| `SourceFilter.SourceTags` | array | Source must have at least one tag |
| `SourceFilter.ExcludeSourceTags` | array | Sources with these tags are excluded |
| `ModifierOverrides` | object | Override default modifier settings |

### Priority Resolution

Rules are checked in descending priority order. The first matching rule is used.

## Quality Modifiers

Modifiers adjust quality weights based on contextual factors.

**Location:** `src/main/resources/Server/Hyforged/Quality/Modifiers/<ModifierName>.json`

### Schema

```json
{
  "Description": "Global default quality modifiers",
  "LevelScaling": {
    "Enabled": true,
    "CurveId": "Linear",
    "QualityBonusPerLevel": {
      "Rare": 0.3,
      "Epic": 0.1,
      "Legendary": 0.02
    }
  },
  "ItemRarity": {
    "Enabled": true,
    "StatId": "hyforged:item-rarity-increased-bps",
    "ScalingFactor": 0.01,
    "MaxBonus": 200,
    "FallbackValue": 0
  },
  "NpcQualityBonus": {
    "Enabled": true,
    "BonusPerTier": {
      "Uncommon": 10,
      "Rare": 25,
      "Epic": 50,
      "Legendary": 100
    }
  }
}
```

### Level Scaling

Adds weight bonuses to higher-tier qualities based on source level.

- `CurveId`: Response curve for level-to-multiplier mapping
- `QualityBonusPerLevel`: Bonus per level for each quality tier

### Item Rarity (Magic Find)

Increases chances of higher-quality drops based on player's Magic Find stat.

- `StatId`: Stat to read from player
- `ScalingFactor`: Multiplier for stat value
- `MaxBonus`: Maximum total bonus (distributed to higher tiers only)

### NPC Quality Bonus

Applies weight bonuses when the loot source is a quality NPC.

- `BonusPerTier`: Additional weight per quality tier when NPC has that tier or higher

## NPC Quality Configuration

NPCs can spawn with quality tiers that affect their stats and loot.

**Location:** `src/main/resources/Server/Hyforged/Quality/NPCRules/<RuleName>.json`

### Schema

```json
{
  "Id": "default-spawn",
  "Priority": 100,
  "Description": "Default NPC quality for spawned entities",
  "WeightProfileId": "default",
  "AppliesTo": {
    "EntityTypes": [],
    "Tags": ["role:monster"],
    "Roles": []
  },
  "Excludes": {
    "Tags": ["no-quality"],
    "EntityTypes": []
  },
  "StatScaling": {
    "Uncommon": { "hyforged:max-health": 1.1, "hyforged:damage": 1.05 },
    "Rare": { "hyforged:max-health": 1.25, "hyforged:damage": 1.15 },
    "Epic": { "hyforged:max-health": 1.5, "hyforged:damage": 1.3 },
    "Legendary": { "hyforged:max-health": 2.0, "hyforged:damage": 1.5 }
  }
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `StatScaling` | map | Quality → stat ID → multiplier |

## Programmatic API

### Reading Effective Quality

```java
import reign.software.hyforged.quality.service.HyforgedQualityService;

// Get effective quality (Hyforged override or base)
String qualityId = HyforgedQualityService.getEffectiveQuality(itemStack);
```

### Rolling Quality Manually

```java
import reign.software.hyforged.quality.service.QualityRollerService;
import reign.software.hyforged.quality.model.QualityRollContext;

QualityRollerService roller = new QualityRollerService();
QualityRollContext context = QualityRollContext.of(
    itemStack.getItemId(),
    itemCategories,
    itemTags
);
String quality = roller.rollQuality(context);
```

### Events

| Event | Description |
|-------|-------------|
| `QualityRolledEvent` | Fired before quality is applied; cancellable |
| `NPCQualityAssignedEvent` | Fired when NPC receives quality; cancellable |

## Integration with Affix System

The quality system integrates with the affix system:

1. `LootQualitySystem` runs **before** `LootAffixSystem`
2. Quality is determined first, then affixes are rolled
3. Higher quality items may have access to better affix pools
4. NPC quality can include affixes via `HyforgedNPCQualityComponent`

## Best Practices

1. **Use meaningful weight values**: Base common items around 500, scale down for rarer tiers
2. **Set appropriate priorities**: Higher priority rules for specific items, lower for catch-all rules
3. **Test weight distributions**: Use the debug command to verify roll distributions
4. **Consider Magic Find scaling**: The `MaxBonus` caps total bonus to prevent excessive skewing

## Debug Commands

```
/hyforged quality roll <itemId> [sourceLevel] [magicFind]
/hyforged quality info <itemId>
/hyforged npc quality <entityId>
```
