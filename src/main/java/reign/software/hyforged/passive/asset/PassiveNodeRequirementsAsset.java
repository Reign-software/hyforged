package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Asset definition for a passive node requirements.
 * Loaded from JSON as part of PassiveNodeAsset.
 */
public class PassiveNodeRequirementsAsset {
    
    public static final BuilderCodec<PassiveNodeRequirementsAsset> CODEC = BuilderCodec.builder(
            PassiveNodeRequirementsAsset.class,
            PassiveNodeRequirementsAsset::new
        )
        .append(
            new KeyedCodec<>("AllocatedNodes", Codec.INTEGER),
            (asset, value) -> asset.allocatedNodes = value,
            asset -> asset.allocatedNodes
        )
        .add()
        .append(
            new KeyedCodec<>("Tags", Codec.STRING_ARRAY),
            (asset, value) -> asset.tags = value != null ? Arrays.asList(value) : null,
            asset -> asset.tags != null ? asset.tags.toArray(new String[0]) : null
        )
        .add()
        .build();
    
    private Integer allocatedNodes;
    private List<String> tags;
    
    public PassiveNodeRequirementsAsset() {
        // Required for codec
    }
    
    public int getAllocatedNodes() {
        return allocatedNodes != null ? allocatedNodes : 0;
    }
    
    @Nonnull
    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }
}
