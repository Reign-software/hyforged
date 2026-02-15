package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.progression.event.CharacterLevelUpEvent;
import reign.software.hyforged.progression.event.ClassLevelUpEvent;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.logging.Logger;

/**
 * System that applies stat modifiers from class level-ups.
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

    private static final Logger LOGGER = Logger.getLogger(ClassLevelModifierSystem.class.getName());
    
    /**
     * Modifier source ID prefix for class level bonuses.
     */
    private static final String CLASS_LEVEL_SOURCE_PREFIX = "class-level:";

    @SuppressWarnings("unused")
    private EventRegistration<Void, ClassLevelUpEvent> classLevelRegistration;
    @SuppressWarnings("unused")
    private EventRegistration<Void, CharacterLevelUpEvent> characterLevelRegistration;

    public ClassLevelModifierSystem() {
        // Subscribe to level-up events
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        classLevelRegistration = HytaleServer.get().getEventBus()
                .register(ClassLevelUpEvent.class, this::onClassLevelUp);
        
        characterLevelRegistration = HytaleServer.get().getEventBus()
                .register(CharacterLevelUpEvent.class, this::onCharacterLevelUp);
        
        LOGGER.info("ClassLevelModifierSystem: Registered level-up event handlers");
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
        
        LOGGER.fine(() -> String.format(
                "ClassLevelModifierSystem: Applying %d ability bonuses for %s level %d",
                abilityBonuses.size(), classId, newLevel));
        
        // Get store from ref - verify entity is still valid
        if (!entityRef.isValid()) {
            LOGGER.warning("ClassLevelModifierSystem: Invalid entity ref for level-up");
            return;
        }
        Store<EntityStore> store = entityRef.getStore();
        
        EntityStatMap statMap = StatAccessor.getStatMap(store, entityRef);
        HyforgedStatComponent statComponent = store.getComponent(
            entityRef,
            HyforgedPlugin.getInstance().getHyforgedStatComponentType()
        );
        if (statMap == null && statComponent == null) {
            LOGGER.fine("ClassLevelModifierSystem: Entity has no stat map");
            return;
        }
        
        // Apply each ability bonus as a modifier
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
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
                LOGGER.warning(() -> String.format(
                        "ClassLevelModifierSystem: Unknown ability stat '%s' for class %s",
                        abilityId, classId));
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
            
            if (statMap != null && StatAccessor.hasStatSlot(statMap, statIndex)) {
                statMap.putModifier(statIndex, sourceId, modifier);
            } else if (statComponent != null) {
                statComponent.upsertModifier(modifier);
            }
        }
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
        // Character level-ups currently don't grant ability bonuses
        // The character level is used for combat effectiveness calculations
        // via the ProgressionStatBridge
        
        LOGGER.fine(() -> String.format(
                "ClassLevelModifierSystem: Character level-up to %d (no bonuses applied)",
                event.newLevel()));
        
        // Note: EntityStatMap auto-recomputes values when accessed,
        // so no explicit dirty marking is needed for effectiveness calculations
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
