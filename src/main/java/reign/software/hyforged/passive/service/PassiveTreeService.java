package reign.software.hyforged.passive.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.currency.service.CurrencyService;
import reign.software.hyforged.currency.service.TransactionResult;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.component.PlayerSpellsComponent;
import reign.software.hyforged.passive.component.PlayerUnlocksComponent;
import reign.software.hyforged.passive.effect.PassiveEffectHandler;
import reign.software.hyforged.passive.effect.PassiveEffectRegistry;
import reign.software.hyforged.passive.event.PassiveNodeAllocatedEvent;
import reign.software.hyforged.passive.event.PassiveNodeRefundedEvent;
import reign.software.hyforged.passive.event.PassiveTreeRespecEvent;
import reign.software.hyforged.passive.graph.PassiveTreeGraph;
import reign.software.hyforged.passive.model.*;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.progression.component.ProgressionComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Service for passive tree operations.
 * <p>
 * Provides methods for querying and modifying passive tree allocations.
 * This service acts as the main entry point for passive tree operations.
 * <p>
 * Point calculation:
 * - General tree: (characterLevel - 1) + bookPointsUsed - generalAllocatedNodes.size()
 * - Class tree: classLevel - classAllocatedNodes.size()
 */
public final class PassiveTreeService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static PassiveTreeService instance;

    // Component types (set during initialization)
    private ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType;
    private ComponentType<EntityStore, PlayerUnlocksComponent> playerUnlocksComponentType;
    private ComponentType<EntityStore, PlayerSpellsComponent> playerSpellsComponentType;
    private ComponentType<EntityStore, ProgressionComponent> progressionComponentType;

    private PassiveTreeService() {
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized PassiveTreeService get() {
        if (instance == null) {
            instance = new PassiveTreeService();
        }
        return instance;
    }

    /**
     * Reset the service (for testing).
     */
    public static synchronized void reset() {
        instance = new PassiveTreeService();
    }

    /**
     * Initialize the service with component types.
     * Must be called during plugin setup after component registration.
     */
    public void initialize(
            @Nonnull ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType,
            @Nonnull ComponentType<EntityStore, PlayerUnlocksComponent> playerUnlocksComponentType,
            @Nonnull ComponentType<EntityStore, PlayerSpellsComponent> playerSpellsComponentType,
            @Nonnull ComponentType<EntityStore, ProgressionComponent> progressionComponentType
    ) {
        this.passiveTreeComponentType = passiveTreeComponentType;
        this.playerUnlocksComponentType = playerUnlocksComponentType;
        this.playerSpellsComponentType = playerSpellsComponentType;
        this.progressionComponentType = progressionComponentType;
    }

    // ========== TREE QUERIES ==========

    /**
     * Get the general passive tree.
     *
     * @return The general tree, or null if not loaded
     */
    @Nullable
    public PassiveTree getGeneralTree() {
        return PassiveTreeRegistry.get().getGeneralTree();
    }

    /**
     * Get a class passive tree.
     *
     * @param classId The class ID
     * @return The class tree, or null if not found
     */
    @Nullable
    public PassiveTree getClassTree(@Nonnull String classId) {
        return PassiveTreeRegistry.get().getClassTree(classId);
    }

    /**
     * Get a passive tree by ID.
     *
     * @param treeId The tree ID
     * @return The tree, or null if not found
     */
    @Nullable
    public PassiveTree getTree(@Nonnull String treeId) {
        return PassiveTreeRegistry.get().getTree(treeId);
    }

    // ========== ALLOCATION QUERIES ==========

    /**
     * Get the allocated nodes for a player in a tree.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @return Set of allocated node IDs
     */
    @Nonnull
    public Set<String> getAllocatedNodes(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId) {
        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return Collections.emptySet();
        }

        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return Collections.emptySet();
        }

        if (tree.isGeneralTree()) {
            return component.getGeneralAllocatedNodes();
        } else if (tree.isClassTree() && tree.getClassId() != null) {
            return component.getClassAllocatedNodes(tree.getClassId());
        }

        return Collections.emptySet();
    }

    /**
     * Get available passive points for a player in a tree.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @return Number of available points
     */
    public int getAvailablePoints(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return 0;
        }

        if (tree.isGeneralTree()) {
            return getAvailableGeneralPoints(entityRef);
        } else if (tree.isClassTree() && tree.getClassId() != null) {
            return getAvailableClassPoints(entityRef, tree.getClassId());
        }

        return 0;
    }

    /**
     * Get available general passive points.
     * Formula: characterLevel + bookPointsUsed - allocatedCount
     * Players start with 1 point at level 1.
     */
    public int getAvailableGeneralPoints(@Nonnull Ref<EntityStore> entityRef) {
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(entityRef);
        ProgressionComponent progressionComponent = getProgressionComponent(entityRef);

        // If no passive component, return 1 point (default for new players)
        if (passiveComponent == null) {
            return 1;
        }

        // Default to level 1 if no progression component
        int characterLevel = progressionComponent != null ? progressionComponent.getCharacterLevel() : 1;
        int bookPoints = passiveComponent.getBookPointsUsed();
        int allocated = passiveComponent.getGeneralAllocatedCount();

        // characterLevel + bookPointsUsed - allocated (1 point at level 1)
        return Math.max(0, characterLevel + bookPoints - allocated);
    }

    /**
     * Get available class passive points.
     * Formula: classLevel - allocatedCount
     */
    public int getAvailableClassPoints(@Nonnull Ref<EntityStore> entityRef, @Nonnull String classId) {
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(entityRef);
        ProgressionComponent progressionComponent = getProgressionComponent(entityRef);

        if (passiveComponent == null || progressionComponent == null) {
            return 0;
        }

        // Get class level from class progression data
        ProgressionComponent.ClassProgressionData classData = progressionComponent.getClassProgression(classId);
        int classLevel = classData != null ? classData.level : 0;
        int allocated = passiveComponent.getClassAllocatedCount(classId);

        return Math.max(0, classLevel - allocated);
    }

    // ========== POINT BOOK OPERATIONS ==========

    /**
     * Get the number of book points a player has used.
     *
     * @param entityRef The player entity reference
     * @return The number of book points used
     */
    public int getBookPointsUsed(@Nonnull Ref<EntityStore> entityRef) {
        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        return component != null ? component.getBookPointsUsed() : 0;
    }

    /**
     * Consume a point book, granting +1 general passive tree point.
     * Does not consume any item - use PointBookInteraction for item consumption.
     *
     * @param entityRef The player entity reference
     * @return true if successful, false if at max book points
     */
    public boolean consumePointBook(@Nonnull Ref<EntityStore> entityRef) {
        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return false;
        }

        int maxBookPoints = PassiveTreeRegistry.get().getRefundConfig().getMaxBookPoints();
        int currentBookPoints = component.getBookPointsUsed();

        if (currentBookPoints >= maxBookPoints) {
            return false;
        }

        component.addBookPoint();
        return true;
    }

    /**
     * Get the maximum book points allowed.
     *
     * @return The maximum book points
     */
    public int getMaxBookPoints() {
        return PassiveTreeRegistry.get().getRefundConfig().getMaxBookPoints();
    }

    // ========== ALLOCATION OPERATIONS ==========

    /**
     * Check if a node can be allocated.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param nodeId The node ID to check
     * @return true if the node can be allocated
     */
    public boolean canAllocate(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull String nodeId) {
        AllocationResult result = validateAllocation(entityRef, treeId, nodeId, false);
        return result.success();
    }

    /**
     * Allocate a node.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param nodeId The node ID to allocate
     * @return Allocation result
     */
    @Nonnull
    public AllocationResult allocateNode(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull String nodeId) {
        // Validate allocation
        AllocationResult validation = validateAllocation(entityRef, treeId, nodeId, true);
        if (!validation.success()) {
            return validation;
        }

        PassiveTree tree = getTree(treeId);
        PassiveNode node = tree.getNode(nodeId);
        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);

        // Perform allocation
        if (tree.isGeneralTree()) {
            component.allocateGeneralNode(nodeId);
            // Set as starting node if it's a starting node and none is set yet
            if (tree.isStartingNode(nodeId) && component.getGeneralStartingNode() == null) {
                component.setGeneralStartingNode(nodeId);
            }
        } else if (tree.isClassTree() && tree.getClassId() != null) {
            component.allocateClassNode(tree.getClassId(), nodeId);
        }

        // Apply effects
        applyNodeEffects(entityRef, node);

        // Update tree version tracking
        component.setTreeVersion(treeId, tree.getVersion());

        int remainingPoints = getAvailablePoints(entityRef, treeId);

        // Emit allocation event
        PassiveNodeAllocatedEvent event = new PassiveNodeAllocatedEvent(
                entityRef,
                treeId,
                nodeId,
                node.isKeystone(),
                List.copyOf(node.effects()),
                remainingPoints
        );
        HytaleServer.get().getEventBus()
                .dispatchFor(PassiveNodeAllocatedEvent.class)
                .dispatch(event);

        return AllocationResult.success(nodeId, remainingPoints);
    }

    /**
     * Allocate all nodes along a path to a target node.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param targetNodeId The target node ID
     * @return Allocation result
     */
    @Nonnull
    public AllocationResult allocatePath(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull String targetNodeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return AllocationResult.failure(targetNodeId, AllocationResult.REASON_TREE_NOT_FOUND);
        }

        Set<String> allocated = getAllocatedNodes(entityRef, treeId);
        List<String> path = PassiveTreeGraph.findShortestPath(tree, allocated, targetNodeId);

        if (path.isEmpty()) {
            return AllocationResult.failure(targetNodeId, AllocationResult.REASON_NO_PATH);
        }

        int availablePoints = getAvailablePoints(entityRef, treeId);
        List<String> toAllocate = PassiveTreeGraph.getPathAllocationOrder(tree, allocated, path);

        if (toAllocate.size() > availablePoints) {
            return AllocationResult.failure(targetNodeId, AllocationResult.REASON_INSUFFICIENT_POINTS);
        }

        // Allocate each node in order
        List<String> allocatedNodes = new ArrayList<>();
        for (String nodeId : toAllocate) {
            AllocationResult result = allocateNode(entityRef, treeId, nodeId);
            if (!result.success()) {
                // Partial allocation - return what we did allocate
                return AllocationResult.failure(nodeId, result.reason());
            }
            allocatedNodes.add(nodeId);
        }

        int remainingPoints = getAvailablePoints(entityRef, treeId);
        return AllocationResult.successPath(targetNodeId, allocatedNodes, remainingPoints);
    }

    /**
     * Find the shortest path to a node from current allocations.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param targetNodeId The target node ID
     * @return List of node IDs in the path (empty if no path)
     */
    @Nonnull
    public List<String> findPathToNode(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull String targetNodeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return Collections.emptyList();
        }

        Set<String> allocated = getAllocatedNodes(entityRef, treeId);
        return PassiveTreeGraph.findShortestPath(tree, allocated, targetNodeId);
    }

    /**
     * Get nodes that are reachable (can be allocated next).
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @return Set of reachable node IDs
     */
    @Nonnull
    public Set<String> getReachableNodes(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return Collections.emptySet();
        }

        Set<String> allocated = getAllocatedNodes(entityRef, treeId);
        return PassiveTreeGraph.getReachableUnallocatedNodes(tree, allocated);
    }

    // ========== REFUND OPERATIONS ==========

    /**
     * Calculate the refund cost for a node.
     *
     * @param entityRef The player entity reference
     * @param nodeId The node ID (used for future per-node cost overrides)
     * @return The Tradebar cost to refund this node
     */
    public int calculateRefundCost(@Nonnull Ref<EntityStore> entityRef, @Nonnull String nodeId) {
        ProgressionComponent progressionComponent = getProgressionComponent(entityRef);
        if (progressionComponent == null) {
            return 0;
        }

        int characterLevel = progressionComponent.getCharacterLevel();
        var refundConfig = PassiveTreeRegistry.get().getRefundConfig();
        if (refundConfig == null) {
            // Default formula
            return 10 + (characterLevel * 2);
        }

        return refundConfig.calculateRefundCostPerNode(characterLevel);
    }

    /**
     * Calculate the total refund cost for multiple nodes.
     *
     * @param entityRef The player entity reference
     * @param nodeIds The node IDs to refund
     * @return The total Tradebar cost
     */
    public int calculateTotalRefundCost(@Nonnull Ref<EntityStore> entityRef, @Nonnull Collection<String> nodeIds) {
        int costPerNode = calculateRefundCost(entityRef, "");
        return costPerNode * nodeIds.size();
    }

    /**
     * Get orphaned nodes that would need to be refunded with a target node.
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param nodeId The node to refund
     * @return Set of node IDs that would be orphaned (includes the target node)
     */
    @Nonnull
    public Set<String> getOrphanedNodes(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull String nodeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return Collections.emptySet();
        }

        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return Collections.emptySet();
        }

        Set<String> allocated = getAllocatedNodes(entityRef, treeId);
        String startingNode = tree.isGeneralTree() ? 
            component.getGeneralStartingNode() : 
            tree.getStartingNodeIds().stream().findFirst().orElse(null);

        if (startingNode == null) {
            return Collections.emptySet();
        }

        return PassiveTreeGraph.getOrphanedNodes(tree, allocated, startingNode, nodeId);
    }

    /**
     * Refund a single node (and any orphaned nodes).
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param nodeId The node ID to refund
     * @return Refund result
     */
    @Nonnull
    public RefundResult refundNode(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull String nodeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return RefundResult.failure(RefundResult.REASON_TREE_NOT_FOUND);
        }

        PassiveNode node = tree.getNode(nodeId);
        if (node == null) {
            return RefundResult.failure(RefundResult.REASON_NODE_NOT_FOUND);
        }

        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return RefundResult.failure(RefundResult.REASON_NO_COMPONENT);
        }

        Set<String> allocated = getAllocatedNodes(entityRef, treeId);
        if (!allocated.contains(nodeId)) {
            return RefundResult.failure(RefundResult.REASON_NODE_NOT_ALLOCATED);
        }

        // Get starting node
        String startingNode = tree.isGeneralTree() ? 
            component.getGeneralStartingNode() : 
            tree.getStartingNodeIds().stream().findFirst().orElse(null);

        // Check if trying to refund starting node
        if (nodeId.equals(startingNode) && allocated.size() > 1) {
            return RefundResult.failure(RefundResult.REASON_CANNOT_REFUND_STARTING_NODE);
        }

        // Class trees: prevent refunding auto-allocated starting node
        if (tree.isClassTree() && tree.isStartingNode(nodeId)) {
            return RefundResult.failure(RefundResult.REASON_CLASS_STARTING_NODE);
        }

        // Get all nodes that will be refunded (includes orphans)
        Set<String> nodesToRefund = PassiveTreeGraph.getOrphanedNodes(tree, allocated, startingNode, nodeId);
        
        // Calculate total cost
        int totalCost = calculateTotalRefundCost(entityRef, nodesToRefund);

        // Check Tradebar balance
        if (totalCost > 0) {
            int balance = CurrencyService.get().getBalance(entityRef);
            if (balance < totalCost) {
                LOGGER.at(Level.FINE).log("Refund failed: insufficient Tradebars (%s < %s)", balance, totalCost);
                return RefundResult.failure(RefundResult.REASON_INSUFFICIENT_TRADEBARS);
            }

            // Deduct Tradebars
            TransactionResult txResult = CurrencyService.get().deduct(
                entityRef, 
                totalCost, 
                "passive_refund:" + treeId + ":" + nodeId
            );
            if (!txResult.success()) {
                LOGGER.atWarning().log("Refund failed: Tradebar deduction failed - %s", txResult.failureReason());
                return RefundResult.failure(RefundResult.REASON_INSUFFICIENT_TRADEBARS);
            }
            LOGGER.at(Level.FINE).log("Deducted %s Tradebars for refund (tx: %s)", totalCost, txResult.transactionId());
        }

        // Remove effects and deallocate nodes
        List<String> refundedList = new ArrayList<>(nodesToRefund);
        for (String refundNodeId : refundedList) {
            PassiveNode refundNode = tree.getNode(refundNodeId);
            if (refundNode != null) {
                removeNodeEffects(entityRef, refundNode);
            }

            if (tree.isGeneralTree()) {
                component.deallocateGeneralNode(refundNodeId);
            } else if (tree.isClassTree() && tree.getClassId() != null) {
                component.deallocateClassNode(tree.getClassId(), refundNodeId);
            }
        }

        // Clear starting node if general tree and it was refunded
        if (tree.isGeneralTree() && nodesToRefund.contains(startingNode)) {
            component.setGeneralStartingNode(null);
        }

        int pointsReturned = refundedList.size();
        LOGGER.at(Level.FINE).log("Refunded %s nodes from tree %s for cost %s", pointsReturned, treeId, totalCost);

        // Emit refund event
        PassiveNodeRefundedEvent event = new PassiveNodeRefundedEvent(
                entityRef,
                treeId,
                refundedList,
                totalCost,
                pointsReturned,
                false // not a free refund
        );
        HytaleServer.get().getEventBus()
                .dispatchFor(PassiveNodeRefundedEvent.class)
                .dispatch(event);

        return RefundResult.success(refundedList, totalCost, pointsReturned);
    }

    /**
     * Refund all allocated nodes in a tree (full respec).
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @return Refund result
     */
    @Nonnull
    public RefundResult refundAll(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return RefundResult.failure(RefundResult.REASON_TREE_NOT_FOUND);
        }

        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return RefundResult.failure(RefundResult.REASON_NO_COMPONENT);
        }

        Set<String> allocated = new HashSet<>(getAllocatedNodes(entityRef, treeId));
        if (allocated.isEmpty()) {
            return RefundResult.failure(RefundResult.REASON_NOTHING_TO_REFUND);
        }

        // Calculate total cost
        int totalCost = calculateTotalRefundCost(entityRef, allocated);

        // Check Tradebar balance
        if (totalCost > 0) {
            int balance = CurrencyService.get().getBalance(entityRef);
            if (balance < totalCost) {
                LOGGER.at(Level.FINE).log("Respec failed: insufficient Tradebars (%s < %s)", balance, totalCost);
                return RefundResult.failure(RefundResult.REASON_INSUFFICIENT_TRADEBARS);
            }

            // Deduct Tradebars
            TransactionResult txResult = CurrencyService.get().deduct(
                entityRef, 
                totalCost, 
                "passive_respec:" + treeId
            );
            if (!txResult.success()) {
                LOGGER.atWarning().log("Respec failed: Tradebar deduction failed - %s", txResult.failureReason());
                return RefundResult.failure(RefundResult.REASON_INSUFFICIENT_TRADEBARS);
            }
            LOGGER.at(Level.FINE).log("Deducted %s Tradebars for respec (tx: %s)", totalCost, txResult.transactionId());
        }

        // Remove effects and deallocate all nodes
        List<String> refundedList = new ArrayList<>(allocated);
        for (String nodeId : refundedList) {
            PassiveNode node = tree.getNode(nodeId);
            if (node != null) {
                removeNodeEffects(entityRef, node);
            }

            if (tree.isGeneralTree()) {
                component.deallocateGeneralNode(nodeId);
            } else if (tree.isClassTree() && tree.getClassId() != null) {
                component.deallocateClassNode(tree.getClassId(), nodeId);
            }
        }

        // Clear starting node for general tree
        if (tree.isGeneralTree()) {
            component.setGeneralStartingNode(null);
        }

        int pointsReturned = refundedList.size();
        LOGGER.at(Level.FINE).log("Full respec of tree %s: refunded %s nodes for cost %s", treeId, pointsReturned, totalCost);

        // Emit respec event
        PassiveTreeRespecEvent event = new PassiveTreeRespecEvent(
                entityRef,
                treeId,
                pointsReturned,
                totalCost,
                pointsReturned
        );
        HytaleServer.get().getEventBus()
                .dispatchFor(PassiveTreeRespecEvent.class)
                .dispatch(event);

        return RefundResult.success(refundedList, totalCost, pointsReturned);
    }

    /**
     * Refund all nodes in a tree without cost (for admin/migration).
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @return Refund result
     */
    @Nonnull
    public RefundResult refundAllFree(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId) {
        Set<String> allocated = getAllocatedNodes(entityRef, treeId);
        if (allocated.isEmpty()) {
            return RefundResult.successFree(List.of(), 0);
        }
        
        RefundResult result = refundNodesFree(entityRef, treeId, new ArrayList<>(allocated));
        
        // Also clear starting node for general tree
        PassiveTree tree = getTree(treeId);
        if (tree != null && tree.isGeneralTree()) {
            PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
            if (component != null) {
                component.setGeneralStartingNode(null);
            }
        }
        
        return result;
    }

    /**
     * Refund nodes without cost (for migration/admin).
     *
     * @param entityRef The player entity reference
     * @param treeId The tree ID
     * @param nodeIds Node IDs to refund
     * @return Refund result
     */
    @Nonnull
    public RefundResult refundNodesFree(@Nonnull Ref<EntityStore> entityRef, @Nonnull String treeId, @Nonnull Collection<String> nodeIds) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return RefundResult.failure(RefundResult.REASON_TREE_NOT_FOUND);
        }

        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return RefundResult.failure(RefundResult.REASON_NO_COMPONENT);
        }

        List<String> refundedList = new ArrayList<>();
        for (String nodeId : nodeIds) {
            Set<String> allocated = getAllocatedNodes(entityRef, treeId);
            if (!allocated.contains(nodeId)) {
                continue; // Skip non-allocated nodes
            }

            PassiveNode node = tree.getNode(nodeId);
            if (node != null) {
                removeNodeEffects(entityRef, node);
            }

            if (tree.isGeneralTree()) {
                component.deallocateGeneralNode(nodeId);
            } else if (tree.isClassTree() && tree.getClassId() != null) {
                component.deallocateClassNode(tree.getClassId(), nodeId);
            }

            refundedList.add(nodeId);
        }

        // Clear starting node if it was refunded
        if (tree.isGeneralTree() && component.getGeneralStartingNode() != null && 
            nodeIds.contains(component.getGeneralStartingNode())) {
            component.setGeneralStartingNode(null);
        }

        LOGGER.at(Level.FINE).log("Free refund of %s nodes from tree %s", refundedList.size(), treeId);

        return RefundResult.successFree(refundedList, refundedList.size());
    }

    // ========== VALIDATION ==========

    /**
     * Validate a node allocation.
     */
    private AllocationResult validateAllocation(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull String treeId,
            @Nonnull String nodeId,
            boolean checkPoints
    ) {
        PassiveTree tree = getTree(treeId);
        if (tree == null) {
            return AllocationResult.failure(nodeId, AllocationResult.REASON_TREE_NOT_FOUND);
        }

        PassiveNode node = tree.getNode(nodeId);
        if (node == null) {
            return AllocationResult.failure(nodeId, AllocationResult.REASON_NODE_NOT_FOUND);
        }

        PassiveTreeComponent component = getPassiveTreeComponent(entityRef);
        if (component == null) {
            return AllocationResult.failure(nodeId, "Player has no passive tree component");
        }

        Set<String> allocated = getAllocatedNodes(entityRef, treeId);

        // Check if already allocated
        if (allocated.contains(nodeId)) {
            return AllocationResult.failure(nodeId, AllocationResult.REASON_ALREADY_ALLOCATED);
        }

        // Check connectivity
        if (!PassiveTreeGraph.canAllocateNode(tree, allocated, nodeId)) {
            return AllocationResult.failure(nodeId, AllocationResult.REASON_NOT_CONNECTED);
        }

        // Check points
        if (checkPoints) {
            int availablePoints = getAvailablePoints(entityRef, treeId);
            if (availablePoints < 1) {
                return AllocationResult.failure(nodeId, AllocationResult.REASON_INSUFFICIENT_POINTS);
            }
        }

        // Check keystone conflict
        if (node.isKeystone() && node.keystoneFamily() != null) {
            for (String allocatedId : allocated) {
                PassiveNode allocatedNode = tree.getNode(allocatedId);
                if (allocatedNode != null && allocatedNode.isKeystone() &&
                        node.keystoneFamily().equals(allocatedNode.keystoneFamily())) {
                    return AllocationResult.failure(nodeId, AllocationResult.REASON_KEYSTONE_CONFLICT);
                }
            }
        }

        return AllocationResult.success(nodeId, -1);
    }

    // ========== EFFECTS ==========

    /**
     * Restore effects for all allocated passive nodes on an entity.
     * <p>
     * Called on entity load (login) to re-apply passive modifiers that are
     * not persisted with the stat component. Iterates all allocated nodes
     * in the general tree and all class trees.
     *
     * @param entityRef The entity reference
     * @param passiveComponent The passive tree component with allocation data
     * @return The total number of node effects restored
     */
    public int restoreAllEffects(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull PassiveTreeComponent passiveComponent
    ) {
        int restored = 0;

        // Restore general tree effects
        PassiveTree generalTree = getGeneralTree();
        if (generalTree != null) {
            for (String nodeId : passiveComponent.getGeneralAllocatedNodes()) {
                PassiveNode node = generalTree.getNode(nodeId);
                if (node != null) {
                    applyNodeEffects(entityRef, node);
                    restored++;
                }
            }
        }

        // Restore class tree effects
        for (String classId : passiveComponent.getClassIdsWithAllocations()) {
            PassiveTree classTree = getClassTree(classId);
            if (classTree == null) {
                continue;
            }
            for (String nodeId : passiveComponent.getClassAllocatedNodes(classId)) {
                PassiveNode node = classTree.getNode(nodeId);
                if (node != null) {
                    applyNodeEffects(entityRef, node);
                    restored++;
                }
            }
        }

        return restored;
    }

    /**
     * Apply node effects when allocated.
     */
    private void applyNodeEffects(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node) {
        for (PassiveNodeEffect effect : node.effects()) {
            applyEffect(entityRef, node, effect);
        }
    }

    /**
     * Remove node effects when deallocated.
     */
    private void removeNodeEffects(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node) {
        for (PassiveNodeEffect effect : node.effects()) {
            removeEffect(entityRef, node, effect);
        }
    }

    /**
     * Apply a single effect using the registered handler.
     */
    private void applyEffect(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PassiveEffectHandler handler = PassiveEffectRegistry.get().getHandler(effect.type());
        if (handler != null) {
            handler.apply(entityRef, node, effect);
        } else {
            LOGGER.atWarning().log("No handler registered for effect type: %s on node %s", effect.type(), node.id());
        }
    }

    /**
     * Remove a single effect using the registered handler.
     */
    private void removeEffect(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PassiveEffectHandler handler = PassiveEffectRegistry.get().getHandler(effect.type());
        if (handler != null) {
            handler.remove(entityRef, node, effect);
        }
        // Silent if no handler - may have been removed during updates
    }

    // ========== COMPONENT ACCESS ==========

    @Nullable
    private PassiveTreeComponent getPassiveTreeComponent(@Nonnull Ref<EntityStore> entityRef) {
        if (passiveTreeComponentType == null) {
            return null;
        }
        return entityRef.getStore().getComponent(entityRef, passiveTreeComponentType);
    }

    @Nullable
    private PlayerUnlocksComponent getPlayerUnlocksComponent(@Nonnull Ref<EntityStore> entityRef) {
        if (playerUnlocksComponentType == null) {
            return null;
        }
        return entityRef.getStore().getComponent(entityRef, playerUnlocksComponentType);
    }

    @Nullable
    private PlayerSpellsComponent getPlayerSpellsComponent(@Nonnull Ref<EntityStore> entityRef) {
        if (playerSpellsComponentType == null) {
            return null;
        }
        return entityRef.getStore().getComponent(entityRef, playerSpellsComponentType);
    }

    @Nullable
    private ProgressionComponent getProgressionComponent(@Nonnull Ref<EntityStore> entityRef) {
        if (progressionComponentType == null) {
            return null;
        }
        return entityRef.getStore().getComponent(entityRef, progressionComponentType);
    }
}
