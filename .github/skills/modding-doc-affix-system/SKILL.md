---
name: modding-doc-affix-system
description: Implements ARPG-style affixes for equipment in Hyforged. Use when adding new affixes, creating affix pools, rolling affixes on items, or working with AffixService, AffixDefinition, RolledAffix, AffixPool, or AffixSpec. Also use when deriving guidance from Modding_Doc/Affixes. Triggers - affix, affixes, prefix, suffix, forged, item modifiers, equipment stats, multi-stat, modding doc.
---

# Hyforged Affix System

This skill provides step-by-step guidance for implementing affix features in Hyforged.

## Quick Reference

| Task | Approach |
|------|----------|
| Add a new affix | JSON in `src/main/resources/Server/Hyforged/Affixes/<Type>/` |
| Create affix pool | JSON in `src/main/resources/Server/Hyforged/AffixPools/` |
| Custom affix at runtime | `AffixService.get().registerAffix(...)` |
| Roll affixes on item | `AffixService.get().rollAffixes(item)` |
| Query item affixes | `AffixService.get().getAffixes(item)` |
| Add triggered effect proc | Add `TriggeredEffects` to affix JSON |

## Documentation References

- [Affix System Overview](../../../Modding_Doc/Affixes/README.md) — Concepts, JSON schemas, best practices
- [Affix API Reference](../../../Modding_Doc/Affixes/API.md) — Complete programmatic API

## Key Concepts

### Multi-Stat Affixes

Each affix tier contains a `Stats` map with independent value ranges per stat:

```json
"Stats": {
  "hyforged:strength": { "MinValue": 45, "MaxValue": 55, "StackType": "FLAT" },
  "hyforged:max-health": { "MinValue": 100, "MaxValue": 150, "StackType": "FLAT" }
}
```

### Pool-Based Targeting

Affixes don't define what items they appear on. Instead, **pools** control targeting:
- Pools list which affix IDs are available
- Pools specify categories/tags they apply to
- Same affix can appear in multiple pools

### Triggered Effect Affixes (Procs)

Affixes can include triggered effects in addition to stat modifiers. This uses a unified model—no separate affix class.

- Add `TriggeredEffects` to the affix JSON definition. Each entry includes a `Trigger`, an `Effect`, and optional stacking/cooldown fields.
- Trigger types (string-based): `on_hit`, `on_damaged`, `on_kill`, `interval`, `on_cast`, `on_block`.
- Effect types (string-based): `spawn_projectile`, `spawn_prefab`, `apply_effect`, `damage_area`, `run_interaction`, `modify_stat`.
- Runtime state is tracked by `HyforgedActiveEffectsComponent` and rebuilt via `ActiveEffectInitializer` for equipment and NPC quality sources.
- Events: `EffectAffixTriggeredEvent` (cancellable) and `EffectAffixExecutedEvent`.

**Source Type Constants:**
When extending the active effect system, use these constants from `ActiveEffectInitializer`:
- `ActiveEffectInitializer.SOURCE_EQUIPMENT` — Effects from equipped items
- `ActiveEffectInitializer.SOURCE_NPC_QUALITY` — Effects from NPC quality affixes

**Minimal Example:**
```json
{
  "Id": "hyforged:blazing_wrath",
  "Type": "suffix",
  "DisplayName": "of Blazing Wrath",
  "Weight": 50,
  "Tiers": [
    {
      "Tier": 1,
      "ItemLevelReq": 40,
      "Weight": 25,
      "Stats": {
        "hyforged:fire_damage_increased_bps": { "MinValue": 500, "MaxValue": 1500, "StackType": "INCREASED" }
      }
    }
  ],
  "TriggeredEffects": [
    {
      "Trigger": { "Type": "on_hit", "Chance": 1500, "DamageCauses": ["Fire"] },
      "Effect": { "Type": "spawn_projectile", "ProjectileId": "hyforged:orbiting_flame", "Count": 3, "Pattern": "orbit", "Duration": 5.0 },
      "StackBehavior": "shared",
      "MaxStacks": 1,
      "CooldownSeconds": 2.0,
      "SharedCooldownGroup": "fire_procs"
    }
  ]
}
```

---

## Implementation Workflows

### Workflow 1: Add a New Affix (JSON)

Use this for data-driven affixes that don't require custom logic.

**Step 1: Create the affix definition file**

Location: `src/main/resources/Server/Hyforged/Affixes/<Type>/<AffixName>.json`

Organize by affix type:
- `Prefix/` - Descriptive adjectives (e.g., "Sturdy", "Vicious", "Flaming")
- `Suffix/` - "of the X" patterns (e.g., "of the Bear", "of Vitality")
- `Forged/` - Corrupted/hidden affixes (best-tier, require Epic+ quality)

**Single-Stat Example:**
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

**Multi-Stat Example:**
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

**Step 2: Add to an affix pool**

Edit or create `src/main/resources/Server/Hyforged/AffixPools/<PoolName>.json`:

```json
{
  "Priority": 100,
  "AppliesTo": {
    "Categories": ["Items.Armor"],
    "Tags": ["Type:Armor"]
  },
  "Prefixes": ["Sturdy", "YourNewAffix"],
  "Suffixes": ["OfTheBear"],
  "Forged": []
}
```

**Key Decisions:**
- `Type`: Choose `prefix`, `suffix`, or `forged` (displayed in separate tooltip section)
- `StackType`: `FLAT` adds flat value, `INCREASED` is additive %, `MORE` is multiplicative %
- `Weight`: Higher = more common. Default baseline is 100.
- Tier 1 is BEST, higher tiers are weaker
- Values use basis points for percentages: 10000 = 100%

> **Display Note:** Affixes are displayed in PoE-style in the item tooltip, NOT in the item name:
> ```
> [T1] +52 Strength (45-55)
> [T1] +125 Max Health (100-150)
> ```
> Players can see the rolled value and the possible range for that tier.

---

### Workflow 2: Create a New Affix Pool

Pools determine which affixes can appear on which items.

**Step 1: Identify item targeting**

Determine what items this pool applies to:
- `Categories`: Broad categories like `Items.Weapons`, `Items.Armor`
- `Tags`: Specific tags like `Type:Sword`, `Type:Heavy`

**Step 2: Create pool file**

Location: `src/main/resources/Server/Hyforged/AffixPools/<PoolName>.json`

```json
{
  "Priority": 100,
  "AppliesTo": {
    "Categories": ["Items.Weapons"],
    "Tags": ["Type:Sword"]
  },
  "Prefixes": ["Sharp", "Vicious", "Flaming"],
  "Suffixes": ["OfSlaying", "OfTheWarrior"],
  "Forged": ["CorruptedFury"]
}
```

**Pool Selection Rules:**
- Multiple pools can match an item
- Higher `Priority` pools are checked first
- All matching pools' affixes are combined into the selection pool

---

### Workflow 3: Register Affix Programmatically

Use when affixes need runtime logic or are defined by plugin code.

**Location:** Your plugin's `setup()` method

```java
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.model.*;
import reign.software.hyforged.stats.model.StatId;
import reign.software.hyforged.stats.model.HyforgedModifier.StackType;

@Override
public void setup(SetupContext context) {
    AffixService service = AffixService.get();
    
    // Create tier stats
    Map<String, AffixTierStat> tier1Stats = Map.of(
        "yourmod:fire-damage", new AffixTierStat(
            StatId.of("yourmod", "fire-damage"),
            StackType.FLAT,
            15, 20  // MinValue, MaxValue
        )
    );
    
    Map<String, AffixTierStat> tier2Stats = Map.of(
        "yourmod:fire-damage", new AffixTierStat(
            StatId.of("yourmod", "fire-damage"),
            StackType.FLAT,
            10, 14
        )
    );
    
    AffixDefinition customAffix = new AffixDefinition(
        "yourmod:blazing",
        "prefix",
        "Blazing",
        List.of(
            new AffixTierDefinition(1, 40, 50, tier1Stats),  // T1: ilvl 40, weight 50
            new AffixTierDefinition(2, 20, 75, tier2Stats)   // T2: ilvl 20, weight 75
        ),
        100  // Selection weight
    );
    
    service.registerAffix(customAffix);
}
```

---

### Workflow 4: Roll Affixes on an Item

**Random Rolling:**
```java
AffixService service = AffixService.get();
ItemStack itemWithAffixes = service.rollAffixes(item);
```

**Deterministic Rolling (for testing):**
```java
ItemStack itemWithAffixes = service.rollAffixes(item, 12345L); // Seed
```

**Roll Factors:**
- **Quality** → Determines capacity (Common=0, Legendary=2 prefix + 2 suffix + 1 forged)
- **Item Level** → Determines available tiers
- **Categories/Tags** → Determines which pool(s) apply

---

### Workflow 5: Create Item with Specific Affixes

Use `AffixSpec` to bypass random rolling:

```java
import reign.software.hyforged.affix.api.AffixSpec;

ItemStack craftedItem = service.createWithAffixes(
    "Items.Weapons.Sword",
    List.of(
        AffixSpec.of("hyforged:sturdy", 2),   // T2
        AffixSpec.of("hyforged:of-the-bear", 1), // T1
        AffixSpec.of("hyforged:sharp")         // Random tier
    )
);
```

---

### Workflow 6: Query Affixes on Items

```java
AffixService service = AffixService.get();

// Check if item has affixes
if (service.hasAffixes(item)) {
    List<RolledAffix> affixes = service.getAffixes(item);
    
    for (RolledAffix affix : affixes) {
        String id = affix.affixId();
        int tier = affix.tier();
        
        // Multi-stat support
        for (var entry : affix.rolledStats().entrySet()) {
            String statId = entry.getKey();
            int value = entry.getValue().value();
            StackType stackType = entry.getValue().stackType();
        }
    }
}
```

---

### Workflow 7: Modify Existing Item Affixes

```java
AffixService service = AffixService.get();

// Add an affix
ItemStack updated = service.addAffix(item, AffixSpec.of("hyforged:sturdy", 1));

// Remove an affix by ID
ItemStack withoutSturdy = service.removeAffix(item, "hyforged:sturdy");

// Clear all affixes
ItemStack clean = service.clearAffixes(item);
```

---

### Workflow 8: Custom Quality Capacity Rules

Override affix capacity for a quality tier:

Location: `src/main/resources/Server/Hyforged/QualityAffixRules/<Quality>.json`

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

---

## Common Patterns

### Pattern: Single-Stat Weapon Damage Affix

```json
{
  "Id": "hyforged:razor-sharp",
  "Type": "prefix",
  "DisplayName": "Razor Sharp",
  "Weight": 80,
  "Tiers": [
    {
      "Tier": 1,
      "ItemLevelReq": 50,
      "Weight": 40,
      "Stats": {
        "hyforged:physical-damage-increased-bps": { "MinValue": 2500, "MaxValue": 3500, "StackType": "INCREASED" }
      }
    },
    {
      "Tier": 2,
      "ItemLevelReq": 30,
      "Weight": 60,
      "Stats": {
        "hyforged:physical-damage-increased-bps": { "MinValue": 1500, "MaxValue": 2400, "StackType": "INCREASED" }
      }
    }
  ]
}
```

### Pattern: Multi-Stat Defensive Suffix

```json
{
  "Id": "hyforged:of-the-fortress",
  "Type": "suffix",
  "DisplayName": "of the Fortress",
  "Weight": 70,
  "Tiers": [
    {
      "Tier": 1,
      "ItemLevelReq": 60,
      "Weight": 35,
      "Stats": {
        "hyforged:armor": { "MinValue": 80, "MaxValue": 120, "StackType": "FLAT" },
        "hyforged:max-health": { "MinValue": 50, "MaxValue": 80, "StackType": "FLAT" }
      }
    }
  ]
}
```

### Pattern: Rare Corrupted Forged Affix

```json
{
  "Id": "hyforged:corrupted-fury",
  "Type": "forged",
  "DisplayName": "Corrupted Fury",
  "Weight": 25,
  "Tiers": [
    {
      "Tier": 1,
      "ItemLevelReq": 78,
      "Weight": 25,
      "Stats": {
        "hyforged:physical-damage-increased-bps": { "MinValue": 2000, "MaxValue": 2800, "StackType": "MORE" }
      }
    },
    {
      "Tier": 2,
      "ItemLevelReq": 58,
      "Weight": 50,
      "Stats": {
        "hyforged:physical-damage-increased-bps": { "MinValue": 1400, "MaxValue": 2000, "StackType": "MORE" }
      }
    }
  ]
}
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Affix not appearing | Not in any pool | Add affix ID to a pool's prefixes/suffixes/forged array |
| Wrong tier rolling | Item level too low | Check `ItemLevelReq` in tier definitions |
| Affix on wrong items | Pool `AppliesTo` too broad | Narrow categories/tags in pool |
| Affix too common/rare | Weight imbalance | Adjust `Weight` relative to other affixes |
| Stats not applying | Stat ID doesn't exist | Verify stat is registered in Stats system |

---

## Checklist: Adding a New Affix

- [ ] Affix ID uses namespace (`hyforged:` or `yourmod:`)
- [ ] Each tier has a `Stats` map with per-stat MinValue/MaxValue/StackType
- [ ] Tier values make sense (T1 = best, progressively weaker)
- [ ] Item level requirements create meaningful progression
- [ ] Weight is balanced relative to pool
- [ ] Affix is added to at least one pool
- [ ] All stat IDs exist in the Stats system
- [ ] Values use basis points for percentages (10000 = 100%)
