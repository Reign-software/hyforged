# Requirements

## Vision
- Provide a concise, high-level source of truth for Hyforged requirements across major systems.

## Goals
- Track feature requirement sets in a navigable index.
- Keep requirements non-implementation-specific; implementation details belong in feature specs/plans.

## Non-Goals
- Detailed design documents.
- Code-level tasks or implementation plans.

## Quality Attributes
- Traceability: each feature spec links back to a requirements entry.
- Clarity: requirements are readable and stable over time.

## Feature Index
- RPG/ARPG Systems: .memory_bank/Requirements/rpg-arpg/README.md
  - Stats System âœ…
  - Entity Stats
  - Resource Stats UI (Concentration & Rage) â€” spec: [resource-stats-ui.spec.md](.memory_bank/Features/resource-stats-ui/resource-stats-ui.spec.md), plan: [resource-stats-ui.plan.md](.memory_bank/Features/resource-stats-ui/resource-stats-ui.plan.md)
  - Concentration Disruption System â€” spec: [concentration-disruption.spec.md](.memory_bank/Features/concentration-disruption/concentration-disruption.spec.md)
  - Progression Systems (Experience + Class) â€” spec: [progression-systems.spec.md](.memory_bank/Features/progression-systems/progression-systems.spec.md), plan: [progression-systems.plan.md](.memory_bank/Features/progression-systems/progression-systems.plan.md)
  - Items (Affixes & Rarity) â€” spec: [items-affix-system.spec.md](.memory_bank/Features/items-affix-system/items-affix-system.spec.md)
  - Random Item Quality â€” spec: [random-item-quality.spec.md](.memory_bank/Features/random-item-quality/random-item-quality.spec.md), plan: [random-item-quality.plan.md](.memory_bank/Features/random-item-quality/random-item-quality.plan.md)
  - NPC Quality & Affixes â€” spec: [random-item-quality.spec.md](.memory_bank/Features/random-item-quality/random-item-quality.spec.md), plan: [random-item-quality.plan.md](.memory_bank/Features/random-item-quality/random-item-quality.plan.md) (included in Random Item Quality)
  - Combat System â€” spec: [combat-system.spec.md](.memory_bank/Features/combat-system/combat-system.spec.md)
  - Passive Trees
  - Passive Tree Stat Diversity â€” spec: [passive-tree-stat-diversity.spec.md](.memory_bank/Features/passive-tree-stat-diversity/passive-tree-stat-diversity.spec.md), plan: [passive-tree-stat-diversity.plan.md](.memory_bank/Features/passive-tree-stat-diversity/passive-tree-stat-diversity.plan.md)
  - Currency (Tradebars) â€” spec: [currency-tradebars.spec.md](.memory_bank/Features/currency-tradebars/currency-tradebars.spec.md), plan: [currency-tradebars.plan.md](.memory_bank/Features/currency-tradebars/currency-tradebars.plan.md)
  - Enchanting, Runes, and Forging
  - Trading & Marketplace
  - **Stat Integration Audit** â€” DD: [stat-integration-audit.dd.md](.memory_bank/Features/stat-integration-audit/stat-integration-audit.dd.md), Spec: [stat-integration-audit.spec.md](.memory_bank/Features/stat-integration-audit/stat-integration-audit.spec.md), Plan: [stat-integration-audit.plan.md](.memory_bank/Features/stat-integration-audit/stat-integration-audit.plan.md)
  - **Equipment Stats Panel** â€” Spec: [equipment-stats-panel.spec.md](.memory_bank/Features/equipment-stats-panel/equipment-stats-panel.spec.md)

## Change Log
- 2026-02-19: Equipment Stats Panel implementation complete  reviewed (Pass with Conditions, 4 minor findings). Updated feature index with plan and review links.
- 2026-02-19: Added Equipment Stats Panel spec to feature index. Enhances Character Stats page with visual equipment panel, item tooltips (quality + affixes), range column removal.
- 2026-01-27: Added Currency (Tradebars) spec and plan to feature index.
- 2026-01-25: Added Passive Tree Stat Diversity plan to feature index.
- 2026-01-24: Added Concentration Disruption System spec to feature index.
- 2026-01-23: Added Random Item Quality plan to feature index.
- 2026-01-23: Added Random Item Quality spec to feature index; linked Items Affix System spec.
- 2026-01-20: Added Combat System spec to feature index.
- 2026-01-20: Added Resource Stats UI (Concentration & Rage) spec to feature index.
- 2026-01-20: Added Progression Systems plan to feature index.
- 2026-01-20: Consolidated Experience/Class specs into Progression Systems spec.
- 2026-01-20: Added Experience System and Class System specs to index.
- 2026-01-19: Added Entity Stats to feature index.
- 2026-01-19: Added initial requirements index.
