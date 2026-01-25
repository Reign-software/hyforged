# Feature Plan: Passive Tree Stat Diversity & Region Plan

## Metadata
- Feature ID (slug): passive-tree-stat-diversity
- Status: In Progress
- Owner: JBurl
- Date: 2026-01-25

## ACID Plan Integrity
- Atomicity: Each phase updates a single region (or hub) and ends in a buildable, validated state.
- Consistency: Every step maps to the spec’s functional requirements (coverage, themes, off-stat rules, rarity, density, and audit).
- Isolation: Regional phases are scoped to their own clusters/layouts, minimizing cross-region dependencies.
- Durability: Plan status, coverage matrix, and audit outcomes are recorded in the memory bank.

## Phase 1: Global Audit & Coverage Matrix (Spec Phase 0)
- Phase Status: [ ] Not Started  [ ] In Progress  [x] Done

### Steps
- [x] Inventory current General Tree nodes and map each node to stat(s), region, and cluster archetype.
- [x] Produce a stat coverage matrix artifact (primary region, secondary region(s), cluster type, rarity tier).
- [x] Identify missing stats, duplicated notables, and travel-heavy paths per region.
- [x] Define configurable density targets and travel-node thresholds for the general tree.
- [x] Define validation outputs for coverage completeness and density checks.

### Exit Criteria
- [x] Coverage matrix artifact created and reviewed.
- [x] Region gap list and density targets documented.
- [x] Validation outputs defined for coverage and density checks.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 2: Strength Region (Spec Phase 1)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill missing physical/bleed/rage/weapon stat coverage from the matrix.
- [x] Add new melee-focused notables and reduce duplicated minors within Strength clusters.
- [x] Add one small off-stat cluster aligned to adjacency rules (Dexterity or Constitution).
- [x] Reduce travel-node chains to meet density targets for Strength branches.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Strength region achieves full stat coverage for assigned primaries.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Strength region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 3: Dexterity Region (Spec Phase 2)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill accuracy/evasion/projectile/crit/stamina gaps from the matrix.
- [x] Add projectile-specialization and mobility clusters with distinct notables.
- [x] Add one off-stat cluster aligned to adjacency rules (Intelligence).
- [x] Reduce travel length between projectile clusters to match density targets.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Dexterity region achieves full stat coverage for assigned primaries.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Dexterity region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 4: Intelligence Region (Spec Phase 3)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill elemental/cast/mana/penetration/ailment gaps from the matrix.
- [x] Add elemental mastery clusters and non-duplicative spell-crit alternatives.
- [x] Add one off-stat cluster aligned to adjacency rules (Spirit).
- [x] Consolidate elemental clusters to reduce repetition.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Intelligence region achieves full stat coverage for assigned primaries.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Intelligence region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 5: Constitution Region (Spec Phase 4)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill health/mitigation/block/resistance/regen gaps from the matrix.
- [x] Add distinct sustain clusters (regen vs. leech vs. mitigation).
- [x] Add one off-stat cluster aligned to adjacency rules (Luck).
- [x] Reduce travel between defense clusters to match density targets.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Constitution region achieves full stat coverage for assigned primaries.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Constitution region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 6: Wisdom Region (Spec Phase 5)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill aura/cooldown/duration/resistance/concentration stability gaps from the matrix.
- [x] Add support-oriented notables and defensive utility clusters.
- [x] Add one off-stat cluster aligned to adjacency rules (Spirit).
- [x] Ensure concentration stats remain rare and clearly isolated.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Wisdom region achieves full stat coverage for assigned primaries.
- [x] Concentration stats remain rare and intentionally placed.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Wisdom region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 7: Spirit Region (Spec Phase 6)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill mana recovery/reservation/minion/curse/totem/trap gaps from the matrix.
- [x] Add distinct archetype clusters (minions vs. curse vs. totems).
- [x] Add one off-stat cluster aligned to adjacency rules (Wisdom).
- [x] Ensure resource clusters are dense and well-connected.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Spirit region achieves full stat coverage for assigned primaries.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Spirit region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 8: Luck Region (Spec Phase 7)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Fill chaos/poison/crit/culling/experience gaps from the matrix.
- [x] Add risk-reward notables with distinct identities.
- [x] Add one off-stat cluster aligned to adjacency rules (Constitution).
- [x] Keep item-find stats sparse and separated from core clusters.
- [x] Run region audit for duplication, coverage, and rarity constraints.

### Exit Criteria
- [x] Luck region achieves full stat coverage for assigned primaries.
- [x] Item-find stats remain rare and intentionally placed.
- [x] Off-stat cluster placed and compliant with adjacency rules.
- [x] Travel density meets configured target for Luck region.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Phase 9: Hub & Bridges Density Pass (Spec Phase 8)
- Phase Status: [ ] Not Started  [x] In Progress  [ ] Done

### Steps
- [x] Ensure hub/bridge clusters contain mixed-theme clusters (not pure travel).
- [x] Add hub clusters for universal stats (all-attributes, all-resistance, all-skill levels) per coverage matrix.
- [x] Trim redundant travel chains and align bridges to adjacency off-stat goals.
- [x] Run final global audit for coverage completeness and duplication reduction.
- [x] Validate rare-stat placement rules across all regions.

### Exit Criteria
- [x] Hub/bridge clusters reflect universal stat placement rules.
- [x] Global coverage audit confirms 100% stat coverage.
- [x] Density targets and rare-stat constraints are satisfied.
- [ ] Build passes.
- [ ] Tests pass (if applicable).

## Dependencies
- Stats catalog in Server/Hyforged/Stats.
- General Passive Tree node templates and layouts.
- Passive Trees system and stat modifier application.
- Coverage matrix artifact and validation outputs defined in Phase 1.

## Risks & Mitigations
- Dense layouts reduce readability or increase UI clutter — enforce density targets and run visual review per region.
- Overlapping themes reintroduce duplication — use coverage matrix and region audits to gate changes.
- Large-scale layout edits require migration/refund handling — plan rollback and coordinate with any respec policies.

## Testing Strategy
- Run coverage validation output after each phase.
- Validate travel density metrics per region against configured targets.
- Build plugin after each phase; run applicable automated tests.

## Rollback Plan
- Revert region layout and node template changes to prior known-good data snapshots.
- Restore previous coverage matrix artifact and disable new validation outputs if needed.

## Deployment / Release Notes
- Data-only changes to passive tree definitions and validation artifacts; no API changes.

## Implementation Summary (post-development)
- Phase 1: Added coverage matrix and audit artifacts with density targets and validation output definitions.
- Phase 2 (in progress): Strength layout trimmed travel chains, added bleed/weapon notables, replaced luck off-stat cluster with constitution-aligned minors/notable, and completed strength region audit.
- Phase 3 (in progress): Dexterity layout trimmed travel spine, added projectile specialization/mobility notables, shifted off-stat cluster to intelligence-aligned spell/elemental minors, and completed dexterity region audit.
- Phase 4 (in progress): Intelligence layout trimmed travel spine, added elemental penetration/ailment coverage with new elemental and casting notables, and shifted off-stat cluster to spirit-aligned reservation/mana/curse minors.
- Phase 5 (in progress): Constitution layout trimmed travel spine, refocused leech and mitigation clusters, removed concentration/item-find, and aligned off-stat cluster to luck-adjacent chaos/crit/culling stats.
- Phase 6 (in progress): Wisdom layout trimmed travel spine, added aura command support notable, repurposed clusters for resistance/duration/cooldown coverage, moved concentration into a rare-focused cluster, and aligned the off-stat cluster to spirit-adjacent resource/support stats.
- Phase 7 (in progress): Spirit layout trimmed travel spine, refocused resource clusters on mana recovery/reservation, diversified minion/curse/totem/trap clusters, and aligned the off-stat cluster to wisdom-adjacent aura/area/resistance stats.
- Phase 8 (in progress): Luck layout trimmed travel spine, added chaos/poison and culling risk-reward notables, shifted experience gain into core clusters, removed extra item-find nodes, and aligned the off-stat cluster to constitution defenses.
- Phase 9 (in progress): Hub clusters gained universal skill-level coverage, and bridges were adjusted to remove item-find and align with adjacent stat themes while keeping mixed-theme routing.

## Test Results (post-validation)
- Build task failed: parent POM packaging/cycle error in Server parent (unrelated to passive tree data).

## Lessons Learned (post-release)
- TBD (post-release).
