package reign.software.hyforged.progression.xp.objective;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.builtin.adventure.objectives.config.completion.ObjectiveCompletionAsset;

import javax.annotation.Nonnull;

/**
 * Asset configuration for XP award objective completion.
 * <p>
 * Used in objective JSON files to grant XP on completion:
 * <pre>
 * "Completions": [
 *   {
 *     "Type": "hyforged:xp_award",
 *     "XpAmount": 100,
 *     "Tier": "standard"
 *   }
 * ]
 * </pre>
 */
public class XPAwardCompletionAsset extends ObjectiveCompletionAsset {
    
    public static final BuilderCodec<XPAwardCompletionAsset> CODEC = BuilderCodec.builder(
            XPAwardCompletionAsset.class, XPAwardCompletionAsset::new, BASE_CODEC
    )
            .append(
                    new KeyedCodec<>("XpAmount", Codec.LONG),
                    (asset, value) -> asset.xpAmount = value != null ? value : -1L,
                    asset -> asset.xpAmount > 0 ? asset.xpAmount : null
            )
            .add()
            .append(
                    new KeyedCodec<>("Tier", Codec.STRING),
                    (asset, value) -> asset.tier = value != null ? value : "standard",
                    asset -> asset.tier
            )
            .add()
            .build();
    
    private long xpAmount = -1;
    private String tier = "standard";
    
    protected XPAwardCompletionAsset() {
        super();
    }
    
    /**
     * Get the explicit XP amount, or -1 if tier-based calculation should be used.
     */
    public long getXpAmount() {
        return xpAmount;
    }
    
    /**
     * Get the XP tier for tier-based calculation.
     * Used when XpAmount is not explicitly set.
     * Valid values: "minor", "standard", "major", "legendary"
     */
    @Nonnull
    public String getTier() {
        return tier;
    }
    
    @Nonnull
    @Override
    public String toString() {
        return String.format("XPAwardCompletionAsset{xpAmount=%d, tier=%s}", xpAmount, tier);
    }
}
