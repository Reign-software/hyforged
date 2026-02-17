package reign.software.hyforged.passive.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.passive.model.PassiveTree;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.passive.service.AllocationResult;
import reign.software.hyforged.passive.service.PassiveTreeService;
import reign.software.hyforged.progression.event.ClassLevelUpEvent;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * System that auto-allocates class tree starting nodes when a class is first leveled.
 * <p>
 * Per spec: "Class trees have a single starting node that is auto-allocated when the class is first leveled."
 * <p>
 * This system listens for {@link ClassLevelUpEvent} and when a player gains their first level
 * in a class (oldLevel == 0), it automatically allocates the class tree starting node.
 */
public class ClassTreeStartingNodeSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EventRegistration<Void, ClassLevelUpEvent> classLevelRegistration;

    public ClassTreeStartingNodeSystem() {
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        classLevelRegistration = HytaleServer.get().getEventBus()
                .register(ClassLevelUpEvent.class, this::onClassLevelUp);
        
        LOGGER.atInfo().log("ClassTreeStartingNodeSystem: Registered class level-up event handler");
    }

    /**
     * Handle class level-up events by auto-allocating the starting node on first level.
     *
     * @param event The class level-up event
     */
    private void onClassLevelUp(@Nonnull ClassLevelUpEvent event) {
        // Only act on first level-up (from level 0 to 1+)
        if (event.oldLevel() != 0) {
            return;
        }

        String classId = event.classId();
        Ref<EntityStore> entityRef = event.entityRef();

        LOGGER.at(Level.FINE).log(
                "ClassTreeStartingNodeSystem: Player gained first level in class %s, auto-allocating starting node",
                classId);

        // Get the class tree
        PassiveTree classTree = PassiveTreeRegistry.get().getClassTree(classId);
        if (classTree == null) {
            LOGGER.atWarning().log(
                    "ClassTreeStartingNodeSystem: No passive tree found for class %s",
                    classId);
            return;
        }

        // Get the starting node(s)
        Set<String> startingNodes = classTree.getStartingNodeIds();
        if (startingNodes.isEmpty()) {
            LOGGER.atWarning().log(
                    "ClassTreeStartingNodeSystem: Class tree %s has no starting nodes defined",
                    classId);
            return;
        }

        // Class trees should have a single starting node per spec
        String startingNodeId = startingNodes.iterator().next();

        // Allocate the starting node
        PassiveTreeService service = PassiveTreeService.get();
        AllocationResult result = service.allocateNode(entityRef, classTree.getId(), startingNodeId);

        if (result.success()) {
            LOGGER.atInfo().log(
                    "ClassTreeStartingNodeSystem: Auto-allocated starting node %s for class %s",
                    startingNodeId, classId);
        } else {
            LOGGER.atWarning().log(
                    "ClassTreeStartingNodeSystem: Failed to auto-allocate starting node %s for class %s: %s",
                    startingNodeId, classId, result.reason());
        }
    }

    /**
     * Shutdown the system and unregister event handlers.
     */
    public void shutdown() {
        if (classLevelRegistration != null) {
            classLevelRegistration.unregister();
            classLevelRegistration = null;
        }
    }
}
