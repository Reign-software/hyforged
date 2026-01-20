package reign.software.hyforged.progression.xp;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;

import javax.annotation.Nonnull;

/**
 * Asset class for loading XP configuration from JSON.
 * <p>
 * Loaded from: Server/Hyforged/Progression/XPConfig.json
 */
public class XPConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, XPConfigAsset>> {
    
    /**
     * Codec for loading XPConfigAsset from JSON.
     */
    public static final AssetBuilderCodec<String, XPConfigAsset> CODEC = AssetBuilderCodec
            .builder(
                    XPConfigAsset.class,
                    XPConfigAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            // Combat settings
            .appendInherited(
                    new KeyedCodec<>("combatBaseXp", Codec.LONG),
                    (asset, value) -> asset.combatBaseXp = value != null ? value : 10L,
                    asset -> asset.combatBaseXp,
                    (asset, parent) -> asset.combatBaseXp = parent.combatBaseXp
            )
            .add()
            // Discovery settings
            .appendInherited(
                    new KeyedCodec<>("discoveryBiomeXp", Codec.LONG),
                    (asset, value) -> asset.discoveryBiomeXp = value != null ? value : 100L,
                    asset -> asset.discoveryBiomeXp,
                    (asset, parent) -> asset.discoveryBiomeXp = parent.discoveryBiomeXp
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("discoveryLandmarkXp", Codec.LONG),
                    (asset, value) -> asset.discoveryLandmarkXp = value != null ? value : 50L,
                    asset -> asset.discoveryLandmarkXp,
                    (asset, parent) -> asset.discoveryLandmarkXp = parent.discoveryLandmarkXp
            )
            .add()
            // Objective settings
            .appendInherited(
                    new KeyedCodec<>("objectiveMinorXp", Codec.LONG),
                    (asset, value) -> asset.objectiveMinorXp = value != null ? value : 25L,
                    asset -> asset.objectiveMinorXp,
                    (asset, parent) -> asset.objectiveMinorXp = parent.objectiveMinorXp
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("objectiveStandardXp", Codec.LONG),
                    (asset, value) -> asset.objectiveStandardXp = value != null ? value : 50L,
                    asset -> asset.objectiveStandardXp,
                    (asset, parent) -> asset.objectiveStandardXp = parent.objectiveStandardXp
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("objectiveMajorXp", Codec.LONG),
                    (asset, value) -> asset.objectiveMajorXp = value != null ? value : 100L,
                    asset -> asset.objectiveMajorXp,
                    (asset, parent) -> asset.objectiveMajorXp = parent.objectiveMajorXp
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("objectiveLegendaryXp", Codec.LONG),
                    (asset, value) -> asset.objectiveLegendaryXp = value != null ? value : 250L,
                    asset -> asset.objectiveLegendaryXp,
                    (asset, parent) -> asset.objectiveLegendaryXp = parent.objectiveLegendaryXp
            )
            .add()
            // Class XP
            .appendInherited(
                    new KeyedCodec<>("classXpRatio", Codec.DOUBLE),
                    (asset, value) -> asset.classXpRatio = value != null ? value : 1.0,
                    asset -> asset.classXpRatio,
                    (asset, parent) -> asset.classXpRatio = parent.classXpRatio
            )
            .add()
            // Level caps
            .appendInherited(
                    new KeyedCodec<>("maxCharacterLevel", Codec.INTEGER),
                    (asset, value) -> asset.maxCharacterLevel = value != null ? value : 100,
                    asset -> asset.maxCharacterLevel,
                    (asset, parent) -> asset.maxCharacterLevel = parent.maxCharacterLevel
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("maxClassLevel", Codec.INTEGER),
                    (asset, value) -> asset.maxClassLevel = value != null ? value : 20,
                    asset -> asset.maxClassLevel,
                    (asset, parent) -> asset.maxClassLevel = parent.maxClassLevel
            )
            .add()
            .build();
    
    // Asset metadata
    private String id;
    private AssetExtraInfo.Data data;
    
    // Configuration fields
    private long combatBaseXp = 10;
    private long discoveryBiomeXp = 100;
    private long discoveryLandmarkXp = 50;
    private long objectiveMinorXp = 25;
    private long objectiveStandardXp = 50;
    private long objectiveMajorXp = 100;
    private long objectiveLegendaryXp = 250;
    private double classXpRatio = 1.0;
    private int maxCharacterLevel = 100;
    private int maxClassLevel = 20;
    
    public XPConfigAsset() {
        // Required for codec
    }
    
    @Nonnull
    public String getId() { return id; }
    
    public long getCombatBaseXp() { return combatBaseXp; }
    public long getDiscoveryBiomeXp() { return discoveryBiomeXp; }
    public long getDiscoveryLandmarkXp() { return discoveryLandmarkXp; }
    
    public long getObjectiveXpForTier(String tier) {
        if (tier == null) return objectiveStandardXp;
        return switch (tier.toLowerCase()) {
            case "minor" -> objectiveMinorXp;
            case "major" -> objectiveMajorXp;
            case "legendary" -> objectiveLegendaryXp;
            default -> objectiveStandardXp;
        };
    }
    
    public long getObjectiveMinorXp() { return objectiveMinorXp; }
    public long getObjectiveStandardXp() { return objectiveStandardXp; }
    public long getObjectiveMajorXp() { return objectiveMajorXp; }
    public long getObjectiveLegendaryXp() { return objectiveLegendaryXp; }
    
    public double getClassXpRatio() { return classXpRatio; }
    public int getMaxCharacterLevel() { return maxCharacterLevel; }
    public int getMaxClassLevel() { return maxClassLevel; }
}
