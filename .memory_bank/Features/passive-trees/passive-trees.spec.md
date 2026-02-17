# Feature Spec: Passive Trees

## Metadata
- Feature ID (slug): passive-trees
- Status: Draft
- Owner: JBurl
- Date: 2026-01-24

## Summary
Implement a Path-of-Exile-scale passive tree system with two distinct tree types: a massive **General Passive Tree** (1000+ nodes) fueled by character level points and consumable Point Books, and smaller, focused **Class Passive Trees** (ascendancy-style) fueled by class level points. Trees support multiple starting regions, various node types (minor, notable, keystone, mastery), stat modifiers, spell grants, and unlock flags. A high-quality UI provides zoom/pan navigation, path highlighting, detailed tooltips with calculations, and seamless refund workflows. All tree definitions are data-driven and extensible by other plugins.

## Goals
- Provide a massive General Passive Tree with 1000+ nodes for deep character customization.
- Provide smaller, focused Class Passive Trees (ascendancy-style) per class for specialization.
- Support multiple starting regions on the outer edge of the General Tree.
- Enable diverse node types: minor, notable, keystone, mastery, and unlock nodes.
- Allow nodes to grant stat modifiers, spells, and unlock flags.
- Provide full respec and per-node refund options with costs scaling by character level.
- Deliver a high-QoL UI with zoom/pan, search, path highlighting, and detailed tooltips.
- Ensure all trees, nodes, and effects are data-driven (JSON) and extensible.
- Support Point Books as legendary consumables granting additional general tree points.

## Non-Goals
- Automatic skill/ability unlocks outside of explicit node effects.
- Client-authoritative allocation changes.
- Procedurally generated trees (trees are hand-authored).
- Per-session or time-limited passive configurations.

## User Experience

### General Passive Tree
- Players access a massive tree (1000+ nodes) from the character UI.
- Tree has multiple **starting regions** positioned on the outer edge.
- Players select a starting region when first allocating points (free choice, not class-locked).
- Points are earned from character level (level - 1, giving 0-99 points) plus up to 20 additional points from Point Books.
- Maximum allocatable points on General Tree: **119** (99 from levels + 20 from books).

### Class Passive Trees
- Each class has its own smaller, focused passive tree (ascendancy-style).
- Class trees have a single starting node that is auto-allocated when the class is first leveled.
- Points are earned from class level (1 point per level, max 20 points per class).
- Players can have progress in multiple class trees (tied to weapon proficiency).
- Switching weapons changes active class but preserves all class tree allocations.

### Point Books
- Legendary-quality consumable items found in world chests.
- Each book grants +1 permanent point to the General Passive Tree.
- Maximum 20 Point Book points per character.
- Consuming a book when at cap shows an error and does not consume the item.

### UI Interactions
- **Navigation**: Smooth zoom (mouse wheel/pinch) and pan (drag).
- **Search**: Filter nodes by name, stat type, or effect; matching nodes are highlighted.
- **Path Highlighting**: When hovering an unallocated node, show the shortest path from current allocations with total point cost.
- **Tooltips**: Show node name, description, effects, and:
  - For allocated nodes: current contribution to stats.
  - For unallocated nodes: projected stat changes if allocated.
  - For refund: cost in Tradebars to refund.
- **Allocation Flow**: Click to allocate; confirm for keystones. Visual feedback on success.
- **Refund Flow**: Right-click or dedicated button to refund; shows cost and confirms before deducting Tradebars.
- **Starting Region Selection**: First-time modal for General Tree; visual indicator of chosen starting region.
- **Comparison Mode**: Toggle to compare current build vs. planned path.
- **Point Display**: Always visible: available points, total allocated, max possible.

## Functional Requirements

### FR-1: Tree Definitions (Data-Driven)

#### FR-1.1: General Passive Tree
- Defined in `Server/Hyforged/PassiveTrees/general.json`.
- Contains 1000+ nodes organized into regions/clusters.
- Multiple starting nodes on the outer edge; players choose one to begin allocation.
- Starting node selection persists and cannot be changed without full respec.

#### FR-1.2: Class Passive Trees
- Defined in `Server/Hyforged/PassiveTrees/classes/<class-id>.json`.
- Each class tree has a single central starting node (auto-allocated on first class level).
- Trees are smaller and focused (50-150 nodes typical).
- Trees are tied to class identity defined in `Server/Hyforged/Stats/Classes/`.

#### FR-1.3: Tree Schema
```json
{
  "Id": "hyforged:passive-tree-general",
  "Type": "PassiveTree",
  "TreeType": "general | class",
  "ClassId": "hyforged:warrior",  // Only for class trees
  "StartingNodes": ["node-001", "node-002", ...],  // Multiple for general, single for class
  "Nodes": { ... },
  "Connections": [ ... ]
}
```

### FR-2: Node Definitions

#### FR-2.1: Node Types
| Type | Description | Visual | Typical Count |
|------|-------------|--------|---------------|
| Minor | Small stat bonuses (+5 STR, +10 HP) | Small circle | 70% of tree |
| Notable | Significant named bonuses | Medium diamond | 20% of tree |
| Keystone | Build-defining; upside + downside | Large octagon | 1-2% of tree |
| Mastery | Unlocks upon reaching a cluster; provides choice | Star icon | Cluster-specific |
| Unlock | Gates mechanics/abilities/spells | Lock icon | Rare |

#### FR-2.2: Node Schema
```json
{
  "Id": "hyforged:node-brutal-force",
  "Type": "notable",
  "Name": "Brutal Force",
  "Description": "Your attacks are devastatingly powerful.",
  "Icon": "icons/passives/brutal_force.png",
  "Position": { "X": 1250, "Y": 800 },
  "Region": "strength-core",
  "Effects": [
    { "Type": "stat-modifier", "Stat": "hyforged:physical-damage-increased-bps", "Value": 1500 },
    { "Type": "stat-modifier", "Stat": "hyforged:attack-speed-increased-bps", "Value": -500 }
  ],
  "Requirements": {
    "AllocatedNodes": 0,  // For keystones, may require X total allocated
    "Tags": []  // Optional tag requirements
  }
}
```

#### FR-2.3: Node Effects
Nodes can grant one or more effects:

| Effect Type | Description | Example |
|-------------|-------------|---------|
| `stat-modifier` | Add modifier to a stat | `+10% physical damage` |
| `spell-grant` | Unlock a spell for the player | `Grant "Flame Dash"` |
| `unlock-flag` | Enable a mechanic or ability | `Enable dual-wielding` |
| `mastery-choice` | Present options when allocated | Choose 1 of 3 bonuses |

#### FR-2.4: Keystone Behavior
- Keystones have significant upsides and downsides.
- Only one keystone of a given "keystone family" can be active (mutual exclusion).
- Allocation requires confirmation dialog showing full effects.

### FR-3: Connection Model

#### FR-3.1: Graph Structure
- Trees are undirected graphs (nodes + edges).
- A node can be allocated if:
  - It is a starting node AND no other starting node is allocated (General Tree), OR
  - It is adjacent to an already-allocated node.
- Connections defined as edge list:
```json
{
  "Connections": [
    { "From": "node-001", "To": "node-002" },
    { "From": "node-002", "To": "node-003" }
  ]
}
```

#### FR-3.2: Connectivity Validation
- On allocation: verify path exists from starting node to target.
- On refund: prevent refunding nodes that would orphan other allocations (unless refunding the orphans too).

### FR-4: Point Economy

#### FR-4.1: General Passive Points
| Source | Points | Notes |
|--------|--------|-------|
| Character Level | Level - 1 | 0 at level 1, 99 at level 100 |
| Point Books | Up to 20 | Legendary consumables |
| **Total Maximum** | **119** | |

#### FR-4.2: Class Passive Points
| Source | Points | Notes |
|--------|--------|-------|
| Class Level | 1 per level | 1 at level 1, 20 at level 20 |
| **Total Maximum** | **20** | Per class |

#### FR-4.3: Point Book Item
- Item ID: `hyforged:point-book-general`
- Quality: Legendary
- Stack size: 1 (non-stackable)
- On use:
  - If player has < 20 book points: grant +1, consume item.
  - If player has >= 20 book points: show error, do not consume.
- Track `bookPointsUsed` in player progression state.

### FR-5: Refund System

#### FR-5.1: Refund Options
- **Full Respec**: Refund all allocated nodes at once.
- **Per-Node Refund**: Refund individual nodes (respecting connectivity).

#### FR-5.2: Refund Cost Formula
- Cost is in Tradebars.
- Cost scales with character level:
  - `costPerNode = baseCost + (characterLevel * levelMultiplier)`
  - Default: `baseCost = 10`, `levelMultiplier = 2`
  - At level 50: 10 + (50 × 2) = 110 Tradebars per node.
  - At level 100: 10 + (100 × 2) = 210 Tradebars per node.
- Full respec cost = sum of per-node costs for all allocated nodes.
- Costs are configurable in `Server/Hyforged/Config/passive-refund.json`.

#### FR-5.3: Refund Validation
- Cannot refund a node if it would orphan other allocated nodes (unless also refunding those).
- UI shows which nodes would be forcibly refunded and total cost.
- Class tree starting nodes cannot be refunded (auto-allocated).

#### FR-5.4: Refund on Tree Migration
- If tree structure changes (nodes removed, connections changed):
  - Affected allocations are fully refunded automatically (no Tradebar cost).
  - Points are returned to available pool.
  - Player is notified on login with details of changes.

### FR-6: Allocation API

#### FR-6.1: Core Operations
```java
public interface PassiveTreeService {
    // Query
    PassiveTree getGeneralTree();
    PassiveTree getClassTree(String classId);
    PlayerAllocations getAllocations(UUID playerId, String treeId);
    int getAvailablePoints(UUID playerId, String treeId);
    
    // Allocation
    AllocationResult allocateNode(UUID playerId, String treeId, String nodeId);
    RefundResult refundNode(UUID playerId, String treeId, String nodeId);
    RefundResult refundAll(UUID playerId, String treeId);
    
    // Point Books
    boolean consumePointBook(UUID playerId);
    int getBookPointsUsed(UUID playerId);
    
    // Validation
    boolean canAllocate(UUID playerId, String treeId, String nodeId);
    List<String> getOrphanedNodes(UUID playerId, String treeId, String nodeId);
    int calculateRefundCost(UUID playerId, String treeId, List<String> nodeIds);
    
    // Path Finding
    List<String> findPathToNode(UUID playerId, String treeId, String nodeId);
}
```

#### FR-6.2: Events
- `PassiveNodeAllocatedEvent`: Fired when a node is allocated.
- `PassiveNodeRefundedEvent`: Fired when a node is refunded.
- `PassiveTreeRespecEvent`: Fired on full respec.
- `PointBookConsumedEvent`: Fired when a Point Book is used.

### FR-7: Stat Integration

#### FR-7.1: Modifier Application
- When a node is allocated, its effects are applied to `HyforgedStatComponent`:
  - `stat-modifier` effects add modifiers with source `ModifierSource.PASSIVE`.
  - Modifiers are tagged with the node ID for breakdown attribution.
- When a node is refunded, its modifiers are removed.

#### FR-7.2: Spell Grants
- `spell-grant` effects register the spell as available to the player.
- Spell availability is tracked in a `PlayerSpellsComponent` (or equivalent).
- Refunding removes the spell (unless granted by another source).

#### FR-7.3: Unlock Flags
- `unlock-flag` effects set boolean flags in `PlayerUnlocksComponent`.
- Flags are queryable by other systems (e.g., combat checks `canDualWield`).
- Refunding clears the flag (unless set by another source).

### FR-8: Persistence

#### FR-8.1: Stored Data
Per player:
```json
{
  "generalTree": {
    "startingNode": "node-strength-start",
    "allocatedNodes": ["node-001", "node-002", "node-003", ...],
    "bookPointsUsed": 5
  },
  "classTrees": {
    "hyforged:warrior": {
      "allocatedNodes": ["warrior-001", "warrior-002", ...]
    },
    "hyforged:ranger": {
      "allocatedNodes": ["ranger-001", ...]
    }
  }
}
```

#### FR-8.2: Migration Support
- Tree definitions include a `version` field.
- On load, compare stored version with current version.
- If version mismatch, run migration:
  - Identify removed/moved nodes.
  - Refund affected allocations (points returned, no cost).
  - Log migration details.
  - Notify player on join.

### FR-9: UI Specification

#### FR-9.1: Tree View Component
- **Canvas**: Large scrollable/zoomable area rendering the full tree.
- **Zoom Levels**: 10% (overview) to 200% (detail). Default: 50%.
- **Performance**: Virtualized rendering; only visible nodes are rendered.
- **Background**: Thematic artwork per region (strength = red/orange, dexterity = green, etc.).

#### FR-9.2: Node Rendering
| State | Visual |
|-------|--------|
| Unallocated, unreachable | Greyed out, low opacity |
| Unallocated, reachable | Full color, glowing border |
| Allocated | Filled/lit, connection lines glow |
| Hovered | Enlarged, tooltip visible |
| Search match | Pulsing highlight |

#### FR-9.3: Connection Rendering
- Lines between connected nodes.
- Allocated connections: bright, glowing.
- Unallocated connections: dim, dashed.
- Path preview: highlighted in distinct color when showing path to hovered node.

#### FR-9.4: Tooltip Content
**For Unallocated Nodes:**
```
[Node Name] (Notable)
━━━━━━━━━━━━━━━━━━━━━
[Description]

Effects:
  • +15% Physical Damage
  • -5% Attack Speed

If Allocated:
  Physical Damage: 1250% → 1265% (+15%)
  Attack Speed: 145% → 140% (-5%)

Path Cost: 3 points
Available: 12 points
```

**For Allocated Nodes:**
```
[Node Name] (Notable) ✓
━━━━━━━━━━━━━━━━━━━━━
[Description]

Current Contribution:
  • +15% Physical Damage
  • -5% Attack Speed

Refund Cost: 110 Tradebars
[Right-click to refund]
```

#### FR-9.5: Search & Filter
- Search bar at top of tree view.
- Filters by: node name, stat name, effect type, region.
- Matching nodes are highlighted; camera can auto-focus on results.
- Filter presets: "Keystones", "Life nodes", "Damage nodes", etc.

#### FR-9.6: Path Highlighting
- When hovering an unreached node, calculate and display shortest path.
- Path shown with distinct color and point cost annotation.
- Multiple paths can be compared (Shift+hover for alternate path).

#### FR-9.7: Allocation Panel
Persistent side panel showing:
- Tree name (General / Class: Warrior)
- Available points / Total allocated / Maximum
- Book points used (for General Tree)
- Quick actions: Full Respec, Undo Last, Search

#### FR-9.8: Respec Confirmation Dialog
```
┌─────────────────────────────────────┐
│         Full Respec                 │
├─────────────────────────────────────┤
│ Refunding 45 nodes                  │
│                                     │
│ Total Cost: 4,950 Tradebars         │
│ Your Tradebars: 12,340              │
│                                     │
│ Points Returned: 45                 │
│                                     │
│   [Cancel]        [Confirm Respec]  │
└─────────────────────────────────────┘
```

#### FR-9.9: Class Tree Selector
- Tab bar or dropdown to switch between General Tree and Class Trees.
- Only shows classes the player has leveled.
- Badge showing available points per tree.

### FR-10: Extensibility API

#### FR-10.1: Tree Registration
Other plugins can register additional trees:
```java
PassiveTreeRegistry.register(customTree);
```

#### FR-10.2: Node Injection
Add nodes to existing trees:
```java
PassiveTreeRegistry.addNode("hyforged:passive-tree-general", customNode);
PassiveTreeRegistry.addConnection("hyforged:passive-tree-general", "existing-node", "custom-node");
```

#### FR-10.3: Custom Effect Types
Register new effect handlers:
```java
PassiveEffectRegistry.register("custom-effect-type", CustomEffectHandler.class);
```

Effect handler interface:
```java
public interface PassiveEffectHandler {
    void apply(EntityRef entity, JsonObject effectData);
    void remove(EntityRef entity, JsonObject effectData);
    String getTooltipText(JsonObject effectData);
}
```

## Non-Functional Requirements

### NFR-1: Performance
- Tree loading: < 500ms for 1000+ node tree.
- Node allocation: < 10ms including stat recalculation.
- UI rendering: 60 FPS during zoom/pan with full tree visible.
- Path finding: < 5ms for any node pair.

### NFR-2: Scalability
- Support trees up to 2000 nodes without degradation.
- Support up to 500 allocated nodes per tree per player.
- Efficient storage: allocations stored as node ID lists, not full snapshots.

### NFR-3: Extensibility
- All tree/node definitions are JSON, loadable from any mod's `Server/<ModName>/PassiveTrees/`.
- Effect types are registered via codec pattern (like `HyforgedModifier`).
- No hardcoded node IDs in core systems.

### NFR-4: Observability
- Log allocations and refunds with player UUID, node ID, and timestamp.
- Log Point Book consumption.
- Admin command to dump player's full allocation state.
- Debug mode to visualize graph connectivity.

## Dependencies
- **Entity Stats** (FR-7): Modifiers applied to `HyforgedStatComponent`.
- **Progression Systems**: Character level provides general points; class level provides class points.
- **Currency System (Tradebars)**: Refunds consume Tradebars.
- **Items System**: Point Books are legendary items with use interaction.
- **Hytale ECS**: Player components for storing allocations.
- **Hytale UI**: Framework for tree visualization (custom canvas component).
- **Hytale Assets**: `AssetRegistry` for loading tree definitions.

## Data/Schema Impact
- New asset type: `PassiveTreeAsset` at `Server/Hyforged/PassiveTrees/`.
- New asset type: `PassiveRefundConfigAsset` at `Server/Hyforged/Config/`.
- New item definition: `hyforged:point-book-general` in `Server/Hyforged/Items/`.
- New ECS component: `PassiveTreeComponent` for storing player allocations.
- New ECS component: `PlayerUnlocksComponent` for unlock flags.
- Extend `PlayerSpellsComponent` (or create) for spell grants.

## API Changes
- New `PassiveTreeService` for allocation operations.
- New `PassiveTreeRegistry` for tree/node registration.
- New `PassiveEffectRegistry` for custom effect types.
- New events: `PassiveNodeAllocatedEvent`, `PassiveNodeRefundedEvent`, `PassiveTreeRespecEvent`, `PointBookConsumedEvent`.

## Security/Privacy
- All allocation changes are server-authoritative.
- Client requests validated: point availability, connectivity, Tradebar balance.
- Rate-limit allocation requests to prevent spam (max 10/second).
- Point Book consumption validated server-side.

## Observability
- Log all allocation/refund operations with player ID and details.
- Metrics: allocations per tree, refunds per day, Point Books consumed.
- Admin commands:
  - `/passive list <player>` — show allocations
  - `/passive grant-point <player> [tree]` — grant a point
  - `/passive reset <player> [tree]` — free reset (admin)
  - `/passive debug <player>` — dump full state

## Risks
| Risk | Mitigation |
|------|------------|
| Tree balance issues at PoE scale | Iterative balancing; all values in JSON for quick tuning |
| UI performance with 1000+ nodes | Virtualized rendering; LOD for zoomed-out view |
| Migration complexity | Version field; automatic refund on breaking changes |
| Orphan detection edge cases | Comprehensive graph algorithms; unit test coverage |
| Point Book duplication exploits | Server-authoritative consumption; atomic transactions |

## Open Questions
- None (all clarifications addressed).

## Acceptance Criteria
- [ ] General Passive Tree loads with 1000+ nodes and multiple starting regions.
- [ ] Class Passive Trees load per class definition.
- [ ] Players can select a starting region on first allocation (General Tree).
- [ ] Points are correctly calculated from character level and class level.
- [ ] Point Books grant +1 general point up to maximum 20.
- [ ] Nodes can be allocated when adjacent to existing allocations.
- [ ] Node effects (stat modifiers, spell grants, unlock flags) apply correctly.
- [ ] Keystone allocation shows confirmation with full effects.
- [ ] Per-node refund costs Tradebars scaled by character level.
- [ ] Full respec refunds all nodes with correct total cost.
- [ ] Refund prevents orphaning unless also refunding orphans.
- [ ] UI renders tree with zoom/pan at 60 FPS.
- [ ] UI shows path highlighting with point cost to hovered node.
- [ ] Tooltips show current stats and projected changes.
- [ ] Search filters and highlights matching nodes.
- [ ] Allocations persist and restore on login.
- [ ] Tree migrations refund affected nodes automatically.
- [ ] Other plugins can register trees, nodes, and effect types.

## Impacted Areas (High-Level)
- Player progression state storage
- Stats system (modifier application)
- Items system (Point Book item)
- Currency system (Tradebar consumption)
- UI framework (new tree view component)
- ECS components (allocations, unlocks, spells)

## Required Codebase/Architecture Changes (High-Level)
- Add `PassiveTreeAsset` and asset loader for tree definitions.
- Add `PassiveTreeComponent` ECS component for player allocations.
- Add `PlayerUnlocksComponent` for unlock flags.
- Implement `PassiveTreeService` with allocation, refund, and path-finding logic.
- Integrate with `HyforgedStatComponent` for modifier application.
- Create Point Book item with use interaction.
- Implement custom UI canvas for tree rendering.
- Add admin commands for passive tree management.

## References
- Requirements: [.memory_bank/Requirements/rpg-arpg/passive-trees.md](../../Requirements/rpg-arpg/passive-trees.md)
- Related specs:
  - [Entity Stats](../entity-stats/entity-stats.spec.md)
  - [Progression Systems](../progression-systems/progression-systems.spec.md)
- Currency: [.memory_bank/Requirements/rpg-arpg/currency-tradebars.md](../../Requirements/rpg-arpg/currency-tradebars.md)
