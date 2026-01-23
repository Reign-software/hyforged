package reign.software.hyforged.quality.service;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility for reading/writing Hyforged quality overrides on ItemStack metadata.
 */
public final class HyforgedQualityService {

    public static final String QUALITY_KEY = "hyforged.quality";
    private static final String DEFAULT_QUALITY = "Common";

    private HyforgedQualityService() {}

    @Nonnull
    public static String getEffectiveQuality(@Nonnull ItemStack itemStack) {
        String override = itemStack.getFromMetadataOrNull(QUALITY_KEY, Codec.STRING);
        if (override != null && !override.isBlank()) {
            return override;
        }
        Item item = itemStack.getItem();
        if (item == null || item == Item.UNKNOWN) {
            return DEFAULT_QUALITY;
        }
        int qualityIndex = item.getQualityIndex();
        if (qualityIndex >= 0) {
            ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityIndex);
            if (quality != null && quality.getId() != null && !quality.getId().isBlank()) {
                return quality.getId();
            }
        }
        return DEFAULT_QUALITY;
    }

    public static boolean hasQualityOverride(@Nonnull ItemStack itemStack) {
        return itemStack.getFromMetadataOrNull(QUALITY_KEY, Codec.STRING) != null;
    }

    @Nonnull
    public static ItemStack withQuality(@Nonnull ItemStack itemStack, @Nonnull String qualityId) {
        return itemStack.withMetadata(QUALITY_KEY, Codec.STRING, qualityId);
    }

    @Nullable
    public static String getQualityOverride(@Nonnull ItemStack itemStack) {
        return itemStack.getFromMetadataOrNull(QUALITY_KEY, Codec.STRING);
    }
}
