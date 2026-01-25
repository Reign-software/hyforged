package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Asset definition for a passive node position.
 * Loaded from JSON as part of PassiveNodeAsset.
 */
public class PassiveNodePositionAsset {
    
    public static final BuilderCodec<PassiveNodePositionAsset> CODEC = BuilderCodec.builder(
            PassiveNodePositionAsset.class,
            PassiveNodePositionAsset::new
        )
        .append(
            new KeyedCodec<>("X", Codec.INTEGER),
            (asset, value) -> asset.x = value,
            asset -> asset.x
        )
        .add()
        .append(
            new KeyedCodec<>("Y", Codec.INTEGER),
            (asset, value) -> asset.y = value,
            asset -> asset.y
        )
        .add()
        .build();
    
    private Integer x;
    private Integer y;
    
    public PassiveNodePositionAsset() {
        // Required for codec
    }
    
    public int getX() {
        return x != null ? x : 0;
    }
    
    public int getY() {
        return y != null ? y : 0;
    }
}
