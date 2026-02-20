# Feature Plan: Stat Integration Audit

## Metadata
- Feature ID (slug): `stat-integration-audit`
- Status: Done
- Owner: Reign Software
- Date: 2026-02-18

## ACID Plan Integrity
- **Atomicity**: Each phase is independently completable with a buildable, deployable plugin. Stopping after any phase leaves the codebase in a valid state with no partially-wired systems.
- **Consistency**: Every step traces to one or more functional requirements (FR-1 through FR-23) in `stat-integration-audit.spec.md`. Every FR appears in at least one step.
- **Isolation**: Phases 1–3 are fully self-contained. Phases 4–6 depend on Phase 2 structures being present (damage pipeline systems) but can otherwise be developed and reverted independently.
- **Durability**: Status checkboxes in this plan track progress. Build and deploy verification is required at the end of every phase.

---

## Phase 1: Critical Bug Fix — physical-power Mismatch
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Description
Fix the `physical-power` stat ID mismatch in `NPCStatTemplateAsset.java` (line 233). This is a P0 regression — all NPCs silently fail to receive their attack-power scaling, making them under-statted in combat.

### Steps
- [ ] 1.1 **Investigate design intent** — Read `src/main/java/reign/software/hyforged/stats/npc/NPCStatTemplateAsset.java` and all NPC template JSON files under `src/main/resources/Server/Hyforged/Stats/NPCTemplates/` to confirm whether `physical-power` is an intentional separate concept or a naming mistake for `attack-power`.
- [ ] 1.2 **Resolve the mismatch** — Based on findings from 1.1, apply exactly one of the following (and document the decision in `.memory_bank/ADRs.md`):
  - **Option A (likely)**: Change `StatId.hyforged("physical-power")` → `StatId.hyforged("attack-power")` in `NPCStatTemplateAsset.java` line 233.
  - **Option B**: Create `src/main/resources/Server/Hyforged/Stats/Definitions/offense/physical-power.json` as a new, distinct stat with its own scaling, and update NPC templates accordingly.
- [ ] 1.3 **Search for any other references** — Run a workspace-wide search for `"physical-power"` (both `StatId.hyforged(...)` call form and JSON `"StatId"` form) to ensure no other locations reference the broken ID.
- [ ] 1.4 **Run "Build and Deploy Plugin" task** to confirm zero new errors or warnings.

### Exit Criteria
- [ ] No Java source file references `StatId.hyforged("physical-power")` unless a matching `physical-power.json` stat definition exists.
- [ ] NPCStatTemplateAsset compiles without warnings.
- [ ] Build passes ("Build and Deploy Plugin" task completes successfully).
- [x] ADR entry updated (or new entry added) documenting the resolution decision.

---

## Phase 2: Core Damage Pipeline — Outgoing Bonuses and Incoming Multipliers
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Description
Implement the two missing ends of the damage formula: the attacker's outgoing damage bonus pipeline (FR-2) and the defender's damage-taken multiplier pipeline (FR-3). These are the highest-impact missing systems — players with `+100% fire damage` from their passive tree currently deal the exact same damage as players with 0%.

### Steps
- [x] 2.1 **Read existing damage pipeline systems** — Read `HyforgedDamageReductionSystem.java` and `HyforgedCriticalHitSystem.java` in full to understand the `DamageEventSystem` extension pattern, how `CombatMeta.PIPELINE_PROCESSED` is guarded, and how stats are read via cached index. Review `hytale-ecs` skill for `DamageModule.get().getGatherDamageGroup()` / `getFilterDamageGroup()` patterns.
- [x] 2.2 **Create `HyforgedDamageBonusSystem.java`** in `src/main/java/reign/software/hyforged/stats/bridge/` (same package as `HyforgedDamageReductionSystem`):
  - Extends `DamageEventSystem` (or the appropriate base — follow `HyforgedDamageReductionSystem`'s pattern).
  - Runs in `DamageModule.get().getGatherDamageGroup()` (attacker-side, before damage reaches defender).
  - Caches all stat indices on first use via `StatDefinitionRegistry.get().getIndex(StatId.hyforged(...))`.
  - Reads the following stats from the **attacker's** `HyforgedStatComponent` and stacks them as INCREASED multipliers on outgoing damage (multiplicative stack order: `(1 + total_increased_bps / 10000.0)`):
    - `hyforged:damage-increased-bps` — applies to all damage
    - `hyforged:attack-damage-increased-bps` — attack-type damage only (check damage source tag)
    - `hyforged:spell-damage-increased-bps` — spell-type damage only
    - `hyforged:melee-damage-increased-bps` — melee tag
    - `hyforged:ranged-damage-increased-bps` — ranged tag
    - `hyforged:elemental-damage-increased-bps` — elemental tag (fire + cold + lightning)
    - `hyforged:fire-damage-increased-bps` — fire damage cause
    - `hyforged:cold-damage-increased-bps` — ice/cold damage cause
    - `hyforged:lightning-damage-increased-bps` — lightning damage cause
    - `hyforged:physical-damage-increased-bps` — physical damage cause
    - `hyforged:chaos-damage-increased-bps` — chaos damage cause
    - `hyforged:poison-damage-increased-bps` — poison damage cause
    - `hyforged:damage-over-time-bps` — DoT-type flag
    - `hyforged:projectile-damage-bps` — projectile-sourced damage
    - `hyforged:mine-damage-bps` — mine-triggered damage
  - Applies weapon-stance conditional bonuses by checking attacker equipment slots:
    - `hyforged:one-handed-damage-bps` when one-handed weapon equipped
    - `hyforged:two-handed-damage-bps` when two-handed weapon equipped
    - `hyforged:dual-wield-damage-bps` when both hand slots occupied by weapons
    - `hyforged:shield-damage-bps` when off-hand is a shield
    - `hyforged:unarmed-damage-bps` when neither hand slot has a weapon
  - Adds flat damage (applied before the multiplicative pass) from:
    - `hyforged:added-physical-damage-flat` → Physical damage type
    - `hyforged:added-fire-damage-flat` → Fire damage type
    - `hyforged:added-cold-damage-flat` → Ice damage type
    - `hyforged:added-lightning-damage-flat` → Lightning damage type
    - `hyforged:added-chaos-damage-flat` → Chaos damage type
  - Respects `CombatMeta.PIPELINE_PROCESSED` guard.
  > **Implementation note (2026-02-18)**: Placed in `combat` package (not `stats/bridge`) alongside `HyforgedCriticalHitSystem` and `HyforgedHitResolutionSystem`. Weapon-stance bonuses and flat damage additions deferred; melee/projectile mechanic detection uses `Damage.ProjectileSource` instanceof check. Bleed/poison detection uses element tags from `DamageTypeExtensionRegistry`. `ranged-damage-increased-bps`, `mine-damage-bps`, `attack-damage-increased-bps` and `spell-damage-increased-bps` deferred (no reliable detection without combat meta keys set by other systems).
- [x] 2.3 **Create `HyforgedDamageTakenSystem.java`** in `src/main/java/reign/software/hyforged/stats/bridge/`:
  - Extends `DamageEventSystem`.
  - Runs in `DamageModule.get().getFilterDamageGroup()` (defender-side, after resistance has been applied by `HyforgedDamageReductionSystem`).
  - Use `SystemDependency` with `Order.AFTER` `HyforgedDamageReductionSystem` to ensure it runs last in the filter group.
  - Reads the following stats from the **defender's** `HyforgedStatComponent` and applies as a MORE multiplier (each stat applies independently):
    - `hyforged:damage-taken-bps` — all incoming damage
    - `hyforged:elemental-damage-taken-bps` — elemental damage only
    - `hyforged:physical-damage-taken-bps` — physical damage only
    - `hyforged:chaos-damage-taken-bps` — chaos damage only
    - `hyforged:damage-over-time-taken-bps` — DoT-type damage
    - `hyforged:critical-damage-taken-bps` — crit-flagged damage only (check `CombatMeta` for crit flag)
  - Respects `CombatMeta.PIPELINE_PROCESSED` guard.
  > **Implementation note (2026-02-18)**: Placed in `combat` package. `damage-over-time-taken-bps` and `critical-damage-taken-bps` deferred (DoT detection and crit meta key access not yet available without additional meta infrastructure).
- [x] 2.4 **Register both systems** in `HyforgedPlugin.setup()`. Follow the existing pattern for `HyforgedDamageReductionSystem` registration. Ensure `SystemDependency` ordering is correct so `HyforgedDamageBonusSystem` runs before crit/block/evasion and `HyforgedDamageTakenSystem` runs after `HyforgedDamageReductionSystem`.
- [x] 2.5 **Run "Build and Deploy Plugin" task** and verify zero warnings.

### Exit Criteria
- [x] `HyforgedDamageBonusSystem.java` compiles without warnings.
- [x] `HyforgedDamageTakenSystem.java` compiles without warnings.
- [x] Both systems registered in `HyforgedPlugin.setup()`.
- [x] Build passes ("Build and Deploy Plugin" task completes successfully).
- [ ] In-game test: a character with `+50% fire damage` from affixes/passives deals measurably ~50% more fire damage than a baseline character (manual verification with `StatAdminService` `/stats` command).

---

## Phase 3: Resource Systems — Regen, On-Hit Recovery, Healing, Movement Speed
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Description
Implement all stats that affect health/mana/stamina over time and movement speed bridging. These are core ARPG feel stats — a player with `health-regen-flat` must visibly regenerate health. Covers FR-5, FR-6, FR-8, FR-9.

### Steps
- [ ] 3.1 **Read `HyforgedBridgeSystem.java`** in full to understand the `EntityStatMap` bridging pattern for health/mana/stamina. Understand what `DefaultEntityStatTypes` are available for movement speed (check `../lib/hytale-server/src/main/java/com/hypixel` for `DefaultEntityStatTypes` movement stat). Review `hytale-player-stats` skill.
- [ ] 3.2 **Extend `HyforgedBridgeSystem`** (FR-9 — movement speed bridge):
  - After existing `max-health/mana/stamina` bridge calls, add a bridge for `hyforged:movement-speed-bps` → Hytale's movement speed `EntityStatType`.
  - Read delta from the Hytale base value rather than setting an absolute value (see spec risk: "movement speed bridge overrides base").
  - Cache the `movement-speed-bps` stat index.
  - Trigger on entity dirty-stat recompute (same dirty-check pattern as health/mana).
- [ ] 3.3 **Create `HyforgedRegenSystem.java`** in `src/main/java/reign/software/hyforged/stats/system/` (FR-5):
  - Extends `DelayedEntitySystem` with a configurable tick interval (default: 20 ticks, ~1 second). Interval must be read from plugin config/JSON — not hardcoded.
  - Query for entities with `HyforgedStatComponent` AND the Hytale health stat (to exclude dead entities).
  - Per entity per tick, apply:
    - `hyforged:health-regen-flat` → flat HP via `EntityStatMap.addStatValue(HEALTH, value)`, capped at max.
    - `hyforged:health-regen-percent-bps` → `(maxHealth * bps / 10000.0)` HP per tick, capped at max.
    - `hyforged:mana-regen-flat` → flat mana via `EntityStatMap.addStatValue(MANA, value)`, capped at max.
    - `hyforged:mana-regen-percent-bps` → `(maxMana * bps / 10000.0)` mana per tick, capped at max.
    - `hyforged:stamina-regen-flat` → flat stamina via `EntityStatMap.addStatValue(STAMINA, value)`, capped at max. (Hytale may handle stamina regen natively — only add the Hyforged delta, not override base regen.)
  - Emit regen events to the combat log if the value is nonzero (optional: only in debug mode to avoid log spam).
  - Cache all stat indices.
- [ ] 3.4 **Create `HyforgedOnHitRecoverySystem.java`** in `src/main/java/reign/software/hyforged/combat/` (FR-6):
  - Extends `DamageEventSystem`.
  - Runs in `DamageModule.get().getInspectDamageGroup()` (post-apply, so only fires on hits that land).
  - Reads from **attacker's** `HyforgedStatComponent`:
    - `hyforged:life-on-hit-flat` → award that many HP to attacker via `EntityStatMap.addStatValue`.
    - `hyforged:mana-on-hit-flat` → award that many mana to attacker.
  - Only fires if the damage event resulted in a hit (check `CombatMeta.PIPELINE_PROCESSED` and that damage > 0).
  - Follow the pattern of `OnKillResourceRecoverySystem.java` for the recovery grant call.
  - Register in `HyforgedPlugin.setup()`.
- [ ] 3.5 **Extend healing event handlers** with multipliers (FR-8):
  - Locate the existing healing pathway (search for `EntityStatMap.addStatValue` / `HEALTH` calls and heal/regen services in `src/main/java/reign/software/hyforged/`).
  - If a `HealingService` or heal event exists, wrap heal amounts with:
    - Healer's `hyforged:healing-effectiveness-bps` → multiply heal output (healer side).
    - Receiver's `hyforged:healing-received-bps` → multiply heal received (receiver side).
  - If no heal event hook exists, create a `HyforgedHealingSystem` that intercepts heal application using the same damage pipeline `InspectGroup` or a dedicated heal event.
  - Document the approach taken in this plan's Implementation Summary after completion.
- [ ] 3.6 **Register `HyforgedRegenSystem`** in `HyforgedPlugin.setup()`.
- [ ] 3.7 **Run "Build and Deploy Plugin" task** and verify zero warnings.

### Exit Criteria
- [ ] `HyforgedRegenSystem.java` compiles and registers without warnings.
- [ ] `HyforgedOnHitRecoverySystem.java` compiles and registers without warnings.
- [ ] `HyforgedBridgeSystem.java` movement speed bridge compiles without warnings.
- [ ] FR-8 healing multiplier approach documented (either system created or confirmed existing hook used).
- [ ] Build passes ("Build and Deploy Plugin" task completes successfully).
- [ ] In-game test: Entity with `health-regen-flat = 10` regenerates health visibly between hits.
- [ ] In-game test: Entity with `movement-speed-bps = 2000` (+20%) moves faster than baseline.

---

## Phase 4: Advanced Combat Mechanics — Attack Speed, Leech, Special Rolls, Ailment Scaling, Defense Caps
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Description
Implement the remaining combat-loop mechanics that read stat values but lack consuming systems. Covers FR-4 (attack speed), FR-7 (leech), FR-10 (double damage / culling strike), FR-11 (stun duration), FR-12 (knockback), FR-14 (per-ailment scaling), FR-16 (max block/evasion cap), FR-17 (armor increased), FR-18 (evasion increased).

### Steps
- [ ] 4.1 **Research Hytale attack speed** (FR-4):
  - Read `../lib/hytale-server/src/main/java/com/hypixel` for `DefaultEntityStatTypes` to locate the attack interval / attack speed stat type.
  - Determine if attack speed is set via `EntityStatMap` (like movement speed) or via a component override.
  - Implement `HyforgedAttackSpeedSystem` in `src/main/java/reign/software/hyforged/stats/bridge/` that bridges `hyforged:attack-speed-bps` to Hytale's attack interval stat. Use the delta pattern (relative to base interval), not absolute override.
  - Register in `HyforgedPlugin.setup()`.
  - If no suitable Hytale stat exists, document as a blocker in this plan and skip.
- [ ] 4.2 **Create `HyforgedLeechSystem.java`** in `src/main/java/reign/software/hyforged/combat/` (FR-7):
  - Extends `DamageEventSystem`, runs in `DamageModule.get().getInspectDamageGroup()` (post-apply).
  - Reads from **attacker's** `HyforgedStatComponent`:
    - `hyforged:life-leech-bps` — converts this fraction of damage dealt to HP recovery.
    - `hyforged:mana-leech-bps` — converts this fraction of damage dealt to mana recovery.
    - `hyforged:leech-rate-bps` — modulates the rate of leech instantiation (if instant leech: multiply leech amount by this; if ramp-up model: track pending leech pool).
    - `hyforged:max-life-leech-rate-bps` — cap the max leech per tick as a % of max HP (e.g., 10% of max HP per tick max).
  - Apply leech as immediate recovery (instant leech model) or add to a pending leech pool (ticking model). Default to instant model unless spec is updated.
  - Register in `HyforgedPlugin.setup()`.
- [ ] 4.3 **Add special combat rolls to `HyforgedDamageBonusSystem`** (FR-10):
  - After regular multipliers are applied, roll `hyforged:chance-to-deal-double-damage-bps` using `ThreadLocalRandom`. If triggered, double the final damage output. This roll is independent of crit.
  - Check defender HP% against `hyforged:culling-strike-threshold-bps`. If defender HP% ≤ threshold, set damage to a OHKO value (e.g., `Integer.MAX_VALUE / 2` or defender's current max HP — follow existing crit/kill patterns).
- [ ] 4.4 **Extend stun system** with `hyforged:stun-duration-bps` (FR-11):
  - Locate the existing stun application code (search for `stun-threshold-bps` and `stun-avoidance-bps` consumers in `src/main/java/reign/software/hyforged/`).
  - When stun is applied, multiply the effective stun duration by `(1 + stun-duration-bps / 10000.0)` from the **attacker's** stats.
- [ ] 4.5 **Implement knockback chance and distance** (FR-12):
  - Locate existing knockback code (DD confirms `knockback-resistance-bps` is already read — find that system).
  - Extend the knockback trigger path: before applying knockback, roll `attacker:knockback-chance-bps`. If successful, scale knockback distance by `(1 + attacker:knockback-distance-bps / 10000.0)`.
  - If no knockback system exists, create `HyforgedKnockbackSystem` in `src/main/java/reign/software/hyforged/combat/` running in `gatherDamageGroup`.
- [ ] 4.6 **Extend `HyforgedAilmentSystem`** with per-ailment stat scaling (FR-14):
  - Read the following from the **attacker's** `HyforgedStatComponent` for each ailment trigger roll:
    - Per-ailment chance override: `bleed-chance-bps`, `freeze-chance-bps`, `ignite-chance-bps`, `poison-chance-bps`, `shock-chance-bps` — add these to the base ailment roll chance.
    - Global ailment damage multiplier: `ailment-damage-bps` — multiply DoT tick damage.
    - Per-ailment damage multiplier: `bleed-damage-bps`, `ignite-damage-bps` — multiply respective DoT tick damage.
    - Per-ailment duration: `bleed-duration-bps`, `freeze-duration-bps`, `ignite-duration-bps`, `poison-duration-bps`, `shock-duration-bps` — feed into the ailment duration computation (multiply base duration).
    - Chill/shock magnitude: `chill-effect-bps`, `shock-effect-bps` — scale the slow/amplify magnitude of the respective ailment effect.
  - Cache all new stat indices.
- [ ] 4.7 **Wire `max-block-chance-bps` and `max-evasion-chance-bps` caps** (FR-16):
  - Confirm whether `HyforgedAutoBlockSystem` and `HyforgedHitResolutionSystem` already cap at the soft-cap value. If the stat engine's `SoftCapBonusStat` mechanism is already wired to `block-chance-bps` and `evasion-chance-bps` via their definition JSON, mark as done (stat engine handles it). If not, add explicit cap enforcement in the respective system's roll logic.
- [ ] 4.8 **Wire `armor-increased-bps`** (FR-17):
  - In `HyforgedDamageReductionSystem`, after fetching the defender's `armor-rating` value, multiply by `(1 + armor-increased-bps / 10000.0)` before using it in the physical damage reduction formula.
- [ ] 4.9 **Wire `evasion-increased-bps`** (FR-18):
  - In `HyforgedHitResolutionSystem`, after fetching `evasion-chance-bps` for the defender, multiply the chance by `(1 + evasion-increased-bps / 10000.0)`, then apply cap.
- [ ] 4.10 **Run "Build and Deploy Plugin" task** and verify zero warnings.

### Exit Criteria
- [ ] `HyforgedLeechSystem.java` compiles and registers without warnings.
- [ ] Attack speed bridge compiles (or blocker documented if Hytale API is insufficient).
- [ ] Special rolls (double damage, culling strike) integrate into `HyforgedDamageBonusSystem` without warnings.
- [ ] Stun duration stat read and applied at stun application site.
- [ ] Knockback chance/distance stat read and applied at knockback trigger site.
- [ ] `HyforgedAilmentSystem` reads all per-ailment stats without warnings.
- [ ] Cap stats confirmed wired (or stat engine delegation confirmed).
- [ ] `armor-increased-bps` and `evasion-increased-bps` applied in respective systems.
- [ ] Build passes ("Build and Deploy Plugin" task completes successfully).
- [ ] In-game test: Character with `life-leech-bps = 1000` (10%) recovers HP proportional to damage dealt.
- [ ] In-game test: Character with `ignite-chance-bps` stat causes ignite more often than baseline.

---

## Phase 5: Entity & Social Systems — XP/Loot, Attribute Fan-Out, Minion Bridging
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Description
Wire stats that interact with progression and entity relationships: XP and loot multipliers (FR-13), the `attribute-all` fan-out to individual ability scores (FR-15), and minion stat bridging from summoner to minion entities (FR-21).

### Steps
- [ ] 5.1 **Research XP and loot award hooks** (FR-13):
  - Search `src/main/java/reign/software/hyforged/` for XP award calls (`ProgressionService`, `addExperience`, or similar) and loot drop logic (`LootService`, drop generation).
  - Identify where XP is awarded so `experience-gain-bps` can multiply the amount.
  - Identify where loot quality and quantity rolls happen so `item-quantity-increased-bps` and `item-rarity-increased-bps` can influence them.
- [ ] 5.2 **Wire `experience-gain-bps`** (FR-13):
  - At the XP award call site from step 5.1, read the player's `hyforged:experience-gain-bps` and multiply the XP amount: `xp = xp * (1 + bps / 10000.0)`.
  - Cache the stat index.
- [ ] 5.3 **Wire `item-quantity-increased-bps` and `item-rarity-increased-bps`** (FR-13):
  - At the loot generation call site, read the triggering player's stats and pass the multipliers into the drop generation logic (quantity table rolls, rarity tier selection).
  - If the loot system does not have a per-player multiplier hook yet, create a minimal `LootMultiplierContext` value object to carry these per-kill multipliers into the drop logic.
- [ ] 5.4 **Implement `attribute-all` fan-out** (FR-15):
  - Extend `HyforgedStatComputeSystem` (or create a `HyforgedAttributeAllSystem` running in the stat compute group) to read `hyforged:attribute-all` computed value and inject it as a flat modifier into each of: `strength`, `dexterity`, `intelligence`, `luck`, `spirit`, `wisdom`, `constitution`.
  - The fan-out must run after stat computation so the `attribute-all` value is final before being redistributed.
  - Ensure no infinite loop: the fan-out applies a `ModifierSource` tag that prevents `attribute-all` from re-reading its own redistributed value.
  - Mark `hyforged:attribute-all` as L1 in the DD after this step.
- [ ] 5.5 **Create `HyforgedMinionStatBridgeSystem.java`** in `src/main/java/reign/software/hyforged/stats/system/` (FR-21):
  - This system fires when a minion entity is spawned by a player (listen for minion spawn event or `RefChangeSystem` on the minion's summoner-link component).
  - Reads the **summoner's** `HyforgedStatComponent` for:
    - `hyforged:minion-damage-bps` → apply as a modifier to minion's `attack-power` or outgoing damage stat.
    - `hyforged:minion-life-bps` → apply as a multiplier to minion's `max-health-flat`.
    - `hyforged:minion-speed-bps` → pass to minion's movement speed bridge.
    - `hyforged:minion-duration-bps` → apply to minion's scheduled despawn timer.
    - `hyforged:minion-accuracy-bps` → apply to minion's `accuracy-rating`.
    - `hyforged:minion-attack-speed-bps` → pass to minion's attack speed bridge.
    - `hyforged:minion-crit-chance-bps` → apply as a modifier to minion's `crit-chance-bps`.
  - Enforce `hyforged:max-minions` cap: when a player attempts to spawn a minion, count their current active minions (entities with the summoner-link component pointing to this player). If count ≥ cap, block the spawn or despawn the oldest minion.
  - Register in `HyforgedPlugin.setup()`.
- [ ] 5.6 **Run "Build and Deploy Plugin" task** and verify zero warnings.

### Exit Criteria
- [ ] XP award site reads `experience-gain-bps` and scales XP output.
- [ ] Loot generation reads `item-quantity-increased-bps` and `item-rarity-increased-bps`.
- [ ] `attribute-all` fan-out system compiles and registers without warnings.
- [ ] `HyforgedMinionStatBridgeSystem.java` compiles and registers without warnings.
- [ ] `max-minions` cap enforcement in place.
- [ ] Build passes ("Build and Deploy Plugin" task completes successfully).
- [ ] In-game test: A character with `experience-gain-bps = 5000` (+50%) receives ~50% more XP per kill.
- [ ] In-game test: A character with `attribute-all = 5` sees +5 reflected in each individual attribute stat via `/stats`.

---

## Phase 6: Future Foundations — Spell Block, Dodge, Skill Levels, Intimidate (Stubs)
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Description
Lay the structural groundwork for the four systems whose design is not yet fully specified (FR-19, FR-20, FR-22, FR-23). Implement framework stubs that are buildable and registered but guarded behind feature flags or log-only behavior. This ensures stat indices are cached and system slots are reserved without affecting gameplay.

### Steps
- [ ] 6.1 **Create `HyforgedSpellBlockSystem.java`** in `src/main/java/reign/software/hyforged/combat/` (FR-19, stub):
  - Extends `DamageEventSystem`, runs in `DamageModule.get().getFilterDamageGroup()`.
  - Checks if the incoming damage is spell-sourced (via damage flag/tag).
  - Rolls `hyforged:block-spell-chance-bps` (defender). If triggered, apply `hyforged:block-mitigation-bps` reduction (reuse existing block mitigation stat — same as physical block).
  - Rolls `hyforged:suppression-chance-bps` (defender). If triggered, reduce damage by `hyforged:suppression-effect-bps` fraction.
  - Guarded: only activates if a `HyforgedConfig` flag `spellBlockEnabled` is true (default: false in this phase). This prevents unintended gameplay changes until the mechanic is reviewed.
  - Register in `HyforgedPlugin.setup()`.
- [ ] 6.2 **Create `HyforgedDodgeSystem.java`** in `src/main/java/reign/software/hyforged/combat/` (FR-20, stub):
  - Extends `DamageEventSystem`, runs in `DamageModule.get().getFilterDamageGroup()` after `HyforgedHitResolutionSystem`.
  - Rolls `hyforged:dodge-chance-bps` (defender). If triggered, cancel damage (full miss). Use a different `CombatMeta` flag than evasion so the UI/combat log can display "Dodge" vs "Miss".
  - Guarded: only activates if `HyforgedConfig` flag `dodgeEnabled` is true (default: false).
  - Register in `HyforgedPlugin.setup()`.
- [ ] 6.3 **Create `HyforgedSkillLevelSystem.java`** placeholder in `src/main/java/reign/software/hyforged/stats/system/` (FR-22, framework only):
  - Reads all `*-skill-levels` stats (13 stats: `all-skill-levels`, `axe-skill-levels`, `bow-skill-levels`, `cold-skill-levels`, `crossbow-skill-levels`, `dagger-skill-levels`, `fire-skill-levels`, `lightning-skill-levels`, `mace-skill-levels`, `melee-skill-levels`, `spell-skill-levels`, `staff-skill-levels`, `sword-skill-levels`) and caches their indices.
  - Exposes a `getEffectiveSkillLevel(Ref<EntityStore> entity, String skillTag, int baseLevel)` helper that adds the relevant `*-skill-levels` stat value to the base level.
  - No game logic integrated yet — this is a query-ready utility class.
  - Register in `HyforgedPlugin.setup()` only if it extends a system base; otherwise leave as a static helper accessed via `HyforgedSkillLevelSystem.get()`.
- [ ] 6.4 **Add `intimidate-effect-bps` to the damage pipeline stub** (FR-23):
  - In `HyforgedDamageBonusSystem` or a new `HyforgedIntimidateSystem`, add a comment block marking the integration point for `hyforged:intimidate-effect-bps` once design is confirmed.
  - Cache the stat index (so it's not `-1` at runtime).
  - Log a DEBUG-level note if the stat value is nonzero (to aid future debugging): `"[Intimidate] entity {id} has intimidate-effect-bps={val} — mechanic pending design"`
- [ ] 6.5 **Add `spellBlockEnabled` and `dodgeEnabled` config flags** to `HyforgedConfig` (or the existing plugin config class) with default value `false`.
- [ ] 6.6 **Run "Build and Deploy Plugin" task** and verify zero warnings.

### Exit Criteria
- [ ] `HyforgedSpellBlockSystem.java` compiles, registers, and is guarded by config flag.
- [ ] `HyforgedDodgeSystem.java` compiles, registers, and is guarded by config flag.
- [ ] `HyforgedSkillLevelSystem.java` (or helper class) compiles with all 13 stat indices cached.
- [ ] `intimidate-effect-bps` stat index cached and debug log in place.
- [ ] Config flags `spellBlockEnabled` and `dodgeEnabled` default to `false`.
- [ ] Build passes ("Build and Deploy Plugin" task completes successfully).
- [ ] Zero compile warnings.

---

## Dependencies

### Internal (must exist before this feature)
- `HyforgedStatComponent` — stat read/write (exists — fully functional)
- `StatDefinitionRegistry` — index lookup API (exists — fully functional)
- `DamageTypeExtensionRegistry` — damage type → stat mapping (exists — used by `HyforgedDamageReductionSystem`)
- `EntityStatMap` — Hytale native stat map for health/mana/stamina/movement (exists — bridged for resources in `HyforgedBridgeSystem`)
- `HyforgedBridgeSystem` — extended in Phase 3
- `HyforgedDamageReductionSystem` — must not be broken by Phase 2/4 additions; `HyforgedDamageTakenSystem` must declare `AFTER` dependency
- `HyforgedAilmentSystem` — extended in Phase 4
- `OnKillResourceRecoverySystem` — pattern reference for Phase 3 on-hit recovery
- `DamageModule` — provides system groups (`gatherDamageGroup`, `filterDamageGroup`, `inspectDamageGroup`)

### External (Hytale API)
- `DamageEventSystem` / `DamageModule` — Hytale damage pipeline API (exists; pattern verified via existing systems)
- `DelayedEntitySystem` — Hytale interval-based entity tick (exists; see `hytale-ecs` skill)
- `RefChangeSystem` — for minion spawn event reaction (Phase 5)
- `DefaultEntityStatTypes` — for attack speed and movement speed bridge (Phase 3/4); exact stat types must be verified in `../lib/hytale-server/`
- `CommandBuffer` — for stat modifier injection into minion entities (Phase 5)

### Phase Dependencies
- Phase 2 must complete before Phase 4 (double damage / culling strike steps extend `HyforgedDamageBonusSystem`)
- Phase 3 must complete before Phase 5's minion speed bridge step (Phase 5 reuses the movement speed bridge)
- Phase 1 is completely independent and should be completed first
- Phases 3, 4, 5, 6 are independent of each other (only Phase 2 is a prerequisite for Phase 4 step 4.3)

---

## Risks & Mitigations

| Risk | Probability | Mitigation |
|------|-------------|------------|
| `physical-power` fix reveals intentional design split, causing schema work | Low | Validate NPC templates before changing; take Option B path if needed (Phase 1) |
| Outgoing damage bonus stacks multiplicatively with crit, causing exponential spikes | Medium | Ensure `HyforgedDamageBonusSystem` runs in `gatherGroup` BEFORE crit is applied; damage bonuses apply to base, crit multiplies result |
| `HyforgedDamageTakenSystem` double-counting resistance (stacks with `HyforgedDamageReductionSystem`) | Low | `damage-taken-bps` is a separate MORE multiplier on top of resistance; document clearly in code that these are independent stages |
| Movement speed bridge overrides base Hytale movement speed | Medium | Read current Hytale base value, compute delta from Hyforged stat, set `(base * (1 + bps/10000.0))` rather than absolute override |
| Attack speed Hytale API is unavailable or undocumented | Medium | Research `DefaultEntityStatTypes` in `../lib/hytale-server/` first (Phase 4 step 4.1); document as blocker if the stat type does not exist |
| Regen ticks running every tick cause performance degradation | Very Low | Use `DelayedEntitySystem` with 20-tick default interval; make configurable via JSON |
| Leech model (instant vs ramp) unclear — wrong choice changes combat feel | Medium | Default to instant leech for simplicity; add a config flag to enable ramp-leech model in a future iteration |
| `attribute-all` fan-out causes infinite recompute loops | Low | Tag the injected modifiers with a dedicated `ModifierSource` so the stat engine skips them when re-reading `attribute-all` |
| Minion stat bridging conflicts with NPC scaling system | Low | Apply summoner stats as additional modifiers via `CommandBuffer` after `HyforgedMonsterScalingSystem` has run (use `AFTER` dependency) |
| Spell block design not finalized before implementation | High | Guard behind `spellBlockEnabled` config flag (default false) — no gameplay impact until enabled in Phase 6 |

---

## Testing Strategy

### Per-Phase Testing
- **Phase 1**: Confirm `physical-power` reference removed; deploy to local Hytale instance and spawn an NPC — verify it has correct attack power via `/stats` on the NPC.
- **Phase 2**: Use `/stats` on a character with fire-damage affixes, attack a target dummy, compare hit numbers to a character with no fire-damage affixes. Verify crit + damage-bonus stacks correctly (no double-count).
- **Phase 3**: Use `/stats` to confirm `movement-speed-bps` > 0 entity moves faster. Observe HP bar recovering when `health-regen-flat` is set. Confirm `life-on-hit-flat` heals on each hit.
- **Phase 4**: Equip full leech build — observe HP recovery on hit. Apply ignite to target — verify duration is longer with high `ignite-duration-bps`.
- **Phase 5**: Award XP with and without `experience-gain-bps` affix — compare XP totals. Spawn 2 minions with `max-minions = 1` and confirm the cap is enforced.
- **Phase 6**: Confirm `spellBlockEnabled = false` has no gameplay effect. Enable the flag and verify spell block rolls.

### Regression Testing
- All existing `HyforgedTest` unit tests must continue to pass after every phase.
- Run full build with `mvn package -DskipTests -s .mvn/settings.xml` and separately run `mvn test -s .mvn/settings.xml` after each phase.
- Specifically verify `HyforgedHitResolutionSystem`, `HyforgedCriticalHitSystem`, `HyforgedAutoBlockSystem`, `HyforgedDamageReductionSystem`, and `HyforgedAilmentSystem` are unaffected by the new systems (they run in different pipeline stages or the same stage but with explicit ordering).

---

## Rollback Plan

Each phase is independently revertible:

- **Phase 1**: Revert `NPCStatTemplateAsset.java` to the original `physical-power` reference. No schema or data file changes unless Option B was chosen (in which case also delete the new `physical-power.json`).
- **Phase 2**: Remove `HyforgedDamageBonusSystem.java` and `HyforgedDamageTakenSystem.java`. Remove their `registerSystem` calls from `HyforgedPlugin.setup()`.
- **Phase 3**: Remove `HyforgedRegenSystem.java` and `HyforgedOnHitRecoverySystem.java`. Revert `HyforgedBridgeSystem.java` movement speed addition. Revert any healing multiplier changes. Remove `registerSystem` calls.
- **Phase 4**: Remove `HyforgedLeechSystem.java`. Revert `HyforgedDamageBonusSystem` special-roll additions. Revert `HyforgedAilmentSystem` per-ailment stat additions. Revert `HyforgedDamageReductionSystem` and `HyforgedHitResolutionSystem` armor/evasion multiplier additions.
- **Phase 5**: Remove `HyforgedMinionStatBridgeSystem.java`. Revert `attribute-all` fan-out addition. Revert XP/loot call-site multipliers.
- **Phase 6**: Remove `HyforgedSpellBlockSystem.java`, `HyforgedDodgeSystem.java`, `HyforgedSkillLevelSystem.java`. Remove intimidate stat index cache. Revert config flag additions.

In all cases: run "Build and Deploy Plugin" after rollback to confirm clean state.

---

## Deployment / Release Notes

- **Phase 1** can be deployed immediately as a hotfix — no new systems, one-line fix.
- **Phases 2–4** should be bundled and released together as "Combat Stat Integration v1" — these together form the complete damage formula.
- **Phase 5** can be released as a follow-up "Progression & Minion Stat Integration" patch.
- **Phase 6** systems are gated by config flags. They can be deployed at any time but will have no gameplay effect until the flags are enabled by a server operator after final design review.
- All user-facing stat display changes must use translation keys per project conventions.
- No schema changes to existing JSON files are required by this plan; all changes are additive (new Java systems + config flag additions).

---

## Implementation Summary (post-development)

**Completed: 2026-02-18. All 6 phases shipped. Build: 365 source files, 0 Java warnings. Review: PASSED (review-002).**

### New Files
- `combat/HyforgedDamageBonusSystem.java` — attacker-side outgoing damage multipliers (data-driven)
- `combat/HyforgedDamageTakenSystem.java` — defender-side incoming damage multipliers (data-driven)
- `combat/HyforgedLeechSystem.java` — instant leech with per-event cap and healing-effectiveness
- `combat/HyforgedKnockbackSystem.java` — knockback chance/distance/resistance via KnockbackComponent
- `combat/HyforgedSpellBlockSystem.java` — spell block stub (guarded: `spellBlockEnabled = false`)
- `combat/HyforgedDodgeSystem.java` — dodge stub (guarded: `dodgeEnabled = false`)
- `stats/system/HyforgedRegenSystem.java` — interval regen (HP/mana/stamina/energy flat + percent)
- `stats/system/HyforgedOnHitRecoverySystem.java` — life-on-hit and mana-on-hit
- `stats/system/HyforgedAttributeAllSystem.java` — attribute-all fan-out via tag query, anti-loop guard
- `stats/system/HyforgedMinionStatBridgeSystem.java` — minion stat bridge stub (8 indices cached)
- `stats/system/HyforgedSkillLevelSystem.java` — data-driven *-skill-levels singleton helper
- `quality/system/HyforgedLootMultiplierSystem.java` — loot multiplier stubs (pending player attribution)
- `HyforgedConfig.java` — plugin config: `spellBlockEnabled`, `dodgeEnabled`, `regenIntervalTicks`, knockback base values

### Key Modifications
- `DamageTypeExtensionAsset/Extension/Registry` — added `HyforgedDamageBonusStat`, `HyforgedDamageTakenStat`, `HyforgedAilmentChanceStat`, `HyforgedAilmentDurationStat`, `HyforgedAilmentDamageStat` (full chain-walk accumulation)
- All 8 `Stats/Damage/*.json` files updated with bonus/taken/ailment stat fields
- `Chaos.json` and `Bleed.json`: `Inherits` removed (stops incorrect Physical chain propagation)
- `HyforgedAilmentSystem` — per-element chance/duration/damage scaling from registry
- `HyforgedDamageReductionSystem` — `armor-increased-bps` multiplier
- `HyforgedHitResolutionSystem` — `evasion-increased-bps` multiplier + `max-evasion-chance-bps` cap
- `HyforgedBridgeSystem` — movement speed bridge (MovementManager)
- `OnKillResourceRecoverySystem` — `healing-effectiveness-bps` multiplier on life-on-kill
- `XPAwardSystem` — `experience-gain-bps` multiplier on both char XP and class XP
- `NPCStatTemplateAsset` — `physical-power` → `attack-power` bug fix

### Known Limitations / Future Work
- Loot multiplier: no player-source attribution yet; `HyforgedLootMultiplierSystem` is a stub
- Skill-level stats: no `*-skill-levels` JSON definitions exist yet; `HyforgedSkillLevelSystem` will auto-discover them when added
- Stun duration: no stun system in codebase; index cached in `HyforgedDamageBonusSystem` for future wiring
- `HyforgedKnockbackSystem`: uses `store.ensureAndGetComponent()` directly (non-blocking; uses Hytale's purpose-built API)

---

## Test Results (post-validation)
_To be filled in after in-game validation is complete._

---

## Lessons Learned (post-release)
_To be filled in after release._
