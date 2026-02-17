# Feature Spec: Random Item Quality System

## Metadata
- Feature ID (slug): random-item-quality
- Status: Draft
- Owner: JBurl
- Date: 2026-01-23

## Summary
Implement a data-driven system that assigns random Quality tiers to items when they spawn (from loot drops, chests, or other sources). This replaces the static Quality defined in item JSON with a dynamically rolled Quality based on configurable weight distributions. The system integrates with the existing affix system so that affixes are rolled based on the new Quality. Additionally, introduce Quality for NPCs/mobs to influence loot quality chances and enable elite/boss variants with affixes.

This spec also covers **Triggered Effect Affixes** (procs) - affixes that trigger gameplay effects rather than just modifying stats. Examples include spawning projectiles on hit, creating ground pools on kill, or periodic orbiting effects.

## Goals
- **Replace Static Quality**: Items receive a randomly rolled Quality at spawn time instead of using the hardcoded Quality from item definitions.
- **Data-Driven Weight Distributions**: Quality roll weights are fully configurable via JSON files, supporting:
  - Global default weights
  - Per-item-category weights (weapons, armor, etc.)
  - Per-loot-source weights (mob type, chest type, zone)
- **Leverage Hytale's Quality System**: Use Hytale's existing `ItemQuality` asset system (`ItemQuality.getAssetMap()`) to programmatically access all available qualities.
- **Context-Aware Rolling**: Quality chances can be influenced by:
  - Mob difficulty/level (higher level mobs drop higher quality more often)
  - World zone/region
  - Chest/container type
  - Player stats (e.g., Item Rarity stat)
  - NPC Quality tier (new concept)
- **NPC/Mob Quality Tiers**: Introduce Quality for NPCs to create elite/boss variants that:
  - Drop higher quality loot
  - Can have affixes applied (using the affix system)
  - Have scaled stats based on their quality
- **Triggered Effect Affixes**: Extend the affix system to support gameplay effects (procs) in addition to stat modifiers:
  - Unified affix model: a single affix can have stat modifiers, triggered effects, or both
  - On-hit, on-kill, on-damaged, interval, and on-cast triggers
  - Spawn projectiles, prefabs, apply effects, damage areas
  - No separate "effect affix" class - just additional fields on existing affix definitions
- **Separate System with Events**: Quality rolling runs as a separate ECS system before affix rolling, emitting game events for modding.
- **Foundation Mod Compatibility**: As a foundation mod, directly replace item quality rather than layering metadata overrides.

## Non-Goals
- **Custom Quality Tiers**: Not adding new Quality tiers beyond Hytale's existing ones (Junk, Common, Uncommon, Rare, Epic, Legendary, Tool, Template, Debug, Developer, Technical).
- **UI Changes**: No UI modifications; Hytale's existing Quality display (colors, tooltips, particles) applies automatically.
- **Unique/Named Items**: Unique items with fixed quality are out of scope.
- **Crafting Quality Selection**: Player-controlled quality selection during crafting is deferred.

## User Experience

### Loot Drops
1. Player defeats an enemy or opens a container.
2. Hytale's loot system generates an item drop (with static base Quality).
3. Hyforged's `LootQualitySystem` intercepts the item creation and:
   a. Looks up the applicable quality weight rules for the item/source context.
   b. Rolls a new Quality based on weighted random selection.
   c. Replaces the item's Quality with the rolled value.
4. Hyforged's `LootAffixSystem` then rolls affixes based on the new Quality.
5. The item spawns with its final Quality and affixes.

### Elite/Boss Mobs
1. Server spawns an NPC with a Hyforged Quality component (e.g., "Rare Goblin Duke").
2. The NPC's stats are scaled based on its Quality (via stat modifiers).
3. The NPC can have affixes (e.g., "Blazing Rare Goblin Duke").
4. When defeated, loot quality weights are boosted by the NPC's Quality tier.

### Quality Visibility
- Items display their Quality via Hytale's existing UI:
  - Slot texture/background
  - Tooltip texture and text color
  - Drop particle effects
  - Quality label in tooltip
- **Limitation**: Hytale's native UI reads quality from `Item.qualityId`, not our metadata override. Vanilla UI will show base quality; Hyforged tooltips/systems use effective quality.

## Technical Research Summary

### Hytale Quality Architecture (Researched 2026-01-23)

**ItemStack Quality Model:**
- Quality is **per-Item-definition**, not per-ItemStack instance
- `Item.java` → `qualityId` and `qualityIndex` resolved at asset load
- `ItemStack.java` → has NO quality field; uses `getItem().getQualityId()`
- `ItemWithAllMetadata` protocol → sends `itemId`, `quantity`, `durability`, `metadata` (no quality)
- `SortType.RARITY` → reads `i.getItem().getQualityIndex()` for sorting

**Key Source Files:**
- [ItemStack.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/inventory/ItemStack.java) - lines 251-268 (`toPacket()`)
- [Item.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/asset/type/item/config/Item.java) - `qualityId`, `qualityIndex`
- [SortType.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/entity/entities/player/Page.java) - `RARITY` comparator

**NPC Spawn Hooks:**
- `RoleBuilderSystem` is a `HolderSystem<EntityStore>` with `onEntityAdd()`
- Dependencies: `Order.AFTER, EntityStatsSystems.Setup.class`, `Order.AFTER, PhysicsValuesAddSystem.class`
- We can register after `RoleBuilderSystem` to add NPC quality component

**Key Source Files:**
- [RoleBuilderSystem.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/npc/systems/RoleBuilderSystem.java) - lines 59-100

**Container/Loot Flow:**
- `ItemModule.getRandomItemDrops(droplistId)` creates `List<ItemStack>`
- `ItemDropList.getContainer().populateDrops()` generates `ItemDrop` list
- `ItemComponent.generateItemDrop()` creates entity from ItemStack
- `LootAffixSystem` hooks `ItemComponent` addition via `RefChangeSystem`

**Key Source Files:**
- [ItemModule.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/modules/item/ItemModule.java) - lines 70-100
- [ItemContainerState.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/universe/world/meta/state/ItemContainerState.java)
- [ItemComponent.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/modules/entity/item/ItemComponent.java) - lines 224-250

## Functional Requirements

### FR-1: Quality Weight Configuration
- Quality weight profiles are defined in JSON at `Server/Hyforged/Quality/Weights/`.
- Each profile has a unique ID and defines weights for quality tiers.
- Profiles are **referenced by ID** from eligibility configs (FR-2) - no implicit matching.
- Format:
  ```json
  {
    "id": "default",
    "description": "Global default quality weights",
    "weights": {
      "Common": 500,
      "Uncommon": 300,
      "Rare": 150,
      "Epic": 40,
      "Legendary": 10
    },
    "eligibleQualities": ["Common", "Uncommon", "Rare", "Epic", "Legendary"]
  }
  ```
- `eligibleQualities` restricts which qualities can be rolled (e.g., armor can't roll "Tool" quality).
- Weights are relative; a quality with weight 0 or not listed is excluded.
- Weight profiles have no item matching logic - they are pure data referenced by eligibility configs.

### FR-2: Item Eligibility Configuration
- Define which items are eligible for random quality in JSON at `Server/Hyforged/Quality/Eligibility/`.
- Each eligibility config **explicitly references** a weight profile by ID.
- Eligibility rules match items by:
  - Categories (e.g., `"Items.Weapons"`, `"Items.Armor"`)
  - Tags (e.g., `"Type:Weapon"`, `"Material:Iron"`)
  - Explicit item ID patterns
- **Resolution Order**: First matching eligibility config wins (configs have priority field).
- Format:
  ```json
  {
    "id": "dungeon-weapons",
    "priority": 100,
    "description": "Weapons from dungeon loot with higher quality chances",
    "weightProfileId": "dungeon-loot",
    "appliesTo": {
      "categories": ["Items.Weapons"],
      "tags": ["Type:Weapon"]
    },
    "excludes": {
      "tags": ["Unique", "Quest"]
    },
    "sourceFilter": {
      "sourceTags": ["Dungeon", "Boss"],
      "excludeSourceTags": []
    }
  }
  ```
- `weightProfileId` references a weight profile from FR-1.
- `priority` determines evaluation order (higher = checked first).
- `sourceFilter` optionally restricts to specific loot sources (mob tags, chest tags, etc.).
- Items not matching any eligibility rule retain their base quality.

### FR-3: Quality Rolling Service
- `QualityRollerService` handles quality determination:
  ```java
  public interface QualityRollerService {
      String rollQuality(QualityRollContext context, Random random);
      String rollQuality(QualityRollContext context, long seed);
      List<String> getEligibleQualities(QualityRollContext context);
  }
  ```
- `QualityRollContext` contains:
  - Item ID, categories, tags
  - Loot source type (mob, chest, crafting, etc.)
  - Source entity reference (for mob level/quality)
  - Player reference (for Magic Find stat)
  - Zone/region identifier
- Supports deterministic rolling with seed for debugging.

### FR-4: Loot Quality System (ECS)
- `LootQualitySystem` extends `RefChangeSystem<EntityStore, ItemComponent>`.
- Triggers on `ItemComponent` addition (same hook point as `LootAffixSystem`).
- System execution order: `LootQualitySystem` runs BEFORE `LootAffixSystem` (via system dependencies).
- Process:
  1. Check if item is eligible for random quality.
  2. Build `QualityRollContext` from item and loot source.
  3. Roll quality via `QualityRollerService`.
  4. Replace item's quality (update `ItemStack` via `ItemComponent`).
  5. Emit `QualityRolledEvent`.

### FR-5: Quality Replacement on ItemStack
- **Research Complete**: Hytale's quality is per-Item-definition, not per-ItemStack.
- Key findings:
  - `Item.java` has `qualityId`/`qualityIndex` resolved at asset load time
  - `ItemStack.toPacket()` does NOT include quality (only itemId, quantity, durability, metadata)
  - `SortType.RARITY` reads quality via `i.getItem().getQualityIndex()` (from Item config)
  - No `setQuality()` or `withQuality()` methods exist on ItemStack
  
**Technical Approach: Hyforged Quality Layer**

Store quality override in `ItemStack.metadata` under a Hyforged key (e.g., `hyforged.quality`). All Hyforged systems read quality from metadata first, falling back to `Item.getQualityId()` if not present.

Implementation:
```java
public class HyforgedQualityService {
    private static final String QUALITY_KEY = "hyforged.quality";
    
    /**
     * Get effective quality, checking Hyforged override first.
     */
    public static String getEffectiveQuality(ItemStack itemStack) {
        String override = itemStack.getFromMetadataOrNull(QUALITY_KEY, Codec.STRING);
        if (override != null) {
            return override;
        }
        return itemStack.getItem().getQualityId();
    }
    
    /**
     * Set quality override in metadata.
     */
    public static ItemStack withQuality(ItemStack itemStack, String qualityId) {
        return itemStack.withMetadata(QUALITY_KEY, Codec.STRING, qualityId);
    }
}
```

**Trade-offs**:
- ✅ Works with server-only mod (no client changes needed)
- ✅ Hyforged systems (affixes, loot bonuses, stats) use overridden quality
- ⚠️ Vanilla Hytale UI displays base Item quality, not override
- ⚠️ Sorting by rarity uses base quality (Hytale's `SortType.RARITY`)

**Mitigation for UI limitation**:
- Accept that Hytale's native quality display shows base quality
- Future: If Hytale adds client modding, override UI rendering
- Consider: Tooltips via Hyforged could show effective quality

### FR-6: Game Events
- Emit events for modding integration:
  ```java
  public class QualityRolledEvent implements Cancellable {
      ItemStack item;
      String originalQuality;
      String rolledQuality;
      QualityRollContext context;
      boolean cancelled;
      
      // Listeners can modify rolledQuality or cancel
  }
  ```
- Event is dispatched BEFORE the quality is applied, allowing modification or cancellation.
- If cancelled, item retains its original quality.

### FR-7: Context Modifiers
- Quality weights can be modified by context factors.
- **Two-tier configuration**: Default global config + per-source overrides.

#### Default Modifier Config
- Global defaults defined in `Server/Hyforged/Quality/Modifiers/Default.json`:
  ```json
  {
    "id": "default",
    "description": "Global default quality modifiers",
    "levelScaling": {
      "enabled": true,
      "curveId": "hyforged:default_level_scaling",
      "qualityBonusPerLevel": {
        "Rare": 0.3,
        "Epic": 0.1,
        "Legendary": 0.02
      }
    },
    "itemRarity": {
      "enabled": true,
      "statId": "hyforged:item-rarity-increased-bps",
      "scalingFactor": 0.01,
      "maxBonus": 200,
      "fallbackValue": 0
    },
    "npcQualityBonus": {
      "enabled": true,
      "bonusPerTier": {
        "Uncommon": 10,
        "Rare": 25,
        "Epic": 50,
        "Legendary": 100
      }
    }
  }
  ```
- Applied to ALL quality rolls unless overridden.

#### Per-Source Modifier Overrides
- Eligibility configs can **override** specific modifiers:
  ```json
  {
    "id": "dungeon-boss-drops",
    "priority": 200,
    "weightProfileId": "boss-loot",
    "appliesTo": { "categories": ["Items.Weapons", "Items.Armor"] },
    "sourceFilter": { "sourceTags": ["Boss"] },
    "modifierOverrides": {
      "levelScaling": {
        "curveId": "hyforged:boss_level_scaling",
        "qualityBonusPerLevel": {
          "Rare": 0.5,
          "Epic": 0.2,
          "Legendary": 0.05
        }
      },
      "npcQualityBonus": {
        "bonusPerTier": {
          "Uncommon": 20,
          "Rare": 50,
          "Epic": 100,
          "Legendary": 200
        }
      }
    }
  }
  ```
- Only specified fields are overridden; unspecified fields use defaults.
- Set `"enabled": false` to disable a modifier for that source.

#### Modifier Resolution Order
1. Load default modifiers from `Default.json`
2. If eligibility config has `modifierOverrides`, merge over defaults
3. Apply merged modifiers to quality roll

#### Modifier Types

**Level Scaling**:
- Uses `ResponseCurve` assets for non-linear scaling
- `qualityBonusPerLevel` multiplied by curve output for entity level
- Example: Level 50 mob with curve output 0.5 → half the per-level bonus

**Item Rarity**:
- Reads `hyforged:item-rarity-increased-bps` stat from player via Hyforged stat system
- Stat scales with Luck (10 bps per point of Luck)
- `scalingFactor` converts stat value (in basis points) to weight bonus
- `maxBonus` caps total bonus from Item Rarity
- `fallbackValue` used if stat is not registered (modded out) - defaults to 0 (no bonus)
- Silent fallback: If stat lookup fails, use fallback value without logging (modding is expected)

**NPC Quality Bonus**:
- When loot source is an NPC with `HyforgedNPCQualityComponent`
- Adds flat bonus to higher quality weights based on NPC tier

### FR-8: NPC Quality Component
- **Research Complete**: NPC spawn hooks available via ECS system dependencies.
- Key findings:
  - `RoleBuilderSystem` is a `HolderSystem<EntityStore>` that processes NPC spawns
  - `onEntityAdd()` processes new NPCs with `NPCEntity` component
  - Dependencies: runs AFTER `EntityStatsSystems.Setup` and `PhysicsValuesAddSystem`
  - We register `NPCQualitySystem` to run AFTER `RoleBuilderSystem`
  
**Technical Approach: ECS System with Dependencies**

```java
public class NPCQualitySystem extends HolderSystem<EntityStore> {
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
        new SystemDependency<>(Order.AFTER, RoleBuilderSystem.class)
    );
    
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }
    
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }
    
    @Override
    public void onEntityAdd(Holder<EntityStore> holder, AddReason reason, Store<EntityStore> store) {
        NPCEntity npc = holder.getComponent(NPCEntity.getComponentType());
        if (npc == null) return;
        
        // Roll quality for this NPC based on spawn context
        String rolledQuality = rollNPCQuality(npc);
        holder.putComponent(HyforgedNPCQualityComponent.getComponentType(), 
            new HyforgedNPCQualityComponent(rolledQuality));
    }
}
```

- New `HyforgedNPCQualityComponent` for entity quality:
  ```java
  public class HyforgedNPCQualityComponent implements Component<EntityStore> {
      private String qualityId;  // e.g., "Rare", "Epic"
      private List<RolledAffix> affixes;  // Optional affixes for elite mobs
  }
  ```
- NPCs with this component:
  - Have visual indicators (name color, particles) based on quality.
  - Receive stat modifiers based on quality tier.
  - Drop loot with boosted quality weights.
  - Can have affixes that modify their stats.

### FR-9: NPC Quality Configuration
- NPC quality rules defined in JSON at `Server/Hyforged/Quality/NPCRules/`.
- Configuration:
  ```json
  {
    "id": "default-spawn",
    "description": "Default spawn quality weights for NPCs",
    "weights": {
      "Common": 700,
      "Uncommon": 200,
      "Rare": 80,
      "Epic": 18,
      "Legendary": 2
    },
    "statMultipliers": {
      "Common": 1.0,
      "Uncommon": 1.2,
      "Rare": 1.5,
      "Epic": 2.0,
      "Legendary": 3.0
    },
    "lootQualityBonus": {
      "Common": 0,
      "Uncommon": 25,
      "Rare": 75,
      "Epic": 150,
      "Legendary": 300
    }
  }
  ```
- `statMultipliers` scale NPC base stats.
- `lootQualityBonus` adds flat weight to all non-Common qualities when calculating loot.

### FR-10: Programmatic Quality Access
- Access Hytale's quality definitions via:
  ```java
  IndexedLookupTableAssetMap<String, ItemQuality> qualityMap = ItemQuality.getAssetMap();
  
  // Get quality by ID
  ItemQuality quality = qualityMap.getAsset("Rare");
  
  // Get quality by index
  ItemQuality quality = qualityMap.getAsset(3);
  
  // Iterate all qualities
  for (int i = 0; i < qualityMap.getNextIndex(); i++) {
      ItemQuality q = qualityMap.getAsset(i);
      if (q != null && isEquipmentQuality(q.getId())) {
          // Process equipment-eligible quality
      }
  }
  ```
- Filter equipment-eligible qualities (exclude Junk, Tool, Template, Debug, Developer, Technical).

## Non-Functional Requirements

### Performance
- Quality rolling is O(1) weighted random selection (pre-compute cumulative weights).
- System runs once per item spawn, minimal overhead.
- Quality lookup via index is O(1).

### Reliability
- Server-authoritative quality assignment.
- Client receives final quality via existing Hytale sync mechanisms.
- Invalid quality IDs in config logged and skipped.

### Extensibility
- Other plugins can:
  - Register custom quality weight rules
  - Listen to `QualityRolledEvent` to modify or cancel
  - Query/set NPC quality components
  - Extend context modifiers (custom "Magic Find"-like stats)

### Backward Compatibility
- Items without random quality processing retain their base quality.
- Existing items in world/inventory unaffected (no migration needed).

## Dependencies
- **Items Affix System**: Quality affects affix capacity; must run before affix rolling.
- **Stats System**: NPC quality uses stat modifiers; Magic Find stat integration.
- **Hytale ItemQuality**: Uses existing quality definitions and UI.
- **Hytale ItemStack/ItemComponent**: For item creation hooks.
- **Hytale ECS**: System registration and execution order.

## Data/Schema Impact

### New JSON Asset Types
| Path | Description |
|------|-------------|
| `Server/Hyforged/Quality/Weights/*.json` | Quality weight distributions |
| `Server/Hyforged/Quality/Eligibility/*.json` | Item eligibility rules |
| `Server/Hyforged/Quality/NPCRules/*.json` | NPC quality spawn rules |

### Existing Impact
- No changes to base Hytale JSON formats.
- Items use Hytale's existing Quality field; we just change its effective value at runtime.

## API Changes

### New Public API
```java
// Roll quality for an item
String QualityService.rollQuality(QualityRollContext context, Random random);

// Check if item is eligible for random quality
boolean QualityService.isEligible(ItemStack item);

// Get/set NPC quality
String NPCQualityService.getQuality(Ref<EntityStore> entity);
void NPCQualityService.setQuality(Ref<EntityStore> entity, String qualityId);

// Get NPC affixes
List<RolledAffix> NPCQualityService.getAffixes(Ref<EntityStore> entity);

// Register custom weight rules (for other plugins)
void QualityWeightRegistry.register(QualityWeightRule rule);
```

### Events
```java
// Emitted when quality is rolled for an item
class QualityRolledEvent implements Cancellable {
    ItemStack item;
    String originalQuality;
    String rolledQuality;
    QualityRollContext context;
}

// Emitted when NPC quality is assigned
class NPCQualityAssignedEvent {
    Ref<EntityStore> entity;
    String qualityId;
    List<RolledAffix> affixes;
}
```

## Security/Privacy
- Server-authoritative: clients cannot influence quality rolls.
- Config files validated on load; invalid entries logged and skipped.
- No player data involved beyond Magic Find stat (already server-side).

## Observability
- Log quality rolls at DEBUG level.
- Log weight calculations at TRACE level.
- Metrics: quality distribution per source type, rolls per tick.
- Debug command: `/hyforged quality roll <itemId>` to test roll without spawning.
- Debug command: `/hyforged npc quality <target>` to display NPC quality.

## Risks

| Risk | Status | Mitigation |
|------|--------|------------|
| ItemStack quality immutable | **RESOLVED** | Use Hyforged metadata layer; Hyforged systems read `hyforged.quality` from metadata first. Vanilla UI uses base quality. |
| Performance impact with many spawns | Open | Batch processing, weight caching, pre-compute cumulative weights |
| Weight misconfiguration causing broken drops | Open | Validation on load, sensible defaults, fallback to base quality |
| NPC quality visual indicators not syncing | Open | Use existing Hytale name color/particle systems via ECS components |
| Container items pre-populated before quality system | Open | Phase 1 focuses on drops; container quality is Phase 2 |
| Hytale UI shows base quality, not override | **Accepted** | Document as known limitation; Hyforged systems use correct quality |

## Open Questions

### Resolved Questions

- **Q1 (RESOLVED)**: How does Hytale's ItemStack handle quality internally? Is it mutable or derived from Item config?
  - **Answer**: Quality is per-Item-definition, not per-ItemStack. `Item.qualityId` is set at asset load. ItemStack does not carry quality; it's read via `itemStack.getItem().getQualityId()`. No quality field in `ItemWithAllMetadata` protocol packet. 
  - **Solution**: Use Hyforged metadata layer (`hyforged.quality`) for effective quality. Hyforged systems read from metadata first.

- **Q2 (RESOLVED)**: Can we hook into NPC spawn to assign quality, or do we need a periodic scan?
  - **Answer**: Yes, use ECS `HolderSystem` with `onEntityAdd()`. `RoleBuilderSystem` processes NPC spawns; we register `NPCQualitySystem` with dependency `Order.AFTER, RoleBuilderSystem.class`.
  - **Solution**: Same pattern as existing `LootAffixSystem` and `BalancingInitialisationSystem`.

- **Q3 (RESOLVED)**: Should chest/container contents be pre-rolled or rolled on open?
  - **Answer**: Items in containers are created via `ItemModule.getRandomItemDrops(droplistId)` which creates `ItemStack` objects. For `ItemContainerState`, items exist in `itemContainer` when container is opened. Dropped items trigger `ItemComponent` addition.
  - **Solution**: For drops, hook `ItemComponent` addition (like `LootAffixSystem`). For container items, may need to hook `ItemContainerState.initialize()` or when `ItemStack` is added to container.

### Remaining Questions

- **Q4**: How to handle container items that are already in `ItemContainerState` before being opened?
  - Containers like treasure chests pre-populate items at spawn time
  - Options: (A) Hook `SimpleItemContainer.addItemStacks()`, (B) Process on open, (C) Accept containers use base quality
  - Recommendation: Start with dropped items only; container quality rolling is Phase 2

- **Q5**: Should Hytale UI limitations affect MVP scope?
  - Vanilla UI shows base quality, not Hyforged override
  - Accept for MVP; document as known limitation
  - Future enhancement when client modding available

## Acceptance Criteria
- [ ] Items eligible for random quality receive rolled quality on spawn.
- [ ] Quality weights are configurable via JSON.
- [ ] Per-category and per-source weight overrides work correctly.
- [ ] `QualityRolledEvent` is emitted and can be cancelled/modified.
- [ ] LootQualitySystem runs before LootAffixSystem.
- [ ] NPCs can have quality assigned via component.
- [ ] NPC quality influences loot quality weights.
- [ ] Magic Find stat (or equivalent) increases high-quality drop rates.
- [ ] Default configuration works out-of-box for weapons and armor.
- [ ] Hytale's quality UI displays the rolled quality correctly.

## Impacted Areas (High-Level)
- **Item System**: Quality assignment at spawn time.
- **Loot System**: Hook for quality rolling before affix rolling.
- **NPC System**: New quality component and spawn integration.
- **Stats System**: Magic Find stat, NPC stat scaling.
- **Event System**: New quality-related events.

## Required Codebase/Architecture Changes (High-Level)

### New Components
- `HyforgedNPCQualityComponent`: Stores NPC quality and affixes.

### New Systems
- `LootQualitySystem`: Rolls quality on item spawn.
- `NPCQualitySystem`: Applies stat modifiers based on NPC quality.

### New Services
- `QualityWeightRegistry`: Stores and resolves quality weight rules.
- `QualityRollerService`: Weighted random quality selection.
- `QualityService`: Public API for quality operations.
- `NPCQualityService`: Public API for NPC quality.

### New Assets
- JSON loaders for quality weights, eligibility, and NPC quality rules.

### Existing Code Changes
- `LootAffixSystem`: Add system dependency to run after `LootQualitySystem`.
- `ItemContextExtractor`: Modify to read quality from Hyforged metadata first, fallback to Item config.
  ```java
  // Before:
  String quality = item.getQualityId();
  
  // After:
  String quality = HyforgedQualityService.getEffectiveQuality(itemStack);
  ```
- System registration: Register both systems with proper ordering.

## References
- Requirements: [Items: Affixes & Quality](.memory_bank/Requirements/rpg-arpg/items-affixes-rarity.md)
- Affix System Spec: [items-affix-system.spec.md](../items-affix-system/items-affix-system.spec.md)
- ADR-0009: Use Hytale Quality and ItemStack Metadata for Affixes
- Hytale Quality Assets: `lib/Server/Item/Qualities/`
- Hytale ItemQuality Class: `lib/hytale-server/.../ItemQuality.java`

---

## Triggered Effect Affixes

### FR-11: Unified Affix Model with Effects
- Affixes are a **single unified model** that can include stat modifiers, triggered effects, or both.
- Do NOT create separate affix classes for effects vs stats - one affix can do both.
- **Entity-Agnostic**: Affixes work on ANY entity in Hytale's ECS:
  - **Players**: Affixes come from equipped items (`RolledAffix` in `ItemStack` metadata)
  - **NPCs/Mobs**: Affixes come from `HyforgedNPCQualityComponent.affixes` (rolled at spawn)
  - Trigger systems process all entities with `HyforgedActiveEffectsComponent` uniformly
- Extended affix definition format in `Server/Hyforged/Affixes/Definitions/`:
  ```json
  {
    "id": "hyforged:blazing_wrath",
    "name": "of Blazing Wrath",
    "slot": "suffix",
    "tier": "rare",
    "statModifiers": [
      { "statId": "hyforged:fire_damage_increased_bps", "min": 500, "max": 1500 }
    ],
    "triggeredEffects": [
      {
        "trigger": {
          "type": "on_hit",
          "chance": 1500,
          "damageCauses": ["Fire"]
        },
        "effect": {
          "type": "spawn_projectile",
          "projectileId": "hyforged:orbiting_flame",
          "count": 3,
          "pattern": "orbit",
          "duration": 5.0
        }
      }
    ],
    "description": "+{fire_damage}% Fire Damage. On fire hit: 15% chance to spawn 3 orbiting flames"
  }
  ```
- `statModifiers` (optional): Array of stat modifications (existing affix behavior).
- `triggeredEffects` (optional): Array of trigger/effect pairs (new behavior).
- An affix with both provides stats AND procs.
- An affix with only `statModifiers` behaves like current stat affixes.
- An affix with only `triggeredEffects` is a pure proc affix.
- Rolling uses same pool/eligibility system - no separate pools needed.

### FR-12: Trigger Types
- Define trigger conditions that activate effect affixes:

| Trigger Type | Description | Parameters |
|--------------|-------------|------------|
| `on_hit` | When dealing damage | `chance` (0-10000 bps), `damageCauses` (filter) |
| `on_kill` | When defeating an enemy | `chance`, `targetTags` (filter) |
| `on_damaged` | When taking damage | `chance`, `damageCauses`, `minDamage` |
| `interval` | Every X seconds while equipped | `intervalSeconds`, `requireCombat` |
| `on_cast` | When using Primary/Secondary/Ability | `interactionTypes` (array) |
| `on_block` | When successfully blocking | `chance` |

- Example trigger configs:
  ```json
  {
    "type": "on_hit",
    "chance": 1500,
    "damageCauses": ["Physical", "Fire"]
  }
  ```
  ```json
  {
    "type": "interval",
    "intervalSeconds": 5.0,
    "requireCombat": true
  }
  ```

### FR-13: Effect Types
- Define effect actions that execute when triggered.
- Leverage existing Hytale systems for maximum compatibility:

| Effect Type | Hytale Integration | Parameters |
|-------------|-------------------|------------|
| `spawn_projectile` | `ProjectileComponent.assembleDefaultProjectile()` | `projectileId`, `count`, `pattern`, `velocity` |
| `spawn_prefab` | `SpawnPrefabInteraction` | `prefabPath`, `offset`, `duration` |
| `apply_effect` | `EffectControllerComponent.addEffect()` | `effectId`, `duration`, `target` (self/target/area) |
| `damage_area` | `DamageSystems.executeDamage()` with spatial query | `radius`, `damage`, `damageCause`, `excludeSelf` |
| `run_interaction` | Hytale Interaction system | `interactionId`, `context` |
| `modify_stat` | Hyforged stat system | `statId`, `amount`, `duration` |

- Example effect configs:
  ```json
  {
    "type": "spawn_projectile",
    "projectileId": "hyforged:chain_lightning",
    "count": 1,
    "pattern": "targeted",
    "velocity": 20.0
  }
  ```
  ```json
  {
    "type": "apply_effect",
    "effectId": "hyforged:burning",
    "duration": 5.0,
    "target": "hit_target"
  }
  ```

### FR-14: Projectile Patterns
- Define spawn patterns for `spawn_projectile` effects:

| Pattern | Description |
|---------|-------------|
| `forward` | Single projectile in facing direction |
| `spread` | Fan of projectiles (uses `count` and `spreadAngle`) |
| `orbit` | Rotating projectiles around player |
| `nova` | Projectiles in all directions from player |
| `targeted` | Aimed at current target or cursor |
| `ground` | Spawn at ground level (for pools/areas) |

- Pattern config example:
  ```json
  {
    "pattern": "orbit",
    "count": 4,
    "orbitRadius": 2.0,
    "rotationSpeed": 90
  }
  ```

### FR-15: Active Effect Tracking Component
- `HyforgedActiveEffectsComponent` tracks active triggered effects on ANY entity.
- Works for players (from equipment) AND NPCs/mobs (from quality component).
- **Fully data-driven** - no enums; source types are strings for extensibility.
- Runtime state for all active affix effects:
  ```java
  public class HyforgedActiveEffectsComponent implements Component<EntityStore> {
      private Map<String, ActiveEffectState> activeEffects;
      
      public static class ActiveEffectState {
          String affixId;           // Source affix definition ID
          String sourceType;        // e.g., "equipment", "npc_quality", "buff", "skill", "set_bonus"
          String sourceId;          // e.g., "mainhand", "chest", "hyforged:rare_goblin", "hyforged:berserker_3"
          long lastTriggeredMs;     // Cooldown tracking
          int stacks;               // For stacking effects
          float accumulatedTime;    // Interval tracking
      }
  }
  ```
- `sourceType` examples (string, not enum):
  - `"equipment"` - from equipped item (sourceId = slot name)
  - `"npc_quality"` - from NPC quality affix (sourceId = NPC asset ID)
  - `"buff"` - from active buff/consumable (sourceId = buff ID)
  - `"skill"` - from skill tree unlock (sourceId = skill node ID)
  - `"spell"` - from learned/active spell (sourceId = spell ID) - e.g., D2-style auras
  - `"aura"` - persistent area effect (sourceId = aura ID) - affects self + nearby entities
  - `"set_bonus"` - from equipment set (sourceId = set ID)
  - `"passive"` - always-on ability (sourceId = passive ID)
  - Custom types can be added by other mods
- **Persistent Effects**: Source types like `spell` and `aura` support always-on effects:
  - Player learns "Thorns Aura" → adds interval effect that damages nearby attackers
  - Player activates "Concentration" → adds passive stat buff to self and allies
  - These use the same trigger/effect system as equipment affixes
- Component added to any entity with triggered effects, regardless of source.
- Trigger systems query this component regardless of entity type or source type.
- Affixes with only stat modifiers don't need entries here.

### FR-16: Effect Affix Trigger System
- Trigger systems are **entity-agnostic** - process any entity with `HyforgedActiveEffectsComponent`.
- Works bidirectionally: player hits mob, mob hits player, mob hits mob.

**On-Hit/On-Damaged Triggers**:
- Extend `DamageEventSystem` in damage pipeline's `inspectDamageGroup`:
  ```java
  public class EffectAffixOnHitSystem extends DamageEventSystem {
      @Override
      public void onEvent(Damage damage, ...) {
          // ATTACKER (player OR npc): Check for on_hit triggers
          Ref<EntityStore> attacker = damage.getAttacker();
          processOnHitTriggers(attacker, damage);
          
          // VICTIM (player OR npc): Check for on_damaged triggers
          Ref<EntityStore> victim = damage.getVictim();
          processOnDamagedTriggers(victim, damage);
      }
  }
  ```
- Example: "Blazing Rare Goblin" with `on_hit` affix procs when goblin hits player.
- Uses existing damage pipeline pattern (like `HyforgedCriticalHitSystem`).

**On-Kill Triggers**:
- Extend `DeathSystems.OnDeathSystem`:
  ```java
  public class EffectAffixOnKillSystem extends DeathSystems.OnDeathSystem {
      @Override
      public void onComponentAdded(DeathComponent death, ...) {
          // Find killer, check for on_kill effect affixes
          // Execute effects
      }
  }
  ```
- Uses existing death system pattern (like `XPAwardOnKillSystem`).

**Interval Triggers**:
- `DelayedEntitySystem` with configurable tick rate:
  ```java
  public class EffectAffixIntervalSystem extends DelayedEntitySystem<EntityStore> {
      public EffectAffixIntervalSystem() {
          super(0.1f); // 100ms tick
      }
      
      @Override
      public void tick(float dt, int index, ...) {
          // Accumulate time per affix
          // Execute effects when interval reached
          // Reset timer
      }
  }
  ```
- Uses existing interval pattern (like `RageDecaySystem`).

### FR-17: Effect Executor Service
- `EffectExecutorService` wraps Hytale systems for effect execution:
  ```java
  public interface EffectExecutorService {
      void execute(EffectAffixDefinition affix, EffectContext context);
      
      void spawnProjectile(ProjectileEffectConfig config, EffectContext context);
      void spawnPrefab(PrefabEffectConfig config, EffectContext context);
      void applyEntityEffect(EntityEffectConfig config, EffectContext context);
      void damageArea(AreaDamageConfig config, EffectContext context);
  }
  ```
- `EffectContext` contains:
  - Source entity reference (who has the affix)
  - Target entity reference (if applicable)
  - Hit position (for area effects)
  - Command buffer for entity operations
  - Store for component access

### FR-18: Affix Effect Initialization
- Initialize `HyforgedActiveEffectsComponent` from various affix sources.
- **Extensible by source type** - any system can register active effects.

**Source Type: `equipment`**
- When equipment changes and affixes are recalculated:
  1. Existing flow: Apply stat modifiers from `statModifiers` arrays
  2. Scan all equipped affixes for `triggeredEffects`
  3. Add/update entries with `sourceType: "equipment"`, `sourceId: <slot>`
  4. Initialize cooldown/interval timers for new effects
  5. Clear state for removed effects

**Source Type: `npc_quality`**
- When NPC spawns with `HyforgedNPCQualityComponent`:
  1. Apply stat modifiers from NPC affixes (existing flow)
  2. Scan affixes for `triggeredEffects`
  3. Add entries with `sourceType: "npc_quality"`, `sourceId: <npc_asset_id>`
  4. Initialize cooldown/interval timers
- NPC active effects persist until entity despawns.

**Future Source Types** (extensible):
- `buff` - from consumables or applied buffs
- `skill` - from skill tree unlocks (passive nodes)
- `spell` - from learned spells (e.g., D2-style Paladin auras like Concentration, Thorns, Fanaticism)
- `aura` - persistent area effects affecting self + nearby entities
- `passive` - always-on abilities unlocked via progression
- `set_bonus` - from equipment set completion
- Custom source types from other mods

**Design Note: Persistent Spell Effects**
- Spells/auras are first-class citizens in this system, not special cases.
- A learned spell like "Thorns Aura" would:
  1. Add entry with `sourceType: "spell"`, `sourceId: "hyforged:thorns_aura"`
  2. Use `interval` trigger type for periodic area damage reflection
  3. Remain active while spell is "equipped" or toggled on
- Same trigger/effect infrastructure as equipment affixes - no separate spell system needed.

**Shared Behavior**:
- All source types use the same `ActiveEffectState` structure.
- Triggered effect state is runtime-only (not persisted); rebuilt on spawn/equip.
- All flows feed into the same trigger systems (FR-16).

### FR-19: Effect Affix Visual Feedback
- Effects provide visual feedback via existing Hytale systems:
  - **Projectiles**: Use Hytale's projectile rendering
  - **Entity Effects**: Use Hytale's `EntityEffect` visuals (particles, model changes)
  - **Areas**: Use Hytale's `WorldParticle` for ground effects
- Trigger activation feedback:
  - Play sound via `SoundUtil.playSoundEvent3d()`
  - Show particles via `ParticleUtil`
  - Combat text for proc notification (optional)

### FR-20: Effect Affix Stacking & Cooldowns
- **Stacking**: Multiple items with same effect affix:
  - `stackBehavior`: `"independent"` (each triggers separately) or `"shared"` (single cooldown)
  - `maxStacks`: Maximum instances (default 1)
- **Cooldowns**:
  - `cooldownSeconds`: Minimum time between triggers (0 = no cooldown)
  - `sharedCooldownGroup`: Effects in same group share cooldown
- Example:
  ```json
  {
    "id": "hyforged:chain_lightning_proc",
    "stackBehavior": "shared",
    "maxStacks": 1,
    "cooldownSeconds": 2.0,
    "sharedCooldownGroup": "lightning_procs"
  }
  ```

### FR-21: Effect Affix Events
- Emit events for modding integration:
  ```java
  public class EffectAffixTriggeredEvent implements Cancellable {
      Ref<EntityStore> source;
      String affixId;
      TriggerType triggerType;
      @Nullable Ref<EntityStore> target;
      boolean cancelled;
  }
  
  public class EffectAffixExecutedEvent {
      Ref<EntityStore> source;
      String affixId;
      EffectType effectType;
      // Result details (projectiles spawned, damage dealt, etc.)
  }
  ```
- `EffectAffixTriggeredEvent` dispatched BEFORE effect execution (cancellable).
- `EffectAffixExecutedEvent` dispatched AFTER effect execution (informational).

## Technical Research: Effect Affixes

### Hytale Integration Points (Researched 2026-01-23)

**Damage Pipeline**:
- `DamageEventSystem` base class for damage pipeline hooks
- Groups: `gatherDamageGroup`, `filterDamageGroup`, `inspectDamageGroup`
- Existing Hyforged systems use this: `HyforgedCriticalHitSystem`, `HyforgedAilmentSystem`
- Key file: [DamageEventSystem.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/modules/entity/damage/DamageEventSystem.java)

**Death System**:
- `DeathSystems.OnDeathSystem` extends `RefChangeSystem<EntityStore, DeathComponent>`
- Triggers when entity dies (DeathComponent added)
- Provides killer info via `deathComponent.getDeathInfo().getSource()`
- Existing usage: `XPAwardOnKillSystem`
- Key file: [DeathSystems.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/modules/entity/damage/DeathSystems.java)

**Interval Systems**:
- `DelayedEntitySystem<EntityStore>` for periodic ticks
- Constructor takes interval in seconds
- Existing usage: `RageDecaySystem` (0.2s), `ResourceStatsHudSystem`
- Key file: [DelayedEntitySystem.java](lib/hytale-server/src/main/java/com/hypixel/hytale/component/system/tick/DelayedEntitySystem.java)

**Projectile System**:
- `ProjectileComponent.assembleDefaultProjectile(time, assetName, position, rotation)`
- `projectileComponent.shoot(holder, creatorUuid, x, y, z, yaw, pitch)`
- Add to world via `commandBuffer.addEntity(holder, AddReason.SPAWN)`
- Key file: [ProjectileComponent.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/entity/entities/ProjectileComponent.java)

**Entity Effects**:
- `EffectControllerComponent.addEffect(ref, entityEffect, accessor)`
- `EntityEffect` assets define visuals, stat mods, duration
- Key file: [EffectControllerComponent.java](lib/hytale-server/src/main/java/com/hypixel/hytale/server/core/entity/effect/EffectControllerComponent.java)

**Interaction Types (for on_cast triggers)**:
```java
InteractionType.Primary    // Left click
InteractionType.Secondary  // Right click
InteractionType.Ability1   // Ability slot 1
InteractionType.Ability2   // Ability slot 2
InteractionType.Ability3   // Ability slot 3
```
