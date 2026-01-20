package reign.software.hyforged.affix.service;

import reign.software.hyforged.affix.model.AffixDefinition;
import reign.software.hyforged.affix.model.AffixType;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.model.RolledAffix;
import reign.software.hyforged.affix.registry.AffixDefinitionRegistry;
import reign.software.hyforged.affix.registry.AffixTypeRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for generating display names for items with affixes.
 * <p>
 * Generates names in the format: "{prefixes} {baseName} {suffixes}"
 * <ul>
 *   <li>Prefixes (BEFORE): Names appear before the base item name, space-separated</li>
 *   <li>Suffixes (AFTER): Names appear after the base item name, space-separated</li>
 *   <li>Forged (NONE): Do not modify the item name (shown in tooltip only)</li>
 * </ul>
 * <p>
 * Example outputs:
 * <ul>
 *   <li>Sturdy Iron Sword of the Bear</li>
 *   <li>Gleaming Sharp Dagger of Speed of Precision</li>
 *   <li>Iron Sword (no affixes)</li>
 * </ul>
 */
public final class AffixNameGenerator {
    
    private static final Logger LOGGER = Logger.getLogger(AffixNameGenerator.class.getName());
    
    private AffixNameGenerator() {
        // Utility class - no instantiation
    }
    
    /**
     * Generate a display name for an item with affixes.
     *
     * @param baseName  The base item name (e.g., "Iron Sword")
     * @param itemData  The Hyforged item data containing rolled affixes
     * @return The formatted display name with prefixes and suffixes
     */
    @Nonnull
    public static String generateDisplayName(@Nonnull String baseName, @Nonnull HyforgedItemData itemData) {
        Objects.requireNonNull(baseName, "baseName cannot be null");
        Objects.requireNonNull(itemData, "itemData cannot be null");
        
        if (!itemData.hasAffixes()) {
            return baseName;
        }
        
        return generateDisplayName(baseName, itemData.affixes());
    }
    
    /**
     * Generate a display name for an item with affixes.
     *
     * @param baseName  The base item name (e.g., "Iron Sword")
     * @param affixes   The list of rolled affixes
     * @return The formatted display name with prefixes and suffixes
     */
    @Nonnull
    public static String generateDisplayName(@Nonnull String baseName, @Nonnull List<RolledAffix> affixes) {
        Objects.requireNonNull(baseName, "baseName cannot be null");
        Objects.requireNonNull(affixes, "affixes cannot be null");
        
        if (affixes.isEmpty()) {
            return baseName;
        }
        
        List<String> prefixes = new ArrayList<>();
        List<String> suffixes = new ArrayList<>();
        
        AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
        AffixTypeRegistry typeRegistry = AffixTypeRegistry.get();
        
        for (RolledAffix affix : affixes) {
            // Look up the affix definition to get display name
            AffixDefinition definition = affixRegistry.get(affix.affixId());
            if (definition == null) {
                LOGGER.log(Level.WARNING, "Unknown affix definition for name generation: {0}", affix.affixId());
                continue;
            }
            
            // Look up the affix type to determine position
            AffixType type = typeRegistry.get(definition.type());
            if (type == null) {
                LOGGER.log(Level.WARNING, "Unknown affix type for name generation: {0}", definition.type());
                continue;
            }
            
            String displayName = definition.displayName();
            
            switch (type.displayNamePosition()) {
                case BEFORE -> prefixes.add(displayName);
                case AFTER -> suffixes.add(displayName);
                case NONE -> {
                    // Do not modify item name (forged affixes, shown in tooltip only)
                }
            }
        }
        
        return buildDisplayName(baseName, prefixes, suffixes);
    }
    
    /**
     * Build the final display name from components.
     *
     * @param baseName The base item name
     * @param prefixes List of prefix display names (appear before base name)
     * @param suffixes List of suffix display names (appear after base name)
     * @return The formatted display name
     */
    @Nonnull
    private static String buildDisplayName(
            @Nonnull String baseName,
            @Nonnull List<String> prefixes,
            @Nonnull List<String> suffixes
    ) {
        StringBuilder result = new StringBuilder();
        
        // Add prefixes (space-separated before base name)
        for (String prefix : prefixes) {
            result.append(prefix).append(" ");
        }
        
        // Add base name
        result.append(baseName);
        
        // Add suffixes (space-separated after base name)
        for (String suffix : suffixes) {
            result.append(" ").append(suffix);
        }
        
        return result.toString();
    }
    
    /**
     * Generate a display name using an ItemStack by reading its metadata.
     * <p>
     * This is a convenience method that reads the Hyforged item data from
     * the ItemStack's metadata and generates the display name.
     *
     * @param baseName The base item name (from Item config or Hytale)
     * @param itemStack The ItemStack to read affixes from
     * @return The formatted display name with prefixes and suffixes
     */
    @Nonnull
    public static String generateDisplayName(
            @Nonnull String baseName,
            @Nonnull com.hypixel.hytale.server.core.inventory.ItemStack itemStack
    ) {
        Objects.requireNonNull(baseName, "baseName cannot be null");
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        
        HyforgedItemData itemData = HyforgedItemDataService.read(itemStack);
        return generateDisplayName(baseName, itemData);
    }
    
    /**
     * Check if an item's display name would be modified by its affixes.
     *
     * @param itemData The Hyforged item data
     * @return true if any affixes have BEFORE or AFTER display positions
     */
    public static boolean hasVisibleNameModifiers(@Nonnull HyforgedItemData itemData) {
        Objects.requireNonNull(itemData, "itemData cannot be null");
        
        if (!itemData.hasAffixes()) {
            return false;
        }
        
        AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
        AffixTypeRegistry typeRegistry = AffixTypeRegistry.get();
        
        for (RolledAffix affix : itemData.affixes()) {
            AffixDefinition definition = affixRegistry.get(affix.affixId());
            if (definition == null) {
                continue;
            }
            
            AffixType type = typeRegistry.get(definition.type());
            if (type == null) {
                continue;
            }
            
            if (type.displayNamePosition() != AffixType.DisplayNamePosition.NONE) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get just the prefix portion of a generated name.
     *
     * @param affixes The list of rolled affixes
     * @return The prefix string (empty if no prefixes)
     */
    @Nonnull
    public static String getPrefixString(@Nonnull List<RolledAffix> affixes) {
        Objects.requireNonNull(affixes, "affixes cannot be null");
        
        List<String> prefixes = new ArrayList<>();
        AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
        AffixTypeRegistry typeRegistry = AffixTypeRegistry.get();
        
        for (RolledAffix affix : affixes) {
            AffixDefinition definition = affixRegistry.get(affix.affixId());
            if (definition == null) continue;
            
            AffixType type = typeRegistry.get(definition.type());
            if (type == null) continue;
            
            if (type.displayNamePosition() == AffixType.DisplayNamePosition.BEFORE) {
                prefixes.add(definition.displayName());
            }
        }
        
        return String.join(" ", prefixes);
    }
    
    /**
     * Get just the suffix portion of a generated name.
     *
     * @param affixes The list of rolled affixes
     * @return The suffix string (empty if no suffixes)
     */
    @Nonnull
    public static String getSuffixString(@Nonnull List<RolledAffix> affixes) {
        Objects.requireNonNull(affixes, "affixes cannot be null");
        
        List<String> suffixes = new ArrayList<>();
        AffixDefinitionRegistry affixRegistry = AffixDefinitionRegistry.get();
        AffixTypeRegistry typeRegistry = AffixTypeRegistry.get();
        
        for (RolledAffix affix : affixes) {
            AffixDefinition definition = affixRegistry.get(affix.affixId());
            if (definition == null) continue;
            
            AffixType type = typeRegistry.get(definition.type());
            if (type == null) continue;
            
            if (type.displayNamePosition() == AffixType.DisplayNamePosition.AFTER) {
                suffixes.add(definition.displayName());
            }
        }
        
        return String.join(" ", suffixes);
    }
}
