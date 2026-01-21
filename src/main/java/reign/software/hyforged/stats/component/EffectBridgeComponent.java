package reign.software.hyforged.stats.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nonnull;

/**
 * ECS Component tracking which EntityEffects have been bridged to Hyforged stats.
 * <p>
 * This is pure data, following ECS principles. The {@code HyforgedEffectBridgeSystem}
 * uses this to detect effect additions and removals by comparing current active effects
 * to the tracked set.
 * <p>
 * Each effect index in {@code bridgedEffectIndices} represents an effect whose
 * stat modifiers have been mirrored to {@code HyforgedStatComponent}.
 */
public class EffectBridgeComponent implements Component<EntityStore> {

    /**
     * Set of effect indices that have been bridged to Hyforged stats.
     * <p>
     * When an effect is added, its index is added here and modifiers are created.
     * When an effect is removed, its index is removed and modifiers are cleaned up.
     */
    @Nonnull
    private final IntSet bridgedEffectIndices;

    /**
     * Create an empty effect bridge component.
     */
    public EffectBridgeComponent() {
        this.bridgedEffectIndices = new IntOpenHashSet();
    }

    /**
     * Copy constructor for component cloning.
     *
     * @param other The component to copy from
     */
    public EffectBridgeComponent(@Nonnull EffectBridgeComponent other) {
        this.bridgedEffectIndices = new IntOpenHashSet(other.bridgedEffectIndices);
    }

    /**
     * Get the set of effect indices that have been bridged.
     * <p>
     * Do not modify the returned set directly; use the add/remove methods.
     *
     * @return Read-only view of bridged effect indices
     */
    @Nonnull
    public IntSet getBridgedEffectIndices() {
        return bridgedEffectIndices;
    }

    /**
     * Check if an effect has been bridged.
     *
     * @param effectIndex The effect index to check
     * @return true if the effect is currently bridged
     */
    public boolean isBridged(int effectIndex) {
        return bridgedEffectIndices.contains(effectIndex);
    }

    /**
     * Mark an effect as bridged.
     *
     * @param effectIndex The effect index to mark
     */
    public void markBridged(int effectIndex) {
        bridgedEffectIndices.add(effectIndex);
    }

    /**
     * Unmark an effect as bridged.
     *
     * @param effectIndex The effect index to unmark
     */
    public void unmarkBridged(int effectIndex) {
        bridgedEffectIndices.remove(effectIndex);
    }

    /**
     * Clear all bridged effect indices.
     */
    public void clear() {
        bridgedEffectIndices.clear();
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new EffectBridgeComponent(this);
    }
}
