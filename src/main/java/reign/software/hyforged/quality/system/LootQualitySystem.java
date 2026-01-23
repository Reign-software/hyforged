package reign.software.hyforged.quality.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import reign.software.hyforged.affix.model.HyforgedItemData;
import reign.software.hyforged.affix.service.HyforgedItemDataService;
import reign.software.hyforged.affix.system.LootAffixSystem;
import reign.software.hyforged.quality.event.QualityRolledEvent;
import reign.software.hyforged.quality.model.QualityRollContext;
import reign.software.hyforged.quality.service.HyforgedQualityService;
import reign.software.hyforged.quality.service.QualityContextBuilder;
import reign.software.hyforged.quality.service.QualityRollerService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ECS system that rolls item quality on new item drops before affixes are applied.
 */
public class LootQualitySystem extends RefChangeSystem<EntityStore, ItemComponent> {

    private static final Logger LOGGER = Logger.getLogger(LootQualitySystem.class.getName());
    private static final float SOURCE_SEARCH_RADIUS = 8.0f;
    private static final String META_SOURCE_UUID = "hyforged.loot.sourceUuid";
    private static final String META_PLAYER_UUID = "hyforged.loot.playerUuid";
    private static final String META_SOURCE_TYPE = "hyforged.loot.sourceType";
    private static final String META_SOURCE_TAGS = "hyforged.loot.sourceTags";
    private static final String META_ZONE_ID = "hyforged.loot.zoneId";

    private final QualityRollerService rollerService;
    private final Set<Dependency<EntityStore>> dependencies;

    public LootQualitySystem() {
        this(new QualityRollerService());
    }

    public LootQualitySystem(@Nonnull QualityRollerService rollerService) {
        this.rollerService = rollerService;
        this.dependencies = Set.of(new SystemDependency<>(Order.BEFORE, LootAffixSystem.class));
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, ItemComponent> componentType() {
        return ItemComponent.getComponentType();
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return ItemComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent itemComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ItemStack itemStack = itemComponent.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        if (HyforgedQualityService.hasQualityOverride(itemStack)) {
            return;
        }

        HyforgedItemData existingData = HyforgedItemDataService.read(itemStack);
        if (existingData.hasAffixes()) {
            return;
        }

        LootContext lootContext = resolveLootContext(ref, itemStack, store);
        QualityRollContext context = QualityContextBuilder.fromItemStack(
                itemStack,
                store,
                lootContext.sourceRef,
                lootContext.playerRef,
                lootContext.sourceType,
                lootContext.zoneId,
                lootContext.sourceTags
        );
        if (context == null) {
            return;
        }

        String originalQuality = HyforgedQualityService.getEffectiveQuality(itemStack);
        long seed = new Random().nextLong();
        String rolledQuality = rollerService.rollQuality(context, new Random(seed));
        if (rolledQuality == null || rolledQuality.isBlank()) {
            return;
        }

        QualityRolledEvent event = emitQualityRolledEvent(context, originalQuality, rolledQuality, seed);
        if (event != null && event.isCancelled()) {
            LOGGER.log(Level.FINE, "Quality roll cancelled for item {0}", itemStack.getItemId());
            return;
        }

        String finalQuality = event != null ? event.getRolledQuality() : rolledQuality;
        if (finalQuality == null || finalQuality.isBlank()) {
            return;
        }

        if (finalQuality.equals(originalQuality)) {
            return;
        }

        ItemStack updated = HyforgedQualityService.withQuality(itemStack, finalQuality);
        itemComponent.setItemStack(updated);
    }

    @Nullable
        private QualityRolledEvent emitQualityRolledEvent(
            @Nonnull QualityRollContext context,
            @Nonnull String originalQuality,
            @Nonnull String rolledQuality,
            long seed
    ) {
        try {
            IEventDispatcher<QualityRolledEvent, QualityRolledEvent> dispatcher =
                    HytaleServer.get().getEventBus().dispatchFor(QualityRolledEvent.class);

            QualityRolledEvent event = new QualityRolledEvent(
                    context,
                    originalQuality,
                    rolledQuality,
                seed
            );

            dispatcher.dispatch(event);
            return event;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to emit QualityRolledEvent for item " + context.itemId(), e);
            return null;
        }
    }

    @Nonnull
    private LootContext resolveLootContext(
            @Nonnull Ref<EntityStore> itemRef,
            @Nonnull ItemStack itemStack,
            @Nonnull Store<EntityStore> store
    ) {
        Ref<EntityStore> sourceRef = resolveRefFromMetadata(itemStack, store, META_SOURCE_UUID);
        Ref<EntityStore> playerRef = resolveRefFromMetadata(itemStack, store, META_PLAYER_UUID);
        String sourceType = readMetadataString(itemStack, META_SOURCE_TYPE);
        String zoneId = readMetadataString(itemStack, META_ZONE_ID);
        String[] sourceTags = readMetadataStringArray(itemStack, META_SOURCE_TAGS);

        if (zoneId == null || zoneId.isBlank()) {
            zoneId = store.getExternalData().getWorld().getName();
        }

        if (sourceRef == null) {
            sourceRef = findNearbyLootSource(itemRef, store);
        }

        if (sourceRef != null && playerRef == null) {
            playerRef = resolveKillerPlayer(sourceRef, store);
        }

        if (sourceType == null || sourceType.isBlank()) {
            sourceType = deriveSourceType(sourceRef, store);
        }

        if (sourceTags.length == 0) {
            sourceTags = deriveSourceTags(sourceRef, store);
        }

        return new LootContext(sourceRef, playerRef, sourceType, zoneId, sourceTags);
    }

    @Nullable
    private Ref<EntityStore> resolveRefFromMetadata(
            @Nonnull ItemStack itemStack,
            @Nonnull Store<EntityStore> store,
            @Nonnull String key
    ) {
        UUID uuid = itemStack.getFromMetadataOrNull(key, Codec.UUID_STRING);
        if (uuid == null) {
            return null;
        }
        Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(uuid);
        return ref != null && ref.isValid() ? ref : null;
    }

    @Nullable
    private String readMetadataString(@Nonnull ItemStack itemStack, @Nonnull String key) {
        return itemStack.getFromMetadataOrNull(key, Codec.STRING);
    }

    @Nonnull
    private String[] readMetadataStringArray(@Nonnull ItemStack itemStack, @Nonnull String key) {
        String[] tags = itemStack.getFromMetadataOrNull(key, Codec.STRING_ARRAY);
        return tags != null ? tags : new String[0];
    }

    @Nullable
    private Ref<EntityStore> findNearbyLootSource(
            @Nonnull Ref<EntityStore> itemRef,
            @Nonnull Store<EntityStore> store
    ) {
        TransformComponent transform = store.getComponent(itemRef, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }

        List<Ref<EntityStore>> candidates = TargetUtil.getAllEntitiesInSphere(
                transform.getPosition(),
                SOURCE_SEARCH_RADIUS,
                store
        );

        Ref<EntityStore> bestRef = null;
        double bestDistance = Double.MAX_VALUE;

        for (Ref<EntityStore> candidate : candidates) {
            if (candidate == null || !candidate.isValid() || candidate.equals(itemRef)) {
                continue;
            }

            DeathComponent death = store.getComponent(candidate, DeathComponent.getComponentType());
            if (death == null) {
                continue;
            }

            TransformComponent candidateTransform = store.getComponent(candidate, TransformComponent.getComponentType());
            if (candidateTransform == null) {
                continue;
            }

            double distance = candidateTransform.getPosition().distanceSquaredTo(transform.getPosition());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestRef = candidate;
            }
        }

        return bestRef;
    }

    @Nullable
    private Ref<EntityStore> resolveKillerPlayer(@Nonnull Ref<EntityStore> sourceRef, @Nonnull Store<EntityStore> store) {
        DeathComponent death = store.getComponent(sourceRef, DeathComponent.getComponentType());
        if (death == null) {
            return null;
        }
        Damage deathInfo = death.getDeathInfo();
        if (deathInfo == null || !(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        Ref<EntityStore> killerRef = entitySource.getRef();
        if (killerRef == null || !killerRef.isValid()) {
            return null;
        }
        Player player = store.getComponent(killerRef, Player.getComponentType());
        return player != null ? killerRef : null;
    }

    @Nonnull
    private String deriveSourceType(@Nullable Ref<EntityStore> sourceRef, @Nonnull Store<EntityStore> store) {
        if (sourceRef == null || !sourceRef.isValid()) {
            return "loot";
        }
        if (store.getComponent(sourceRef, NPCEntity.getComponentType()) != null) {
            return "npc";
        }
        if (store.getComponent(sourceRef, Player.getComponentType()) != null) {
            return "player";
        }
        return "loot";
    }

    @Nonnull
    private String[] deriveSourceTags(@Nullable Ref<EntityStore> sourceRef, @Nonnull Store<EntityStore> store) {
        if (sourceRef == null || !sourceRef.isValid()) {
            return new String[0];
        }

        List<String> tags = new ArrayList<>();
        NPCEntity npcEntity = store.getComponent(sourceRef, NPCEntity.getComponentType());
        if (npcEntity != null) {
            tags.add("Type:NPC");
            String roleName = npcEntity.getRoleName();
            if (roleName != null && !roleName.isBlank()) {
                tags.add("Role:" + roleName);
            }
            Role role = npcEntity.getRole();
            if (role != null && role.getDropListId() != null && !role.getDropListId().isBlank()) {
                tags.add("DropList:" + role.getDropListId());
            }
            return tags.toArray(String[]::new);
        }

        if (store.getComponent(sourceRef, Player.getComponentType()) != null) {
            tags.add("Type:Player");
        }

        return tags.toArray(String[]::new);
    }

    private record LootContext(
            @Nullable Ref<EntityStore> sourceRef,
            @Nullable Ref<EntityStore> playerRef,
            @Nonnull String sourceType,
            @Nonnull String zoneId,
            @Nonnull String[] sourceTags
    ) {
        private LootContext {
            sourceType = sourceType != null ? sourceType : "loot";
            zoneId = zoneId != null ? zoneId : "default";
            sourceTags = sourceTags != null ? sourceTags : new String[0];
        }
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nullable ItemComponent oldComponent,
            @Nonnull ItemComponent newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action on component modification
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemComponent itemComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed
    }
}
