package reign.software.hyforged.progression.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.progression.CharacterProgression;
import reign.software.hyforged.progression.ClassProgression;
import reign.software.hyforged.progression.XPCurve;
import reign.software.hyforged.progression.asset.XPCurveRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ECS Component holding progression data for a player entity.
 * <p>
 * This is PURE DATA - no behavior, following ECS principles.
 * All computation is done by Systems that process this component.
 * <p>
 * Data stored:
 * - Character level and XP
 * - Class progressions (multi-class support)
 * - Active class ID (derived from weapon, not persisted)
 * - General passive points allocated
 * - Class passive points allocated per class
 */
public class ProgressionComponent implements Component<EntityStore> {
    
    /** Schema version for persistence migration */
    public static final int SCHEMA_VERSION = 1;
    
    // ========== CHARACTER PROGRESSION ==========
    
    private int characterLevel = 1;
    private long characterXp = 0;
    
    // ========== CLASS PROGRESSIONS ==========
    // Maps class ID to class-specific progression
    
    private final Map<String, ClassProgressionData> classProgressions = new HashMap<>();
    
    // ========== ACTIVE CLASS (DERIVED, NOT PERSISTED) ==========
    // Resolved from main-hand weapon tags by ActiveClassResolutionSystem
    
    private String activeClassId = null;
    
    // ========== PASSIVE POINT ALLOCATION ==========
    
    private int generalPassivePointsAllocated = 0;
    private final Map<String, Integer> classPassivePointsAllocated = new HashMap<>();
    
    // ========== DIRTY FLAGS ==========
    
    private boolean dirty = false;
    
    // ========== TEMPORARY LOAD STATE ==========
    // Used during deserialization
    
    private String[] tempClassIds;
    private int[] tempClassLevels;
    private long[] tempClassXps;
    
    public ProgressionComponent() {
        // Required for codec
    }
    
    /**
     * Copy constructor for clone().
     */
    public ProgressionComponent(ProgressionComponent other) {
        this.characterLevel = other.characterLevel;
        this.characterXp = other.characterXp;
        this.activeClassId = other.activeClassId;
        this.generalPassivePointsAllocated = other.generalPassivePointsAllocated;
        
        for (Map.Entry<String, ClassProgressionData> entry : other.classProgressions.entrySet()) {
            this.classProgressions.put(entry.getKey(), new ClassProgressionData(entry.getValue()));
        }
        
        this.classPassivePointsAllocated.putAll(other.classPassivePointsAllocated);
        this.dirty = other.dirty;
    }
    
    @Override
    public ProgressionComponent clone() {
        return new ProgressionComponent(this);
    }
    
    // ========== CHARACTER PROGRESSION ACCESSORS ==========
    
    public int getCharacterLevel() {
        return characterLevel;
    }
    
    public void setCharacterLevel(int level) {
        if (level < CharacterProgression.MIN_LEVEL) {
            level = CharacterProgression.MIN_LEVEL;
        }
        if (level > CharacterProgression.MAX_LEVEL) {
            level = CharacterProgression.MAX_LEVEL;
        }
        if (this.characterLevel != level) {
            this.characterLevel = level;
            this.dirty = true;
        }
    }
    
    public long getCharacterXp() {
        return characterXp;
    }
    
    public void setCharacterXp(long xp) {
        if (xp < 0) {
            xp = 0;
        }
        if (this.characterXp != xp) {
            this.characterXp = xp;
            this.dirty = true;
        }
    }
    
    /**
     * Add XP to character. Does not handle level-up - that's done by systems.
     *
     * @param amount XP to add
     * @return New total XP
     */
    public long addCharacterXp(long amount) {
        if (amount <= 0) {
            return characterXp;
        }
        characterXp += amount;
        dirty = true;
        return characterXp;
    }
    
    /**
     * Get the XP required to reach the next character level (incremental).
     *
     * @return XP required for next level (not remaining, but total for that level)
     */
    public long getCharacterXpToNext() {
        XPCurve curve = XPCurveRegistry.get().getCharacterCurve();
        if (characterLevel >= CharacterProgression.MAX_LEVEL) {
            return 0;
        }
        return curve.getXpForLevel(characterLevel + 1);
    }
    
    /**
     * Get XP earned toward the next character level.
     * This is the total XP minus the cumulative XP required to reach current level.
     *
     * @return XP progress toward next level
     */
    public long getCharacterXpProgress() {
        XPCurve curve = XPCurveRegistry.get().getCharacterCurve();
        long xpAtCurrentLevel = curve.getTotalXpForLevel(characterLevel);
        return Math.max(0, characterXp - xpAtCurrentLevel);
    }
    
    /**
     * Get general passive points available (total - allocated).
     *
     * @return Available general passive points
     */
    public int getAvailableGeneralPassivePoints() {
        int total = characterLevel - 1; // Level 1 = 0 points
        return Math.max(0, total - generalPassivePointsAllocated);
    }
    
    /**
     * Get a snapshot of character progression.
     * Uses XP progress toward next level and incremental XP requirement.
     *
     * @return CharacterProgression record
     */
    @Nonnull
    public CharacterProgression getCharacterProgression() {
        return new CharacterProgression(
            characterLevel,
            getCharacterXpProgress(),  // XP toward next level
            getCharacterXpToNext()     // XP needed for next level
        );
    }
    
    // ========== CLASS PROGRESSION ACCESSORS ==========
    
    /**
     * Get the active class ID (resolved from weapon tags).
     *
     * @return Active class ID, or null if no class is active
     */
    @Nullable
    public String getActiveClassId() {
        return activeClassId;
    }
    
    /**
     * Set the active class ID. Called by ActiveClassResolutionSystem.
     *
     * @param classId The class ID, or null for no active class
     */
    public void setActiveClassId(@Nullable String classId) {
        this.activeClassId = classId;
        // Note: activeClassId is not persisted, so don't set dirty
    }
    
    /**
     * Get or create class progression data for a class.
     *
     * @param classId The class ID
     * @return Class progression data
     */
    @Nonnull
    public ClassProgressionData getOrCreateClassProgression(@Nonnull String classId) {
        return classProgressions.computeIfAbsent(classId, id -> new ClassProgressionData());
    }
    
    /**
     * Get class progression data if it exists.
     *
     * @param classId The class ID
     * @return Class progression data, or null if not started
     */
    @Nullable
    public ClassProgressionData getClassProgression(@Nonnull String classId) {
        return classProgressions.get(classId);
    }
    
    /**
     * Get a snapshot of class progression.
     * Uses XP progress toward next level (not total XP).
     *
     * @param classId The class ID
     * @return ClassProgression record
     */
    @Nonnull
    public ClassProgression getClassProgressionSnapshot(@Nonnull String classId) {
        ClassProgressionData data = classProgressions.get(classId);
        XPCurve curve = XPCurveRegistry.get().getClassCurve();
        
        if (data == null) {
            return ClassProgression.initial(classId, curve.getXpForLevel(2));
        }
        
        long xpToNext = data.level >= ClassProgression.MAX_LEVEL 
            ? 0 
            : curve.getXpForLevel(data.level + 1);
        
        // Calculate XP progress toward next level (total XP minus cumulative threshold)
        long xpAtCurrentLevel = curve.getTotalXpForLevel(data.level);
        long xpProgress = Math.max(0, data.xp - xpAtCurrentLevel);
            
        return new ClassProgression(classId, data.level, xpProgress, xpToNext);
    }
    
    /**
     * Get all class IDs with progression.
     *
     * @return Unmodifiable set of class IDs
     */
    @Nonnull
    public java.util.Set<String> getClassIds() {
        return Collections.unmodifiableSet(classProgressions.keySet());
    }
    
    /**
     * Add XP to the active class. Does not handle level-up.
     *
     * @param amount XP to add
     * @return New total class XP, or -1 if no active class
     */
    public long addActiveClassXp(long amount) {
        if (activeClassId == null || amount <= 0) {
            return -1;
        }
        ClassProgressionData data = getOrCreateClassProgression(activeClassId);
        data.xp += amount;
        dirty = true;
        return data.xp;
    }
    
    /**
     * Get class passive points available for a class.
     *
     * @param classId The class ID
     * @return Available class passive points
     */
    public int getAvailableClassPassivePoints(@Nonnull String classId) {
        ClassProgressionData data = classProgressions.get(classId);
        if (data == null) {
            return 0;
        }
        int total = data.level; // Each class level grants 1 point
        int allocated = classPassivePointsAllocated.getOrDefault(classId, 0);
        return Math.max(0, total - allocated);
    }
    
    // ========== PASSIVE POINT ALLOCATION ==========
    
    public int getGeneralPassivePointsAllocated() {
        return generalPassivePointsAllocated;
    }
    
    public void setGeneralPassivePointsAllocated(int points) {
        if (points < 0) {
            points = 0;
        }
        if (this.generalPassivePointsAllocated != points) {
            this.generalPassivePointsAllocated = points;
            this.dirty = true;
        }
    }
    
    public int getClassPassivePointsAllocated(@Nonnull String classId) {
        return classPassivePointsAllocated.getOrDefault(classId, 0);
    }
    
    public void setClassPassivePointsAllocated(@Nonnull String classId, int points) {
        if (points < 0) {
            points = 0;
        }
        Integer current = classPassivePointsAllocated.get(classId);
        if (current == null || current != points) {
            classPassivePointsAllocated.put(classId, points);
            dirty = true;
        }
    }
    
    // ========== DIRTY FLAG ==========
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void markDirty() {
        dirty = true;
    }
    
    public void clearDirty() {
        dirty = false;
    }
    
    // ========== RESET ==========
    
    /**
     * Reset all progression to initial state.
     * <p>
     * This resets:
     * - Character level to 1
     * - Character XP to 0
     * - All class progressions cleared
     * - All passive point allocations cleared
     * - Active class cleared
     */
    public void reset() {
        this.characterLevel = 1;
        this.characterXp = 0;
        this.classProgressions.clear();
        this.generalPassivePointsAllocated = 0;
        this.activeClassId = null;
        markDirty();
    }
    
    // ========== SERIALIZATION HELPERS ==========
    
    // For codec - get class progression arrays
    @Nonnull
    public String[] getClassProgressionIds() {
        return classProgressions.keySet().toArray(new String[0]);
    }
    
    @Nonnull
    public int[] getClassProgressionLevels() {
        int[] levels = new int[classProgressions.size()];
        int i = 0;
        for (String id : classProgressions.keySet()) {
            levels[i++] = classProgressions.get(id).level;
        }
        return levels;
    }
    
    @Nonnull
    public long[] getClassProgressionXps() {
        long[] xps = new long[classProgressions.size()];
        int i = 0;
        for (String id : classProgressions.keySet()) {
            xps[i++] = classProgressions.get(id).xp;
        }
        return xps;
    }
    
    // Temporary storage for deserialization
    public void setTempClassIds(String[] ids) {
        this.tempClassIds = ids;
    }
    
    public String[] getTempClassIds() {
        return tempClassIds;
    }
    
    public void setTempClassLevels(int[] levels) {
        this.tempClassLevels = levels;
    }
    
    public int[] getTempClassLevels() {
        return tempClassLevels;
    }
    
    public void setTempClassXps(long[] xps) {
        this.tempClassXps = xps;
    }
    
    public long[] getTempClassXps() {
        return tempClassXps;
    }
    
    public void applyTempClassData() {
        if (tempClassIds != null && tempClassLevels != null && tempClassXps != null) {
            int len = Math.min(Math.min(tempClassIds.length, tempClassLevels.length), tempClassXps.length);
            for (int i = 0; i < len; i++) {
                ClassProgressionData data = new ClassProgressionData();
                data.level = tempClassLevels[i];
                data.xp = tempClassXps[i];
                classProgressions.put(tempClassIds[i], data);
            }
        }
        tempClassIds = null;
        tempClassLevels = null;
        tempClassXps = null;
    }
    
    // ========== INNER CLASS ==========
    
    /**
     * Mutable class progression data holder.
     */
    public static class ClassProgressionData {
        public int level = 1;
        public long xp = 0;
        
        public ClassProgressionData() {}
        
        public ClassProgressionData(ClassProgressionData other) {
            this.level = other.level;
            this.xp = other.xp;
        }
    }
}
