package reign.software.hyforged.passive.registry;

import reign.software.hyforged.passive.asset.StatIconConfigAsset;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Registry for stat-to-icon mappings.
 * <p>
 * Provides data-driven icon resolution for passive nodes:
 * <ol>
 *   <li>Explicit node icon (from node JSON)</li>
 *   <li>Node type icon (keystone, notable)</li>
 *   <li>Stat pattern matching (from config)</li>
 *   <li>Default icon</li>
 * </ol>
 */
public class StatIconRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(StatIconRegistry.class.getName());
    
    private static StatIconRegistry instance;
    
    private final List<CompiledMapping> mappings = new ArrayList<>();
    private StatIconConfigAsset config;
    private boolean loaded = false;
    
    private StatIconRegistry() {
        // Singleton
    }
    
    /**
     * Get the singleton instance.
     */
    public static synchronized StatIconRegistry get() {
        if (instance == null) {
            instance = new StatIconRegistry();
        }
        return instance;
    }
    
    /**
     * Reset the registry (for testing).
     */
    public static synchronized void reset() {
        instance = new StatIconRegistry();
    }
    
    /**
     * Load the stat icon configuration from a loaded asset.
     *
     * @param configAsset The loaded configuration asset
     */
    public void load(@Nonnull StatIconConfigAsset configAsset) {
        this.config = configAsset;
        compileMappings();
        loaded = true;
        LOGGER.info("Loaded " + mappings.size() + " stat icon mappings from config");
    }
    
    /**
     * Initialize with default mappings if no config is loaded.
     */
    public void loadDefaults() {
        if (loaded) {
            return;
        }
        
        config = new StatIconConfigAsset();
        // Add some default mappings programmatically
        addDefaultMapping("strength", "Hyforged/Textures/Strength.png", 100);
        addDefaultMapping("dexterity", "Hyforged/Textures/Dexterity.png", 100);
        addDefaultMapping("intelligence", "Hyforged/Textures/Intelligence.png", 100);
        addDefaultMapping("wisdom", "Hyforged/Textures/Wisdom.png", 100);
        addDefaultMapping("constitution", "Hyforged/Textures/Constitution.png", 90);
        addDefaultMapping("spirit", "Hyforged/Textures/Spirit.png", 90);
        addDefaultMapping("luck", "Hyforged/Textures/Luck.png", 90);
        addDefaultMapping("health|life|hp|vitality", "Hyforged/Textures/Health.png", 80);
        addDefaultMapping("resist|armor|defence|defense", "Hyforged/Textures/Resist.png", 80);
        addDefaultMapping("fire|cold|lightning|elemental", "Hyforged/Textures/Elemental.png", 75);
        
        loaded = true;
        LOGGER.info("Using " + mappings.size() + " default stat icon mappings");
    }
    
    private void addDefaultMapping(String pattern, String icon, int priority) {
        try {
            mappings.add(new CompiledMapping(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), icon, priority));
        } catch (Exception e) {
            LOGGER.warning("Invalid default pattern: " + pattern);
        }
    }
    
    /**
     * Compile regex patterns from config.
     */
    private void compileMappings() {
        mappings.clear();
        
        for (StatIconConfigAsset.StatIconMappingAsset mapping : config.getMappings()) {
            String pattern = mapping.getPattern();
            String icon = mapping.getIcon();
            
            if (pattern == null || pattern.isEmpty() || icon == null || icon.isEmpty()) {
                continue;
            }
            
            try {
                Pattern compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                mappings.add(new CompiledMapping(compiled, icon, mapping.getPriority()));
            } catch (Exception e) {
                LOGGER.warning("Invalid stat icon pattern: " + pattern + " - " + e.getMessage());
            }
        }
        
        // Sort by priority (highest first)
        mappings.sort(Comparator.comparingInt(CompiledMapping::priority).reversed());
    }
    
    /**
     * Get the icon for a passive node.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Explicit node icon (if set in node JSON)</li>
     *   <li>Node type icon (keystone, notable)</li>
     *   <li>Stat pattern matching from effects</li>
     *   <li>Default icon</li>
     * </ol>
     *
     * @param node The passive node
     * @param isStarting Whether this is a starting node
     * @return The icon path, never null
     */
    @Nonnull
    public String getIconForNode(@Nonnull PassiveNode node, boolean isStarting) {
        // 1. Explicit node icon
        if (node.icon() != null && !node.icon().isEmpty()) {
            return node.icon();
        }
        
        // 2. Starting nodes - use attribute from ID
        if (isStarting) {
            String startingIcon = getIconForStartingNode(node.id());
            if (startingIcon != null) {
                return startingIcon;
            }
        }
        
        // 3. Node type icons (keystone, notable)
        String nodeType = node.type().toLowerCase();
        if (config != null) {
            String typeIcon = config.getNodeTypeIcon(nodeType);
            if (typeIcon != null) {
                return typeIcon;
            }
        }
        
        // 4. Stat pattern matching from effects
        String statIcon = getIconFromEffects(node);
        if (statIcon != null) {
            return statIcon;
        }
        
        // 5. Default icon
        return config != null ? config.getDefaultIcon() : "Hyforged/Textures/Passive.png";
    }
    
    /**
     * Get icon for a starting node based on its ID.
     */
    @Nullable
    private String getIconForStartingNode(@Nonnull String nodeId) {
        String id = nodeId.toLowerCase();
        
        // Match starting node to primary attribute
        if (id.contains("strength")) return "Hyforged/Textures/Strength.png";
        if (id.contains("dexterity")) return "Hyforged/Textures/Dexterity.png";
        if (id.contains("intelligence")) return "Hyforged/Textures/Intelligence.png";
        if (id.contains("wisdom")) return "Hyforged/Textures/Wisdom.png";
        
        return null;
    }
    
    /**
     * Get icon based on node effects using pattern matching.
     */
    @Nullable
    private String getIconFromEffects(@Nonnull PassiveNode node) {
        for (PassiveNodeEffect effect : node.effects()) {
            // Only check stat-modifier effects
            if (!"stat-modifier".equals(effect.type())) {
                continue;
            }
            
            Object statObj = effect.data().get("Stat");
            if (statObj == null) {
                continue;
            }
            
            String stat = statObj.toString();
            String icon = matchStatToIcon(stat);
            if (icon != null) {
                return icon;
            }
        }
        
        return null;
    }
    
    /**
     * Match a stat name against configured patterns.
     */
    @Nullable
    public String matchStatToIcon(@Nonnull String stat) {
        for (CompiledMapping mapping : mappings) {
            if (mapping.pattern().matcher(stat).find()) {
                return mapping.icon();
            }
        }
        return null;
    }
    
    /**
     * Check if the registry is loaded.
     */
    public boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Compiled pattern mapping.
     */
    private record CompiledMapping(Pattern pattern, String icon, int priority) {}
}
