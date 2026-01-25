# Passive Trees API Reference

This document provides code examples for working with the Hyforged Passive Trees system.

## Table of Contents

- [Service Access](#service-access)
- [Tree Queries](#tree-queries)
- [Node Allocation](#node-allocation)
- [Refund Operations](#refund-operations)
- [Events](#events)
- [Custom Effect Handlers](#custom-effect-handlers)
- [Point Books](#point-books)

---

## Service Access

### Get the Service

```java
PassiveTreeService service = PassiveTreeService.get();
```

### Get the Registry

```java
PassiveTreeRegistry registry = PassiveTreeRegistry.get();
```

---

## Tree Queries

### Get Trees

```java
// Get the general tree
PassiveTree generalTree = service.getGeneralTree();

// Get a class tree
PassiveTree classTree = service.getClassTree("hyforged:warrior");

// Get any tree by ID
PassiveTree tree = service.getTree("hyforged:passive-tree-general");
```

### Query Allocated Nodes

```java
// Get all allocated nodes for a player in a tree
Set<String> allocated = service.getAllocatedNodes(entityRef, treeId);

// Get available points
int points = service.getAvailablePoints(entityRef, treeId);
int generalPoints = service.getAvailableGeneralPoints(entityRef);
int classPoints = service.getAvailableClassPoints(entityRef, "hyforged:warrior");
```

### Query Reachable Nodes

```java
// Get nodes the player can allocate next
Set<String> reachable = service.getReachableNodes(entityRef, treeId);

// Find path to a distant node
List<String> path = service.findPathToNode(entityRef, treeId, "hyforged:keystone-node");
```

---

## Node Allocation

### Allocate Single Node

```java
AllocationResult result = service.allocateNode(entityRef, treeId, nodeId);

if (result.success()) {
    // Node allocated successfully
} else {
    // Check failure reason
    switch (result.reason()) {
        case AllocationResult.ALREADY_ALLOCATED -> { /* ... */ }
        case AllocationResult.NOT_ADJACENT -> { /* ... */ }
        case AllocationResult.INSUFFICIENT_POINTS -> { /* ... */ }
        case AllocationResult.REQUIREMENTS_NOT_MET -> { /* ... */ }
        case AllocationResult.KEYSTONE_CONFLICT -> { /* ... */ }
    }
}
```

### Allocate Path

Auto-allocate all nodes in the path to a target node:

```java
AllocationResult result = service.allocatePath(entityRef, treeId, targetNodeId);

if (result.success()) {
    // All nodes in path allocated
} else {
    // Path allocation failed (insufficient points, blocked, etc.)
}
```

### Check Before Allocating

```java
boolean canAllocate = service.canAllocate(entityRef, treeId, nodeId);
```

---

## Refund Operations

### Calculate Refund Cost

```java
// Cost to refund a single node
int cost = service.calculateRefundCost(entityRef, nodeId);

// Cost to refund multiple nodes
int totalCost = service.calculateTotalRefundCost(entityRef, List.of(node1, node2, node3));
```

### Refund Single Node

```java
RefundResult result = service.refundNode(entityRef, treeId, nodeId);

if (result.success()) {
    int pointsRefunded = result.pointsRefunded();
    int currencySpent = result.currencySpent();
} else {
    // Check failure reason
    switch (result.reason()) {
        case RefundResult.NOT_ALLOCATED -> { /* ... */ }
        case RefundResult.WOULD_ORPHAN -> { /* ... */ }
        case RefundResult.INSUFFICIENT_CURRENCY -> { /* ... */ }
    }
}
```

### Full Respec

```java
// Refund all nodes in a tree (costs currency)
RefundResult result = service.refundAll(entityRef, treeId);

// Free respec (admin/special circumstances)
RefundResult result = service.refundAllFree(entityRef, treeId);
```

### Check for Orphans

```java
// Get nodes that would be orphaned if a node is refunded
Set<String> orphaned = service.getOrphanedNodes(entityRef, treeId, nodeId);
```

---

## Events

### Node Allocated Event

```java
EventRegistry.register(PassiveNodeAllocatedEvent.class, EventPriority.NORMAL, event -> {
    Ref<EntityStore> entityRef = event.getEntityRef();
    String treeId = event.getTreeId();
    String nodeId = event.getNodeId();
    PassiveNode node = event.getNode();
    
    // React to allocation
});
```

### Node Refunded Event

```java
EventRegistry.register(PassiveNodeRefundedEvent.class, EventPriority.NORMAL, event -> {
    Ref<EntityStore> entityRef = event.getEntityRef();
    String treeId = event.getTreeId();
    String nodeId = event.getNodeId();
    int currencySpent = event.getCurrencySpent();
    
    // React to refund
});
```

### Tree Respec Event

```java
EventRegistry.register(PassiveTreeRespecEvent.class, EventPriority.NORMAL, event -> {
    Ref<EntityStore> entityRef = event.getEntityRef();
    String treeId = event.getTreeId();
    int nodesRefunded = event.getNodesRefunded();
    int totalCost = event.getTotalCost();
    
    // React to full respec
});
```

### Point Book Consumed Event

```java
EventRegistry.register(PointBookConsumedEvent.class, EventPriority.NORMAL, event -> {
    Ref<EntityStore> entityRef = event.getEntityRef();
    int pointsGranted = event.getPointsGranted();
    int totalBookPoints = event.getTotalBookPoints();
    
    // React to point book use
});
```

---

## Custom Effect Handlers

### Implement PassiveEffectHandler

```java
public class MyCustomEffectHandler implements PassiveEffectHandler {
    
    @Override
    public void apply(Ref<EntityStore> entityRef, PassiveNode node, PassiveNodeEffect effect) {
        // Extract data from effect
        String customData = effect.data().getString("CustomField");
        
        // Apply your effect
        // ...
    }
    
    @Override
    public void remove(Ref<EntityStore> entityRef, PassiveNode node, PassiveNodeEffect effect) {
        // Remove your effect
        // ...
    }
    
    @Override
    public String getTooltipText(PassiveNodeEffect effect) {
        return "Custom effect description";
    }
}
```

### Register Your Handler

```java
@Override
public void setup(PluginHandle handle) {
    // Register during plugin setup
    PassiveEffectRegistry.get().register("my-custom-effect", new MyCustomEffectHandler());
}
```

### Use in JSON

```json
{
    "Id": "yourmod:custom-node",
    "Type": "notable",
    "Name": "Custom Node",
    "Description": "Grants a custom effect.",
    "Position": { "X": 0, "Y": 0 },
    "Effects": [
        { 
            "Type": "my-custom-effect",
            "CustomField": "custom-value"
        }
    ]
}
```

---

## Point Books

### Create Point Book Item

Define the item in `Server/<YourMod>/Item/point-book.json`:

```json
{
    "Id": "yourmod:skill-point-book",
    "DisplayName": "Book of Skill Points",
    "Description": "Grants 1 passive skill point when consumed.",
    "MaxStackSize": 99,
    "Interactions": [
        {
            "Type": "hyforged:point-book",
            "PointsGranted": 1
        }
    ]
}
```

### Programmatic Point Grant

```java
PassiveTreeComponent component = entityRef.get(passiveTreeComponentType);
if (component != null) {
    component.addBookPoints(1);
}
```

---

## Graph Utilities

### PassiveTreeGraph Static Methods

```java
// Find shortest path from allocated nodes to a target
List<String> path = PassiveTreeGraph.findShortestPath(tree, allocatedNodes, targetNodeId);

// Check if allocations remain connected to start
boolean connected = PassiveTreeGraph.isConnectedToStart(tree, allocatedNodes, startNodeId);

// Get nodes reachable from start through allocated nodes
Set<String> reachable = PassiveTreeGraph.getReachableFromStart(tree, allocatedNodes, startNodeId);

// Get nodes that would be orphaned if a node is removed
Set<String> orphaned = PassiveTreeGraph.getOrphanedNodes(tree, allocatedNodes, startNodeId, nodeToRemove);

// Get unallocated nodes adjacent to allocated nodes
Set<String> available = PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocatedNodes);

// Validation helpers
boolean canAlloc = PassiveTreeGraph.canAllocateNode(tree, allocatedNodes, nodeId);
boolean canDealloc = PassiveTreeGraph.canDeallocateNode(tree, allocatedNodes, startNodeId, nodeId);

// Get allocation order for a path
List<String> order = PassiveTreeGraph.getPathAllocationOrder(tree, allocatedNodes, path);
```

---

## Component Access

### PassiveTreeComponent

```java
PassiveTreeComponent component = entityRef.get(passiveTreeComponentType);

// General tree allocations
Set<String> generalNodes = component.getGeneralAllocatedNodes();
int generalCount = component.getGeneralAllocatedCount();

// Class tree allocations
Set<String> classNodes = component.getClassAllocatedNodes("hyforged:warrior");
int classCount = component.getClassAllocatedCount("hyforged:warrior");

// Book points
int bookPoints = component.getBookPointsUsed();
component.addBookPoints(1);
```

### PlayerUnlocksComponent

```java
PlayerUnlocksComponent unlocks = entityRef.get(playerUnlocksComponentType);

// Check if a flag is unlocked
boolean hasFlag = unlocks.hasFlag("hyforged:stun-immune");

// Get all flags from a source node
Set<String> nodeFlags = unlocks.getFlagsFromSource("hyforged:keystone-node");
```

### PlayerSpellsComponent

```java
PlayerSpellsComponent spells = entityRef.get(playerSpellsComponentType);

// Check if a spell is granted
boolean hasSpell = spells.hasSpell("hyforged:fireball");

// Get all spells from a source node
Set<String> nodeSpells = spells.getSpellsFromSource("hyforged:spell-unlock-node");
```
