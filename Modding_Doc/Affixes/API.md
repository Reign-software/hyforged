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
- **Quality**: Determines affix capacity (Common=0, Legendary=2 prefix + 2 suffix + 1 forged)
- **Item Level**: Determines which affix tiers are available
- **Item Categories/Tags**: Determines which affix pool is used

## Creating Items with Specific Affixes

Use `AffixSpec` to specify exact affixes:

```java
import reign.software.hyforged.affix.api.AffixSpec;

// Create item with specific affixes
ItemStack craftedItem = service.createWithAffixes(
    "Items.Weapons.Sword",
    List.of(
        AffixSpec.of("sturdy", 2),         // Tier 2 Sturdy, random values
        AffixSpec.of("of-the-bear", 1),    // Tier 1 Of the Bear
        AffixSpec.of("mighty")              // Random tier
    )
);
```

### AffixSpec Options

```java
// Tier specified, values will be rolled within tier ranges
AffixSpec.of("sturdy", 2)

// Affix ID only, tier will be rolled
AffixSpec.of("sturdy")
```

## Modifying Existing Items

```java
// Add an affix to an existing item
ItemStack updated = service.addAffix(item, AffixSpec.of("sturdy", 1));

// Remove an affix by ID
ItemStack withoutSturdy = service.removeAffix(item, "sturdy");

// Clear all affixes
ItemStack clean = service.clearAffixes(item);
```

## Registering Custom Affixes

Plugins can register custom affixes at runtime:

### Register an Affix Definition

```java
import reign.software.hyforged.affix.model.AffixTierStat;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixDefinition;

// Create stats for tier 1
Map<String, AffixTierStat> tier1Stats = Map.of(
    "hyforged:strength", new AffixTierStat(
        StatId.hyforged("strength"),
        StackType.FLAT,
        45, 55  // MinValue, MaxValue
    ),
    "hyforged:max-health", new AffixTierStat(
        StatId.hyforged("max-health"),
        StackType.FLAT,
        100, 150
    )
);

// Create stats for tier 2
Map<String, AffixTierStat> tier2Stats = Map.of(
    "hyforged:strength", new AffixTierStat(
        StatId.hyforged("strength"),
        StackType.FLAT,
        30, 40
    ),
    "hyforged:max-health", new AffixTierStat(
        StatId.hyforged("max-health"),
        StackType.FLAT,
        60, 90
    )
);

AffixDefinition customAffix = new AffixDefinition(
    "yourmod:of-the-titan",                   // Namespaced ID
    "suffix",                                  // Type
    "of the Titan",                            // Display name
    List.of(
        new AffixTierDefinition(1, 70, 35, tier1Stats),  // Tier 1: ilvl 70, weight 35
        new AffixTierDefinition(2, 50, 50, tier2Stats)   // Tier 2: ilvl 50, weight 50
    ),
    80                                         // Selection weight
);

service.registerAffix(customAffix);
```

### Register an Affix Pool

```java
AffixPool weaponPool = new AffixPool(
    "my-weapons",                    // Pool ID
    150,                             // Priority (higher = preferred)
    new AffixPoolAppliesTo(
        Set.of("Items.Weapons"),     // Categories
        Set.of("melee")              // Tags
    ),
    List.of("vampiric", "sharp"),    // Prefix affix IDs
    List.of("of-slaying"),           // Suffix affix IDs
    List.of()                        // Forged affix IDs
);

service.registerPool(weaponPool);
```

### Register an Affix Type

```java
// NOTE: DisplayNamePosition is deprecated - affixes are now displayed in PoE style
// in the tooltip, not in the item name. Use NONE for all new types.
AffixType enchantType = new AffixType(
    "enchant",                                  // Type ID
    AffixType.DisplayNamePosition.NONE,         // Use NONE (PoE-style tooltip display)
    "[T{tier}] {name}",                         // Tooltip format (optional)
    false,                                       // Not stackable (one per item)
    "enchant",                                   // HUD section name
    "#bca57a"                                    // HUD color
);

service.registerType(enchantType);
```

> **Display Note:** Affixes are displayed in the item tooltip using Path of Exile style:
> ```
> [T1] +75 Health (50-100)
> [T2] +12% Movement Speed (10%-15%)
> ```
> The `DisplayNamePosition` field exists for categorization (regular vs forged affixes)
> but item names are NOT modified by affixes.

## Data Structures

### RolledAffix

Represents an affix that has been rolled on an item:

```java
public record RolledAffix(
    String affixId,                           // Reference to AffixDefinition
    String type,                              // "prefix", "suffix", or "forged"
    int tier,                                 // 1 = best, higher = weaker
    Map<String, RolledStat> rolledStats       // Rolled stats with values
) {
    // Nested record for each rolled stat
    public record RolledStat(
        int value,                            // Rolled value
        StackType stackType                   // FLAT, INCREASED, or MORE
    ) {}
    
    // Convert to modifiers for stat system
    public List<HyforgedModifier> toModifiers(String sourceId) { ... }
}
```

### AffixDefinition

Defines an affix's properties and tier structure:

```java
public record AffixDefinition(
    String id,                                // Unique identifier
    String type,                              // Type reference
    String displayName,                       // Localization key
    List<AffixTierDefinition> tiers,          // Tier definitions with stats
    int weight                                // Selection weight for rolling
) {
    // Get all stat IDs granted by any tier
    public Set<String> getStatIds() { ... }
}
```

### AffixTierDefinition

Defines a single tier within an affix:

```java
public record AffixTierDefinition(
    int tier,                                 // Tier number (1 = best)
    int itemLevelReq,                         // Required item level
    int weight,                               // Tier selection weight
    Map<String, AffixTierStat> stats          // Stats granted by this tier
) {
    // Check if tier can roll at item level
    public boolean canRollAt(int itemLevel) { ... }
    
    // Check if tier grants a specific stat
    public boolean grantsStat(String statId) { ... }
}
```

### AffixTierStat

Defines a single stat within a tier:

```java
public record AffixTierStat(
    StatId statId,                            // The stat being modified
    StackType stackType,                      // FLAT, INCREASED, or MORE
    int minValue,                             // Minimum rolled value
    int maxValue                              // Maximum rolled value
) {
    // Roll a value within the range
    public int rollValue(double randomFraction) { ... }
}
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

### Affix Definition (`Server/Hyforged/Affixes/Definitions/Prefix/Sturdy.json`)

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
    }
  ]
}
```

### Multi-Stat Affix (`Server/Hyforged/Affixes/Definitions/Suffix/OfTheTitan.json`)

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
    }
  ]
}
```

### Affix Type (`Server/Hyforged/Affixes/Types/Prefix.json`)

```json
{
  "Id": "prefix",
  "DisplayNamePosition": "none",
  "DisplayFormat": "[T{tier}] {name}",
  "Stackable": true
}
```

> **Note:** `DisplayNamePosition` is retained for backwards compatibility and to 
> categorize affixes for tooltip sectioning (regular vs forged). Use `"none"` for 
> all new types. Affixes are displayed in PoE-style in the tooltip, not in item names.

### Quality Rules (`Server/Hyforged/Quality/AffixRules/Rare.json`)

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

### Affix Pool (`Server/Hyforged/Affixes/Pools/Armor.json`)

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

## Best Practices

1. **Use Deterministic Seeds for Testing**: When testing, use `rollAffixes(item, seed)` for reproducible results.

2. **Register Affixes Early**: Register custom affixes in your plugin's `setup()` method before any items are created.

3. **Use Pools for Targeting**: Pools determine which items get which affixes. Create separate pools for different item categories.

4. **Tier Design**: Remember Tier 1 = best. Design tier values with meaningful progression.

5. **Weight Balancing**: Use the `weight` field to make some affixes rarer than others within a pool.

6. **Multi-Stat Design**: Use multi-stat affixes for thematic combinations (e.g., "of the Titan" = Strength + Health).

## Compatibility

- Requires Hyforged Stats System (for `StatId` and `HyforgedModifier`)
- Works with Hytale's Quality system for capacity rules
- Items without affixes work normally (backward compatible)
