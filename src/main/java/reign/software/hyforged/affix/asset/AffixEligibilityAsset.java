package reign.software.hyforged.affix.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import reign.software.hyforged.affix.model.AffixEligibility;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Codec for AffixEligibility within affix asset JSON.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "ItemCategories": ["Items.Weapons", "Items.Armor"],
 *   "ItemTags": ["Type:Weapon", "Family:Axe"],
 *   "ExcludeTags": ["Tool"],
 *   "MinQuality": "Rare",
 *   "MaxQuality": "Legendary"
 * }
 * </pre>
 */
public class AffixEligibilityAsset {

    /**
     * Codec for eligibility constraints.
     */
    public static final BuilderCodec<AffixEligibilityAsset> CODEC = BuilderCodec.builder(
                    AffixEligibilityAsset.class,
                    AffixEligibilityAsset::new
            )
            .append(
                    new KeyedCodec<>("ItemCategories", Codec.STRING_ARRAY),
                    (asset, value) -> asset.itemCategories = value,
                    asset -> asset.itemCategories
            )
            .add()
            .append(
                    new KeyedCodec<>("ItemTags", Codec.STRING_ARRAY),
                    (asset, value) -> asset.itemTags = value,
                    asset -> asset.itemTags
            )
            .add()
            .append(
                    new KeyedCodec<>("ExcludeTags", Codec.STRING_ARRAY),
                    (asset, value) -> asset.excludeTags = value,
                    asset -> asset.excludeTags
            )
            .add()
            .append(
                    new KeyedCodec<>("MinQuality", Codec.STRING),
                    (asset, value) -> asset.minQuality = value,
                    asset -> asset.minQuality
            )
            .add()
            .append(
                    new KeyedCodec<>("MaxQuality", Codec.STRING),
                    (asset, value) -> asset.maxQuality = value,
                    asset -> asset.maxQuality
            )
            .add()
            .build();

    private String[] itemCategories;
    private String[] itemTags;
    private String[] excludeTags;
    private String minQuality;
    private String maxQuality;

    public AffixEligibilityAsset() {
    }

    /**
     * Convert this asset to an AffixEligibility model object.
     */
    @Nonnull
    public AffixEligibility toEligibility() {
        return new AffixEligibility(
                toSet(itemCategories),
                toSet(itemTags),
                toSet(excludeTags),
                minQuality,
                maxQuality
        );
    }

    private static Set<String> toSet(String[] array) {
        if (array == null || array.length == 0) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(array));
    }

    // ========== Accessors ==========

    public String[] getItemCategories() {
        return itemCategories;
    }

    public String[] getItemTags() {
        return itemTags;
    }

    public String[] getExcludeTags() {
        return excludeTags;
    }

    public String getMinQuality() {
        return minQuality;
    }

    public String getMaxQuality() {
        return maxQuality;
    }
}
