# Architecture Decision Records (ADRs)

## ADR Index

- ADR-0001: Hybrid Hyforged + Hytale Stats (2026-01-19) — Superseded by ADR-0010
- ADR-0002: Extend Hytale Modifier System (2026-01-19) — Accepted
- ADR-0003: Data-Driven Stat and Tag Definitions (2026-01-19) — Superseded by ADR-0005
- ADR-0004: Data-Driven Category Definitions (2026-01-19) — Accepted
- ADR-0005: Tags as Simple Strings (2026-01-19) — Superseded by ADR-0008
- ADR-0006: Replace Hytale Stat/Damage Systems for Exclusive Hyforged Control (2026-01-20) — Accepted
- ADR-0007: Data-Driven Damage Type Extensions (2026-01-20) — Accepted
- ADR-0008: Use Hytale AssetRegistry Tag System (2026-01-20) — Accepted
- ADR-0009: Use Hytale Quality and ItemStack Metadata for Affixes (2026-01-20) — Accepted
- ADR-0010: Unified Stat Integration via EntityStatValue Extension (2026-01-21) — Accepted
- ADR-0011: Runtime Quality Replacement for Items and NPCs (2026-01-23) — Proposed


## ADR Template

### ADR-0001: <Title>
- Date: YYYY-MM-DD
- Status: Proposed | Accepted | Superseded | Deprecated
- Deciders: <names/roles>

#### Context
- <why this decision is needed>

#### Decision
- <what was decided>

#### Consequences
- <trade-offs and impacts>

#### Alternatives Considered
- <alternatives and why they were rejected>

#### Links
- <related specs, plans, PRs, issues>

### ADR-0001: Hybrid Hyforged + Hytale Stats
- Date: 2026-01-19
- Status: Superseded by ADR-0010
- Deciders: JBurl

#### Context
- Hytale already provides a server-authoritative `entitystats` system geared toward resource stats (min/max/current, regeneration, min/max effects) and network replication.
- Hyforged requires an ARPG-grade stats framework with:
	- many derived stats (30–40+)
	- deterministic stacking (Flat / Increased / More / Caps)
	- integer-only authoritative math
	- detailed breakdowns for UI
	- extensibility for other systems/plugins
- The built-in `entitystats` modifier model and client support are intentionally limited (primarily min/max modifiers and basic additive/multiplicative).

#### Decision
- Implement a Hyforged-owned stats layer to model ability scores, derived stats, modifier stacking, and UI breakdowns.
- Continue using Hytale `entitystats` for built-in resource stats (Health/Mana/Stamina/etc.) and their existing regeneration/effects/replication.
- Add a bridge that exports selected Hyforged-derived outcomes into Hytale `EntityStatMap` via server-side modifiers (e.g., ability scores increasing MaxHealth).

#### Consequences
- Pros:
	- Preserves compatibility with base game resource systems and UI replication.
	- Enables ARPG-grade stat math and extensibility without fighting the base system’s constraints.
	- Allows a clean UI/UX breakdown model owned by Hyforged.
- Cons:
	- Requires careful synchronization boundaries (what is Hyforged-owned vs Hytale-owned).
	- Additional complexity: two layers of stats with a bridging mechanism.

#### Alternatives Considered
- Use only Hytale `entitystats` for all stats
	- Rejected: not a good fit for large derived stat catalogs, rich stacking semantics, or detailed breakdown attribution.
- Replace Hytale `entitystats` entirely
	- Rejected: would forfeit built-in regeneration/min/max effects, and increase risk/cost of UI replication.

#### Links
- Spec: .memory_bank/Features/hyforged-stats-system/hyforged-stats-system.spec.md
- Requirements: .memory_bank/Requirements/rpg-arpg/stats-system.md

---

### ADR-0002: Extend Hytale Modifier System
- Date: 2026-01-19
- Status: Accepted
- Deciders: JBurl

#### Context
- Our initial implementation created a parallel stat/modifier system using Java records (`StatModifier`, `ModifierType`) separate from Hytale's native `Modifier` class.
- Hytale's `ItemWeapon`, `ItemArmor`, and effects use `StatModifiers` fields that deserialize via `Modifier.CODEC`, a polymorphic `CodecMapCodec<Modifier>`.
- Hytale natively registers `StaticModifier` with `CalculationType.ADDITIVE` and `MULTIPLICATIVE`.
- Our ARPG stacking (FLAT/INCREASED/MORE/CAP) is not available in vanilla Hytale.
- Items using Hytale's JSON format could not use our modifiers without code changes.

#### Decision
- Create `HyforgedModifier` extending Hytale's `Modifier` abstract class.
- Register it with `Modifier.CODEC.register("Hyforged", ...)` in plugin setup.
- Include `StackType` enum (FLAT/INCREASED/MORE/CAP) and source metadata in the modifier.
- Items can now use `{ "Type": "Hyforged", "StackType": "INCREASED", "Amount": 100 }` in their JSON.
- Bridge system updated to use `HyforgedModifier` for consistency.

#### Consequences
- Pros:
	- Items can use ARPG modifiers directly in their JSON (`ItemWeapon.StatModifiers`).
	- Integrates with Hytale's equipment recalculation system (`StatModifiersManager`).
	- Uses Hytale's network packet format for client sync via `toPacket()`.
	- Other mods can use `HyforgedModifier` in their items.
- Cons:
	- Hytale's `apply(float)` is called per-modifier, not with full stacking context.
	- For proper ARPG stacking order, a custom system must collect all modifiers first.
	- Dual representation: `HyforgedModifier` for Hytale integration, component-level tracking for breakdown.

#### Alternatives Considered
- Keep fully parallel system, intercept equipment events:
	- Rejected: Items couldn't use our modifiers in JSON; more complex event handling.
- Replace Hytale's modifier system entirely:
	- Rejected: Would break compatibility with base game equipment/effects.

#### Links
- Implementation: `reign.software.hyforged.stats.modifier.HyforgedModifier`
- Related ADR: ADR-0001 (Hybrid approach)

---

### ADR-0003: Data-Driven Stat and Tag Definitions
- Date: 2026-01-19
- Status: Accepted
- Deciders: JBurl

#### Context
- Initial implementation hardcoded stat and tag definitions in `CoreStats.registerAll()` and `CoreTags.registerAll()`.
- Hytale uses a data-driven pattern with JSON assets in `Server/` directories loaded via `AssetRegistry`.
- Hardcoded Java definitions don't follow Hytale's extensibility patterns.
- Other mods cannot add custom stats without code changes.
- The modding documentation specifies `src/main/resources/Server/` for server-side data.

#### Decision
- Convert all stat and tag definitions to JSON assets in `Server/Hyforged/Stats/` and `Server/Hyforged/Tags/`.
- Register `StatDefinitionAsset` and `TagDefinitionAsset` with Hytale's `AssetRegistry`.
- Use `StatAssetLoader` to handle asset loading and registration with `StatDefinitionRegistry`.
- Simplify `CoreStats.java` and `CoreTags.java` to contain only constant IDs for compile-time references.
- Remove `registerAll()` methods from both classes.

#### Consequences
- Pros:
	- Follows Hytale's data-driven asset pattern.
	- Other mods can add stats/tags via JSON without code.
	- Stat definitions can be hot-reloaded during development.
	- Clear separation: Java for logic, JSON for content.
	- Asset loading order can be controlled via `loadsBefore()`.
- Cons:
	- Requires asset store registration boilerplate.
	- Stats not available until asset loading phase completes.
	- JSON schema must be documented for modders.

#### Alternatives Considered
- Keep hardcoded Java definitions:
	- Rejected: Doesn't follow Hytale patterns; not extensible by other mods.
- Use only JSON with no Java constants:
	- Rejected: Compile-time references useful for type safety in code.

#### Links
- Stats path: `src/main/resources/Server/Hyforged/Stats/`
- Tags path: `src/main/resources/Server/Hyforged/Tags/`
- Loader: `reign.software.hyforged.stats.asset.StatAssetLoader`
- Related ADR: ADR-0001 (Hybrid approach)

---

### ADR-0004: Data-Driven Category Definitions
- Date: 2026-01-19
- Status: Accepted
- Deciders: JBurl

#### Context
- Initial implementation used a Java enum `StatCategory` with hardcoded values.
- Categories are used to group stats for UI organization (Ability Scores, Resources, Offense, etc.).
- Hardcoded enums don't allow mods to add custom categories.
- Stats and tags were already converted to data-driven assets (ADR-0003).
- Consistency: categories should follow the same data-driven pattern.

#### Decision
- Remove the `StatCategory` Java enum.
- Create `CategoryDefinition` record and `CategoryDefinitionAsset` for JSON loading.
- Categories defined in JSON files in `Server/Hyforged/Categories/`.
- Stats reference categories by string ID (e.g., `"ability-score"`, `"offense"`).
- `CoreCategories` class provides compile-time constants for common category IDs.
- `StatDefinitionRegistry` tracks categories and provides lookup by category.

#### Consequences
- Pros:
	- Mods can define custom categories for their stats.
	- Consistent data-driven approach for stats, tags, and categories.
	- Category metadata (displayName, description, sortOrder) can be customized.
	- UI can dynamically discover and display all categories.
- Cons:
	- Category ID typos won't be caught at compile time (use `CoreCategories` constants).
	- Slightly more complex asset loading (3 asset types instead of 2).

#### Alternatives Considered
- Keep enum-based categories:
	- Rejected: Inconsistent with data-driven stats/tags; not extensible.
- Use tags instead of categories:
	- Rejected: Categories serve a different purpose (UI grouping vs stat grouping).

#### Links
- Categories path: `src/main/resources/Server/Hyforged/Categories/`
- Constants: `reign.software.hyforged.stats.CoreCategories`
- Definition: `reign.software.hyforged.stats.CategoryDefinition`
- Related ADR: ADR-0003 (Data-driven stats/tags)

---

### ADR-0005: Tags as Simple Strings
- Date: 2026-01-19
- Status: Accepted
- Deciders: JBurl

#### Context
- ADR-0003 created separate `TagDefinition` records and `TagDefinitionAsset` JSON files.
- Tag JSON files contained `AffectedStats` arrays, duplicating what stats already declare via their `Tags` array.
- Tag definitions also had `DisplayName` and `Description` fields, but UI only shows tag names.
- This duplication creates maintenance burden and potential inconsistency.
- The tag-to-stat mapping is already derivable from stats themselves.

#### Decision
- Remove `TagDefinition.java` record class.
- Remove `TagDefinitionAsset.java` and tag JSON loading from `StatAssetLoader`.
- Delete the `Server/Hyforged/Tags/` directory and all tag JSON files.
- Tags are now simple strings declared in each stat's `Tags` array.
- `StatDefinitionRegistry` builds tag-to-stat mappings dynamically from loaded stats.
- `CoreTags.java` remains as compile-time constants for common tag identifiers.

#### Consequences
- Pros:
	- Single source of truth: stats declare their tags, mappings are derived.
	- No redundant JSON files to maintain.
	- Simpler codebase (fewer classes, less asset loading).
	- Adding a new tag is automatic when any stat uses it.
	- DRY principle: no duplication of tag-stat relationships.
- Cons:
	- Cannot define a tag without at least one stat using it.
	- Tag metadata (if ever needed) would require a different approach.

#### Alternatives Considered
- Keep tag JSON files with validation:
	- Rejected: Still duplication; validation adds complexity.
- Use tag JSON only for metadata, derive stats:
	- Rejected: Tags don't need metadata for current UI requirements.

#### Links
- Supersedes: ADR-0003 (tag portion)
- Registry: `reign.software.hyforged.stats.StatDefinitionRegistry`
- Constants: `reign.software.hyforged.stats.CoreTags`
---

### ADR-0006: Replace Hytale Stat/Damage Systems for Exclusive Hyforged Control
- Date: 2026-01-20
- Status: Accepted
- Deciders: JBurl

#### Context
- ADR-0001 established a hybrid approach bridging Hyforged stats to Hytale's `EntityStatMap`.
- Investigation revealed Hytale has parallel systems that could conflict with Hyforged:
  - `StatModifiersManager`: Per-entity manager handling armor/weapon/effect stat modifiers (instantiated as `private final` in `LivingEntity`)
  - `EntityStatsSystems.Recalculate`: Calls `StatModifiersManager.recalculateEntityStatModifiers()` on equipment changes
  - `DamageSystems.ArmorDamageReduction`: Reduces damage based on `ItemArmor.getDamageResistanceValues()` and effect-based resistance
  - `DamageSystems.ArmorKnockbackReduction`: Reduces knockback based on armor
- These systems read directly from Hytale's `ItemArmor` and `EntityEffect` JSON fields, bypassing Hyforged's ARPG stacking.
- User wants Hyforged to handle stats exclusively.

#### Investigation Findings
- **Can Replace**: `ComponentRegistry.unregisterSystem(Class<? extends ISystem>)` exists and can remove registered systems
- **Cannot Replace**: `StatModifiersManager` is `private final` in `LivingEntity`—cannot swap the instance
- **Systems Registered**: In `EntityStatsModule.setup()` and `DamageModule.setup()`
- **Damage Flow**: `ArmorDamageReduction` → `ApplyDamage` → `EntityStatMap.subtractStatValue()`
- **Health Storage**: `DamageSystems.ApplyDamage` reads health from `EntityStatMap`, not `HyforgedStatComponent`

#### Decision
- **Unregister Hytale's stat/damage systems** that conflict with Hyforged:
  - `DamageSystems.ArmorDamageReduction` — replaced by Hyforged resistance calculation
  - `DamageSystems.ArmorKnockbackReduction` — replaced by Hyforged knockback resistance
  - `EntityStatsSystems.Recalculate` — we don't need Hytale's armor stat modifiers
- **Keep `DamageSystems.ApplyDamage`** — it handles final health subtraction and death
- **Bridge Hyforged MaxHealth to `EntityStatMap`** — so `ApplyDamage` has correct max health
- **Register Hyforged damage reduction system** — reads from `HyforgedStatComponent` for resistance
- **Ignore `StatModifiersManager`** — it will still exist but with `Recalculate` unregistered, it won't interfere

#### Implementation Plan
1. Create `HyforgedStatSystemBridge` plugin initializer:
   - Call `unregisterSystem(DamageSystems.ArmorDamageReduction.class)`
   - Call `unregisterSystem(DamageSystems.ArmorKnockbackReduction.class)`
   - Call `unregisterSystem(EntityStatsSystems.Recalculate.class)`
2. Create `HyforgedDamageReductionSystem` extending `DamageEventSystem`:
   - Query for entities with `HyforgedStatComponent`
   - Read resistance from Hyforged stats (e.g., `PhysicalResistance`, `FireResistance`)
   - Apply ARPG damage reduction formula
3. Update `HyforgedStatBridge` to sync MaxHealth to `EntityStatMap` on every recalculation

#### Consequences
- Pros:
  - Hyforged exclusively controls armor and effect stat contributions
  - ARPG stacking (FLAT/INCREASED/MORE/CAP) applied to all defense stats
  - Consistent damage reduction formula across all sources
  - Eliminates "double dipping" where Hytale and Hyforged both reduce damage
- Cons:
  - Hytale's native `ItemArmor.StatModifiers` and `DamageResistance` JSON fields become inert
  - Must document that modders use Hyforged modifier system instead
  - Slightly higher integration complexity

#### Alternatives Considered
- Full hybrid (let both run):
  - Rejected: Double-dipping on armor stats causes inconsistent balance
- Replace `ApplyDamage` entirely:
  - Rejected: Would break health/death sync with client UI; high risk
- Event interception only:
  - Rejected: Less clean; timing dependencies; harder to debug

#### Links
- Related ADR: ADR-0001 (Hybrid approach—partially superseded for armor/damage)
- Related ADR: ADR-0002 (HyforgedModifier integration)
- Hytale API: `ComponentRegistry.unregisterSystem()`
- Hytale: `DamageModule.setup()`, `EntityStatsModule.setup()`

---

### ADR-0007: Data-Driven Damage Type Extensions
- Date: 2026-01-20
- Status: Accepted
- Deciders: JBurl

#### Context
- `HyforgedDamageReductionSystem` needed to map Hytale's `DamageCause` IDs to Hyforged resistance stats.
- Initial implementation hardcoded damage type tags (`DAMAGE_TYPE_TAGS = Set.of("physical", "fire", "cold", ...)`) and inferred the mapping from stat tags.
- This violated ECS/data-driven principles: adding a new damage type required code changes.
- User questioned: "With the way ECS works, would the mapping be on the damage type entity itself?"

#### Decision
- **Create damage type extension assets** (`Server/Hyforged/Damage/*.json`) that extend Hytale's `DamageCause` without modifying it.
- Each extension specifies which resistance stat applies to that damage type via `HyforgedResistanceStat`.
- **New components**:
  - `DamageTypeExtensionAsset`: JSON asset for loading extensions
  - `DamageTypeExtension`: Immutable record holding parsed extension data
  - `DamageTypeExtensionRegistry`: Singleton registry with inheritance-aware stat lookup
  - `DamageTypeAssetLoader`: Registers asset store and handles `LoadedAssetsEvent`
- **Update `HyforgedDamageReductionSystem`** to query `DamageTypeExtensionRegistry` instead of using hardcoded mapping.
- Extension IDs match Hytale `DamageCause` IDs (e.g., "Fire", "Physical", "Bleed").

#### Consequences
- Pros:
  - Fully data-driven: modders add damage types by creating JSON files, no code changes
  - Follows ECS principle: damage type entity defines its own resistance relationship
  - Supports inheritance: child damage types inherit parent's resistance stat if not overridden
  - Extensible: `HyforgedPenetrationStat` field ready for future penetration mechanics
- Cons:
  - Requires JSON file for each damage type that should use resistances
  - Slightly more complex asset loading (additional loader and registry)
  - Extension files must be named to match Hytale's `DamageCause` IDs exactly

#### JSON Example
```json
{
    "$Comment": "Fire damage extension for Hyforged resistance system",
    "Inherits": "Elemental",
    "HyforgedResistanceStat": "hyforged:fire-resistance-bps",
    "HyforgedPenetrationStat": "hyforged:fire-penetration-bps"
}
```

#### Files Changed
- New: `reign.software.hyforged.stats.damage.DamageTypeExtensionAsset`
- New: `reign.software.hyforged.stats.damage.DamageTypeExtension`
- New: `reign.software.hyforged.stats.damage.DamageTypeExtensionRegistry`
- New: `reign.software.hyforged.stats.damage.DamageTypeAssetLoader`
- Modified: `reign.software.hyforged.stats.bridge.HyforgedDamageReductionSystem`
- Modified: `reign.software.hyforged.HyforgedPlugin` (initialize loader)
- New: `Server/Hyforged/Damage/Fire.json`, `Ice.json`, `Physical.json`, `Poison.json`, `Elemental.json`

#### Links
- Related ADR: ADR-0006 (Hyforged damage system replacement)
- Asset path: `Server/Hyforged/Damage/`

---

### ADR-0008: Use Hytale AssetRegistry Tag System
- Date: 2026-01-20
- Status: Accepted
- Deciders: JBurl

#### Context
- ADR-0005 established tags as simple strings declared in stat definitions.
- Hyforged implemented its own tag-to-stat mapping using `Map<String, Set<Integer>>`.
- `StatModifier` used `String targetTagId` to target stats by tag.
- Investigation revealed Hytale already has a robust tag system:
  - `AssetRegistry.getOrCreateTagIndex(String)`: Global tag index registration returning `int`
  - `AssetExtraInfo.Data.getExpandedTagIndexes()`: Returns `IntSet` for O(1) membership tests
  - `TagSet` interface for composable tag groups with include/exclude and glob patterns
  - `TagSetLookupTable` for flattening tag hierarchies into efficient int sets
- Hytale's tag system is used throughout items, blocks, NPCs (e.g., `NPCGroup`).
- Hytale items use hierarchical tag format: `{"Type": ["Weapon"], "Family": ["Axe"]}`.
- Using integer indices avoids string comparisons and enables O(1) lookups.

#### Decision
- **Integrate fully with Hytale's tag system** including both the integer indices AND the hierarchical JSON format.

**Phase 1: Integer Indices (completed)**
- `StatDefinitionRegistry`:
  - Register stat tags via `AssetRegistry.getOrCreateTagIndex(String)` during stat loading
  - Store `Int2ObjectMap<IntSet> tagIndexToStatIndices` for tag-to-stat reverse lookup
  - Add `getStatIndicesForTagIndex(int tagIndex)` returning `IntSet`
- `StatModifier`:
  - Change `targetTagId` (String) to `targetTagIndex` (int)
  - Use sentinel `NO_TAG = Integer.MIN_VALUE`

**Phase 2: Hierarchical JSON Format (completed)**
- `StatDefinitionAsset`:
  - Change tags codec from `Codec.STRING_ARRAY` to `MapCodec<>(Codec.STRING_ARRAY, HashMap::new)`
  - Store `Map<String, String[]> rawTags` instead of `String[] tags`
  - Add `getExpandedTags()` method that flattens hierarchical tags following Hytale's pattern
- Stat JSON files:
  - Convert from: `"Tags": ["offense", "fire", "damage"]`
  - Convert to: `"Tags": {"Domain": ["offense"], "Element": ["fire"], "Type": ["damage"]}`
- Tag expansion follows Hytale's `AssetExtraInfo.Data.putTags()` pattern:
  - Category key becomes a tag: `Domain`
  - Each value becomes a tag: `offense`
  - Category=Value becomes a tag: `Domain=offense`

#### Tag Categories
Standard categories established for stat definitions:
| Category | Values | Purpose |
|----------|--------|---------|
| `Domain` | offense, defense, resource, utility, attributes | Primary classification |
| `Element` | physical, fire, cold, lightning, chaos, elemental | Damage/resistance element |
| `Type` | damage, resistance, rating, ability-score, speed, etc. | Stat function |
| `Modifier` | flat, percent, more | Application method |
| `Source` | derived, base | Value origin |
| `Mechanic` | attack, spell, projectile, melee, minion, etc. | Usage mechanism |
| `Resource` | health, mana, stamina, rage | Affected resource |
| `Ailment` | bleed, poison, ignite, chill, shock, freeze | Ailment type |
| `Weapon` | sword, axe, mace, dagger, bow, etc. | Weapon affinity |

#### Consequences
- Pros:
  - Fully aligns with Hytale's engine patterns (same format as items/blocks)
  - O(1) integer-based lookups with FastUtil `IntSet`
  - Shared tag namespace with Hytale assets
  - Hierarchical tags enable flexible querying (by category, value, or combination)
  - `Element=fire` can target specific elements without matching all `elemental` stats
- Cons:
  - Breaking change for existing stat JSON files (all must be converted)
  - Slightly more verbose JSON format
  - Debug output shows tag indices (can add reverse lookup if needed)

#### Files Changed
- `StatDefinitionAsset`: MapCodec for tags, `getExpandedTags()` method
- `StatDefinitionRegistry`: Added `Int2ObjectMap<IntSet>`, integrated `AssetRegistry`
- `StatModifier`: Changed `targetTagId` (String) to `targetTagIndex` (int)
- `ConditionalStatModifier`: Updated delegation method
- `HyforgedStatComputeSystem`: Use `getStatIndicesForTagIndex(int)` and `IntSet`
- `HyforgedStatQueryService`: Use integer-based tag matching
- `HyforgedStatComponent`: Use `targetTagIndex` for dirty marking
- All 171 stat JSON files: Converted to hierarchical tag format

#### Links
- Supersedes: ADR-0005 (Tags as Simple Strings)
- Hytale API: `AssetRegistry.getOrCreateTagIndex()`, `AssetExtraInfo.Data.putTags()`
- Hytale: `TagSet`, `TagSetLookupTable`, `NPCGroup`

---

### ADR-0009: Use Hytale Quality and ItemStack Metadata for Affixes
- Date: 2026-01-20
- Status: Accepted
- Deciders: JBurl

#### Context
- Hyforged needs an ARPG-style affix system for items (prefixes, suffixes, forged modifiers).
- Hytale already provides a Quality system (`ItemQuality` assets in `Server/Item/Qualities/`) with UI support (colors, textures, tooltips).
- Hytale's `ItemStack` class supports custom metadata via `BsonDocument` that persists and replicates to clients.
- Initial Hyforged prototype code created separate affix classes (`AffixTier`, `AffixMetadata`, `AffixRoller`) but didn't integrate with Hytale's data formats.
- Key decision: should affixes use Hytale's Quality system or create a separate "Rarity" concept?

#### Decision
- **Use Hytale's existing Quality system** for determining affix capacity per item tier.
  - Common, Uncommon, Rare, Epic, Legendary tiers define how many prefixes/suffixes can appear.
  - No separate "Rarity" concept; Quality serves this purpose.
- **Store affix data in ItemStack.metadata** using a `"Hyforged"` key.
  - Use `ItemStack.withMetadata()` and `getFromMetadataOrNull()` with a typed codec.
  - Affix data serializes to BSON for persistence and JSON for client sync.
- **Tier 1 = Best** convention for affix tiers (ARPG standard).
  - Existing prototype code had inverted convention; this will be corrected.
- **Data-driven affix types** with initial support for prefix, suffix, and forged.
  - Affix types defined in JSON, extensible for future types.
- **Affix capacity rules per Quality** defined in JSON configuration.
  - Default: Common(1p), Uncommon(1p,1s), Rare(2p,2s), Epic(3p,3s), Legendary(4p,4s).
  - Forged affixes managed separately (set to 1 when item is forged).

#### Consequences
- Pros:
  - Leverages Hytale's existing Quality UI (colors, textures, particles).
  - ItemStack metadata is already persisted and replicated; no custom serialization needed.
  - Maintains compatibility with Hytale's item system and loot tables.
  - Quality-based capacity rules are intuitive and match player expectations.
  - Data-driven configuration allows balance tuning without code changes.
- Cons:
  - Cannot modify Quality tiers themselves (e.g., can't add new Quality levels).
  - ItemStack metadata has size limits (unlikely to be an issue for affixes).
  - Existing prototype code must be replaced (minimal sunk cost).

#### Alternatives Considered
- Create separate "Rarity" system parallel to Quality:
  - Rejected: Redundant with Hytale's Quality; confusing for players; extra UI work.
- Store affixes in a separate ECS component on items:
  - Rejected: Items are not entities in Hytale; ItemStack is the canonical item representation.
- Use Hytale's `StatModifiers` field on items for affixes:
  - Rejected: `StatModifiers` is for static modifiers defined in item JSON; not suitable for rolled random values.

#### Links
- Spec: `.memory_bank/Features/items-affix-system/items-affix-system.spec.md`
- Requirements: `.memory_bank/Requirements/rpg-arpg/items-affixes-rarity.md`
- Hytale: `ItemStack.metadata`, `ItemQuality` asset type

---

### ADR-0010: Unified Stat Integration via EntityStatValue Extension
- Date: 2026-01-21
- Status: Accepted
- Implementation Date: 2026-01-21
- Deciders: JBurl

#### Context
- ADR-0001 established a "hybrid" architecture with Hyforged-owned stats layer + Hytale entitystats + bridge systems.
- This approach requires extensive workarounds:
  - Every Hytale system (combat, items, buffs, effects) expects `EntityStatValue`
  - Hyforged must intercept/wrap each integration point with bridge code
  - Adding new features (e.g., buff system) requires writing Hyforged-aware wrappers
  - Cannot use Hyforged stats directly in Hytale item/buff JSON
- The hybrid approach results in "re-writing the whole game" to make Hyforged stats work everywhere.
- Investigation revealed `EntityStatValue` has protected extensibility:
  - `protected EntityStatValue()` — allows subclassing
  - `protected float set(float)` — overridable
  - `protected void computeModifiers(EntityStatType)` — key extension point for stacking logic

#### Decision
- Create `HyforgedStatValue extends EntityStatValue` with ARPG stacking semantics.
- Override `computeModifiers()` to implement FLAT → INCREASED → MORE → CAP order.
- Create installer system that replaces `EntityStatValue` instances in `EntityStatMap` with `HyforgedStatValue` after entity initialization.
- This enables:
  - Items, buffs, effects to use `HyforgedModifier` via standard Hytale APIs
  - Combat and other systems to automatically use ARPG-modified stats
  - Gradual deprecation of bridge workarounds
- Continue using `HyforgedModifier` (ADR-0002) which is already registered with `Modifier.CODEC`.

#### Consequences
- Pros:
  - Single stat representation works with both Hyforged and Hytale systems
  - Items/buffs/effects can use HyforgedModifier directly in JSON
  - Eliminates need for per-subsystem bridge code
  - Significantly reduces codebase complexity and maintenance burden
  - Other mods can use HyforgedModifier without understanding bridge layer
- Cons:
  - Must handle EntityStatMap re-instantiation scenarios
  - Persistence/codec may require custom handling for subclass fields
  - Some HyforgedStatComponent functionality may need to migrate
  - Requires installer system to swap values post-initialization

#### Migration Path
1. Implement HyforgedStatValue and installer (non-breaking)
2. Verify integration with items, combat, buffs
3. Gradually deprecate bridge systems
4. Simplify HyforgedStatComponent to unique functionality only

#### Alternatives Considered
- Continue with hybrid + bridge approach (ADR-0001):
  - Rejected: Unsustainable complexity; every new feature requires bridge code
- Replace EntityStatMap entirely with custom component:
  - Rejected: Would break more Hytale integrations; higher risk
- Use reflection/mixins to modify EntityStatMap:
  - Rejected: Fragile; breaks on Hytale updates; not officially supported
- Intercept at Modifier.apply() level only:
  - Rejected: Hytale calls apply() per-modifier; can't implement proper stacking order

#### Links
- Spec: `.memory_bank/Features/unified-stat-integration/unified-stat-integration.spec.md`
- Plan: `.memory_bank/Features/unified-stat-integration/unified-stat-integration.plan.md`
- Supersedes: ADR-0001 (Hybrid approach)
- Related: ADR-0002 (HyforgedModifier), ADR-0006 (System replacement)
- Hytale: `EntityStatValue.java`, `EntityStatMap.java`

---

### ADR-0011: Runtime Quality Replacement for Items and NPCs
- Date: 2026-01-23
- Status: Proposed
- Deciders: JBurl

#### Context
- Hytale defines item Quality statically in item JSON files (e.g., `"Quality": "Common"`).
- Quality is loaded into `Item.qualityId` and resolved to `qualityIndex` at asset load time.
- `ItemStack` does not have a dedicated quality override mechanism; it inherits quality from its `Item` config.
- For an ARPG loot system, items need to have random Quality rolled at drop time.
- ADR-0009 established using Hytale's Quality system for affix capacity; random quality is the natural extension.
- As a foundation mod, Hyforged should directly replace quality rather than layering metadata overrides.
- NPCs should also have Quality tiers (elite/boss variants) that affect their stats and loot drops.

#### Decision
- **Replace item Quality at runtime** when items spawn from loot/chests.
  - Research exact mechanism: likely ItemStack metadata with `QualityOverride` key that systems read.
  - If Hytale syncs `Item.qualityIndex` to clients, may need protocol-level override.
  - Alternatively, use item state/variant switching if Hytale supports it.
- **Implement LootQualitySystem** as separate ECS system running before LootAffixSystem.
  - Uses system dependencies for execution order.
  - Emits `QualityRolledEvent` for modding integration.
- **Data-driven quality weights** via JSON configuration.
  - Global defaults, per-category overrides, per-loot-source overrides.
  - Context modifiers (mob level, Magic Find, zone difficulty).
- **NPC Quality Component** for elite/boss variants.
  - `HyforgedNPCQualityComponent` stores quality and optional affixes.
  - NPC quality boosts loot quality weights.
  - NPC stats scaled by quality tier multipliers.
- **Use Hytale's ItemQuality.getAssetMap()** to programmatically access all quality definitions.
  - Filter to equipment-eligible qualities (exclude Junk, Tool, Template, Debug, Developer, Technical).

#### Consequences
- Pros:
  - Enables ARPG-style random loot quality without modifying base item definitions.
  - Foundation mod compatibility: other mods see the "real" quality, not metadata overlay.
  - NPC quality creates elite/boss variety using existing affix infrastructure.
  - Hytale's Quality UI (colors, particles, tooltips) works automatically.
  - Data-driven configuration allows server customization without code.
- Cons:
  - Technical research needed to determine best quality replacement mechanism.
  - May require understanding Hytale's client sync for quality display.
  - NPC quality adds complexity to spawn systems.

#### Alternatives Considered
- Store quality override in ItemStack.metadata only:
  - Rejected: Other systems would see base quality; not a true foundation mod approach.
- Modify Item assets at runtime:
  - Rejected: Would affect all items of that type, not per-instance.
- Create separate "Rarity" parallel to Quality:
  - Rejected: Already rejected in ADR-0009; redundant and confusing.
- Only roll quality for legendary/unique items:
  - Rejected: Full random quality provides better loot variety.

#### Links
- Spec: `.memory_bank/Features/random-item-quality/random-item-quality.spec.md`
- Related: ADR-0009 (Quality for affixes)
- Hytale: `ItemQuality.java`, `Item.java`, `ItemStack.java`