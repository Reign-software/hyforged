package reign.software.hyforged.currency.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import reign.software.hyforged.HyforgedPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Component for Tradebar Vault blocks.
 * <p>
 * Stored as a block component in ChunkStore, persisted via CODEC.
 * Each vault is owned by a player and can store Tradebars securely.
 */
public class TradebarVaultComponent implements Component<ChunkStore> {

    @Nonnull
    public static final BuilderCodec<TradebarVaultComponent> CODEC = BuilderCodec.builder(
            TradebarVaultComponent.class,
            TradebarVaultComponent::new
    )
            .appendInherited(
                    new KeyedCodec<>("OwnerUUID", Codec.UUID_BINARY),
                    (o, i) -> o.ownerUUID = i,
                    o -> o.ownerUUID,
                    (o, p) -> o.ownerUUID = p.ownerUUID
            ).add()
            .appendInherited(
                    new KeyedCodec<>("Tier", Codec.INTEGER),
                    (o, i) -> o.tier = i,
                    o -> o.tier,
                    (o, p) -> o.tier = p.tier
            ).add()
            .appendInherited(
                    new KeyedCodec<>("StoredAmount", Codec.INTEGER),
                    (o, i) -> o.storedAmount = i,
                    o -> o.storedAmount,
                    (o, p) -> o.storedAmount = p.storedAmount
            ).add()
            .build();

    /** The UUID of the player who owns this vault */
    @Nullable
    private UUID ownerUUID;

    /** The upgrade tier of the vault (1-5, determines capacity) */
    private int tier = 1;

    /** The amount of Tradebars currently stored */
    private int storedAmount = 0;

    public TradebarVaultComponent() {
        // Default constructor required for ECS
    }

    public TradebarVaultComponent(@Nonnull UUID ownerUUID, int tier, int storedAmount) {
        this.ownerUUID = ownerUUID;
        this.tier = tier;
        this.storedAmount = storedAmount;
    }

    /**
     * Get the component type for TradebarVaultComponent.
     */
    @Nonnull
    public static ComponentType<ChunkStore, TradebarVaultComponent> getComponentType() {
        return HyforgedPlugin.getInstance().getTradebarVaultComponentType();
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nonnull UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public int getStoredAmount() {
        return storedAmount;
    }

    public void setStoredAmount(int storedAmount) {
        this.storedAmount = storedAmount;
    }

    /**
     * Check if a player is the owner of this vault.
     *
     * @param playerUUID The player's UUID
     * @return True if the player owns this vault
     */
    public boolean isOwner(@Nonnull UUID playerUUID) {
        return ownerUUID != null && ownerUUID.equals(playerUUID);
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new TradebarVaultComponent(
                ownerUUID,
                tier,
                storedAmount
        );
    }
}
