package reign.software.hyforged.passive.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asset definition for a passive node effect.
 * Loaded from JSON as part of PassiveNodeAsset.
 */
public class PassiveNodeEffectAsset {
    
    // Note: For self-referencing codec, we define it after the base codec
    public static final BuilderCodec<PassiveNodeEffectAsset> CODEC = BuilderCodec.builder(
            PassiveNodeEffectAsset.class,
            PassiveNodeEffectAsset::new
        )
        .append(
            new KeyedCodec<>("Type", Codec.STRING),
            (asset, value) -> asset.type = value,
            asset -> asset.type
        )
        .add()
        // Generic data map for effect-specific parameters
        .append(
            new KeyedCodec<>("Stat", Codec.STRING),
            (asset, value) -> asset.stat = value,
            asset -> asset.stat
        )
        .add()
        .append(
            new KeyedCodec<>("Value", Codec.INTEGER),
            (asset, value) -> asset.value = value,
            asset -> asset.value
        )
        .add()
        .append(
            new KeyedCodec<>("SpellId", Codec.STRING),
            (asset, value) -> asset.spellId = value,
            asset -> asset.spellId
        )
        .add()
        .append(
            new KeyedCodec<>("FlagId", Codec.STRING),
            (asset, value) -> asset.flagId = value,
            asset -> asset.flagId
        )
        .add()
        // Note: Choices are serialized as arrays, loaded at asset loading time
        .build();
    
    // Array codec for use by PassiveNodeAsset - defined after CODEC
    public static final ArrayCodec<PassiveNodeEffectAsset> ARRAY_CODEC = 
        new ArrayCodec<>(CODEC, PassiveNodeEffectAsset[]::new);
    
    private String type;
    private String stat;
    private Integer value;
    private String spellId;
    private String flagId;
    private List<PassiveNodeEffectAsset> choices;
    
    public PassiveNodeEffectAsset() {
        // Required for codec
    }
    
    @Nonnull
    public String getType() {
        return type != null ? type : "";
    }
    
    public String getStat() {
        return stat;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getSpellId() {
        return spellId;
    }
    
    public String getFlagId() {
        return flagId;
    }
    
    public List<PassiveNodeEffectAsset> getChoices() {
        return choices != null ? choices : new ArrayList<>();
    }
    
    /**
     * Convert to a data map for PassiveNodeEffect.
     */
    @Nonnull
    public Map<String, Object> toDataMap() {
        Map<String, Object> data = new HashMap<>();
        if (stat != null) {
            data.put("Stat", stat);
        }
        if (value != null) {
            data.put("Value", value);
        }
        if (spellId != null) {
            data.put("SpellId", spellId);
        }
        if (flagId != null) {
            data.put("FlagId", flagId);
        }
        if (choices != null && !choices.isEmpty()) {
            data.put("Choices", choices);
        }
        return data;
    }
}
