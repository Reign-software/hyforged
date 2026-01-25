package reign.software.hyforged.passive.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * ECS Component holding unlock flags for a player entity.
 * <p>
 * Unlock flags are set by passive tree nodes with unlock-flag effects.
 * They enable game mechanics, abilities, or other features.
 * <p>
 * This is PURE DATA - no behavior, following ECS principles.
 * <p>
 * Example unlock flags:
 * - hyforged:stun-immune (Cannot be stunned)
 * - hyforged:dual-wield (Can use two one-handed weapons)
 * - hyforged:aspect-of-spider (Gains spider-themed bonuses)
 */
public class PlayerUnlocksComponent implements Component<EntityStore> {

    /** Schema version for persistence migration */
    public static final int SCHEMA_VERSION = 1;

    // ========== UNLOCK FLAGS ==========

    /** Set of enabled unlock flag IDs */
    private final Set<String> unlockFlags = new HashSet<>();

    /** Maps flag ID to set of source node IDs that granted this flag */
    private final Map<String, Set<String>> flagSources = new HashMap<>();

    // ========== PERSISTENCE ==========

    private int schemaVersion = SCHEMA_VERSION;
    private boolean dirty = false;

    public PlayerUnlocksComponent() {
        // Required for codec
    }

    /**
     * Copy constructor for clone().
     */
    public PlayerUnlocksComponent(PlayerUnlocksComponent other) {
        this.unlockFlags.addAll(other.unlockFlags);
        
        for (Map.Entry<String, Set<String>> entry : other.flagSources.entrySet()) {
            this.flagSources.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        this.schemaVersion = other.schemaVersion;
        this.dirty = other.dirty;
    }

    @Override
    public PlayerUnlocksComponent clone() {
        return new PlayerUnlocksComponent(this);
    }

    // ========== UNLOCK FLAG ACCESSORS ==========

    /**
     * Check if an unlock flag is enabled.
     *
     * @param flagId The flag ID to check
     * @return true if the flag is enabled
     */
    public boolean hasFlag(@Nonnull String flagId) {
        return unlockFlags.contains(flagId);
    }

    /**
     * Get all enabled unlock flags.
     */
    @Nonnull
    public Set<String> getUnlockFlags() {
        return Collections.unmodifiableSet(unlockFlags);
    }

    /**
     * Get the count of enabled flags.
     */
    public int getFlagCount() {
        return unlockFlags.size();
    }

    /**
     * Enable an unlock flag from a passive node.
     *
     * @param flagId The flag ID to enable
     * @param sourceNodeId The node ID that grants this flag
     * @return true if the flag was newly enabled
     */
    public boolean enableFlag(@Nonnull String flagId, @Nonnull String sourceNodeId) {
        boolean newFlag = unlockFlags.add(flagId);
        
        Set<String> sources = flagSources.computeIfAbsent(flagId, k -> new HashSet<>());
        sources.add(sourceNodeId);
        
        if (newFlag) {
            dirty = true;
        }
        return newFlag;
    }

    /**
     * Disable an unlock flag from a passive node.
     * The flag is only removed if no other nodes grant it.
     *
     * @param flagId The flag ID to potentially disable
     * @param sourceNodeId The node ID that was granting this flag
     * @return true if the flag was disabled (no sources remain)
     */
    public boolean disableFlag(@Nonnull String flagId, @Nonnull String sourceNodeId) {
        Set<String> sources = flagSources.get(flagId);
        if (sources == null) {
            return false;
        }
        
        sources.remove(sourceNodeId);
        
        if (sources.isEmpty()) {
            flagSources.remove(flagId);
            unlockFlags.remove(flagId);
            dirty = true;
            return true;
        }
        
        return false;
    }

    /**
     * Get the source nodes that grant a specific flag.
     *
     * @param flagId The flag ID
     * @return Set of source node IDs (empty if flag not enabled)
     */
    @Nonnull
    public Set<String> getFlagSources(@Nonnull String flagId) {
        Set<String> sources = flagSources.get(flagId);
        return sources != null ? Collections.unmodifiableSet(sources) : Collections.emptySet();
    }

    /**
     * Clear all unlock flags.
     */
    public void clearAllFlags() {
        if (!unlockFlags.isEmpty()) {
            unlockFlags.clear();
            flagSources.clear();
            dirty = true;
        }
    }

    // ========== DIRTY FLAG ==========

    /**
     * Check if this component has been modified since last save.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Mark this component as clean (after saving).
     */
    public void clearDirty() {
        dirty = false;
    }

    /**
     * Mark this component as dirty (needs saving).
     */
    public void markDirty() {
        dirty = true;
    }

    // ========== SCHEMA VERSION ==========

    /**
     * Get the schema version for migration support.
     */
    public int getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * Set the schema version (used during deserialization).
     */
    public void setSchemaVersion(int version) {
        this.schemaVersion = version;
    }
}
