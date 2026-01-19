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

## Implementation Order

The systems are grouped into phases based on dependencies. Complete each phase before moving to the next.

### Phase 1: Foundation
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 1 | [Stats System](stats-system.md) ✅ | None | Defines stat taxonomy, modifier model, and API. Everything else builds on this. |

### Phase 2: Entity Application
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 2 | [Entity Stats](entity-stats.md) | Stats System | Attaches stats to players/NPCs. Required before any system can *apply* stats to entities. |

### Phase 3: Core Progression
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 3 | [Experience System](experience-system.md) | Entity Stats | Levels grant ability score points applied via Entity Stats. |
| 4 | [Class System](class-system.md) | Entity Stats, Experience System | Class levels grant ability score bonuses; shares progression concepts with XP. |

### Phase 4: Items & Equipment
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 5 | [Items: Affixes & Rarity](items-affixes-rarity.md) | Stats System, Entity Stats | Affixes use the modifier model; equipping items applies stats to entities. |

### Phase 5: Combat
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 6 | [Combat System](combat-system.md) | Entity Stats, Items | Consumes effective stats for combat math; weapons/armor provide damage/defense. |

### Phase 6: Advanced Progression
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 7 | [Passive Trees](passive-trees.md) | Entity Stats, (Combat for testing) | Passives add modifiers to entities. Combat helps validate the impact. |

### Phase 7: Economy & Crafting
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 8 | [Currency: Tradebars](currency-tradebars.md) | None (can start earlier) | Economy foundation. Could be developed in parallel with earlier phases. |
| 9 | [Enchanting, Runes, Forging](enchanting-runes-forging.md) | Items, Currency | Modifies items; consumes currency for crafting costs. |

### Phase 8: Social & Trade
| Order | System | Dependencies | Notes |
|-------|--------|--------------|-------|
| 10 | [Trading & Marketplace](trading-marketplace.md) | Items, Currency | Player-to-player economy; requires items to trade and currency for pricing. |

---

## Feature Index
- [Stats System](stats-system.md)
- [Entity Stats](entity-stats.md)
- [Experience System](experience-system.md)
- [Class System](class-system.md)
- [Items: Affixes & Rarity](items-affixes-rarity.md)
- [Combat System](combat-system.md)
- [Passive Trees](passive-trees.md)
- [Currency: Tradebars](currency-tradebars.md)
- [Enchanting, Runes, Forging](enchanting-runes-forging.md)
- [Trading & Marketplace](trading-marketplace.md)

## Change Log
- 2026-01-19: Added implementation order with phased dependencies.
- 2026-01-19: Added Entity Stats requirement for applying stats to players and NPCs.
- 2026-01-19: Initial requirements set scaffold created.
