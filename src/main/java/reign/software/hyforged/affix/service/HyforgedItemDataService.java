package reign.software.hyforged.affix.service;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import reign.software.hyforged.affix.model.HyforgedItemData;

import javax.annotation.Nonnull;

/**
 * Service for reading and writing Hyforged item data to ItemStack metadata.
 * <p>
 * This provides a clean API for accessing affix data stored on items,
 * handling serialization/deserialization through the codec system.
 * <p>
 * Usage:
 * <pre>
 * // Read affix data from an item
 * HyforgedItemData data = HyforgedItemDataService.read(itemStack);
 * 
 * // Check if item has affixes
 * if (data.hasAffixes()) {
 *     // Process affixes
 * }
 * 
 * // Write modified data back
 * itemStack = HyforgedItemDataService.write(itemStack, data.withAffix(newAffix));
 * </pre>
 */
public final class HyforgedItemDataService {
    
    private HyforgedItemDataService() {
        // Utility class, no instantiation
    }
    
    /**
     * Read Hyforged item data from an ItemStack.
     * <p>
     * Returns {@link HyforgedItemData#EMPTY} if the item has no Hyforged metadata.
     *
     * @param itemStack The item to read from
     * @return The item's Hyforged data, or EMPTY if none exists
     */
    @Nonnull
    public static HyforgedItemData read(@Nonnull ItemStack itemStack) {
        HyforgedItemData.HyforgedItemDataAsset asset = itemStack.getFromMetadataOrNull(
                HyforgedItemData.METADATA_KEY,
                HyforgedItemData.CODEC
        );
        
        if (asset == null) {
            return HyforgedItemData.EMPTY;
        }
        
        HyforgedItemData data = asset.toItemData();
        
        // Handle schema migration if needed
        if (data.needsMigration()) {
            data = migrateSchema(data);
        }
        
        return data;
    }
    
    /**
     * Write Hyforged item data to an ItemStack.
     * <p>
     * Returns a new ItemStack with the metadata applied (ItemStack is immutable).
     *
     * @param itemStack The item to write to
     * @param data      The data to write
     * @return A new ItemStack with the data written
     */
    @Nonnull
    public static ItemStack write(@Nonnull ItemStack itemStack, @Nonnull HyforgedItemData data) {
        return itemStack.withMetadata(
                HyforgedItemData.METADATA_KEY,
                HyforgedItemData.CODEC,
                data.toAsset()
        );
    }
    
    /**
     * Check if an ItemStack has any Hyforged affix data.
     *
     * @param itemStack The item to check
     * @return true if the item has Hyforged metadata
     */
    public static boolean hasData(@Nonnull ItemStack itemStack) {
        return itemStack.getFromMetadataOrNull(
                HyforgedItemData.METADATA_KEY,
                HyforgedItemData.CODEC
        ) != null;
    }
    
    /**
     * Clear all Hyforged data from an ItemStack.
     * <p>
     * This writes EMPTY data to the item, effectively removing all affixes.
     *
     * @param itemStack The item to clear
     * @return A new ItemStack with the data cleared
     */
    @Nonnull
    public static ItemStack clear(@Nonnull ItemStack itemStack) {
        return write(itemStack, HyforgedItemData.EMPTY);
    }
    
    /**
     * Migrate item data to the current schema version.
     * <p>
     * This is called automatically when reading data with an old schema version.
     *
     * @param oldData The data to migrate
     * @return Migrated data at current schema version
     */
    @Nonnull
    private static HyforgedItemData migrateSchema(@Nonnull HyforgedItemData oldData) {
        // Currently only version 1 exists, so nothing to migrate
        // Future migrations will be added here as needed:
        //
        // if (oldData.schemaVersion() == 1) {
        //     oldData = migrateV1ToV2(oldData);
        // }
        // if (oldData.schemaVersion() == 2) {
        //     oldData = migrateV2ToV3(oldData);
        // }
        
        // Return with current version
        return new HyforgedItemData(
                HyforgedItemData.CURRENT_SCHEMA_VERSION,
                oldData.affixes()
        );
    }
}
