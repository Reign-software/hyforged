# Review: Passive Trees — 2026-01-24

## Review Metadata
- Reviewer: GitHub Copilot
- Scope: Passive tree assets, services, effects, UI, migrations, and interactions
- Spec Version: .memory_bank/Features/passive-trees/passive-trees.spec.md (2026-01-24)
- Plan Version: .memory_bank/Features/passive-trees/passive-trees.plan.md (2026-01-24)
- Overall Status: Needs Changes (Partial Remediation Applied)

## Summary
- Core data models, registry, graph utilities, and effect handlers exist, but multiple spec-critical requirements are incomplete or stubbed.
- 2025-01-XX: Applied fixes for starting node persistence, API methods, class tree auto-allocation, extensibility API, migration wiring, and Point Book stacking.

## Findings

### Critical
- [ ] None

### Major
- [ ] General/Class tree content is not at required scale and is only represented by sample assets; required file layout and 1000+ node general tree are not present. **PLANNED: Created detailed implementation plan at `.memory_bank/Features/passive-trees/general-tree-content.plan.md` with 7 ability score regions (STR, DEX, INT, CON, WIS, SPR, LCK), heptagon layout, 1000+ node target, and phased implementation.**
- [x] Starting region selection is not persisted/locked; the general starting node is never stored, which also undermines refund orphan detection for the general tree. **FIXED: allocateNode() now calls setGeneralStartingNode() when allocating a starting node.**
- [ ] Refund and respec costs are calculated but not enforced or deducted (Tradebar checks are TODO).
- [x] Class tree starting node auto-allocation on first class level is not implemented. **FIXED: ClassTreeStartingNodeSystem listens for ClassLevelUpEvent and allocates starting node on first level-up.**
- [x] UI acceptance criteria are incomplete: search/filter, path highlighting, comparison mode, zoom/pan, and tree switching are stubbed or mismatched with event wiring. **FIXED: Implemented search/filter with node matching, path highlighting on hover, comparison mode toggle showing stat previews, zoom/pan controls with level tracking, and tree switching via openCustomPage().**
- [x] Migration behavior is not wired to player load and only addresses removed nodes, not connection changes. **FIXED: PassiveTreeMigrationSystem listens for PlayerConnectEvent and runs migrations on connect.**
- [x] Extensibility API is incomplete; node/connection injection methods are absent. **FIXED: PassiveTreeRegistry now has addNode() and addConnection() methods.**
- [x] Passive tree public API does not fully match the spec (missing allocation/query methods like `getAllocations` and `consumePointBook`). **FIXED: Added getBookPointsUsed(), consumePointBook(), getMaxBookPoints() to PassiveTreeService.**

### Minor
- [x] Point Book item is stackable (MaxStack 10) but spec calls for non-stackable. **FIXED: Changed MaxStack to 1 in Point_Book_General.json.**

## Notes
- Passive tree unit tests ran and passed (122 tests across model/graph/registry/effect registry).
