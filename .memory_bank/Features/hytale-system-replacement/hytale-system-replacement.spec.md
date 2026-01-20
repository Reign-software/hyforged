# Feature Specification: Hytale System Replacement

## Metadata
- Feature ID (slug): hytale-system-replacement
- Status: Complete
- Owner: JBurl
- Date: 2026-01-20
- Related ADR: ADR-0006

## Summary
Replace Hytale's native armor/damage/stat systems with Hyforged equivalents so that Hyforged exclusively controls stat contributions from armor, effects, and weapons. This eliminates "double dipping" where both systems modify stats and ensures ARPG stacking rules apply consistently.

## Problem Statement
Hytale has built-in systems that modify entity stats based on armor, weapons, and effects:
- `EntityStatsSystems.Recalculate` applies armor/weapon stat modifiers via `StatModifiersManager`
- `DamageSystems.ArmorDamageReduction` reduces damage based on `ItemArmor.DamageResistance`
- `DamageSystems.ArmorKnockbackReduction` reduces knockback based on armor

These systems read directly from Hytale's JSON fields (`ItemArmor.StatModifiers`, `DamageResistance`) and use simple additive/multiplicative stacking. This conflicts with Hyforged's ARPG modifier system which uses FLAT/INCREASED/MORE/CAP stacking.

## Goals
- Hyforged exclusively controls armor and effect stat contributions
- ARPG stacking (FLAT/INCREASED/MORE/CAP) applies to all defense stats
- Consistent damage reduction formula across all sources
- Eliminate double-dipping from parallel systems
- Maintain compatibility with Hytale's health/death mechanics

## Non-Goals
- Replacing `DamageSystems.ApplyDamage` (needed for health subtraction and client sync)
- Modifying Hytale's core `LivingEntity` class
- Supporting Hytale's native `ItemArmor.StatModifiers` JSON field for stat bonuses

## Functional Requirements

### FR-1: System Unregistration
- On plugin startup, unregister conflicting Hytale systems:
  - `DamageSystems.ArmorDamageReduction`
  - `DamageSystems.ArmorKnockbackReduction`
  - `EntityStatsSystems.Recalculate`
- Use `ComponentRegistry.unregisterSystem(Class)` API

### FR-2: Hyforged Damage Reduction System
- Create `HyforgedDamageReductionSystem` extending `DamageEventSystem`
- Query for entities with `HyforgedStatComponent`
- Read resistance stats from `HyforgedStatComponent` (e.g., `PhysicalResistance`, `FireResistance`)
- Apply ARPG damage reduction formula: `damage = damage * (1 - resistance%)`
- Support damage type inheritance (e.g., `Physical` → `Bleed`)
- Run in `FilterDamageGroup` before `ApplyDamage`

### FR-3: Hyforged Knockback Reduction System
- Create `HyforgedKnockbackReductionSystem` extending `DamageEventSystem`
- Read knockback resistance from `HyforgedStatComponent`
- Reduce knockback vector magnitude based on resistance

### FR-4: MaxHealth Bridge
- Update `HyforgedStatBridge` to sync `MaxHealth` to Hytale's `EntityStatMap`
- Sync on every stat recalculation
- Ensure `DamageSystems.ApplyDamage` reads correct max health

### FR-5: Current Health Initialization
- On entity spawn, initialize `EntityStatMap` current health to match `HyforgedStatComponent` max health
- Handle respawn scenarios

## Acceptance Criteria
- [x] Hytale's `ArmorDamageReduction` system is not running after plugin startup
- [x] Hytale's `ArmorKnockbackReduction` system is not running after plugin startup
- [x] Hytale's `EntityStatsSystems.Recalculate` system is not running after plugin startup
- [x] Damage is reduced based on `HyforgedStatComponent` resistance stats
- [x] Knockback is reduced based on `HyforgedStatComponent` knockback resistance
- [x] `EntityStatMap.MaxHealth` matches `HyforgedStatComponent.MaxHealth`
- [x] Entities can take damage and die correctly
- [x] No "double dipping" from both Hytale and Hyforged reducing damage

## Technical Notes
- `StatModifiersManager` is `private final` in `LivingEntity` and cannot be replaced; however, with `Recalculate` unregistered, it becomes effectively inert
- `DamageSystems.ApplyDamage` reads health from `EntityStatMap`, not `HyforgedStatComponent`
- System dependencies must be configured correctly for execution order

## Dependencies
- Entity Stats feature (complete)
- Stats System feature (complete)
- Hytale ECS: `ComponentRegistry`, `DamageEventSystem`, `SystemGroup`

## Links
- ADR: [ADR-0006](../../ADRs.md#adr-0006-replace-hytale-statdamage-systems-for-exclusive-hyforged-control)
- Requirements: [entity-stats.md](../../Requirements/rpg-arpg/entity-stats.md)
