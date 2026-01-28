package reign.software.hyforged.passive.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.HyforgedPlugin;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.event.PointBookConsumedEvent;
import reign.software.hyforged.passive.registry.PassiveTreeRegistry;
import reign.software.hyforged.util.MessageColors;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interaction for consuming a Point Book item.
 * <p>
 * When used, grants +1 general passive tree point if under the cap.
 * The interaction consumes the item on success.
 * <p>
 * JSON usage in item definition:
 * <pre>
 * "Interactions": {
 *   "Secondary": {
 *     "Type": "Simple",
 *     "Interactions": [
 *       { "Type": "hyforged:point-book-consume" }
 *     ]
 *   }
 * }
 * </pre>
 */
public class PointBookInteraction extends SimpleInstantInteraction {

    private static final Logger LOGGER = Logger.getLogger(PointBookInteraction.class.getName());

    /**
     * Interaction type ID for codec registration.
     */
    public static final String TYPE_ID = "hyforged:point-book-consume";

    /**
     * Codec for JSON deserialization.
     */
    public static final BuilderCodec<PointBookInteraction> CODEC = BuilderCodec.builder(
            PointBookInteraction.class,
            PointBookInteraction::new,
            SimpleInstantInteraction.CODEC
    ).build();

    public PointBookInteraction() {
        super();
    }

    public PointBookInteraction(String id) {
        super(id);
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    protected void firstRun(
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> entityRef = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        
        // Get the player component
        Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get PassiveTreeComponent
        PassiveTreeComponent passiveComponent = getPassiveTreeComponent(entityRef, commandBuffer);
        if (passiveComponent == null) {
            LOGGER.log(Level.WARNING, "Player has no PassiveTreeComponent, cannot use Point Book");
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get max book points from config
        int maxBookPoints = getMaxBookPoints();
        int currentBookPoints = passiveComponent.getBookPointsUsed();

        // Check if at cap
        if (currentBookPoints >= maxBookPoints) {
            // At cap - fail the interaction (preserves the item)
            LOGGER.log(Level.FINE, "Player tried to use Point Book but is at max ({0})", maxBookPoints);
            context.getState().state = InteractionState.Failed;
            player.sendMessage(
                    Message.translation("hyforged.passive.pointBook.maxReached")
                            .param("max", maxBookPoints)
                            .color(MessageColors.WARNING)
            );
            return;
        }

        // Grant the point
        int newTotal = passiveComponent.addBookPoint();
        LOGGER.log(Level.FINE, "Player used Point Book. Book points: {0}/{1}", new Object[]{newTotal, maxBookPoints});

        // Consume the item (reduce held item by 1)
        consumeHeldItem(context);

        // Emit event
        PointBookConsumedEvent event = new PointBookConsumedEvent(entityRef, newTotal, maxBookPoints);
        HytaleServer.get().getEventBus()
                .dispatchFor(PointBookConsumedEvent.class)
                .dispatch(event);

        // Success
        context.getState().state = InteractionState.Finished;
        
        // TODO: Play sound effect
        // TODO: Show visual effect
    }

    /**
     * Get the PassiveTreeComponent from the entity.
     */
    private PassiveTreeComponent getPassiveTreeComponent(Ref<EntityStore> entityRef, CommandBuffer<EntityStore> commandBuffer) {
        var componentType = HyforgedPlugin.getInstance().getPassiveTreeComponentType();
        if (componentType == null) {
            return null;
        }
        return commandBuffer.getComponent(entityRef, componentType);
    }

    /**
     * Get the maximum book points from the refund config.
     */
    private int getMaxBookPoints() {
        var refundConfig = PassiveTreeRegistry.get().getRefundConfig();
        if (refundConfig != null) {
            return refundConfig.getMaxBookPoints();
        }
        // Default fallback
        return 20;
    }

    /**
     * Consume one item from the held stack.
     */
    private void consumeHeldItem(InteractionContext context) {
        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null) {
            return;
        }
        
        // Use the interaction context's method to adjust held item
        int currentQuantity = heldItem.getQuantity();
        if (currentQuantity <= 1) {
            // Remove the item entirely
            context.setHeldItem(null);
        } else {
            // Reduce by 1
            ItemStackSlotTransaction transaction = context.getHeldItemContainer()
                    .removeItemStackFromSlot(context.getHeldItemSlot(), heldItem, 1);
            if (transaction.succeeded()) {
                context.setHeldItem(transaction.getSlotAfter());
            }
        }
    }

    @Override
    public String toString() {
        return "PointBookInteraction{} " + super.toString();
    }
}
