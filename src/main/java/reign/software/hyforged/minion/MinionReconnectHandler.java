package reign.software.hyforged.minion;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.concentration.ConcentratedAbility;
import reign.software.hyforged.concentration.ConcentrationPriorityComponent;
import reign.software.hyforged.concentration.ConcentrationService;
import reign.software.hyforged.minion.component.MinionTrackerComponent;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles player reconnection for the minion concentration system.
 * <p>
 * When a player reconnects, their {@link ConcentrationPriorityComponent} may
 * still contain minion ability entries (persisted across sessions). This handler
 * re-registers the concentration callbacks so that the enable/disable flow works,
 * and auto re-summons any minions whose abilities are still enabled.
 * <p>
 * Called from the {@code PlayerReadyEvent} handler in {@code HyforgedPlugin}.
 */
public final class MinionReconnectHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private MinionReconnectHandler() {
        // Utility class
    }

    /**
     * Process a player's reconnection for minion state restoration.
     * <p>
     * Must be called on the world tick thread (inside {@code world.execute()}).
     *
     * @param event the player ready event
     */
    public static void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) {
                return;
            }

            processReconnect(store, ref);
        });
    }

    /**
     * Core reconnect processing. Runs on the world tick thread.
     */
    private static void processReconnect(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef
    ) {
        // Read ConcentrationPriorityComponent
        ComponentType<EntityStore, ConcentrationPriorityComponent> concType =
                HyforgedPlugin.getInstance().getConcentrationPriorityComponentType();
        ConcentrationPriorityComponent concComp = store.getComponent(playerRef, concType);
        if (concComp == null) {
            return; // No concentration data — nothing to restore
        }

        // Get abilities snapshot (immutable copy)
        List<ConcentratedAbility> abilities = concComp.getAbilities();
        if (abilities.isEmpty()) {
            return;
        }

        // Collect minion abilities
        List<MinionAbilityEntry> minionEntries = new ArrayList<>();
        List<String> orphanedIds = new ArrayList<>();

        for (ConcentratedAbility ability : abilities) {
            String abilityId = ability.abilityId();
            if (!abilityId.startsWith(MinionSummonService.MINION_ABILITY_PREFIX)) {
                continue; // Not a minion ability
            }

            // Parse minionTypeId from "minion:{typeId}:{index}"
            String minionTypeId = parseMinionTypeId(abilityId);
            if (minionTypeId == null) {
                LOGGER.atWarning().log(
                        "Reconnect: malformed minion ability ID '%s', removing", abilityId);
                orphanedIds.add(abilityId);
                continue;
            }

            // Look up definition
            MinionDefinition definition = MinionDefinitionRegistry.get().get(minionTypeId);
            if (definition == null) {
                LOGGER.atWarning().log(
                        "Reconnect: minion definition not found for '%s' (abilityId=%s), removing orphaned entry",
                        minionTypeId, abilityId);
                orphanedIds.add(abilityId);
                continue;
            }

            minionEntries.add(new MinionAbilityEntry(abilityId, minionTypeId, definition, ability));
        }

        // Remove orphaned entries
        for (String orphanedId : orphanedIds) {
            concComp.removeAbility(orphanedId);
            LOGGER.at(Level.FINE).log("Reconnect: removed orphaned ability '%s'", orphanedId);
        }

        if (minionEntries.isEmpty()) {
            return;
        }

        // Get summoner UUID for MinionSummonService calls
        UUIDComponent uuidComp = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            LOGGER.atWarning().log("Reconnect: player has no UUIDComponent");
            return;
        }
        UUID summonerUuid = uuidComp.getUuid();

        // Ensure MinionTrackerComponent exists on the player entity
        store.ensureAndGetComponent(playerRef, MinionSummonService.get().getMinionTrackerType());

        // Re-register callbacks for all minion abilities
        MinionSummonService service = MinionSummonService.get();
        int enabledCount = 0;
        int disabledCount = 0;

        for (MinionAbilityEntry entry : minionEntries) {
            String abilityId = entry.abilityId;
            String minionTypeId = entry.minionTypeId;
            MinionDefinition definition = entry.definition;
            boolean wasEnabled = entry.ability.enabled();

            // Re-register callbacks via setAbility (preserves enabled state)
            Runnable onDisable = () -> {
                // On disable: enqueue despawn for this minion
                // Need to find the minion ref from tracker at disable-time
                Ref<EntityStore> currentPlayerRef = store.getExternalData().getRefFromUUID(summonerUuid);
                if (currentPlayerRef != null && currentPlayerRef.isValid()) {
                    MinionTrackerComponent tracker = store.getComponent(
                            currentPlayerRef, service.getMinionTrackerType());
                    if (tracker != null) {
                        List<Ref<EntityStore>> refs = tracker.getMinionRef(abilityId);
                        for (Ref<EntityStore> minionRef : refs) {
                            if (minionRef.isValid()) {
                                service.enqueueDespawn(minionRef, abilityId, summonerUuid, false);
                            }
                        }
                    }
                }
            };

            Runnable onEnable = () -> service.enqueueRespawn(summonerUuid, minionTypeId, abilityId);

            concComp.setAbility(
                    abilityId,
                    entry.ability.cost(),
                    entry.ability.priority(),
                    onDisable,
                    onEnable
            );

            // Queue respawn for enabled abilities
            if (wasEnabled) {
                service.enqueueRespawn(summonerUuid, minionTypeId, abilityId);
                enabledCount++;
            } else {
                disabledCount++;
            }
        }

        // Send reconnect summary messages to the player
        PlayerRef playerRefComp = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComp != null) {
            if (enabledCount > 0) {
                playerRefComp.sendMessage(
                        Message.translation("minion.reconnect_resummon")
                                .param("count", String.valueOf(enabledCount))
                                .color(MessageColors.SUCCESS)
                );
            }
            if (disabledCount > 0) {
                playerRefComp.sendMessage(
                        Message.translation("minion.reconnect_disabled")
                                .param("count", String.valueOf(disabledCount))
                                .color(MessageColors.WARNING)
                );
            }
        }

        LOGGER.at(Level.FINE).log(
                "Reconnect: player=%s, re-registered %d minion abilities (%d enabled, %d disabled, %d orphaned)",
                summonerUuid, minionEntries.size(), enabledCount, disabledCount, orphanedIds.size());
    }

    /**
     * Parse the minion type ID from an ability ID.
     * <p>
     * Delegates to {@link MinionSummonService#parseMinionTypeId(String)} where the
     * ability ID format is defined.
     *
     * @return the minion type ID, or null if the format is invalid
     * @deprecated Use {@link MinionSummonService#parseMinionTypeId(String)} directly.
     */
    @Deprecated
    static String parseMinionTypeId(@Nonnull String abilityId) {
        return MinionSummonService.parseMinionTypeId(abilityId);
    }

    /**
     * Internal holder for a minion ability entry during reconnect processing.
     */
    private record MinionAbilityEntry(
            @Nonnull String abilityId,
            @Nonnull String minionTypeId,
            @Nonnull MinionDefinition definition,
            @Nonnull ConcentratedAbility ability
    ) {
    }
}
