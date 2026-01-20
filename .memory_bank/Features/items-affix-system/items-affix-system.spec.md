# Feature Spec: Items Affix System

## Metadata
- Feature ID (slug): items-affix-system
- Status: Draft
- Owner: JBurl
- Date: 2026-01-20

## Summary
Extend items with an ARPG-style affix system (prefixes, suffixes, and forged affixes) that produces meaningful, readable loot variety. The system integrates with Hytale's existing Quality tiers and uses `ItemStack.metadata` for affix storage. Affixes are fully data-driven, leverage the Stats System modifier model, and are displayed in inventory tooltips via custom UI.

## Goals
- **Use Hytale's Quality System**: Leverage existing Quality tiers (Common, Uncommon, Rare, Epic, Legendary) to determine affix capacity.
- **Data-Driven Affix Types**: Affix types (prefix, suffix, forged, etc.) are defined in JSON; new types can be added by plugins without code changes.
- **Data-Driven Affix Capacity**: Each Quality tier has a corresponding JSON file defining how many affixes of each type an item can have.
- **Data-Driven Affix Definitions**: Affix pools defined per item type/category with tier-based value ranges.
- **Tier 1 = Best**: Affix tiers follow ARPG convention where Tier 1 is the strongest roll.
- **Item Level Based**: Affix eligibility and tier availability based on item's `ItemLevel` field.
- **Hytale Data Format**: Store affixes in `ItemStack.metadata` using BSON/JSON for persistence and network sync.
- **Stats Integration**: Affixes apply modifiers using the existing Hyforged Stats System (`HyforgedModifier`).
- **UI Integration**: Display affixes in item tooltips and a character stats screen.
- **Loot Integration**: Hook into Hytale's loot system via event listeners to roll affixes on item drops.
- **Extensibility**: All affix types, capacities, definitions, and pools are configurable via JSON; Hyforged ships sensible defaults that servers can override or extend.

## Non-Goals
- **Unique Items**: Special fixed-affix items are out of scope for this spec.
- **Set Bonuses**: No set bonus system.
- **Forging Mechanic**: The forging UI/process is deferred; only the `forged` affix type is defined here.
- **Custom Item Rendering**: No visual changes to item models based on affixes.
- **Replacing Base Item System**: This layers affixes on top of existing items.

## User Experience

### Loot Drops
1. Player defeats an enemy or opens a container.
2. Hytale's loot system generates an item drop.
3. Hyforged intercepts the drop event and rolls affixes based on:
   - Item's Quality tier → affix capacity
   - Item's `ItemLevel` → eligible affix tiers
   - Item's type/category → eligible affix pool
4. Affixes are stored in `ItemStack.metadata` and the item drops with its affixes.

### Inventory Tooltips
1. Player hovers over an item in inventory.
2. Tooltip displays:
   - Item name with prefix/suffix decorations (e.g., "Sturdy Adamantite Axe of the Bear")
   - Quality tier (using existing Quality colors/labels)
   - Base item stats
   - Affix lines grouped by type (prefixes, suffixes, forged)
   - Each affix shows: name, tier indicator (T1-T5), and stat modifier value

### Character Stats Screen
1. Player opens character screen (new UI page).
2. Screen displays:
   - All character stats with breakdowns by source
   - Equipment slots with equipped item affix summaries
   - Total modifier contributions from affixes

## Functional Requirements

### FR-1: Affix Type Definitions
- Affix types are fully data-driven and defined in JSON at `Server/Hyforged/AffixTypes/`.
- One file per affix type (e.g., `Prefix.json`, `Suffix.json`, `Forged.json`).
- Hyforged ships default types; other plugins can add new types via additional JSON files.
- Registry behavior on duplicate type IDs: accept the latest entry by load order and log at WARN to highlight overrides.
- Each type defines:
  - `id`: Unique identifier (e.g., `"prefix"`, `"suffix"`, `"forged"`)
  - `displayNamePosition`: `"before"` | `"after"` | `"none"` (relative to item name)
  - `displayFormat`: Template for tooltip display (e.g., `"{name} (T{tier})"`)
  - `stackable`: Whether multiple affixes of this type can coexist (prefixes: yes, forged: no)
- Example `Prefix.json`:
  ```json
  {
    "id": "prefix",
    "displayNamePosition": "before",
    "displayFormat": "{name}",
    "stackable": true
  }
  ```

### FR-2: Quality Affix Capacity
- Affix capacities per Quality are defined in JSON at `Server/Hyforged/QualityAffixRules/`.
- One file per Quality tier (e.g., `Common.json`, `Rare.json`, `Legendary.json`).
- Mirrors Hytale's pattern (`Server/Item/Qualities/`) for consistency.
- Hyforged ships default rules; servers can override or extend with custom Quality rules.
- Example `Common.json`:
  ```json
  {
    "quality": "Common",
    "affixCapacity": {
      "prefix": 1,
      "suffix": 0,
      "forged": 0
    }
  }
  ```
- Example `Legendary.json`:
  ```json
  {
    "quality": "Legendary",
    "affixCapacity": {
      "prefix": 4,
      "suffix": 4,
      "forged": 0
    }
  }
  ```
- Default capacities:
  | Quality | Prefix | Suffix | Forged |
  |---------|--------|--------|--------|
  | Common | 1 | 0 | 0 |
  | Uncommon | 1 | 1 | 0 |
  | Rare | 2 | 2 | 0 |
  | Epic | 3 | 3 | 0 |
  | Legendary | 4 | 4 | 0 |
- `forged` capacity is 0 by default; forging mechanic sets it to 1 when applied.
- Non-equipment Qualities (Junk, Tool, Technical, Template, Debug, Developer) have no rules file (treated as 0 capacity).

### FR-3: Affix Definitions
- Affixes are defined in JSON at `Server/Hyforged/Affixes/`.
- Each affix definition includes:
  - `id`: Unique identifier (e.g., `"sturdy"`, `"of-the-bear"`)
  - `type`: Affix type reference (`"prefix"`, `"suffix"`, `"forged"`)
  - `displayName`: Localization key for the affix name
  - `statId`: The Hyforged stat this affix modifies
  - `modifierType`: Modifier stack type (`FLAT`, `INCREASED`, `MORE`)
  - `tiers`: Array of tier definitions (T1 = best, higher numbers = weaker)
  - `eligibility`: Constraints for where this affix can appear
  - `weight`: Base selection weight for rolling
- Registry behavior on duplicate affix IDs: accept the latest entry by load order and log at WARN to highlight overrides.

#### Tier Definition
```json
{
  "tier": 1,
  "minValue": 50,
  "maxValue": 75,
  "itemLevelReq": 40
}
```

#### Eligibility Constraints
```json
{
  "itemCategories": ["Items.Weapons", "Items.Armor"],
  "itemTags": ["Type:Weapon", "Family:Axe"],
  "excludeTags": ["Tool"],
  "minQuality": "Rare",
  "maxQuality": "Legendary"
}
```

### FR-4: Affix Tier Templates in Stat Definitions
- Stat definitions (`Server/Hyforged/Stats/`) can include affix tier templates.
- This allows stats to define default tier progressions reusable across affixes.
- Affix definitions may reference the stat template; explicit affix tiers override template values.
- Format (optional extension to stat definition):
  ```json
  {
    "id": "hyforged:strength",
    "affixTierTemplate": {
      "tierCount": 5,
      "t1Range": [8, 10],
      "t5Range": [1, 2],
      "scalingCurve": "linear"
    }
  }
  ```

### FR-5: Affix Pools per Item Type
- Affix pools define which affixes can appear on which item types.
- Pools are defined in JSON at `Server/Hyforged/AffixPools/`.
- Pools can be referenced by item category or tag pattern.
- Registry behavior on duplicate pool IDs: accept the latest entry by load order and log at WARN to highlight overrides.
- Format:
  ```json
  {
    "id": "weapon-melee",
    "appliesTo": {
      "categories": ["Items.Weapons"],
      "tags": ["Type:Weapon"]
    },
    "prefixes": ["sturdy", "sharp", "mighty", ...],
    "suffixes": ["of-the-bear", "of-precision", ...],
    "forged": ["legendary-might", ...]
  }
  ```

### FR-6: Affix Storage on Items
- Rolled affixes are stored in `ItemStack.metadata` under a `"Hyforged"` key.
- Format:
  ```json
  {
    "Hyforged": {
      "schemaVersion": 1,
      "affixes": [
        {
          "id": "sturdy",
          "type": "prefix",
          "tier": 2,
          "value": 35,
          "statId": "hyforged:armor"
        },
        {
          "id": "of-the-bear",
          "type": "suffix",
          "tier": 1,
          "value": 10,
          "statId": "hyforged:strength"
        }
      ]
    }
  }
  ```
- A `BuilderCodec<HyforgedItemData>` handles serialization.
- Schema versioning is required for future migrations; unrecognized versions are ignored with warnings.

### FR-7: Affix Rolling
- `AffixRollerService` generates affixes for items.
- Rolling process:
  1. Determine item's Quality → get affix capacities per type.
  2. Determine item's `ItemLevel` → filter eligible tiers.
  3. Determine item's categories/tags → select applicable affix pool.
  4. For each affix type, roll affixes up to capacity:
     a. Filter eligible affixes by tier and constraints.
     b. Select affix using weighted random.
  c. Select tier using weighted random (higher tiers more common by default).
     d. Roll value within tier's range.
     e. Record rolled affix; exclude duplicates.
- Supports deterministic rolling with seed for debugging.
- Pool resolution is deterministic: use highest priority pool when multiple pools match; ties resolve by lexicographic `id`.
- Duplicate exclusion is by `affixId` and `statId` (one affix per `affixId`, and one affix per `statId` unless explicitly allowed by type).
- Tier weights default to a linear curve favoring lower tier numbers; affixes can override tier weights per tier.

### FR-8: Loot System Integration
- Register event listener for Hytale's item drop/creation events.
- On item creation:
  1. Check if item is eligible for affixes (equipment tag/category list, valid Quality).
  2. Call `AffixRollerService` to generate affixes.
  3. Update `ItemStack` with affix metadata.
- Loot source context (mob difficulty, region) can influence:
  - Quality tier chances (handled by Hytale loot tables).
  - Affix tier weight bonuses (passed to roller).

### FR-9: Affix Stat Application
- When equipment is equipped, `EquipmentAffixSystem` (ECS system):
  1. Reads affixes from `ItemStack.metadata`.
  2. Creates `HyforgedModifier` instances for each affix.
  3. Applies modifiers to the entity's `HyforgedStatComponent` with source `"equipment:{slot}:{affixId}"`.
- When equipment is unequipped:
  1. Removes modifiers matching the source pattern.
- Recalculation is triggered via existing stat dirty-flag model.

### FR-10: Item Name Generation
- Item display name incorporates prefix and suffix affix names.
- Format: `"{prefix} {baseName} {suffix}"`.
- Multiple prefixes/suffixes concatenated with commas or styled.
- Example: "Sturdy, Mighty Adamantite Axe of the Bear, of Precision".
- Forged affixes do not modify item name; shown in tooltip only.

### FR-11: Tooltip UI
- Extend item tooltips to display affix information.
- Tooltip sections:
  1. Item name (with prefix/suffix integrated)
  2. Quality label (existing)
  3. Base stats (existing)
  4. Affix section header: "Affixes"
  5. Each affix: `"[T{tier}] {name}: +{value} {statName}"`
  6. Forged section (if applicable): "Forged: {forged affix details}"
- Affix tiers color-coded (T1 = gold, T2 = purple, T3 = blue, T4 = green, T5 = white).

### FR-12: Character Stats Screen
- New UI page accessible via hotkey or menu.
- Displays:
  - Character level and class
  - All stats organized by category
  - Each stat shows: base value, modifiers breakdown, effective value
  - Equipment overview with affix summaries
- Uses Hytale's `.ui` file format and `InteractiveCustomUIPage`.

## Non-Functional Requirements

### Performance
- Affix rolling is bounded: max 8 affixes per item (4 prefix + 4 suffix).
- Affix data is compact: ~200 bytes per item in metadata.
- Stat recalculation batched per tick (existing model).

### Reliability
- Server is authoritative for affix generation and stat calculation.
- Client tooltip displays server-provided affix data from metadata.
- Invalid/corrupted affix data gracefully ignored with logging.

### Extensibility
- New affix types addable via JSON without code changes.
- New affixes addable via JSON without code changes.
- API for other plugins to:
  - Register custom affixes
  - Query item affixes
  - Create items with specific affixes
  - Modify affix pools

### Backward Compatibility
- Items without affix metadata treated as having no affixes.
- Migration path for existing items: no affixes by default.

## Dependencies
- **Stats System** (Phase 1): `HyforgedModifier`, stat definitions, modifier application.
- **Entity Stats** (Phase 2): `HyforgedStatComponent` for stat storage.
- **Hytale Quality System**: Existing Quality definitions and UI.
- **Hytale ItemStack**: Metadata storage and serialization.
- **Hytale Event System**: Loot/drop event listeners.
- **Hytale UI System**: Custom tooltip and page rendering.

## Data/Schema Impact

### New JSON Asset Types
| Path | Description |
|------|-------------|
| `Server/Hyforged/AffixTypes/*.json` | Affix type definitions (one per type) |
| `Server/Hyforged/Affixes/*.json` | Individual affix definitions (one per affix) |
| `Server/Hyforged/AffixPools/*.json` | Affix pools per item type (one per pool) |
| `Server/Hyforged/QualityAffixRules/*.json` | Capacity rules per Quality (one per Quality tier) |

### ItemStack Metadata Extension
- New `"Hyforged"` key in `ItemStack.metadata` containing affix array.

### Stat Definition Extension
- Optional `affixTierTemplate` field in stat definitions.

## API Changes

### New Public API
```java
// Query affixes on an item
List<RolledAffix> AffixService.getAffixes(ItemStack item);

// Roll affixes for an item (mutates item)
ItemStack AffixService.rollAffixes(ItemStack item, Random random);
ItemStack AffixService.rollAffixes(ItemStack item, long seed);

// Create item with specific affixes
ItemStack AffixService.createWithAffixes(String itemId, List<AffixSpec> affixes);

// Register custom affix (for other plugins)
void AffixRegistry.registerAffix(AffixDefinition affix);
void AffixRegistry.registerPool(AffixPool pool);
```

### Events
```java
// Emitted when affixes are rolled on an item
class AffixesRolledEvent {
  ItemStack item;
  List<RolledAffix> affixes;
  boolean cancelled; // Allow listeners to cancel/modify
}

// Emitted when affix modifiers are applied to an entity
class AffixModifiersAppliedEvent {
  Ref<EntityStore> entity;
  ItemStack item;
  List<HyforgedModifier> modifiers;
}
```

## Security/Privacy
- Server-authoritative: clients cannot spoof affixes.
- Affix data in metadata is validated on server before stat application.
- Invalid affix IDs or out-of-range values logged and rejected.

## Observability
- Log affix rolling results at DEBUG level.
- Log affix modifier application/removal at TRACE level.
- Metrics: affixes rolled per Quality tier, tier distribution.
- Debug command: `/hyforged affixes <player>` to dump equipped affixes.

## Risks

| Risk | Mitigation |
|------|------------|
| Affix bloat making tooltips unreadable | Limit max affixes; use compact display format |
| Unbalanced affix tier weights | Configuration-driven; tunable per affix/tier |
| Loot event integration fragility | Abstract listener; fallback to post-creation hook |
| UI complexity for character screen | Incremental delivery; start with stats-only view |

## Open Questions
- **Q1**: Should affix names be localized or use static English names initially?
  - **Proposal**: Use localization keys from day one for i18n support.
- **Q2**: How should duplicate stat modifiers from multiple affixes be displayed?
  - **Proposal**: Show each affix separately; effective stat shows combined total.
- **Q3**: Should the character screen show comparison when hovering equipment?
  - **Proposal**: Defer comparison feature to a future iteration.

## Acceptance Criteria
- [ ] Items with Quality Common-Legendary can roll affixes on drop.
- [ ] Affix count matches Quality tier capacity rules.
- [ ] Affix tiers respect item's `ItemLevel`.
- [ ] Affixes stored in `ItemStack.metadata` persist across save/load.
- [ ] Equipped items apply affix modifiers to character stats.
- [ ] Unequipping removes affix modifiers correctly.
- [ ] Item tooltips display affix names, tiers, and values.
- [ ] Character stats screen displays stat breakdowns including affix sources.
- [ ] Affix definitions loadable from JSON without code changes.
- [ ] API allows other plugins to register custom affixes.
- [ ] Tier 1 affixes are the strongest; higher tier numbers are weaker.

## Impacted Areas (High-Level)
- **Item System**: Metadata storage, tooltip generation.
- **Stats System**: Modifier application from equipment affixes.
- **Loot System**: Event listener for affix rolling on drops.
- **UI System**: Tooltip extension, new character screen.
- **Asset Loading**: New JSON asset types for affixes.

## Required Codebase/Architecture Changes (High-Level)

### New Components
- `AffixComponent` (or embedded in `HyforgedStatComponent`): Tracks equipped affix modifiers.

### New Systems
- `EquipmentAffixSystem`: Applies/removes affix modifiers on equip/unequip events.

### New Services
- `AffixRegistry`: Stores affix definitions, types, and pools.
- `AffixRollerService`: Rolls affixes for items.
- `AffixService`: Public API for affix queries and manipulation.

### New Assets
- JSON asset loaders for affix types, definitions, pools, and capacity rules.

### UI
- Tooltip extension for affix display.
- New `CharacterStatsPage` UI page.

### Existing Code Changes
- Remove placeholder `reign.software.hyforged.stats.affix` package (replace with new implementation).
- Update `HyforgedBridgeSystem` or `EquipmentAffixSystem` to handle equipment modifier sources.

## References
- Requirements: [Items: Affixes & Rarity](../../Requirements/rpg-arpg/items-affixes-rarity.md)
- Stats System Spec: [hyforged-stats-system.spec.md](../hyforged-stats-system/hyforged-stats-system.spec.md)
- Entity Stats Spec: [entity-stats.spec.md](../entity-stats/entity-stats.spec.md)
- ADR-0002: Extend Hytale Modifier System
- UI Modding Reference: [.doc/references/ui-modding.md](../../../.doc/references/ui-modding.md)
- Hytale Quality System: `lib/Server/Item/Qualities/`
