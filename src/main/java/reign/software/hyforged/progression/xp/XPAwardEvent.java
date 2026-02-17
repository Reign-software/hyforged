package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ECS event for awarding XP to an entity with a ProgressionComponent.
 * <p>
 * Dispatch via: {@code commandBuffer.invoke(entityRef, xpAwardEvent)}
 * Handled by: {@code XPAwardSystem}
 * <p>
 * XP is awarded to both character and active class (if set).
 * Class XP is only awarded if the entity has an active class set in their ProgressionComponent.
 */
public class XPAwardEvent extends EcsEvent {
    
    private final long characterXpAmount;
    private final long classXpAmount;
    
    @Nonnull
    private final XPSource source;
    
    @Nullable
    private final Ref<EntityStore> sourceEntityRef;
    
    @Nullable
    private final String sourceId;

    /** Display name of the source entity (e.g., killed mob name). Null if not applicable. */
    @Nullable
    private final String sourceDisplayName;

    /** Level of the source entity (e.g., killed mob level). 0 if not applicable. */
    private final int sourceLevel;

    /** Quality tier of the source entity (e.g., "Common", "Rare"). Null if not applicable. */
    @Nullable
    private final String sourceQuality;
    
    /**
     * Create an XP award event with separate character and class XP amounts.
     * 
     * @param characterXpAmount amount of character XP to award
     * @param classXpAmount amount of class XP to award (only applied if active class set)
     * @param source the category of XP source
     * @param sourceEntityRef optional entity that caused the XP gain (for audit)
     * @param sourceId optional identifier for the source (e.g., objective ID, biome ID)
     */
    public XPAwardEvent(
            long characterXpAmount,
            long classXpAmount,
            @Nonnull XPSource source,
            @Nullable Ref<EntityStore> sourceEntityRef,
            @Nullable String sourceId
    ) {
        this(characterXpAmount, classXpAmount, source, sourceEntityRef, sourceId, null, 0, null);
    }

    /**
     * Create an XP award event with full mob info for combat log display.
     *
     * @param characterXpAmount amount of character XP to award
     * @param classXpAmount amount of class XP to award (only applied if active class set)
     * @param source the category of XP source
     * @param sourceEntityRef optional entity that caused the XP gain (for audit)
     * @param sourceId optional identifier for the source (e.g., objective ID, biome ID)
     * @param sourceDisplayName display name of the source entity (e.g., mob name)
     * @param sourceLevel level of the source entity (e.g., mob level)
     */
    public XPAwardEvent(
            long characterXpAmount,
            long classXpAmount,
            @Nonnull XPSource source,
            @Nullable Ref<EntityStore> sourceEntityRef,
            @Nullable String sourceId,
            @Nullable String sourceDisplayName,
            int sourceLevel
    ) {
        this(characterXpAmount, classXpAmount, source, sourceEntityRef, sourceId, sourceDisplayName, sourceLevel, null);
    }

    /**
     * Create an XP award event with full mob info including quality for combat log display.
     *
     * @param characterXpAmount amount of character XP to award
     * @param classXpAmount amount of class XP to award (only applied if active class set)
     * @param source the category of XP source
     * @param sourceEntityRef optional entity that caused the XP gain (for audit)
     * @param sourceId optional identifier for the source (e.g., objective ID, biome ID)
     * @param sourceDisplayName display name of the source entity (e.g., mob name)
     * @param sourceLevel level of the source entity (e.g., mob level)
     * @param sourceQuality quality tier of the source entity (e.g., "Rare")
     */
    public XPAwardEvent(
            long characterXpAmount,
            long classXpAmount,
            @Nonnull XPSource source,
            @Nullable Ref<EntityStore> sourceEntityRef,
            @Nullable String sourceId,
            @Nullable String sourceDisplayName,
            int sourceLevel,
            @Nullable String sourceQuality
    ) {
        this.characterXpAmount = characterXpAmount;
        this.classXpAmount = classXpAmount;
        this.source = source;
        this.sourceEntityRef = sourceEntityRef;
        this.sourceId = sourceId;
        this.sourceDisplayName = sourceDisplayName;
        this.sourceLevel = sourceLevel;
        this.sourceQuality = sourceQuality;
    }
    
    /**
     * Create an XP award event where character and class XP are the same amount.
     * 
     * @param xpAmount amount of XP to award (same for character and class)
     * @param source the category of XP source
     * @param sourceEntityRef optional entity that caused the XP gain
     */
    public XPAwardEvent(long xpAmount, @Nonnull XPSource source, @Nullable Ref<EntityStore> sourceEntityRef) {
        this(xpAmount, xpAmount, source, sourceEntityRef, null);
    }
    
    /**
     * Create an XP award event with a source ID but no entity reference.
     * 
     * @param xpAmount amount of XP to award
     * @param source the category of XP source
     * @param sourceId identifier for the source
     */
    public XPAwardEvent(long xpAmount, @Nonnull XPSource source, @Nonnull String sourceId) {
        this(xpAmount, xpAmount, source, null, sourceId);
    }
    
    // ========== FACTORY METHODS ==========
    
    /**
     * Create a combat XP award from killing an entity.
     * 
     * @param xpAmount amount of XP
     * @param victimRef the entity that was killed
     * @return the XP award event
     */
    public static XPAwardEvent combat(long xpAmount, @Nonnull Ref<EntityStore> victimRef) {
        return new XPAwardEvent(xpAmount, XPSource.COMBAT, victimRef);
    }

    /**
     * Create a combat XP award from killing an entity, with mob info for combat log.
     *
     * @param xpAmount amount of XP
     * @param victimRef the entity that was killed
     * @param mobDisplayName the killed mob's display name
     * @param mobLevel the killed mob's level
     * @return the XP award event
     */
    public static XPAwardEvent combat(long xpAmount, @Nonnull Ref<EntityStore> victimRef,
                                      @Nullable String mobDisplayName, int mobLevel) {
        return new XPAwardEvent(xpAmount, xpAmount, XPSource.COMBAT, victimRef, null,
                mobDisplayName, mobLevel, null);
    }

    /**
     * Create a combat XP award from killing an entity, with full mob info including quality.
     *
     * @param xpAmount amount of XP
     * @param victimRef the entity that was killed
     * @param mobDisplayName the killed mob's display name
     * @param mobLevel the killed mob's level
     * @param mobQuality the killed mob's quality tier (e.g., "Rare")
     * @return the XP award event
     */
    public static XPAwardEvent combat(long xpAmount, @Nonnull Ref<EntityStore> victimRef,
                                      @Nullable String mobDisplayName, int mobLevel,
                                      @Nullable String mobQuality) {
        return new XPAwardEvent(xpAmount, xpAmount, XPSource.COMBAT, victimRef, null,
                mobDisplayName, mobLevel, mobQuality);
    }
    
    /**
     * Create a discovery XP award for discovering a biome.
     * 
     * @param xpAmount amount of XP
     * @param biomeId the discovered biome ID
     * @return the XP award event
     */
    public static XPAwardEvent discovery(long xpAmount, @Nonnull String biomeId) {
        return new XPAwardEvent(xpAmount, XPSource.DISCOVERY, biomeId);
    }
    
    /**
     * Create an objective completion XP award.
     * 
     * @param xpAmount amount of XP
     * @param objectiveId the completed objective ID
     * @return the XP award event
     */
    public static XPAwardEvent objective(long xpAmount, @Nonnull String objectiveId) {
        return new XPAwardEvent(xpAmount, XPSource.OBJECTIVE, objectiveId);
    }
    
    /**
     * Create an admin XP award (bypasses validation).
     * 
     * @param xpAmount amount of XP
     * @return the XP award event
     */
    public static XPAwardEvent admin(long xpAmount) {
        return new XPAwardEvent(xpAmount, xpAmount, XPSource.ADMIN, null, "admin");
    }
    
    // ========== ACCESSORS ==========
    
    public long getCharacterXpAmount() {
        return characterXpAmount;
    }
    
    public long getClassXpAmount() {
        return classXpAmount;
    }
    
    @Nonnull
    public XPSource getSource() {
        return source;
    }
    
    @Nullable
    public Ref<EntityStore> getSourceEntityRef() {
        return sourceEntityRef;
    }
    
    @Nullable
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Get the display name of the source entity (e.g., killed mob name).
     *
     * @return display name, or null if not applicable
     */
    @Nullable
    public String getSourceDisplayName() {
        return sourceDisplayName;
    }

    /**
     * Get the level of the source entity (e.g., killed mob level).
     *
     * @return source level, or 0 if not applicable
     */
    public int getSourceLevel() {
        return sourceLevel;
    }

    /**
     * Get the quality tier of the source entity (e.g., "Common", "Rare").
     *
     * @return quality tier, or null if not applicable
     */
    @Nullable
    public String getSourceQuality() {
        return sourceQuality;
    }
    
    /**
     * Get a human-readable description of the XP source for logging.
     * 
     * @return description string
     */
    @Nonnull
    public String getSourceDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(source.getId());
        if (sourceId != null) {
            sb.append(":").append(sourceId);
        }
        if (sourceEntityRef != null) {
            sb.append(" (entity)");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "XPAwardEvent{" +
                "charXP=" + characterXpAmount +
                ", classXP=" + classXpAmount +
                ", source=" + getSourceDescription() +
                (sourceDisplayName != null ? ", mob=" + sourceDisplayName : "") +
                (sourceLevel > 0 ? ", mobLv=" + sourceLevel : "") +
                (sourceQuality != null ? ", quality=" + sourceQuality : "") +
                '}';
    }
}
