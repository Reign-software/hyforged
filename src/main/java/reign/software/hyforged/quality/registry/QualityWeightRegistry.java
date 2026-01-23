package reign.software.hyforged.quality.registry;

import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import reign.software.hyforged.quality.model.QualityWeightProfile;
import reign.software.hyforged.quality.model.QualityWeightTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for quality weight profiles.
 */
public final class QualityWeightRegistry {

    private static final Logger LOGGER = Logger.getLogger(QualityWeightRegistry.class.getName());
    private static final Set<String> EXCLUDED_QUALITIES = Set.of(
            "Junk",
            "Tool",
            "Template",
            "Debug",
            "Developer",
            "Technical"
    );

    private static QualityWeightRegistry instance;

    private final Map<String, QualityWeightProfile> profilesById = new ConcurrentHashMap<>();
    private final Map<String, QualityWeightTable> tablesById = new ConcurrentHashMap<>();
    private Map<String, Integer> qualityOrderCache;

    private boolean frozen = false;

    private QualityWeightRegistry() {}

    @Nonnull
    public static synchronized QualityWeightRegistry get() {
        if (instance == null) {
            instance = new QualityWeightRegistry();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new QualityWeightRegistry();
    }

    public synchronized void register(@Nonnull QualityWeightProfile profile) {
        Objects.requireNonNull(profile, "profile cannot be null");
        if (frozen) {
            throw new IllegalStateException("Registry is frozen, cannot register new profiles");
        }

        if (profilesById.containsKey(profile.id())) {
            LOGGER.log(Level.WARNING, "Quality weight profile ''{0}'' overridden by later definition", profile.id());
        }

        validateProfile(profile);
        profilesById.put(profile.id(), profile);
        tablesById.put(profile.id(), QualityWeightTable.fromWeights(profile.weights()));
    }

    @Nullable
    public QualityWeightProfile get(@Nonnull String id) {
        return profilesById.get(id);
    }

    @Nullable
    public QualityWeightTable getTable(@Nonnull String id) {
        return tablesById.get(id);
    }

    public boolean contains(@Nonnull String id) {
        return profilesById.containsKey(id);
    }

    @Nonnull
    public Map<String, Integer> getQualityOrder() {
        if (qualityOrderCache == null) {
            qualityOrderCache = buildQualityOrder();
        }
        return qualityOrderCache;
    }

    @Nonnull
    public Set<String> getEquipmentEligibleQualities() {
        return getQualityOrder().keySet();
    }

    public synchronized void freeze() {
        frozen = true;
    }

    private void validateProfile(@Nonnull QualityWeightProfile profile) {
        Set<String> equipmentQualities = getEquipmentEligibleQualities();
        for (String quality : profile.weights().keySet()) {
            if (!equipmentQualities.contains(quality)) {
                LOGGER.log(Level.WARNING, "Quality weight profile {0} references non-equipment quality: {1}",
                        new Object[]{profile.id(), quality});
            }
        }
        for (String quality : profile.eligibleQualities()) {
            if (!equipmentQualities.contains(quality)) {
                LOGGER.log(Level.WARNING, "Quality weight profile {0} has invalid eligible quality: {1}",
                        new Object[]{profile.id(), quality});
            }
        }
    }

    private Map<String, Integer> buildQualityOrder() {
        Map<String, Integer> order = new HashMap<>();
        IndexedLookupTableAssetMap<String, ItemQuality> map = ItemQuality.getAssetMap();
        int max = map.getNextIndex();
        for (int i = 0; i < max; i++) {
            ItemQuality quality = map.getAsset(i);
            if (quality == null) {
                continue;
            }
            String id = quality.getId();
            if (id == null || id.isBlank() || EXCLUDED_QUALITIES.contains(id)) {
                continue;
            }
            order.put(id, i);
        }
        return Collections.unmodifiableMap(new HashMap<>(order));
    }
}
