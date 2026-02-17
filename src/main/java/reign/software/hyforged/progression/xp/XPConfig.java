package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration holder for XP scaling values.
 * <p>
 * Values are loaded from Server/Hyforged/Progression/XPConfig.json via XPConfigAssetLoader.
 * <p>
 * Provides:
 * - Combat XP scaling (base, level scaling, difficulty multipliers)
 * - Discovery XP amounts
 * - Objective XP by tier
 * - Class XP ratio
 * - Notification settings
 */
public class XPConfig {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private static XPConfig instance;
    
    // Combat XP
    private long combatBaseXp = 10;
    private boolean combatLevelScalingEnabled = true;
    private double combatPerLevelMultiplier = 0.1;
    private double combatMaxMultiplier = 5.0;
    private final Map<String, Double> combatDifficultyMultipliers = new HashMap<>();
    
    // Discovery XP
    private long discoveryBiomeXp = 100;
    private long discoveryLandmarkXp = 50;
    
    // Objective XP
    private final Map<String, Long> objectiveTierXp = new HashMap<>();
    
    // Class XP
    private double classXpRatio = 1.0;
    
    // Caps
    private int maxCharacterLevel = 100;
    private int maxClassLevel = 20;
    
    // Notifications
    private int notificationAggregationTicks = 20;
    private boolean showFloatingXpText = true;
    private String xpTextColor = "#FFD700";
    
    private XPConfig() {
        // Initialize defaults
        combatDifficultyMultipliers.put("trivial", 0.1);
        combatDifficultyMultipliers.put("easy", 0.5);
        combatDifficultyMultipliers.put("normal", 1.0);
        combatDifficultyMultipliers.put("hard", 1.5);
        combatDifficultyMultipliers.put("boss", 3.0);
        
        objectiveTierXp.put("minor", 25L);
        objectiveTierXp.put("standard", 50L);
        objectiveTierXp.put("major", 100L);
        objectiveTierXp.put("legendary", 250L);
        
        LOGGER.atInfo().log("XPConfig initialized with defaults");
    }
    
    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static XPConfig get() {
        if (instance == null) {
            instance = new XPConfig();
        }
        return instance;
    }
    
    /**
     * Apply configuration values from a loaded XPConfigAsset.
     * This should be called after assets are loaded to override defaults.
     *
     * @param asset the loaded asset
     */
    public void applyFromAsset(@Nonnull XPConfigAsset asset) {
        LOGGER.atInfo().log("Applying XP configuration from loaded asset...");
        
        // Combat
        this.combatBaseXp = asset.getCombatBaseXp();
        
        // Discovery
        this.discoveryBiomeXp = asset.getDiscoveryBiomeXp();
        this.discoveryLandmarkXp = asset.getDiscoveryLandmarkXp();
        
        // Objective
        this.objectiveTierXp.clear();
        this.objectiveTierXp.put("minor", asset.getObjectiveMinorXp());
        this.objectiveTierXp.put("standard", asset.getObjectiveStandardXp());
        this.objectiveTierXp.put("major", asset.getObjectiveMajorXp());
        this.objectiveTierXp.put("legendary", asset.getObjectiveLegendaryXp());
        
        // Globals
        this.classXpRatio = asset.getClassXpRatio();
        this.maxCharacterLevel = asset.getMaxCharacterLevel();
        this.maxClassLevel = asset.getMaxClassLevel();
        
        LOGGER.atInfo().log("XP configuration applied from asset");
    }
    
    // ========== ACCESSORS ==========
    
    public long getCombatBaseXp() {
        return combatBaseXp;
    }
    
    /**
     * Calculate combat XP for an enemy of a given level.
     * 
     * @param enemyLevel the enemy's level (or 1 if unknown)
     * @param difficulty the difficulty tier (or "normal" if unknown)
     * @return calculated XP amount
     */
    public long calculateCombatXp(int enemyLevel, String difficulty) {
        double xp = combatBaseXp;
        
        // Apply level scaling
        if (combatLevelScalingEnabled && enemyLevel > 1) {
            double levelMultiplier = 1.0 + (enemyLevel - 1) * combatPerLevelMultiplier;
            levelMultiplier = Math.min(levelMultiplier, combatMaxMultiplier);
            xp *= levelMultiplier;
        }
        
        // Apply difficulty multiplier
        double diffMultiplier = combatDifficultyMultipliers.getOrDefault(
                difficulty != null ? difficulty.toLowerCase() : "normal", 1.0);
        xp *= diffMultiplier;
        
        return Math.round(xp);
    }
    
    public long getDiscoveryBiomeXp() {
        return discoveryBiomeXp;
    }
    
    public long getDiscoveryLandmarkXp() {
        return discoveryLandmarkXp;
    }
    
    public long getObjectiveXp(String tier) {
        return objectiveTierXp.getOrDefault(tier != null ? tier.toLowerCase() : "standard", 50L);
    }
    
    public double getClassXpRatio() {
        return classXpRatio;
    }
    
    public int getMaxCharacterLevel() {
        return maxCharacterLevel;
    }
    
    public int getMaxClassLevel() {
        return maxClassLevel;
    }
    
    public int getNotificationAggregationTicks() {
        return notificationAggregationTicks;
    }
    
    public boolean isShowFloatingXpText() {
        return showFloatingXpText;
    }
    
    public String getXpTextColor() {
        return xpTextColor;
    }
}
