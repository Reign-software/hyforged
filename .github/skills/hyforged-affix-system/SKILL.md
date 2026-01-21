---
name: hyforged-affix-system
description: Implements ARPG-style affixes for equipment in Hyforged. Use when adding new affixes, creating affix pools, modifying affix eligibility, rolling affixes on items, or working with AffixService, AffixDefinition, RolledAffix, AffixPool, or AffixSpec. Also use when deriving guidance from Modding_Doc/Affixes. Triggers - affix, affixes, prefix, suffix, forged, item modifiers, equipment stats, modding doc.
---

# Hyforged Affix System

This skill provides step-by-step guidance for implementing affix features in Hyforged.

## Quick Reference

| Task | Approach |
|------|----------|
| Add a new affix | JSON in `src/main/resources/Server/Hyforged/Affixes/` |
| Create affix pool | JSON in `src/main/resources/Server/Hyforged/AffixPools/` |
| Custom affix at runtime | `AffixService.get().registerAffix(...)` |
| Roll affixes on item | `AffixService.get().rollAffixes(item)` |
| Query item affixes | `AffixService.get().getAffixes(item)` |

## Documentation References

- [Affix System Overview](../../../Modding_Doc/Affixes/README.md) — Concepts, JSON schemas, best practices
- [Affix API Reference](../../../Modding_Doc/Affixes/API.md) — Complete programmatic API

## Doc-Derived How-To (Adding Affixes)

Use this as the high-level checklist when translating Modding_Doc into actionable steps.

1. Define the affix in `src/main/resources/Server/<YourMod>/Affixes/` using a namespaced `id`, tiers, and eligibility rules.
2. Add the affix ID to one or more pools in `src/main/resources/Server/<YourMod>/AffixPools/` with appropriate categories/tags.
3. (Optional) Add quality capacity overrides in `src/main/resources/Server/<YourMod>/QualityAffixRules/` for new rarities.
4. (Optional) Register affixes, pools, or types via `AffixService` for runtime-only or dynamic content.
5. Validate using deterministic rolling (seeded) or debug commands before shipping.

Notes:
- Keep everything data-driven and namespaced; avoid hard-coded values.
- Ensure target stats exist in the Stats system.

---

## Implementation Workflows

### Workflow 1: Add a New Affix (JSON)

Use this for data-driven affixes that don't require custom logic.

**Step 1: Create the affix definition file**

Location: `src/main/resources/Server/Hyforged/Affixes/<AffixName>.json`

```json
{
  "id": "hyforged:<affix-id>",
  "type": "prefix",
  "displayName": "Display Name",
  "statId": "hyforged:<stat-id>",
  "modifierType": "FLAT",
  "weight": 1000,
  "tiers": [
    { "tier": 1, "minValue": 15, "maxValue": 20, "itemLevelRequirement": 40 },
    { "tier": 2, "minValue": 10, "maxValue": 14, "itemLevelRequirement": 25 },
    { "tier": 3, "minValue": 5, "maxValue": 9, "itemLevelRequirement": 1 }
  ],
  "eligibility": {
    "itemCategories": ["weapon"],
    "itemTags": [],
    "excludeTags": [],
    "minQuality": "Uncommon",
    "maxQuality": null
  }
}
```

**Step 2: Add to an affix pool**

Edit or create `src/main/resources/Server/Hyforged/AffixPools/<PoolName>.json`:

```json
{
  "id": "hyforged:<pool-id>",
  "weight": 100,
  "appliesTo": {
    "itemCategories": ["weapon"],
    "itemTags": ["sword"]
  },
  "prefixes": ["hyforged:<your-new-affix-id>"],
  "suffixes": [],
  "forged": []
}
```

**Key Decisions:**
- `type`: Choose `prefix` (before name), `suffix` (after name), or `forged` (hidden)
- `modifierType`: `FLAT` adds flat value, `INCREASED` is additive %, `MORE` is multiplicative %
- `weight`: Higher = more common. Default baseline is 1000.
- Tier 1 is BEST, higher tiers are weaker

---

### Workflow 2: Create a New Affix Pool

Pools determine which affixes can appear on which items.

**Step 1: Identify item targeting**

Determine what items this pool applies to:
- `itemCategories`: Broad categories like `weapon`, `armor`, `accessory`
- `itemTags`: Specific tags like `sword`, `heavy`, `magic`

**Step 2: Create pool file**

Location: `src/main/resources/Server/Hyforged/AffixPools/<PoolName>.json`

```json
{
  "id": "hyforged:<pool-id>",
  "weight": 100,
  "appliesTo": {
    "itemCategories": ["<category>"],
    "itemTags": ["<tag>"]
  },
  "prefixes": ["hyforged:affix1", "hyforged:affix2"],
  "suffixes": ["hyforged:affix3"],
  "forged": []
}
```

**Pool Selection Rules:**
- Multiple pools can match an item
- Higher `weight` pools are preferred
- All matching pools' affixes are combined into the selection pool

---

### Workflow 3: Register Affix Programmatically

Use when affixes need runtime logic or are defined by plugin code.

**Location:** Your plugin's `setup()` method

```java
import reign.software.hyforged.affix.api.AffixService;
import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixTierDefinition;
import reign.software.hyforged.affix.model.AffixEligibility;
import reign.software.hyforged.stats.model.StatId;
import reign.software.hyforged.stats.model.HyforgedModifier;

@Override
public void setup(SetupContext context) {
    AffixService service = AffixService.get();
    
    AffixDefinition customAffix = new AffixDefinition(
        "yourmod:blazing",                           // Namespaced ID
        "prefix",                                     // Type
        "Blazing",                                    // Display name
        StatId.of("yourmod", "fireDamage"),          // Target stat
        HyforgedModifier.StackType.FLAT,             // Modifier type
        List.of(
            new AffixTierDefinition(1, 15, 20, 40),  // T1: 15-20, requires ilvl 40
            new AffixTierDefinition(2, 10, 14, 25), // T2: 10-14, requires ilvl 25
            new AffixTierDefinition(3, 5, 9, 1)      // T3: 5-9, requires ilvl 1
        ),
        AffixEligibility.ANY,                        // Can appear on any equipment
        1000                                          // Selection weight
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
- **Quality** → Determines capacity (Common=0, Legendary=4 prefix + 4 suffix)
- **Item Level** → Determines eligible tiers
- **Categories/Tags** → Determines which pool(s) apply

---

### Workflow 5: Create Item with Specific Affixes

Use `AffixSpec` to bypass random rolling:

```java
import reign.software.hyforged.affix.api.AffixSpec;

ItemStack craftedItem = service.createWithAffixes(
    "Items.Weapons.Sword",
    List.of(
        AffixSpec.of("hyforged:sturdy", 2, 35),  // Exact: T2 with value 35
        AffixSpec.of("hyforged:of-the-bear", 1), // T1, random value in range
        AffixSpec.of("hyforged:sharp")            // Random tier and value
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
        int value = affix.value();
        StatId stat = affix.statId();
    }
}
```

---

### Workflow 7: Modify Existing Item Affixes

```java
AffixService service = AffixService.get();

// Add an affix
ItemStack updated = service.addAffix(item, AffixSpec.of("hyforged:sturdy", 1, 60));

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
  "quality": "Mythic",
  "affixCapacity": {
    "prefix": 3,
    "suffix": 3,
    "forged": 2
  }
}
```

---

## Common Patterns

### Pattern: Weapon-Only Damage Affix

```json
{
  "id": "hyforged:razor-sharp",
  "type": "prefix",
  "displayName": "Razor Sharp",
  "statId": "hyforged:physicalDamage",
  "modifierType": "INCREASED",
  "weight": 800,
  "tiers": [
    { "tier": 1, "minValue": 25, "maxValue": 35, "itemLevelRequirement": 50 },
    { "tier": 2, "minValue": 15, "maxValue": 24, "itemLevelRequirement": 30 },
    { "tier": 3, "minValue": 8, "maxValue": 14, "itemLevelRequirement": 10 }
  ],
  "eligibility": {
    "itemCategories": ["weapon"],
    "itemTags": [],
    "excludeTags": ["magic", "staff"],
    "minQuality": "Rare"
  }
}
```

### Pattern: Armor Defense Suffix

```json
{
  "id": "hyforged:of-iron-skin",
  "type": "suffix",
  "displayName": "of Iron Skin",
  "statId": "hyforged:armor",
  "modifierType": "FLAT",
  "weight": 1000,
  "tiers": [
    { "tier": 1, "minValue": 50, "maxValue": 75, "itemLevelRequirement": 40 },
    { "tier": 2, "minValue": 30, "maxValue": 49, "itemLevelRequirement": 20 },
    { "tier": 3, "minValue": 15, "maxValue": 29, "itemLevelRequirement": 1 }
  ],
  "eligibility": {
    "itemCategories": ["armor"],
    "itemTags": []
  }
}
```

### Pattern: Rare Legendary-Only Forged Affix

```json
{
  "id": "hyforged:soulbound",
  "type": "forged",
  "displayName": "Soulbound",
  "statId": "hyforged:allStats",
  "modifierType": "INCREASED",
  "weight": 50,
  "tiers": [
    { "tier": 1, "minValue": 10, "maxValue": 15, "itemLevelRequirement": 60 }
  ],
  "eligibility": {
    "minQuality": "Legendary",
    "maxQuality": "Legendary"
  }
}
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Affix not appearing | Not in any pool | Add affix ID to a pool's prefixes/suffixes/forged array |
| Wrong tier rolling | Item level too low | Check `itemLevelRequirement` in tier definitions |
| Affix on wrong items | Pool `appliesTo` too broad | Narrow categories/tags or use eligibility filters |
| Affix too common/rare | Weight imbalance | Adjust `weight` relative to other affixes (baseline: 1000) |

---

## Checklist: Adding a New Affix

- [ ] Affix ID uses namespace (`hyforged:` or `yourmod:`)
- [ ] Tier values make sense (T1 = best, progressively weaker)
- [ ] Item level requirements create meaningful progression
- [ ] Weight is balanced relative to pool
- [ ] Eligibility filters prevent appearing on wrong items
- [ ] Affix is added to at least one pool
- [ ] Stat ID exists in the Stats system
