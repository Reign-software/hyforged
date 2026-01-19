package reign.software.hyforged.stats.modifier;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Hyforged's ARPG-style modifier extending Hytale's native Modifier system.
 * <p>
 * This integrates with Hytale's EntityStatMap and can be used directly in item JSON
 * via the "Hyforged" modifier type.
 * <p>
 * Stacking semantics:
 * <ul>
 *   <li>FLAT - Added directly to base value</li>
 *   <li>INCREASED - Additive with other INCREASED, then multiplied (PoE-style)</li>
 *   <li>MORE - Multiplicative with each other (PoE-style)</li>
 *   <li>CAP - Enforces min/max bounds</li>
 * </ul>
 * <p>
 * Values use basis points: 1000 = 100%
 * <p>
 * Note: When used via Hytale's apply() method, this converts to float-based math.
 * For full ARPG stacking, use HyforgedStackingSystem which collects all modifiers
 * per stat and applies them in the correct order.
 */
public class HyforgedModifier extends Modifier {
    
    /**
     * Base codec for HyforgedModifier fields (without Type discriminator).
     * Used for JSON deserialization in item StatModifiers.
     */
    public static final BuilderCodec<HyforgedModifier> CODEC = BuilderCodec.builder(
            HyforgedModifier.class,
            HyforgedModifier::new,
            Modifier.BASE_CODEC
        )
        .append(
            new KeyedCodec<>("StackType", new EnumCodec<>(StackType.class)),
            (mod, value) -> mod.stackType = value,
            mod -> mod.stackType
        )
        .add()
        .append(
            new KeyedCodec<>("Amount", Codec.INTEGER),
            (mod, value) -> mod.amount = value,
            mod -> mod.amount
        )
        .add()
        .append(
            new KeyedCodec<>("SourceType", new EnumCodec<>(SourceType.class)),
            (mod, value) -> mod.sourceType = value,
            mod -> mod.sourceType
        )
        .add()
        .append(
            new KeyedCodec<>("SourceId", Codec.STRING),
            (mod, value) -> mod.sourceId = value,
            mod -> mod.sourceId
        )
        .add()
        .append(
            new KeyedCodec<>("Priority", Codec.INTEGER),
            (mod, value) -> mod.priority = value,
            mod -> mod.priority
        )
        .add()
        .build();
    
    /**
     * Stacking behavior type for ARPG-style modifier computation.
     */
    public enum StackType {
        /**
         * Flat addition/subtraction. Applied first, all FLAT modifiers are summed.
         * Amount is the raw value to add.
         */
        FLAT(0),
        
        /**
         * Percentage increase (additive with other INCREASED).
         * Applied second, all INCREASED values are summed then applied as multiplier.
         * Amount is in basis points (100 = 10% increase).
         */
        INCREASED(1),
        
        /**
         * Percentage more (multiplicative with each other).
         * Applied third, each MORE modifier is applied sequentially.
         * Amount is in basis points (100 = 10% more).
         */
        MORE(2),
        
        /**
         * Cap/clamp enforcement. Applied last.
         * Positive amount = max cap, negative amount = min cap.
         */
        CAP(3);
        
        private final int order;
        
        StackType(int order) {
            this.order = order;
        }
        
        public int getOrder() {
            return order;
        }
    }
    
    /**
     * Source category for UI grouping and debugging.
     */
    public enum SourceType {
        BASE,
        ABILITY_SCORE,
        EQUIPMENT,
        BUFF,
        PASSIVE,
        CLASS,
        EFFECT,
        ADMIN
    }
    
    /** Basis points representing 100% */
    public static final int BPS_100_PERCENT = 1000;
    
    protected StackType stackType = StackType.FLAT;
    protected int amount = 0;
    protected SourceType sourceType = SourceType.EQUIPMENT;
    protected String sourceId = "";
    protected int priority = 0;
    
    /**
     * Default constructor for codec deserialization.
     */
    public HyforgedModifier() {
        super();
    }
    
    /**
     * Full constructor for programmatic creation.
     */
    public HyforgedModifier(
            ModifierTarget target,
            StackType stackType,
            int amount,
            SourceType sourceType,
            String sourceId,
            int priority
    ) {
        super(target);
        this.stackType = stackType;
        this.amount = amount;
        this.sourceType = sourceType;
        this.sourceId = sourceId != null ? sourceId : "";
        this.priority = priority;
    }
    
    /**
     * Convenience constructor for simple modifiers.
     */
    public HyforgedModifier(StackType stackType, int amount) {
        this(ModifierTarget.MAX, stackType, amount, SourceType.EQUIPMENT, "", 0);
    }
    
    // ============ Getters ============
    
    public StackType getStackType() {
        return stackType;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public SourceType getSourceType() {
        return sourceType;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public int getPriority() {
        return priority;
    }
    
    // ============ Hytale Modifier Integration ============
    
    /**
     * Apply this modifier to a stat value.
     * <p>
     * Note: This is called by Hytale's EntityStatMap for individual modifier application.
     * For proper ARPG stacking (where FLAT is summed before INCREASED is applied),
     * use HyforgedStackingSystem which collects all modifiers first.
     * <p>
     * This method provides a reasonable approximation for single-modifier application:
     * - FLAT: Adds amount directly
     * - INCREASED: Multiplies by (1 + amount/1000)
     * - MORE: Multiplies by (1 + amount/1000)
     * - CAP: Clamps to amount (positive = max, negative = min)
     */
    @Override
    public float apply(float statValue) {
        return switch (stackType) {
            case FLAT -> statValue + amount;
            case INCREASED, MORE -> statValue * (1.0f + (float) amount / BPS_100_PERCENT);
            case CAP -> {
                if (amount >= 0) {
                    yield Math.min(statValue, amount);
                } else {
                    yield Math.max(statValue, -amount);
                }
            }
        };
    }
    
    /**
     * Convert to network packet for client sync.
     * <p>
     * Uses StaticModifier's packet format for client compatibility,
     * mapping our StackType to Hytale's CalculationType.
     */
    @Nonnull
    @Override
    public com.hypixel.hytale.protocol.Modifier toPacket() {
        com.hypixel.hytale.protocol.Modifier packet = super.toPacket();
        
        // Map our StackType to Hytale's CalculationType for client display
        packet.calculationType = switch (stackType) {
            case FLAT -> com.hypixel.hytale.protocol.CalculationType.Additive;
            case INCREASED, MORE, CAP -> com.hypixel.hytale.protocol.CalculationType.Multiplicative;
        };
        
        // Convert basis points to float for client
        packet.amount = switch (stackType) {
            case FLAT -> (float) amount;
            case INCREASED, MORE -> (float) amount / BPS_100_PERCENT;
            case CAP -> (float) amount;
        };
        
        return packet;
    }
    
    // ============ Object Methods ============
    
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        HyforgedModifier that = (HyforgedModifier) o;
        return amount == that.amount
            && priority == that.priority
            && stackType == that.stackType
            && sourceType == that.sourceType
            && sourceId.equals(that.sourceId);
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + stackType.hashCode();
        result = 31 * result + amount;
        result = 31 * result + sourceType.hashCode();
        result = 31 * result + sourceId.hashCode();
        result = 31 * result + priority;
        return result;
    }
    
    @Nonnull
    @Override
    public String toString() {
        return "HyforgedModifier{" +
            "stackType=" + stackType +
            ", amount=" + amount +
            ", sourceType=" + sourceType +
            ", sourceId='" + sourceId + "'" +
            ", priority=" + priority +
            "} " + super.toString();
    }
    
    // ============ Builder ============
    
    /**
     * Create a builder for constructing HyforgedModifier instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ModifierTarget target = ModifierTarget.MAX;
        private StackType stackType = StackType.FLAT;
        private int amount = 0;
        private SourceType sourceType = SourceType.EQUIPMENT;
        private String sourceId = "";
        private int priority = 0;
        
        public Builder target(ModifierTarget target) {
            this.target = target;
            return this;
        }
        
        public Builder stackType(StackType stackType) {
            this.stackType = stackType;
            return this;
        }
        
        public Builder flat(int amount) {
            this.stackType = StackType.FLAT;
            this.amount = amount;
            return this;
        }
        
        public Builder increased(int basisPoints) {
            this.stackType = StackType.INCREASED;
            this.amount = basisPoints;
            return this;
        }
        
        public Builder more(int basisPoints) {
            this.stackType = StackType.MORE;
            this.amount = basisPoints;
            return this;
        }
        
        public Builder maxCap(int cap) {
            this.stackType = StackType.CAP;
            this.amount = cap;
            return this;
        }
        
        public Builder minCap(int cap) {
            this.stackType = StackType.CAP;
            this.amount = -cap;
            return this;
        }
        
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder sourceType(SourceType sourceType) {
            this.sourceType = sourceType;
            return this;
        }
        
        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public HyforgedModifier build() {
            return new HyforgedModifier(target, stackType, amount, sourceType, sourceId, priority);
        }
    }
}
