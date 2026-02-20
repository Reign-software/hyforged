# Hyforged Stats System - API Reference

This document provides a comprehensive guide to using the Hyforged Stats System in your mod.

## Table of Contents

1. [Overview](#overview)
2. [Querying Entity Stats](#querying-entity-stats)
3. [Modifying Stats](#modifying-stats)
4. [Subscribing to Stat Change Events](#subscribing-to-stat-change-events)
5. [Creating Conditional Modifiers](#creating-conditional-modifiers)
6. [NPC Templates](#npc-templates)
7. [Class Definitions](#class-definitions)
8. [Damage Types](#damage-types)

---

## Overview

The Hyforged Stats System extends Hytale's entity stats with ARPG-style mechanics:

- **HyforgedStatValue**: Extends Hytale's `EntityStatValue` with ARPG stacking
- **HyforgedModifier**: Unified modifier type registered with Hytale's `Modifier.CODEC`
- **Ability Scores**: Primary stats (Strength, Dexterity, Intelligence, etc.)
- **Derived Stats**: Computed from ability scores via scaling rules
- **Modifiers**: Flat, percentage (increased), and multiplier (more) stacking modes
- **Events**: Stat change notifications for reactive systems
- **Templates**: Data-driven NPC stat configuration

### Key Components

| Component | Purpose |
|-----------|---------|
| `EntityStatMap` | Hytale's native stat map component (used directly) |
| `HyforgedStatValue` | ARPG stat value that replaces EntityStatValue at runtime |
| `HyforgedModifier` | Unified modifier type for ARPG stacking |
| `StatAccessor` | Static utility for reading stats from EntityStatMap |
| `StatDefinitionRegistry` | Registry of all stat definitions |
| `HyforgedStatComponent` | Supplementary component for base values and conditionals |

---

## Querying Entity Stats

### Using StatAccessor (Recommended)

```java
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.CoreStats;

// Get stat index
StatDefinitionRegistry registry = StatDefinitionRegistry.get();
int strengthIndex = registry.getIndex(CoreStats.STRENGTH);

// Read stat value from EntityStatMap (includes all modifiers)
int totalValue = StatAccessor.getStatValueInt(entityStatMap, strengthIndex);

// Or from archetypeChunk in a system
int totalValue = StatAccessor.getStatValueInt(archetypeChunk, index, strengthIndex);
```

### Getting EntityStatMap Directly

```java
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;

// Get the component type
ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatMap.getComponentType();

// Get component from chunk
EntityStatMap statMap = archetypeChunk.getComponent(index, statMapType);

// Read value directly
float value = statMap.get(strengthIndex).get();
```

---

## Modifying Stats

### Adding Modifiers with HyforgedModifier

```java
import reign.software.hyforged.stats.modifier.HyforgedModifier;

// Create a modifier using the builder
HyforgedModifier modifier = HyforgedModifier.builder()
    .amount(1500)                                    // +15% (1500 basis points)
    .stackType(HyforgedModifier.StackType.INCREASED)
    .sourceType(HyforgedModifier.SourceType.EQUIPMENT)
    .sourceId("iron_sword")
    .targetStatIndex(strengthIndex)
    .build();

// Add to EntityStatMap with a unique key
entityStatMap.putModifier(strengthIndex, "iron_sword:strength", modifier);
```

### StackType Options

| StackType | Behavior | Example |
|-----------|----------|---------|
| `FLAT` | Adds directly to base | `amount=100` → +100 |
| `INCREASED` | Sums with other INCREASED, applies once | `amount=1000` → +10% |
| `MORE` | Applied multiplicatively in sequence | `amount=2000` → ×1.20 |
| `CAP` | Enforces min/max bounds | `amount=7500` → max 75% |

### Adding Conditional Modifiers

```java
import reign.software.hyforged.stats.component.ConditionalStatModifier;
import reign.software.hyforged.stats.condition.*;

// Create a condition: active when health < 30%
ModifierCondition lowHealthCondition = new HealthThresholdCondition(
    3000,  // 30% threshold
    HealthThresholdCondition.Comparison.BELOW
);

// Create the underlying HyforgedModifier
HyforgedModifier baseModifier = HyforgedModifier.builder()
    .amount(2500)                                    // +25% damage when low health
    .stackType(HyforgedModifier.StackType.INCREASED)
    .sourceType(HyforgedModifier.SourceType.PASSIVE)
    .sourceId("berserker_rage")
    .targetStatIndex(damageIndex)
    .build();

// Wrap in conditional
ConditionalStatModifier conditional = ConditionalStatModifier.conditional(
    baseModifier, 
    lowHealthCondition
);

// Add to HyforgedStatComponent (conditionals still use the component)
stats.addConditionalModifier(conditional);
```

### Removing Modifiers

```java
// Remove a specific modifier by key
entityStatMap.removeModifier(strengthIndex, "iron_sword:strength");

// Remove all modifiers with a key prefix using StatAccessor
StatAccessor.removeAllModifiersByKeyPrefix(entityStatMap, "iron_sword:");

// Remove conditional modifiers by source
stats.removeConditionalModifiersBySource("berserker_rage");
```

---

## Tag-Based Modifiers

One of the most powerful features of the Hyforged stat system is the ability to target **multiple stats at once** using tags. This enables items and effects that say things like "+10% to ALL resistances" or "+5 to ALL attributes".

### How Tag Targeting Works

Instead of targeting a single stat by index, you can target a **tag**. All stats that have that tag in their definition will receive the modifier.

```java
// "+10% to ALL elemental resistances"
// This single modifier affects fire-res, cold-res, AND lightning-res
StatModifier modifier = StatModifier.builder()
    .sourceId("item:ring:elemental_ward")
    .sourceType(ModifierSource.EQUIPMENT)
    .modifierType(ModifierType.INCREASED)
    .targetTag("elemental-resistances")  // Tag, not a stat index
    .value(1000)  // +10% in basis points
    .build();

stats.addModifier(modifier);
```

### Common Tags

| Tag | Stats Affected |
|-----|----------------|
| `elemental-resistances` | fire-res, cold-res, lightning-res |
| `physical-resistances` | physical-res, bleed-res |
| `all-resistances` | All resistance stats |
| `attributes` | strength, dexterity, intelligence, etc. |
| `offensive` | physical-power, spell-power, crit-chance, etc. |
| `defensive` | armor, evasion, block-chance, etc. |

### Defining Tags in Stat Definitions

Tags are defined in the stat's JSON definition:

```json
{
    "Id": "hyforged:fire-resistance-bps",
    "DisplayName": "Fire Resistance",
    "Tags": ["elemental-resistances", "all-resistances", "defensive"]
}
```

---

## Stat-to-Stat Interactions

Stats can **modify other stats** based on their values. This enables derived stats, scaling bonuses, and complex ARPG mechanics.

### How Stat Scaling Works

A stat can define a `ScalesFrom` relationship, meaning its value is partially derived from another stat:

```json
{
    "Id": "hyforged:physical-power",
    "DisplayName": "Physical Power",
    "ScalesFrom": [
        {
            "Stat": "hyforged:strength",
            "Ratio": 200,
            "Type": "FLAT"
        }
    ]
}
```

In this example, every point of Strength adds +2 Physical Power (ratio 200 = 2.00 in basis points).

### Scaling Types

| Type | Formula | Use Case |
|------|---------|----------|
| `FLAT` | `+sourceStat * ratio / 100` | Attribute → derived stat |
| `INCREASED` | `+sourceStat * ratio / 10000` as % | Scaling percentage bonuses |
| `MORE` | Multiplicative scaling | Rare, powerful interactions |

### Example: Attribute Scaling

```json
{
    "Id": "hyforged:crit-chance-bps",
    "DisplayName": "Critical Strike Chance",
    "BaseValue": 500,
    "ScalesFrom": [
        {
            "Stat": "hyforged:dexterity",
            "Ratio": 50,
            "Type": "FLAT"
        },
        {
            "Stat": "hyforged:luck",
            "Ratio": 25,
            "Type": "FLAT"
        }
    ]
}
```

This means:
- Base crit chance: 5% (500 bps)
- Each point of Dexterity adds +0.5% crit (50 bps)
- Each point of Luck adds +0.25% crit (25 bps)

### Dynamic Caps via Stat References

Stats can have their **soft cap raised by another stat**. This is perfect for "increased maximum resistance" effects:

```json
{
    "Id": "hyforged:fire-resistance-bps",
    "DisplayName": "Fire Resistance",
    "SoftCapBps": 7500,
    "HardCapBps": 9000,
    "SoftCapBonusStat": "hyforged:max-fire-resistance-bps"
}
```

Now if a player has `+500 max-fire-resistance-bps`, their fire resistance soft cap becomes 80% instead of 75%.

### Querying Scaling Relationships

```java
StatDefinitionRegistry registry = StatDefinitionRegistry.get();
StatDefinition physicalPower = registry.get(CoreStats.PHYSICAL_POWER);

// Get all stats that this stat scales from
List<StatScaling> scalings = physicalPower.scalesFrom();

for (StatScaling scaling : scalings) {
    System.out.println("Scales from: " + scaling.stat() + 
        " at ratio " + scaling.ratio() + 
        " type " + scaling.type());
}
```

### Example: Complete Attribute Chain

Here's how a full attribute → derived stat → combat stat chain works:

```
Strength (attribute)
    ↓ scales to (+2 per point)
Physical Power (derived)
    ↓ scales to (+1% per 100 power)
Melee Damage (combat)
```

This allows a single "+5 Strength" modifier to cascade through the entire stat system, affecting multiple combat calculations automatically.

---

## Subscribing to Stat Change Events

### Event Types

| Event | When Fired |
|-------|------------|
| `StatChangedEvent` | Individual stat changes |
| `StatBatchChangedEvent` | Batch of changes in a tick |

### Subscribing to Events

```java
import reign.software.hyforged.stats.event.StatChangedEvent;
import reign.software.hyforged.stats.event.StatBatchChangedEvent;

// In your plugin's init method
getEventRegistry().register(
    StatChangedEvent.class,
    EntityStore.class,
    this::onStatChanged
);

getEventRegistry().register(
    StatBatchChangedEvent.class,
    EntityStore.class,
    this::onStatBatchChanged
);

// Event handler
private void onStatChanged(StatChangedEvent event) {
    StatChange change = event.change();
    
    System.out.println("Stat " + change.statId() + 
        " changed from " + change.oldValue() + 
        " to " + change.newValue());
    
    if (change.isIncrease()) {
        // Handle stat increase
    }
}

private void onStatBatchChanged(StatBatchChangedEvent event) {
    for (StatChange change : event.changes()) {
        // Process each change
    }
}
```

---

## Creating Conditional Modifiers

### Available Conditions

| Condition | Description |
|-----------|-------------|
| `HealthThresholdCondition` | Based on health percentage |
| `StateCondition` | Based on status effects |
| `EquipmentCondition` | Based on equipped weapon type |

### Combining Conditions

```java
// AND: Both conditions must be true
ModifierCondition combined = condition1.and(condition2);

// OR: Either condition can be true
ModifierCondition either = condition1.or(condition2);

// NOT: Negate a condition
ModifierCondition inverted = condition.negate();
```

### Custom Conditions

```java
// Implement ModifierCondition interface
public class InCombatCondition implements ModifierCondition {
    @Override
    public boolean evaluate(Ref<EntityStore> entityRef, QueryContext context) {
        return context.isInCombat();
    }
}
```

---

## NPC Templates

### JSON Schema

Create templates in `Server/Hyforged/Stats/NPCTemplates/`:

```json
{
    "Id": "hyforged:goblin",
    "Parent": "hyforged:hostile",
    "MaxHealth": 80,
    "MaxHealthPerLevel": 10,
    "Strength": 8,
    "StrengthPerLevel": 1,
    "Dexterity": 12,
    "DexterityPerLevel": 2,
    "PhysicalPower": 15,
    "PhysicalPowerPerLevel": 3
}
```

### Available Fields

| Field | Type | Description |
|-------|------|-------------|
| `Id` | string | Unique template identifier |
| `Parent` | string | Parent template for inheritance |
| `Strength`, `Dexterity`, etc. | int | Base ability score |
| `StrengthPerLevel`, etc. | int | Per-level bonus |
| `MaxHealth` | int | Base max health |
| `MaxHealthPerLevel` | int | Health per level |

### Inheritance

Templates can inherit from parent templates. Child values override parent values.

```json
{
    "Id": "hyforged:goblin_elite",
    "Parent": "hyforged:goblin",
    "MaxHealth": 200,
    "PhysicalPower": 25
}
```

### Querying Templates

```java
NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();

// Get template
NPCStatTemplate template = registry.getTemplate("hyforged:goblin");

// Resolve stats for a level
Map<StatId, Integer> stats = registry.resolveStats("hyforged:goblin", 5);
```

---

## Class Definitions

### JSON Schema

Create classes in `Server/Hyforged/Progression/Classes/`:

```json
{
    "DisplayName": "Warrior",
    "Description": "A powerful melee combatant focusing on strength.",
    "Strength": 5,
    "Constitution": 4,
    "Dexterity": 2,
    "Intelligence": 1,
    "Wisdom": 1,
    "Spirit": 1,
    "Luck": 1
}
```

### Querying Classes

```java
ClassDefinitionRegistry registry = ClassDefinitionRegistry.get();

// Get class definition
ClassDefinition classDef = registry.get("hyforged:warrior");

// Get default class
ClassDefinition defaultClass = registry.getDefault();

// Get ability scores
Map<StatId, Integer> abilityScores = classDef.abilityScores();
```

---

## Damage Types

### Custom Damage Types

Hyforged uses a fully data-driven system for damage type extensions. Create JSON files in `Server/Hyforged/Stats/Damage/` to define which resistance stat applies to each damage type. The file name must match the Hytale `DamageCause` ID (e.g., `Fire.json` for Fire damage).

```json
{
    "$Comment": "Description of this damage type",
    "Inherits": "Physical",
    "HyforgedResistanceStat": "hyforged:bleed-resistance-bps",
    "HyforgedPenetrationStat": "hyforged:bleed-penetration-bps"
}
```

### Damage Type Fields

| Field | Description |
|-------|-------------|
| `Inherits` | Parent damage type for stat inheritance (optional) |
| `HyforgedResistanceStat` | Stat ID that provides resistance to this damage type |
| `HyforgedPenetrationStat` | Stat ID that bypasses resistance (future use) |

### Inheritance

If a damage type doesn't define a `HyforgedResistanceStat`, the system will check parent damage types. For example:
- `Bleed` inherits from `Physical`
- If `Bleed` has no resistance stat, `Physical` resistance applies

### Built-in Damage Types

Hyforged includes extensions for common damage types:
- `Physical.json` → `hyforged:physical-resistance-bps`
- `Fire.json` → `hyforged:fire-resistance-bps`
- `Ice.json` → `hyforged:cold-resistance-bps`
- `Lightning.json` → `hyforged:lightning-resistance-bps`
- `Poison.json` → `hyforged:poison-resistance-bps`
- `Chaos.json` → `hyforged:chaos-resistance-bps`
- `Bleed.json` → `hyforged:bleed-resistance-bps`

---

## Best Practices

1. **Always mark stats dirty** after modifying modifiers
2. **Use source IDs** to track modifier origins for easy removal
3. **Prefer batch operations** when modifying multiple stats
4. **Subscribe to batch events** for efficiency when processing many changes
5. **Use conditional modifiers** for reactive gameplay mechanics
6. **Extend templates** rather than duplicating values

---

## Version

This documentation is for Hyforged Entity Stats v1.0.0.
