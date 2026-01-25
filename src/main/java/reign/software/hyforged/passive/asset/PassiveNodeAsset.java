package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Asset definition for a passive node.
 * Loaded from JSON as part of PassiveTreeAsset.
 */
public class PassiveNodeAsset {
    
    public static final BuilderCodec<PassiveNodeAsset> CODEC = BuilderCodec.builder(
            PassiveNodeAsset.class,
            PassiveNodeAsset::new
        )
        .append(
            new KeyedCodec<>("Id", Codec.STRING),
            (asset, value) -> asset.id = value,
            asset -> asset.id
        )
        .add()
        .append(
            new KeyedCodec<>("Type", Codec.STRING),
            (asset, value) -> asset.type = value,
            asset -> asset.type
        )
        .add()
        .append(
            new KeyedCodec<>("Name", Codec.STRING),
            (asset, value) -> asset.name = value,
            asset -> asset.name
        )
        .add()
        .append(
            new KeyedCodec<>("Description", Codec.STRING),
            (asset, value) -> asset.description = value,
            asset -> asset.description
        )
        .add()
        .append(
            new KeyedCodec<>("Icon", Codec.STRING),
            (asset, value) -> asset.icon = value,
            asset -> asset.icon
        )
        .add()
        .append(
            new KeyedCodec<>("Position", PassiveNodePositionAsset.CODEC),
            (asset, value) -> asset.position = value,
            asset -> asset.position
        )
        .add()
        .append(
            new KeyedCodec<>("Region", Codec.STRING),
            (asset, value) -> asset.region = value,
            asset -> asset.region
        )
        .add()
        .append(
            new KeyedCodec<>("Effects", PassiveNodeEffectAsset.ARRAY_CODEC),
            (asset, value) -> asset.effects = value != null ? Arrays.asList(value) : null,
            asset -> asset.effects != null ? asset.effects.toArray(new PassiveNodeEffectAsset[0]) : null
        )
        .add()
        .append(
            new KeyedCodec<>("Requirements", PassiveNodeRequirementsAsset.CODEC),
            (asset, value) -> asset.requirements = value,
            asset -> asset.requirements
        )
        .add()
        .append(
            new KeyedCodec<>("KeystoneFamily", Codec.STRING),
            (asset, value) -> asset.keystoneFamily = value,
            asset -> asset.keystoneFamily
        )
        .add()
        .build();
    
    private String id;
    private String type;
    private String name;
    private String description;
    private String icon;
    private PassiveNodePositionAsset position;
    private String region;
    private List<PassiveNodeEffectAsset> effects;
    private PassiveNodeRequirementsAsset requirements;
    private String keystoneFamily;
    
    public PassiveNodeAsset() {
        // Required for codec
    }
    
    @Nonnull
    public String getId() {
        return id != null ? id : "";
    }
    
    @Nonnull
    public String getType() {
        return type != null ? type : "minor";
    }
    
    @Nonnull
    public String getName() {
        return name != null ? name : "";
    }
    
    @Nonnull
    public String getDescription() {
        return description != null ? description : "";
    }
    
    @Nullable
    public String getIcon() {
        return icon;
    }
    
    @Nullable
    public PassiveNodePositionAsset getPosition() {
        return position;
    }
    
    @Nullable
    public String getRegion() {
        return region;
    }
    
    @Nonnull
    public List<PassiveNodeEffectAsset> getEffects() {
        return effects != null ? effects : new ArrayList<>();
    }
    
    @Nullable
    public PassiveNodeRequirementsAsset getRequirements() {
        return requirements;
    }
    
    @Nullable
    public String getKeystoneFamily() {
        return keystoneFamily;
    }
}
