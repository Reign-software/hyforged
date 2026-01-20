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
        this.characterXpAmount = characterXpAmount;
        this.classXpAmount = classXpAmount;
        this.source = source;
        this.sourceEntityRef = sourceEntityRef;
        this.sourceId = sourceId;
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
                '}';
    }
}
