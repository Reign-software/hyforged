package reign.software.hyforged.concentration;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Component storing priority ordering for concentrated abilities.
 * <p>
 * Pure data component following ECS principles.
 */
public class ConcentrationPriorityComponent implements Component<EntityStore> {

    /** Schema version for persistence migration. */
    public static final int SCHEMA_VERSION = 1;

    private final List<ConcentratedAbility> abilities = new ArrayList<>();
    private int currentConcentration = 0;

    // Accumulates fractional regeneration between ticks.
    private float regenRemainder = 0f;

    // Temporary codec load buffers
    private transient String[] tempAbilityIds;
    private transient int[] tempAbilityCosts;
    private transient int[] tempAbilityPriorities;
    private transient int[] tempAbilityEnabled;

    public ConcentrationPriorityComponent() {
    }

    public ConcentrationPriorityComponent(@Nonnull ConcentrationPriorityComponent other) {
        this.currentConcentration = other.currentConcentration;
        this.regenRemainder = other.regenRemainder;
        this.abilities.addAll(other.abilities);
    }

    /**
     * Get the ordered list of abilities (highest priority first).
     */
    @Nonnull
    public List<ConcentratedAbility> getAbilities() {
        return List.copyOf(abilities);
    }

    @Nullable
    public ConcentratedAbility getAbility(@Nonnull String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        int index = findAbilityIndex(abilityId);
        if (index >= 0) {
            return abilities.get(index);
        }
        return null;
    }

    @Nonnull
    List<ConcentratedAbility> getAbilitiesInternal() {
        return abilities;
    }

    /**
     * Add or update an ability entry.
     */
    public void setAbility(
            @Nonnull String abilityId,
            int cost,
            int priority,
            @Nullable Runnable onDisable,
            @Nullable Runnable onEnable
    ) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        int index = findAbilityIndex(abilityId);
        if (index >= 0) {
            ConcentratedAbility existing = abilities.get(index);
            abilities.set(index, new ConcentratedAbility(
                    abilityId,
                    Math.max(0, cost),
                    priority,
                    existing.enabled(),
                    onDisable,
                    onEnable
            ));
        } else {
            abilities.add(new ConcentratedAbility(abilityId, Math.max(0, cost), priority, true, onDisable, onEnable));
        }
        sortAbilities();
    }

    /**
     * Remove an ability entry.
     */
    public void removeAbility(@Nonnull String abilityId) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        int index = findAbilityIndex(abilityId);
        if (index >= 0) {
            abilities.remove(index);
        }
    }

    /**
     * Reorder an ability by setting a new priority value.
     */
    public void reorderAbility(@Nonnull String abilityId, int newPriority) {
        Objects.requireNonNull(abilityId, "abilityId cannot be null");
        int index = findAbilityIndex(abilityId);
        if (index >= 0) {
            ConcentratedAbility existing = abilities.get(index);
            abilities.set(index, existing.withPriority(newPriority));
            sortAbilities();
        }
    }

    /**
     * Get current concentration.
     */
    public int getCurrentConcentration() {
        return currentConcentration;
    }

    /**
     * Set current concentration (clamped at zero).
     */
    public void setCurrentConcentration(int value) {
        this.currentConcentration = Math.max(0, value);
    }

    public float getRegenRemainder() {
        return regenRemainder;
    }

    public void setRegenRemainder(float regenRemainder) {
        this.regenRemainder = regenRemainder;
    }

    public void resetRegenRemainder() {
        this.regenRemainder = 0f;
    }

    public void setTempAbilityIds(@Nullable String[] ids) {
        this.tempAbilityIds = ids;
    }

    public void setTempAbilityCosts(@Nullable int[] costs) {
        this.tempAbilityCosts = costs;
    }

    public void setTempAbilityPriorities(@Nullable int[] priorities) {
        this.tempAbilityPriorities = priorities;
    }

    public void setTempAbilityEnabled(@Nullable int[] enabled) {
        this.tempAbilityEnabled = enabled;
    }

    @Nullable
    public String[] getTempAbilityIds() {
        return tempAbilityIds;
    }

    @Nullable
    public int[] getTempAbilityCosts() {
        return tempAbilityCosts;
    }

    @Nullable
    public int[] getTempAbilityPriorities() {
        return tempAbilityPriorities;
    }

    @Nullable
    public int[] getTempAbilityEnabled() {
        return tempAbilityEnabled;
    }

    @Nonnull
    public String[] getAbilityIdsForSave() {
        String[] ids = new String[abilities.size()];
        for (int i = 0; i < abilities.size(); i++) {
            ids[i] = abilities.get(i).abilityId();
        }
        return ids;
    }

    @Nonnull
    public int[] getAbilityCostsForSave() {
        int[] costs = new int[abilities.size()];
        for (int i = 0; i < abilities.size(); i++) {
            costs[i] = abilities.get(i).cost();
        }
        return costs;
    }

    @Nonnull
    public int[] getAbilityPrioritiesForSave() {
        int[] priorities = new int[abilities.size()];
        for (int i = 0; i < abilities.size(); i++) {
            priorities[i] = abilities.get(i).priority();
        }
        return priorities;
    }

    @Nonnull
    public int[] getAbilityEnabledForSave() {
        int[] enabled = new int[abilities.size()];
        for (int i = 0; i < abilities.size(); i++) {
            enabled[i] = abilities.get(i).enabled() ? 1 : 0;
        }
        return enabled;
    }

    public void clearTempLoadData() {
        tempAbilityIds = null;
        tempAbilityCosts = null;
        tempAbilityPriorities = null;
        tempAbilityEnabled = null;
    }

    public void applyLoadedAbilities() {
        abilities.clear();
        if (tempAbilityIds == null || tempAbilityCosts == null || tempAbilityPriorities == null) {
            return;
        }
        int count = Math.min(tempAbilityIds.length,
                Math.min(tempAbilityCosts.length, tempAbilityPriorities.length));
        for (int i = 0; i < count; i++) {
            String id = tempAbilityIds[i];
            if (id == null || id.isBlank()) {
                continue;
            }
            int cost = tempAbilityCosts[i];
            int priority = tempAbilityPriorities[i];
            boolean enabled = tempAbilityEnabled == null
                    || i >= tempAbilityEnabled.length
                    || tempAbilityEnabled[i] != 0;
            abilities.add(new ConcentratedAbility(id, cost, priority, enabled, null, null));
        }
        sortAbilities();
    }

    @Override
    @Nonnull
    public ConcentrationPriorityComponent clone() {
        return new ConcentrationPriorityComponent(this);
    }

    private int findAbilityIndex(@Nonnull String abilityId) {
        for (int i = 0; i < abilities.size(); i++) {
            if (abilities.get(i).abilityId().equals(abilityId)) {
                return i;
            }
        }
        return -1;
    }

    private void sortAbilities() {
        abilities.sort(Comparator
                .comparingInt(ConcentratedAbility::priority)
                .reversed()
                .thenComparing(ConcentratedAbility::abilityId));
    }
}
