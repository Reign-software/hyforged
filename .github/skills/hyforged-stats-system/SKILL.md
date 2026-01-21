---
name: hyforged-stats-system
description: Implements ARPG-style stats and modifiers for entities in Hyforged. Use when defining new stats, adding modifiers, working with scaling/derived stats, querying stat values, or handling stat events. Works with HyforgedStatComponent, StatDefinitionRegistry, StatModifier, CoreStats, and rating conversions. Also use when deriving guidance from Modding_Doc/Stats. Triggers - stats, stat, modifier, ability score, derived stat, stat scaling, resistance, rating, basis points, bps, tag targeting, modding doc.
---

# Hyforged Stats System

This skill provides step-by-step guidance for implementing stat features in Hyforged.

## Quick Reference

| Task | Approach |
|------|----------|
| Add a new stat | JSON in `src/main/resources/Server/Hyforged/Stats/` |
| Define NPC stat template | JSON in `src/main/resources/Server/Hyforged/NPCStats/` |
| Add modifier at runtime | `stats.addModifier(StatModifier.flat(...))` |
| Query stat value | `stats.getTotalValue(statIndex)` |
| Target stats by tag | `StatModifier.Builder.targetTag("elemental")` |
| React to stat changes | Subscribe to `StatChangedEvent` |

## Documentation References

- [Stats System Overview](../../../Modding_Doc/Stats/README.md) — Concepts, JSON schemas, scaling, tags
- [Stats API Reference](../../../Modding_Doc/Stats/API.md) — Complete programmatic API

## Doc-Derived How-To (Adding Stats)

Use this as the high-level checklist when translating Modding_Doc into actionable steps.

1. Define a stat JSON in `src/main/resources/Server/<YourMod>/Stats/` using a namespaced `Id`, category, display metadata, and tags.
2. For resources with HUD bars, add assets in `src/main/resources/Server/<YourMod>/Entity/Stats/` and `src/main/resources/Server/<YourMod>/Entity/UI/`.
3. If the stat is derived or rating-based, configure scaling/rating fields as described in the Modding_Doc.
4. Apply or remove modifiers through `HyforgedStatComponent` using `StatModifier` and mark dirty for recomputation.
5. Subscribe to stat change events when you need reactive behavior.

Notes:
- Keep everything data-driven and namespaced; avoid hard-coded values.
- Use tags for broad targeting and compatibility with other systems.

---

## Implementation Workflows

### Workflow 1: Add a New Stat (JSON)

Use this for data-driven stats that don't require custom logic.

**Step 1: Create the stat definition file**

Location: `src/main/resources/Server/Hyforged/Stats/<StatName>.json`

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

**Key Decisions:**
- `Category`: Groups stats in UI (`ability-score`, `offense`, `defense`, `resource`, `utility`, `loot`)
- `IsRating`: Set `true` for stats that use diminishing returns curves
- Suffix convention: `*-flat` for additive, `*-bps` for percentage (basis points)

**Step 2: Define tags for targeting**

Tags enable modifiers to affect multiple stats at once. Use Hytale's hierarchical format:

```json
"Tags": {
  "Domain": ["offense"],
  "Element": ["fire", "elemental"],
  "Type": ["damage"]
}
```

This expands to searchable tags: `Domain`, `offense`, `Domain=offense`, `Element`, `fire`, `elemental`, etc.

---

### Workflow 2: Add a Derived Stat (Scaling)

Derived stats compute their value from other stats.

**Step 1: Define the source stat** (if not already defined)

**Step 2: Add scaling rules**

```json
{
  "Id": "yourmod:attack-power",
  "Category": "offense",
  "DisplayName": "Attack Power",
  "Description": "Increases physical damage.",
  "Scaling": [
    {
      "Type": "linear",
      "Source": "hyforged:strength",
      "Ratio": 2.0
    },
    {
      "Type": "threshold",
      "Source": "hyforged:dexterity",
      "PerPoints": 5,
      "BonusBps": 100
    }
  ]
}
```

**Scaling Types:**

| Type | Formula | Use Case |
|------|---------|----------|
| `linear` | `sourceValue × ratio` | Attribute → derived stat |
| `threshold` | `(sourceValue / perPoints) × bonusBps` | Every X points = Y bonus |
| `diminishing` | Rating curve with soft cap | Defense ratings |

---

### Workflow 3: Add a Rating Stat

Rating stats use diminishing returns curves for effectiveness.

**Step 1: Create the rating stat**

```json
{
  "Id": "yourmod:armor-rating",
  "Category": "defense",
  "DisplayName": "Armor",
  "Description": "Reduces physical damage taken.",
  "IsRating": true,
  "Tags": {
    "Domain": ["defense"],
    "Type": ["rating"]
  }
}
```

**Step 2: Query effectiveness in code**

```java
int armorRating = stats.getCachedValue(armorIndex);
int targetLevel = 50;
int effectivenessBps = stats.getEffectiveness(armorIndex, targetLevel);
// e.g., 2500 = 25% damage reduction
```

**Available Rating Curves:**

| Curve | k Value | Use Case |
|-------|---------|----------|
| `armor` | 20 | Physical damage reduction |
| `evasion` | 25 | Dodge chance |
| `resistance` | 30 | Elemental resistances |
| `accuracy` | 15 | Hit chance |
| `crit` | 40 | Critical from rating |

---

### Workflow 4: Add Modifiers to an Entity

**Getting the Component:**

```java
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.component.HyforgedStatComponent;

ComponentType<EntityStore, HyforgedStatComponent> statType = 
    HyforgedPlugin.getInstance().getHyforgedStatComponentType();

HyforgedStatComponent stats = store.getComponent(entityRef, statType);
```

**Adding Modifiers:**

```java
import reign.software.hyforged.stats.component.StatModifier;

// Quick factory methods
StatModifier flatBonus = StatModifier.flat(
    "item-uuid-123",           // Unique source ID
    ModifierSource.EQUIPMENT,  // Source type
    armorIndex,                // Target stat index
    100                        // +100 armor
);
stats.addModifier(flatBonus);
stats.markDirty(armorIndex);   // Mark for recomputation
```

**Modifier Types:**

| Type | Effect | Example |
|------|--------|---------|
| `FLAT` | Direct addition | +50 armor |
| `INCREASED` | Summed %, then applied | +15% damage (all INCREASED stack additively) |
| `MORE` | Multiplicative % | 20% more (each MORE applies sequentially) |
| `CAP` | Enforces bounds | Max crit 75% |

---

### Workflow 5: Target Stats by Tag

Apply modifiers to multiple stats at once using tags.

```java
// +10% to ALL stats tagged with "elemental"
StatModifier elementalBonus = new StatModifier.Builder("fire-mastery")
    .sourceType(ModifierSource.PASSIVE)
    .modifierType(ModifierType.INCREASED)
    .targetTag("elemental")  // Matches any stat with "elemental" tag
    .value(1000)             // +10% in basis points
    .build();
stats.addModifier(elementalBonus);
```

**Common Tags:**

| Tag | Stats Affected |
|-----|----------------|
| `elemental` | fire, cold, lightning damage/resistance |
| `physical` | physical damage, armor, bleed |
| `offense` | damage, attack speed, crit |
| `defense` | armor, evasion, resistances |
| `attributes` | strength, dexterity, intelligence, etc. |

---

### Workflow 6: Add Conditional Modifiers

Modifiers that only apply under certain conditions.

```java
import reign.software.hyforged.stats.component.ConditionalStatModifier;
import reign.software.hyforged.stats.condition.*;

// Active when health < 30%
ModifierCondition lowHealth = new HealthThresholdCondition(
    3000,  // 30% threshold in bps
    HealthThresholdCondition.Comparison.BELOW
);

StatModifier baseModifier = StatModifier.builder()
    .statIndex(strengthIndex)
    .percentageBonus(2500)  // +25% when low health
    .source(ModifierSource.SKILL)
    .sourceId("berserker_rage")
    .build();

ConditionalStatModifier conditional = ConditionalStatModifier.conditional(
    baseModifier, 
    lowHealth
);
stats.addConditionalModifier(conditional);
```

**Combining Conditions:**

```java
// AND: Both must be true
ModifierCondition combined = condition1.and(condition2);

// OR: Either can be true
ModifierCondition either = condition1.or(condition2);

// NOT: Negate
ModifierCondition inverted = condition.negate();
```

---

### Workflow 7: Subscribe to Stat Change Events

```java
import reign.software.hyforged.stats.event.StatChangedEvent;
import reign.software.hyforged.stats.event.StatBatchChangedEvent;

// In your plugin's init method
getEventRegistry().register(
    StatChangedEvent.class,
    EntityStore.class,
    this::onStatChanged
);

private void onStatChanged(StatChangedEvent event) {
    StatChange change = event.change();
    
    System.out.println("Stat " + change.statId() + 
        " changed from " + change.oldValue() + 
        " to " + change.newValue());
}
```

---

### Workflow 8: Define NPC Stat Templates

Data-driven NPC stat configuration with inheritance.

**Step 1: Create the template file**

Location: `src/main/resources/Server/Hyforged/NPCStats/<TemplateName>.json`

```json
{
  "Id": "yourmod:goblin",
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

**Step 2: Create variants via inheritance**

```json
{
  "Id": "yourmod:goblin_elite",
  "Parent": "yourmod:goblin",
  "MaxHealth": 200,
  "PhysicalPower": 25
}
```

**Step 3: Query templates**

```java
NPCStatTemplateRegistry registry = NPCStatTemplateRegistry.get();
Map<StatId, Integer> stats = registry.resolveStats("yourmod:goblin", 5);
```

---

### Workflow 9: Define Custom Damage Type Resistance

Link damage types to resistance stats.

**Location:** `src/main/resources/Server/Hyforged/Damage/<DamageCause>.json`

```json
{
  "$Comment": "Void damage - a custom damage type",
  "Inherits": "Physical",
  "HyforgedResistanceStat": "yourmod:void-resistance-bps",
  "HyforgedPenetrationStat": "yourmod:void-penetration-bps"
}
```

---

## Common Patterns

### Pattern: Ability Score Stat

```json
{
  "Id": "yourmod:charisma",
  "Category": "ability-score",
  "DisplayName": "Charisma",
  "Description": "Affects NPC interactions and persuasion.",
  "DefaultValue": 10,
  "MinValue": 1,
  "MaxValue": 100,
  "Tags": {
    "Domain": ["attributes"],
    "Type": ["ability-score"]
  }
}
```

### Pattern: Percentage Resistance Stat

```json
{
  "Id": "yourmod:void-resistance-bps",
  "Category": "defense",
  "DisplayName": "Void Resistance",
  "Description": "Reduces void damage taken.",
  "DefaultValue": 0,
  "MinValue": -10000,
  "MaxValue": 10000,
  "Tags": {
    "Domain": ["defense"],
    "Type": ["resistance"],
    "Element": ["void"]
  }
}
```

### Pattern: Derived Stat from Multiple Sources

```json
{
  "Id": "yourmod:hybrid-damage",
  "DisplayName": "Hybrid Damage",
  "Description": "Scales from both Strength and Intelligence.",
  "Scaling": [
    { "Type": "linear", "Source": "hyforged:strength", "Ratio": 1.0 },
    { "Type": "linear", "Source": "hyforged:intelligence", "Ratio": 0.5 }
  ],
  "Tags": {
    "Domain": ["offense"],
    "Source": ["derived"]
  }
}
```

### Pattern: Equipment with Multiple Stat Modifiers

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
    
    // +5% more damage to all elemental stats via tag
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

### Pattern: Timed Buff

```java
public void applyBerserkBuff(HyforgedStatComponent stats, long currentTick) {
    String buffId = "berserk-" + UUID.randomUUID();
    long duration = 20 * 30; // 30 seconds at 20 ticks/sec
    
    stats.addModifier(new StatModifier.Builder(buffId)
        .sourceType(ModifierSource.BUFF)
        .modifierType(ModifierType.MORE)
        .targetStat(attackDamageIndex)
        .value(5000)  // +50% more damage
        .expiresAt(currentTick + duration)
        .build());
}
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Stat not found | Registry not initialized | Ensure stat JSON loaded before querying |
| Value stuck at 0 | Forgot to mark dirty | Call `stats.markDirty(index)` after adding modifiers |
| Circular dependency error | Stat A scales from B, B scales from A | Refactor scaling to avoid cycles |
| Modifier not applying | Wrong stat index | Use `registry.getIndex(statId)` to get correct index |
| Tag targeting not working | Tag not in stat definition | Add tag to stat's `Tags` field in JSON |
| Rating returns 0% | Missing target level | Pass target level to `getEffectiveness()` |

---

## Integer Math & Basis Points

All stats use integer math. Percentages use **basis points (bps)** where `10000 bps = 100%`.

| Percentage | Basis Points |
|------------|--------------|
| 1% | 100 bps |
| 10% | 1000 bps |
| 25% | 2500 bps |
| 100% | 10000 bps |
| 150% | 15000 bps |

---

## Stacking Order

Modifiers apply in ARPG-style order:

1. **FLAT** — All flat bonuses summed
2. **INCREASED** — All % bonuses summed, then applied once
3. **MORE** — Each multiplier applied sequentially
4. **CAP** — Min/max bounds enforced

Example: Base 100, +50 flat, +20% increased, 10% more
```
= (100 + 50) × 1.20 × 1.10
= 150 × 1.20 × 1.10
= 198
```

---

## Checklist: Adding a New Stat

- [ ] Stat ID uses namespace (`hyforged:` or `yourmod:`)
- [ ] Category is appropriate for UI grouping
- [ ] Naming convention: `*-flat` for additive, `*-bps` for percentage
- [ ] Tags defined for group targeting
- [ ] If derived, source stats exist and no circular dependencies
- [ ] If rating, `IsRating: true` and curve is appropriate
- [ ] Min/Max values are sensible

---

## Built-in Stats Quick Reference

### Ability Scores
`hyforged:strength`, `hyforged:dexterity`, `hyforged:intelligence`, `hyforged:constitution`, `hyforged:wisdom`, `hyforged:spirit`, `hyforged:luck`

### Resources
`hyforged:max-health-flat`, `hyforged:max-mana-flat`, `hyforged:max-stamina-flat`, `hyforged:health-regen-flat`, `hyforged:mana-regen-flat`

### Offense
`hyforged:attack-power`, `hyforged:spell-power`, `hyforged:attack-speed-bps`, `hyforged:crit-chance-bps`, `hyforged:crit-multiplier-bps`

### Defense
`hyforged:armor-rating`, `hyforged:evasion-rating`, `hyforged:fire-resistance-bps`, `hyforged:cold-resistance-bps`, `hyforged:lightning-resistance-bps`, `hyforged:poison-resistance-bps`
