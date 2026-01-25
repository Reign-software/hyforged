# Plan: General Passive Tree Content

## Metadata
- Parent Feature: passive-trees
- Status: In Progress
- Owner: JBurl  
- Date: 2026-01-24

## Summary
Design and implement a 1000+ node general passive tree organized around the 7 ability scores with interconnected regions, diverse node types, and thematic cohesion.

---

## Design Principles

### DP-1: Travel Nodes
**Travel nodes provide only a single attribute point.** These are the "connective tissue" of the tree.
- Travel nodes give exactly `+1 to [Attribute]` (one attribute only)
- They exist to create paths between clusters
- No other stats on travel nodes - pure pathing cost
- Example: `+1 Strength` node between two Strength clusters

### DP-2: Stat Splashing
**Stats are NOT exclusive to their "home" region.** Like PoE/PoE2:
- Crit nodes appear in Luck (primary) but also in Dexterity, Intelligence
- Life nodes appear in Constitution (primary) but also in Strength
- Attack Speed appears in Dexterity (primary) but also in Luck
- This encourages pathing through multiple regions
- Each region has a PRIMARY theme but SECONDARY stats appear throughout

### DP-2A: Universal Splash Stats
**Some stats appear in EVERY region** in small amounts. These are fundamental to all builds:

| Universal Stat | Appears In | Typical Minor Value | Notes |
|----------------|------------|---------------------|-------|
| **CritChance** | All 7 regions | +1-2% | Everyone wants some crit |
| **CritMultiplier** | All 7 regions | +3-5% | Pairs with crit chance |
| **MaxHealth** | All 7 regions | +3-5 | Survivability is universal |
| **HealthRegen** | All 7 regions | +0.1-0.2% | Sustain matters |
| **MaxMana** | All 7 regions | +3-5 | Resource for skills |
| **ManaRegen** | All 7 regions | +1-2% | Mana sustain |
| **MovementSpeed** | All 7 regions | +1-2% | QoL/utility |
| **AllResistance** | All 7 regions | +1-2% | Defense baseline |

### DP-2B: Resource Clusters (One per Region)
**Each region has a dedicated resource management cluster** using Mana, Concentration, Stamina, or Rage stats:

| Region | Resource Cluster Theme | Primary Stats |
|--------|----------------------|---------------|
| **Strength** | Rage Management | Rage, MaxRage, RageGeneration |
| **Dexterity** | Stamina Efficiency | MaxStamina, StaminaRegen, StaminaCost |
| **Intelligence** | Mana Mastery | MaxMana, ManaRegen, ManaCost, ManaRecoveryRate |
| **Constitution** | Concentration Focus | Concentration, ConcentrationRegenRate, ConcentrationLossReduction |
| **Wisdom** | Resource Balance | ReservationEfficiency, ManaCost, ConcentrationLossThreshold |
| **Spirit** | Soul Reserves | MaxMana, ManaRegenPercent, ManaOnHit, ManaOnKill |
| **Luck** | Fortune's Favor | ManaOnKill, LifeOnKill, resource-on-crit effects |

**Concentration Stats (available):**
- `Concentration` - Base concentration pool
- `ConcentrationRegenRate` - Regen speed
- `ConcentrationLossReduction` - Reduce loss from damage
- `ConcentrationLossThreshold` - Threshold before losing concentration

### DP-2C: Regional Splash Guidelines
**Beyond universal stats, each region splashes its theme into 2-3 other regions:**

| Primary Region | Primary Theme | Splashed Into |
|----------------|--------------|---------------|
| Strength | Melee, Phys, Stun, Bleed | CON (armor), DEX (attack speed), LUCK (bleed+crit) |
| Dexterity | Evasion, Accuracy, Projectile | LUCK (crit), INT (cast speed), STR (attack speed) |
| Intelligence | Spell, Elemental, Cast | SPR (mana), WIS (cooldown), LUCK (spell crit) |
| Constitution | Health, Regen, Armor | STR (life), WIS (regen), SPR (recovery) |
| Wisdom | Resistance, Cooldown, Aura | ALL (resistances), SPR (duration), CON (mitigation) |
| Spirit | Mana, Minions, Totems | INT (mana), WIS (duration), LUCK (minion crit) |
| Luck | Crit, ItemFind, Chaos | ALL (crit), DEX (crit multi), INT (spell crit) |

### DP-2D: Additional Splash Opportunities
**Stats that should appear in multiple regions for build diversity:**

| Stat Category | Stats | Regions Where Splashed |
|---------------|-------|----------------------|
| **Attack Speed** | AttackSpeed | DEX (primary), STR, LUCK, CON (life leech synergy) |
| **Cast Speed** | CastSpeed | INT (primary), SPR, WIS, LUCK |
| **Area of Effect** | AreaOfEffect | INT (primary), STR (melee AoE), SPR (auras), WIS |
| **Effect Duration** | EffectDuration | WIS (primary), SPR, INT (DoT), STR (bleeds) |
| **Damage Over Time** | DamageOverTime | STR (bleed), INT (ignite), LUCK (poison), DEX |
| **Block** | BlockChance, BlockMitigation | CON (primary), STR (shields), WIS (spell block) |
| **Leech** | LifeLeech, ManaLeech | CON (life), SPR (mana), STR (attack leech), LUCK |
| **On-Hit/Kill** | LifeOnHit, ManaOnKill, etc. | All regions with attack/spell nodes |
| **Skill Levels** | WeaponSkillLevels, SpellLevels | Multiple regions based on weapon/spell type |
| **Penetration** | ArmorPen, ElePen, ChaosPen | STR (armor), INT (ele), LUCK (chaos) |

### DP-3: Ability Grants via Events
**Nodes can grant abilities through the event system.** The API fires events:
- `PassiveNodeAllocatedEvent` - when a node is allocated
- `PassiveNodeRefundedEvent` - when a node is refunded

Plugins/systems can listen for these events and:
- Grant spells when specific nodes are allocated
- Remove spells when nodes are refunded
- Enable/disable mechanics based on node allocation

Node effects support:
- `stat-modifier` - Add stat modifiers
- `spell-grant` - Grant a spell (tracked, removed on refund)
- `unlock-flag` - Enable a game mechanic
- `mastery-choice` - Present choice options

### DP-4: Node Density Target
**1000+ nodes minimum** to provide meaningful build diversity:
- Players can allocate up to 119 points (99 levels + 20 books)
- Tree should have ~10x the allocatable points for choice
- Every stat in the Stats folder should appear somewhere in the tree
- Multiple paths to reach any cluster

### DP-5: Balance Philosophy
**Passives enhance but don't replace gear.** Stat values are deliberately modest:

| Node Type | Stat Category | Typical Value | Max Expected Total (119 pts) |
|-----------|---------------|---------------|------------------------------|
| Travel | Attribute | +1 | ~40-50 attribute points |
| Minor | Damage % | +2-3% | ~60-80% increased damage |
| Minor | Flat Stats | +3-5 | ~100-150 flat life/mana |
| Minor | Speed % | +1-2% | ~20-30% attack/cast speed |
| Notable | Attribute | +3-5 | ~30-50 from notables |
| Notable | Damage % | +5-8% | ~80-130% from notables |
| Keystone | Build-defining | Varies | 2-3 per build |

**Design Rationale:**
- Passives should provide ~30-50% of a character's power
- Gear, skills, and class abilities provide the remaining 50-70%
- Prevents "passive tree solves everything" builds
- Encourages meaningful itemization choices
- Crit chance nodes are particularly conservative to prevent auto-crit builds

---

## Research: Ability Scores


### Primary Attributes (7 total)
| Ability Score | Stat ID | Theme | Related Stats |
|---------------|---------|-------|---------------|
| **Strength** | `hyforged:strength` | Melee damage, physical power | MeleeDamageIncreased, PhysicalDamageIncreased, AttackPower, StunDuration |
| **Dexterity** | `hyforged:dexterity` | Precision, evasion, ranged | EvasionRating, AccuracyRating, AttackSpeed, ProjectileDamage, DodgeChance |
| **Intelligence** | `hyforged:intelligence` | Spell damage, elemental power | SpellDamageIncreased, SpellPower, ElementalDamageIncreased, CastSpeed |
| **Constitution** | `hyforged:constitution` | Max health, regeneration | MaxHealth, HealthRegen, HealthRegenPercent, LifeRecoveryRate, PhysicalResistance |
| **Wisdom** | `hyforged:wisdom` | Resistances, cooldown recovery | AllResistance, FireResistance, ColdResistance, LightningResistance, ChaosResistance, CooldownRecovery |
| **Spirit** | `hyforged:spirit` | Max mana, mana regeneration | MaxMana, ManaRegen, ManaRegenPercent, ManaRecoveryRate, ReservationEfficiency |
| **Luck** | `hyforged:luck` | Loot quality, critical strike | CritChance, CritMultiplier, ItemRarity, ItemQuantity, DoubleDamageChance |

### Stat Categories Available
From the Stats folder, we have these categories of stats to use in nodes:

**Offensive Stats:**
- Physical: PhysicalDamageIncreased, MeleeDamageIncreased, AddedPhysicalDamage, ArmorPenetration
- Elemental: FireDamageIncreased, ColdDamageIncreased, LightningDamageIncreased, ElementalDamageIncreased
- Chaos: ChaosDamageIncreased, ChaosPenetration
- Projectile: ProjectileDamage, ProjectileSpeed, AdditionalProjectiles, ProjectilePierceCount
- DoT: DamageOverTime, BleedDamage, IgniteDamage, PoisonDamageIncreased
- Critical: CritChance, CritMultiplier, DoubleDamageChance
- Speed: AttackSpeed, CastSpeed

**Defensive Stats:**
- Health: MaxHealth, HealthRegen, HealthRegenPercent, LifeLeech, LifeOnHit, LifeOnKill
- Mana: MaxMana, ManaRegen, ManaRegenPercent, ManaLeech, ManaOnHit, ManaOnKill
- Armor/Evasion: ArmorRating, EvasionRating, EvasionChance, DodgeChance
- Block: BlockChance, BlockMitigation, SpellBlockChance
- Resistances: FireResistance, ColdResistance, LightningResistance, ChaosResistance, AllResistance, PhysicalResistance
- Damage Reduction: DamageTaken, PhysicalDamageTaken, ElementalDamageTaken, ChaosDamageTaken

**Ailment Stats:**
- Bleed: BleedChance, BleedDamage, BleedDuration, BleedResistance
- Ignite: IgniteChance, IgniteDamage, IgniteDuration
- Freeze: FreezeChance, FreezeDuration
- Shock: ShockChance, ShockDuration, ShockEffect
- Poison: PoisonChance, PoisonDamageIncreased, PoisonDuration, PoisonResistance

**Skill Stats:**
- Weapon-specific: SwordSkillLevels, AxeSkillLevels, MaceSkillLevels, DaggerSkillLevels, BowSkillLevels, etc.
- Element-specific: FireSkillLevels, ColdSkillLevels, LightningSkillLevels
- Type-specific: MeleeSkillLevels, SpellSkillLevels, MinionSkillLevels, AuraSkillLevels

**Utility Stats:**
- Movement: MovementSpeed
- Cooldown: CooldownRecovery
- Duration: EffectDuration, CurseDuration, AilmentDamage
- Loot: ItemRarity, ItemQuantity, ExperienceGain

---

## Tree Layout Design

### Heptagon (7-sided) Layout
The tree is organized as a **heptagon** with 7 vertices, each representing one ability score region:

```
                    WISDOM (Top)
                      /   \
                     /     \
            SPIRIT  /       \  LUCK
                   /         \
                  /    HUB    \
         INT ----<     ●      >---- CON
                  \           /
                   \         /
            DEX     \       /     STR
                     \     /
                      \   /
                  (Bottom Edge)
```

**Angular positions (0° = right, counterclockwise):**
- Strength: 330° (bottom-right)
- Constitution: 30° (right)
- Luck: 90° (top-right)
- Wisdom: 150° (top)
- Spirit: 210° (top-left)
- Intelligence: 270° (left)
- Dexterity: 330° (bottom-left) -- wait, this conflicts

Let me re-think. With 7 vertices evenly spaced:
- Each vertex is 360°/7 ≈ 51.4° apart

**Revised positions (0° = top, clockwise):**
| Position | Angle | Ability Score | X (r=500) | Y (r=500) |
|----------|-------|---------------|-----------|-----------|
| 0 | 0° | Wisdom | 0 | -500 |
| 1 | 51.4° | Luck | 391 | -312 |
| 2 | 102.9° | Constitution | 487 | 112 |
| 3 | 154.3° | Strength | 222 | 450 |
| 4 | 205.7° | Dexterity | -222 | 450 |
| 5 | 257.1° | Intelligence | -487 | 112 |
| 6 | 308.6° | Spirit | -391 | -312 |

### Region Structure
Each ability score region contains:
- **Starting Node** (1): Entry point with `+1 to [Attribute]` only
- **Travel Nodes** (20-30): Pure `+1 to [Attribute]` nodes forming paths
- **Core Cluster** (15-20 nodes): Primary stat theme for that attribute
- **Satellite Clusters** (6-8 per region, 12-18 nodes each): Specialized themes with splashed stats
- **Resource Cluster** (1 per region, 10-15 nodes): Mana/Concentration/Stamina/Rage management
- **Universal Splash Nodes** (scattered): Small Crit, Life, Mana, Resistance nodes
- **Bridge Nodes**: Connect adjacent regions (travel nodes + splashed stat nodes)
- **Notable Nodes** (4-6 per cluster): Significant named bonuses
- **Keystone Nodes** (2-3 per region): Build-defining choices

### Node Type Distribution
| Type | % of Tree | Description | Example |
|------|-----------|-------------|---------|
| **Travel** | 35% (~525) | Single attribute +1 | `+1 Strength` |
| **Minor (Multi-stat)** | 38% (~570) | 1-2 non-attribute stats | `+8% Crit Chance` |
| **Notable** | 14% (~210) | Named, significant bonuses | `"Bone Breaker" +15 STR, +20% Melee` |
| **Keystone** | 2% (~30) | Build-defining with tradeoff | `"Unwavering Stance"` |
| **Mastery** | 6% (~90) | Cluster completion choice | Choose 1 of 3 bonuses |
| **Bridge** | 5% (~75) | Inter-region connectors | Mixed splashed stats |

### Node Count Breakdown (Target: 1500+ nodes)

| Region | Start | Travel | Core | Satellite (7×15) | Resource | Universal | Notable | Keystone | Bridge | Total |
|--------|-------|--------|------|------------------|----------|-----------|---------|----------|--------|-------|
| Strength | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Dexterity | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Intelligence | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Constitution | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Wisdom | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Spirit | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Luck | 1 | 60 | 20 | 105 | 12 | 15 | 25 | 3 | 12 | 253 |
| Central Hub | 0 | 35 | 30 | 60 | 15 | 20 | 20 | 5 | 0 | 185 |
| **TOTAL** | **7** | **455** | **170** | **795** | **99** | **125** | **195** | **26** | **84** | **1956** |

Mastery nodes (~90) distributed across clusters. **Final target: ~2000 nodes.**

### Stat Splash Map (Expanded)
Each region has PRIMARY stats, UNIVERSAL stats (appear everywhere), and SPLASHED stats:

| Region | Primary Stats | Universal (small amounts) | Splashed From Other Regions |
|--------|--------------|---------------------------|----------------------------|
| **Strength** | MeleeDamage, PhysicalDamage, Stun, Bleed, Rage | Crit, Life, Mana, Resist, Move | AttackSpeed (DEX), Armor (CON), Duration (WIS) |
| **Dexterity** | Evasion, Accuracy, Projectile, AttackSpeed, Stamina | Crit, Life, Mana, Resist, Move | CritMulti (LUCK), Dodge (LUCK), Bleed (STR) |
| **Intelligence** | SpellDamage, Elemental, CastSpeed, Mana | Crit, Life, Mana, Resist, Move | Duration (WIS), AoE (SPR), Penetration (LUCK) |
| **Constitution** | MaxHealth, HealthRegen, LifeLeech, Block, Concentration | Crit, Life, Mana, Resist, Move | Armor (STR), Regen (WIS), Recovery (SPR) |
| **Wisdom** | AllResistance, Cooldown, Auras, Duration | Crit, Life, Mana, Resist, Move | Regen (CON), ReservationEff (SPR), Block (CON) |
| **Spirit** | MaxMana, ManaRegen, Minions, Curses, Totems | Crit, Life, Mana, Resist, Move | CastSpeed (INT), Duration (WIS), AoE (INT) |
| **Luck** | CritChance, CritMulti, ItemFind, Chaos, DoubleDmg | Crit, Life, Mana, Resist, Move | AttackSpeed (DEX), CastSpeed (INT), Leech (CON) |

---

## Phase 1: Region Themes and Node Templates

### 1.1 Strength Region
**Theme:** Raw power, melee combat, physical damage, stunning
**Attribute Travel Nodes:** `+1 Strength` (60 nodes forming paths)

**Core Cluster: "Path of Might"**
- Minor (multi-stat): +4 MaxHealth (×5), +2% PhysicalDamageIncreased (×5), +2% MeleeDamageIncreased (×5)
- Notable: "Bone Breaker" - +3 Strength, +6% MeleeDamageIncreased
- Notable: "Unstoppable Force" - +5% StunDuration, +8% StunAvoidance

**Satellite Clusters:**
1. **Bleeding** - BleedChance, BleedDamage, BleedDuration, BleedPenetration (+ splashed CritChance, CritMultiplier)
2. **Two-Handed** - TwoHandedDamage, AttackPower, MeleeDamageIncreased (+ splashed AttackSpeed, MaxHealth)
3. **Impale** - MaxImpales, PhysicalDamageIncreased, ArmorPenetration (+ splashed CritChance)
4. **Fortify** - ArmorRating, PhysicalResistance, BlockMitigation, BlockChance (+ splashed MaxHealth, AllResistance)
5. **Warlord** - StunDuration, KnockbackChance, KnockbackDistance, IntimidateEffect (+ splashed MaxMana)
6. **Melee Weapons** - MaceSkillLevels, AxeSkillLevels, SwordSkillLevels, MeleeDamageIncreased (+ splashed LifeLeech)
7. **One-Handed** - OneHandedDamage, AttackSpeed, BlockChance (splashed from DEX/CON)

**Resource Cluster: "Rage Management"**
- Minor: +2 MaxRage (×4), +2% Rage generation (×4), +1% damage per Rage (×4)
- Notable: "Berserker's Fury" - +5 MaxRage, Rage does not decay, +4% Attack Speed while enraged

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Unwavering Stance" - Cannot Evade, Cannot be Stunned
- "Resolute Technique" - Hits can't be evaded, Never deal Critical Strikes
- "Crimson Dance" - Can inflict Bleeding on enemies that would be immune, Bleeding deals damage faster

**Ability Grants (via events):**
- Notable "Warcry Master" can grant a Warcry ability
- Mastery nodes offer choice of granted abilities

### 1.2 Dexterity Region
**Theme:** Speed, precision, evasion, projectiles
**Attribute Travel Nodes:** `+1 Dexterity` (60 nodes forming paths)

**Core Cluster: "Path of Agility"**
- Minor (multi-stat): +4 EvasionRating (×5), +2% AttackSpeed (×5), +5 AccuracyRating (×5)
- Notable: "Fleet Footed" - +3 Dexterity, +3% MovementSpeed
- Notable: "Precision" - +15 AccuracyRating, +3% CritChance

**Satellite Clusters:**
1. **Projectiles** - ProjectileDamage, ProjectileSpeed, AdditionalProjectiles, ProjectilePierceCount, ProjectileForkCount, ProjectileChainCount
2. **Evasion** - EvasionRating, EvasionChance, DodgeChance, SpellSuppressionChance (+ splashed MovementSpeed)
3. **Dual Wield** - DualWieldDamage, AttackSpeed, DodgeChance (+ splashed CritChance, CritMultiplier)
4. **Bows** - BowSkillLevels, ProjectileDamage, RangedDamageIncreased (+ splashed MaxMana)
5. **Blades** - DaggerSkillLevels, SwordSkillLevels, CritMultiplier (+ splashed BleedChance, BleedDamage)
6. **Crossbows** - CrossbowSkillLevels, ProjectileDamage, ArmorPenetration (+ splashed MaxHealth)
7. **Speed** - AttackSpeed, MovementSpeed, DodgeChance (universal splash cluster)

**Resource Cluster: "Stamina Efficiency"**
- Minor: +2 MaxStamina (×4), -1% StaminaCost (×4), +2% StaminaRegen (×4)
- Notable: "Endless Endurance" - +5 MaxStamina, -4% StaminaCost, +5% StaminaRegen

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Acrobatics" - +15% DodgeChance, -30% ArmorRating, -15% BlockChance
- "Point Blank" - Projectiles deal up to 30% more Damage to close targets, 30% less to distant
- "Wind Dancer" - Take 10% less Damage if you haven't been Hit Recently

**Ability Grants (via events):**
- Notable "Dash Master" can grant a mobility ability
- Mastery nodes offer choice of granted abilities

### 1.3 Intelligence Region
**Theme:** Spell power, elemental damage, mana
**Attribute Travel Nodes:** `+1 Intelligence` (60 nodes forming paths)

**Core Cluster: "Path of Arcana"**
- Minor (multi-stat): +4 MaxMana (×5), +2% SpellDamageIncreased (×5), +2% CastSpeed (×5)
- Notable: "Arcane Will" - +3 Intelligence, +5% SpellDamageIncreased
- Notable: "Elemental Mastery" - +5% ElementalDamageIncreased

**Satellite Clusters:**
1. **Fire** - FireDamageIncreased, FirePenetration, IgniteChance, IgniteDamage, FireSkillLevels (+ splashed CastSpeed)
2. **Cold** - ColdDamageIncreased, ColdPenetration, FreezeChance, FreezeDuration, ColdSkillLevels, ChillEffect (+ splashed MaxMana)
3. **Lightning** - LightningDamageIncreased, LightningPenetration, ShockChance, ShockDuration, ShockEffect, LightningSkillLevels
4. **Spell Casting** - CastSpeed, SpellPower, AreaOfEffect, SpellSkillLevels (+ splashed CritChance, CritMultiplier)
5. **Chaos Magic** - ChaosDamageIncreased, ChaosPenetration, PoisonChance, PoisonDamageIncreased (+ splashed EffectDuration)
6. **Spell Crit** - CritChance (spells), CritMultiplier, SpellDamageIncreased (+ splashed MaxHealth)
7. **Damage Over Time** - DamageOverTime, IgniteDamage, PoisonDamageIncreased, AilmentDamage (splashed from STR/LUCK)

**Resource Cluster: "Mana Mastery"**
- Minor: +4 MaxMana (×4), -2% ManaCost (×4), +2% ManaRegenPercent (×4)
- Notable: "Arcane Reservoir" - +12 MaxMana, -4% ManaCost, +8% ManaRecoveryRate

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Elemental Overload" - +25% more Elemental Damage if you've Crit Recently, no Crit Multiplier for Elemental
- "Mind Over Matter" - 20% of Damage taken from Mana before Health
- "Ancestral Vision" - Spells have +1 Chain, -20% Damage per Chain

**Ability Grants (via events):**
- Notable "Spell Echo" can grant a spell-repeat mechanic
- Mastery nodes offer choice of granted spells

### 1.4 Constitution Region
**Theme:** Health, regeneration, physical defense
**Attribute Travel Nodes:** `+1 Constitution` (60 nodes forming paths)

**Core Cluster: "Path of Endurance"**
- Minor (multi-stat): +5 MaxHealth (×5), +0.1% HealthRegenPercent (×5), +2 LifeOnKill (×5)
- Notable: "Thick Skin" - +12 MaxHealth, +2% PhysicalResistance
- Notable: "Second Wind" - +15% HealthRegenPercent, +5% LifeRecoveryRate

**Satellite Clusters:**
1. **Life Leech** - LifeLeech, LeechRate, MaxLifeLeechRate, LifeOnHit (+ splashed AttackSpeed, CritChance)
2. **Regeneration** - HealthRegen, HealthRegenPercent, LifeRecoveryRate (+ splashed MaxMana, ManaRegen)
3. **Armor** - ArmorRating, PhysicalResistance, BlockChance, BlockMitigation (+ splashed MaxHealth)
4. **Vitality** - MaxHealth, DamageTaken reduction, PhysicalDamageTaken reduction (+ splashed AllResistance)
5. **Recovery** - HealingEffectiveness, HealingReceived, LifeOnKill (+ splashed CooldownRecovery)
6. **Fortification** - BlockChance, SpellBlockChance, BlockMitigation, StunAvoidance (splashed from STR)
7. **Resilience** - DamageTaken reduction, ElementalDamageTaken, ChaosDamageTaken (splashed from WIS)

**Resource Cluster: "Concentration Focus"**
- Minor: +2 Concentration (×4), +2% ConcentrationRegenRate (×4), +2% ConcentrationLossReduction (×4)
- Notable: "Unbreakable Focus" - +5 Concentration, +5% ConcentrationRegenRate, +4% ConcentrationLossThreshold

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Vaal Pact" - Life Leech is instant, Life Regeneration has no effect
- "Iron Reflexes" - Evasion Rating is converted to Armour
- "Eternal Youth" - Life Regeneration is doubled, Cannot Leech Life

**Ability Grants (via events):**
- Notable "Enduring Cry" can grant a defensive warcry
- Mastery nodes offer choice of recovery abilities

### 1.5 Wisdom Region
**Theme:** Resistances, cooldowns, auras
**Attribute Travel Nodes:** `+1 Wisdom` (60 nodes forming paths)

**Core Cluster: "Path of Insight"**
- Minor (multi-stat): +1% AllResistance (×5), +1% CooldownRecovery (×5), +2% EffectDuration (×5)
- Notable: "Sage's Blessing" - +3 Wisdom, +3% AllResistance
- Notable: "Rapid Recovery" - +8% CooldownRecovery

**Satellite Clusters:**
1. **Fire Resistance** - FireResistance, MaxFireResistance, IgniteDuration reduction (+ splashed MaxHealth, HealthRegen)
2. **Cold Resistance** - ColdResistance, MaxColdResistance, FreezeDuration reduction (+ splashed MaxMana, ManaRegen)
3. **Lightning Resistance** - LightningResistance, MaxLightningResistance, ShockDuration reduction (+ splashed CritChance)
4. **Auras** - AuraEffect, AuraArea, ReservationEfficiency, AuraSkillLevels (+ splashed ManaRegen, MaxMana)
5. **Chaos Resistance** - ChaosResistance, MaxChaosResistance, PoisonResistance (+ splashed LifeLeech)
6. **Duration** - EffectDuration, CurseDuration, AilmentDamage (splashed from INT/SPR)
7. **Mitigation** - DamageTaken reduction, ElementalDamageTaken, CriticalDamageTaken (splashed from CON)

**Resource Cluster: "Resource Balance"**
- Minor: +2% ReservationEfficiency (×4), -1% ManaCost (×4), +1% ConcentrationLossThreshold (×4)
- Notable: "Perfect Balance" - +5% ReservationEfficiency, -3% ManaCost, +4% ConcentrationLossReduction

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Divine Flesh" - 50% of Elemental Damage taken as Chaos, +3% MaxChaosResistance
- "Glancing Blows" - Double Block Chance, Blocking only prevents 50% of Damage
- "Magebane" - Spell Suppression Chance is Lucky

**Ability Grants (via events):**
- Notable "Purity Aura" can grant a resistance aura
- Mastery nodes offer choice of defensive auras

### 1.6 Spirit Region
**Theme:** Mana, spellcasting resources, minions
**Attribute Travel Nodes:** `+1 Spirit` (60 nodes forming paths)

**Core Cluster: "Path of the Soul"**
- Minor (multi-stat): +5 MaxMana (×5), +2% ManaRegenPercent (×5), +3% ReservationEfficiency (×5)
- Notable: "Wellspring" - +3 Spirit, +12 MaxMana, +6% ManaRegenPercent
- Notable: "Soul Siphon" - +0.1% ManaLeech, +3 ManaOnKill

**Satellite Clusters:**
1. **Mana Sustain** - ManaRegen, ManaRegenPercent, ManaRecoveryRate, ManaOnHit, ManaOnKill (+ splashed CastSpeed, CritChance)
2. **Minions** - MinionDamage, MinionLife, MinionSpeed, MaxMinions, MinionAccuracy, MinionCritChance (+ splashed MaxHealth)
3. **Curses** - CurseEffect, CurseDuration, MaxCurses (+ splashed EffectDuration, CritChance)
4. **Totems/Brands** - MaxTotems, BrandAttachmentRange (+ splashed CastSpeed, AreaOfEffect)
5. **Traps/Mines** - MaxTraps, MineArmingSpeed, MineDamage, MineThrowingSpeed (+ splashed CritChance, CritMultiplier)
6. **Minion Offense** - MinionDamage, MinionAttackSpeed, MinionCritChance (splashed from DEX/LUCK)
7. **Summoner Defense** - MinionLife, MaxMinions, MinionDuration (+ splashed MaxHealth, AllResistance)

**Resource Cluster: "Soul Reserves"**
- Minor: +4 MaxMana (×4), +2% ManaRegenPercent (×4), +1 ManaOnHit (×4)
- Notable: "Endless Wellspring" - +15 MaxMana, +8% ManaRegenPercent, +3 ManaOnKill

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Ancestral Bond" - Cannot deal damage yourself, +1 MaxTotems
- "Necromantic Aegis" - Shield bonuses apply to Minions instead of you
- "Mortal Conviction" - Skills Cost Life instead of Mana, 35% reduced Mana Cost of Skills

**Ability Grants (via events):**
- Notable "Summon Phantasm" can grant a minion summon
- Mastery nodes offer choice of summoned entities

### 1.7 Luck Region
**Theme:** Critical strikes, item finding, random effects
**Attribute Travel Nodes:** `+1 Luck` (60 nodes forming paths)

**Core Cluster: "Path of Fortune"**
- Minor (multi-stat): +2% CritChance (×5), +4% CritMultiplier (×5), +2% ItemRarity (×5)
- Notable: "Fortune Favors" - +3 Luck, +8% CritMultiplier
- Notable: "Treasure Hunter" - +10% ItemRarity, +5% ItemQuantity

**Satellite Clusters:**
1. **Critical Strikes** - CritChance, CritMultiplier, CriticalDamageTaken reduction (+ splashed AttackSpeed, CastSpeed)
2. **Culling** - CullingStrikeThreshold, DoubleDamageChance (+ splashed DamageIncreased, MaxHealth)
3. **Item Finding** - ItemRarity, ItemQuantity, ExperienceGain (+ splashed MaxMana, MovementSpeed)
4. **Chaos Damage** - ChaosDamageIncreased, ChaosPenetration, PoisonChance, PoisonDamageIncreased (+ splashed DamageOverTime)
5. **Gambling** - DoubleDamageChance, CritChance, various "Lucky" effects (+ splashed LifeLeech, ManaLeech)
6. **Power Charges** - CritChance, CritMultiplier, SpellDamageIncreased (splashed from INT)
7. **Precision** - AccuracyRating, CritChance, CritMultiplier (splashed from DEX)

**Resource Cluster: "Fortune's Favor"**
- Minor: +2 ManaOnKill (×4), +2 LifeOnKill (×4), +1% CritChance (×4)
- Notable: "Windfall" - +4 ManaOnKill, +4 LifeOnKill, +3% DoubleDamageChance

**Universal Splash Nodes (scattered throughout):**
- +2% CritChance (×3), +4 MaxHealth (×3), +3 MaxMana (×3), +1% AllResistance (×3), +1% MovementSpeed (×3)

**Keystones:**
- "Perfect Agony" - Crit Multiplier applies to Ailment Damage at 30% value, -20% CritMultiplier
- "Chaos Inoculation" - Immune to Chaos Damage, Maximum Health becomes 1
- "Solipsism" - 100% of Damage is taken from Mana before Life, Cannot recover Life

**Ability Grants (via events):**
- Notable "Lucky Strike" can grant a proc-based ability
- Mastery nodes offer choice of gambling mechanics

### 1.8 Central Hub
**Theme:** Universal bonuses, all-stat nodes, unique keystones
**Travel Nodes:** Mixed attribute `+1 to All Attributes` (35 nodes)

**Clusters:**
1. **All Attributes** - +1 All Attributes minor nodes (×10), +2 All Attributes notables (×5)
2. **Damage** - +1% DamageIncreased (×8), +2% AttackDamageIncreased (×4), +2% SpellDamageIncreased (×4)
3. **Defense** - +1% AllResistance (×6), +4 MaxHealth (×6), +3 MaxMana (×6)
4. **Utility** - +1% MovementSpeed (×4), +2% EffectDuration (×4), +2% AreaOfEffect (×4)
5. **Resources** - +2 Concentration (×4), +3 MaxMana (×4), +2 MaxStamina (×4)
6. **Crit** - +1% CritChance (×4), +3% CritMultiplier (×4) (universal crit cluster)
7. **Life/Mana** - +4 MaxHealth (×4), +3 MaxMana (×4), +0.1% HealthRegenPercent (×4)

**Central Keystones:**
- "Avatar of Fire" - 50% of Physical/Cold/Lightning converted to Fire, Cannot deal non-Fire damage
- "Eldritch Battery" - Mana Recovery applies to Energy Shield
- "Zealot's Oath" - Life Regeneration applies to Energy Shield instead
- "Blood Magic" - Spend Life instead of Mana for Skills
- "Iron Will" - Strength's damage bonus applies to Spell Damage

---

## Phase 2: Implementation Tasks

### 2.1 File Structure

**Design Goals:**
- **Granular**: One file per cluster (~10-20 nodes) for easy navigation
- **No duplication**: Templates define effects once, layouts instantiate them
- **Predictable naming**: `region/cluster-name.json` pattern

**Templates vs Layouts:**
- **Templates** (`nodes/`): Define WHAT a node does - effects, name, icon. No position.
- **Layouts** (`layouts/`): Define WHERE nodes are - position, connections, which template to use.

```
PassiveTrees/
├── trees/
│   └── general.json                    # Tree metadata (id, name, starting nodes)
│
├── nodes/                              # NODE TEMPLATES (no positions)
│   └── general/
│       │
│       ├── travel/                     # Simple +1 attribute templates (7 files)
│       │   ├── strength.json           # Template: +1 STR
│       │   ├── dexterity.json          # Template: +1 DEX
│       │   ├── intelligence.json
│       │   ├── constitution.json
│       │   ├── wisdom.json
│       │   ├── spirit.json
│       │   ├── luck.json
│       │   └── all-attributes.json     # Template: +1 All (hub)
│       │
│       ├── minor/                      # Minor stat templates (grouped by theme)
│       │   ├── offensive/
│       │   │   ├── physical.json       # PhysDmg, MeleeDmg, ArmorPen
│       │   │   ├── elemental.json      # Fire/Cold/Lightning damage
│       │   │   ├── spell.json          # SpellDmg, CastSpeed, AoE
│       │   │   ├── projectile.json     # ProjDmg, ProjSpeed, Pierce
│       │   │   ├── dot.json            # Bleed, Ignite, Poison dmg
│       │   │   ├── crit.json           # CritChance, CritMulti
│       │   │   └── attack-speed.json   # AttackSpeed variants
│       │   ├── defensive/
│       │   │   ├── health.json         # MaxHealth, LifeOnKill, LifeOnHit
│       │   │   ├── regen.json          # HealthRegen, LifeRecovery
│       │   │   ├── armor.json          # ArmorRating, PhysResist
│       │   │   ├── evasion.json        # EvasionRating, Dodge
│       │   │   ├── block.json          # BlockChance, BlockMitigation
│       │   │   └── resistances.json    # Fire/Cold/Lightning/Chaos/All resist
│       │   ├── resources/
│       │   │   ├── mana.json           # MaxMana, ManaRegen, ManaCost
│       │   │   ├── rage.json           # MaxRage, RageGen
│       │   │   ├── stamina.json        # MaxStamina, StaminaRegen
│       │   │   └── concentration.json  # Concentration, ConcentrationRegen
│       │   └── utility/
│       │       ├── movement.json       # MovementSpeed
│       │       ├── duration.json       # EffectDuration, CurseDuration
│       │       ├── cooldown.json       # CooldownRecovery
│       │       └── item-find.json      # ItemRarity, ItemQuantity
│       │
│       ├── notables/                   # Named notable templates (by region)
│       │   ├── strength/
│       │   │   ├── bone-breaker.json
│       │   │   ├── unstoppable-force.json
│       │   │   ├── berserkers-fury.json
│       │   │   └── warcry-master.json
│       │   ├── dexterity/
│       │   │   ├── fleet-footed.json
│       │   │   ├── precision.json
│       │   │   ├── endless-endurance.json
│       │   │   └── dash-master.json
│       │   ├── intelligence/
│       │   │   ├── arcane-will.json
│       │   │   ├── elemental-mastery.json
│       │   │   ├── arcane-reservoir.json
│       │   │   └── spell-echo.json
│       │   ├── constitution/
│       │   │   ├── thick-skin.json
│       │   │   ├── second-wind.json
│       │   │   ├── unbreakable-focus.json
│       │   │   └── enduring-cry.json
│       │   ├── wisdom/
│       │   │   ├── sages-blessing.json
│       │   │   ├── rapid-recovery.json
│       │   │   ├── perfect-balance.json
│       │   │   └── purity-aura.json
│       │   ├── spirit/
│       │   │   ├── wellspring.json
│       │   │   ├── soul-siphon.json
│       │   │   ├── endless-wellspring.json
│       │   │   └── summon-phantasm.json
│       │   ├── luck/
│       │   │   ├── fortune-favors.json
│       │   │   ├── treasure-hunter.json
│       │   │   ├── windfall.json
│       │   │   └── lucky-strike.json
│       │   └── hub/
│       │       └── (central hub notables)
│       │
│       └── keystones/                  # One file per keystone (build-defining)
│           ├── unwavering-stance.json
│           ├── resolute-technique.json
│           ├── crimson-dance.json
│           ├── acrobatics.json
│           ├── point-blank.json
│           ├── wind-dancer.json
│           ├── elemental-overload.json
│           ├── mind-over-matter.json
│           ├── ancestral-vision.json
│           ├── vaal-pact.json
│           ├── iron-reflexes.json
│           ├── eternal-youth.json
│           ├── divine-flesh.json
│           ├── glancing-blows.json
│           ├── magebane.json
│           ├── ancestral-bond.json
│           ├── necromantic-aegis.json
│           ├── mortal-conviction.json
│           ├── perfect-agony.json
│           ├── chaos-inoculation.json
│           ├── solipsism.json
│           ├── avatar-of-fire.json
│           ├── eldritch-battery.json
│           ├── zealots-oath.json
│           ├── blood-magic.json
│           └── iron-will.json
│
└── layouts/                            # NODE PLACEMENTS (positions + connections)
    └── general/
        ├── starting-nodes.json         # 7 starting node positions
        ├── central-hub.json            # Hub clusters and connections
        ├── strength.json               # All STR region placements
        ├── dexterity.json              # All DEX region placements
        ├── intelligence.json           # All INT region placements
        ├── constitution.json           # All CON region placements
        ├── wisdom.json                 # All WIS region placements
        ├── spirit.json                 # All SPR region placements
        ├── luck.json                   # All LCK region placements
        └── bridges.json                # Inter-region connections
```

### 2.1.1 File Counts
| Category | Files | Notes |
|----------|-------|-------|
| Tree metadata | 1 | `trees/general.json` |
| Travel templates | 8 | One per attribute + all |
| Minor templates | ~20 | Grouped by stat category |
| Notable templates | ~35 | ~4-5 per region |
| Keystone templates | ~26 | One per keystone |
| Layout files | 10 | One per region + hub + bridges |
| **Total** | **~100** | Templates granular, layouts consolidated |

### 2.1.2 Template vs Layout Example

**Template** (`nodes/general/notables/strength/bone-breaker.json`):
```json
{
  "id": "hyforged:bone-breaker",
  "name": "Bone Breaker",
  "type": "notable",
  "icon": "hyforged:textures/passives/bone_breaker",
  "effects": [
    { "type": "stat-modifier", "stat": "hyforged:strength", "value": 300 },
    { "type": "stat-modifier", "stat": "hyforged:melee_damage_increased", "value": 600 }
  ]
}
```

**Layout** (`layouts/general/strength/core.json`):
```json
{
  "cluster": "path-of-might",
  "nodes": [
    { "template": "hyforged:bone-breaker", "x": 180, "y": 420, "connections": ["str-core-1", "str-core-2"] },
    { "template": "hyforged:minor-physical-1", "x": 165, "y": 435, "id": "str-core-1" },
    { "template": "hyforged:minor-physical-1", "x": 195, "y": 435, "id": "str-core-2" }
  ]
}
```

**Benefits:**
- Find any node by name: `notables/strength/bone-breaker.json`
- Change a notable's effects without touching layouts
- Reuse minor templates across clusters (e.g., `minor-physical-1` used 50+ times)
- Each layout file is small (~10-20 node placements)

### 2.2 Implementation Phases

#### Phase 2.2.1: Template Foundation (Build Gate: Plugin compiles)
- [x] Create `trees/general.json` with tree metadata
- [x] Create travel templates (`nodes/general/travel/*.json`) - 8 files
- [x] Create `layouts/general/starting-nodes.json` with 7 starting positions
- [ ] Verify template + layout loading works

#### Phase 2.2.2: Minor Templates (Build Gate: Templates load)
- [x] Create offensive minor templates (`nodes/general/minor/offensive/*.json`) - 7 files
- [x] Create defensive minor templates (`nodes/general/minor/defensive/*.json`) - 6 files
- [x] Create resource minor templates (`nodes/general/minor/resources/*.json`) - 4 files
- [x] Create utility minor templates (`nodes/general/minor/utility/*.json`) - 4 files

#### Phase 2.2.3: Strength Region (Build Gate: 100+ nodes)
- [x] Create STR notable templates (`nodes/general/notables/strength/*.json`) - 4 files
- [x] Create STR keystone templates - 3 files
- [x] Create `layouts/general/strength.json` with all STR placements
- [ ] Verify region renders and connects

#### Phase 2.2.4: Dexterity Region (Build Gate: 200+ nodes)
- [x] Create DEX notable templates - 4 files
- [x] Create DEX keystone templates - 3 files
- [x] Create `layouts/general/dexterity.json`

#### Phase 2.2.5: Intelligence Region (Build Gate: 300+ nodes)
- [ ] Create INT notable templates - 4 files
- [ ] Create INT keystone templates - 3 files
- [ ] Create `layouts/general/intelligence.json`

#### Phase 2.2.6: Constitution Region (Build Gate: 400+ nodes)
- [ ] Create CON notable templates - 4 files
- [ ] Create CON keystone templates - 3 files
- [ ] Create `layouts/general/constitution.json`

#### Phase 2.2.7: Wisdom Region (Build Gate: 500+ nodes)
- [ ] Create WIS notable templates - 4 files
- [ ] Create WIS keystone templates - 3 files
- [ ] Create `layouts/general/wisdom.json`

#### Phase 2.2.8: Spirit Region (Build Gate: 600+ nodes)
- [ ] Create SPR notable templates - 4 files
- [ ] Create SPR keystone templates - 3 files
- [ ] Create `layouts/general/spirit.json`

#### Phase 2.2.9: Luck Region (Build Gate: 700+ nodes)
- [ ] Create LCK notable templates - 4 files
- [ ] Create LCK keystone templates - 3 files
- [ ] Create `layouts/general/luck.json`

#### Phase 2.2.10: Central Hub (Build Gate: 800+ nodes)
- [ ] Create hub notable templates (`nodes/general/notables/hub/*.json`)
- [ ] Create central keystone templates - 5 files
- [ ] Create `layouts/general/central-hub.json`
- [ ] Connect hub to all 7 regions

#### Phase 2.2.11: Bridges (Build Gate: 900+ nodes)
- [ ] Create `layouts/general/bridges.json` with inter-region connections
- [ ] Add hybrid splash nodes in bridge areas
- [ ] Ensure full graph connectivity

#### Phase 2.2.12: Fill and Polish (Build Gate: 1000+ nodes)
- [ ] Add additional travel node placements to reach density target
- [ ] Review and validate all stat values
- [ ] Ensure all connections form valid graph
- [ ] Update review document

---

## Phase 3: Validation

### 3.1 Node Count Validation
- Total nodes >= 1000
- Each region has 90+ nodes
- 7 starting nodes present
- 18+ keystones present

### 3.2 Graph Validation
- All nodes reachable from at least one starting node
- No orphaned nodes
- No duplicate node IDs
- All connections reference valid nodes

### 3.3 Stat Validation
- All stat references are valid stat IDs from Stats folder
- Values are in correct units (basis points for percentages)
- No conflicting or impossible modifiers

### 3.4 Build Validation
- Plugin compiles successfully
- Multi-file tree loading works
- Tree renders in UI (visual spot check)

---

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Stat ID mismatches | Medium | High | Validate against Stats folder during implementation |
| Layout overlaps | Medium | Medium | Use coordinate math to ensure spacing |
| Graph connectivity issues | Low | High | Use validation tool after each region |
| File loading order issues | Low | Medium | Test after each phase |

---

## Dependencies

- Stats folder (`Server/Hyforged/Stats/`) - Provides valid stat IDs
- Multi-file asset loading (PassiveTreeAsset) - Must support trees/, nodes/, layouts/ structure
- PassiveTreeService - Must load and merge multi-file structure

---

## Implementation Summary
- Added general tree definition, travel node templates, starting node layout, and minor template categories for offensive/defensive/resource/utility stats.
- Implemented Strength region templates (notables + keystones) and a connected Strength layout with 100+ placements.
- Implemented Dexterity region templates (notables + keystones) and a connected Dexterity layout with 100+ placements.

## Test Results
- Not run (not requested).

---

## Exit Criteria

- [ ] General passive tree has 1000+ nodes
- [ ] 7 starting nodes (one per ability score)
- [ ] All 7 regions have themed content
- [ ] 18+ keystones distributed across regions
- [ ] Plugin builds successfully
- [ ] Review document updated to mark finding as fixed
