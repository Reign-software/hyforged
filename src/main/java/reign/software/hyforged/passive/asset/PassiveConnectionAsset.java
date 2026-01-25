package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Asset definition for a passive tree connection.
 * Loaded from JSON as part of PassiveTreeAsset.
 */
public class PassiveConnectionAsset {
    
    public static final BuilderCodec<PassiveConnectionAsset> CODEC = BuilderCodec.builder(
            PassiveConnectionAsset.class,
            PassiveConnectionAsset::new
        )
        .append(
            new KeyedCodec<>("From", Codec.STRING),
            (asset, value) -> asset.from = value,
            asset -> asset.from
        )
        .add()
        .append(
            new KeyedCodec<>("To", Codec.STRING),
            (asset, value) -> asset.to = value,
            asset -> asset.to
        )
        .add()
        .build();
    
    private String from;
    private String to;
    
    public PassiveConnectionAsset() {
        // Required for codec
    }
    
    @Nonnull
    public String getFrom() {
        return from != null ? from : "";
    }
    
    @Nonnull
    public String getTo() {
        return to != null ? to : "";
    }
}
