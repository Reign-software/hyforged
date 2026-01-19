# Hyforged Stats System — Modding Guide

This guide explains how to add custom stats to the Hyforged stat system. The system follows an Entity Component System (ECS) architecture with data-driven definitions.

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
- [Built-in Stats Reference](#built-in-stats-reference)
- [Examples](#examples)

---

## Core Concepts

### Integer Math
All stat values use **integer arithmetic** to ensure deterministic computation across clients and servers. Percentages are expressed in **basis points (bps)** where `1000 bps = 100%`.

| Percentage | Basis Points |
|------------|--------------|
| 10%        | 100 bps      |
| 25%        | 250 bps      |
| 100%       | 1000 bps     |
| 150%       | 1500 bps     |

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
  "Tags": ["offense", "custom"]
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
| `Tags` | string[] | `[]` | Tags for group targeting |

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
// +10% increased damage (100 bps)
StatModifier.increased("passive-1", ModifierSource.PASSIVE, damageIndex, 100);

// Another +15% increased damage (150 bps)  
// Total: 25% increased applied to base
StatModifier.increased("passive-2", ModifierSource.PASSIVE, damageIndex, 150);
```

### MORE
Percentage multiplier in basis points. Each MORE modifier is applied sequentially.

```java
// 20% more damage (200 bps)
StatModifier.more("buff-1", ModifierSource.BUFF, damageIndex, 200);

// Another 10% more damage (100 bps)
// Applied as: base * 1.20 * 1.10 (NOT base * 1.30)
StatModifier.more("buff-2", ModifierSource.BUFF, damageIndex, 100);
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

Tags allow modifiers to affect multiple stats at once.

### Defining Tags in JSON

```json
{
  "Id": "yourmod:fire-damage-flat",
  "Tags": ["offense", "elemental", "fire", "damage"]
}
```

### Targeting a Tag

```java
// +10% to ALL stats tagged with "elemental"
StatModifier elementalBonus = new StatModifier.Builder("fire-mastery")
    .sourceType(ModifierSource.PASSIVE)
    .modifierType(ModifierType.INCREASED)
    .targetTag("elemental")  // Affects all stats with this tag
    .value(100)              // +10%
    .build();
```

### Common Tags

| Tag | Used For |
|-----|----------|
| `offense` | All offensive stats |
| `defense` | All defensive stats |
| `elemental` | All elemental damage/resistance |
| `fire`, `cold`, `lightning`, `poison` | Specific elements |
| `physical` | Physical damage/defense |
| `rating` | Stats using rating curves |
| `percent` | Stats displayed as percentages |

---

## Rating Stats

Rating stats convert a raw value to an effectiveness percentage using diminishing returns curves (PoE-style).

### Defining a Rating Stat

```json
{
  "Id": "yourmod:magic-resist-rating",
  "Category": "defense",
  "DisplayName": "Magic Resistance",
  "Description": "Reduces magic damage taken.",
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

### Defense (Ratings)

| Stat ID | Display Name |
|---------|--------------|
| `hyforged:armor-rating` | Armor |
| `hyforged:evasion-rating` | Evasion |
| `hyforged:fire-resistance-rating` | Fire Resistance |
| `hyforged:cold-resistance-rating` | Cold Resistance |
| `hyforged:lightning-resistance-rating` | Lightning Resistance |
| `hyforged:poison-resistance-rating` | Poison Resistance |

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

### Example 2: Custom Defense Rating

**Server/YourMod/Stats/VoidResistance.json**
```json
{
  "Id": "yourmod:void-resistance-rating",
  "Category": "defense",
  "DisplayName": "Void Resistance",
  "Description": "Reduces void damage taken.",
  "DefaultValue": 0,
  "IsRating": true,
  "Tags": ["defense", "rating", "void"]
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
        reg.getIndex(CoreStats.FIRE_RESISTANCE_RATING), 150
    ));
    
    // +5% more damage to all elemental stats
    stats.addModifier(new StatModifier.Builder(itemId)
        .sourceType(ModifierSource.EQUIPMENT)
        .modifierType(ModifierType.MORE)
        .targetTag("elemental")
        .value(50)
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
