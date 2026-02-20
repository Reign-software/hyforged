package reign.software.hyforged.minion.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Component attached to a summoner (player) entity that tracks all active minions.
 * <p>
 * Keyed by concentration ability ID so that each ability slot maps to its spawned
 * minion(s). Maintains a running {@code totalMinionCount} for O(1) cap checks.
 * <p>
 * Transient component — no persistence codec. Minion tracking is rebuilt on
 * reconnect from concentration state.
 */
public class MinionTrackerComponent implements Component<EntityStore> {

    // Design note (m-4): activeMinions uses List<Ref<EntityStore>> per ability ID
    // rather than a single Ref. This supports future multi-minion-per-ability scenarios
    // (e.g., an ability that summons 2+ copies). For current single-minion abilities,
    // the list always contains 0 or 1 entries.
    private final Map<String, List<Ref<EntityStore>>> activeMinions = new HashMap<>();
    private int totalMinionCount;

    /** Default constructor required by ECS. */
    public MinionTrackerComponent() {
    }

    /** Copy constructor for clone(). */
    public MinionTrackerComponent(@Nonnull MinionTrackerComponent other) {
        for (Map.Entry<String, List<Ref<EntityStore>>> entry : other.activeMinions.entrySet()) {
            this.activeMinions.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.totalMinionCount = other.totalMinionCount;
    }

    @Override
    public MinionTrackerComponent clone() {
        return new MinionTrackerComponent(this);
    }

    // --- Mutation methods ---

    /**
     * Register a newly spawned minion under the given ability ID.
     *
     * @param abilityId  the concentration ability ID that spawned this minion
     * @param minionRef  a live reference to the minion entity
     */
    public void addMinion(@Nonnull String abilityId, @Nonnull Ref<EntityStore> minionRef) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        Objects.requireNonNull(minionRef, "minionRef cannot be null");
        activeMinions.computeIfAbsent(abilityId, k -> new ArrayList<>()).add(minionRef);
        totalMinionCount++;
    }

    /**
     * Remove all minions associated with the given ability ID.
     *
     * @param abilityId the concentration ability ID
     * @return the removed minion refs, or an empty list if none were tracked
     */
    @Nonnull
    public List<Ref<EntityStore>> removeMinion(@Nonnull String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        List<Ref<EntityStore>> removed = activeMinions.remove(abilityId);
        if (removed != null) {
            totalMinionCount -= removed.size();
            return removed;
        }
        return Collections.emptyList();
    }

    /**
     * Remove a specific minion ref from whichever ability it belongs to.
     *
     * @param minionRef the minion entity ref to remove
     * @return true if the ref was found and removed
     */
    public boolean removeMinionRef(@Nonnull Ref<EntityStore> minionRef) {
        Objects.requireNonNull(minionRef, "minionRef cannot be null");
        for (Iterator<Map.Entry<String, List<Ref<EntityStore>>>> it = activeMinions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, List<Ref<EntityStore>>> entry = it.next();
            if (entry.getValue().remove(minionRef)) {
                totalMinionCount--;
                if (entry.getValue().isEmpty()) {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all tracked minions.
     */
    public void clear() {
        activeMinions.clear();
        totalMinionCount = 0;
    }

    // --- Query methods ---

    /**
     * Get the minion refs for a specific ability ID.
     *
     * @param abilityId the concentration ability ID
     * @return unmodifiable list of minion refs, or empty list if none
     */
    @Nonnull
    public List<Ref<EntityStore>> getMinionRef(@Nonnull String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        List<Ref<EntityStore>> refs = activeMinions.get(abilityId);
        return refs != null ? Collections.unmodifiableList(refs) : Collections.emptyList();
    }

    /**
     * Get all active minion refs across all abilities.
     *
     * @return unmodifiable list of all minion refs
     */
    @Nonnull
    public List<Ref<EntityStore>> getAllMinionRefs() {
        List<Ref<EntityStore>> all = new ArrayList<>(totalMinionCount);
        for (List<Ref<EntityStore>> refs : activeMinions.values()) {
            all.addAll(refs);
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Get the total number of active minions. O(1).
     */
    public int getCount() {
        return totalMinionCount;
    }

    /**
     * Get all ability IDs that have active minions.
     *
     * @return a snapshot set of ability IDs (safe to iterate while modifying the tracker)
     */
    @Nonnull
    public Set<String> getAbilityIds() {
        return new HashSet<>(activeMinions.keySet());
    }

    /**
     * Check if any minions are tracked for the given ability.
     */
    public boolean hasMinion(@Nonnull String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        List<Ref<EntityStore>> refs = activeMinions.get(abilityId);
        return refs != null && !refs.isEmpty();
    }
}
