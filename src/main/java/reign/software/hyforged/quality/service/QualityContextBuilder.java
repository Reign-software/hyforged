package reign.software.hyforged.quality.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.affix.service.ItemContextExtractor;
import reign.software.hyforged.combat.scaling.MonsterLevelComponent;
import reign.software.hyforged.quality.component.HyforgedNPCQualityComponent;
import reign.software.hyforged.quality.model.QualityRollContext;
import reign.software.hyforged.stats.bridge.ProgressionStatBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds QualityRollContext from item and entity references.
 */
public final class QualityContextBuilder {

    private static final Logger LOGGER = Logger.getLogger(QualityContextBuilder.class.getName());

    private QualityContextBuilder() {}

    @Nullable
    public static QualityRollContext fromItemStack(
            @Nonnull ItemStack itemStack,
            @Nonnull Store<EntityStore> store,
            @Nullable Ref<EntityStore> sourceRef,
            @Nullable Ref<EntityStore> playerRef,
            @Nullable String sourceType,
            @Nullable String zoneId,
            @Nonnull String[] sourceTags
    ) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        Objects.requireNonNull(store, "store cannot be null");

        if (itemStack.isEmpty()) {
            return null;
        }

        String itemId = itemStack.getItemId();
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        Item item = itemStack.getItem();
        if (item == null || item == Item.UNKNOWN) {
            LOGGER.log(Level.FINE, "Unknown item type for quality context: {0}", itemId);
            return null;
        }

        String[] categories = ItemContextExtractor.extractCategories(item);
        String[] tags = ItemContextExtractor.extractTags(item);

        int sourceLevel = resolveSourceLevel(store, sourceRef);
        String npcQuality = resolveNpcQuality(store, sourceRef);

        return new QualityRollContext(
                itemId,
                categories,
                tags,
                sourceTags != null ? sourceTags : new String[0],
                sourceType,
                sourceLevel,
                npcQuality,
                sourceRef,
                playerRef,
                zoneId
        );
    }

    private static int resolveSourceLevel(@Nonnull Store<EntityStore> store, @Nullable Ref<EntityStore> sourceRef) {
        if (sourceRef == null || !sourceRef.isValid()) {
            return 0;
        }
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        MonsterLevelComponent monsterLevel = store.getComponent(sourceRef, plugin.getMonsterLevelComponentType());
        if (monsterLevel != null) {
            return monsterLevel.getLevel();
        }
        return ProgressionStatBridge.getCharacterLevel(sourceRef, store);
    }

    @Nullable
    private static String resolveNpcQuality(@Nonnull Store<EntityStore> store, @Nullable Ref<EntityStore> sourceRef) {
        if (sourceRef == null || !sourceRef.isValid()) {
            return null;
        }
        HyforgedPlugin plugin = HyforgedPlugin.getInstance();
        HyforgedNPCQualityComponent component = store.getComponent(sourceRef, plugin.getNpcQualityComponentType());
        return component != null ? component.getQualityId() : null;
    }
}
