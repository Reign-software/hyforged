# Hyforged Stats System — Modding Guide

This guide explains how to add custom stats to the Hyforged stat system. The system follows an Entity Component System (ECS) architecture with data-driven definitions.

## Documentation

- [API Reference](API.md) — Code examples for querying stats, modifiers, events, and conditional modifiers
- This README — JSON schema and data-driven configuration

## Quick Start

1. Create a stat definition JSON in your mod's `Server/<YourMod>/Stats/` folder
2. Use namespaced IDs to avoid conflicts with other mods
3. Apply modifiers to entities via the API

---

## Table of Contents

- [Core Concepts](#core-concepts)
- [Defining Stats (JSON)](#defining-stats-json)
- [Stat ID Namespacing](#stat-id-namespacing)
- [Modifier Types (Stacking)](#modifier-types-stacking)
- [Adding Modifiers via Code](#adding-modifiers-via-code)
- [Tags and Tag Targeting](#tags-and-tag-targeting)
- [Rating Stats](#rating-stats)
- [Scaling (Derived Stats)](#scaling-derived-stats)
- [Built-in Stats Reference](#built-in-stats-reference)
- [Examples](#examples)

---

## Core Concepts

### Integer Math
All stat values use **integer arithmetic** to ensure deterministic computation across clients and servers. Percentages are expressed in **basis points (bps)** where `10000 bps = 100%`.

| Percentage | Basis Points |
|------------|--------------|
| 10%        | 1000 bps     |
| 25%        | 2500 bps     |
| 100%       | 10000 bps    |
| 150%       | 15000 bps    |

### Stacking Order
Modifiers apply in a strict ARPG-style order:
1. **FLAT** — All flat bonuses are summed
2. **INCREASED** — All increased% bonuses are summed, then applied
3. **MORE** — Each more% bonus is applied multiplicatively in sequence
4. **CAP** — Min/max bounds are enforced

### Data-Driven Definitions
Stats are defined in JSON files and loaded at server startup. No code changes required to add new stats.

---

## Defining Stats (JSON)

Create a JSON file in `Server/<YourMod>/Stats/<StatName>.json`:

```json
{
  "Id": "yourmod:custom-stat",
  "Category": "offense",
  "DisplayName": "Custom Stat",
  "Description": "Description shown in tooltips.",
  "DefaultValue": 0,
  "MinValue": 0,
  "MaxValue": 10000,
  "IsRating": false,
  "Tags": {
    "Domain": ["offense"],
    "Type": ["damage"]
  }
}
```

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `Id` | string | Namespaced identifier (`namespace:name`) |
| `Category` | string | UI grouping category |
| `DisplayName` | string | Human-readable name for UI |
| `Description` | string | Tooltip description |

### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `DefaultValue` | int | `0` | Starting value for the stat |
| `MinValue` | int | `0` | Minimum allowed value |
| `MaxValue` | int | `MAX_INT` | Maximum allowed value |
| `IsRating` | bool | `false` | Whether this stat uses rating-to-effectiveness conversion |
| `Tags` | object | `{}` | Hierarchical tags using Hytale's format `{"Category": ["Value"]}` |

### Categories

Use these standard categories or define your own:

| Category | Description |
|----------|-------------|
| `ability-score` | Primary attributes (STR, DEX, etc.) |
| `offense` | Damage and attack stats |
| `defense` | Armor, evasion, resistances |
| `resource` | Health, mana, stamina pools |
| `utility` | Cooldown, duration, area effects |
| `loot` | Item find bonuses |

---

## Stat ID Namespacing

All stat IDs use the format `namespace:name` to prevent collisions between mods.

```
hyforged:strength        ← Core Hyforged stat
yourmod:custom-stat      ← Your mod's stat
othermod:custom-stat     ← Different mod, no collision!
```

### Rules
- Namespace should be your mod's unique identifier
- Use lowercase with hyphens: `my-cool-stat` ✓, `MyCoolStat` ✗
- No colons in namespace or name

### In Code

```java
// Parse from string
StatId id = StatId.parse("yourmod:custom-stat");

// Create with explicit namespace
StatId id = new StatId("yourmod", "custom-stat");
```

---

## Modifier Types (Stacking)

### FLAT
Direct addition/subtraction to the base value.

```java
// +50 to max health
StatModifier.flat("item-123", ModifierSource.EQUIPMENT, healthIndex, 50);
```

### INCREASED
Percentage bonus in basis points. All INCREASED modifiers are summed together.

```java
// +10% increased damage (1000 bps)
StatModifier.increased("passive-1", ModifierSource.PASSIVE, damageIndex, 1000);

// Another +15% increased damage (1500 bps)  
// Total: 25% increased applied to base
StatModifier.increased("passive-2", ModifierSource.PASSIVE, damageIndex, 1500);
```

### MORE
Percentage multiplier in basis points. Each MORE modifier is applied sequentially.

```java
// 20% more damage (2000 bps)
StatModifier.more("buff-1", ModifierSource.BUFF, damageIndex, 2000);

// Another 10% more damage (1000 bps)
// Applied as: base * 1.20 * 1.10 (NOT base * 1.30)
StatModifier.more("buff-2", ModifierSource.BUFF, damageIndex, 1000);
```

### CAP
Enforces minimum or maximum bounds.

```java
// Using the Builder for cap modifiers
new StatModifier.Builder("class-cap")
    .sourceType(ModifierSource.CLASS)
    .modifierType(ModifierType.CAP)
    .targetStat(critChanceIndex)
    .value(7500)  // Cap crit chance at 75% (7500 bps)
    .build();
```

---

## Adding Modifiers via Code

### Getting the Component

```java
// From an entity
HyforgedStatComponent stats = entity.getComponent(HyforgedStatComponent.class);
```

### Looking Up Stat Index

```java
StatDefinitionRegistry registry = StatDefinitionRegistry.get();

// By StatId constant
int armorIndex = registry.getIndex(CoreStats.ARMOR_RATING);

// By parsed ID
StatId customId = StatId.parse("yourmod:custom-stat");
int customIndex = registry.getIndex(customId);
```

### Adding Modifiers

```java
// Quick factory methods
StatModifier flatBonus = StatModifier.flat(
    "item-uuid-123",           // Unique source ID
    ModifierSource.EQUIPMENT,  // Source type
    armorIndex,                // Target stat index
    100                        // +100 armor
);
stats.addModifier(flatBonus);

// Builder for more control
StatModifier timedBuff = new StatModifier.Builder("speed-potion")
    .sourceType(ModifierSource.BUFF)
    .modifierType(ModifierType.INCREASED)
    .targetStat(attackSpeedIndex)
    .value(200)                    // +20% attack speed
    .expiresAt(currentTick + 6000) // Expires in 5 minutes (20 ticks/sec)
    .priority(10)                  // Lower priority = applied first
    .build();
stats.addModifier(timedBuff);
```

### Removing Modifiers

```java
// Remove all modifiers from a source (e.g., when item unequipped)
stats.removeModifiersBySource("item-uuid-123");

// Remove all modifiers of a type (e.g., clear all buffs)
stats.removeModifiersBySourceType(ModifierSource.BUFF);

// Remove expired modifiers (called automatically by systems)
stats.removeExpiredModifiers(currentTick);
```

---

## Tags and Tag Targeting

Tags allow modifiers to affect multiple stats at once. The Hyforged stat system integrates with Hytale's global `AssetRegistry` tag system, meaning stat tags share the same namespace as item, block, and NPC tags.

### How Tags Work

Tags use Hytale's hierarchical format `{"Category": ["Value1", "Value2"]}`. When a stat is loaded, each tag entry is automatically expanded into multiple searchable tags:

- The **category key** itself (e.g., `"Domain"`)
- Each **value** in the array (e.g., `"offense"`)
- The **category=value** combination (e.g., `"Domain=offense"`)

This provides:
- **Shared namespace**: Your stat tags can match item/block tags used elsewhere in the game
- **Integer indices**: Fast O(1) lookups using integer indices instead of string comparisons
- **Flexible queries**: Match by category, value, or specific combinations

### Defining Tags in JSON

```json
{
  "Id": "yourmod:fire-damage-flat",
  "Tags": {
    "Domain": ["offense"],
    "Type": ["damage"],
    "Element": ["fire", "elemental"],
    "Modifier": ["flat"]
  }
}
```

This expands to these searchable tags:
- `Domain`, `offense`, `Domain=offense`
- `Type`, `damage`, `Type=damage`
- `Element`, `fire`, `elemental`, `Element=fire`, `Element=elemental`
- `Modifier`, `flat`, `Modifier=flat`

### Standard Tag Categories

| Category | Values | Description |
|----------|--------|-------------|
| `Domain` | `offense`, `defense`, `resource`, `utility`, `attributes` | Primary functional classification |
| `Element` | `physical`, `fire`, `cold`, `lightning`, `chaos`, `elemental` | Damage/resistance element |
| `Type` | `damage`, `resistance`, `rating`, `ability-score`, `speed`, `critical`, etc. | What the stat represents |
| `Modifier` | `flat`, `percent`, `more` | How the stat applies |
| `Source` | `derived`, `base` | Origin of the stat value |
| `Mechanic` | `attack`, `spell`, `projectile`, `melee`, `ranged`, `minion`, etc. | Usage mechanism |
| `Resource` | `health`, `mana`, `stamina`, `rage` | Which resource it affects |
| `Ailment` | `bleed`, `poison`, `ignite`, `chill`, `shock`, `freeze` | Specific ailment type |
| `Weapon` | `sword`, `axe`, `mace`, `dagger`, `bow`, etc. | Weapon type affinity |

### Targeting a Tag

```java
// +10% to ALL stats tagged with "elemental"
StatModifier elementalBonus = new StatModifier.Builder("fire-mastery")
    .sourceType(ModifierSource.PASSIVE)
    .modifierType(ModifierType.INCREASED)
    .targetTag("elemental")  // Matches any stat with "elemental" in expanded tags
    .value(1000)             // +10%
    .build();

// Target a specific category=value combination
StatModifier fireOnlyBonus = new StatModifier.Builder("fire-affinity")
    .sourceType(ModifierSource.PASSIVE)
    .modifierType(ModifierType.INCREASED)
    .targetTag("Element=fire")  // Only stats with Element: ["fire"]
    .value(1500)
    .build();

// Or use a pre-resolved tag index for performance in hot paths
int elementalTagIndex = AssetRegistry.getOrCreateTagIndex("elemental");
StatModifier fastBonus = new StatModifier.Builder("fire-mastery")
    .sourceType(ModifierSource.PASSIVE)
    .modifierType(ModifierType.INCREASED)
    .targetTagIndex(elementalTagIndex)  // Direct integer index
    .value(1000)
    .build();
```

---

## Rating Stats

Rating stats convert a raw value to an effectiveness percentage using diminishing returns curves (PoE-style).

### Defining a Rating Stat

```json
{
  "Id": "yourmod:accuracy-rating",
  "Category": "offense",
  "DisplayName": "Accuracy",
  "Description": "Improves chance to hit targets.",
  "IsRating": true,
  "Tags": ["defense", "rating", "magic"]
}
```

### How Ratings Work

Ratings use a level-scaled formula with diminishing returns:

```
effectiveness = rating / (rating + k * targetLevel)
```

Where `k` is a configurable constant that controls the curve steepness.

Example with Armor Rating:
- 500 armor vs level 50 target → ~25% physical reduction
- 1000 armor vs level 50 target → ~40% physical reduction
- 2000 armor vs level 50 target → ~55% physical reduction

### Querying Effectiveness

```java
// Get raw rating value
int armorRating = stats.getCachedValue(armorIndex);

// Get effectiveness against a target
int targetLevel = 50;
int effectivenessBps = stats.getEffectiveness(armorIndex, targetLevel);
// e.g., 2500 = 25% damage reduction
```

---

## Scaling (Derived Stats)

Scaling allows stats to derive their base value from other stats. This is how ability scores affect derived stats like Attack Power (from Strength) or Critical Chance (from Luck).

### Scaling Types

| Type | Description | Use Case |
|------|-------------|----------|
| `linear` | Multiplies source value by a ratio | Attack Power = Strength × 2 |
| `threshold` | Grants bonus per X points of source | Every 5 Luck = +1% Crit Chance |
| `diminishing` | Uses a rating curve for soft caps | Defense ratings with diminishing returns |

### Defining Scaling in JSON

Add a `Scaling` array to your stat definition:

```json
{
  "Id": "yourmod:custom-attack",
  "Category": "offense",
  "DisplayName": "Custom Attack",
  "Scaling": [
    {
      "Type": "linear",
      "Source": "hyforged:strength",
      "Ratio": 1.5
    }
  ]
}
```

### Linear Scaling

Multiplies the source stat's computed value by a ratio.

```json
{
  "Type": "linear",
  "Source": "hyforged:strength",
  "Ratio": 2.0
}
```

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `Type` | string | Must be `"linear"` |
| `Source` | string | Source stat ID (namespaced) |
| `Ratio` | number | Multiplier (can be fractional) |

**Formula:** `contribution = floor(sourceValue × ratio)`

**Example:** With Strength = 25 and Ratio = 2.0:
- Contribution = floor(25 × 2.0) = 50

### Threshold Scaling

Grants a fixed bonus for every X points of the source stat.

```json
{
  "Type": "threshold",
  "Source": "hyforged:luck",
  "PerPoints": 5,
  "BonusBps": 100
}
```

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `Type` | string | Must be `"threshold"` |
| `Source` | string | Source stat ID (namespaced) |
| `PerPoints` | int | Points of source stat per bonus |
| `BonusBps` | int | Bonus granted per threshold (basis points for % stats) |

**Formula:** `contribution = (sourceValue / perPoints) × bonusBps`

**Example:** With Luck = 25, PerPoints = 5, BonusBps = 100:
- Contribution = (25 / 5) × 100 = 500 bps = 5% crit chance

### Diminishing Scaling

Uses a rating curve for soft-capped scaling (PoE-style diminishing returns).

```json
{
  "Type": "diminishing",
  "Source": "hyforged:armor-rating",
  "Curve": "armor",
  "Scale": 100.0,
  "CapBps": 9000
}
```

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `Type` | string | Must be `"diminishing"` |
| `Source` | string | Source stat ID (namespaced) |
| `Curve` | string | Rating curve name (see below) |
| `Scale` | number | Output multiplier |
| `CapBps` | int | Maximum contribution in basis points |

**Available Curves:**
| Curve | k Value | Use Case |
|-------|---------|----------|
| `armor` | 20 | Physical damage reduction |
| `evasion` | 25 | Dodge chance |
| `resistance` | 30 | Elemental resistances |
| `accuracy` | 15 | Hit chance |
| `crit` | 40 | Critical from rating |
| `default` | 20 | General purpose |

**Formula:** `effectiveness = rating / (rating + k × targetLevel)`

### Multiple Scaling Rules

Stats can have multiple scaling sources. All contributions are summed:

```json
{
  "Id": "yourmod:hybrid-damage",
  "DisplayName": "Hybrid Damage",
  "Scaling": [
    {
      "Type": "linear",
      "Source": "hyforged:strength",
      "Ratio": 1.0
    },
    {
      "Type": "linear",
      "Source": "hyforged:intelligence",
      "Ratio": 0.5
    }
  ]
}
```

This stat gains 1 point per Strength plus 0.5 points per Intelligence.

### Evaluation Order

Stats are evaluated in **dependency order** using topological sorting:
1. Source stats (no dependencies) are computed first
2. Derived stats are computed after their sources
3. Circular dependencies are detected and rejected at load time

This means if Attack Power scales from Strength, Strength is always computed before Attack Power.

### Stat Breakdown

The stat breakdown UI shows scaling contributions separately:

```
Attack Power: 70
  Base: 0
  + 50 from Strength (linear ×2)
  + 20 flat from equipment
  = 70
```

### Tips for Scaling

1. **Use `derived` tag** — Tag stats with scaling as `derived` for clarity
2. **Source must exist** — The source stat must be registered before the derived stat loads
3. **Order matters** — Define source stats before derived stats in your mod's load order
4. **Integer math** — All scaling uses integer arithmetic; use appropriate ratios

---

## Built-in Stats Reference

### Ability Scores

| Stat ID | Display Name | Description |
|---------|--------------|-------------|
| `hyforged:strength` | Strength | Melee damage, physical power |
| `hyforged:dexterity` | Dexterity | Attack speed, evasion |
| `hyforged:intelligence` | Intelligence | Spell power, mana |
| `hyforged:constitution` | Constitution | Max health, health regen |
| `hyforged:wisdom` | Wisdom | Resistances, effect duration |
| `hyforged:spirit` | Spirit | Mana regen, healing power |
| `hyforged:luck` | Luck | Crit chance, item find |

### Resources

| Stat ID | Display Name |
|---------|--------------|
| `hyforged:max-health-flat` | Max Health |
| `hyforged:max-mana-flat` | Max Mana |
| `hyforged:max-stamina-flat` | Max Stamina |
| `hyforged:health-regen-flat` | Health Regen |
| `hyforged:mana-regen-flat` | Mana Regen |
| `hyforged:stamina-regen-flat` | Stamina Regen |

### Offense

| Stat ID | Display Name |
|---------|--------------|
| `hyforged:attack-power` | Attack Power |
| `hyforged:spell-power` | Spell Power |
| `hyforged:attack-speed-bps` | Attack Speed |
| `hyforged:cast-speed-bps` | Cast Speed |
| `hyforged:crit-chance-bps` | Critical Chance |
| `hyforged:crit-multiplier-bps` | Critical Multiplier |
| `hyforged:accuracy-rating` | Accuracy |

### Defense (Ratings + Resistances)

| Stat ID | Display Name |
|---------|--------------|
| `hyforged:armor-rating` | Armor |
| `hyforged:evasion-rating` | Evasion |
| `hyforged:fire-resistance-bps` | Fire Resistance |
| `hyforged:cold-resistance-bps` | Cold Resistance |
| `hyforged:lightning-resistance-bps` | Lightning Resistance |
| `hyforged:poison-resistance-bps` | Poison Resistance |

---

## Examples

### Example 1: Custom Damage Type

Create a new elemental damage stat for your mod:

**Server/YourMod/Stats/VoidDamage.json**
```json
{
  "Id": "yourmod:void-damage-increased-bps",
  "Category": "offense",
  "DisplayName": "Void Damage",
  "Description": "Increases void damage dealt.",
  "DefaultValue": 0,
  "MinValue": -10000,
  "MaxValue": 100000,
  "IsRating": false,
  "Tags": ["offense", "elemental", "void", "percent"]
}
```

### Example 2: Custom Resistance (Percent)

**Server/YourMod/Stats/VoidResistance.json**
```json
{
  "Id": "yourmod:void-resistance-bps",
  "Category": "resistance",
  "DisplayName": "Void Resistance",
  "Description": "Reduces void damage taken.",
  "DefaultValue": 0,
  "MinValue": -10000,
  "MaxValue": 10000,
  "IsRating": false,
  "Tags": ["defense", "resistance", "percent", "void"]
}
```

### Example 3: Equipment with Multiple Stats

```java
public void applyItemStats(HyforgedStatComponent stats, String itemId) {
    StatDefinitionRegistry reg = StatDefinitionRegistry.get();
    
    // Flat armor bonus
    stats.addModifier(StatModifier.flat(
        itemId, ModifierSource.EQUIPMENT,
        reg.getIndex(CoreStats.ARMOR_RATING), 150
    ));
    
    // +15% increased fire resistance
    stats.addModifier(StatModifier.increased(
        itemId, ModifierSource.EQUIPMENT,
      reg.getIndex(CoreStats.FIRE_RESISTANCE_BPS), 1500
    ));
    
    // +5% more damage to all elemental stats
    stats.addModifier(new StatModifier.Builder(itemId)
        .sourceType(ModifierSource.EQUIPMENT)
        .modifierType(ModifierType.MORE)
        .targetTag("elemental")
      .value(500)
        .build());
}

public void removeItemStats(HyforgedStatComponent stats, String itemId) {
    stats.removeModifiersBySource(itemId);
}
```

### Example 4: Timed Buff

```java
public void applyBerserkBuff(HyforgedStatComponent stats, long currentTick) {
    String buffId = "berserk-" + UUID.randomUUID();
    StatDefinitionRegistry reg = StatDefinitionRegistry.get();
    long duration = 20 * 30; // 30 seconds at 20 ticks/sec
    
    // +50% more attack damage
    stats.addModifier(new StatModifier.Builder(buffId)
        .sourceType(ModifierSource.BUFF)
        .modifierType(ModifierType.MORE)
        .targetStat(reg.getIndex(CoreStats.ATTACK_DAMAGE_INCREASED_BPS))
        .value(500)
        .expiresAt(currentTick + duration)
        .build());
    
    // -20% armor (negative value)
    stats.addModifier(new StatModifier.Builder(buffId)
        .sourceType(ModifierSource.BUFF)
        .modifierType(ModifierType.INCREASED)
        .targetStat(reg.getIndex(CoreStats.ARMOR_RATING))
        .value(-200)
        .expiresAt(currentTick + duration)
        .build());
}
```

---

## Tips

1. **Use unique source IDs** — Always use UUIDs or unique identifiers for equipment/buffs so they can be cleanly removed.

2. **Namespace everything** — Use `yourmod:stat-name` to avoid conflicts.

3. **Prefer tags for group bonuses** — Instead of adding 5 modifiers for 5 elemental stats, target the `elemental` tag.

4. **Remember basis points** — 10% = 100 bps, not 10!

5. **Test with the debug tracer** — Use `StatDebugTracer` to see the full breakdown of how a stat is computed.

---

## See Also

- [Stats System Architecture](../../.memory_bank/Features/hyforged-stats-system/) — Internal design docs
- [Core Stats Source](../../src/main/java/reign/software/hyforged/stats/CoreStats.java) — Built-in stat constants
- [Stat Definitions](../../src/main/resources/Server/Hyforged/Stats/) — JSON examples
