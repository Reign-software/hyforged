# Requirements: Items (Affixes & Rarity)

## Vision
- Extend items with an ARPG-style rarity + prefix/suffix affix system that produces meaningful, readable loot variety and cleanly integrates with the stats system, inventory UI, crafting/enchanting, and trading.

## Goals
- Rarity tiers
  - Items have a rarity tier that influences baseline quality and affix capacity.
  - Rarity tiers use the game’s existing rarity definitions where possible.
  - Rarity is consistently surfaced in UI (colors, labels, tooltips) in all contexts (inventory, ground drops, trade, crafting).
- Affix system (prefix/suffix)
  - Items can roll prefixes and suffixes that modify stats and/or grant special effects.
  - Affixes have tiers where Tier 1 is the strongest roll and higher tier numbers are weaker.
  - Affixes define:
    - Stat modifications (using the Stats System modifier model)
    - Tiered value ranges per tier
    - Eligibility constraints (item types, required level, rarity constraints, mutually exclusive groups)
    - Weighting for roll probability
  - The affix system is extensible and data-driven.
- Affix capacity rules
  - The number of affixes on an item depends on item rarity.
  - Rules exist for:
    - Minimum/maximum prefixes
    - Minimum/maximum suffixes
    - Special cases (unique-like items, quest items, etc.)
- Loot generation
  - Dropped items can roll rarity and affixes at spawn/creation time.
  - Loot rules are configurable by content type (enemy type, region difficulty, quest reward).
  - Loot generation is deterministic given a seed where feasible (for debugging/auditability).
- UI integration
  - Inventory UI displays:
    - Rarity tier
    - Prefix and suffix names
    - Resulting stat changes (summary)
    - Detailed breakdown on hover/tooltips
  - Ground-drop UI (labels/tooltips) includes rarity and affix names in a readable format.
- Extensible API
  - Provide an API for other plugins/systems to:
    - Register new affixes and tiers
    - Modify affix eligibility rules
    - Create items with specific affixes (for quests/crafting)
    - Query affix data from an item
- Safety and integrity
  - Server is authoritative for item generation, affix rolls, and stat application.
  - Item descriptions shown to players must match server-effective stats.

## Non-Goals
- Replacing the entire base item system; this layers affixes/rarity on top.
- A fully player-driven affix editor (outside of crafting/enchanting flows).

## Quality Attributes
- Readability: item text is not overwhelming; abbreviations are consistent.
- Balance-friendly: weights, tiers, and caps are configurable.
- Interoperable: affixes are expressed using the shared Stats System.
- Secure: prevents spoofed client tooltips or “fake affix” exploits.

## Feature Index
- Rarity model
  - Tier definitions and UI representation
  - Capacity rules for affixes
- Affix model
  - Prefix vs suffix semantics
  - Tier system (T1 best)
  - Eligibility constraints and weighting
- Generation
  - Drop-time roll process
  - Configurable rules by source
- UI
  - Inventory display
  - Ground label display
  - Tooltip breakdown
- API and integration
  - Register/query affixes
  - Create deterministic items for rewards

## Change Log
- 2026-01-19: Initial version drafted.
