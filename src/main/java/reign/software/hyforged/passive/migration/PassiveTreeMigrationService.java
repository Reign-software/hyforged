package reign.software.hyforged.passive.migration;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.passive.service.RefundResult;

import javax.annotation.Nonnull;
import java.util.*;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Handles migration of passive tree allocations when tree definitions change.
 * <p>
 * When a tree's version changes (nodes added, removed, or restructured),
 * this service detects affected allocations and auto-refunds them without cost.
 */
public class PassiveTreeMigrationService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
            @Nonnull List<Message> messages
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
        List<Message> messages = new ArrayList<>();
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

        // Check class trees based on actual allocations (not version map), so legacy
        // saves without TreeVersions still get validated and migrated.
        for (String classId : passiveComponent.getClassIdsWithAllocations()) {
            if (passiveComponent.getClassAllocatedCount(classId) <= 0) {
                continue;
            }

            PassiveTree classTree = PassiveTreeRegistry.get().getClassTree(classId);
            if (classTree == null) {
                continue;
            }

            MigrationResult classResult = migrateTree(entityRef, passiveComponent, classTree);
            if (classResult.migrationsPerformed()) {
                treesMigrated.add(classTree.getId());
                totalNodesRefunded += classResult.totalNodesRefunded();
                messages.addAll(classResult.messages());
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

        // Find allocated nodes that no longer exist in the current tree definition.
        // This handles stale IDs from legacy layouts even when versions match.
        Set<String> allocatedNodes = tree.isGeneralTree()
                ? passiveComponent.getGeneralAllocatedNodes()
                : (tree.getClassId() != null
                    ? passiveComponent.getClassAllocatedNodes(tree.getClassId())
                    : Collections.emptySet());

        Set<String> removedNodes = new HashSet<>();
        for (String nodeId : allocatedNodes) {
            if (tree.getNode(nodeId) == null) {
                removedNodes.add(nodeId);
            }
        }

        // If nothing to refund, just keep version tracking up to date.
        if (removedNodes.isEmpty()) {
            if (storedVersion != currentVersion) {
                LOGGER.atInfo().log("Updating tree version for %s: %s -> %s", treeId, storedVersion, currentVersion);
                passiveComponent.setTreeVersion(treeId, currentVersion);
            }
            return MigrationResult.noMigration();
        }

        LOGGER.atInfo().log(
                "Migrating tree %s (version %s -> %s), refunding %s invalid nodes",
                treeId,
                storedVersion,
                currentVersion,
                removedNodes.size()
        );

        // Refund removed nodes without cost
        RefundResult refundResult = PassiveTreeService.get().refundNodesFree(entityRef, treeId, removedNodes);

        // Update version
        passiveComponent.setTreeVersion(treeId, currentVersion);

        List<Message> messages = new ArrayList<>();
        if (refundResult.success()) {
            if (tree.isGeneralTree()) {
                messages.add(
                        Message.translation("hyforged.passive.treeMigration.refund.general")
                                .param("points", refundResult.pointsReturned())
                );
            } else {
                String classId = tree.getClassId();
                if (classId == null || classId.isBlank()) {
                    messages.add(
                            Message.translation("hyforged.passive.treeMigration.refund.classUnknown")
                                    .param("points", refundResult.pointsReturned())
                    );
                } else {
                    messages.add(
                            Message.translation("hyforged.passive.treeMigration.refund.class")
                                    .param("classId", classId)
                                    .param("points", refundResult.pointsReturned())
                    );
                }
            }
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
