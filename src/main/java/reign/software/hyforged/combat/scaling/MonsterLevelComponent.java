package reign.software.hyforged.combat.scaling;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * ECS Component storing the computed monster level for an NPC entity.
 * <p>
 * This component is attached to NPC entities by the {@link HyforgedMonsterScalingSystem}
 * when they spawn. It caches the monster's level so it doesn't need to be recalculated.
 * <p>
 * Monster level is determined by distance from world spawn at the time of spawning.
 */
public class MonsterLevelComponent implements Component<EntityStore> {

    /** The monster's combat level */
    private int level;

    /** Whether the level-based stat modifiers have been applied */
    private boolean statsApplied;

    /**
     * Default constructor required for ECS.
     */
    public MonsterLevelComponent() {
        this.level = 1;
        this.statsApplied = false;
    }

    /**
     * Create a component with a specified level.
     * 
     * @param level The monster's level
     */
    public MonsterLevelComponent(int level) {
        this.level = level;
        this.statsApplied = false;
    }

    /**
     * Copy constructor for cloning.
     */
    public MonsterLevelComponent(@Nonnull MonsterLevelComponent other) {
        this.level = other.level;
        this.statsApplied = other.statsApplied;
    }

    /**
     * Get the monster's level.
     * 
     * @return The combat level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Set the monster's level.
     * 
     * @param level The new level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Check if level-based stat modifiers have been applied.
     */
    public boolean isStatsApplied() {
        return statsApplied;
    }

    /**
     * Mark that level-based stat modifiers have been applied.
     */
    public void setStatsApplied(boolean applied) {
        this.statsApplied = applied;
    }

    @Nonnull
    @Override
    public MonsterLevelComponent clone() {
        return new MonsterLevelComponent(this);
    }
}
