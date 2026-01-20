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

- **Ability Scores**: Primary stats (Strength, Dexterity, Intelligence, etc.)
- **Derived Stats**: Computed from ability scores via scaling rules
- **Modifiers**: Flat, percentage, and multiplier stacking modes
- **Events**: Stat change notifications for reactive systems
- **Templates**: Data-driven NPC stat configuration

### Key Components

| Component | Purpose |
|-----------|---------|
| `HyforgedStatComponent` | ECS component storing stat values and modifiers |
| `StatDefinitionRegistry` | Registry of all stat definitions |
| `ClassDefinitionRegistry` | Registry of player classes |
| `NPCStatTemplateRegistry` | Registry of NPC stat templates |

---

## Querying Entity Stats

### Getting the Stat Component

```java
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Get the component type from the plugin
ComponentType<EntityStore, HyforgedStatComponent> statType = 
    HyforgedPlugin.getInstance().getHyforgedStatComponentType();

// Get component from entity reference
HyforgedStatComponent stats = store.getComponent(entityRef, statType);
```

### Reading Stat Values

```java
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.CoreStats;

// Get stat index
StatDefinitionRegistry registry = StatDefinitionRegistry.get();
int strengthIndex = registry.getIndex(CoreStats.STRENGTH);

// Read values
int baseValue = stats.getBaseValue(strengthIndex);
int totalValue = stats.getTotalValue(strengthIndex);

// For context-aware stats (with conditional modifiers)
QueryContext context = QueryContext.builder()
    .healthPercentBps(5000)  // 50% health
    .inCombat(true)
    .build();
int effectiveValue = stats.getEffectiveValue(strengthIndex, entityRef, context);
```

---

## Modifying Stats

### Adding Modifiers

```java
import reign.software.hyforged.stats.component.StatModifier;

// Create a modifier
StatModifier modifier = StatModifier.builder()
    .statIndex(strengthIndex)
    .flatBonus(10)           // +10 flat
    .percentageBonus(1500)   // +15% (basis points)
    .multiplier(1000)        // x1.0 (basis points, 1000 = 1x)
    .source(ModifierSource.EQUIPMENT)
    .sourceId("iron_sword")
    .duration(0)             // Permanent (0 = no duration)
    .build();

// Add to component
stats.addModifier(modifier);
stats.markDirty(strengthIndex);  // Mark for recomputation
```

### Adding Conditional Modifiers

```java
import reign.software.hyforged.stats.component.ConditionalStatModifier;
import reign.software.hyforged.stats.condition.*;

// Create a condition: active when health < 30%
ModifierCondition lowHealthCondition = new HealthThresholdCondition(
    3000,  // 30% threshold
    HealthThresholdCondition.Comparison.BELOW
);

// Create conditional modifier
StatModifier baseModifier = StatModifier.builder()
    .statIndex(strengthIndex)
    .percentageBonus(2500)  // +25% damage when low health
    .source(ModifierSource.SKILL)
    .sourceId("berserker_rage")
    .build();

ConditionalStatModifier conditional = ConditionalStatModifier.conditional(
    baseModifier, 
    lowHealthCondition
);

stats.addConditionalModifier(conditional);
```

### Removing Modifiers

```java
// Remove all modifiers from a specific source ID
stats.removeModifiersBySource("iron_sword");

// Remove conditional modifiers by source
stats.removeConditionalModifiersBySource("berserker_rage");
```

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

Create templates in `Server/Hyforged/NPCStats/`:

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

Create classes in `Server/Hyforged/Classes/`:

```json
{
    "Id": "hyforged:warrior",
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

Create damage types in `Server/Hyforged/Damage/`:

```json
{
    "$Comment": "Description of this damage type",
    "Parent": "Physical",
    "Inherits": "Physical",
    "HyforgedResistanceStat": "hyforged:bleed-resistance-bps",
    "HyforgedPenetrationStat": "hyforged:bleed-penetration-bps"
}
```

### Hyforged Extensions

| Field | Description |
|-------|-------------|
| `HyforgedResistanceStat` | Stat ID for resistance lookup |
| `HyforgedPenetrationStat` | Stat ID for penetration lookup |

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
