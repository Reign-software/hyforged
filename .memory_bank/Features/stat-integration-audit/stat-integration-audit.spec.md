# Feature Spec: Full Stat Integration

## Metadata
- Feature ID (slug): `stat-integration-audit`
- Status: Draft
- Owner: Reign Software
- Date: 2026-02-18

## Summary

185 Hyforged stat definitions exist and correctly receive modifiers from the affix system, passive trees, and entity effects. However, only ~60 stats are actively consumed by game systems — the remaining ~122 stats accumulate modifiers but have **zero gameplay impact**. This spec describes the work required to fully integrate every stat into the combat and game loop.

Reference: [Due Diligence Report](stat-integration-audit.dd.md)

---

## Goals

- Every stat definition that is exposed to players (via affixes, passive trees, or tooltips) must produce a measurable gameplay effect.
- Outgoing and incoming damage pipelines must honour all relevant stat multipliers.
- Resource regeneration (health, mana, stamina) must tick using Hyforged stat values.
- On-hit and on-kill recovery must cover all defined recovery stats.
- Movement speed stat bonuses from affixes/passives must reach the actual entity movement speed.
- The `physical-power` stat ID mismatch in `NPCStatTemplateAsset.java` must be resolved.

---

## Non-Goals

- Implementing full minion AI autonomy (out of scope of this audit — minion stat bridging is in scope).
- Implementing aura, curse, brand, banner, totem, or trap gameplay mechanics (the stat framework will be laid; the mechanics themselves are separate features).
- Implementing mine gameplay.
- Implementing a full projectile system (projectile stat bridging is in scope for the future; not this pass).
- Skill level system design (separate feature).

---

## User Experience

- A player investing in fire damage nodes on the passive tree should see measurably higher fire hit numbers in combat.
- A player with high health regeneration should visibly recover health between fights.
- A player with high attack speed should attack noticeably faster than a player with none.
- Affixes that grant "+X to all elemental resistance" should reduce incoming elemental damage.
- NPCs should have properly scaled attack power matching their template definition.

---

## Functional Requirements

### FR-1: Fix physical-power MISMATCH
- `NPCStatTemplateAsset.java` must reference `hyforged:attack-power` instead of `hyforged:physical-power`, OR a new definition `physical-power.json` must be created if design intent is separate.

### FR-2: Outgoing Damage Bonus Pipeline
- A `HyforgedDamageBonusSystem` must apply, as a MORE multiplier on outgoing damage:
  - `hyforged:damage-increased-bps` (all damage)
  - `hyforged:attack-damage-increased-bps` (attack-type damage only)
  - `hyforged:spell-damage-increased-bps` (spell-type only)
  - `hyforged:melee-damage-increased-bps` (melee tag)
  - `hyforged:ranged-damage-increased-bps` (ranged tag)
  - `hyforged:elemental-damage-increased-bps` (elemental tag)
  - `hyforged:fire-damage-increased-bps` (fire damage cause)
  - `hyforged:cold-damage-increased-bps` (cold/ice damage cause)
  - `hyforged:lightning-damage-increased-bps` (lightning damage cause)
  - `hyforged:physical-damage-increased-bps` (physical damage cause)
  - `hyforged:chaos-damage-increased-bps` (chaos damage cause)
  - `hyforged:poison-damage-increased-bps` (poison damage cause)
  - `hyforged:damage-over-time-bps` (DoT-type damage)
  - `hyforged:projectile-damage-bps` (projectile-sourced damage)
  - `hyforged:mine-damage-bps` (mine-triggered damage)
- Weapon-stance bonuses applied when conditions are met:
  - `hyforged:one-handed-damage-bps` when attacker wields one-handed weapon
  - `hyforged:two-handed-damage-bps` when attacker wields two-handed weapon
  - `hyforged:dual-wield-damage-bps` when attacker dual-wields
  - `hyforged:shield-damage-bps` when attacker has shield equipped
  - `hyforged:unarmed-damage-bps` when attacker has no weapon
- Added flat damage applied before multipliers:
  - `hyforged:added-physical-damage-flat` → Physical damage type
  - `hyforged:added-fire-damage-flat` → Fire damage type
  - `hyforged:added-cold-damage-flat` → Ice damage type
  - `hyforged:added-lightning-damage-flat` → Lightning damage type
  - `hyforged:added-chaos-damage-flat` → Chaos damage type

### FR-3: Incoming Damage-Taken Multipliers
- A pipeline stage (new system or extension of `HyforgedDamageReductionSystem`) must apply:
  - `hyforged:damage-taken-bps` (all incoming damage)
  - `hyforged:elemental-damage-taken-bps` (elemental damage)
  - `hyforged:physical-damage-taken-bps` (physical damage)
  - `hyforged:chaos-damage-taken-bps` (chaos damage)
  - `hyforged:damage-over-time-taken-bps` (DoT damage)
  - `hyforged:critical-damage-taken-bps` (incoming crit-flagged damage)

### FR-4: Attack Speed Bridge
- `hyforged:attack-speed-bps` must be bridged to Hytale's attack animation interval or equivalent tick-rate, so higher attack speed = faster attacks.

### FR-5: Resource Regeneration Tick System
- A `HyforgedRegenSystem` must tick on a configurable interval (e.g., every 20 ticks) and apply:
  - `hyforged:health-regen-flat` → flat HP per tick
  - `hyforged:health-regen-percent-bps` → % of max HP per tick
  - `hyforged:mana-regen-flat` → flat mana per tick
  - `hyforged:mana-regen-percent-bps` → % of max mana per tick
  - `hyforged:stamina-regen-flat` → flat stamina per tick (or bridge to Hytale's stamina regen override)

### FR-6: On-Hit Recovery
- A post-damage-apply handler must award:
  - `hyforged:life-on-hit-flat` HP to the attacker per successful hit
  - `hyforged:mana-on-hit-flat` mana to the attacker per successful hit

### FR-7: Leech System
- A leech pipeline must convert a percentage of damage dealt (by `life-leech-bps`, `mana-leech-bps`) into recovery, capped by `max-life-leech-rate-bps` and modulated by `leech-rate-bps`.

### FR-8: Healing Multipliers
- Heal events must be multiplied by:
  - `hyforged:healing-effectiveness-bps` on the healer
  - `hyforged:healing-received-bps` on the receiver

### FR-9: Movement Speed Bridge
- `hyforged:movement-speed-bps` final computed value must be bridged to Hytale's `EntityStatMap` movement speed stat via `HyforgedBridgeSystem` (same pattern as health/mana/stamina).

### FR-10: Double Damage & Culling Strike
- `hyforged:chance-to-deal-double-damage-bps` — roll per attack; if successful, double damage output.
- `hyforged:culling-strike-threshold-bps` — if target HP% ≤ threshold, damage is set to OHKO.

### FR-11: Stun Duration
- `hyforged:stun-duration-bps` must modify the duration of stun effects applied by the attacker.

### FR-12: Knockback Chance & Distance
- `hyforged:knockback-chance-bps` — roll per hit; if triggered, apply knockback.
- `hyforged:knockback-distance-bps` — scale the knockback distance.

### FR-13: XP and Loot Multipliers
- `hyforged:experience-gain-bps` must multiply XP awards.
- `hyforged:item-quantity-increased-bps` and `hyforged:item-rarity-increased-bps` must influence loot generation.

### FR-14: Per-Ailment Scaling
- `HyforgedAilmentSystem` must read and apply per-ailment stats for active ailments:
  - Chance: `bleed-chance-bps`, `freeze-chance-bps`, `ignite-chance-bps`, `poison-chance-bps`, `shock-chance-bps`
  - Duration: `bleed-/freeze-/ignite-/poison-/shock-duration-bps`
  - Damage: `bleed-damage-bps`, `ignite-damage-bps`, `ailment-damage-bps` (global)
  - Magnitude: `chill-effect-bps`, `shock-effect-bps`

### FR-15: Attribute-All Fan-Out
- A mechanism must propagate the `attribute-all` value as flat modifiers to all individual attribute stats (strength, dexterity, intelligence, luck, spirit, wisdom, constitution).

### FR-16: Max Block / Max Evasion Caps
- `hyforged:max-block-chance-bps` must be wired as a soft-cap enforcing mechanism on `block-chance-bps`.
- `hyforged:max-evasion-chance-bps` must be wired as a soft-cap on `evasion-chance-bps`.

### FR-17: Armor Increased
- `hyforged:armor-increased-bps` must multiply effective armor in the damage reduction pipeline.

### FR-18: Evasion Increased
- `hyforged:evasion-increased-bps` must multiply final `evasion-chance-bps` in the hit resolution pipeline.

### FR-19: Spell Block & Suppression
- `hyforged:block-spell-chance-bps` — roll per incoming spell; if blocked, reduce damage.
- `hyforged:suppression-chance-bps` and `hyforged:suppression-effect-bps` — roll per incoming spell; if triggered, reduce damage by suppression effect %.

### FR-20: Dodge
- `hyforged:dodge-chance-bps` — a separate dodge mechanic (distinct from evasion) that results in a full miss with a different animation/feedback than evasion.

### FR-21: Minion Stat Bridging
- When a minion entity is spawned by a player, the following stats from the summoner must be applied as modifiers to the minion:
  - `minion-damage-bps`, `minion-life-bps`, `minion-speed-bps`, `minion-duration-bps`
  - `minion-accuracy-bps`, `minion-attack-speed-bps`, `minion-crit-chance-bps`
- `max-minions` must cap how many minion entities a player can have active simultaneously.

### FR-22: Skill Level Stats
- A skill level system must read `*-skill-levels` stats to boost effective level of typed spells/abilities. (Design TBD.)

### FR-23: Intimidate Effect
- `hyforged:intimidate-effect-bps` — applies a debuff to targets causing them to deal reduced damage (design TBD).

---

## Non-Functional Requirements

- **Performance**: All new systems must use cached stat indices. Regen tick must run at reduced frequency (not every tick).
- **Latency**: No stat read should block the game thread beyond a single tick.
- **Data-driven**: All thresholds, frequencies, and multiplier stacking rules must come from JSON, not hard-coded constants.
- **No compile warnings**: All new code must compile cleanly.
- **Localization**: Any new user-facing text added as part of stat display changes must use translation keys.

---

## Dependencies

- `HyforgedStatComponent` (read)
- `StatDefinitionRegistry` (index lookup)
- `DamageTypeExtensionRegistry` (damage type → stat mapping)
- `EntityStatMap` (Hytale native — for bridging and recovery)
- `HyforgedBridgeSystem` (extend for movement speed)
- `HyforgedDamageReductionSystem` (extend for damage-taken pass)
- `HyforgedAilmentSystem` (extend for per-ailment stat scaling)

---

## Data / Schema Impact

- Add `movement-speed` entity stat mapping to `HyforgedBridgeSystem`.
- All new systems are additive; no existing JSON schema changes required.

---

## Security / Privacy

None.

---

## Observability

- `StatAdminService` (`/stats` command) should be extended to show integration level per stat (Phase 6 of plan).
- Regen and recovery events should emit to the combat log.

---

## Risks

| Risk | Probability | Mitigation |
|------|-------------|------------|
| Attack speed conflicts with Hytale's native cooldown system | Medium | Research Hytale's `DefaultEntityStatTypes` attack interval before implementing |
| Damage bonus stack with existing resistance pipeline causes unexpected double-dipping | Low | Clear before/after ordering in SystemGroup dependencies |
| Movement speed bridge overrides players' base movement speed | Medium | Read delta from base Hytale value rather than setting absolute value |
| Regen ticks are too frequent and cause performance issues | Low | Use `DelayedEntitySystem` with configurable interval |

---

## Open Questions

1. Is `physical-power` intentionally different from `attack-power`, or a naming mistake? (Likely a mistake — see DD critical mismatch.)
2. Should `dodge-chance-bps` be a complete miss (same as evasion), or show a "Dodge" visual indicator? Is it a second roll after evasion fails, or an alternative roll?
3. Should double-damage be applied post-crit or independent of crit?
4. What is the intended mechanic for `intimidate-effect-bps` — debuff to attacked entity's damage output, or damage reduction like fear?
5. Should `attribute-all` fan-out be handled by the stat engine (automatic) or a dedicated system?

---

## Acceptance Criteria

- [ ] `physical-power` mismatch resolved.
- [ ] All `*-damage-increased-bps` stats produce measurable damage change in combat.
- [ ] Added flat damage stats add to the correct damage type.
- [ ] `health-regen-flat` produces visible health regeneration over time.
- [ ] `mana-regen-flat` produces visible mana regeneration over time.
- [ ] `life-on-hit-flat` heals the attacker on each successful hit.
- [ ] `life-on-kill-flat` and `mana-on-kill-flat` continue working (already passing).
- [ ] `movement-speed-bps` from a passive node measurably increases entity movement speed.
- [ ] `experience-gain-bps` multiplies XP awards.
- [ ] `attack-speed-bps` measurably changes attack rate.
- [ ] `block-spell-chance-bps` produces a spell-specific block probability.
- [ ] Per-ailment chance and duration stats affect ailment behaviour.
- [ ] All stated minion stats correctly transfer from summoner to minion on spawn.
- [ ] Zero new compile warnings or errors.

---

## Impacted Areas

- Combat damage pipeline (Gather → Filter → Inspect)
- Entity stat computation (`HyforgedStatComputeSystem`, `HyforgedBridgeSystem`)
- Resource management (regen, leech, on-hit recovery)
- NPC stat templates
- Movement system bridge
- All affix/passive stats that were L2

---

## Required Codebase Changes (High-Level)

1. Fix `NPCStatTemplateAsset.java` stat ID reference.
2. New `HyforgedDamageBonusSystem` — applies outgoing damage multipliers.
3. New damage-taken pass in `HyforgedDamageReductionSystem` or new `HyforgedDamageTakenSystem`.
4. New `HyforgedRegenSystem` (interval-based, reads regen stats).
5. Extend damage pipeline post-apply for on-hit healing and leech.
6. Extend `HyforgedBridgeSystem` with movement speed bridging.
7. Extend `HyforgedAilmentSystem` with per-ailment stat scaling.
8. New `HyforgedAttackSpeedSystem` or bridge to Hytale attack interval stat.
9. Extend `HyforgedAutoBlockSystem` or add `HyforgedSpellBlockSystem`.
10. Add special combat rolls (double damage, culling strike, knockback chance, dodge).
11. Minion stat bridge system.
12. XP / loot multiplier integration.
13. `attribute-all` fan-out mechanism.

---

## References

- [Due Diligence Report](stat-integration-audit.dd.md)
- `.memory_bank/Features/combat-system/combat-system.spec.md`
- `.memory_bank/Features/hyforged-stats-system/`
- `.memory_bank/ADRs.md` (ADR-0006: Damage Type Extensions)
