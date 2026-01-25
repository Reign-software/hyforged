# Phase 2 Audit: Strength Region

## Metadata
- Feature ID: passive-tree-stat-diversity
- Date: 2026-01-25

## Coverage Review
- Primary strength themes (physical damage, attack power, attack speed, bleed, rage, knockback, weapon skill levels) are represented within the strength region clusters.
- Added bleed and weapon specialization notables to reduce reliance on generic minors.
- Armor penetration and added physical damage now appear in-region via updated minors and notables.

## Off-Stat Placement
- Strength off-stat cluster aligned to Constitution adjacency (armor/block/regen + constitution notable).
- Luck/item-find removed from strength region to preserve rarity rules.

## Density Review
- Travel chain reduced from a long linear spine to a compact set of nodes (approx. <= 20 travel nodes).
- Cluster entry points now sit closer to the spine, reducing travel tax.

## Duplication Notes
- Minor duplication reduced by replacing repeat crit/armor minors with bleed mastery, knockback, and weapon skill level coverage.
- Notables remain distinct from one another and aligned to cluster themes.

## Follow-Ups
- Build/test checks remain blocked by parent POM cycle error.
