package reign.software.hyforged.affix.model;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Container for all Hyforged-specific item metadata.
 * <p>
 * This is stored in the item's metadata under the "Hyforged" key.
 * It contains a schema version for migration support and a list
 * of rolled affixes.
 * <p>
 * Usage:
 * <pre>
 * // Read from ItemStack
 * HyforgedItemData data = HyforgedItemDataService.read(itemStack);
 * 
 * // Modify and write back
 * HyforgedItemData updated = data.withAffixes(newAffixes);
 * itemStack = HyforgedItemDataService.write(itemStack, updated);
 * </pre>
 *
 * @param schemaVersion The data schema version for migration support
 * @param affixes       The list of rolled affixes on this item
 */
public record HyforgedItemData(
        int schemaVersion,
        @Nonnull List<RolledAffix> affixes
) {
    
    /**
     * Current schema version. Increment when making breaking changes to the data format.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;
    
    /**
     * Metadata key for storing Hyforged data in ItemStack.
     */
    public static final String METADATA_KEY = "Hyforged";
    
    /**
     * Codec for serializing HyforgedItemData to/from BSON.
     */
    public static final BuilderCodec<HyforgedItemDataAsset> CODEC = BuilderCodec.builder(
            HyforgedItemDataAsset.class,
            HyforgedItemDataAsset::new
    )
    .append(
            new KeyedCodec<>("SchemaVersion", Codec.INTEGER),
            (data, value) -> data.schemaVersion = value != null ? value : CURRENT_SCHEMA_VERSION,
            data -> data.schemaVersion
    )
    .add()
    .append(
            new KeyedCodec<>("Affixes", RolledAffix.ARRAY_CODEC),
            (data, value) -> data.affixes = value != null ? value : new RolledAffix.RolledAffixData[0],
            data -> data.affixes
    )
    .add()
    .build();
    
    /**
     * Empty data instance for items without affixes.
     */
    public static final HyforgedItemData EMPTY = new HyforgedItemData(
            CURRENT_SCHEMA_VERSION,
            Collections.emptyList()
    );
    
    /**
     * Mutable asset class for codec serialization.
     */
    public static final class HyforgedItemDataAsset {
        public int schemaVersion = CURRENT_SCHEMA_VERSION;
        public RolledAffix.RolledAffixData[] affixes = new RolledAffix.RolledAffixData[0];
        
        public HyforgedItemDataAsset() {}
        
        public HyforgedItemDataAsset(HyforgedItemData data) {
            this.schemaVersion = data.schemaVersion();
            this.affixes = data.affixes().stream()
                    .map(RolledAffix::toData)
                    .toArray(RolledAffix.RolledAffixData[]::new);
        }
        
        public HyforgedItemData toItemData() {
            List<RolledAffix> affixList = new ArrayList<>();
            if (affixes != null) {
                for (RolledAffix.RolledAffixData affixData : affixes) {
                    affixList.add(affixData.toRolledAffix());
                }
            }
            return new HyforgedItemData(schemaVersion, affixList);
        }
    }
    
    /**
     * Canonical constructor with validation.
     */
    public HyforgedItemData {
        Objects.requireNonNull(affixes, "affixes cannot be null");
        // Make immutable defensive copy
        affixes = List.copyOf(affixes);
    }
    
    /**
     * Create item data with the current schema version and given affixes.
     */
    public static HyforgedItemData create(@Nonnull List<RolledAffix> affixes) {
        return new HyforgedItemData(CURRENT_SCHEMA_VERSION, affixes);
    }
    
    /**
     * Create item data with a single affix.
     */
    public static HyforgedItemData of(@Nonnull RolledAffix affix) {
        return new HyforgedItemData(CURRENT_SCHEMA_VERSION, List.of(affix));
    }
    
    /**
     * Create item data with multiple affixes.
     */
    public static HyforgedItemData of(@Nonnull RolledAffix... affixes) {
        return new HyforgedItemData(CURRENT_SCHEMA_VERSION, List.of(affixes));
    }
    
    /**
     * Check if this item has any affixes.
     */
    public boolean hasAffixes() {
        return !affixes.isEmpty();
    }
    
    /**
     * Get the number of affixes on this item.
     */
    public int affixCount() {
        return affixes.size();
    }
    
    /**
     * Get affixes of a specific type (prefix, suffix, forged).
     */
    public List<RolledAffix> getAffixesByType(@Nonnull String type) {
        return affixes.stream()
                .filter(a -> a.type().equals(type))
                .collect(Collectors.toList());
    }
    
    /**
     * Count affixes of a specific type.
     */
    public int countByType(@Nonnull String type) {
        return (int) affixes.stream()
                .filter(a -> a.type().equals(type))
                .count();
    }
    
    /**
     * Check if this item has an affix with the given ID.
     */
    public boolean hasAffix(@Nonnull String affixId) {
        return affixes.stream().anyMatch(a -> a.affixId().equals(affixId));
    }
    
    /**
     * Create a new HyforgedItemData with the given affixes, replacing existing ones.
     */
    public HyforgedItemData withAffixes(@Nonnull List<RolledAffix> newAffixes) {
        return new HyforgedItemData(schemaVersion, newAffixes);
    }
    
    /**
     * Create a new HyforgedItemData with an additional affix appended.
     */
    public HyforgedItemData withAffix(@Nonnull RolledAffix affix) {
        List<RolledAffix> newAffixes = new ArrayList<>(affixes);
        newAffixes.add(affix);
        return new HyforgedItemData(schemaVersion, newAffixes);
    }
    
    /**
     * Create a new HyforgedItemData with all affixes of the given type removed.
     */
    public HyforgedItemData withoutType(@Nonnull String type) {
        List<RolledAffix> filtered = affixes.stream()
                .filter(a -> !a.type().equals(type))
                .collect(Collectors.toList());
        return new HyforgedItemData(schemaVersion, filtered);
    }
    
    /**
     * Create a new HyforgedItemData with the specified affix removed.
     */
    public HyforgedItemData withoutAffix(@Nonnull String affixId) {
        List<RolledAffix> filtered = affixes.stream()
                .filter(a -> !a.affixId().equals(affixId))
                .collect(Collectors.toList());
        return new HyforgedItemData(schemaVersion, filtered);
    }
    
    /**
     * Check if this data needs schema migration.
     */
    public boolean needsMigration() {
        return schemaVersion < CURRENT_SCHEMA_VERSION;
    }
    
    /**
     * Convert to mutable asset for serialization.
     */
    public HyforgedItemDataAsset toAsset() {
        return new HyforgedItemDataAsset(this);
    }
}
