# Requirements: RPG/ARPG Systems

## Vision
- Deliver a server-authoritative RPG/ARPG progression layer (levels, stats, items, combat, classes, passives, currency, enchanting, trading) that feels deep like an ARPG while remaining performant, understandable, and extensible for future systems and other plugins.

## Goals
- Provide a single, coherent “character power” model that connects: experience → levels → ability scores → derived stats → combat outcomes.
- Ensure all gameplay-critical state is server authoritative and persists reliably.
- Keep all systems data-driven and extensible (new stats, affixes, classes, passives, enchantments, currencies, trade rules).
- Present player-facing information clearly (UI + inventory surfaces) with transparent breakdowns where appropriate.

## Non-Goals
- Replacing or re-authoring all base-game content, items, or encounters.
- Client trust for any economy/combat-critical decisions.
- Shipping final balance values up-front; curves and weights are expected to evolve via configuration.

## Quality Attributes
- Extensibility: clear integration points for other plugins/systems.
- Determinism: stat/combat math should be consistent and reproducible.
- Performance: bounded computation per tick and per combat event.
- Observability: loggable/auditable events for progression, currency, and trades.
- Backward compatibility: saved data supports migrations.

## Feature Index
- [Experience System](experience-system.md)
- [Stats System](stats-system.md)
- [Items: Affixes & Rarity](items-affixes-rarity.md)
- [Combat System](combat-system.md)
- [Class System](class-system.md)
- [Passive Trees](passive-trees.md)
- [Currency: Tradebars](currency-tradebars.md)
- [Enchanting, Runes, Forging](enchanting-runes-forging.md)
- [Trading & Marketplace](trading-marketplace.md)

## Change Log
- 2026-01-19: Initial requirements set scaffold created.
