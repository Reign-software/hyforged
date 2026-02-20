package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinition;
import reign.software.hyforged.stats.StatDefinitionRegistry;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Static singleton helper for reading skill-level stat bonuses from entities.
 * <p>
 * This is <b>not</b> an ECS system — it is a utility accessed via {@link #get()}.
 * <p>
 * On first use (lazy discovery), this helper scans {@link StatDefinitionRegistry} for all
 * registered stat definitions whose {@code name} component ends with {@code "-skill-levels"}
 * (e.g., {@code hyforged:sword-skill-levels}, {@code hyforged:all-skill-levels}).
 * Stat discovery is fully data-driven: adding a new skill-level stat to
 * {@code Server/Hyforged/Stats/Definitions/} is sufficient — no Java changes required.
 * <p>
 * The {@link #getEffectiveSkillLevel} method returns:
 * <pre>
 *   baseLevel + allSkillLevelsBonus + tagSpecificBonus
 * </pre>
 * If no matching stats are defined, {@code baseLevel} is returned unchanged.
 *
 * <h3>Currently discovered skill-level stats</h3>
 * <p>
 * At the time of implementation no {@code *-skill-levels} JSON definitions exist in
 * {@code Server/Hyforged/Stats/Definitions/}. When they are added the discovery step will
 * pick them up automatically without code changes.
 */
public final class HyforgedSkillLevelSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Suffix that identifies a skill-level stat (e.g. {@code sword-skill-levels}). */
    private static final String SKILL_LEVELS_SUFFIX = "-skill-levels";

    /** The special "all skills" tag key. */
    private static final String ALL_SKILL_TAG = "all";

    private static final HyforgedSkillLevelSystem INSTANCE = new HyforgedSkillLevelSystem();

    /**
     * Map from skill tag (e.g. {@code "sword"}, {@code "fire"}) to the cached stat index
     * for {@code hyforged:<tag>-skill-levels}.
     */
    private final Map<String, Integer> skillTagToIndex = new HashMap<>();

    /**
     * Cached index for {@code hyforged:all-skill-levels}, or {@code -1} if not registered.
     */
    private int allSkillLevelsIndex = -1;

    private volatile boolean discovered = false;

    private HyforgedSkillLevelSystem() {
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static HyforgedSkillLevelSystem get() {
        return INSTANCE;
    }

    /**
     * Return the effective skill level for the entity at {@code entityIndex} in {@code chunk},
     * combining the base level with {@code all-skill-levels} and any tag-specific bonus.
     *
     * @param store       The entity store (used for store-based stat reads if needed)
     * @param chunk       The archetype chunk containing the entity
     * @param entityIndex The entity's position within the chunk
     * @param skillTag    The skill tag key to look up (e.g. {@code "sword"}, {@code "fire"})
     * @param baseLevel   The base skill level before any bonuses
     * @return {@code baseLevel + allSkillLevelsBonus + tagBonus}, or {@code baseLevel} if no
     *         skill-level stats are registered
     */
    public int getEffectiveSkillLevel(
            @Nonnull Store<EntityStore> store,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int entityIndex,
            @Nonnull String skillTag,
            int baseLevel
    ) {
        ensureDiscovered();

        int result = baseLevel;

        // Apply all-skill-levels bonus (applies regardless of skillTag)
        if (allSkillLevelsIndex >= 0) {
            result += StatAccessor.getStatValueInt(chunk, entityIndex, allSkillLevelsIndex);
        }

        // Apply tag-specific bonus (skip if skillTag is "all" — already handled above)
        if (!ALL_SKILL_TAG.equals(skillTag)) {
            Integer tagIndex = skillTagToIndex.get(skillTag);
            if (tagIndex != null && tagIndex >= 0) {
                result += StatAccessor.getStatValueInt(chunk, entityIndex, tagIndex);
            }
        }

        return result;
    }

    /**
     * Reset discovery state (useful after a stat registry reload or in tests).
     */
    public void reset() {
        synchronized (this) {
            discovered = false;
            skillTagToIndex.clear();
            allSkillLevelsIndex = -1;
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Lazily discover all {@code *-skill-levels} stats on first use.
     * Thread-safe via double-checked locking on {@link #discovered}.
     */
    private void ensureDiscovered() {
        if (discovered) {
            return;
        }
        synchronized (this) {
            if (discovered) {
                return;
            }
            discoverSkillLevelStats();
            discovered = true;
        }
    }

    private void discoverSkillLevelStats() {
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int found = 0;

        for (StatDefinition def : registry.getAllStats()) {
            String statName = def.id().name(); // e.g. "sword-skill-levels"
            if (!statName.endsWith(SKILL_LEVELS_SUFFIX)) {
                continue;
            }

            // Extract skill tag: strip the suffix to get the tag key
            // "all-skill-levels" → "all", "sword-skill-levels" → "sword"
            String tag = statName.substring(0, statName.length() - SKILL_LEVELS_SUFFIX.length());
            int statIndex = registry.getIndex(def.id());

            if (ALL_SKILL_TAG.equals(tag)) {
                allSkillLevelsIndex = statIndex;
            } else {
                skillTagToIndex.put(tag, statIndex);
            }

            found++;
            LOGGER.at(Level.FINE).log(
                    "[SkillLevel] Discovered skill-level stat: %s (index=%d, tag=%s)",
                    def.id().fullId(), statIndex, tag);
        }

        LOGGER.at(Level.INFO).log("[SkillLevel] Discovered %d skill-level stat(s).", found);
    }
}
