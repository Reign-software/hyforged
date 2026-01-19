package reign.software.hyforged.stats.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.stats.CoreStats;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.component.HyforgedStatComponent;
import reign.software.hyforged.stats.component.ModifierSource;
import reign.software.hyforged.stats.component.ModifierType;
import reign.software.hyforged.stats.component.StatModifier;

import javax.annotation.Nonnull;

/**
 * ECS System for initializing entities with HyforgedStatComponent.
 * <p>
 * This RefSystem handles entity lifecycle events:
 * - onEntityAdded: Initialize default ability scores and add base modifiers
 * - onEntityRemove: Clean up any external state if needed
 * <p>
 * Following ECS principles, this system contains only processing logic.
 * All data is stored in the HyforgedStatComponent.
 */
public class HyforgedStatInitSystem extends RefSystem<EntityStore> {

    @Nonnull
    private final ComponentType<EntityStore, HyforgedStatComponent> statComponentType;
    
    @Nonnull
    private final Query<EntityStore> query;

    /**
     * Default base value for ability scores when initializing a new entity.
     */
    private static final int DEFAULT_ABILITY_SCORE = 10;

    public HyforgedStatInitSystem() {
        this.statComponentType = HyforgedPlugin.getInstance().getHyforgedStatComponentType();
        this.query = statComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        HyforgedStatComponent component = commandBuffer.getComponent(ref, statComponentType);
        if (component == null) {
            return;
        }

        // Initialize default ability scores if not already set
        initializeAbilityScores(component);
        
        // Add ability score modifiers to derived stats
        applyAbilityScoreModifiers(component);
        
        // Mark all stats dirty for initial computation
        component.markAllDirty();
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed - component data is disposed with entity
        // If we had external references (e.g., party stat sharing), clean them here
    }

    /**
     * Initialize ability scores to default values if they haven't been set.
     */
    private void initializeAbilityScores(@Nonnull HyforgedStatComponent component) {
        int[] currentScores = component.getAbilityScores();
        boolean needsInit = true;
        
        // Check if scores are already initialized (non-zero)
        for (int score : currentScores) {
            if (score != 0) {
                needsInit = false;
                break;
            }
        }
        
        if (needsInit) {
            int[] defaults = new int[7];
            for (int i = 0; i < 7; i++) {
                defaults[i] = DEFAULT_ABILITY_SCORE;
            }
            component.setAbilityScores(defaults);
        }
    }

    /**
     * Apply ability score modifiers to all derived stats.
     * <p>
     * Each ability score contributes FLAT bonuses to its derived stats:
     * - STR → Attack Power
     * - DEX → Evasion, Accuracy
     * - INT → Spell Power
     * - CON → Max health
     * - WIS → Max mana (via spirit derivation)
     * - SPI → Max mana, stamina
     * - LCK → Critical chance
     * <p>
     * The formulas use score - 10 to get the modifier from base.
     * Each point above 10 grants a bonus to primary derived stats.
     */
    private void applyAbilityScoreModifiers(@Nonnull HyforgedStatComponent component) {
        // Remove existing ability score modifiers to avoid stacking
        component.removeModifiersBySourceType(ModifierSource.ABILITY_SCORE);
        
        int[] scores = component.getAbilityScores();
        StatDefinitionRegistry registry = StatDefinitionRegistry.get();
        
        // STR (index 0) → Attack Power
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.STRENGTH),
                registry.getIndex(CoreStats.ATTACK_POWER), scores[0], 2);
        
        // DEX (index 1) → Evasion Rating, Accuracy Rating
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.DEXTERITY),
                registry.getIndex(CoreStats.EVASION_RATING), scores[1], 2);
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.DEXTERITY),
                registry.getIndex(CoreStats.ACCURACY_RATING), scores[1], 1);
        
        // INT (index 2) → Spell Power
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.INTELLIGENCE),
                registry.getIndex(CoreStats.SPELL_POWER), scores[2], 2);
        
        // CON (index 3) → Max Health
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.CONSTITUTION),
                registry.getIndex(CoreStats.MAX_HEALTH_FLAT), scores[3], 5);
        
        // WIS (index 4) → Cooldown Recovery Rate (bonus to cooldowns)
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.WISDOM),
                registry.getIndex(CoreStats.COOLDOWN_RECOVERY_RATE_BPS), scores[4], 10);
        
        // SPI (index 5) → Max Mana and Max Stamina
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.SPIRIT),
                registry.getIndex(CoreStats.MAX_MANA_FLAT), scores[5], 3);
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.SPIRIT),
                registry.getIndex(CoreStats.MAX_STAMINA_FLAT), scores[5], 2);
        
        // LCK (index 6) → Crit Chance (in basis points, 10 = 1%)
        applyAbilityModifier(component, registry,
                registry.getIndex(CoreStats.LUCK),
                registry.getIndex(CoreStats.CRIT_CHANCE_BPS), scores[6], 10);
    }

    /**
     * Apply a single ability score modifier to a derived stat.
     *
     * @param component The stat component
     * @param registry The stat definition registry
     * @param abilityStatIndex The index of the ability score stat
     * @param derivedStatIndex The index of the derived stat to modify
     * @param abilityValue The current ability score value
     * @param multiplier How much each point above 10 contributes
     */
    private void applyAbilityModifier(
            @Nonnull HyforgedStatComponent component,
            @Nonnull StatDefinitionRegistry registry,
            int abilityStatIndex,
            int derivedStatIndex,
            int abilityValue,
            int multiplier
    ) {
        if (derivedStatIndex < 0) {
            return; // Stat not registered
        }
        
        // Calculate modifier value: (score - 10) * multiplier
        int modifierValue = (abilityValue - 10) * multiplier;
        
        if (modifierValue != 0) {
            StatModifier modifier = new StatModifier(
                    "ability_score_" + abilityStatIndex,  // sourceId
                    ModifierSource.ABILITY_SCORE,          // sourceType
                    ModifierType.FLAT,                     // modifierType
                    derivedStatIndex,                      // targetStatIndex
                    null,                                  // targetTagId (not a tag modifier)
                    modifierValue,                         // value
                    0,                                     // expirationTick (0 = permanent)
                    0                                      // priority
            );
            component.addModifier(modifier);
        }
    }
}
