# Affix System

The Hyforged affix system adds ARPG-style affixes to equipment items. Affixes are magical properties that modify stats and enhance items with prefixes, suffixes, and unique forged effects.

## Overview

- **Data-driven**: All affixes, pools, and rules are defined in JSON files
- **Multi-stat support**: Each tier can grant multiple stats with independent value ranges
- **Quality-based**: Item quality (Common → Legendary) determines affix capacity
- **Pool-based targeting**: Affix pools determine which affixes appear on which items
- **Deterministic**: Affix rolling supports seeded randomness for reproducibility

## Quick Start

1. Create an affix definition JSON in the appropriate subfolder:
   - `Server/YourMod/Affixes/Prefix/` - for prefix affixes
   - `Server/YourMod/Affixes/Suffix/` - for suffix affixes  
   - `Server/YourMod/Affixes/Forged/` - for forged (corrupted) affixes
2. Add the affix ID to a pool in `Server/YourMod/AffixPools/`
3. Affixes will automatically roll on items matching the pool criteria

> **Note:** The asset loader recursively scans subfolders, so organizing by affix type keeps things clean.

## Core Concepts

### Affix Types

Three types of affixes exist:

| Type | Category | Description |
|------|----------|-------------|
| `prefix` | Regular | Traditional prefix affixes (defensive stats, utility) |
| `suffix` | Regular | Traditional suffix affixes (offensive stats, "of the X") |
| `forged` | Special | Unique corrupted effects (hidden from regular tooltip, separate section) |

> **Display Note:** Affixes are displayed in the item's tooltip using Path of Exile style, 
> showing the rolled value and its possible range. Example:
> ```
> [T1] +75 Health (50-100)
> [T2] +12% Movement Speed (10%-15%)
> ```
> Item names are NOT modified by affixes (no "Sturdy Iron Sword of the Bear" pattern).

### Affix Tiers

Each affix has multiple tiers (T1 = best, T5 = worst):

- **Tier 1**: Highest values, requires high item level
- **Tier 5**: Lowest values, available early

Each tier defines its own stats with independent value ranges, allowing:
- Different stats per tier
- Different stack types per stat
- Independent min/max ranges for each stat

Tier selection is weighted and considers item level requirements.

### Multi-Stat Affixes

Each tier can grant multiple stats. For example, "of the Titan" might grant:
- +45-55 Strength (FLAT)
- +100-150 Max Health (FLAT)

Both stats are rolled independently when the affix is applied.

### Quality Capacity

Item quality determines how many affixes can roll:

| Quality | Prefixes | Suffixes | Forged |
|---------|----------|----------|--------|
| Common | 1 | 0 | 0 |
| Uncommon | 1 | 1 | 0 |
| Rare | 2 | 2 | 0 |
| Epic | 3 | 3 | 0 |
| Legendary | 4 | 4 | 0 |

> **Note:** Hyforged can override item quality via metadata for rolls and systems. The default Hytale UI
> still displays the base item quality from the asset, so visuals may not reflect the effective quality.

### Pool-Based Targeting

Affixes themselves don't define what items they can appear on. Instead, **affix pools** control targeting:

- Pools specify which item categories/tags they apply to
- Pools list which affix IDs are available
- Multiple pools can apply to the same item
- This allows the same affix to appear in different pools with different targeting

## JSON Schemas

### Affix Definition

Place in the appropriate subfolder under `Server/YourMod/Affixes/`:
- `Prefix/` for prefix affixes (adjectives before item name)
- `Suffix/` for suffix affixes ("of the X" patterns after item name)
- `Forged/` for forged affixes (corrupted, hidden, best-tier)

#### Single-Stat Affix Example

```json
{
  "Id": "hyforged:sturdy",
  "Type": "prefix",
  "DisplayName": "Sturdy",
  "Weight": 100,
  "Tiers": [
    {
      "Tier": 1,
      "ItemLevelReq": 40,
      "Weight": 50,
      "Stats": {
        "hyforged:armor": { "MinValue": 50, "MaxValue": 75, "StackType": "FLAT" }
      }
    },
    {
      "Tier": 2,
      "ItemLevelReq": 25,
      "Weight": 75,
      "Stats": {
        "hyforged:armor": { "MinValue": 35, "MaxValue": 50, "StackType": "FLAT" }
      }
    },
    {
      "Tier": 3,
      "ItemLevelReq": 10,
      "Weight": 100,
      "Stats": {
        "hyforged:armor": { "MinValue": 20, "MaxValue": 35, "StackType": "FLAT" }
      }
    }
  ]
}
```

#### Multi-Stat Affix Example

```json
{
  "Id": "hyforged:of-the-titan",
  "Type": "suffix",
  "DisplayName": "of the Titan",
  "Weight": 80,
  "Tiers": [
    {
      "Tier": 1,
      "ItemLevelReq": 70,
      "Weight": 35,
      "Stats": {
        "hyforged:strength": { "MinValue": 45, "MaxValue": 55, "StackType": "FLAT" },
        "hyforged:max-health": { "MinValue": 100, "MaxValue": 150, "StackType": "FLAT" }
      }
    },
    {
      "Tier": 2,
      "ItemLevelReq": 50,
      "Weight": 50,
      "Stats": {
        "hyforged:strength": { "MinValue": 30, "MaxValue": 40, "StackType": "FLAT" },
        "hyforged:max-health": { "MinValue": 60, "MaxValue": 90, "StackType": "FLAT" }
      }
    }
  ]
}
```

**Top-Level Fields:**
- `Id`: Unique identifier (use `yourmod:` namespace)
- `Type`: `prefix`, `suffix`, or `forged`
- `DisplayName`: Shown on tooltips
- `Weight`: Selection weight (higher = more common)
- `Tiers`: Array of tier definitions

**Tier Fields:**
- `Tier`: Tier number (1 = best)
- `ItemLevelReq`: Minimum item level to roll this tier
- `Weight`: Optional tier selection weight (default computed from tier number)
- `Stats`: Map of stat IDs to stat definitions

**Stat Fields:**
- `MinValue`/`MaxValue`: Rolled value range for this stat
- `StackType`: `FLAT`, `INCREASED`, or `MORE`

### Stat StackTypes

| StackType | Formula | Use Case |
|-----------|---------|----------|
| `FLAT` | base + value | Flat bonuses (+50 Armor) |
| `INCREASED` | base * (1 + sum%) | Additive % (+15% Damage) |
| `MORE` | base * (1 + value%) | Multiplicative % (20% MORE Damage) |

Values use basis points: 10000 = 100%, so +15% = 1500.

### Affix Pool

Place in `Server/YourMod/AffixPools/*.json`:

```json
{
  "Priority": 100,
  "AppliesTo": {
    "Categories": ["Items.Armor"],
    "Tags": ["Type:Armor"]
  },
  "Prefixes": ["Sturdy", "Armored"],
  "Suffixes": ["OfTheBear", "OfVitality"],
  "Forged": []
}
```

**Fields:**
- `Priority`: Selection priority when multiple pools match (higher = preferred)
- `AppliesTo`: Which items use this pool
  - `Categories`: Item category IDs
  - `Tags`: Item tags
- `Prefixes`/`Suffixes`/`Forged`: Affix IDs available in this pool

### Quality Affix Rules

Override capacity per quality in `Server/YourMod/QualityAffixRules/*.json`:

```json
{
  "Quality": "Mythic",
  "AffixCapacity": {
    "prefix": 3,
    "suffix": 3,
    "forged": 2
  }
}
```

## Programmatic API

### Rolling Affixes

```java
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.service.AffixRollContext;
import reign.software.hyforged.affix.service.AffixRollResult;

AffixService service = AffixService.get();

// Create roll context
AffixRollContext context = AffixRollContext.of(
    "Items.Armor.ChestPlate",    // Item ID
    "Rare",                       // Quality
    25,                           // Item level
    new String[]{"armor"},        // Categories
    new String[]{"heavy"}         // Tags
);

// Roll with random seed
AffixRollResult result = service.rollAffixes(context);

// Or with deterministic seed
AffixRollResult result = service.rollAffixes(context, 12345L);
```

### Querying Affixes

```java
// Get affix definition
AffixDefinition def = service.getAffixDefinition("hyforged:sturdy");

// Get all affix IDs
Set<String> allAffixes = service.getAllAffixIds();

// Get affixes by type
List<AffixDefinition> prefixes = service.getAffixesByType("prefix");
```

### Registering Custom Affixes

```java
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.model.AffixTierStat;

AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();

// Create tier with multiple stats
Map<String, AffixTierStat> tier1Stats = Map.of(
    "hyforged:strength", new AffixTierStat(
        StatId.hyforged("strength"), 
        StackType.FLAT, 
        45, 55
    ),
    "hyforged:max-health", new AffixTierStat(
        StatId.hyforged("max-health"), 
        StackType.FLAT, 
        100, 150
    )
);

registry.register(new AffixDefinition(
    "yourmod:of-the-titan",
    "suffix",
    "of the Titan",
    List.of(
        new AffixTierDefinition(1, 70, 35, tier1Stats)
    ),
    80
));
```

### Reading Item Affixes

```java
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.item.HyforgedItemData;

// From ItemStack
HyforgedItemData data = HyforgedItemData.from(itemStack);
List<RolledAffix> affixes = data.affixes();

for (RolledAffix affix : affixes) {
    System.out.println(affix.affixId() + " T" + affix.tier());
    
    // Multi-stat support
    for (var entry : affix.rolledStats().entrySet()) {
        String statId = entry.getKey();
        RolledAffix.RolledStat stat = entry.getValue();
        System.out.println("  " + statId + ": " + stat.value() + " (" + stat.stackType() + ")");
    }
}
```

## Debug Commands

Available in-game commands (requires op):

| Command | Description |
|---------|-------------|
| `/hyforged affixes` | Dump affixes on all equipped items |
| `/hyforged rollaffix [seed]` | Roll affixes on held item |
| `/hyforged giveaffix <id> <tier>` | Add specific affix to held item |
| `/hyforged affixmetrics` | View system metrics |

## Tooltip Display

Hyforged writes affix tooltip payloads into item metadata so tooltip-capable UI code can display rolled affixes:

- `Hyforged.AffixTooltipLines` - string array of tooltip lines
- `Hyforged.AffixTooltipSummary` - newline-joined summary string

The Hyforged item panel also appends a `Hyforged Stats` section that shows aggregated stat totals from all rolled affixes (for example, total `+Health`, `+Damage`, `+Crit`, etc.).

Example rendered content:

```
Sturdy Iron Chestplate of the Bear
+18 Armor (Tier 2)
+55 Health (Tier 1)
+12 Strength (Tier 1)
```

Forged affixes are displayed in a separate section with special formatting.

Note: vanilla Hytale tooltip rendering remains client-driven. Full native tooltip integration still requires client-side UI modding to read these metadata keys.

## Best Practices

1. **Use namespaced IDs** — Always prefix with `yourmod:` to prevent conflicts
2. **Balance tier weights** — Higher tiers should be rarer
3. **Set item level requirements** — Prevent T1 affixes on low-level items
4. **Use pools for targeting** — Create pools for different item categories
5. **Test with seeds** — Use deterministic rolling for testing
6. **One affix, one pool minimum** — Every affix needs to be in at least one pool to appear

## See Also

- [Stats System](../Stats/README.md) — Understanding stat modifiers
- [API Reference](API.md) — Complete API documentation
