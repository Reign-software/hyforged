# Hyforged Affix System API

The Hyforged Affix System provides an ARPG-style affix system for equipment items. This document describes the public API for querying, creating, and extending affixes.

## Getting Started

The main entry point is `AffixService`:

```java
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.api.AffixSpec;
import reign.software.hyforged.affix.model.RolledAffix;

// Get the service instance
AffixService service = AffixService.get();
```

## Querying Affixes

### Get Affixes on an Item

```java
ItemStack item = /* your item */;

// Get all affixes
List<RolledAffix> affixes = service.getAffixes(item);

// Check if item has affixes
boolean hasAffixes = service.hasAffixes(item);

// Get full item data
HyforgedItemData data = service.getItemData(item);
```

### Query Affix Definitions

```java
// Get a specific affix definition
AffixDefinition sturdy = service.getAffixDefinition("sturdy");

// Get an affix type
AffixType prefixType = service.getAffixType("prefix");

// Get all registered IDs
Set<String> affixIds = service.getAllAffixIds();
Set<String> typeIds = service.getAllTypeIds();
Set<String> poolIds = service.getAllPoolIds();
```

## Rolling Affixes

### Random Rolling

```java
ItemStack item = new ItemStack("Items.Weapons.Sword", 1);

// Roll with random seed
ItemStack itemWithAffixes = service.rollAffixes(item);

// Roll with specific seed (deterministic)
ItemStack deterministicItem = service.rollAffixes(item, 12345L);
```

### Rolling Factors

Affix rolling considers:
- **Quality**: Determines affix capacity (Common=1 prefix, Legendary=4 prefix + 4 suffix)
- **Item Level**: Determines which affix tiers are eligible
- **Item Categories/Tags**: Determines which affix pool is used

## Creating Items with Specific Affixes

Use `AffixSpec` to specify exact affixes:

```java
import reign.software.hyforged.affix.api.AffixSpec;

// Create item with specific affixes
ItemStack craftedItem = service.createWithAffixes(
    "Items.Weapons.Sword",
    List.of(
        AffixSpec.of("sturdy", 2, 35),     // Tier 2 Sturdy with value 35
        AffixSpec.of("of-the-bear", 1),    // Tier 1 Of the Bear, random value
        AffixSpec.of("mighty")              // Random tier and value
    )
);
```

### AffixSpec Options

```java
// Full specification: affix ID, tier, and exact value
AffixSpec.of("sturdy", 2, 35)

// Tier specified, value will be rolled within tier range
AffixSpec.of("sturdy", 2)

// Affix ID only, tier and value will be rolled
AffixSpec.of("sturdy")
```

## Modifying Existing Items

```java
// Add an affix to an existing item
ItemStack updated = service.addAffix(item, AffixSpec.of("sturdy", 1, 60));

// Remove an affix by ID
ItemStack withoutSturdy = service.removeAffix(item, "sturdy");

// Clear all affixes
ItemStack clean = service.clearAffixes(item);
```

## Registering Custom Affixes

Plugins can register custom affixes at runtime:

### Register an Affix Definition

```java
AffixDefinition customAffix = new AffixDefinition(
    "vampiric",                              // ID
    "prefix",                                 // Type
    "Vampiric",                               // Display name
    StatId.hyforged("lifesteal"),            // Stat to modify
    HyforgedModifier.StackType.FLAT,         // Modifier type
    List.of(
        new AffixTierDefinition(1, 8, 10, 40),  // T1: 8-10%, requires item level 40
        new AffixTierDefinition(2, 5, 7, 20),   // T2: 5-7%, requires item level 20
        new AffixTierDefinition(3, 2, 4, 1)     // T3: 2-4%, requires item level 1
    ),
    AffixEligibility.ANY,                     // Can appear on any equipment
    100                                        // Selection weight
);

service.registerAffix(customAffix);
```

### Register an Affix Pool

```java
AffixPool weaponPool = new AffixPool(
    "my-weapons",                    // Pool ID
    new String[]{"Items.Weapons"},   // Item categories
    new String[]{"melee"},           // Item tags
    List.of("vampiric", "sharp"),    // Prefix affix IDs
    List.of("of-slaying"),           // Suffix affix IDs
    List.of(),                        // Forged affix IDs
    150                               // Priority (higher = preferred)
);

service.registerPool(weaponPool);
```

### Register an Affix Type

```java
AffixType enchantType = new AffixType(
    "enchant",                                  // Type ID
    AffixType.DisplayNamePosition.NONE,         // No name modification
    "Enchanted: {name}",                        // Tooltip format
    false                                        // Not stackable (one per item)
);

service.registerType(enchantType);
```

## Data Structures

### RolledAffix

Represents an affix that has been rolled on an item:

```java
public record RolledAffix(
    String affixId,         // Reference to AffixDefinition
    String type,            // "prefix", "suffix", or "forged"
    int tier,               // 1 = best, higher = weaker
    int value,              // Rolled value within tier range
    StatId statId,          // Stat being modified
    HyforgedModifier.StackType modifierType  // FLAT, INCREASED, MORE
) {}
```

### AffixDefinition

Defines an affix's properties and tier structure:

```java
public record AffixDefinition(
    String id,              // Unique identifier
    String type,            // Type reference
    String displayName,     // Localization key
    StatId statId,          // Target stat
    HyforgedModifier.StackType modifierType,
    List<AffixTierDefinition> tiers,
    AffixEligibility eligibility,
    int weight              // Selection weight for rolling
) {}
```

### AffixTierDefinition

Defines a single tier within an affix:

```java
public record AffixTierDefinition(
    int tier,           // Tier number (1 = best)
    int minValue,       // Minimum rolled value
    int maxValue,       // Maximum rolled value
    int itemLevelReq    // Required item level to roll this tier
) {}
```

## Events

The affix system emits events for extensibility:

### AffixesRolledEvent

Fired after affixes are rolled on an item:

```java
HytaleServer.getEventBus().subscribe(AffixesRolledEvent.class, event -> {
    ItemStack item = event.getItem();
    List<RolledAffix> affixes = event.getAffixes();
    // Handle the event
});
```

### AffixModifiersAppliedEvent

Fired when affix modifiers are applied to an entity's stats:

```java
HytaleServer.getEventBus().subscribe(AffixModifiersAppliedEvent.class, event -> {
    Ref<EntityStore> entity = event.getEntity();
    List<StatModifier> modifiers = event.getModifiers();
    // Handle the event
});
```

## JSON Configuration

Affixes can be defined in JSON files under `Server/Hyforged/`:

### Affix Definition (`Server/Hyforged/Affixes/Sturdy.json`)

```json
{
  "Id": "sturdy",
  "Type": "prefix",
  "DisplayName": "Sturdy",
  "StatId": "hyforged:armor",
  "ModifierType": "FLAT",
  "Tiers": [
    {"Tier": 1, "MinValue": 50, "MaxValue": 75, "ItemLevelReq": 40},
    {"Tier": 2, "MinValue": 30, "MaxValue": 49, "ItemLevelReq": 20},
    {"Tier": 3, "MinValue": 15, "MaxValue": 29, "ItemLevelReq": 1}
  ],
  "Eligibility": {
    "ItemCategories": ["Items.Armor"],
    "ItemTags": []
  },
  "Weight": 100
}
```

### Affix Type (`Server/Hyforged/AffixTypes/Prefix.json`)

```json
{
  "Id": "prefix",
  "DisplayNamePosition": "before",
  "DisplayFormat": "{name}",
  "Stackable": true
}
```

### Quality Rules (`Server/Hyforged/QualityAffixRules/Rare.json`)

```json
{
  "Quality": "Rare",
  "AffixCapacity": {
    "prefix": 2,
    "suffix": 2,
    "forged": 0
  }
}
```

### Affix Pool (`Server/Hyforged/AffixPools/Armor.json`)

```json
{
  "Id": "armor",
  "AppliesTo": {
    "Categories": ["Items.Armor"],
    "Tags": []
  },
  "Prefixes": ["sturdy", "reinforced"],
  "Suffixes": ["of-the-bear", "of-fortitude"],
  "Forged": [],
  "Priority": 100
}
```

## Best Practices

1. **Use Deterministic Seeds for Testing**: When testing, use `rollAffixes(item, seed)` for reproducible results.

2. **Register Affixes Early**: Register custom affixes in your plugin's `setup()` method before any items are created.

3. **Use Pools for Organization**: Create separate pools for different item categories to control which affixes appear where.

4. **Tier Design**: Remember Tier 1 = best. Design tier values with meaningful progression.

5. **Weight Balancing**: Use the `weight` field to make some affixes rarer than others within a pool.

## Compatibility

- Requires Hyforged Stats System (for `StatId` and `HyforgedModifier`)
- Works with Hytale's Quality system for capacity rules
- Items without affixes work normally (backward compatible)
