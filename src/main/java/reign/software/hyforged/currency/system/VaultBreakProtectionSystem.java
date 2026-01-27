package reign.software.hyforged.currency.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.currency.component.TradebarVaultComponent;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ECS system that prevents non-owners from breaking Tradebar Vault blocks.
 * Listens for BreakBlockEvent and cancels if the block is a vault owned by another player.
 */
public class VaultBreakProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger(VaultBreakProtectionSystem.class.getName());
    
    /** The block type identifier for Tradebar Vault */
    private static final String VAULT_BLOCK_TYPE_ID = "hyforged:Tradebar_Vault";

    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;

    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;

    public VaultBreakProtectionSystem() {
        super(BreakBlockEvent.class);
        this.playerComponentType = Player.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event
    ) {
        // Skip if already cancelled
        if (event.isCancelled()) {
            return;
        }

        // Check if the block being broken is a Tradebar Vault
        BlockType blockType = event.getBlockType();
        if (blockType == null) {
            return;
        }

        String blockTypeId = blockType.getId();
        if (!VAULT_BLOCK_TYPE_ID.equals(blockTypeId)) {
            return;
        }

        // Get the player who is breaking the block
        Player player = archetypeChunk.getComponent(index, playerComponentType);
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, uuidComponentType);
        
        if (player == null || uuidComponent == null) {
            // Non-player entity breaking vault - cancel for safety
            event.setCancelled(true);
            return;
        }

        UUID breakerUUID = uuidComponent.getUuid();
        Vector3i targetBlock = event.getTargetBlock();

        // Get the vault component from the block position
        TradebarVaultComponent vaultComponent = getVaultComponent(store, targetBlock);
        
        if (vaultComponent == null) {
            // No vault component - allow break (shouldn't happen, but safe fallback)
            LOGGER.warning("Vault block at " + targetBlock + " has no vault component");
            return;
        }

        UUID ownerUUID = vaultComponent.getOwnerUUID();
        
        // Check ownership
        if (ownerUUID == null) {
            // No owner set - allow break
            return;
        }

        if (!ownerUUID.equals(breakerUUID)) {
            // Non-owner trying to break vault - cancel!
            event.setCancelled(true);
            
            // Send message to player
            player.sendMessage(
                MessageColors.error("You cannot break this vault - it belongs to another player.")
            );
            
            LOGGER.fine("Blocked non-owner " + breakerUUID + " from breaking vault owned by " + ownerUUID);
        }
        // Owner is breaking their own vault - allow (drops handled by block definition)
    }

    @Nullable
    private TradebarVaultComponent getVaultComponent(@Nonnull Store<EntityStore> store, @Nonnull Vector3i blockPos) {
        try {
            World world = store.getExternalData().getWorld();
            if (world == null) {
                return null;
            }

            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
            if (chunk == null) {
                return null;
            }

            // Get block entity reference at this position
            Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(blockPos.x, blockPos.y, blockPos.z);
            if (blockEntityRef == null) {
                return null;
            }

            // Get the vault component from the block entity
            ComponentType<ChunkStore, TradebarVaultComponent> vaultComponentType = 
                HyforgedPlugin.getInstance().getTradebarVaultComponentType();
            
            return blockEntityRef.getStore().getComponent(blockEntityRef, vaultComponentType);
        } catch (Exception e) {
            LOGGER.warning("Error getting vault component at " + blockPos + ": " + e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Query for entities that have Player and UUIDComponent (players breaking blocks)
        return Query.and(playerComponentType, uuidComponentType);
    }
}
