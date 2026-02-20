package reign.software.hyforged.minion;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.concentration.ConcentratedAbility;
import reign.software.hyforged.concentration.ConcentrationPriorityComponent;
import reign.software.hyforged.concentration.ConcentrationService;
import reign.software.hyforged.minion.component.MinionTrackerComponent;
import reign.software.hyforged.minion.component.SummonerLinkComponent;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Singleton service for spawning and despawning minion entities.
 * <p>
 * Uses thread-safe request queues drained each tick by {@link reign.software.hyforged.minion.system.MinionSummonTickingSystem}.
 * Concentration reservation callbacks enqueue requests rather than performing
 * entity operations directly, ensuring all mutations happen on the world tick thread.
 * <p>
 * Follows the singleton pattern of {@link ConcentrationService}.
 */
public final class MinionSummonService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Ability ID prefix for minion concentration entries. */
    public static final String MINION_ABILITY_PREFIX = "minion:";

    private static final StatId RESERVATION_EFFICIENCY_STAT = StatId.hyforged("reservation-efficiency-bps");
    private static final StatId MAX_MINIONS_STAT = StatId.hyforged("max-minions");
    private static final StatId MINION_DURATION_BPS_STAT = StatId.hyforged("minion-duration-bps");

    private static MinionSummonService instance;

    // --- Request queues (thread-safe) ---
    private final ConcurrentLinkedQueue<SpawnRequest> spawnQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<DespawnRequest> despawnQueue = new ConcurrentLinkedQueue<>();

    // --- Duration tracking ---
    /** Maps ability ID → despawn game tick. Only populated for duration-based minions. */
    private final Map<String, Long> durationTimers = new ConcurrentHashMap<>();

    // --- Reverse lookup: ability ID → summoner UUID (for duration expiry and cleanup) ---
    private final Map<String, UUID> abilityIdToSummonerUuid = new ConcurrentHashMap<>();

    // --- Disconnect queue (thread-safe, drained on tick thread) ---
    private final ConcurrentLinkedQueue<UUID> disconnectQueue = new ConcurrentLinkedQueue<>();

    // --- Active summoner tracking for stale ref scanning ---
    private final Set<UUID> activeSummoners = new HashSet<>();
    private int staleRefScanCounter = 0;
    private static final int STALE_REF_SCAN_INTERVAL = 200;

    // --- Tick-based timing (M-2: replaces wall-clock for durations) ---
    private long currentTickCounter = 0;

    // --- Reactive cap enforcement (C-1) ---
    private int capEnforcementCounter = 0;
    private static final int CAP_ENFORCEMENT_INTERVAL = 20;

    // --- Cached stat indices (lazily resolved) ---
    private int reservationEfficiencyIndex = -1;
    private int maxMinionsIndex = -1;
    private int minionDurationBpsIndex = -1;
    private volatile boolean indicesCached = false;

    // --- Component types (lazily initialized) ---
    private volatile ComponentType<EntityStore, SummonerLinkComponent> summonerLinkType;
    private volatile ComponentType<EntityStore, MinionTrackerComponent> minionTrackerType;

    private MinionSummonService() {
    }

    /**
     * Get or create the singleton instance.
     */
    @Nonnull
    public static synchronized MinionSummonService get() {
        if (instance == null) {
            instance = new MinionSummonService();
        }
        return instance;
    }

    /**
     * Reset singleton for testing.
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Initialize component types. Called once during plugin setup after component registration.
     */
    public void initialize(
            @Nonnull ComponentType<EntityStore, SummonerLinkComponent> summonerLinkType,
            @Nonnull ComponentType<EntityStore, MinionTrackerComponent> minionTrackerType
    ) {
        this.summonerLinkType = Objects.requireNonNull(summonerLinkType);
        this.minionTrackerType = Objects.requireNonNull(minionTrackerType);
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Request a minion summon. Validates cap and concentration before enqueuing.
     *
     * @param summonerRef  the summoner entity reference
     * @param minionTypeId the namespaced minion type ID (e.g. "hyforged:skeleton-warrior")
     * @return true if the request was accepted (enqueued), false if denied
     */
    public boolean summon(@Nonnull Ref<EntityStore> summonerRef, @Nonnull String minionTypeId) {
        Objects.requireNonNull(summonerRef, "summonerRef cannot be null");
        Objects.requireNonNull(minionTypeId, "minionTypeId cannot be null");

        if (!summonerRef.isValid()) {
            return false;
        }

        MinionDefinition definition = MinionDefinitionRegistry.get().get(minionTypeId);
        if (definition == null) {
            LOGGER.atWarning().log("Unknown minion type: %s", minionTypeId);
            return false;
        }

        Store<EntityStore> store = summonerRef.getStore();

        // Validate max-minions cap
        ensureIndicesCached();
        int maxMinions = maxMinionsIndex >= 0
                ? StatAccessor.getStatValueInt(store, summonerRef, maxMinionsIndex)
                : Integer.MAX_VALUE;

        MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
        int currentCount = tracker != null ? tracker.getCount() : 0;

        if (maxMinions > 0 && currentCount >= maxMinions) {
            sendCapReachedMessage(store, summonerRef, currentCount, maxMinions);
            return false;
        }

        // Validate available concentration
        int baseCost = definition.getConcentrationCost();
        int adjustedCost = applyReservationEfficiency(store, summonerRef, baseCost);
        int availableConcentration = ConcentrationService.get().getCurrentConcentration(summonerRef);

        if (adjustedCost > 0 && availableConcentration < adjustedCost) {
            sendInsufficientConcentrationMessage(store, summonerRef, adjustedCost, availableConcentration);
            return false;
        }

        // Resolve ability ID
        String abilityId = resolveNextAbilityId(tracker, minionTypeId);

        // Read summoner UUID
        UUID summonerUuid = getEntityUuid(store, summonerRef);
        if (summonerUuid == null) {
            LOGGER.atWarning().log("Cannot summon: summoner has no UUID");
            return false;
        }

        spawnQueue.add(new SpawnRequest(summonerUuid, minionTypeId, abilityId, summonerRef));
        LOGGER.at(Level.FINE).log("Enqueued spawn request: type=%s, abilityId=%s, summoner=%s",
                minionTypeId, abilityId, summonerUuid);
        return true;
    }

    /**
     * Voluntarily release a specific minion by ability ID.
     *
     * @param summonerRef the summoner entity reference
     * @param abilityId   the concentration ability ID of the minion to release
     */
    public void unsummon(@Nonnull Ref<EntityStore> summonerRef, @Nonnull String abilityId) {
        Objects.requireNonNull(summonerRef, "summonerRef cannot be null");
        Objects.requireNonNull(abilityId, "abilityId cannot be null");

        if (!summonerRef.isValid()) {
            return;
        }

        Store<EntityStore> store = summonerRef.getStore();
        MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
        if (tracker == null) {
            return;
        }

        List<Ref<EntityStore>> minionRefs = tracker.getMinionRef(abilityId);
        if (minionRefs.isEmpty()) {
            return;
        }

        UUID summonerUuid = getEntityUuid(store, summonerRef);
        if (summonerUuid == null) {
            return;
        }

        for (Ref<EntityStore> minionRef : minionRefs) {
            if (minionRef.isValid()) {
                despawnQueue.add(new DespawnRequest(minionRef, abilityId, summonerUuid, true));
            }
        }

        LOGGER.at(Level.FINE).log("Enqueued voluntary unsummon: abilityId=%s, summoner=%s",
                abilityId, summonerUuid);
    }

    /**
     * Release all minions for a summoner.
     *
     * @param summonerRef the summoner entity reference
     */
    public void unsummonAll(@Nonnull Ref<EntityStore> summonerRef) {
        Objects.requireNonNull(summonerRef, "summonerRef cannot be null");

        if (!summonerRef.isValid()) {
            return;
        }

        Store<EntityStore> store = summonerRef.getStore();
        MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
        if (tracker == null) {
            return;
        }

        UUID summonerUuid = getEntityUuid(store, summonerRef);
        if (summonerUuid == null) {
            return;
        }

        for (Ref<EntityStore> minionRef : tracker.getAllMinionRefs()) {
            if (minionRef.isValid()) {
                SummonerLinkComponent link = store.getComponent(minionRef, getSummonerLinkType());
                String abilityId = link != null ? link.getConcentrationAbilityId() : null;
                if (abilityId != null) {
                    despawnQueue.add(new DespawnRequest(minionRef, abilityId, summonerUuid, true));
                }
            }
        }

        LOGGER.at(Level.FINE).log("Enqueued unsummon-all for summoner=%s", summonerUuid);
    }

    /**
     * Get unmodifiable list of active minion refs for a summoner.
     */
    @Nonnull
    public List<Ref<EntityStore>> getActiveMinions(@Nonnull Ref<EntityStore> summonerRef) {
        Objects.requireNonNull(summonerRef, "summonerRef cannot be null");
        if (!summonerRef.isValid()) {
            return Collections.emptyList();
        }

        Store<EntityStore> store = summonerRef.getStore();
        MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
        if (tracker == null) {
            return Collections.emptyList();
        }
        return tracker.getAllMinionRefs();
    }

    /**
     * Get the current minion count for a summoner.
     */
    public int getMinionCount(@Nonnull Ref<EntityStore> summonerRef) {
        Objects.requireNonNull(summonerRef, "summonerRef cannot be null");
        if (!summonerRef.isValid()) {
            return 0;
        }

        Store<EntityStore> store = summonerRef.getStore();
        MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
        return tracker != null ? tracker.getCount() : 0;
    }

    /**
     * Enqueue a despawn request. Called from onDisable concentration callback.
     *
     * @param minionRef            the minion entity reference
     * @param abilityId            the concentration ability ID
     * @param summonerUuid         the summoner's UUID
     * @param releaseConcentration whether to release the concentration entry on despawn
     */
    public void enqueueDespawn(
            @Nonnull Ref<EntityStore> minionRef,
            @Nonnull String abilityId,
            @Nonnull UUID summonerUuid,
            boolean releaseConcentration
    ) {
        Objects.requireNonNull(minionRef, "minionRef cannot be null");
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        Objects.requireNonNull(summonerUuid, "summonerUuid cannot be null");
        despawnQueue.add(new DespawnRequest(minionRef, abilityId, summonerUuid, releaseConcentration));
    }

    /**
     * Enqueue a respawn request. Called from onEnable concentration callback.
     *
     * @param summonerUuid the summoner's UUID
     * @param minionTypeId the minion type ID
     * @param abilityId    the concentration ability ID to reuse
     */
    public void enqueueRespawn(
            @Nonnull UUID summonerUuid,
            @Nonnull String minionTypeId,
            @Nonnull String abilityId
    ) {
        Objects.requireNonNull(summonerUuid, "summonerUuid cannot be null");
        Objects.requireNonNull(minionTypeId, "minionTypeId cannot be null");
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        spawnQueue.add(new SpawnRequest(summonerUuid, minionTypeId, abilityId, null));
    }

    /**
     * Enqueue a player disconnect for minion cleanup on the tick thread.
     * Called from the {@code PlayerDisconnectEvent} handler. Thread-safe.
     *
     * @param playerUuid the disconnected player's UUID
     */
    public void enqueuePlayerDisconnect(@Nonnull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        disconnectQueue.add(playerUuid);
    }

    /**
     * Remove a duration timer for a minion ability.
     * Called externally by {@link reign.software.hyforged.minion.system.MinionDeathSystem}
     * when a minion dies, to prevent stale timer entries.
     *
     * @param abilityId the concentration ability ID
     */
    public void removeDurationTimer(@Nonnull String abilityId) {
        durationTimers.remove(abilityId);
        abilityIdToSummonerUuid.remove(abilityId);
    }

    // ========================================================================
    // Tick processing (called from MinionSummonTickingSystem)
    // ========================================================================

    /**
     * Process all pending spawn/despawn requests and check duration timers.
     * Called once per tick by {@link reign.software.hyforged.minion.system.MinionSummonTickingSystem}.
     * <p>
     * Processing order:
     * <ol>
     *   <li>Increment tick counter (for tick-based duration timers)</li>
     *   <li>Drain despawn queue (removals before additions)</li>
     *   <li>Process disconnect queue (despawn minions, preserve concentration for reconnect)</li>
     *   <li>Drain spawn queue</li>
     *   <li>Check duration timers for expired minions</li>
     *   <li>Reactive cap enforcement (every ~20 ticks)</li>
     *   <li>Periodic stale ref scan</li>
     * </ol>
     *
     * @param store the entity store for this world tick
     */
    public void processTick(@Nonnull Store<EntityStore> store) {
        // 0. Increment tick counter for tick-based duration timers (M-2)
        currentTickCounter++;
        // 1. Drain despawn queue first
        DespawnRequest despawnReq;
        int despawnCount = 0;
        while ((despawnReq = despawnQueue.poll()) != null) {
            try {
                performDespawn(store, despawnReq);
                despawnCount++;
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "Error processing despawn request: abilityId=%s", despawnReq.abilityId());
            }
        }

        // 2. Process disconnect queue (despawn minions, preserve concentration for reconnect)
        UUID disconnectedUuid;
        while ((disconnectedUuid = disconnectQueue.poll()) != null) {
            try {
                processPlayerDisconnect(store, disconnectedUuid);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "Error processing disconnect for player %s", disconnectedUuid);
            }
        }

        // 3. Drain spawn queue
        SpawnRequest spawnReq;
        int spawnCount = 0;
        while ((spawnReq = spawnQueue.poll()) != null) {
            try {
                performSpawn(store, spawnReq);
                spawnCount++;
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log(
                        "Error processing spawn request: type=%s, abilityId=%s",
                        spawnReq.minionTypeId(), spawnReq.abilityId());
            }
        }

        if (despawnCount > 0 || spawnCount > 0) {
            LOGGER.at(Level.FINE).log("Tick processed: %d despawn(s), %d spawn(s)",
                    despawnCount, spawnCount);
        }

        // 4. Check duration timers
        checkDurationTimers(store);

        // 5. Reactive cap enforcement (C-1): check every ~20 ticks
        capEnforcementCounter++;
        if (capEnforcementCounter >= CAP_ENFORCEMENT_INTERVAL) {
            capEnforcementCounter = 0;
            enforceCapReduction(store);
        }

        // 6. Periodic stale ref scan
        staleRefScanCounter++;
        if (staleRefScanCounter >= STALE_REF_SCAN_INTERVAL) {
            staleRefScanCounter = 0;
            cleanStaleRefs(store);
        }
    }

    /**
     * Check duration timers and enqueue despawns for expired minions.
     * Uses the {@link #abilityIdToSummonerUuid} reverse map to locate summoners
     * and enqueue full-release despawns for expired minions.
     *
     * @param store the entity store for this tick
     */
    private void checkDurationTimers(@Nonnull Store<EntityStore> store) {
        if (durationTimers.isEmpty()) {
            return;
        }

        long now = currentTickCounter;
        var iterator = durationTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now >= entry.getValue()) {
                String abilityId = entry.getKey();
                iterator.remove();

                // Look up summoner and enqueue despawn (processed next tick)
                UUID summonerUuid = abilityIdToSummonerUuid.remove(abilityId);
                if (summonerUuid != null) {
                    Ref<EntityStore> summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
                    if (summonerRef != null && summonerRef.isValid()) {
                        MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
                        if (tracker != null) {
                            for (Ref<EntityStore> minionRef : tracker.getMinionRef(abilityId)) {
                                if (minionRef.isValid()) {
                                    despawnQueue.add(new DespawnRequest(minionRef, abilityId, summonerUuid, true));
                                }
                            }
                        }
                    }
                }

                LOGGER.at(Level.FINE).log("Duration expired for minion abilityId=%s", abilityId);
            }
        }
    }

    /**
     * Process a player disconnect: despawn all minions without releasing concentration.
     * Concentration reservations are preserved in {@code ConcentrationPriorityComponent}
     * for reconnect (FR-12).
     *
     * @param store      the entity store
     * @param playerUuid the disconnected player's UUID
     */
    private void processPlayerDisconnect(@Nonnull Store<EntityStore> store, @Nonnull UUID playerUuid) {
        Ref<EntityStore> playerRef = store.getExternalData().getRefFromUUID(playerUuid);
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.at(Level.FINE).log(
                    "Disconnect cleanup: player %s not found/invalid, orphaned minions will be caught by stale ref scanner",
                    playerUuid);
            activeSummoners.remove(playerUuid);
            return;
        }

        MinionTrackerComponent tracker = store.getComponent(playerRef, getMinionTrackerType());
        if (tracker == null || tracker.getCount() == 0) {
            activeSummoners.remove(playerUuid);
            return;
        }

        int despawnedCount = 0;

        // Despawn all minions without releasing concentration (preserved for reconnect)
        for (Ref<EntityStore> minionRef : tracker.getAllMinionRefs()) {
            if (minionRef.isValid()) {
                store.removeEntity(minionRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
                despawnedCount++;
            }
        }

        // Clean up duration timers and reverse lookup for all abilities
        for (String abilityId : tracker.getAbilityIds()) {
            durationTimers.remove(abilityId);
            abilityIdToSummonerUuid.remove(abilityId);
        }

        // Clear tracker (do NOT release concentration — preserved for reconnect FR-12)
        tracker.clear();
        activeSummoners.remove(playerUuid);

        LOGGER.at(Level.FINE).log("Disconnect cleanup: despawned %d minion(s) for player %s",
                despawnedCount, playerUuid);
    }

    /**
     * Periodically scan active summoners for stale (invalid) minion refs.
     * If all refs for an ability are stale, release concentration and clean the tracker.
     * This handles edge cases like admin-removed entities or non-death removals.
     *
     * @param store the entity store
     */
    private void cleanStaleRefs(@Nonnull Store<EntityStore> store) {
        if (activeSummoners.isEmpty()) {
            return;
        }

        var iterator = activeSummoners.iterator();
        while (iterator.hasNext()) {
            UUID summonerUuid = iterator.next();
            Ref<EntityStore> summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
            if (summonerRef == null || !summonerRef.isValid()) {
                iterator.remove();
                continue;
            }

            MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
            if (tracker == null || tracker.getCount() == 0) {
                iterator.remove();
                continue;
            }

            // Check each ability for stale refs
            for (String abilityId : tracker.getAbilityIds()) {
                List<Ref<EntityStore>> refs = tracker.getMinionRef(abilityId);
                boolean hasValidRef = false;
                for (Ref<EntityStore> minionRef : refs) {
                    if (minionRef.isValid()) {
                        hasValidRef = true;
                        break;
                    }
                }

                if (!hasValidRef) {
                    // All refs for this ability are stale — full cleanup
                    tracker.removeMinion(abilityId);
                    ConcentrationService.get().releaseConcentration(summonerRef, abilityId);
                    durationTimers.remove(abilityId);
                    abilityIdToSummonerUuid.remove(abilityId);
                    LOGGER.at(Level.FINE).log(
                            "Stale ref cleanup: released abilityId=%s for summoner=%s",
                            abilityId, summonerUuid);
                }
            }

            if (tracker.getCount() == 0) {
                iterator.remove();
            }
        }
    }

    // ========================================================================
    // Spawn/despawn execution (internal, called from processTick)
    // ========================================================================

    /**
     * Perform the actual NPC spawn after dequeueing.
     */
    private void performSpawn(@Nonnull Store<EntityStore> store, @Nonnull SpawnRequest request) {
        UUID summonerUuid = request.summonerUuid();
        String minionTypeId = request.minionTypeId();
        String abilityId = request.abilityId();

        // m-2: Try cached summonerRef as fast-path before UUID lookup
        Ref<EntityStore> summonerRef = request.summonerRef();
        if (summonerRef == null || !summonerRef.isValid()) {
            summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
        }
        if (summonerRef == null || !summonerRef.isValid()) {
            LOGGER.at(Level.FINE).log("Spawn aborted: summoner %s not found or invalid", summonerUuid);
            return;
        }

        MinionDefinition definition = MinionDefinitionRegistry.get().get(minionTypeId);
        if (definition == null) {
            LOGGER.atWarning().log("Spawn aborted: unknown minion type %s", minionTypeId);
            return;
        }

        // Resolve NPC role index
        int roleIndex = NPCPlugin.get().getIndex(definition.getNpcTemplate());
        if (roleIndex < 0) {
            LOGGER.atWarning().log("Spawn aborted: unknown NPC template '%s' for minion %s",
                    definition.getNpcTemplate(), minionTypeId);
            return;
        }

        // Get summoner position for spawn offset
        TransformComponent transform = store.getComponent(
                summonerRef, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.atWarning().log("Spawn aborted: summoner %s has no TransformComponent", summonerUuid);
            return;
        }

        Vector3d summonerPos = transform.getPosition();
        Vector3d spawnPos = new Vector3d(
                summonerPos.x + definition.getSpawnOffsetX(),
                summonerPos.y + definition.getSpawnOffsetY(),
                summonerPos.z + definition.getSpawnOffsetZ()
        );

        // Reserve concentration
        int baseCost = definition.getConcentrationCost();
        int adjustedCost = applyReservationEfficiency(store, summonerRef, baseCost);

        // M-1: Re-validate concentration at execution time (TOCTOU guard)
        int availableConcentration = ConcentrationService.get().getCurrentConcentration(summonerRef);
        if (adjustedCost > 0 && availableConcentration < adjustedCost) {
            LOGGER.at(Level.FINE).log("Spawn aborted: insufficient concentration at execution time for %s (need %d, have %d)",
                    minionTypeId, adjustedCost, availableConcentration);
            return;
        }

        // M-1: Re-validate max-minions cap at execution time (TOCTOU guard)
        MinionTrackerComponent preSpawnTracker = store.getComponent(summonerRef, getMinionTrackerType());
        int currentCount = preSpawnTracker != null ? preSpawnTracker.getCount() : 0;
        ensureIndicesCached();
        int maxMinions = maxMinionsIndex >= 0 ? StatAccessor.getStatValueInt(store, summonerRef, maxMinionsIndex) : Integer.MAX_VALUE;
        if (maxMinions > 0 && currentCount >= maxMinions) {
            LOGGER.at(Level.FINE).log("Spawn aborted: max-minions cap reached at execution time for %s (%d/%d)",
                    minionTypeId, currentCount, maxMinions);
            return;
        }

        // Prepare the SummonerLinkComponent to inject via preAddToWorld
        ComponentType<EntityStore, SummonerLinkComponent> linkType = getSummonerLinkType();
        ComponentType<EntityStore, MinionTrackerComponent> trackerType = getMinionTrackerType();

        // Spawn the NPC with preAddToWorld callback to inject SummonerLinkComponent
        Pair<Ref<EntityStore>, NPCEntity> result = NPCPlugin.get().spawnEntity(
                store,
                roleIndex,
                spawnPos,
                new Vector3f(0f, 0f, 0f),
                null, // no override model
                (npcEntity, holder, spawnStore) -> {
                    // preAddToWorld: inject SummonerLinkComponent on the holder
                    SummonerLinkComponent link = new SummonerLinkComponent();
                    link.setSummonerUuid(summonerUuid);
                    link.setMinionTypeId(minionTypeId);
                    link.setConcentrationAbilityId(abilityId);
                    link.setSummonTimestamp(System.currentTimeMillis());
                    holder.addComponent(linkType, link);
                },
                null // no postSpawn
        );

        if (result == null) {
            LOGGER.atWarning().log("Failed to spawn minion %s for summoner %s", minionTypeId, summonerUuid);
            return;
        }

        Ref<EntityStore> minionRef = result.first();
        if (minionRef == null || !minionRef.isValid()) {
            LOGGER.atWarning().log("Spawn returned null/invalid ref for minion %s", minionTypeId);
            return;
        }

        // Register with MinionTrackerComponent on summoner
        MinionTrackerComponent tracker = store.ensureAndGetComponent(summonerRef, trackerType);
        tracker.addMinion(abilityId, minionRef);

        // Reserve concentration with callbacks
        // Capture refs for use in callbacks
        final Ref<EntityStore> capturedMinionRef = minionRef;
        final MinionSummonService service = this;

        ConcentrationService.get().reserveConcentration(
                summonerRef,
                abilityId,
                adjustedCost,
                // onDisable: despawn the minion (don't release concentration — system is disabling)
                () -> {
                    if (capturedMinionRef.isValid()) {
                        service.enqueueDespawn(capturedMinionRef, abilityId, summonerUuid, false);
                    }
                },
                // onEnable: respawn the minion
                () -> service.enqueueRespawn(summonerUuid, minionTypeId, abilityId)
        );

        // Set priority from definition
        if (definition.getDefaultPriority() > 0) {
            ConcentrationService.get().setPriority(summonerRef, abilityId, definition.getDefaultPriority());
        }

        // Duration timer (M-2: tick-based instead of wall-clock)
        if (definition.getBaseDuration() > 0) {
            long effectiveDurationTicks = computeEffectiveDurationTicks(
                    store, summonerRef, definition.getBaseDuration());
            durationTimers.put(abilityId, currentTickCounter + effectiveDurationTicks);
        }

        // Track summoner for stale ref scanning
        activeSummoners.add(summonerUuid);
        abilityIdToSummonerUuid.put(abilityId, summonerUuid);

        // Notify summoner
        sendSummonMessage(store, summonerRef, definition.getNpcTemplate());

        LOGGER.at(Level.FINE).log("Spawned minion %s (abilityId=%s) for summoner %s at (%s, %s, %s)",
                minionTypeId, abilityId, summonerUuid,
                spawnPos.x, spawnPos.y, spawnPos.z);
    }

    /**
     * Perform the actual entity removal for a despawn.
     */
    private void performDespawn(@Nonnull Store<EntityStore> store, @Nonnull DespawnRequest request) {
        Ref<EntityStore> minionRef = request.minionRef();
        String abilityId = request.abilityId();
        UUID summonerUuid = request.summonerUuid();

        // Remove the minion entity using store.removeEntity() directly.
        // This follows the established Hytale pattern for ticking systems where
        // entity removal is performed on the world tick thread (processTick).
        // CommandBuffer is not used because we are already on the tick thread.
        if (minionRef.isValid()) {
            store.removeEntity(minionRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
        }

        // Clean up tracker
        Ref<EntityStore> summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
        if (summonerRef != null && summonerRef.isValid()) {
            MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
            if (tracker != null) {
                tracker.removeMinion(abilityId);
            }

            // Release concentration if requested (voluntary/death)
            if (request.releaseConcentration()) {
                ConcentrationService.get().releaseConcentration(summonerRef, abilityId);
            }
        }

        // Clean up duration timer and reverse lookup
        durationTimers.remove(abilityId);
        abilityIdToSummonerUuid.remove(abilityId);

        LOGGER.at(Level.FINE).log("Despawned minion for abilityId=%s, summoner=%s, releaseConcentration=%s",
                abilityId, summonerUuid, request.releaseConcentration());
    }

    // ========================================================================
    // Internal helpers
    // ========================================================================

    /**
     * Apply reservation-efficiency-bps to a concentration cost.
     * Follows the pattern from {@link reign.software.hyforged.stats.system.HyforgedEffectBridgeSystem#applyReservationEfficiency}.
     */
    private int applyReservationEfficiency(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> entityRef,
            int baseCost
    ) {
        if (baseCost <= 0) {
            return 0;
        }
        ensureIndicesCached();
        if (reservationEfficiencyIndex < 0) {
            return baseCost;
        }
        int efficiencyBps = StatAccessor.getStatValueInt(store, entityRef, reservationEfficiencyIndex);
        float multiplier = (HyforgedModifier.BPS_100_PERCENT - efficiencyBps) / (float) HyforgedModifier.BPS_100_PERCENT;
        return Math.max(0, Math.round(baseCost * multiplier));
    }

    /**
     * Generate the next ability ID for a minion type.
     * Convention: "minion:{typeId}:{index}"
     */
    @Nonnull
    String resolveNextAbilityId(@Nullable MinionTrackerComponent tracker, @Nonnull String minionTypeId) {
        if (tracker == null) {
            return MINION_ABILITY_PREFIX + minionTypeId + ":0";
        }

        // Find next available index for this type
        int nextIndex = 0;
        while (tracker.hasMinion(MINION_ABILITY_PREFIX + minionTypeId + ":" + nextIndex)) {
            nextIndex++;
        }
        return MINION_ABILITY_PREFIX + minionTypeId + ":" + nextIndex;
    }

    /**
     * Parse the minion type ID from an ability ID.
     * <p>
     * Ability ID format: {@code "minion:{typeId}:{index}"}
     * <p>
     * The typeId itself may contain colons (e.g., {@code "hyforged:skeleton-warrior"}),
     * so the last colon-separated segment is the index.
     *
     * @return the minion type ID, or null if the format is invalid
     */
    static String parseMinionTypeId(@Nonnull String abilityId) {
        // Strip the "minion:" prefix
        String remainder = abilityId.substring(MINION_ABILITY_PREFIX.length());
        // Format: "{typeId}:{index}" where typeId itself may contain ":"
        // e.g., "hyforged:skeleton-warrior:0" → typeId = "hyforged:skeleton-warrior", index = "0"
        int lastColon = remainder.lastIndexOf(':');
        if (lastColon <= 0) {
            return null; // No type:index separator found, or empty type
        }

        String typeId = remainder.substring(0, lastColon);
        String indexStr = remainder.substring(lastColon + 1);

        // Validate index is numeric
        try {
            Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            return null;
        }

        return typeId.isEmpty() ? null : typeId;
    }

    /**
     * Reactive cap enforcement: when max-minions stat decreases (e.g., unequip item),
     * existing minions may exceed the new cap. This method checks all active summoners
     * and despawns excess minions in ascending priority order (lowest priority first).
     * <p>
     * Called periodically from {@link #processTick(Store)} (every ~20 ticks).
     *
     * @param store the entity store for this tick
     */
    private void enforceCapReduction(@Nonnull Store<EntityStore> store) {
        if (activeSummoners.isEmpty()) {
            return;
        }

        ensureIndicesCached();
        if (maxMinionsIndex < 0) {
            return; // No max-minions stat defined
        }

        ComponentType<EntityStore, ConcentrationPriorityComponent> concType =
                HyforgedPlugin.getInstance().getConcentrationPriorityComponentType();

        for (UUID summonerUuid : activeSummoners) {
            Ref<EntityStore> summonerRef = store.getExternalData().getRefFromUUID(summonerUuid);
            if (summonerRef == null || !summonerRef.isValid()) {
                continue;
            }

            int maxMinions = StatAccessor.getStatValueInt(store, summonerRef, maxMinionsIndex);
            if (maxMinions <= 0) {
                continue; // No cap or unlimited
            }

            MinionTrackerComponent tracker = store.getComponent(summonerRef, getMinionTrackerType());
            if (tracker == null) {
                continue;
            }

            int currentCount = tracker.getCount();
            if (currentCount <= maxMinions) {
                continue; // Within cap
            }

            int excess = currentCount - maxMinions;

            // Collect minion abilities with their priorities, sorted ascending (lowest priority first)
            ConcentrationPriorityComponent concComp = store.getComponent(summonerRef, concType);
            List<String> minionAbilityIds = new ArrayList<>();
            for (String abilityId : tracker.getAbilityIds()) {
                minionAbilityIds.add(abilityId);
            }

            // Sort by priority ascending (lowest first = despawn first)
            if (concComp != null) {
                minionAbilityIds.sort((a, b) -> {
                    ConcentratedAbility abilityA = concComp.getAbility(a);
                    ConcentratedAbility abilityB = concComp.getAbility(b);
                    int priorityA = abilityA != null ? abilityA.priority() : 0;
                    int priorityB = abilityB != null ? abilityB.priority() : 0;
                    return Integer.compare(priorityA, priorityB); // ascending: lowest first
                });
            }

            // Despawn excess minions starting from lowest priority
            int despawned = 0;
            for (String abilityId : minionAbilityIds) {
                if (despawned >= excess) {
                    break;
                }
                List<Ref<EntityStore>> refs = tracker.getMinionRef(abilityId);
                for (Ref<EntityStore> minionRef : refs) {
                    if (despawned >= excess) {
                        break;
                    }
                    if (minionRef.isValid()) {
                        despawnQueue.add(new DespawnRequest(minionRef, abilityId, summonerUuid, true));
                        despawned++;
                    }
                }
            }

            if (despawned > 0) {
                LOGGER.atInfo().log(
                        "Cap enforcement: despawning %d excess minion(s) for summoner %s (count=%d, cap=%d)",
                        despawned, summonerUuid, currentCount, maxMinions);
            }
        }
    }

    /**
     * Compute effective duration in game ticks, applying minion-duration-bps.
     * Returns actual ticks (20 ticks per second).
     */
    private long computeEffectiveDurationTicks(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> summonerRef,
            int baseDurationSeconds
    ) {
        ensureIndicesCached();
        long baseTicks = baseDurationSeconds * 20L; // 20 ticks per second
        if (minionDurationBpsIndex < 0) {
            return baseTicks;
        }
        int durationBps = StatAccessor.getStatValueInt(store, summonerRef, minionDurationBpsIndex);
        float multiplier = 1.0f + durationBps / (float) HyforgedModifier.BPS_100_PERCENT;
        return Math.max(0, Math.round(baseTicks * multiplier));
    }

    private void ensureIndicesCached() {
        if (indicesCached) {
            return;
        }
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        reservationEfficiencyIndex = registry.getIndex(RESERVATION_EFFICIENCY_STAT);
        maxMinionsIndex = registry.getIndex(MAX_MINIONS_STAT);
        minionDurationBpsIndex = registry.getIndex(MINION_DURATION_BPS_STAT);
        indicesCached = true;
    }

    @Nullable
    private UUID getEntityUuid(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> entityRef) {
        UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        return uuidComponent != null ? uuidComponent.getUuid() : null;
    }

    @Nonnull
    private ComponentType<EntityStore, SummonerLinkComponent> getSummonerLinkType() {
        if (summonerLinkType == null) {
            throw new IllegalStateException("MinionSummonService not initialized. Call initialize() first.");
        }
        return summonerLinkType;
    }

    @Nonnull
    ComponentType<EntityStore, MinionTrackerComponent> getMinionTrackerType() {
        if (minionTrackerType == null) {
            throw new IllegalStateException("MinionSummonService not initialized. Call initialize() first.");
        }
        return minionTrackerType;
    }

    /**
     * Send a cap-reached denial message to the summoner player.
     */
    private void sendCapReachedMessage(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> summonerRef,
            int current,
            int max
    ) {
        PlayerRef playerRef = store.getComponent(summonerRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(
                    Message.translation("minion.cap_reached")
                            .param("current", current)
                            .param("max", max)
                            .color(MessageColors.ERROR)
            );
        }
    }

    /**
     * Send an insufficient-concentration denial message to the summoner player.
     */
    private void sendInsufficientConcentrationMessage(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> summonerRef,
            int required,
            int available
    ) {
        PlayerRef playerRef = store.getComponent(summonerRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(
                    Message.translation("minion.insufficient_concentration")
                            .param("required", required)
                            .param("available", available)
                            .color(MessageColors.ERROR)
            );
        }
    }

    /**
     * Send a summon success message to the summoner player.
     */
    private void sendSummonMessage(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> summonerRef,
            @Nonnull String minionName
    ) {
        PlayerRef playerRef = store.getComponent(summonerRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(
                    Message.translation("minion.summoned")
                            .param("name", minionName)
                            .color(MessageColors.SUCCESS)
            );
        }
    }

    // ========================================================================
    // Request records
    // ========================================================================

    /**
     * A request to spawn a minion entity.
     *
     * @param summonerUuid the summoner's UUID
     * @param minionTypeId the minion definition ID
     * @param abilityId    the concentration ability ID
     * @param summonerRef  optional cached summoner ref (may be null for respawns)
     */
    record SpawnRequest(
            @Nonnull UUID summonerUuid,
            @Nonnull String minionTypeId,
            @Nonnull String abilityId,
            @Nullable Ref<EntityStore> summonerRef
    ) {
    }

    /**
     * A request to despawn a minion entity.
     *
     * @param minionRef            the minion entity reference
     * @param abilityId            the concentration ability ID
     * @param summonerUuid         the summoner's UUID
     * @param releaseConcentration whether to release the concentration reservation
     */
    record DespawnRequest(
            @Nonnull Ref<EntityStore> minionRef,
            @Nonnull String abilityId,
            @Nonnull UUID summonerUuid,
            boolean releaseConcentration
    ) {
    }
}
