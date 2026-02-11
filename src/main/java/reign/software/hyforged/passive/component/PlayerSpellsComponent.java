package reign.software.hyforged.passive.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * ECS Component holding granted spells for a player entity.
 * <p>
 * Spells are granted by passive tree nodes with spell-grant effects.
 * Multiple nodes can grant the same spell (tracked for refund purposes).
 * <p>
 * This is PURE DATA - no behavior, following ECS principles.
 * <p>
 * Example granted spells:
 * - hyforged:rallying-cry (Warcry skill)
 * - hyforged:blink (Movement skill)
 * - hyforged:fireball (Attack skill)
 */
public class PlayerSpellsComponent implements Component<EntityStore> {

    /** Schema version for persistence migration */
    public static final int SCHEMA_VERSION = 1;

    // ========== GRANTED SPELLS ==========

    /** Maps spell ID to set of source node IDs that grant this spell */
    private final Map<String, Set<String>> grantedSpells = new HashMap<>();

    // ========== PERSISTENCE ==========

    private int schemaVersion = SCHEMA_VERSION;
    private boolean dirty = false;

    // Temporary fields used during codec deserialization
    private String[] tempLoadSpellIds;
    private int[] tempLoadSpellSourceCounts;

    public PlayerSpellsComponent() {
        // Required for codec
    }

    /**
     * Copy constructor for clone().
     */
    public PlayerSpellsComponent(PlayerSpellsComponent other) {
        for (Map.Entry<String, Set<String>> entry : other.grantedSpells.entrySet()) {
            this.grantedSpells.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        this.schemaVersion = other.schemaVersion;
        this.dirty = other.dirty;
    }

    @Override
    public PlayerSpellsComponent clone() {
        return new PlayerSpellsComponent(this);
    }

    // ========== SPELL ACCESSORS ==========

    /**
     * Check if a spell is granted.
     *
     * @param spellId The spell ID to check
     * @return true if the spell is granted
     */
    public boolean hasSpell(@Nonnull String spellId) {
        return grantedSpells.containsKey(spellId);
    }

    /**
     * Get all granted spell IDs.
     */
    @Nonnull
    public Set<String> getGrantedSpells() {
        return Collections.unmodifiableSet(grantedSpells.keySet());
    }

    /**
     * Get the count of granted spells.
     */
    public int getSpellCount() {
        return grantedSpells.size();
    }

    /**
     * Grant a spell from a passive node.
     *
     * @param spellId The spell ID to grant
     * @param sourceNodeId The node ID that grants this spell
     * @return true if this is a newly granted spell
     */
    public boolean grantSpell(@Nonnull String spellId, @Nonnull String sourceNodeId) {
        boolean newSpell = !grantedSpells.containsKey(spellId);
        
        Set<String> sources = grantedSpells.computeIfAbsent(spellId, k -> new HashSet<>());
        sources.add(sourceNodeId);
        
        if (newSpell) {
            dirty = true;
        }
        return newSpell;
    }

    /**
     * Revoke a spell from a passive node.
     * The spell is only removed if no other nodes grant it.
     *
     * @param spellId The spell ID to potentially revoke
     * @param sourceNodeId The node ID that was granting this spell
     * @return true if the spell was revoked (no sources remain)
     */
    public boolean revokeSpell(@Nonnull String spellId, @Nonnull String sourceNodeId) {
        Set<String> sources = grantedSpells.get(spellId);
        if (sources == null) {
            return false;
        }
        
        sources.remove(sourceNodeId);
        
        if (sources.isEmpty()) {
            grantedSpells.remove(spellId);
            dirty = true;
            return true;
        }
        
        return false;
    }

    /**
     * Get the source nodes that grant a specific spell.
     *
     * @param spellId The spell ID
     * @return Set of source node IDs (empty if spell not granted)
     */
    @Nonnull
    public Set<String> getSpellSources(@Nonnull String spellId) {
        Set<String> sources = grantedSpells.get(spellId);
        return sources != null ? Collections.unmodifiableSet(sources) : Collections.emptySet();
    }

    /**
     * Clear all granted spells.
     */
    public void clearAllSpells() {
        if (!grantedSpells.isEmpty()) {
            grantedSpells.clear();
            dirty = true;
        }
    }

    // ========== TEMP LOAD HELPERS ==========

    /**
     * Set temporary spell IDs during codec deserialization.
     */
    public void setTempLoadSpellIds(@Nonnull String[] spellIds) {
        this.tempLoadSpellIds = spellIds;
    }

    /**
     * Get temporary spell IDs set during deserialization.
     */
    @Nullable
    public String[] getTempLoadSpellIds() {
        return tempLoadSpellIds;
    }

    /**
     * Set temporary spell source counts during codec deserialization.
     */
    public void setTempLoadSpellSourceCounts(@Nonnull int[] counts) {
        this.tempLoadSpellSourceCounts = counts;
    }

    /**
     * Get temporary spell source counts set during deserialization.
     */
    @Nullable
    public int[] getTempLoadSpellSourceCounts() {
        return tempLoadSpellSourceCounts;
    }

    /**
     * Clear temporary load data after deserialization is complete.
     */
    public void clearTempLoadData() {
        this.tempLoadSpellIds = null;
        this.tempLoadSpellSourceCounts = null;
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
