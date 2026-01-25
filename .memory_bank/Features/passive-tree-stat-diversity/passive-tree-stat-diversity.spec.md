# Feature Spec: Passive Tree Stat Diversity & Region Plan

## Metadata
- Feature ID (slug): passive-tree-stat-diversity
- Status: Draft
- Owner: JBurl
- Date: 2026-01-25

## Summary
Define and execute a region-by-region stat coverage plan for the General Passive Tree that delivers PoE-style density, reduces repetitive nodes, and ensures 100% coverage of the existing stat catalog. The plan assigns every stat to a primary region plus sparse off-region placement, increases notable/cluster variety, and limits travel-node bloat to keep point spend focused on meaningful choices.

## Goals
- Ensure every stat definition in the Stats catalog appears at least once in the General Tree.
- Increase region identity with deeper, thematic clusters and more unique notables.
- Reduce repetition of minor nodes and avoid near-identical notables within a region.
- Keep travel nodes sparse; clusters should be dense and reachable with minimal point tax.
- Make concentration and item-find stats rare and intentionally placed.

## Non-Goals
- Rebalancing numerical values or tuning end-game balance.
- Changes to class trees or weapon-class identity.
- UI feature changes or new UI tooling.

## User Experience
- Players perceive stronger region identity with clear build archetypes per region.
- Traveling between clusters feels efficient, with minimal “dead” travel spend.
- Off-region stats exist but are noticeably sparser, encouraging intentional pathing.
- Rare stats (concentration, item-find) feel special and require deliberate investment.

## Functional Requirements

### FR-1: Stat Coverage Matrix
- Every stat in the Stats catalog must be mapped to:
  - A primary region.
  - At least one secondary/off-region placement (sparse).
  - A cluster archetype (minor/notable/keystone, or mixed).
- A coverage matrix must be produced and reviewed to verify 100% coverage.

### FR-2: Region Theme Coverage
Each region must define primary and secondary stat themes. Off-region stats appear in every region, but are sparse and clustered (PoE-style).

**Region Themes (High-Level)**

| Region | Primary Themes (Examples) | Secondary / Off-Stat (Examples) | Rare / Sparse |
|---|---|---|---|
| Strength | Physical damage, attack power, melee speed, bleed, rage, knockback, weapon skill levels aligned to strength weapons | Armor, block, health sustain; small accuracy/crit | Concentration (very rare), item-find (very rare) |
| Dexterity | Accuracy, evasion/dodge, attack speed, crit, projectiles, stamina, dexterity-aligned weapon skill levels | Bleed/poison overlap, small spell/elemental access | Concentration (very rare), item-find (very rare) |
| Intelligence | Spell damage, cast speed, elemental damage/pen, mana, elemental ailments | Crit, cooldown/duration overlap | Item-find (very rare) |
| Constitution | Max health, health regen, armor, block, damage mitigation, resistances, ailment threshold | Life leech/attack sustain, small movement/accuracy | Concentration (rare), item-find (very rare) |
| Wisdom | Resistances & max resist, cooldown recovery, duration, auras, healing effectiveness, concentration stability | Mana efficiency, small crit/area | Item-find (very rare) |
| Spirit | Mana recovery/on-hit/on-kill, reservation, minions, curses, totems/brands, traps/mines | Spell utility, duration overlap | Concentration (rare), item-find (very rare) |
| Luck | Crit volatility, culling, chaos/poison, experience gain, fortune effects | Movement/attack speed overlap, small defenses | Item-find (rare but limited) |

Notes:
- “Examples” are thematic; the coverage matrix will map every stat to a region and cluster.
- Weapon/element skill-level stats are assigned to their most thematic regions (e.g., bow/crossbow → Dexterity; elemental skill levels → Intelligence/Wisdom; universal skill levels → Hub).
- Ailment families are concentrated by theme (bleed → Strength, poison/chaos → Luck, elemental ailments → Intelligence/Wisdom), with sparse off-region access.

### FR-2.1: Region Adjacency & Off-Stat Flow
- The general tree’s adjacency ring defines off-stat flow: Wisdom → Luck → Constitution → Strength → Dexterity → Intelligence → Spirit → Wisdom.
- Off-stat clusters only pull from adjacent regions in this ring.
- Hub/bridge clusters are reserved for universal stats and cross-region blending.

### FR-2.2: Off-Stat Pools (Sparse Clusters)
Each region has a fixed, sparse off-stat pool to guide placement. These clusters are intentionally small and placed near bridges/hub paths.

- Strength (off-stat pools): AccuracyRating, AttackSpeed, MovementSpeed; ArmorRating, BlockChance, HealthRegen.
- Dexterity (off-stat pools): PhysicalDamageIncreased, AttackPower, Rage; SpellDamageIncreased, CastSpeed, ElementalDamageIncreased.
- Intelligence (off-stat pools): AttackSpeed, AccuracyRating, ProjectileSpeed; ReservationEfficiency, ManaRecoveryRate, CurseEffect.
- Constitution (off-stat pools): MeleeDamageIncreased, PhysicalDamageIncreased, StunThreshold; CritChance, ChaosDamageIncreased, CullingStrikeThreshold.
- Wisdom (off-stat pools): ReservationEfficiency, ManaRecoveryRate, MaxCurses; CritChance, ExperienceGain.
- Spirit (off-stat pools): SpellDamageIncreased, CastSpeed, ElementalDamageIncreased; AllResistance, CooldownRecovery, AuraEffect.
- Luck (off-stat pools): AllResistance, HealingEffectiveness, CooldownRecovery; MaxHealth, ArmorRating, BlockChance.

### FR-2.3: Stat-to-Region Index (Primary + Secondary)
This table defines the primary region for every stat, plus the intended sparse secondary regions (adjacent ring). “Hub” indicates universal placement limited to hub/bridge clusters.

| Stat | Primary Region | Secondary (Sparse) | Notes |
|---|---|---|---|
| AccuracyRating | Dexterity | Strength, Intelligence |  |
| AddedChaosDamage | Luck | Wisdom, Constitution |  |
| AddedColdDamage | Intelligence | Dexterity, Spirit |  |
| AddedFireDamage | Intelligence | Dexterity, Spirit |  |
| AddedLightningDamage | Intelligence | Dexterity, Spirit |  |
| AddedPhysicalDamage | Strength | Constitution, Dexterity |  |
| AdditionalProjectiles | Dexterity | Strength, Intelligence |  |
| AilmentDamage | Luck | Wisdom, Constitution |  |
| AilmentThreshold | Constitution | Luck, Strength |  |
| AllResistance | Wisdom | Spirit, Luck |  |
| AllSkillLevels | Hub | All regions (very sparse) | Hub/bridges only |
| AreaOfEffect | Wisdom | Spirit, Luck |  |
| ArmorPenetration | Strength | Constitution, Dexterity |  |
| ArmorRating | Constitution | Luck, Strength |  |
| AttackDamageIncreased | Strength | Constitution, Dexterity |  |
| AttackPower | Strength | Constitution, Dexterity |  |
| AttackSpeed | Dexterity | Strength, Intelligence |  |
| AttributeAll | Hub | All regions (very sparse) | Hub/bridges only |
| AuraArea | Wisdom | Spirit, Luck |  |
| AuraEffect | Wisdom | Spirit, Luck |  |
| AuraSkillLevels | Wisdom | Spirit, Luck |  |
| AutoBlockStaminaCost | Constitution | Luck, Strength |  |
| AxeSkillLevels | Strength | Constitution, Dexterity |  |
| BannerEffect | Wisdom | Spirit, Luck |  |
| BleedChance | Strength | Constitution, Dexterity |  |
| BleedDamage | Strength | Constitution, Dexterity |  |
| BleedDuration | Strength | Constitution, Dexterity |  |
| BleedPenetration | Strength | Constitution, Dexterity |  |
| BleedResistance | Constitution | Luck, Strength |  |
| BlockChance | Constitution | Luck, Strength |  |
| BlockMitigation | Constitution | Luck, Strength |  |
| BowSkillLevels | Dexterity | Strength, Intelligence |  |
| BrandAttachmentRange | Spirit | Intelligence, Wisdom |  |
| CastSpeed | Intelligence | Dexterity, Spirit |  |
| ChaosDamageIncreased | Luck | Wisdom, Constitution |  |
| ChaosDamageTaken | Constitution | Luck, Strength |  |
| ChaosPenetration | Luck | Wisdom, Constitution |  |
| ChaosResistance | Wisdom | Spirit, Luck |  |
| ChillEffect | Intelligence | Dexterity, Spirit |  |
| ColdDamageIncreased | Intelligence | Dexterity, Spirit |  |
| ColdPenetration | Intelligence | Dexterity, Spirit |  |
| ColdResistance | Wisdom | Spirit, Luck |  |
| ColdSkillLevels | Intelligence | Dexterity, Spirit |  |
| Concentration | Wisdom | Spirit (very rare) | Rare |
| ConcentrationLossReduction | Wisdom | Spirit (very rare) | Rare |
| ConcentrationLossThreshold | Wisdom | Spirit (very rare) | Rare |
| ConcentrationRegenRate | Wisdom | Spirit (very rare) | Rare |
| Constitution | Constitution | Luck, Strength |  |
| CooldownRecovery | Wisdom | Spirit, Luck |  |
| CritChance | Luck | Wisdom, Constitution |  |
| CriticalDamageTaken | Constitution | Luck, Strength |  |
| CritMultiplier | Luck | Wisdom, Constitution |  |
| CrossbowSkillLevels | Dexterity | Strength, Intelligence |  |
| CullingStrikeThreshold | Luck | Wisdom, Constitution |  |
| CurseDuration | Spirit | Intelligence, Wisdom |  |
| CurseEffect | Spirit | Intelligence, Wisdom |  |
| DaggerSkillLevels | Dexterity | Strength, Intelligence |  |
| DamageIncreased | Hub | All regions (very sparse) | Hub/bridges only |
| DamageOverTime | Luck | Wisdom, Constitution |  |
| DamageOverTimeTaken | Constitution | Luck, Strength |  |
| DamageTaken | Constitution | Luck, Strength |  |
| Dexterity | Dexterity | Strength, Intelligence |  |
| DodgeChance | Dexterity | Strength, Intelligence |  |
| DoubleDamageChance | Luck | Wisdom, Constitution |  |
| DualWieldDamage | Dexterity | Strength, Intelligence |  |
| EffectDuration | Wisdom | Spirit, Luck |  |
| ElementalDamageIncreased | Intelligence | Dexterity, Spirit |  |
| ElementalDamageTaken | Wisdom | Spirit, Luck |  |
| ElementalPenetration | Intelligence | Dexterity, Spirit |  |
| EvasionChance | Dexterity | Strength, Intelligence |  |
| EvasionRating | Dexterity | Strength, Intelligence |  |
| ExperienceGain | Luck | Wisdom, Constitution |  |
| FireDamageIncreased | Intelligence | Dexterity, Spirit |  |
| FirePenetration | Intelligence | Dexterity, Spirit |  |
| FireResistance | Wisdom | Spirit, Luck |  |
| FireSkillLevels | Intelligence | Dexterity, Spirit |  |
| FreezeChance | Intelligence | Dexterity, Spirit |  |
| FreezeDuration | Intelligence | Dexterity, Spirit |  |
| HealingEffectiveness | Wisdom | Spirit, Luck |  |
| HealingReceived | Constitution | Luck, Strength |  |
| HealthRegen | Constitution | Luck, Strength |  |
| HealthRegenPercent | Constitution | Luck, Strength |  |
| IgniteChance | Intelligence | Dexterity, Spirit |  |
| IgniteDamage | Intelligence | Dexterity, Spirit |  |
| IgniteDuration | Intelligence | Dexterity, Spirit |  |
| Intelligence | Intelligence | Dexterity, Spirit |  |
| IntimidateEffect | Strength | Constitution, Dexterity |  |
| ItemQuantity | Luck | Hub (very rare) | Rare |
| ItemRarity | Luck | Hub (very rare) | Rare |
| KnockbackChance | Strength | Constitution, Dexterity |  |
| KnockbackDistance | Strength | Constitution, Dexterity |  |
| KnockbackResistance | Constitution | Luck, Strength |  |
| LeechRate | Constitution | Luck, Strength |  |
| LifeCost | Constitution | Luck, Strength |  |
| LifeLeech | Constitution | Luck, Strength |  |
| LifeOnHit | Constitution | Luck, Strength |  |
| LifeOnKill | Constitution | Luck, Strength |  |
| LifeRecoveryRate | Constitution | Luck, Strength |  |
| LightningDamageIncreased | Intelligence | Dexterity, Spirit |  |
| LightningPenetration | Intelligence | Dexterity, Spirit |  |
| LightningResistance | Wisdom | Spirit, Luck |  |
| LightningSkillLevels | Intelligence | Dexterity, Spirit |  |
| Luck | Luck | Wisdom, Constitution |  |
| MaceSkillLevels | Strength | Constitution, Dexterity |  |
| ManaCost | Spirit | Intelligence, Wisdom |  |
| ManaLeech | Spirit | Intelligence, Wisdom |  |
| ManaOnHit | Spirit | Intelligence, Wisdom |  |
| ManaOnKill | Spirit | Intelligence, Wisdom |  |
| ManaRecoveryRate | Spirit | Intelligence, Wisdom |  |
| ManaRegen | Intelligence | Dexterity, Spirit |  |
| ManaRegenPercent | Intelligence | Dexterity, Spirit |  |
| MaxBlockChance | Constitution | Luck, Strength |  |
| MaxChaosResistance | Wisdom | Spirit, Luck |  |
| MaxColdResistance | Wisdom | Spirit, Luck |  |
| MaxCritChance | Luck | Wisdom, Constitution |  |
| MaxCurses | Spirit | Intelligence, Wisdom |  |
| MaxEvasionChance | Dexterity | Strength, Intelligence |  |
| MaxFireResistance | Wisdom | Spirit, Luck |  |
| MaxHealth | Constitution | Luck, Strength |  |
| MaxImpales | Strength | Constitution, Dexterity |  |
| MaxLifeLeechRate | Constitution | Luck, Strength |  |
| MaxLightningResistance | Wisdom | Spirit, Luck |  |
| MaxMana | Intelligence | Dexterity, Spirit |  |
| MaxMinions | Spirit | Intelligence, Wisdom |  |
| MaxRage | Strength | Constitution, Dexterity |  |
| MaxStamina | Dexterity | Strength, Intelligence |  |
| MaxTotems | Spirit | Intelligence, Wisdom |  |
| MaxTraps | Spirit | Intelligence, Wisdom |  |
| MeleeDamageIncreased | Strength | Constitution, Dexterity |  |
| MeleeSkillLevels | Strength | Constitution, Dexterity |  |
| MineArmingSpeed | Spirit | Intelligence, Wisdom |  |
| MineDamage | Spirit | Intelligence, Wisdom |  |
| MineThrowingSpeed | Spirit | Intelligence, Wisdom |  |
| MinionAccuracy | Spirit | Intelligence, Wisdom |  |
| MinionAttackSpeed | Spirit | Intelligence, Wisdom |  |
| MinionCritChance | Spirit | Intelligence, Wisdom |  |
| MinionDamage | Spirit | Intelligence, Wisdom |  |
| MinionDuration | Spirit | Intelligence, Wisdom |  |
| MinionLife | Spirit | Intelligence, Wisdom |  |
| MinionSkillLevels | Spirit | Intelligence, Wisdom |  |
| MinionSpeed | Spirit | Intelligence, Wisdom |  |
| MovementSpeed | Dexterity | Strength, Intelligence |  |
| OneHandedDamage | Strength | Constitution, Dexterity |  |
| PhysicalDamageIncreased | Strength | Constitution, Dexterity |  |
| PhysicalDamageTaken | Constitution | Luck, Strength |  |
| PhysicalResistance | Constitution | Luck, Strength |  |
| PoisonChance | Luck | Wisdom, Constitution |  |
| PoisonDamageIncreased | Luck | Wisdom, Constitution |  |
| PoisonDuration | Luck | Wisdom, Constitution |  |
| PoisonResistance | Constitution | Luck, Strength |  |
| ProjectileChainCount | Dexterity | Strength, Intelligence |  |
| ProjectileDamage | Dexterity | Strength, Intelligence |  |
| ProjectileForkCount | Dexterity | Strength, Intelligence |  |
| ProjectilePierceCount | Dexterity | Strength, Intelligence |  |
| ProjectileSpeed | Dexterity | Strength, Intelligence |  |
| Rage | Strength | Constitution, Dexterity |  |
| RangedDamageIncreased | Dexterity | Strength, Intelligence |  |
| ReflectDamageTaken | Constitution | Luck, Strength |  |
| ReservationEfficiency | Spirit | Intelligence, Wisdom |  |
| ShieldDamage | Strength | Constitution, Dexterity |  |
| ShockChance | Intelligence | Dexterity, Spirit |  |
| ShockDuration | Intelligence | Dexterity, Spirit |  |
| ShockEffect | Intelligence | Dexterity, Spirit |  |
| SkillCostFlat | Spirit | Intelligence, Wisdom |  |
| SpellBlockChance | Constitution | Luck, Strength |  |
| SpellDamageIncreased | Intelligence | Dexterity, Spirit |  |
| SpellPower | Intelligence | Dexterity, Spirit |  |
| SpellSkillLevels | Intelligence | Dexterity, Spirit |  |
| SpellSuppressionChance | Dexterity | Strength, Intelligence |  |
| SpellSuppressionEffect | Dexterity | Strength, Intelligence |  |
| Spirit | Spirit | Intelligence, Wisdom |  |
| StaffSkillLevels | Intelligence | Dexterity, Spirit |  |
| StaminaCost | Dexterity | Strength, Intelligence |  |
| StaminaRegen | Dexterity | Strength, Intelligence |  |
| Strength | Strength | Constitution, Dexterity |  |
| StunAvoidance | Constitution | Luck, Strength |  |
| StunDuration | Strength | Constitution, Dexterity |  |
| StunThreshold | Strength | Constitution, Dexterity |  |
| SwordSkillLevels | Dexterity | Strength, Intelligence |  |
| TwoHandedDamage | Strength | Constitution, Dexterity |  |
| UnarmedDamage | Strength | Constitution, Dexterity |  |
| Wisdom | Wisdom | Spirit, Luck |  |

### FR-3: Off-Stat Placement Rules
- Each region must contain small off-stat clusters (2–4 minors + 1 notable) tied to adjacent regions.
- Off-stat clusters are fewer than primary clusters and intentionally placed near bridges/hub paths.

### FR-4: Rare Stat Placement Rules
- Concentration and item-find stats are rare and appear in a small number of focused clusters.
- Concentration is primarily located in Wisdom/Spirit (with minimal presence elsewhere).
- Item-find is primarily located in Luck and limited hub clusters.

### FR-5: Density & Travel Constraints
- Travel nodes exist only to connect clusters, not to drain points.
- Target a compact layout where the average travel between clusters is minimal (configurable density target).
- Cluster entry should be reachable with 0–2 travel nodes from a branch.

### FR-6: Cluster Variety & Duplication Control
- Each region must include multiple cluster archetypes (offense, defense, resource, utility, ailment, specialization).
- No two notables in the same region should have identical stat sets.
- Minors within a cluster should vary within the same theme to avoid repetitive stacking.

### FR-7: Review & Validation
- A region-by-region audit is required before and after changes to validate:
  - Stat coverage completeness.
  - Duplication reduction.
  - Density targets.
  - Rare-stat constraints.

## Non-Functional Requirements
- Data-driven implementation (no hard-coded values).
- Coverage and density targets should be configurable in data.
- Maintainable and extensible for future stat additions.

## Dependencies
- Stats catalog in `Server/Hyforged/Stats`.
- General Passive Tree definitions (nodes/layouts).
- Passive Trees system and stat modifier application.

## Data/Schema Impact
- Introduce a coverage matrix artifact (data or documentation) mapping every stat to region, cluster type, and rarity tier.
- Optional: a lightweight region theme configuration file to guide future additions.

## API Changes
- None.

## Security/Privacy
- No security or privacy impact.

## Observability
- Add validation output summarizing stat coverage and travel density checks (build-time or editor-time reporting).

## Risks
- Dense layouts may reduce readability or increase UI clutter if not grouped well.
- Overlapping themes may reintroduce duplication if the coverage matrix is not enforced.
- Large-scale layout edits may require migration/refund handling.

## Open Questions
- None.

## Acceptance Criteria
- [ ] Every stat in the Stats catalog appears at least once in the General Tree.
- [ ] Each region has primary clusters aligned to its themes and at least one off-stat cluster.
- [ ] Concentration and item-find stats are rare and limited to intentional clusters.
- [ ] Travel nodes are reduced to a minimal, configurable target and do not dominate point spend.
- [ ] Region audit sign-off confirms reduced duplication and increased unique notables.

## Impacted Areas (High-Level)
- General tree node templates and layouts (by region).
- Cluster/notable definitions for missing stats.
- Documentation or data artifacts for coverage tracking.

## Required Codebase/Architecture Changes (High-Level)
- Expand region node templates with missing stat coverage.
- Rework region layouts to increase density and reduce travel node chains.
- Add a stat coverage matrix and validation checks.

## Phased Rollout (Region-by-Region)

### Phase 0 — Global Audit & Coverage Matrix
- Inventory all current nodes and map them to stats and regions.
- Identify missing stats, duplicated notables, and travel-heavy paths.
- Produce the coverage matrix and initial region gaps list.

### Phase 1 — Strength Region
- Fill missing physical/bleed/rage/weapon stat gaps.
- Add new melee-focused notables and reduce duplicated minors.
- Add one small off-stat cluster (Dexterity agility or Constitution sustain).
- Tighten travel chains to cluster entries.

### Phase 2 — Dexterity Region
- Fill accuracy/evasion/projectile/crit/stamina gaps.
- Add projectile-specialization and mobility clusters.
- Add one off-stat cluster (Intelligence elemental/spell utility).
- Reduce travel length between projectile clusters.

### Phase 3 — Intelligence Region
- Fill elemental/cast/mana/penetration/ailment gaps.
- Add new elemental mastery clusters and spell-crit alternatives.
- Add one off-stat cluster (Wisdom resistance/cooldown).
- Consolidate elemental clusters to reduce repetition.

### Phase 4 — Constitution Region
- Fill health/mitigation/block/resistance/regen gaps.
- Add distinct sustain clusters (regen vs. leech vs. mitigation).
- Add one off-stat cluster (Strength melee sustain or Wisdom resistance).
- Reduce travel between defense clusters.

### Phase 5 — Wisdom Region
- Fill aura/cooldown/duration/resistance/concentration stability gaps.
- Add support-oriented notables and defensive utility clusters.
- Add one off-stat cluster (Spirit reservation or Intelligence spell utility).
- Keep concentration rare and clearly signposted.

### Phase 6 — Spirit Region
- Fill mana recovery/reservation/minion/curse/totem/trap gaps.
- Add distinct archetype clusters (minions vs. curse vs. totems).
- Add one off-stat cluster (Wisdom aura or Intelligence spell utility).
- Ensure resource clusters are dense and connected.

### Phase 7 — Luck Region
- Fill chaos/poison/crit/culling/experience gaps.
- Add risk-reward notables with clear identity.
- Add one off-stat cluster (Dexterity speed or Constitution sustain).
- Keep item-find sparse and separated from core clusters.

### Phase 8 — Hub & Bridges Density Pass
- Ensure cross-region bridges contain mixed-theme clusters (not pure travel).
- Add hub clusters for universal stats (all-attributes, all-resistance, all-skill levels).
- Trim redundant travel chains and align bridges to off-stat goals.

## References
- Requirements entry: RPG/ARPG Systems → Passive Trees
- Existing Passive Trees spec
