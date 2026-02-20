package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.Color;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.combat.hud.CombatLogHudSystem;
import reign.software.hyforged.progression.CharacterProgression;
import reign.software.hyforged.progression.ClassProgression;
import reign.software.hyforged.progression.component.ProgressionComponent;
import reign.software.hyforged.progression.event.CharacterLevelUpEvent;
import reign.software.hyforged.progression.event.ClassLevelUpEvent;
import reign.software.hyforged.progression.event.LevelUpNotificationEvent;
import reign.software.hyforged.stats.StatId;
import reign.software.hyforged.stats.StatAccessor;
import reign.software.hyforged.stats.StatDefinitionRegistry;
import reign.software.hyforged.stats.asset.ClassDefinition;
import reign.software.hyforged.stats.asset.ClassDefinitionRegistry;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * ECS event system that processes XP awards from any source.
 * <p>
 * Handles:
 * - Adding character XP to ProgressionComponent
 * - Adding class XP if entity has an active class
 * - Detecting level-up thresholds
 * - Audit logging of all XP awards
 * <p>
 * XP is server-authoritative - only systems can dispatch XPAwardEvents.
 */
public class XPAwardSystem extends EntityEventSystem<EntityStore, XPAwardEvent> {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private final ComponentType<EntityStore, ProgressionComponent> progressionComponentType;
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    // Cached index for hyforged:experience-gain-bps (Step 5.1)
    private int experienceGainBpsIndex = -1;
    private boolean xpBonusIndexCached = false;
    
    public XPAwardSystem() {
        super(XPAwardEvent.class);
        this.progressionComponentType = HyforgedPlugin.getInstance().getProgressionComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return progressionComponentType;
    }
    
    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull XPAwardEvent event
    ) {
        ProgressionComponent progression = archetypeChunk.getComponent(index, progressionComponentType);
        if (progression == null) {
            return;
        }
        
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        ensureXpBonusIndexCached();
        
        // ========== CHARACTER XP ==========
        long charXpAmount = event.getCharacterXpAmount();
        charXpAmount = applyXpGainBonus(charXpAmount, store, entityRef);
        
        // Don't award XP if at max level
        if (progression.getCharacterLevel() >= CharacterProgression.MAX_LEVEL) {
            charXpAmount = 0;
        }
        
        int oldCharLevel = progression.getCharacterLevel();
        
        if (charXpAmount > 0) {
            progression.addCharacterXp(charXpAmount);
            
            // Check for character level-up and emit event
            checkCharacterLevelUp(progression, oldCharLevel, entityRef);
        }
        
        // ========== CLASS XP ==========
        String activeClassId = progression.getActiveClassId();
        long classXpAmount = event.getClassXpAmount();
        classXpAmount = applyXpGainBonus(classXpAmount, store, entityRef);
        
        if (activeClassId != null && classXpAmount > 0) {
            ProgressionComponent.ClassProgressionData classData = progression.getOrCreateClassProgression(activeClassId);
            
            // Don't award XP if at max class level
            if (classData.level < ClassProgression.MAX_LEVEL) {
                int oldClassLevel = classData.level;
                classData.xp += classXpAmount;
                progression.markDirty();
                
                // Check for class level-up and emit event
                checkClassLevelUp(progression, activeClassId, classData, oldClassLevel, entityRef);
                
                if (LOGGER.at(Level.FINE).isEnabled()) {
                    LOGGER.at(Level.FINE).log("Awarded %d class XP to class '%s' for entity %s",
                            classXpAmount, activeClassId, entityRef);
                }
            }
        }
        
        // ========== NOTIFICATION AGGREGATION ==========
        if (charXpAmount > 0 || (activeClassId != null && classXpAmount > 0)) {
            XPNotificationAggregator.recordXPGain(
                    store,
                    entityRef,
                    charXpAmount,
                    activeClassId != null ? classXpAmount : 0,
                    event.getSource(),
                    activeClassId
            );
        }

        // ========== PER-KILL COMBAT LOG WITH MOB INFO ==========
        if (event.getSource() == XPSource.COMBAT && event.getSourceDisplayName() != null) {
            addCombatKillXpLine(archetypeChunk, index, event, charXpAmount);
        }
        
        // ========== AUDIT LOGGING ==========
        if (charXpAmount > 0 || (activeClassId != null && classXpAmount > 0)) {
            LOGGER.atInfo().log("XP Award: entity=%s, source=%s, charXP=%d, classXP=%d, class=%s",
                    entityRef,
                    event.getSourceDescription(),
                    charXpAmount,
                    activeClassId != null ? classXpAmount : 0,
                    activeClassId != null ? activeClassId : "none");
        }
    }

    // ========== STEP 5.1: experience-gain-bps multiplier ==========

    /**
     * Apply the hyforged:experience-gain-bps multiplier to a base XP amount.
     * Returns the base amount unchanged if the stat is absent or zero.
     *
     * @param base      Base XP amount to scale
     * @param store     The entity store
     * @param entityRef Reference to the entity receiving XP
     * @return Scaled XP amount (rounded to nearest long)
     */
    private long applyXpGainBonus(long base, Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (base <= 0 || experienceGainBpsIndex < 0) return base;
        int bps = StatAccessor.getStatValueInt(store, entityRef, experienceGainBpsIndex);
        return bps == 0 ? base : Math.round(base * (1.0 + bps / 10000.0));
    }

    /**
     * Lazily resolve hyforged:experience-gain-bps stat index (called once per system lifetime).
     */
    private void ensureXpBonusIndexCached() {
        if (xpBonusIndexCached) return;
        experienceGainBpsIndex = StatDefinitionRegistry.get().getIndex(
                StatId.hyforged("experience-gain-bps"));
        xpBonusIndexCached = true;
    }

    /**
     * Add a per-kill combat log line with mob info when XP is gained from a combat kill.
     * <p>
     * Format: {@code +{xp} XP ([Quality] {mobName} Lv.{level})} with quality colored by tier.
     */
    private void addCombatKillXpLine(
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            int index,
            @Nonnull XPAwardEvent event,
            long charXpAmount
    ) {
        UUIDComponent uuidComp = archetypeChunk.getComponent(index, uuidComponentType);
        if (uuidComp == null) {
            return;
        }
        UUID playerUuid = uuidComp.getUuid();

        Message line = Message.raw("");

        // XP amount in blue
        line.insert(Message.raw("+" + charXpAmount + " XP").color("#55AAFF"));

        // Mob info in parentheses
        String mobName = event.getSourceDisplayName();
        int mobLevel = event.getSourceLevel();
        String quality = event.getSourceQuality();
        if (mobName != null) {
            line.insert(Message.raw(" (").color("#888888"));

            // Quality tag with tier-appropriate color
            if (quality != null && !quality.isBlank() && !"Common".equalsIgnoreCase(quality)) {
                String qualityColor = resolveQualityColor(quality);
                line.insert(Message.raw("[" + capitalizeFirst(quality) + "] ").color(qualityColor));
            }

            line.insert(Message.raw(mobName).color("#FF6666"));
            if (mobLevel > 0) {
                line.insert(Message.raw(" Lv." + mobLevel).color("#CCCCCC"));
            }
            line.insert(Message.raw(")").color("#888888"));
        }

        CombatLogHudSystem.addExtraLine(playerUuid, line);
    }

    /**
     * Resolve the hex color for a quality tier from Hytale's ItemQuality asset registry.
     * Falls back to a gray color if the quality is not found.
     */
    @Nonnull
    private static String resolveQualityColor(@Nonnull String qualityId) {
        try {
            ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityId);
            if (quality != null && quality.getTextColor() != null) {
                Color c = quality.getTextColor();
                return String.format("#%02X%02X%02X",
                        Byte.toUnsignedInt(c.red),
                        Byte.toUnsignedInt(c.green),
                        Byte.toUnsignedInt(c.blue));
            }
        } catch (Exception ignored) {
            // Asset registry may not be ready
        }
        return "#CCCCCC";
    }

    /**
     * Capitalize the first letter of a string.
     */
    @Nonnull
    private static String capitalizeFirst(@Nonnull String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
    
    /**
     * Check if character has leveled up and emit events.
     * Uses cumulative XP thresholds from the XP curve.
     */
    private void checkCharacterLevelUp(
            ProgressionComponent progression,
            int oldLevel,
            Ref<EntityStore> entityRef
    ) {
        reign.software.hyforged.progression.XPCurve curve = 
                reign.software.hyforged.progression.asset.XPCurveRegistry.get().getCharacterCurve();
        
        long totalXp = progression.getCharacterXp();
        
        // Determine level from total cumulative XP
        int newLevel = curve.getLevelForTotalXp(totalXp);
        newLevel = Math.min(newLevel, CharacterProgression.MAX_LEVEL);
        
        java.util.List<Integer> levelsGained = new java.util.ArrayList<>();
        if (newLevel > oldLevel) {
            for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
                levelsGained.add(lvl);
            }
            progression.setCharacterLevel(newLevel);
        }
        
        if (!levelsGained.isEmpty()) {
            int passivePoints = LevelUpProcessor.calculatePassivePointsForLevels(levelsGained);
            
            LOGGER.atInfo().log("Character level-up: %d -> %d, +%d passive points",
                    oldLevel, newLevel, passivePoints);
            
            // Emit level-up event
            CharacterLevelUpEvent event = new CharacterLevelUpEvent(
                    entityRef,
                    oldLevel,
                    newLevel,
                    levelsGained,
                    passivePoints
            );
            
            HytaleServer.get().getEventBus()
                    .dispatchFor(CharacterLevelUpEvent.class)
                    .dispatch(event);
            
            // Emit level-up notification event (bypasses rate limiting)
            LevelUpNotificationEvent notification = LevelUpNotificationEvent.character(
                    entityRef,
                    oldLevel,
                    newLevel,
                    levelsGained,
                    passivePoints
            );
            
            HytaleServer.get().getEventBus()
                    .dispatchFor(LevelUpNotificationEvent.class)
                    .dispatch(notification);
        }
    }
    
    /**
     * Check if class has leveled up and emit events.
     * Uses LevelUpProcessor for proper XP curve evaluation.
     */
    private void checkClassLevelUp(
            ProgressionComponent progression,
            String classId,
            ProgressionComponent.ClassProgressionData classData,
            int oldLevel,
            Ref<EntityStore> entityRef
    ) {
        reign.software.hyforged.progression.XPCurve curve = 
                reign.software.hyforged.progression.asset.XPCurveRegistry.get().getClassCurve();
        
        long totalXp = classData.xp;
        
        // Determine level from total cumulative XP
        int newLevel = curve.getLevelForTotalXp(totalXp);
        newLevel = Math.min(newLevel, ClassProgression.MAX_LEVEL);
        
        java.util.List<Integer> levelsGained = new java.util.ArrayList<>();
        if (newLevel > oldLevel) {
            for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
                levelsGained.add(lvl);
            }
            classData.level = newLevel;
            progression.markDirty();
        }
        
        if (!levelsGained.isEmpty()) {
            int classPassivePoints = LevelUpProcessor.calculateClassPassivePointsForLevels(levelsGained);
            
            // Calculate ability bonuses from ClassDefinition.levelRewards
            Map<String, Integer> abilityBonuses = calculateAbilityBonusesForLevels(classId, levelsGained);
            
            LOGGER.atInfo().log("Class level-up: %s %d -> %d, +%d class passive points, bonuses=%s",
                    classId, oldLevel, newLevel, classPassivePoints, abilityBonuses);
            
            // Emit level-up event
            ClassLevelUpEvent event = new ClassLevelUpEvent(
                    entityRef,
                    classId,
                    oldLevel,
                    newLevel,
                    levelsGained,
                    abilityBonuses,
                    classPassivePoints
            );
            
            HytaleServer.get().getEventBus()
                    .dispatchFor(ClassLevelUpEvent.class)
                    .dispatch(event);
            
            // Emit level-up notification event (bypasses rate limiting)
            LevelUpNotificationEvent notification = LevelUpNotificationEvent.classLevel(
                    entityRef,
                    classId,
                    oldLevel,
                    newLevel,
                    levelsGained,
                    classPassivePoints
            );
            
            HytaleServer.get().getEventBus()
                    .dispatchFor(LevelUpNotificationEvent.class)
                    .dispatch(notification);
        }
    }
    
    /**
     * Calculate the ability bonuses granted for the specified class levels.
     * 
     * @param classId the class ID to look up
     * @param levelsGained list of levels gained (not cumulative, just the new levels)
     * @return map of ability stat ID to total bonus from these levels
     */
    @Nonnull
    private Map<String, Integer> calculateAbilityBonusesForLevels(@Nonnull String classId, @Nonnull java.util.List<Integer> levelsGained) {
        ClassDefinition classDef = ClassDefinitionRegistry.get().get(classId);
        if (classDef == null || !classDef.hasLevelRewards()) {
            return java.util.Collections.emptyMap();
        }
        
        Map<String, Integer> bonuses = new HashMap<>();
        for (int level : levelsGained) {
            Map<StatId, Integer> levelReward = classDef.getLevelReward(level);
            for (Map.Entry<StatId, Integer> entry : levelReward.entrySet()) {
                String statKey = entry.getKey().fullId();
                bonuses.merge(statKey, entry.getValue(), Integer::sum);
            }
        }
        return bonuses;
    }
}
