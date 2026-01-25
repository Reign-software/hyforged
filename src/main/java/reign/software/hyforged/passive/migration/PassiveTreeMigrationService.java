package reign.software.hyforged.passive.migration;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.passive.service.RefundResult;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles migration of passive tree allocations when tree definitions change.
 * <p>
 * When a tree's version changes (nodes added, removed, or restructured),
 * this service detects affected allocations and auto-refunds them without cost.
 */
public class PassiveTreeMigrationService {

    private static final Logger LOGGER = Logger.getLogger(PassiveTreeMigrationService.class.getName());

    private static PassiveTreeMigrationService instance;

    private PassiveTreeMigrationService() {
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static synchronized PassiveTreeMigrationService get() {
        if (instance == null) {
            instance = new PassiveTreeMigrationService();
        }
        return instance;
    }

    /**
     * Result of migration check for a player.
     *
     * @param migrationsPerformed Whether any migrations were performed
     * @param treesMigrated List of tree IDs that were migrated
     * @param totalNodesRefunded Total number of nodes refunded across all trees
     * @param messages User-friendly messages about what was migrated
     */
    public record MigrationResult(
            boolean migrationsPerformed,
            @Nonnull List<String> treesMigrated,
            int totalNodesRefunded,
            @Nonnull List<String> messages
    ) {
        public static MigrationResult noMigration() {
            return new MigrationResult(false, List.of(), 0, List.of());
        }
    }

    /**
     * Check and perform migrations for a player.
     * Called on player join/load.
     *
     * @param entityRef The player entity reference
     * @param passiveComponent The player's passive tree component
     * @return Migration result
     */
    @Nonnull
    public MigrationResult checkAndMigrate(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull PassiveTreeComponent passiveComponent
    ) {
        List<String> treesMigrated = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int totalNodesRefunded = 0;

        // Check general tree
        PassiveTree generalTree = PassiveTreeRegistry.get().getGeneralTree();
        if (generalTree != null && passiveComponent.getGeneralAllocatedCount() > 0) {
            MigrationResult generalResult = migrateTree(entityRef, passiveComponent, generalTree);
            if (generalResult.migrationsPerformed()) {
                treesMigrated.add(generalTree.getId());
                totalNodesRefunded += generalResult.totalNodesRefunded();
                messages.addAll(generalResult.messages());
            }
        }

        // Check class trees
        for (Map.Entry<String, Integer> entry : passiveComponent.getAllTreeVersions().entrySet()) {
            String treeId = entry.getKey();
            if (treeId.equals(generalTree != null ? generalTree.getId() : null)) {
                continue; // Already checked general tree
            }

            PassiveTree classTree = PassiveTreeRegistry.get().getTree(treeId);
            if (classTree != null) {
                String classId = classTree.getClassId();
                if (classId != null && passiveComponent.getClassAllocatedCount(classId) > 0) {
                    MigrationResult classResult = migrateTree(entityRef, passiveComponent, classTree);
                    if (classResult.migrationsPerformed()) {
                        treesMigrated.add(treeId);
                        totalNodesRefunded += classResult.totalNodesRefunded();
                        messages.addAll(classResult.messages());
                    }
                }
            }
        }

        if (treesMigrated.isEmpty()) {
            return MigrationResult.noMigration();
        }

        return new MigrationResult(true, treesMigrated, totalNodesRefunded, messages);
    }

    /**
     * Migrate a single tree for a player.
     */
    private MigrationResult migrateTree(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull PassiveTreeComponent passiveComponent,
            @Nonnull PassiveTree tree
    ) {
        String treeId = tree.getId();
        int storedVersion = passiveComponent.getTreeVersion(treeId);
        int currentVersion = tree.getVersion();

        // No migration needed if versions match
        if (storedVersion == currentVersion) {
            return MigrationResult.noMigration();
        }

        // If no stored version, this is first load - just update version
        if (storedVersion == 0) {
            passiveComponent.setTreeVersion(treeId, currentVersion);
            return MigrationResult.noMigration();
        }

        LOGGER.info("Migrating tree " + treeId + " from version " + storedVersion + " to " + currentVersion);

        // Find nodes that no longer exist
        Set<String> allocatedNodes = tree.isGeneralTree()
                ? passiveComponent.getGeneralAllocatedNodes()
                : passiveComponent.getClassAllocatedNodes(tree.getClassId());

        Set<String> removedNodes = new HashSet<>();
        for (String nodeId : allocatedNodes) {
            if (tree.getNode(nodeId) == null) {
                removedNodes.add(nodeId);
            }
        }

        if (removedNodes.isEmpty()) {
            // No nodes removed, just update version
            passiveComponent.setTreeVersion(treeId, currentVersion);
            return MigrationResult.noMigration();
        }

        // Refund removed nodes without cost
        RefundResult refundResult = PassiveTreeService.get().refundNodesFree(entityRef, treeId, removedNodes);

        // Update version
        passiveComponent.setTreeVersion(treeId, currentVersion);

        List<String> messages = new ArrayList<>();
        if (refundResult.success()) {
            String treeName = tree.isGeneralTree() ? "General Tree" : tree.getClassId() + " Class Tree";
            messages.add("The " + treeName + " has been updated. " + 
                    refundResult.pointsReturned() + " passive points have been refunded.");
        }

        return new MigrationResult(
                true,
                List.of(treeId),
                refundResult.pointsReturned(),
                messages
        );
    }

    /**
     * Update tree version after allocation (called after successful allocation).
     *
     * @param passiveComponent The player's passive component
     * @param tree The tree that was allocated to
     */
    public void updateTreeVersion(
            @Nonnull PassiveTreeComponent passiveComponent,
            @Nonnull PassiveTree tree
    ) {
        passiveComponent.setTreeVersion(tree.getId(), tree.getVersion());
    }
}
