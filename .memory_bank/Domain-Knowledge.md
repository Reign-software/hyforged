# Domain Knowledge

## Domain Overview
- Hyforged adds a server-authoritative RPG/ARPG progression layer on top of the base game (experience, levels, stats, affixed items, class progression, passive trees, currency, enchanting, trading).
- The client UI is a presentation layer; all gameplay-critical state and outcomes are validated and computed server-side.

## Core Concepts
- Player Level: global character level (cap 100) earned via XP; grants ability-score growth.
- Class: determined by equipped weapon category; each class has its own class level and passive tree.
- Ability Scores: primary stats that are the only direct reward from leveling.
- Derived Stats: secondary stats computed from ability scores plus modifiers (equipment, buffs, passives, etc.).
- Modifiers: additive/percentage/multiplicative/capped contributions applied to stats with deterministic stacking rules.
- Affixes (Prefix/Suffix): tiered item modifiers (Tier 1 best) that contribute stat modifiers/effects.
- Tradebars: the core currency item used across sinks (respec, enchanting, trading, forging).
- Runes: tradeable progression items used to learn/upgrade enchantments.
- Forging: an irreversible item operation that locks further modification and grants a powerful random enchantment.

## Glossary
- Affix: A named modifier rolled on an item; categorized by type (prefix, suffix, forged).
- Affix Type: A category of affix (prefix, suffix, forged); extensible via JSON.
- Tier: A strength band for an affix or enchantment; Tier 1 is the strongest.
- Quality: Hytale's built-in item quality tier (Junk, Common, Uncommon, Rare, Epic, Legendary) that influences affix capacity.
- Item Level: An item's power level that determines eligible affix tiers.
- Passive Point: A spendable point used to allocate nodes in a class passive tree.
- Atomic Transaction: A server-side exchange that either fully succeeds or fully fails with no partial state.

## Business Rules
- Player level cap is 100 and must be clearly shown in UI.
- Character level ability-score rewards are optional and data-driven via `hyforged:default` level rewards.
- Character level grants 1 general passive point per level (100 total at cap).
- Each class level grants 1 passive point.
- Passive point refunds require Tradebars.
- Tradebars are an item and stack to 500 per inventory slot.
- Forged items become unmodifiable (no enchanting or disenchanting after forging).

## Integrations
- Experience System → Player Level → Ability Scores.
- Stats System is the source of truth for effective stat values and modifier stacking.
- Items (rarity/affixes/enchantments) express their power via Stats System modifiers.
- Combat System consumes effective stats to compute outcomes (damage/healing/mitigation).
- Currency (Tradebars) is a shared dependency for enchanting, passive refunds, and trading.
- Trading/Marketplace operates on server-authoritative item and currency state.
- Passive trees depend on node-ID stability; when layouts change, allocations must be validated and migrated so effects remain consistent after reconnect/restart.

## Assumptions
- The base game provides item definitions and rarity concepts that can be reused and extended.
- The plugin can add server-side data persistence for player progression.
- The plugin can provide/extend UI surfaces to display progression and item metadata.

## Risks
- Large passive tree UI and data transfer may require careful performance design.
- Economy/trading features increase the risk of duplication and fraud exploits without strict atomicity.
- Save format evolution requires robust migrations for long-lived servers.

## References
- Requirements set: .memory_bank/Requirements/rpg-arpg/README.md
