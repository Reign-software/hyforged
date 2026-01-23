package reign.software.hyforged.quality.model;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Context data for rolling item quality.
 */
public record QualityRollContext(
        @Nonnull String itemId,
        @Nonnull String[] itemCategories,
        @Nonnull String[] itemTags,
        @Nonnull String[] sourceTags,
        @Nullable String sourceType,
        int sourceLevel,
        @Nullable String npcQuality,
        @Nullable Ref<EntityStore> sourceRef,
        @Nullable Ref<EntityStore> playerRef,
        @Nullable String zoneId
) {
    public QualityRollContext {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        Objects.requireNonNull(itemCategories, "itemCategories cannot be null");
        Objects.requireNonNull(itemTags, "itemTags cannot be null");
        Objects.requireNonNull(sourceTags, "sourceTags cannot be null");
    }

    public static QualityRollContext of(
            @Nonnull String itemId,
            @Nonnull String[] itemCategories,
            @Nonnull String[] itemTags
    ) {
        return new QualityRollContext(itemId, itemCategories, itemTags, new String[0], null, 0, null, null, null, null);
    }
}
