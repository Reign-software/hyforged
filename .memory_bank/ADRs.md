# Architecture Decision Records (ADRs)

## ADR Index

- ADR-0001: Hybrid Hyforged + Hytale Stats (2026-01-19) — Accepted
- ADR-0002: Extend Hytale Modifier System (2026-01-19) — Accepted
- ADR-0003: Data-Driven Stat and Tag Definitions (2026-01-19) — Superseded by ADR-0005
- ADR-0004: Data-Driven Category Definitions (2026-01-19) — Accepted
- ADR-0005: Tags as Simple Strings (2026-01-19) — Accepted
- ADR-0006: Replace Hytale Stat/Damage Systems for Exclusive Hyforged Control (2026-01-20) — Accepted
- ADR-0007: Data-Driven Damage Type Extensions (2026-01-20) — Accepted


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
- Status: Accepted
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