package reign.software.hyforged.minion;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a minion type, loaded from JSON.
 * <p>
 * All values are data-driven from {@code Server/Hyforged/Minions/*.json}.
 * No hard-coded defaults — if a field is missing from JSON the loader
 * supplies explicit defaults.
 */
public final class MinionDefinition {

    private final String id;
    private final String npcTemplate;
    private final int concentrationCost;
    private final int defaultPriority;
    private final int baseDuration;
    private final float spawnOffsetX;
    private final float spawnOffsetY;
    private final float spawnOffsetZ;
    private final List<String> tags;
    private final Map<String, Integer> statOverrides;

    /**
     * Construct with explicit stat overrides.
     */
    public MinionDefinition(
            @Nonnull String id,
            @Nonnull String npcTemplate,
            int concentrationCost,
            int defaultPriority,
            int baseDuration,
            float spawnOffsetX,
            float spawnOffsetY,
            float spawnOffsetZ,
            @Nonnull List<String> tags,
            @Nonnull Map<String, Integer> statOverrides
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.npcTemplate = Objects.requireNonNull(npcTemplate, "npcTemplate cannot be null");
        this.concentrationCost = Math.max(0, concentrationCost);
        this.defaultPriority = defaultPriority;
        this.baseDuration = Math.max(0, baseDuration);
        this.spawnOffsetX = spawnOffsetX;
        this.spawnOffsetY = spawnOffsetY;
        this.spawnOffsetZ = spawnOffsetZ;
        this.tags = List.copyOf(Objects.requireNonNull(tags, "tags cannot be null"));
        this.statOverrides = Map.copyOf(Objects.requireNonNull(statOverrides, "statOverrides cannot be null"));
    }

    /**
     * Backwards-compatible constructor with empty stat overrides.
     */
    public MinionDefinition(
            @Nonnull String id,
            @Nonnull String npcTemplate,
            int concentrationCost,
            int defaultPriority,
            int baseDuration,
            float spawnOffsetX,
            float spawnOffsetY,
            float spawnOffsetZ,
            @Nonnull List<String> tags
    ) {
        this(id, npcTemplate, concentrationCost, defaultPriority, baseDuration,
                spawnOffsetX, spawnOffsetY, spawnOffsetZ, tags, Collections.emptyMap());
    }

    /** Namespaced ID, e.g. {@code "hyforged:skeleton-warrior"}. */
    @Nonnull
    public String getId() {
        return id;
    }

    /** Hytale NPC role/template name, e.g. {@code "Skeleton"}. */
    @Nonnull
    public String getNpcTemplate() {
        return npcTemplate;
    }

    /** Concentration cost to maintain this minion. */
    public int getConcentrationCost() {
        return concentrationCost;
    }

    /** Default priority for concentration ordering. */
    public int getDefaultPriority() {
        return defaultPriority;
    }

    /** Base duration in seconds (0 = permanent until released). */
    public int getBaseDuration() {
        return baseDuration;
    }

    /** Spawn offset X relative to summoner position. */
    public float getSpawnOffsetX() {
        return spawnOffsetX;
    }

    /** Spawn offset Y relative to summoner position. */
    public float getSpawnOffsetY() {
        return spawnOffsetY;
    }

    /** Spawn offset Z relative to summoner position. */
    public float getSpawnOffsetZ() {
        return spawnOffsetZ;
    }

    /** Unmodifiable list of tags for this minion type. */
    @Nonnull
    public List<String> getTags() {
        return tags;
    }

    /** Stat overrides keyed by stat ID (e.g. {@code "hyforged:minion-damage-bps"} &rarr; 500). */
    @Nonnull
    public Map<String, Integer> getStatOverrides() {
        return statOverrides;
    }

    @Override
    public String toString() {
        return "MinionDefinition{id='" + id + "', npcTemplate='" + npcTemplate
                + "', concentrationCost=" + concentrationCost
                + ", baseDuration=" + baseDuration
                + ", statOverrides=" + statOverrides + "}";
    }
}
