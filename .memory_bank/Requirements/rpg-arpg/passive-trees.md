# Requirements: Passive Trees

## Vision
- Provide large, Path-of-Exile-scale passive trees that enable deep build customization: a massive **General Passive Tree** (1000+ nodes) for character-level progression and smaller, focused **Class Passive Trees** (ascendancy-style) for class specialization. Both tree types feature performant backends, high-QoL UI navigation, and full extensibility.

## Goals
- Tree structure
  - **General Passive Tree**: A massive tree (1000+ nodes) for all characters.
    - **Top-down vertical layout** with 4 main attribute lanes (STR, DEX, INT, WIS).
    - Starting nodes positioned at top of tree (Y=0), one per lane.
    - Bridge zones between lanes using Constitution, Spirit, and Luck nodes.
    - Players freely choose a starting lane on first allocation.
    - Starting lane choice persists until full respec.
  - **Class Passive Trees**: Smaller, focused trees per class (ascendancy-style, 50-150 nodes typical).
    - Each class tree has a single central starting node (auto-allocated on first class level).
    - Players can have allocations in multiple class trees (tied to weapon proficiency).
  - Trees form graphs where players unlock nodes by spending points and meeting adjacency rules.
  - Trees are designed to scale to PoE-like sizes (1000+ nodes).
- Passive points and progression
  - **General Passive Points**:
    - Character level grants points: `level - 1` (0 at level 1, 99 at level 100).
    - **Point Books**: Legendary consumable items found in world chests grant +1 point each, up to 20 additional points.
    - Maximum general tree points: **119** (99 from levels + 20 from books).
  - **Class Passive Points**:
    - Each class level grants 1 class passive point.
    - Maximum per class: 20 points (class level cap 20).
    - Points are specific to each class tree; multiple classes can be progressed.
  - Points can be refunded using Tradebars.
  - Refund cost scales with character level (configurable formula).
  - Full respec and per-node refund options available.
- Passive effects
  - Passive nodes can grant:
    - Stat modifiers (via Stats System)
    - Spell grants (unlocking spells for the player)
    - Unlock flags (enabling mechanics or abilities, e.g., dual-wielding)
    - Conditional bonuses (where supported by the stat/mechanic model)
  - Node types:
    - **Minor**: Small stat bonuses (majority of tree).
    - **Notable**: Significant named bonuses.
    - **Keystone**: Build-defining nodes with upsides and downsides.
    - **Mastery**: Unlocks upon reaching a cluster; provides a choice.
    - **Unlock**: Gates mechanics, abilities, or spells.
  - Passive effects are deterministic, server-authoritative, and auditable.
- Extensibility and API
  - Passive trees are data-driven (JSON).
  - Provide an API for other plugins/systems to:
    - Register entire new trees
    - Add nodes and edges to existing trees
    - Register custom node effect types
    - Query allocation state
    - Listen to allocation change events
- UI
  - Passive trees are visually represented in a high-QoL UI.
  - UI supports navigation for very large graphs:
    - Smooth zoom/pan
    - Search/filter by stat, effect, or node name
    - Path highlighting showing shortest path and point cost to hovered node
    - Tooltips with effect breakdown and projected stat changes
    - Highlighting connected/available nodes
  - UI clearly communicates:
    - Available points (per tree)
    - Total allocated and maximum
    - Refund cost per node and for full respec
    - Current allocations and starting region
  - Comparison mode for planning builds.
- Persistence
  - Persist per-player passive allocations per tree (General + each Class).
  - Persist starting region selection (General Tree).
  - Persist Point Book usage count.
  - Support migrations if node IDs or tree structure evolve:
    - Affected allocations refunded automatically (no cost).
    - Player notified on login.
- Performance
  - Allocation changes update stats efficiently (incremental update).
  - Tree navigation UI uses virtualized rendering for large trees.
  - Path finding completes in < 5ms for any node pair.

## Non-Goals
- Allowing players to allocate nodes without connectivity rules.
- Free unlimited respecs (refund requires Tradebars).
- Client-authoritative allocation changes.
- Procedurally generated trees.

## Quality Attributes
- Scalable UI/UX for very large trees (1000+ nodes).
- Safe and auditable allocation changes.
- Extensible node/effect model.
- High-performance caching and rendering.
- High QoL with path highlighting, detailed tooltips, and smooth navigation.

## Feature Index
- Graph model
  - General Tree: 1000+ nodes, 4 main attribute lanes (STR, DEX, INT, WIS)
  - Bridge zones: Constitution (STR↔DEX), Spirit (DEX↔INT), Luck (INT↔WIS)
  - Class Trees: 50-150 nodes, single central starting node
  - Connectivity/eligibility rules
- Point economy
  - General: character level + Point Books (max 119)
  - Class: class level (max 20 per class)
  - Refund costs scaled by character level
- Effects
  - Stat modifiers
  - Spell grants
  - Unlock flags
  - Mastery choices
- UI
  - Zoom/pan/search
  - Path highlighting
  - Detailed tooltips with projections
  - Comparison mode
- Persistence & migration
  - Allocations, starting region, book points
  - Automatic refund on tree changes

## Spec
- [passive-trees.spec.md](../../Features/passive-trees/passive-trees.spec.md)

## Change Log
- 2026-01-24: Major revision with clarified requirements: two tree types, Point Books, spell grants, full UI spec, PoE scale.
- 2026-01-19: Initial version drafted.
