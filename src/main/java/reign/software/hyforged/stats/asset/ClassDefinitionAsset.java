package reign.software.hyforged.stats.asset;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import reign.software.hyforged.stats.StatId;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON asset definition for character classes.
 * <p>
 * Classes define the base ability score distribution for players.
 * Each class specifies which ability scores to allocate and their values.
 * <p>
 * JSON Schema:
 * <pre>
 * {
 *   "Id": "hyforged:warrior",
 *   "DisplayName": "Warrior",
 *   "Description": "A powerful melee combatant...",
 *   "Strength": 5,
 *   "Constitution": 3,
 *   "Dexterity": 2
 * }
 * </pre>
 * <p>
 * Ability scores not specified default to 1.
 */
public class ClassDefinitionAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, ClassDefinitionAsset>> {

    /**
     * Codec for loading ClassDefinitionAsset from JSON.
     */
    public static final AssetBuilderCodec<String, ClassDefinitionAsset> CODEC = AssetBuilderCodec
            .builder(
                    ClassDefinitionAsset.class,
                    ClassDefinitionAsset::new,
                    Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data
            )
            .appendInherited(
                    new KeyedCodec<>("DisplayName", Codec.STRING),
                    (asset, value) -> asset.displayName = value != null ? value : "Unknown Class",
                    asset -> asset.displayName,
                    (asset, parent) -> asset.displayName = parent.displayName
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Description", Codec.STRING),
                    (asset, value) -> asset.description = value != null ? value : "",
                    asset -> asset.description,
                    (asset, parent) -> asset.description = parent.description
            )
            .add()
            // Core ability scores as individual fields
            .appendInherited(
                    new KeyedCodec<>("Strength", Codec.INTEGER),
                    (asset, value) -> asset.strength = value != null ? value : 1,
                    asset -> asset.strength,
                    (asset, parent) -> asset.strength = parent.strength
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Dexterity", Codec.INTEGER),
                    (asset, value) -> asset.dexterity = value != null ? value : 1,
                    asset -> asset.dexterity,
                    (asset, parent) -> asset.dexterity = parent.dexterity
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Intelligence", Codec.INTEGER),
                    (asset, value) -> asset.intelligence = value != null ? value : 1,
                    asset -> asset.intelligence,
                    (asset, parent) -> asset.intelligence = parent.intelligence
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Constitution", Codec.INTEGER),
                    (asset, value) -> asset.constitution = value != null ? value : 1,
                    asset -> asset.constitution,
                    (asset, parent) -> asset.constitution = parent.constitution
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Wisdom", Codec.INTEGER),
                    (asset, value) -> asset.wisdom = value != null ? value : 1,
                    asset -> asset.wisdom,
                    (asset, parent) -> asset.wisdom = parent.wisdom
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Spirit", Codec.INTEGER),
                    (asset, value) -> asset.spirit = value != null ? value : 1,
                    asset -> asset.spirit,
                    (asset, parent) -> asset.spirit = parent.spirit
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Luck", Codec.INTEGER),
                    (asset, value) -> asset.luck = value != null ? value : 1,
                    asset -> asset.luck,
                    (asset, parent) -> asset.luck = parent.luck
            )
            .add()
            .build();

    // Asset metadata
    private String id;
    private AssetExtraInfo.Data data;
    
    // Class properties
    private String displayName = "Unknown Class";
    private String description = "";
    
    // Ability scores
    private int strength = 1;
    private int dexterity = 1;
    private int intelligence = 1;
    private int constitution = 1;
    private int wisdom = 1;
    private int spirit = 1;
    private int luck = 1;

    public ClassDefinitionAsset() {
        // Required for codec
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }
    
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getIntelligence() { return intelligence; }
    public int getConstitution() { return constitution; }
    public int getWisdom() { return wisdom; }
    public int getSpirit() { return spirit; }
    public int getLuck() { return luck; }

    /**
     * Convert to a ClassDefinition domain object.
     */
    @Nonnull
    public ClassDefinition toClassDefinition() {
        Map<StatId, Integer> abilityScores = new HashMap<>();
        
        // Map the fixed ability scores to StatIds
        abilityScores.put(StatId.hyforged("strength"), strength);
        abilityScores.put(StatId.hyforged("dexterity"), dexterity);
        abilityScores.put(StatId.hyforged("intelligence"), intelligence);
        abilityScores.put(StatId.hyforged("constitution"), constitution);
        abilityScores.put(StatId.hyforged("wisdom"), wisdom);
        abilityScores.put(StatId.hyforged("spirit"), spirit);
        abilityScores.put(StatId.hyforged("luck"), luck);
        
        return new ClassDefinition(
            id,
            displayName,
            description,
            abilityScores
        );
    }
}

