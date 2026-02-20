# Due Diligence: Stat Integration Audit

**Date:** 2026-02-18  
**Scope:** All 185 Hyforged stat definitions under `Server/Hyforged/Stats/Definitions/`  
**Goal:** Determine integration level per stat — does defining it and putting it on affixes/passives _actually affect gameplay_?

---

## Integration Levels

| Level | Label | Meaning |
|-------|-------|---------|
| **L1** | ACTIVE | Stat value is read by a Java system that produces a measurable gameplay effect. Fully functional. |
| **L2** | COMPUTED | Stat definition exists, modifiers accumulate correctly in `HyforgedStatComponent`, but the computed value is not consumed by any game logic. Harmless, but has no gameplay impact. |
| **L3** | ORPHANED | Stat definition exists but is not referenced in any affix pool, passive tree, NPC template, or Java system. Dead weight. |
| **MISMATCH** | BROKEN | Code references a stat ID that does not match any definition. Will silently resolve to index `-1`. |

---

## Research Sources

| Source | Method |
|--------|--------|
| Stat definitions | `src/main/resources/Server/Hyforged/Stats/Definitions/*.json` (185 files) |
| Java ID lookups | `StatId.hyforged("...")` calls across all `.java` files (34 unique IDs found) |
| Data file references | `"StatId": "hyforged:..."` in all non-definition JSON (130+ unique IDs) |
| Combat pipeline | `HyforgedHitResolutionSystem`, `HyforgedCriticalHitSystem`, `HyforgedAutoBlockSystem`, `HyforgedDamageReductionSystem`, `HyforgedAilmentSystem` |
| Resource bridge | `HyforgedBridgeSystem` (max-health/mana/stamina/concentration/rage) |
| Effect bridge | `HyforgedEffectBridgeSystem` (translates Hytale EntityEffects → Hyforged modifiers) |
| On-kill recovery | `OnKillResourceRecoverySystem` |
| Damage types | `Server/Hyforged/Stats/Damage/*.json` (8 files mapping DamageCause → resistance/penetration stats) |
| NPC templates | `NPCStatTemplateAsset.java` (references some stat IDs directly) |
| Combat log | `HyforgedCombatLogSystem` |

---

## Summary

| Category | Total | L1 Active | L2 Computed | L3 Orphaned | MISMATCH |
|----------|-------|-----------|-------------|-------------|----------|
| ability-score | 8 | 7 | 1 | 0 | 0 |
| ailment | 15 | 5 | 10 | 0 | 0 |
| aura | 7 | 1 | 6 | 0 | 0 |
| critical | 3 | 3 | 0 | 0 | 0 |
| defense | 25 | 10 | 15 | 0 | 0 |
| loot | 3 | 0 | 3 | 0 | 0 |
| minion | 9 | 0 | 9 | 0 | 0 |
| offense | 42 | 7 | 34 | 1 | 0 |
| projectile | 6 | 0 | 6 | 0 | 0 |
| recovery | 17 | 4 | 13 | 0 | 0 |
| resistance | 12 | 12 | 0 | 0 | 0 |
| resource | 9 | 9 | 0 | 0 | 0 |
| skill | 13 | 0 | 13 | 0 | 0 |
| utility | 14 | 2 | 12 | 0 | 0 |
| **TOTAL** | **183** | **60** | **122** | **1** | **1** |

> **Core finding: ~122 stats (66%) accumulate modifiers correctly but have zero gameplay impact because no system reads their values.**

---

## Critical Mismatch

### `physical-power` (MISMATCH in `NPCStatTemplateAsset.java` line 233)
- Code: `StatId.hyforged("physical-power")`  
- Definition: **Does not exist**. Definitions are `hyforged:attack-power` and `hyforged:spell-power`.
- Impact: NPC stat templates that set physical-power silently fail. NPCs will be under-statted.
- **Action Required**: Either rename `attack-power` → `physical-power` or fix the reference in `NPCStatTemplateAsset.java`.

---

## Per-Category Detail

### ABILITY-SCORE

| Stat ID | Level | Consuming System |
|---------|-------|-----------------|
| `hyforged:strength` | L1 | `ClassLevelModifierSystem`; scaling source for `attack-power` |
| `hyforged:dexterity` | L1 | Scaling source for `accuracy-rating` |
| `hyforged:intelligence` | L1 | `ClassLevelModifierSystem`; expected scaling source for `spell-power` |
| `hyforged:luck` | L1 | Scaling source for `crit-chance-bps` (threshold scaling) |
| `hyforged:spirit` | L1 | `ClassLevelModifierSystem` |
| `hyforged:wisdom` | L1 | `ClassLevelModifierSystem` |
| `hyforged:constitution` | L1 | Scaling source for `armor-rating` |
| `hyforged:attribute-all` | L2 | Defined; used as modifier target in affixes ("+N to all attributes"). No system redistributes its value as bonuses to individual attributes. Modifiers from `attribute-all` source only land on this single stat's bucket and are not further propagated. |

> **Gap**: `attribute-all` requires a system (or stat-engine enhancement) that fans out its value as modifiers to strength, dexterity, intelligence, luck, spirit, wisdom simultaneously.

---

### AILMENT

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:ailment-threshold-bps` | L1 | Read by `HyforgedAilmentSystem` to scale damage threshold needed to trigger ailments |
| `hyforged:bleed-chance-bps` | L2 | Defined, in data — no system rolls bleed chance on hit |
| `hyforged:bleed-damage-bps` | L2 | Defined, in data — no system applies this to bleed DoT ticks |
| `hyforged:bleed-duration-bps` | L2 | Defined — no system adjusts bleed effect duration using this |
| `hyforged:chill-effect-bps` | L2 | Defined — no system reads chill magnitude from this stat |
| `hyforged:freeze-chance-bps` | L2 | Defined — no roll-based freeze system; freeze is only threshold via ailment system |
| `hyforged:freeze-duration-bps` | L2 | Defined — no system adjusts freeze duration from this stat |
| `hyforged:ignite-chance-bps` | L2 | Defined — ignite is threshold-based, not chance-based currently |
| `hyforged:ignite-damage-bps` | L2 | Defined — no system reads to scale ignite tick damage |
| `hyforged:ignite-duration-bps` | L2 | Defined — no system adjusts ignite duration from this stat |
| `hyforged:ailment-damage-bps` | L2 | Defined — no system applies this as a global ailment damage multiplier |
| `hyforged:poison-chance-bps` | L2 | Defined — poison is threshold-based only |
| `hyforged:poison-duration-bps` | L2 | Defined — no duration scaling system |
| `hyforged:shock-chance-bps` | L2 | Defined — no roll-based shock system |
| `hyforged:shock-duration-bps` | L2 | Defined — no duration scaling system |
| `hyforged:shock-effect-bps` | L2 | Defined — no system reads shock effect magnitude |

> **Gaps**: The ailment system uses only `ailment-threshold-bps` and `effect-duration-bps`. Per-ailment scaling stats (chance, damage multiplier, duration) are all unimplemented. Requires dedicated ailment modifier application in `HyforgedAilmentSystem` and DoT tick systems.

---

### AURA

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:reservation-efficiency-bps` | L1 | Read by `HyforgedEffectBridgeSystem` to reduce concentration cost of maintained effects |
| `hyforged:aura-area-bps` | L2 | Defined — no system reads to scale aura radius |
| `hyforged:aura-effect-bps` | L2 | Defined — no system reads to scale aura potency |
| `hyforged:aura-skill-levels` | L2 | Defined — no skill level system for aura skills |
| `hyforged:curse-duration-bps` | L2 | Defined — no system reads to scale curse duration |
| `hyforged:curse-effect-bps` | L2 | Defined — no system reads to scale curse effectiveness |
| `hyforged:max-curses` | L2 | Defined — no system enforces a curse cap |

> **Gaps**: Aura/curse system is entirely unimplemented beyond concentration reservation plumbing.

---

### CRITICAL

| Stat ID | Level | Consuming System |
|---------|-------|-----------------|
| `hyforged:crit-chance-bps` | L1 | `HyforgedCriticalHitSystem` — rolls per attack, clampable via `max-crit-chance-bps` |
| `hyforged:crit-multiplier-bps` | L1 | `HyforgedCriticalHitSystem` — multiplies damage on crit |
| `hyforged:max-crit-chance-bps` | L1 | Used as `SoftCapBonusStat` by `crit-chance-bps` definition; read during stat compute to set per-entity cap |

> All three critical stats are fully integrated.

---

### DEFENSE

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:ailment-threshold-bps` | L1 | Scales ailment accumulation threshold (see AILMENT) |
| `hyforged:armor-rating` | L1 | Used in `HyforgedDamageReductionSystem` (via Physical resistance chain); also scaling source in `PhysicalResistance.json` |
| `hyforged:evasion-chance-bps` | L1 | `HyforgedHitResolutionSystem` — defender's evasion |
| `hyforged:evasion-rating` | L1 | Scaling source for `evasion-chance-bps` (via evasion rating curve) |
| `hyforged:block-chance-bps` | L1 | `HyforgedAutoBlockSystem` — defender's auto-block roll |
| `hyforged:block-mitigation-bps` | L1 | `HyforgedAutoBlockSystem` — damage reduction per block |
| `hyforged:auto-block-stamina-cost-bps` | L1 | `HyforgedAutoBlockSystem` — stamina cost modifier per block |
| `hyforged:knockback-resistance-bps` | L1 | Read in knockback system (Java reference found); likely reduces knockback distance |
| `hyforged:stun-threshold-bps` | L1 | Read in stun system — threshold before stun is applied |
| `hyforged:stun-avoidance-bps` | L1 | Read in stun system — chance to avoid stun |
| `hyforged:armor-increased-bps` | L2 | Defined — no system applies this as a multiplier to effective armor; would need to feed into `HyforgedDamageReductionSystem` |
| `hyforged:chaos-damage-taken-bps` | L2 | Defined — not read in `HyforgedDamageReductionSystem` per-entity; damage-taken multipliers require a separate pipeline pass |
| `hyforged:critical-damage-taken-bps` | L2 | Defined — no system applies this modifier to crit damage received |
| `hyforged:damage-over-time-taken-bps` | L2 | Defined — no DoT damage-received modifier system |
| `hyforged:damage-taken-bps` | L2 | Defined — no global damage-taken multiplier system |
| `hyforged:dodge-chance-bps` | L2 | Defined — separate from `evasion-chance-bps`; no dodge roll system |
| `hyforged:elemental-damage-taken-bps` | L2 | Defined — no system applies elemental-specific damage-taken multipliers |
| `hyforged:evasion-increased-bps` | L2 | Defined — should multiply final `evasion-chance-bps` but no system applies the multiplication |
| `hyforged:max-block-chance-bps` | L2 | Defined as soft-cap stat for block but no system applies the cap via stat engine |
| `hyforged:max-evasion-chance-bps` | L2 | Same — defined as cap stat but capping not active |
| `hyforged:physical-damage-taken-bps` | L2 | Defined — no per-type damage-taken pipeline |
| `hyforged:reflect-damage-taken-bps` | L2 | Defined — no damage reflection system |
| `hyforged:block-spell-chance-bps` | L2 | Defined — no spell-block roll system |
| `hyforged:suppression-chance-bps` | L2 | Defined — no spell suppression system |
| `hyforged:suppression-effect-bps` | L2 | Defined — no spell suppression effect system |

> **Gaps**: 15 of 25 defense stats are unimplemented. The biggest missing pieces are:
> - Global damage-taken multipliers (`damage-taken-bps`, `chaos-/elemental-/physical-/critical-/dot-damage-taken-bps`)
> - Per-cap enforcement for evasion and block
> - Dodge as a separate mechanic from evasion
> - Spell block and suppression systems

---

### LOOT

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:experience-gain-bps` | L2 | Defined, in data — no system reads this to multiply XP awards |
| `hyforged:item-quantity-increased-bps` | L2 | Defined, in data — no system reads this during loot drops |
| `hyforged:item-rarity-increased-bps` | L2 | Defined, in data — no system reads this during quality/rarity rolls |

> **Gap**: Entire loot modifier system is undefined. All three loot stats need integration with the XP and loot generation systems.

---

### MINION

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:max-minions` | L2 | Defined, in data — no system enforces minion cap |
| `hyforged:minion-damage-bps` | L2 | Defined, in data — no system applies to minion damage output |
| `hyforged:minion-duration-bps` | L2 | Defined — no minion lifespan scaling |
| `hyforged:minion-life-bps` | L2 | Defined — no minion HP scaling |
| `hyforged:minion-speed-bps` | L2 | Defined — no minion movement scaling |
| `hyforged:minion-accuracy-bps` | L2 | Defined — no minion accuracy system |
| `hyforged:minion-attack-speed-bps` | L2 | Defined — no minion attack speed system |
| `hyforged:minion-crit-chance-bps` | L2 | Defined — no minion crit system |
| `hyforged:minion-skill-levels` | L2 | Defined — no minion skill level system |

> **Gap**: Entire minion stat subsystem is unimplemented. Minions can be spawned but no stats flow from the summoner.

---

### OFFENSE

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:accuracy-rating` | L1 | `HyforgedHitResolutionSystem` — attacker's hit chance |
| `hyforged:attack-power` | L1 | Computed (scaling from strength); used via `physical-power` in NPC templates → **see MISMATCH** |
| `hyforged:spell-power` | L1 | Computed (scaling from intelligence); expected base for outgoing magic damage |
| `hyforged:fire-penetration-bps` | L1 | `HyforgedDamageReductionSystem` via `DamageTypeExtensionRegistry` |
| `hyforged:cold-penetration-bps` | L1 | Same |
| `hyforged:lightning-penetration-bps` | L1 | Same |
| `hyforged:chaos-penetration-bps` | L1 | Same |
| `hyforged:armor-penetration-bps` | L1 | Same (Physical damage type) |
| `hyforged:bleed-penetration-bps` | L1 | Same (Bleed damage type) |
| `hyforged:elemental-penetration-bps` | L1 | `DamageTypeExtensionRegistry` — Elemental type is parent; fire/cold/lightning inherit |
| `hyforged:attack-speed-bps` | L2 | Defined — no system bridges this to Hytale's attack animation interval |
| `hyforged:cast-speed-bps` | L2 | Defined — no system adjusts spell casting delays |
| `hyforged:added-physical-damage-flat` | L2 | Defined — no system adds this to outgoing damage rolls |
| `hyforged:added-fire-damage-flat` | L2 | Same |
| `hyforged:added-cold-damage-flat` | L2 | Same |
| `hyforged:added-lightning-damage-flat` | L2 | Same |
| `hyforged:added-chaos-damage-flat` | L2 | Same |
| `hyforged:damage-increased-bps` | L2 | Defined — no outgoing damage multiplier pipeline reads this |
| `hyforged:attack-damage-increased-bps` | L2 | Same |
| `hyforged:melee-damage-increased-bps` | L2 | Same |
| `hyforged:ranged-damage-increased-bps` | L2 | Same |
| `hyforged:spell-damage-increased-bps` | L2 | Same |
| `hyforged:elemental-damage-increased-bps` | L2 | Same |
| `hyforged:fire-damage-increased-bps` | L2 | Same |
| `hyforged:cold-damage-increased-bps` | L2 | Same |
| `hyforged:lightning-damage-increased-bps` | L2 | Same |
| `hyforged:chaos-damage-increased-bps` | L2 | Same |
| `hyforged:physical-damage-increased-bps` | L2 | Same |
| `hyforged:poison-damage-increased-bps` | L2 | Same |
| `hyforged:one-handed-damage-bps` | L2 | Defined — no weapon-type-conditional damage system |
| `hyforged:two-handed-damage-bps` | L2 | Same |
| `hyforged:dual-wield-damage-bps` | L2 | Defined — no dual-wield detection system |
| `hyforged:shield-damage-bps` | L2 | Defined — no shield-equipped-conditional system |
| `hyforged:unarmed-damage-bps` | L2 | Defined — no unarmed detection system |
| `hyforged:damage-over-time-bps` | L2 | Defined — no DoT outgoing multiplier system |
| `hyforged:chance-to-deal-double-damage-bps` | L2 | Defined — no double-damage roll system |
| `hyforged:culling-strike-threshold-bps` | L2 | Defined — no culling-strike execution system |
| `hyforged:stun-duration-bps` | L2 | Defined — stunned duration modifier not read by stun system |
| `hyforged:knockback-chance-bps` | L2 | Defined — no knockback-on-hit chance system |
| `hyforged:knockback-distance-bps` | L2 | Defined — no knockback distance scaling |
| `hyforged:intimidate-effect-bps` | L2 | Defined — no intimidate debuff system |
| `hyforged:max-impales` | L2 | Defined — no impale mechanic implemented |
| `hyforged:mine-damage-bps` | L2 | Defined — no mine system |

> **Gap**: This is the largest gap in the codebase. 34 of 42 offense stats accumulate modifiers but the **outgoing damage multiplier pipeline** (`damage-increased-bps`, per-type increased, penetration for elemental via inheritance) is not applied to outgoing attacks. Attack speed, cast speed, added damage flat, and all stance/weapon-conditional bonuses have no consuming system.

---

### PROJECTILE

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:additional-projectiles` | L2 | Defined, in data — no system reads to spawn extra projectiles |
| `hyforged:projectile-chain-count` | L2 | Defined — no projectile chaining system |
| `hyforged:projectile-fork-count` | L2 | Defined — no fork system |
| `hyforged:projectile-pierce-count` | L2 | Defined — no pierce system |
| `hyforged:projectile-damage-bps` | L2 | Defined — no outgoing projectile damage multiplier |
| `hyforged:projectile-speed-bps` | L2 | Defined — no projectile velocity scaling |

> **Gap**: Entire projectile modifier system is unimplemented.

---

### RECOVERY

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:life-on-kill-flat` | L1 | `OnKillResourceRecoverySystem` |
| `hyforged:mana-on-kill-flat` | L1 | `OnKillResourceRecoverySystem` |
| `hyforged:mana-on-kill-bps` | L1 | `OnKillResourceRecoverySystem` (% of max mana) |
| `hyforged:life-recovery-rate-bps` | L1 | Java reference in recovery system |
| `hyforged:healing-effectiveness-bps` | L2 | Java reference found but no confirmed system applies this to actual healing amounts |
| `hyforged:healing-received-bps` | L2 | Same |
| `hyforged:health-regen-flat` | L2 | Defined — no health regen tick system |
| `hyforged:health-regen-percent-bps` | L2 | Defined — no regen tick system |
| `hyforged:mana-regen-flat` | L2 | Defined — no mana regen tick system |
| `hyforged:mana-regen-percent-bps` | L2 | Defined — no mana regen tick system |
| `hyforged:stamina-regen-flat` | L2 | Defined — Hytale handles stamina regen natively; Hyforged value not fed in |
| `hyforged:life-on-hit-flat` | L2 | Defined, in data — no on-hit heal system |
| `hyforged:mana-on-hit-flat` | L2 | Defined, in data — no on-hit mana recovery |
| `hyforged:life-leech-bps` | L2 | Defined, in data — no leech pipeline |
| `hyforged:mana-leech-bps` | L2 | Defined, in data — no leech pipeline |
| `hyforged:leech-rate-bps` | L2 | Defined — no leech instantiation rate system |
| `hyforged:max-life-leech-rate-bps` | L2 | Defined — no leech cap system |

> **Gaps**: On-hit resource recovery (life/mana-on-hit), regen ticks (health/mana regen), leech, and healing multipliers all unimplemented. The `OnKillResourceRecoverySystem` is the only functioning recovery beyond the bridge.

---

### RESISTANCE

| Stat ID | Level | Consuming System |
|---------|-------|-----------------|
| `hyforged:fire-resistance-bps` | L1 | `HyforgedDamageReductionSystem` via `DamageTypeExtensionRegistry` (Fire.json) |
| `hyforged:cold-resistance-bps` | L1 | Same (Ice.json) |
| `hyforged:lightning-resistance-bps` | L1 | Same (Lightning.json) |
| `hyforged:chaos-resistance-bps` | L1 | Same (Chaos.json) |
| `hyforged:physical-resistance-bps` | L1 | Same (Physical.json) |
| `hyforged:bleed-resistance-bps` | L1 | Same (Bleed.json) |
| `hyforged:poison-resistance-bps` | L1 | Same (Poison.json) |
| `hyforged:all-resistance-bps` | L1 | Via tags: modifiers targeting `all-resistance-bps` are distributed by the tag system to all resistance stats |
| `hyforged:maximum-resistance-fire-bps` | L1 | `SoftCapBonusStat` on `fire-resistance-bps` — entity-level fire resistance cap |
| `hyforged:maximum-resistance-cold-bps` | L1 | Same for cold |
| `hyforged:maximum-resistance-lightning-bps` | L1 | Same for lightning |
| `hyforged:maximum-resistance-chaos-bps` | L1 | Same for chaos |

> Entire resistance category is fully integrated. The only noted gap is `Poison.json` has no `HyforgedPenetrationStat` (intentional — poison penetration is not defined as a stat yet).

---

### RESOURCE

| Stat ID | Level | Consuming System |
|---------|-------|-----------------|
| `hyforged:max-health-flat` | L1 | `HyforgedBridgeSystem` → `EntityStatMap` Health max |
| `hyforged:max-mana-flat` | L1 | `HyforgedBridgeSystem` → `EntityStatMap` Mana max |
| `hyforged:max-stamina-flat` | L1 | `HyforgedBridgeSystem` → `EntityStatMap` Stamina max |
| `hyforged:concentration` | L1 | `HyforgedBridgeSystem` → `EntityStatMap` Concentration max; also managed by `ConcentrationService` |
| `hyforged:concentration-loss-reduction-bps` | L1 | Java reference — read to reduce concentration drain when taking hits |
| `hyforged:concentration-loss-threshold-bps` | L1 | Java reference — threshold before concentration starts draining |
| `hyforged:concentration-regen-rate-bps` | L1 | Java reference — read to scale concentration regeneration rate |
| `hyforged:rage-max` | L1 | `HyforgedBridgeSystem` → `EntityStatMap` Rage max |
| `hyforged:rage` | L1 | Stat for current rage value; bridged to Hytale EntityStatMap |
| `hyforged:auto-block-stamina-cost-bps` | L1 | `HyforgedAutoBlockSystem` |

> All resource stats are fully integrated.

---

### SKILL LEVELS

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:all-skill-levels` | L2 | Defined — no system reads to boost all spell/skill levels |
| `hyforged:axe-skill-levels` | L2 | All weapon-specific skill level stats are defined and in passive data, but no skill system reads them |
| `hyforged:bow-skill-levels` | L2 | Same |
| `hyforged:cold-skill-levels` | L2 | Same |
| `hyforged:crossbow-skill-levels` | L2 | Same |
| `hyforged:dagger-skill-levels` | L2 | Same |
| `hyforged:fire-skill-levels` | L2 | Same |
| `hyforged:lightning-skill-levels` | L2 | Same |
| `hyforged:mace-skill-levels` | L2 | Same |
| `hyforged:melee-skill-levels` | L2 | Same |
| `hyforged:spell-skill-levels` | L2 | Same |
| `hyforged:staff-skill-levels` | L2 | Same |
| `hyforged:sword-skill-levels` | L2 | Same |

> **Gap**: Skill level system is entirely unimplemented. All 13 skill level stats are L2.

---

### UTILITY

| Stat ID | Level | Notes |
|---------|-------|-------|
| `hyforged:effect-duration-bps` | L1 | `HyforgedAilmentSystem` — scales ailment effect duration applied to target |
| `hyforged:movement-speed-bps` | L1 | `HyforgedEffectBridgeSystem` — when effects apply movement speed modifiers, they land in `HyforgedStatComponent`; a mechanism bridges this to Hytale's movement stat. **Partial** — only works if the movement speed change comes via an EntityEffect. Passive/affix movement speed modifiers may not be bridged. |
| `hyforged:cooldown-recovery-rate-bps` | L2 | Defined — no skill/ability cooldown system reads this |
| `hyforged:mana-cost-bps` | L2 | Defined — no skill activation system reads this to modify cost |
| `hyforged:stamina-cost-bps` | L2 | Defined — similar |
| `hyforged:skill-cost-flat` | L2 | Defined — similar |
| `hyforged:life-cost-bps` | L2 | Defined — no life-cost skill activation system |
| `hyforged:area-of-effect-bps` | L2 | Defined — no system scales skill/effect radius from this stat |
| `hyforged:max-totems` | L2 | Defined — no totem placement cap system |
| `hyforged:max-traps` | L2 | Defined — no trap placement cap system |
| `hyforged:mine-arming-speed-bps` | L2 | Defined — no mine system |
| `hyforged:mine-throwing-speed-bps` | L2 | Defined — same |
| `hyforged:banner-effect-bps` | L2 | Defined — no banner system |
| `hyforged:brand-attachment-range-bps` | L2 | Defined — no brand system |

> **Note on movement-speed-bps**: It IS bridged for effect-sourced changes (buffs/potions). However, passive tree nodes granting +X% movement speed would accumulate in HyforgedStatComponent but NOT be applied to the entity's actual movement because `HyforgedBridgeSystem` only bridges the five resource caps. Movement speed stat accumulator needs to be bridged to Hytale's movement stat.

---

## Systems That Read Stats vs Systems That Write

### Systems Writing to HyforgedStatComponent (creating modifiers)
| System / Mechanism | Stats Modified |
|-------------------|---------------|
| `ClassLevelModifierSystem` | Ability scores (strength, dex, int, etc.) per level |
| `HyforgedEffectBridgeSystem` | Any stat referenced in an EntityEffect JSON `StatModifiers` array |
| `HyforgedStatInitSystem` | Class base stats from `ClassDefinition` JSON |
| `HyforgedMonsterScalingSystem` | NPC stats per scaling config |
| Affix system | Any stat referenced in `AffixDefinition.json` stat tiers |
| Passive tree system | Any stat referenced in passive node `Effects` |

### Systems Reading from HyforgedStatComponent (consuming values)
| System | Stats Consumed |
|--------|---------------|
| `HyforgedHitResolutionSystem` | `accuracy-rating`, `evasion-chance-bps` |
| `HyforgedCriticalHitSystem` | `crit-chance-bps`, `crit-multiplier-bps` |
| `HyforgedAutoBlockSystem` | `block-chance-bps`, `block-mitigation-bps`, `auto-block-stamina-cost-bps` |
| `HyforgedDamageReductionSystem` | All resistance stats (via DamageTypeExtensionRegistry), all penetration stats |
| `HyforgedAilmentSystem` | `ailment-threshold-bps`, `effect-duration-bps` |
| `HyforgedBridgeSystem` | `max-health-flat`, `max-mana-flat`, `max-stamina-flat`, `concentration`, `rage-max` |
| `HyforgedEffectBridgeSystem` | `reservation-efficiency-bps` |
| `OnKillResourceRecoverySystem` | `life-on-kill-flat`, `mana-on-kill-flat`, `mana-on-kill-bps` |
| `HyforgedMonsterScalingSystem` | NPC template stat reads |
| `StatAdminService` (debug) | `max-health`, `max-mana`, `max-stamina`, `armor`, `physical-damage` |

---

## Priority Gap Analysis

### P0 — Critical Bugs / Broken Today

1. **`physical-power` MISMATCH** — NPC templates reference a stat ID that does not exist. All NPCs will have broken damage scaling.
2. **Outgoing damage bonuses not applied** — Players with +100% fire damage from their passive tree deal the exact same damage as players with 0% fire damage. Every `*-damage-increased-bps` and `*-penetration-bps` stat (except resistance penetration in `HyforgedDamageReductionSystem`) is silently ignored.

### P1 — Core ARPG Mechanics Missing

3. **Attack speed** — `attack-speed-bps` accumulates but combat interval is unchanged.
4. **Regen ticks** — `health-regen-flat`, `health-regen-percent-bps`, `mana-regen-flat`, `mana-regen-percent-bps`, `stamina-regen-flat` have no tick system.
5. **Leech** — `life-leech-bps`, `mana-leech-bps`, `leech-rate-bps` have no pipeline.
6. **On-hit recovery** — `life-on-hit-flat`, `mana-on-hit-flat` have no on-hit handler.
7. **Damage-taken modifiers** — `damage-taken-bps`, `elemental-damage-taken-bps`, `chaos-damage-taken-bps`, `physical-damage-taken-bps`, `critical-damage-taken-bps` have no pipeline stage.
8. **Movement speed bridge** — passive/affix movement speed bonuses don't reach Hytale's movement system.

### P2 — Important Mechanics Missing

9. **Ailment per-stat scaling** — all chance/damage/duration per-ailment stats are L2.
10. **XP/loot multipliers** — `experience-gain-bps`, `item-quantity-increased-bps`, `item-rarity-increased-bps`.
11. **Attribute-all fan-out** — the `attribute-all` stat doesn't propagate to individual attributes.
12. **Culling strike, double damage, stun duration** — offensive special mechanics.

### P3 — Advanced / Future Systems

13. All minion stats (9 stats) — requires minion system.
14. All skill level stats (13 stats) — requires skill system.
15. Projectile modifiers (6 stats) — requires projectile system.
16. Aura/curse system (6 stats).
17. Utility modifiers: AoE, cooldown, cost (14 stats).
18. Special mechanics: totem, trap, mine, brand, banner (5 stats).

---

## Recommended Next Steps

1. **Fix `physical-power` mismatch** — rename to `attack-power` in `NPCStatTemplateAsset.java` or create a separate `physical-power` definition.
2. **Implement outgoing damage bonus pipeline** — a `HyforgedDamageBonusSystem` that applies `*-damage-increased-bps` as MORE/INCREASED multipliers to outgoing damage before it reaches the defender. This is the single highest-impact implementation.
3. **Implement regen tick system** — a `HyforgedRegenSystem` that reads regen stats and calls `EntityStatMap.addStatValue` each tick.
4. **Implement on-hit/leech recovery** — extend the damage pipeline with post-apply handler.
5. **Bridge movement speed** — add `movement-speed-bps` to `HyforgedBridgeSystem` targeting Hytale's movement stat.
6. **Implement damage-taken multipliers** — add a damage-taken pass to `HyforgedDamageReductionSystem` or a new sister system.

---

## File Inventory for Implementation Reference

| File | Purpose |
|------|---------|
| `HyforgedHitResolutionSystem.java` | Accuracy vs evasion |
| `HyforgedCriticalHitSystem.java` | Crit rolls + multiplier |
| `HyforgedAutoBlockSystem.java` | Block chance + mitigation |
| `HyforgedDamageReductionSystem.java` | Resistance + penetration |
| `HyforgedAilmentSystem.java` | Ailment threshold + triggering |
| `HyforgedBridgeSystem.java` | Resource caps → EntityStatMap |
| `HyforgedEffectBridgeSystem.java` | EntityEffect → HyforgedStatComponent |
| `OnKillResourceRecoverySystem.java` | Kill resource recovery |
| `HyforgedStatComputeSystem.java` | Dirty stat recomputation |
| `HyforgedStatInitSystem.java` | Entity stat initialization |
| `ClassLevelModifierSystem.java` | Level-up stat bonuses |
| `HyforgedMonsterScalingSystem.java` | NPC per-level scaling |
| `Stats/Damage/*.json` | DamageCause → resistance/penetration mapping |
| `Stats/Definitions/*.json` | 185 stat definitions |
| `Stats/NPCTemplates/*.json` | NPC base stat templates |
