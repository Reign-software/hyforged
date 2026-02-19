package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.progression.event.CharacterLevelUpEvent;
import reign.software.hyforged.progression.event.ClassLevelUpEvent;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Map;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * System that applies stat modifiers from class and character level-ups.
 * <p>
 * Listens for {@link ClassLevelUpEvent} and {@link CharacterLevelUpEvent} events
 * and applies the corresponding ability score bonuses as permanent modifiers.
 * <p>
 * Modifier source format: "{classId}:level:{level}" for class bonuses
 * or "character:level:{level}" for character level bonuses.
 * <p>
 * This system runs independently of the ECS tick cycle - it's event-driven.
 */
public class ClassLevelModifierSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /**
     * Modifier source ID prefix for class level bonuses.
     */
    private static final String CLASS_LEVEL_SOURCE_PREFIX = "class-level:";

    /**
     * Modifier source ID prefix for character level bonuses.
     */
    private static final String CHARACTER_LEVEL_SOURCE_PREFIX = "character-level:";

    @SuppressWarnings("unused")
    private EventRegistration<Void, ClassLevelUpEvent> classLevelRegistration;
    @SuppressWarnings("unused")
    private EventRegistration<Void, CharacterLevelUpEvent> characterLevelRegistration;
    @SuppressWarnings("unused")
    private EventRegistration<Void, PlayerConnectEvent> playerConnectRegistration;
    @SuppressWarnings("unused")
    private EventRegistration<String, PlayerReadyEvent> playerReadyRegistration;

    private final com.hypixel.hytale.component.ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    private final com.hypixel.hytale.component.ComponentType<EntityStore, ProgressionComponent> progressionComponentType;

    public ClassLevelModifierSystem() {
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        this.statComponentType = plugin.getHyforgedStatComponentType();
        this.progressionComponentType = plugin.getProgressionComponentType();
        // Subscribe to level-up events
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        classLevelRegistration = HytaleServer.get().getEventBus()
                .register(ClassLevelUpEvent.class, this::onClassLevelUp);
        
        characterLevelRegistration = HytaleServer.get().getEventBus()
                .register(CharacterLevelUpEvent.class, this::onCharacterLevelUp);

        playerConnectRegistration = HytaleServer.get().getEventBus()
            .register(PlayerConnectEvent.class, this::onPlayerConnect);

        playerReadyRegistration = HytaleServer.get().getEventBus()
                .registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        
        LOGGER.atInfo().log("ClassLevelModifierSystem: Registered level-up event handlers");
    }

    /**
     * Rebuild class-level and character-level stat modifiers on connect from persisted progression.
     */
    private void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        if (event.getPlayerRef() == null) {
            return;
        }

        Ref<EntityStore> entityRef = event.getPlayerRef().getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Holder<EntityStore> holder = event.getHolder();
        holder.ensureComponent(statComponentType);
        holder.ensureComponent(progressionComponentType);

        HyforgedStatComponent statComponent = holder.getComponent(statComponentType);
        ProgressionComponent progression = holder.getComponent(progressionComponentType);
        if (statComponent == null || progression == null) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        EntityStatMap statMap = StatAccessor.getStatMap(store, entityRef);
        int totalApplied = reapplyProgressionLevelBonuses(entityRef, progression, statComponent, statMap);
        if (totalApplied > 0) {
            LOGGER.at(Level.FINE).log(
                "ClassLevelModifierSystem: Reapplied %d progression-level modifiers on connect for %s",
                totalApplied,
                event.getPlayerRef().getUsername()
            );
        }
    }

    /**
     * Rebuild class-level and character-level modifiers once the player is fully ready.
     * This catches cases where progression data is finalized after connect.
     */
    private void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        HyforgedStatComponent statComponent = store.getComponent(entityRef, statComponentType);
        ProgressionComponent progression = store.getComponent(entityRef, progressionComponentType);
        if (statComponent == null || progression == null) {
            return;
        }

        EntityStatMap statMap = StatAccessor.getStatMap(store, entityRef);
        int totalApplied = reapplyProgressionLevelBonuses(entityRef, progression, statComponent, statMap);
        if (totalApplied > 0) {
            LOGGER.at(Level.FINE).log(
                    "ClassLevelModifierSystem: Reapplied %d progression-level modifiers on ready",
                    totalApplied
            );
        }
    }

    private int reapplyProgressionLevelBonuses(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull ProgressionComponent progression,
            @Nonnull HyforgedStatComponent statComponent,
            EntityStatMap statMap
    ) {
        if (statMap != null) {
            StatAccessor.removeAllModifiersByKeyPrefix(statMap, CLASS_LEVEL_SOURCE_PREFIX);
            StatAccessor.removeAllModifiersByKeyPrefix(statMap, CHARACTER_LEVEL_SOURCE_PREFIX);
        }

        int removedComponentModifiers = statComponent.removeModifiersIf(
                modifier -> modifier.getSourceType() == HyforgedModifier.SourceType.CLASS
                        && modifier.getSourceId() != null
                        && (modifier.getSourceId().startsWith(CLASS_LEVEL_SOURCE_PREFIX)
                        || modifier.getSourceId().startsWith(CHARACTER_LEVEL_SOURCE_PREFIX)),
                modifier -> {
                }
        );
        if (removedComponentModifiers > 0) {
            statComponent.markAllDirty();
        }

        int classApplied = reapplyClassLevelBonuses(entityRef, progression, statComponent, statMap);
        int characterApplied = reapplyCharacterLevelBonuses(entityRef, progression, statComponent, statMap);
        return classApplied + characterApplied;
    }

    /**
     * Handle class level-up events by applying ability score bonuses.
     *
     * @param event The class level-up event
     */
    private void onClassLevelUp(@Nonnull ClassLevelUpEvent event) {
        Map<String, Integer> abilityBonuses = event.abilityBonuses();
        if (abilityBonuses.isEmpty()) {
            return;
        }
        
        Ref<EntityStore> entityRef = event.entityRef();
        String classId = event.classId();
        int newLevel = event.newLevel();
        
        LOGGER.at(Level.FINE).log(
                "ClassLevelModifierSystem: Applying %d ability bonuses for %s level %d",
                abilityBonuses.size(), classId, newLevel);
        
        // Get store from ref - verify entity is still valid
        if (!entityRef.isValid()) {
            LOGGER.atWarning().log("ClassLevelModifierSystem: Invalid entity ref for level-up");
            return;
        }
        Store<EntityStore> store = entityRef.getStore();
        
        HyforgedStatComponent statComponent = store.getComponent(
            entityRef,
            HyforgedPlugin.getInstance().getHyforgedStatComponentType()
        );
        if (statComponent == null) {
            EntityStatMap statMap = StatAccessor.getStatMap(store, entityRef);
            if (statMap == null) {
                LOGGER.at(Level.FINE).log("ClassLevelModifierSystem: Entity has no stat component or stat map");
                return;
            }
        }

        EntityStatMap statMap = StatAccessor.getStatMap(store, entityRef);
        if (statComponent == null && statMap == null) {
            LOGGER.at(Level.FINE).log("ClassLevelModifierSystem: Entity has no stat component or stat map");
            return;
        }
        
        // Apply each ability bonus as a modifier
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        int applied = 0;
        for (Map.Entry<String, Integer> entry : abilityBonuses.entrySet()) {
            String abilityId = entry.getKey();
            int bonusValue = entry.getValue();
            
            if (bonusValue == 0) {
                continue;
            }
            
            // Find the stat index for this ability
            StatId statId = StatId.parse(abilityId);
            int statIndex = registry.getIndex(statId);
            if (statIndex < 0) {
                LOGGER.atWarning().log(
                        "ClassLevelModifierSystem: Unknown ability stat '%s' for class %s",
                        abilityId, classId);
                continue;
            }
            
            // Create the source ID for this level's bonus
            String sourceId = CLASS_LEVEL_SOURCE_PREFIX + classId + ":" + newLevel + ":" + abilityId;
            
            HyforgedModifier modifier = HyforgedModifier.builder()
                    .sourceId(sourceId)
                    .sourceType(HyforgedModifier.SourceType.CLASS)
                    .flat(bonusValue)
                    .targetStat(statIndex)
                    .permanent()
                    .build();
            
            // Prefer HyforgedStatComponent so dirty flags are set and values recompute immediately.
            if (statComponent != null) {
                statComponent.upsertModifier(modifier);
                applied++;
            } else if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                // Fallback for edge cases where stat component is unavailable.
                statMap.putModifier(statIndex, sourceId, modifier);
                applied++;
            }
        }

        if (applied > 0 && statComponent != null) {
            statComponent.markAllDirty();
        }
    }

    private int reapplyClassLevelBonuses(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull ProgressionComponent progression,
            @Nonnull HyforgedStatComponent statComponent,
            EntityStatMap statMap
    ) {
        StatDefinitionRegistry statRegistry = StatDefinitionRegistry.get();
        ClassDefinitionRegistry classRegistry = ClassDefinitionRegistry.get();
        int applied = 0;

        for (String classId : progression.getClassIds()) {
            ProgressionComponent.ClassProgressionData classData = progression.getClassProgression(classId);
            if (classData == null || classData.level <= 0) {
                continue;
            }

            ClassDefinition classDef = classRegistry.get(classId);
            if (classDef == null || !classDef.hasLevelRewards()) {
                continue;
            }

            for (int level = 1; level <= classData.level; level++) {
                Map<StatId, Integer> levelReward = classDef.getLevelReward(level);
                if (levelReward.isEmpty()) {
                    continue;
                }

                for (Map.Entry<StatId, Integer> rewardEntry : levelReward.entrySet()) {
                    int bonusValue = rewardEntry.getValue();
                    if (bonusValue == 0) {
                        continue;
                    }

                    StatId statId = rewardEntry.getKey();
                    int statIndex = statRegistry.getIndex(statId);
                    if (statIndex < 0) {
                        LOGGER.atWarning().log(
                                "ClassLevelModifierSystem: Unknown ability stat '%s' while restoring class '%s' level %d",
                                statId.fullId(), classId, level
                        );
                        continue;
                    }

                    String sourceId = CLASS_LEVEL_SOURCE_PREFIX + classId + ":" + level + ":" + statId.fullId();
                    HyforgedModifier modifier = HyforgedModifier.builder()
                            .sourceId(sourceId)
                            .sourceType(HyforgedModifier.SourceType.CLASS)
                            .flat(bonusValue)
                            .targetStat(statIndex)
                            .permanent()
                            .build();

                    if (statComponent != null) {
                        statComponent.upsertModifier(modifier);
                        applied++;
                    } else if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                        statMap.putModifier(statIndex, sourceId, modifier);
                        applied++;
                    }
                }
            }
        }

        if (applied > 0) {
            statComponent.markAllDirty();
        }

        return applied;
    }

    private int reapplyCharacterLevelBonuses(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull ProgressionComponent progression,
            @Nonnull HyforgedStatComponent statComponent,
            EntityStatMap statMap
    ) {
        int characterLevel = progression.getCharacterLevel();
        if (characterLevel <= 0) {
            return 0;
        }

        ClassDefinition defaultClass = ClassDefinitionRegistry.get().getDefault();
        if (defaultClass == null || !defaultClass.hasLevelRewards()) {
            return 0;
        }

        StatDefinitionRegistry statRegistry = StatDefinitionRegistry.get();
        int applied = 0;
        for (int level = 1; level <= characterLevel; level++) {
            Map<StatId, Integer> levelReward = defaultClass.getLevelReward(level);
            if (levelReward.isEmpty()) {
                continue;
            }

            for (Map.Entry<StatId, Integer> rewardEntry : levelReward.entrySet()) {
                int bonusValue = rewardEntry.getValue();
                if (bonusValue == 0) {
                    continue;
                }

                StatId statId = rewardEntry.getKey();
                int statIndex = statRegistry.getIndex(statId);
                if (statIndex < 0) {
                    LOGGER.atWarning().log(
                            "ClassLevelModifierSystem: Unknown ability stat '%s' while restoring character level %d",
                            statId.fullId(),
                            level
                    );
                    continue;
                }

                String sourceId = CHARACTER_LEVEL_SOURCE_PREFIX + level + ":" + statId.fullId();
                HyforgedModifier modifier = HyforgedModifier.builder()
                        .sourceId(sourceId)
                        .sourceType(HyforgedModifier.SourceType.CLASS)
                        .flat(bonusValue)
                        .targetStat(statIndex)
                        .permanent()
                        .build();

                if (statComponent != null) {
                    statComponent.upsertModifier(modifier);
                    applied++;
                } else if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                    statMap.putModifier(statIndex, sourceId, modifier);
                    applied++;
                }
            }
        }

        if (applied > 0) {
            statComponent.markAllDirty();
        }

        return applied;
    }

    /**
     * Handle character level-up events.
     * <p>
     * Currently character level-ups don't grant ability bonuses directly
     * (those come from class progression), but this hook is available
     * for future expansion.
     * <p>
     * Note: With EntityStatMap, stat values auto-recompute when accessed,
     * so no explicit dirty flag is needed.
     *
     * @param event The character level-up event
     */
    private void onCharacterLevelUp(@Nonnull CharacterLevelUpEvent event) {
        if (event.levelsGained().isEmpty()) {
            return;
        }

        Ref<EntityStore> entityRef = event.entityRef();
        if (!entityRef.isValid()) {
            LOGGER.atWarning().log("ClassLevelModifierSystem: Invalid entity ref for character level-up");
            return;
        }

        Store<EntityStore> store = entityRef.getStore();
        HyforgedStatComponent statComponent = store.getComponent(
                entityRef,
                HyforgedPlugin.getInstance().getHyforgedStatComponentType()
        );
        EntityStatMap statMap = StatAccessor.getStatMap(store, entityRef);

        if (statComponent == null && statMap == null) {
            LOGGER.at(Level.FINE).log("ClassLevelModifierSystem: Entity has no stat component or stat map");
            return;
        }

        ClassDefinition defaultClass = ClassDefinitionRegistry.get().getDefault();
        if (defaultClass == null || !defaultClass.hasLevelRewards()) {
            return;
        }

        StatDefinitionRegistry statRegistry = StatDefinitionRegistry.get();
        int applied = 0;
        for (int level : event.levelsGained()) {
            Map<StatId, Integer> levelReward = defaultClass.getLevelReward(level);
            if (levelReward.isEmpty()) {
                continue;
            }

            for (Map.Entry<StatId, Integer> rewardEntry : levelReward.entrySet()) {
                int bonusValue = rewardEntry.getValue();
                if (bonusValue == 0) {
                    continue;
                }

                StatId statId = rewardEntry.getKey();
                int statIndex = statRegistry.getIndex(statId);
                if (statIndex < 0) {
                    LOGGER.atWarning().log(
                            "ClassLevelModifierSystem: Unknown ability stat '%s' for character level %d",
                            statId.fullId(),
                            level
                    );
                    continue;
                }

                String sourceId = CHARACTER_LEVEL_SOURCE_PREFIX + level + ":" + statId.fullId();
                HyforgedModifier modifier = HyforgedModifier.builder()
                        .sourceId(sourceId)
                        .sourceType(HyforgedModifier.SourceType.CLASS)
                        .flat(bonusValue)
                        .targetStat(statIndex)
                        .permanent()
                        .build();

                if (statComponent != null) {
                    statComponent.upsertModifier(modifier);
                    applied++;
                } else if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                    statMap.putModifier(statIndex, sourceId, modifier);
                    applied++;
                }
            }
        }

        if (applied > 0 && statComponent != null) {
            statComponent.markAllDirty();
        }

        LOGGER.at(Level.FINE).log(
                "ClassLevelModifierSystem: Character level-up %d->%d applied %d character-level modifiers",
                event.oldLevel(),
                event.newLevel(),
                applied
        );
    }

    /**
     * Remove all class level modifiers for a specific class from an entity.
     * <p>
     * Called when a player changes their active class to clear old bonuses.
     *
     * @param statMap The entity's stat map
     * @param classId The class ID to remove bonuses for
     * @return The number of modifiers removed
     */
    public static int removeClassModifiers(
            @Nonnull EntityStatMap statMap,
            @Nonnull String classId
    ) {
        String prefix = CLASS_LEVEL_SOURCE_PREFIX + classId + ":";
        return StatAccessor.removeAllModifiersByKeyPrefix(statMap, prefix);
    }

    /**
     * Remove all class level modifiers from an entity.
     * <p>
     * Called during progression reset.
     *
     * @param statMap The entity's stat map
     * @return The number of modifiers removed
     */
    public static int removeAllClassModifiers(@Nonnull EntityStatMap statMap) {
        return StatAccessor.removeAllModifiersByKeyPrefix(statMap, CLASS_LEVEL_SOURCE_PREFIX);
    }
}
