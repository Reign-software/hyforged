# Affix System

The Hyforged affix system adds ARPG-style affixes to equipment items. Affixes are magical properties that modify stats and enhance items with prefixes, suffixes, and unique forged effects.

## Overview

- **Data-driven**: All affixes, pools, and rules are defined in JSON files
- **Quality-based**: Item quality (Common → Legendary) determines affix capacity
- **Deterministic**: Affix rolling supports seeded randomness for reproducibility
- **Extensible**: Register custom affixes via JSON or the programmatic API

## Quick Start

1. Create an affix definition JSON in `Server/YourMod/Affixes/`
2. Add the affix to a pool in `Server/YourMod/AffixPools/`
3. Affixes will automatically roll on items matching the pool criteria

## Core Concepts

### Affix Types

Three types of affixes exist:

| Type | Position | Example |
|------|----------|---------|
| `prefix` | Before item name | **Sturdy** Iron Sword |
| `suffix` | After item name | Iron Sword **of the Bear** |
| `forged` | Hidden (stat only) | Special unique effects |

### Affix Tiers

Each affix has multiple tiers (T1 = best, T5 = worst):

- **Tier 1**: Highest values, requires high item level
- **Tier 5**: Lowest values, available early

Tier selection is weighted and considers item level requirements.

### Quality Capacity

Item quality determines how many affixes can roll:

| Quality | Prefixes | Suffixes | Forged |
|---------|----------|----------|--------|
| Common | 0 | 0 | 0 |
| Uncommon | 1 | 0 | 0 |
| Rare | 1 | 1 | 0 |
| Epic | 2 | 1 | 0 |
| Legendary | 2 | 2 | 1 |

## JSON Schemas

### Affix Definition

Place in `Server/YourMod/Affixes/*.json`:

```json
{
  "id": "yourmod:sharp",
  "type": "prefix",
  "displayName": "Sharp",
  "statId": "hyforged:physicalDamage",
  "modifierType": "FLAT",
  "weight": 1000,
  "tiers": [
    { "tier": 1, "minValue": 15, "maxValue": 20, "itemLevelRequirement": 40 },
    { "tier": 2, "minValue": 10, "maxValue": 14, "itemLevelRequirement": 25 },
    { "tier": 3, "minValue": 5, "maxValue": 9, "itemLevelRequirement": 10 }
  ],
  "eligibility": {
    "itemCategories": ["weapon"],
    "itemTags": ["melee"],
    "excludeTags": ["magic"],
    "minQuality": "Uncommon",
    "maxQuality": null
  }
}
```

**Fields:**
- `id`: Unique identifier (use `yourmod:` namespace)
- `type`: `prefix`, `suffix`, or `forged`
- `displayName`: Shown on tooltips
- `statId`: The stat this affix modifies
- `modifierType`: `FLAT`, `INCREASED`, or `MORE`
- `weight`: Selection weight (higher = more common)
- `tiers`: Array of tier definitions (see below)
- `eligibility`: Item filtering rules (all optional)

### Affix Tier Definition

```json
{
  "tier": 1,
  "minValue": 15,
  "maxValue": 20,
  "itemLevelRequirement": 40,
  "weight": 100
}
```

- `tier`: Tier number (1 = best)
- `minValue`/`maxValue`: Rolled stat value range
- `itemLevelRequirement`: Minimum item level to roll this tier
- `weight`: Optional tier selection weight (default: 100)

### Affix Pool

Place in `Server/YourMod/AffixPools/*.json`:

```json
{
  "id": "yourmod:swords",
  "weight": 100,
  "appliesTo": {
    "itemCategories": ["weapon"],
    "itemTags": ["sword"]
  },
  "prefixes": ["yourmod:sharp", "hyforged:sturdy"],
  "suffixes": ["hyforged:of-the-bear"],
  "forged": []
}
```

**Fields:**
- `id`: Unique pool identifier
- `weight`: Selection priority when multiple pools match
- `appliesTo`: Which items use this pool
- `prefixes`/`suffixes`/`forged`: Affix IDs available in this pool

### Quality Affix Rules

Override capacity per quality in `Server/YourMod/QualityAffixRules/*.json`:

```json
{
  "quality": "Mythic",
  "affixCapacity": {
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

AffixDefinitionRegistry registry = AffixDefinitionRegistry.get();

registry.register(new AffixDefinition(
    "yourmod:blazing",
    "prefix",
    "Blazing",
    StatId.of("yourmod", "fireDamage"),
    HyforgedModifier.StackType.FLAT,
    List.of(
        new AffixTierDefinition(1, 10, 15, 30),
        new AffixTierDefinition(2, 5, 9, 15)
    ),
    AffixEligibility.ANY,
    800
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
    System.out.println(affix.affixId() + " T" + affix.tier() + ": " + affix.value());
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

Affixes are automatically shown in item tooltips:

```
Sturdy Iron Chestplate of the Bear
+18 Armor (Tier 2)
+55 Health (Tier 1)
```

Forged affixes are displayed in a separate section with special formatting.

## Best Practices

1. **Use namespaced IDs** — Always prefix with `yourmod:` to prevent conflicts
2. **Balance tier weights** — Higher tiers should be rarer
3. **Set item level requirements** — Prevent T1 affixes on low-level items
4. **Use eligibility filters** — Don't put weapon affixes on armor
5. **Test with seeds** — Use deterministic rolling for testing

## See Also

- [Stats System](../Stats/README.md) — Understanding stat modifiers
- [API Reference](API.md) — Complete API documentation
