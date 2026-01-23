# Requirements: Items (Affixes & Quality)

## Vision
- Extend items with an ARPG-style affix system (prefix, suffix, forged) that produces meaningful, readable loot variety and cleanly integrates with Hytale's existing Quality system, the stats system, inventory UI, crafting/enchanting, and trading.

## Goals
- Quality tiers (use Hytale's existing system)
  - Items have a Quality tier that influences affix capacity.
  - Quality tiers use Hytale's existing Quality definitions (Junk, Common, Uncommon, Rare, Epic, Legendary).
  - Quality is consistently surfaced in UI (colors, labels, tooltips) via existing Hytale mechanisms.
- Affix system (data-driven types)
  - Items can roll affixes of configurable types (initially: prefix, suffix, forged).
  - Affixes have tiers where Tier 1 is the strongest roll and higher tier numbers are weaker.
  - Affixes define:
    - Stat modifications (using the Stats System modifier model)
    - Tiered value ranges per tier
    - Eligibility constraints (item types, item level, Quality constraints, mutually exclusive groups)
    - Weighting for roll probability
  - The affix system is fully extensible and data-driven.
- Affix capacity rules (data-driven per Quality)
  - The number of affixes on an item depends on item Quality.
  - Default capacity rules:
    - Common: 1 prefix, 0 suffix
    - Uncommon: 1 prefix, 1 suffix
    - Rare: 2 prefixes, 2 suffixes
    - Epic: 3 prefixes, 3 suffixes
    - Legendary: 4 prefixes, 4 suffixes
  - All capacities configurable via JSON.
  - Forged affixes added via forging mechanic (0 by default, set to 1 when forged).
- Loot generation
  - Dropped items roll affixes at creation time via Hytale loot system event listeners.
  - Affix eligibility is restricted to equipment defined by tags/categories.
  - Affix eligibility based on item's `ItemLevel` field.
  - Loot generation is deterministic given a seed where feasible (for debugging/auditability).
- UI integration
  - Item tooltips display:
    - Quality tier (existing Hytale display)
    - Prefix and suffix names integrated into item name
    - Affix details with tier indicators and stat values
    - Detailed breakdown on hover
  - Character stats screen displays stat breakdowns including affix contributions.
- Extensible API
  - Provide an API for other plugins/systems to:
    - Register new affix types and definitions
    - Modify affix eligibility rules
    - Create items with specific affixes (for quests/crafting)
    - Query affix data from an item
- Safety and integrity
  - Server is authoritative for item generation, affix rolls, and stat application.
  - Item descriptions shown to players must match server-effective stats.

## Non-Goals
- Replacing the entire base item system; this layers affixes on top.
- A fully player-driven affix editor (outside of crafting/enchanting flows).
- Unique items with fixed affixes (deferred to future scope).
- Set bonuses.

## Quality Attributes
- Readability: item text is not overwhelming; abbreviations are consistent.
- Balance-friendly: weights, tiers, and caps are configurable.
- Interoperable: affixes are expressed using the shared Stats System.
- Secure: prevents spoofed client tooltips or "fake affix" exploits.

## Feature Index
- Quality integration
  - Use Hytale's existing Quality system
  - Data-driven capacity rules per Quality tier
- Affix model
  - Data-driven affix types (prefix, suffix, forged)
  - Tier system (T1 best)
  - Eligibility constraints and weighting
- Generation
  - Drop-time roll process via loot event listeners
  - Item level based tier eligibility
  - Deterministic pool resolution when multiple pools match
  - **Random Quality rolling** — see [random-item-quality.spec.md](../../Features/random-item-quality/random-item-quality.spec.md)
- UI
  - Tooltip affix display
  - Character stats screen
- API and integration
  - Register/query affixes
  - Create deterministic items for rewards
- NPC Quality
  - Elite/boss NPC variants with quality tiers
  - NPC affixes for stat modifications
  - Loot quality boosted by NPC quality

## Change Log
- 2026-01-23: Added Random Quality rolling and NPC Quality features; linked random-item-quality spec.
- 2026-01-20: Updated to use Hytale Quality system; clarified affix types and capacity rules; linked spec.
- 2026-01-20: Clarified equipment eligibility and deterministic pool resolution.
- 2026-01-19: Initial version drafted.
