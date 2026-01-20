# Feature Spec: Hyforged Stats System

## Metadata
- Feature ID (slug): hyforged-stats-system
- Status: Complete
- Owner: JBurl
- Date: 2026-01-19

## Summary
- Implement a Hyforged-owned stats framework (ability scores + 30–40 derived stats) with deterministic integer math, ARPG-style modifier stacking, and UI breakdowns.
- Reuse Hytale’s built-in `entitystats` system for “resource bar” stats (e.g., Health, Mana, Stamina) and network replication where appropriate.
- Bridge Hyforged-derived outcomes into Hytale stats by applying server-side modifiers to built-in stats (e.g., Constitution → +MaxHealth).

## Goals
- Provide ability scores (primary stats) and a large catalog of derived stats (secondary stats) for players and NPCs.
- Support ARPG stacking semantics with deterministic order:
  - Flat (+X)
  - % Increased/Decreased
  - % More/Less
  - Caps/clamps
- Use integer-only storage and computation (no floating-point exposed as authoritative stat values).
- Model Armor/Evasion (and other rating stats) as integer “ratings”; resistances are flat percent stats.
- Present a player-facing character sheet and tooltips with explainable breakdowns by source (equipment, buffs, passives, class progression, level/ability scores).
- Provide an extensible API for other plugins/systems to register stats and apply/query modifiers.

## Non-Goals
- Replacing Hytale’s existing `Entity/Stats/*` resource stats definitions (Health/Mana/Stamina, regeneration, min/max effects).
- Relying on client authority for any stat computation or persistence.
- Shipping final balance constants (curves and conversion constants will be config-driven and iterated).

## User Experience
- Players can view:
  - Ability scores
  - Derived stats organized by category
  - For rating-based stats (Armor/Evasion/Accuracy), both:
    - Rating value
    - Estimated effectiveness versus a selected/typical target level
- Players can inspect a stat to see a breakdown of contributors:
  - Base from ability scores
  - Item affixes/enchantments
  - Buffs/debuffs (effects)
  - Passives
  - Class bonuses
- When Hyforged affects built-in resources (e.g., MaxHealth), players see the updated resource cap reflected in existing UI.

## Functional Requirements
- Hybrid stats architecture
  - Maintain built-in Hytale `EntityStatMap` for resource-like stats and replication.
  - Maintain Hyforged Stats state separately for:
    - Ability scores
    - Derived stats
    - Modifier sources and breakdown attribution
  - Provide a bridge that exports Hyforged-derived adjustments into Hytale stats via server-side modifiers.
    - v1 exports only resource cap changes: Health MAX, Mana MAX, and Stamina MAX.
- Stat catalog
  - Ability scores: configurable, with canonical IDs and descriptions.
  - Derived stats: ~30–40 at initial launch, with canonical IDs, categories, and descriptions.
  - Stats support metadata needed for UI:
    - Category
    - Display formatting (plain number, rating, percent, Hyforged basis points (0–10000), etc.)
    - Caps (if any)
    - Tags (optional, scoped; used for a small number of “apply to all X” style modifiers)
  - Item-affix metadata (for stats that can roll on items)
    - Whether the stat is eligible as a `prefix` and/or `suffix`
    - Which item categories/slots it can appear on
    - Tier table embedded within the stat definition (Tier 1 best)
      - Define the stat’s max tier
      - Define per-tier min/max roll values
      - Define per-item-rarity max tier and per-tier weights within that rarity
    - Forging metadata: whether the stat is eligible to roll on the special **Forged** line, and any forged-only tier/pool overrides

### Proposed v1 Stat List

#### Ability Scores (Primary)
- `strength` — melee scaling.
- `dexterity` — precision, evasion scaling, ranged scaling.
- `intelligence` — spell scaling, elemental scaling.
- `constitution` — survivability; drives max health and regen.
- `wisdom` — crafting efficiency and success rates. Enchanting.
- `spirit` — mana scaling and regeneration scaling.
- `luck` — loot outcomes and chance-based systems (e.g., item rarity/quantity, crit/ailment outcomes if enabled), crit chance scaling.

#### Derived Stats (Secondary) (PoE-style candidate pool; select ~40 for v1)

**Resources**
- `max-health-flat` (exported → Hytale Health MAX)
- `max-mana-flat` (exported → Hytale Mana MAX)
- `max-stamina-flat` (exported → Hytale Stamina MAX)
- `health-regen-flat`
- `mana-regen-flat`
- `stamina-regen-flat`

**Recovery / Sustain (PoE-style knobs)**
- `life-on-kill-flat`
- `mana-on-kill-bps` (percent of max)
- `health-regen-percent-bps` (percent of max per second)
- `mana-regen-percent-bps` (percent of max per second)
- `damage-recouped-as-life-bps`
- `damage-recouped-as-mana-bps`
- `physical-attack-damage-leech-life-bps`
- `physical-attack-damage-leech-mana-bps`
- `life-recovery-rate-bps`
- `mana-recovery-rate-bps`

**Offense (General)**
- `attack-power`
- `spell-power`
- `damage-increased-bps` (generic)
- `attack-damage-increased-bps`
- `spell-damage-increased-bps`
- `melee-damage-increased-bps`
- `ranged-damage-increased-bps`
- `projectile-damage-increased-bps`
- `physical-damage-increased-bps`
- `fire-damage-increased-bps`
- `cold-damage-increased-bps`
- `lightning-damage-increased-bps`
- `poison-damage-increased-bps`
- `bleed-damage-increased-bps`
- `attack-speed-bps`
- `cast-speed-bps`
- `accuracy-rating`

**Actions / Projectiles (PoE2-inspired; system-gated)**
- `cooldown-recovery-rate-bps`
- `effect-duration-bps` (buffs/debuffs/projectiles)
- `area-of-effect-bps`
- `projectile-speed-bps`
- `projectiles-additional-flat` (+N)

**Critical**
- `crit-chance-bps`
- `crit-multiplier-bps`
- `critical-damage-taken-bps`

**Defense (Ratings)**
- `armor-rating`
- `evasion-rating`
- `block-chance-bps`
- `dodge-chance-bps`
- `damage-reduction-cap-bps` (global cap control)

**Defense (Caps / Utility)**
- `maximum-elemental-resistance-bps`
- `maximum-fire-resistance-bps`
- `maximum-cold-resistance-bps`
- `maximum-lightning-resistance-bps`
- `maximum-poison-resistance-bps`
- `stun-threshold-increased-bps`
- `stun-recovery-bps`
- `debuff-duration-on-you-bps`
- `ailment-effect-on-you-bps`

**Elemental / Ailment (Resistances + Chances)**
- `fire-resistance-bps`
- `cold-resistance-bps`
- `lightning-resistance-bps`
- `poison-resistance-bps`
- `bleed-resistance-bps`
- `ignite-chance-bps`
- `freeze-chance-bps`
- `shock-chance-bps`
- `poison-chance-bps`
- `bleed-chance-bps`

**Ailment Power (optional; depends on ailment model)**
- `ignite-effect-bps`
- `freeze-effect-bps`
- `shock-effect-bps`
- `poison-effect-bps`
- `bleed-effect-bps`

**Penetration / Exposure**
- `fire-penetration-rating`
- `cold-penetration-rating`
- `lightning-penetration-rating`
- `physical-penetration-rating`
- `exposure-effect-bps` (if “exposure” exists)

**Mitigation vs Damage Types**
- `physical-damage-taken-bps` (can be negative)
- `elemental-damage-taken-bps` (can be negative)
- `projectile-damage-taken-bps` (can be negative)

**Retaliation / Reactive (optional; system-gated)**
- `thorns-flat`
- `thorns-damage-increased-bps`

**Damage Routing (optional; system-gated)**
- `damage-taken-from-mana-before-life-bps`

**Requirements / Efficiency (PoE2-inspired; system-gated)**
- `attribute-requirements-reduced-bps` (equipment)
- `defences-increased-bps` (applies to tag: `defences`)

**Auras / Buffs (optional; system-gated)**
- `aura-magnitude-bps`
- `buff-effect-bps`

**Loot/Utility (optional v1, but useful for ARPG)**
- `item-rarity-increased-bps`
- `item-quantity-increased-bps`

### Tags

Tags are extensible and can be combined (e.g., a combat event can be tagged `spell` + `cold` + `projectile`).

A “tagged modifier” may apply to all stats under a tag. The system permits tag-wide modifiers for any tag; balance is managed by controlling which tags exist and how frequently content grants tag-wide modifiers.

**Core Stat Tags (for stat-to-stat modifiers)**
- `attributes`
  - Affects: `strength`, `dexterity`, `intelligence`, `constitution`, `wisdom`, `spirit`, `luck`
- `defences`
  - Affects: `armor-rating`, `evasion-rating`
- `elemental-resistances`
  - Affects: `fire-resistance-bps`, `cold-resistance-bps`, `lightning-resistance-bps`
- `life-leech`
  - Affects: `physical-attack-damage-leech-life-bps`
- `mana-leech`
  - Affects: `physical-attack-damage-leech-mana-bps`

**Core Combat Tags (for spell/attack categorization)**

Combat systems may attach tags to actions/damage events so that category stats apply consistently. This enables “spell vs attack” and “cold vs lightning vs physical” style scaling.

- `spell`
  - Affected stats (examples): `spell-power`, `spell-damage-increased-bps`, `cast-speed-bps`
- `ability`
  - Affected stats (examples): `ability-power`, `ability-damage-increased-bps`, `ability-cooldown-recovery-bps`
- `skills`
  - Definition: shorthand tag meaning all actions tagged `spell` or `ability`
  - Notes: Hyforged does not have skill levels; “+All Skills” is a display term for a modifier targeting tag `skills` (i.e., all spells and all abilities)
- `attack`
  - Affected stats (examples): `attack-power`, `attack-damage-increased-bps`, `attack-speed-bps`, `accuracy-rating`
- `melee`, `ranged`, `projectile`
  - Affected stats (examples): `melee-damage-increased-bps`, `ranged-damage-increased-bps`, `projectile-damage-increased-bps`
- `physical`, `fire`, `cold`, `lightning`, `poison`, `bleed`
  - Affected stats (examples): `physical-damage-increased-bps`, `fire-damage-increased-bps`, `cold-damage-increased-bps`, `lightning-damage-increased-bps` and matching `*-penetration-rating` / `*-resistance-bps` where applicable

### Data-Driven Modding (JSON)

Hyforged stats must be data-driven so external mods/plugins can extend Hyforged without requiring Java changes.

- Stat definitions (ability + derived), tag definitions, and display metadata are loadable from JSON.
- Content may define new:
  - stats
  - tags
  - tag-to-stat relationships (which stats a tag affects)
  - item-affix metadata (see item integration below)
- Hyforged ships a default “core ruleset”; external mods can add additional definitions and/or override specific display metadata where allowed.
- All user-facing IDs are namespaced to avoid collisions (e.g., `hyforged:armor-rating`, `othermod:shadow-resistance-bps`).
- Definitions are versioned to support migrations and backward compatibility.

Hyforged should leverage Hytale’s existing mod-folder/asset loading system for discovering and loading these JSON definitions (rather than inventing a parallel config loader).

### Item System Integration (Affixes, Tiers, Prefix/Suffix Pools)

The Stats System provides the modifier model and tagging semantics; the Items system provides item contexts (weapon/armor/jewelry types, rarity, and affix rolls).

- Items do not display affix names as text; instead, items have prefix/suffix capacity (e.g., “3 prefixes, 3 suffixes”) and show the rolled stat lines.
- Rollable stats define their own affix behavior:
  - whether they are eligible as a `prefix` and/or `suffix`
  - which item categories/slots they can appear on
  - tier table (Tier 1 best) and per-tier min/max roll values
  - optional weighting and/or mutual exclusion constraints (where needed)
- Item rarity constrains which tiers are eligible and the relative weights of each tier during rolling.
- When an item rolls affixes, it selects stats from the eligible prefix/suffix sets until the item’s prefix/suffix capacity is filled.
  - After a stat is rolled in a given pool (prefix or suffix), it is removed from consideration for subsequent rolls in that same pool.
- Uniqueness rule (non-forged items): a given stat ID may appear at most once on the same item.
- Forging: when an item becomes **forged**, it gains exactly one additional special stat line called **Forged**.
  - The Forged line rolls using special forging rules:
    - may roll from an expanded stat pool ("any stat")
    - may access higher stat tiers than normally allowed
    - may roll special high-value/global stats (e.g., `+All Defences`, `+All Skills` where “All Skills” means tag `skills` = all spells and all abilities)
    - does not consume prefix/suffix capacity (it is an additional line)
    - may ignore normal item slot/category eligibility restrictions when selecting from the expanded pool
  - Stat uniqueness still applies unless a specific forged-only rule explicitly permits duplicates.
- Tag-wide modifiers on items
  - Tag-wide modifiers are allowed on non-forged items.
  - By default, tag-wide modifier rolls have lower weighting than direct-stat rolls, unless the item definition explicitly overrides weights.
- Stat modifiers produced by items can target:
  - a specific stat ID (direct stat modifier)
  - a stat tag (tag-wide modifier, e.g., “% increased Defences” applying to tag `defences`)
  - optional conditions (system-gated; e.g., “while on Low Life” style constraints)
- Equipped item changes must translate deterministically into:
  - adding/removing modifiers with source metadata (item id + rolled stat id + tier)
  - updating stat breakdown attribution and UI tooltips

### Stat Computation Requirements

- Modifier framework
  - Modifiers are first-class and attributable (source metadata) and can be temporary or permanent.
  - Support stacking semantics:
    - Flat
    - % Increased/Decreased
    - % More/Less
    - Caps/clamps
  - Provide deterministic evaluation order and stable tie-breaking.
- Integer-only math
  - Define canonical integer units for:
    - Chances in Hyforged basis points (0–10000)
    - Percent values (if stored) in Hyforged basis points (0–10000)
    - Ratings (Armor/Evasion/Accuracy)
  - Ensure rounding rules are explicit and consistent.
  - By default, derived stats are clamped to non-negative values; v1 only requires negative values for `*-damage-taken-bps` style mitigation stats.
- Rating-to-effectiveness conversions
  - For Armor/Evasion/Accuracy (and other rating stats), compute effectiveness as a function of rating and target level.
  - Use Option A (PoE-like, bounded growth):
    - Base (for non-negative ratings): `effectivenessBps = rating * 10000 / (rating + k * targetLevel)`
    - Signed extension (to allow negative ratings without singularities):
      - Let `r = rating`.
      - Let `den = abs(r) + k * targetLevel`.
      - `effectivenessBps = sign(r) * (abs(r) * 10000 / den)`
    - `k` is configurable per stat family (armor/evasion/accuracy) and per-damage-type where needed.
      - Default (configurable; confirmed): `kArmor = 10`, `kEvasion = 10`, `kAccuracy = 10`.
    - All computations use integer math with a widening intermediate type (e.g., long) to avoid overflow.
    - Division rounding defaults to floor; UI may show rounded display while combat uses the canonical rule.
  - Ratings are integers and are clamped at configurable minimum/maximum bounds.
    - Proposed default: allow negative ratings; enforce bounds to prevent extreme values.
    - Conversion outputs are also clamped in Hyforged basis points to avoid extremes (e.g., a configurable `minEffectivenessBps` and `maxEffectivenessBps`).
  - Conversion output is used for combat resolution and UI preview.
- Players and NPCs
  - Stats apply to players and NPCs.
  - NPCs have a stat profile and/or level-based scaling inputs so they can participate in the same combat math.
- Persistence
  - Persist minimal player-owned state:
    - Ability score allocations (and/or derived from level)
    - Passive allocations (owned by passive system but consumed here)
    - Class progression (owned by class system but consumed here)
  - Recompute derived stats on load.
- Extensible API (conceptual)
  - Register stat definitions (ability/derived).
  - Apply/remove modifiers by source.
  - Query effective stat values.
  - Subscribe to stat change events.
  - Provide read access to breakdowns for UI.

## Non-Functional Requirements
- Determinism
  - Same inputs produce the same outputs across server restarts.
  - Integer math and rounding rules are documented.
- Performance
  - Stat recomputation is change-driven (cache + invalidation), not per-tick for all entities.
  - Bound worst-case modifier count and guard against pathological modifier explosions.
- Compatibility
  - Exported resource caps (MaxHealth/MaxMana/MaxStamina) integrate cleanly with existing Hytale UI/packets.

## Dependencies
- Hytale built-in stats:
  - `com.hypixel.hytale.server.core.modules.entitystats.*`
  - `Entity/Stats/*` assets for Health/Mana/Stamina etc.
- Hytale effect system for buffs/debuffs:
  - `EntityEffect` assets and effect controller component (as a modifier source)
- Inventory and item systems (as modifier sources)

## Data/Schema Impact
- Add Hyforged-owned persistent player stats data (ability score allocations and versioned migrations).
- Add Hyforged-owned runtime component(s) for computed stats, modifier registry, and cached breakdowns.
- Add JSON definitions for stats/tags, including item-roll metadata on stats (prefix/suffix eligibility, item eligibility, and tier tables) (owned by Hyforged; extensible by other mods).

## API Changes
- Introduce a public Hyforged Stats API surface for other plugins/systems.
- Define reserved namespaces/keys for:
  - Stat IDs
  - Modifier source IDs
  - Bridge keys when exporting into Hytale `EntityStatMap`
  - JSON definition IDs (stats, tags, and optional tier-table assets if tiers are not embedded)

## Security/Privacy
- Server authoritative stat computation and persistence.
- Validate all modifier application entry points; avoid untrusted client input.
- Audit log for admin/debug-only stat mutation tools.

## Observability
- Debug mode trace for stat calculation of a single entity:
  - inputs → intermediate buckets → final outputs
- Metrics:
  - recompute counts
  - average recompute time
  - modifier counts per entity

## Risks
- Complexity of large stat catalog + UI breakdowns.
- Balance sensitivity of rating→% formulas; requires iteration.
- Bridging into Hytale `entitystats` incorrectly could desync UI if update frequency isn’t controlled.

## Open Questions
- JSON modding: how should conflicts be resolved when multiple mods define the same stat/tag IDs (error, deterministic override order, or last-wins by mod load order)?

## Acceptance Criteria
- [x] Hyforged can compute effective stats for players and NPCs using integer math.
- [x] Hyforged supports Flat / Increased / More / Caps with deterministic stacking.
- [x] Constitution (or equivalent) can increase Hytale Health Max via the bridge.
- [x] Hyforged can increase Hytale Stamina Max via the bridge.
- [x] UI can display stat values and a per-stat breakdown by source.
- [x] Rating-based resists/armor/evasion show effectiveness vs target level and are used by combat.
- [x] Performance is change-driven; stat recompute is not unconditional per tick.
- [x] Items enforce one roll per stat ID by default; forged items add exactly one additional **Forged** stat line with expanded eligibility.

## Impacted Areas (High-Level)
- Hyforged plugin: new stats module + persistence + UI panels
- Combat system: consumes Hyforged effective stats and rating conversions
- Items/affixes/enchanting/passives/class: act as modifier sources

## Required Codebase/Architecture Changes (High-Level)
- Add a Hyforged Stats module with:
  - Stat definitions registry (ability + derived)
  - Modifier engine with deterministic integer stacking
  - Cache/invalidation and change events
- Add a bridge layer to apply Hyforged-derived resource cap changes into Hytale `EntityStatMap` via modifiers.
- Add persistence for player ability score allocations (versioned).
- Add UI surfaces (inventory/character sheet) for stat browsing and breakdown.

## References
- Requirements entry: .memory_bank/Requirements/rpg-arpg/stats-system.md
- Relevant built-in system: Hytale `entitystats` module (`Entity/Stats/*`, `EntityStatMap`, `StaticModifier`)
- ADR: ADR-0001 (Hybrid Hyforged + Hytale stats)
