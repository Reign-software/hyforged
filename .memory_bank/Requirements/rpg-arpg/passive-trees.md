# Requirements: Passive Trees

## Vision
- Provide large, Path-of-Exile-like passive trees per class that enable deep build customization, with a performant backend, strong UI navigation, and full extensibility.

## Goals
- Tree structure
  - Each class has a unique passive tree.
  - Each tree has a central starting node.
  - Nodes form a graph where players unlock nodes by spending passive points and meeting adjacency/connection rules.
  - Trees are designed to scale to very large sizes.
- Passive points and progression
  - Each level in a class grants 1 passive skill point.
  - Points can be refunded using Tradebars.
  - Refund rules (cost, partial refunds, constraints) are configurable.
- Passive effects
  - Passive nodes can grant:
    - Stat modifiers (via Stats System)
    - Conditional bonuses (where supported by the stat/mechanic model)
    - Unlock flags (e.g., enabling a mechanic or ability)
  - Passive effects are deterministic, server-authoritative, and auditable.
- Extensibility and API
  - Passive trees are data-driven.
  - Provide an API for other plugins/systems to:
    - Register nodes and edges
    - Register node effects
    - Query allocation state
    - Listen to allocation changes
- UI
  - Passive trees are visually represented in the UI.
  - UI supports navigation for very large graphs:
    - Zoom/pan
    - Search/filter by stat/effect
    - Node hover tooltips with effect breakdown
    - Highlighting connected/available nodes
  - UI clearly communicates:
    - Available points
    - Refund cost
    - Current allocations
- Persistence
  - Persist per-player passive allocations per class.
  - Support migrations if node IDs or tree structure evolves.
- Performance
  - Allocation changes update stats efficiently (incremental update rather than full recompute where possible).
  - Tree navigation UI avoids excessive network chatter; uses efficient data transfer.

## Non-Goals
- Allowing players to allocate nodes without connectivity rules.
- Free unlimited respecs (refund requires Tradebars).

## Quality Attributes
- Scalable UI/UX for very large trees.
- Safe and auditable allocation changes.
- Extensible node/effect model.
- High-performance recomputation/caching.

## Feature Index
- Graph model
  - Nodes, edges, starting node
  - Connectivity/eligibility rules
- Point economy
  - Earned per class level
  - Refund and costs
- Effects
  - Stat modifiers
  - Unlock flags
- UI
  - Zoom/pan/search
  - Tooltips and breakdown
- Persistence & migration

## Change Log
- 2026-01-19: Initial version drafted.
