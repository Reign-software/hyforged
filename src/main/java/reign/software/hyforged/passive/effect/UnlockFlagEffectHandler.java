package reign.software.hyforged.passive.effect;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.component.PlayerUnlocksComponent;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;

import javax.annotation.Nonnull;
import java.util.logging.Level;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Effect handler for unlock-flag type passive node effects.
 * <p>
 * Sets or clears unlock flags on the entity's {@link PlayerUnlocksComponent}
 * when nodes are allocated or deallocated.
 * <p>
 * JSON effect format:
 * <pre>
 * {
 *   "Type": "unlock-flag",
 *   "FlagId": "dual_wield",
 *   "Description": "Can dual-wield weapons"  // Optional, for tooltip
 * }
 * </pre>
 * <p>
 * Flags track their source nodes, so a flag is only fully cleared when
 * no nodes enabling it remain allocated.
 */
public final class UnlockFlagEffectHandler implements PassiveEffectHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Effect type identifier.
     */
    public static final String EFFECT_TYPE = "unlock-flag";

    private final ComponentType<EntityStore, PlayerUnlocksComponent> unlocksComponentType;

    /**
     * Create a new handler.
     *
     * @param unlocksComponentType The component type for PlayerUnlocksComponent
     */
    public UnlockFlagEffectHandler(
            @Nonnull ComponentType<EntityStore, PlayerUnlocksComponent> unlocksComponentType
    ) {
        this.unlocksComponentType = unlocksComponentType;
    }

    @Override
    public void apply(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PlayerUnlocksComponent unlocksComponent = entityRef.getStore().getComponent(entityRef, unlocksComponentType);
        if (unlocksComponent == null) {
            LOGGER.atWarning().log("Entity has no PlayerUnlocksComponent, cannot set unlock flag from node: %s", node.id());
            return;
        }

        String flagId = effect.getString("FlagId");
        if (flagId == null) {
            LOGGER.atWarning().log("unlock-flag effect missing 'FlagId' field on node: %s", node.id());
            return;
        }

        unlocksComponent.enableFlag(flagId, node.id());
        LOGGER.at(Level.FINE).log("Enabled unlock flag: %s from node %s", flagId, node.id());
    }

    @Override
    public void remove(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PlayerUnlocksComponent unlocksComponent = entityRef.getStore().getComponent(entityRef, unlocksComponentType);
        if (unlocksComponent == null) {
            return;
        }

        String flagId = effect.getString("FlagId");
        if (flagId == null) {
            return;
        }

        unlocksComponent.disableFlag(flagId, node.id());
        LOGGER.at(Level.FINE).log("Disabled unlock flag source: %s from node %s", flagId, node.id());
    }

    @Override
    @Nonnull
    public String getTooltipText(@Nonnull PassiveNodeEffect effect) {
        String flagId = effect.getString("FlagId");
        String description = effect.getString("Description");
        
        // Use description if provided, otherwise format the flag ID
        if (description != null && !description.isEmpty()) {
            return description;
        }
        
        if (flagId == null) {
            return "Unlocks: Unknown";
        }
        
        // Format flag ID as display name
        String displayName = formatDisplayName(flagId);
        return "Unlocks: " + displayName;
    }
    
    private String formatDisplayName(String id) {
        // Convert snake_case or kebab-case to Title Case
        String[] parts = id.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
