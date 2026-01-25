# Phase 1 Audit: General Tree Coverage & Density

## Metadata
- Feature ID: passive-tree-stat-diversity
- Date: 2026-01-25

## Coverage Summary (Current State)
- General tree nodes provide broad stat coverage, but regional placement does not consistently align with the planned primary/secondary mapping.
- Notable variety is low (typically ~4 notables per region), leading to repetitive clusters and limited theme depth.
- Off-stat placement frequently leaks beyond adjacent regions (notably luck/item-find minors in non-adjacent regions).

## Region Gap Highlights
- Strength: limited armor penetration/added physical/weapon-skill coverage in-region; bleed chance/penetration nodes are sparse; off-stat cluster alignment needs adjacency cleanup.
- Dexterity: projectile specialization depth (chain/fork/pierce) is thin; accuracy/crit alternatives are underrepresented; off-stat clusters need intelligence-aligned options only.
- Intelligence: elemental penetration and ailment specialization are not clustered distinctly; spell-crit alternatives are limited; off-stat clusters should target Wisdom only.
- Constitution: distinct sustain lanes (regen vs leech vs mitigation) are blended; max-resist and block-mitigation coverage is sparse.
- Wisdom: aura/cooldown/duration clusters are present but shallow; concentration stats are not clearly isolated as rare.
- Spirit: minion/curse/totem/trap themes need clearer separation; resource clusters are dispersed.
- Luck: chaos/poison/experience and culling themes are shallow; item-find should remain isolated and rare.

## Density Findings
- Regional layouts currently use long travel chains (~60 travel nodes) with multiple clusters gated behind large point taxes.
- Travel nodes account for a high share of placements and exceed desired density targets.

## Density Targets (Defined in Data)
- Config: Server/Hyforged/Config/passive-tree-density.json
- Target max travel nodes between cluster entries: 2
- Max travel chain length per region: 20
- Travel nodes share per region: <= 25%

## Validation Outputs (Defined)
- Coverage report: per-region stat coverage and primary/secondary compliance.
- Density report: travel node counts, chain lengths, and average travel between clusters.
- Duplication report: notables with identical or near-identical stat sets within a region.
