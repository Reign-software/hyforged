package reign.software.hyforged.progression.system;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.progression.event.ActiveClassChangedEvent;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

/**
 * ECS System that resolves the active class from the player's main-hand weapon tags.
 * <p>
 * This system runs each tick for entities with ProgressionComponent, Player, and PlayerRef.
 * It reads the main-hand item's tags, matches them against class weapon tag families,
 * and updates the ProgressionComponent's activeClassId when the class changes.
 * <p>
 * Edge cases handled:
 * - No weapon equipped → activeClassId = null (only character XP awarded)
 * - Weapon matches no class → activeClassId = null
 * - Weapon matches multiple classes → warning logged, first alphabetically used
 * <p>
 * Emits ActiveClassChangedEvent when the class changes (for UI updates).
 */
public class ActiveClassResolutionSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final ComponentType<EntityStore, ProgressionComponent> progressionComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;
    
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType;
    
    @Nonnull
    private final Query<EntityStore> query;
    
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies;

    public ActiveClassResolutionSystem() {
        this.progressionComponentType = HyforgedPlugin.getInstance().getProgressionComponentType();
        this.playerComponentType = Player.getComponentType();
        this.playerRefComponentType = PlayerRef.getComponentType();
        
        // Query for entities with all three components (players with progression)
        this.query = Query.and(progressionComponentType, Query.and(playerComponentType, playerRefComponentType));
        
        // No specific system dependencies - runs independently each tick
        this.dependencies = Set.of();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ProgressionComponent progression = archetypeChunk.getComponent(index, progressionComponentType);
        Player player = archetypeChunk.getComponent(index, playerComponentType);
        
        if (progression == null || player == null) {
            return;
        }
        
        // Get main-hand item tags
        Set<String> weaponTags = getMainHandWeaponTags(player);
        
        // Resolve class from weapon tags
        ClassDefinition matchedClass = ClassDefinitionRegistry.get().getClassForWeaponTags(weaponTags);
        String newClassId = matchedClass != null ? matchedClass.id() : null;
        
        // Check if class changed
        String previousClassId = progression.getActiveClassId();
        if (!Objects.equals(previousClassId, newClassId)) {
            // Update the component
            progression.setActiveClassId(newClassId);
            
            // Emit event for UI updates
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
            emitClassChangedEvent(entityRef, previousClassId, newClassId);
            
            LOGGER.at(Level.FINE).log(
                "Active class changed for player: %s -> %s (weapon tags: %s)",
                previousClassId, newClassId, weaponTags
            );
        }
    }
    
    /**
     * Get tags from the player's main-hand weapon.
     *
     * @param player The player component
     * @return Set of tag strings from the main-hand item, empty if no weapon
     */
    @Nonnull
    private Set<String> getMainHandWeaponTags(@Nonnull Player player) {
        Set<String> tags = new HashSet<>();
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return tags;
        }
        
        ItemStack itemStack = inventory.getItemInHand();
        if (itemStack == null || itemStack.isEmpty()) {
            return tags;
        }
        
        Item item = itemStack.getItem();
        if (item == null) {
            return tags;
        }
        
        // Get asset data which contains tags
        AssetExtraInfo.Data data = item.getData();
        if (data == null) {
            return tags;
        }
        
        // Get raw tags (Map<String, String[]> where keys are tag names)
        // Tag format examples: "weapon:sword", "weapon", "melee"
        for (String tagName : data.getRawTags().keySet()) {
            tags.add(tagName);
        }
        
        return tags;
    }
    
    /**
     * Emit an ActiveClassChangedEvent for UI systems to react to.
     *
     * @param entityRef The player entity reference
     * @param previousClassId The previous active class ID (may be null)
     * @param newClassId The new active class ID (may be null)
     */
    private void emitClassChangedEvent(
            @Nonnull Ref<EntityStore> entityRef,
            @Nullable String previousClassId,
            @Nullable String newClassId
    ) {
        try {
            IEventDispatcher<ActiveClassChangedEvent, ActiveClassChangedEvent> dispatcher =
                HytaleServer.get().getEventBus().dispatchFor(ActiveClassChangedEvent.class);
            
            if (dispatcher.hasListener()) {
                ActiveClassChangedEvent event = new ActiveClassChangedEvent(entityRef, previousClassId, newClassId);
                dispatcher.dispatch(event);
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to emit ActiveClassChangedEvent");
        }
    }
}
